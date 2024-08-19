package com.malliina.code

import cats.effect.unsafe.implicits.global
import cats.effect.{ExitCode, IO, IOApp, Resource}
import com.comcast.ip4s.{host, port}
import com.dimafeng.testcontainers.MySQLContainer
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import munit.{FunSuite, Suite}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.ember.server.EmberServerBuilder
import org.testcontainers.utility.DockerImageName

import scala.concurrent.Promise
import scala.concurrent.duration.DurationInt

case class DatabaseConf(url: String, user: String, pass: String)

class AppService extends Http4sDsl[IO]:
  val routes = HttpRoutes
    .of[IO]:
      case GET -> Root / "ping" => Ok("pong")
    .orNotFound

class DatabaseService(database: HikariTransactor[IO]) extends Http4sDsl[IO]:
  val routes = HttpRoutes
    .of[IO]:
      case GET -> Root / "ping" =>
        Ok("pong")
    .orNotFound

object DatabaseApp extends IOApp:
  def appResource(conf: DatabaseConf): Resource[IO, DatabaseService] =
    for
      ce <- ExecutionContexts.fixedThreadPool[IO](32)
      transactor <- HikariTransactor.newHikariTransactor[IO](
        "com.mysql.jdbc.Driver",
        conf.url,
        conf.user,
        conf.pass,
        ce
      )
    yield DatabaseService(transactor)

  def buildServer(conf: DatabaseConf) = for
    app <- appResource(conf)
    server <- EmberServerBuilder
      .default[IO]
      .withHost(host"0.0.0.0")
      .withPort(port"9000")
      .withHttpApp(app.routes)
      .withShutdownTimeout(1.millis)
      .build
  yield server

  override def run(args: List[String]): IO[ExitCode] =
    buildServer(readConf).use(_ => IO.never).as(ExitCode.Success)

  def readConf: DatabaseConf = ???

class AppServiceTests extends FunSuite:
  val app = FunFixture[String](
    setup = t => "ok",
    teardown = str =>
      // Testing
      ()
  )

trait DatabaseSuite:
  self: Suite =>
  val db: Fixture[DatabaseConf] = new Fixture[DatabaseConf]("database"):
    var container: Option[MySQLContainer] = None
    var conf: Option[DatabaseConf] = None
    def apply(): DatabaseConf = conf.get
    override def beforeAll(): Unit =
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
    override def afterAll(): Unit =
      container.foreach(_.stop())

  override def munitFixtures: Seq[Fixture[?]] = Seq(db)

trait DatabaseAppSuite extends DatabaseSuite:
  self: Suite =>
  val dbApp: Fixture[DatabaseService] = new Fixture[DatabaseService]("db-app"):
    private var service: Option[DatabaseService] = None
    val promise = Promise[IO[Unit]]()

    override def apply(): DatabaseService = service.get

    override def beforeAll(): Unit =
      val resource = DatabaseApp.appResource(db())
      val resourceEffect = resource.allocated[DatabaseService]
      val setupEffect =
        resourceEffect
          .map:
            case (t, release) =>
              promise.success(release)
              t
          .flatTap(t => IO.pure(()))

      service = Option(setupEffect.unsafeRunSync())

    override def afterAll(): Unit =
      IO.fromFuture(IO(promise.future)).flatten.unsafeRunSync()

  override def munitFixtures: Seq[Fixture[?]] = Seq(db, dbApp)
