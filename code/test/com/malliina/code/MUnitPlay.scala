package com.malliina.code

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

class TestSuite extends FunSuite with PlayAppFixture {
  app.test("can make app request") { comps =>
    val req = FakeRequest("GET", "/ping")
    val res = await(route(comps.application, req).get)
    assert(res.header.status == 200)
  }
}

class TestServer extends FunSuite with PlayServerFixture {
  implicit val as = ActorSystem("test")
  val http = AhcWSClient()

  server.test("server responds to ping") { server =>
    val port = server.endpoints.httpEndpoint.map(_.port).get
    val res = await(http.url(s"http://localhost:$port/ping").get())
    assert(res.status == 200)
  }
}
