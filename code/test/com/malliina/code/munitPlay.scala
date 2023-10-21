package com.malliina.code

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

class PingApp(context: Context = PingApp.createTestAppContext)
  extends BuiltInComponentsFromContext(context) {

  override def httpFilters = Nil
  override def router = Router.from { case GET(p"/ping") =>
    Action(Ok("pong"))
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

trait AppPerTest { self: FunSuite =>
  val app = FunFixture[PingApp](
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

trait ServerPerTest { self: FunSuite =>
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

trait AppPerSuite { self: Suite =>
  val pingApp: Fixture[PingApp] = new Fixture[PingApp]("ping-app") {
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
  def app = pingApp().application

  override def munitFixtures = Seq(pingApp)
}

trait ServerPerSuite { self: Suite =>
  val server: Fixture[RunningServer] = new Fixture[RunningServer]("ping-server") {
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

class AppTest extends FunSuite with AppPerTest {
  app.test("can make app request") { comps =>
    val req = FakeRequest("GET", "/ping")
    val res = await(route(comps.application, req).get)
    assert(res.header.status == 200)
  }
}

class ServerTest extends FunSuite with ServerPerTest {
  implicit val as: ActorSystem = ActorSystem("test")
  val http = AhcWSClient()

  server.test("server responds to ping") { server =>
    val port = server.endpoints.httpEndpoint.map(_.port).get
    val res = await(http.url(s"http://localhost:$port/ping").get())
    assert(res.status == 200)
  }
}

class AppTests extends FunSuite with AppPerSuite {
  test("request to ping address returns 200") {
    val req = FakeRequest("GET", "/ping")
    val res = await(route(app, req).get)
    assert(res.header.status == 200)
  }

  test("request to wrong address returns 404") {
    val req = FakeRequest("GET", "/nonexistent")
    val res = await(route(app, req).get)
    assert(res.header.status == 404)
  }
}

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
