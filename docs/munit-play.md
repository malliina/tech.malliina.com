---
title: MUnit with Play Framework
date: 2020-04-10
---
# MUnit with Play Framework

[MUnit](https://scalameta.org/munit/ "MUnit website") is a new Scala testing library. This post shows how to test
[Play Framework](https://www.playframework.com/ "Play Framework website") apps using MUnit.

## The Play app

Suppose we have the following app, which uses Play's [compile time dependency injection](https://www.playframework.com/documentation/2.8.x/ScalaCompileTimeDependencyInjection "Compile time dependency injection"):

```scala mdoc:invisible
import java.io.File
import akka.actor.ActorSystem
import munit.{FunSuite, Suite}
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
      new Environment(new File("."), classLoader, Mode.Test)
    Context.create(env)
  }
}
```

We create a MUnit [fixture](https://scalameta.org/munit/docs/fixtures.html "MUnit fixtures") that runs one such app per 
test. Then we create another fixture that runs one server per test. Finally, we create fixtures where all tests
in the suite share the same app or server.

### One app per test

Define the following fixture:

```scala mdoc:silent
trait PlayAppFixture { self: FunSuite =>
  val playApp = FunFixture[PingApp](
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
  playApp.test("app responds to ping") { comps =>
    val req = FakeRequest("GET", "/ping")
    val res = await(route(comps.application, req).get)
    assert(res.header.status == 200)
  }
}
```

This is similar to OneAppPerTest in [scalatestplus-play](https://github.com/playframework/scalatestplus-play "scalatestplus-play").

### One server per test

To run one server per test, define the following fixture:

```scala mdoc:silent
trait PlayServerFixture { self: FunSuite =>
  val server = FunFixture[RunningServer](
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
  implicit val as: ActorSystem = ActorSystem("test")
  val http = AhcWSClient()

  server.test("server responds to ping") { server =>
    val port = server.endpoints.httpEndpoint.map(_.port).get
    val res = await(http.url(s"http://localhost:$port/ping").get())
    assert(res.status == 200)
  }
}
```

This is similar to OneServerPerTest in scalatestplus-play. 

### One app per suite

To use the same app in all tests in the suite, define this fixture:

```scala mdoc:silent
trait AppPerSuite { self: Suite =>
  val pingAppFixture = new Fixture[PingApp]("ping-app") {
    private var comps: PingApp = null
    def apply() = comps
    override def beforeAll(): Unit = {
      comps = new PingApp()
      Play.start(comps.application)
    }
    override def afterAll(): Unit = {
      Play.stop(comps.application)
    }
  }
  def pingApp = pingAppFixture().application

  override def munitFixtures = Seq(pingAppFixture)
}
```

Use it as follows:

```scala mdoc:compile-only
class AppTests extends FunSuite with AppPerSuite {
  test("request to ping address returns 200") {
    val req = FakeRequest("GET", "/ping")
    val res = await(route(pingApp, req).get)
    assert(res.header.status == 200)
  }

  test("request to wrong address returns 404") {
    val req = FakeRequest("GET", "/nonexistent")
    val res = await(route(pingApp, req).get)
    assert(res.header.status == 404)
  }
}
```

### One server per suite

To use the same server in all tests in the suite, define this fixture:

```scala mdoc:silent
trait ServerPerSuite { self: Suite =>
  val server = new Fixture[RunningServer]("ping-server") {
    private var runningServer: RunningServer = null
    def apply() = runningServer
    override def beforeAll(): Unit = {
      val app = new PingApp()
      runningServer = DefaultTestServerFactory.start(app.application)
    }
    override def afterAll(): Unit = {
      runningServer.stopServer.close()
    }
  }
  def port = server().endpoints.httpEndpoint.map(_.port).get

  override def munitFixtures = Seq(server)
}
```

Then mix in that trait when writing your tests:

```scala mdoc:compile-only
class ServerTests extends FunSuite with ServerPerSuite {
  implicit val as: ActorSystem = ActorSystem("test")
  val http = AhcWSClient()

  test("request to ping address returns 200") {
    val res = await(http.url(s"http://localhost:$port/ping").get())
    assert(res.status == 200)
  }

  test("request to wrong address returns 404") {
    val res = await(http.url(s"http://localhost:$port/nonexistent").get())
    assert(res.status == 404)
  }
}
```

Complete code samples are available in [GitHub](https://github.com/malliina/blog/blob/master/code/test/com/malliina/code/MUnitPlay.scala). 
Enjoy!
