# MUnit with Play Framework

[MUnit](https://scalameta.org/munit/) is a new Scala testing library. This post shows how to test
[Play Framework](https://www.playframework.com/) apps using MUnit.



## The Play app

Let's use Play's
[compile time dependency injection](https://www.playframework.com/documentation/2.8.x/ScalaCompileTimeDependencyInjection).
Suppose we have the following app:

```scala mdoc:invisible
import akka.actor.ActorSystem
import munit.FunSuite
import play.api.ApplicationLoader.Context
import play.api._
import play.api.libs.ws.ahc.AhcWSClient
import play.api.mvc.Results.Ok
import play.api.routing.Router
import play.api.routing.sird._
import play.api.test.Helpers.{GET => _, _}
import play.api.test.{DefaultTestServerFactory, FakeRequest, RunningServer}
```

```scala mdoc:silent
class PingApp(context: Context = PingApp.createTestAppContext)
  extends BuiltInComponentsFromContext(context) {

  override def httpFilters = Nil
  override def router = Router.from {
    case GET(p"/ping") => Action(Ok("pong"))
  }
}

object PingApp {
  def createTestAppContext: Context = {
    val classLoader = ApplicationLoader.getClass.getClassLoader
    val env =
      new Environment(new java.io.File("."), classLoader, Mode.Test)
    Context.create(env)
  }
}
```

We create a MUnit [fixture](https://scalameta.org/munit/docs/fixtures.html) that runs one such app per 
test. Then we create another fixture that runs one server per test.

### One App per Test

Define the following fixture:

```scala mdoc:silent
trait PlayAppFixture { self: FunSuite =>
  val app = new FunFixture[PingApp](
    opts => {
      val comps = new PingApp()
      Play.start(comps.application)
      comps
    },
    comps => {
      Play.stop(comps.application)
    }
  )
}
```

Then use it as follows:

```scala mdoc:compile-only
class TestSuite extends FunSuite with PlayAppFixture {
  app.test("app responds to ping") { comps =>
    val req = FakeRequest("GET", "/ping")
    val res = await(route(comps.application, req).get)
    assert(res.header.status == 200)
  }
}
```

This is similar to OneAppPerTest in [scalatestplus-play](https://github.com/playframework/scalatestplus-play).

### One Server per Test

To run one server per test, define the following fixture:

```scala mdoc:silent
trait PlayServerFixture { self: FunSuite =>
  val server = new FunFixture[RunningServer](
    opts => {
      val comps = new PingApp()
      DefaultTestServerFactory.start(comps.application)
    },
    running => {
      running.stopServer.close()
    }
  )
}
```

Use it as follows:

```scala mdoc:compile-only
class TestServer extends FunSuite with PlayServerFixture {
  implicit val as = ActorSystem("test")
  val http = AhcWSClient()

  server.test("server responds to ping") { server =>
    val port = server.endpoints.httpEndpoint.map(_.port).get
    val res = await(http.url(s"http://localhost:$port/ping").get())
    assert(res.status == 200)
  }
}
```

This is similar to OneServerPerTest in scalatestplus-play. Enjoy!
