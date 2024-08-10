---
title: MUnit with http4s
date: 2021-01-29
updated: 2024-08-10
---
# MUnit with http4s

Write integration tests to prevent regressions in your code. [MUnit](https://scalameta.org/munit/ "MUnit website") is a 
Scala testing library. [Http4s](https://http4s.org/ "http4s website") is a web framework for Scala.  This post demonstrates how 
to write integration tests for http4s web services using MUnit.

```scala mdoc:invisible
import cats.data.Kleisli
import cats.effect.unsafe.implicits.global
import cats.effect.{ExitCode, IO, IOApp, Resource}
import com.comcast.ip4s.IpLiteralSyntax
import cats.syntax.flatMap._
import com.dimafeng.testcontainers.MySQLContainer
import doobie.hikari._
import doobie.util.ExecutionContexts
import munit.{FunSuite, Suite}
import org.http4s.ember.server.EmberServerBuilder
import org.testcontainers.utility.DockerImageName
import org.http4s._
import org.http4s.implicits._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.ember.server.EmberServerBuilder
import scala.concurrent.{ExecutionContext, Promise}
import scala.concurrent.duration.DurationInt
```

## The app

Consider the following http4s web service:

```scala mdoc:silent
class AppService extends Http4sDsl[IO] {
  val routes = HttpRoutes.of[IO] {
    case GET -> Root / "ping" => Ok("pong")
  }.orNotFound
}

object AppServer extends IOApp {
  type Routes = Kleisli[IO, Request[IO], Response[IO]]
  val app: Routes = new AppService().routes
  val server = EmberServerBuilder
    .default[IO]
    .withHost(host"0.0.0.0")
    .withPort(port"9000")
    .withHttpApp(app)
    .build
  override def run(args: List[String]): IO[ExitCode] = 
    server.use(_ => IO.never).as(ExitCode.Success)
}
```

## One app per test case

We can test this minimal app in a MUnit test suite as follows:

```scala mdoc:silent
class AppServiceTests extends FunSuite {
  val app = FunFixture[AppServer.Routes](
    setup = { test =>
      AppServer.app
    },
    teardown = { file =>
      // Always gets called, even if test failed.
    }
  )
  
  app.test("ping app") { routes =>
    val response = routes.run(Request(uri = uri"/ping")).unsafeRunSync()
    assertEquals(response.status, Status.Ok)
  }
}
```

## One app per suite

To share the app instance between all tests in the suite, define this trait:

```scala mdoc:silent
trait Http4sSuite { self: Suite =>
  val httpApp: Fixture[AppServer.Routes] = new Fixture[AppServer.Routes]("app") {
    private var service: Option[AppServer.Routes] = None
    override def apply(): AppServer.Routes = service.get
    override def beforeAll(): Unit = {
      service = Option(AppServer.app)
    }
  }

  override def munitFixtures: Seq[Fixture[_]] = Seq(httpApp)
}
```

Use it in your test suite:

```scala mdoc:silent
class OneAppPerSuite extends FunSuite with Http4sSuite {
  test("run app") {
    val routes = httpApp()
    val response = routes.run(Request(uri = uri"/ping")).unsafeRunSync()
    assertEquals(response.status, Status.Ok)
  }
}
```

## Adding a database

It is common to interface with a database in a backend environment, such that a database connection pool is opened on app 
startup and closed on app shutdown. In http4s you model such a pattern using a 
[cats.effect.Resource](https://typelevel.org/cats-effect/datatypes/resource.html). Setting up http4s 
and test suites gets a bit more involved, so in the following we will:

1. Modify the initial example http4s service so that it supports a database resource.
1. Define a MUnit suite that manages a database instance for testing purposes.
1. Define another suite that provides the test database instance to our http4s service.

First, our app that uses a database now looks like this:

```scala mdoc:silent
case class DatabaseConf(url: String, user: String, pass: String)

class DatabaseService(database: HikariTransactor[IO]) extends Http4sDsl[IO] {
  val routes = HttpRoutes
    .of[IO] {
      case GET -> Root / "ping" => Ok("pong")
    }
    .orNotFound
}

object DatabaseApp extends IOApp {
  def appResource(conf: DatabaseConf): Resource[IO, DatabaseService] =
    for {
      ce <- ExecutionContexts.fixedThreadPool[IO](32)
      transactor <- HikariTransactor.newHikariTransactor[IO](
        "com.mysql.jdbc.Driver",
        conf.url,
        conf.user,
        conf.pass,
        ce
      )
    } yield new DatabaseService(transactor)

  def buildServer(conf: DatabaseConf) = for {
    app <- appResource(conf)
    server <- EmberServerBuilder
      .default[IO]
      .withHost(host"0.0.0.0")
      .withPort(port"9000")
      .withHttpApp(app.routes)
      .withShutdownTimeout(1.millis)
      .build
  } yield server

  override def run(args: List[String]): IO[ExitCode] =
    buildServer(readConf).use(_ => IO.never).as(ExitCode.Success)

  def readConf: DatabaseConf = ???
}

```

(You might read the database configuration from environment variables in a production setting, but I leave that up to you.)

To obtain a database for testing purposes, we use 
[testcontainers-scala](https://github.com/testcontainers/testcontainers-scala "testcontainers-scala GitHub"). The 
following MUnit suite utilizes [Docker](https://www.docker.com/ "Docker website") to start and stop a database once 
for all tests in your suite:

```scala mdoc:silent
trait DatabaseSuite { self: Suite =>
  val db: Fixture[DatabaseConf] = new Fixture[DatabaseConf]("database") {
    var container: Option[MySQLContainer] = None
    var conf: Option[DatabaseConf] = None
    def apply(): DatabaseConf = conf.get
    override def beforeAll(): Unit = {
      val image = DockerImageName.parse("mysql:5.7.29")
      val cont = MySQLContainer(mysqlImageVersion = image)
      cont.start()
      container = Option(cont)
      val databaseConf = DatabaseConf(
        s"${cont.jdbcUrl}?useSSL=false",
        cont.username,
        cont.password
      )
      conf = Option(databaseConf)
    }
    override def afterAll(): Unit = {
      container.foreach(_.stop())
    }
  }

  override def munitFixtures: Seq[Fixture[_]] = Seq(db)
}
```

Now we create a master test suite that starts a database and launches an http4s service that uses the database:

```scala mdoc:silent
trait DatabaseAppSuite extends DatabaseSuite { self: Suite =>
  val dbApp: Fixture[DatabaseService] = new Fixture[DatabaseService]("db-app") {
    private var service: Option[DatabaseService] = None
    val promise = Promise[IO[Unit]]()

    override def apply(): DatabaseService = service.get

    override def beforeAll(): Unit = {
      val resource = DatabaseApp.appResource(db())
      val resourceEffect = resource.allocated[DatabaseService]
      val setupEffect =
        resourceEffect.map {
          case (t, release) =>
            promise.success(release)
            t
        }.flatTap(t => IO.pure(()))

      service = Option(setupEffect.unsafeRunSync())
    }

    override def afterAll(): Unit = {
      IO.fromFuture(IO(promise.future)).flatten.unsafeRunSync()
    }
  }

  override def munitFixtures: Seq[Fixture[_]] = Seq(db, dbApp)
}
```

Note that the fixtures are initialized in the order listed in `munitFixtures`. You want your database to be available 
when your application starts, therefore the database fixture must precede the application fixture. Finally, let's write 
the tests:

```scala mdoc:silent
class DatabaseAppTests extends FunSuite with DatabaseAppSuite {
  test("test app") {
    val service = dbApp()
    val request = Request[IO](uri = uri"/ping")
    val response = service.routes.run(request).unsafeRunSync()
    assertEquals(response.status, Status.Ok)
  }
}
```

The code for this post is available on [GitHub](https://github.com/malliina/tech.malliina.com).
