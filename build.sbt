import play.sbt.PlayImport

val scala212 = "2.12.16"
val scala213 = "2.13.10"
val scala3 = "3.2.1"

val frontendDirectory = settingKey[File]("frontend base dir")
ThisBuild / frontendDirectory := baseDirectory.value / "frontend"
val docsDir = settingKey[File]("Docs target dir")

val http4sModules = Seq("blaze-server", "dsl")

val code = project
  .in(file("code"))
  .enablePlugins(PlayScala)
  .settings(
    scalaVersion := scala213,
    libraryDependencies ++= http4sModules.map { m =>
      "org.http4s" %% s"http4s-$m" % "0.23.12"
    } ++ Seq("doobie-core", "doobie-hikari").map { d =>
      "org.tpolecat" %% d % "1.0.0-RC2"
    } ++ Seq(
      PlayImport.ws,
      "com.dimafeng" %% "testcontainers-scala-mysql" % "0.40.12" % Test,
      "org.scalameta" %% "munit" % "0.7.29" % Test
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )

val docs = project
  .in(file("mdoc"))
  .enablePlugins(MdocPlugin)
  .dependsOn(code, code % "compile->test")
  .settings(
    organization := "com.malliina",
    scalaVersion := scala213,
    publish / skip := true,
    mdocVariables := Map("NAME" -> name.value, "VERSION" -> version.value),
    mdocOut := (ThisBuild / baseDirectory).value / "target" / "docs"
  )

val frontend = project
  .in(file("frontend"))
  .enablePlugins(NodeJsPlugin, RollupPlugin)

val content = project
  .in(file("content"))
  .enablePlugins(NetlifyPlugin)
  .settings(
    scalajsProject := frontend,
    copyFolders += ((Compile / resourceDirectory).value / "public").toPath,
    crossScalaVersions := scala213 :: scala212 :: Nil,
    scalaVersion := scala212,
    libraryDependencies ++= Seq(
      "com.malliina" %% "primitives" % "3.4.0",
      "com.lihaoyi" %% "scalatags" % "0.12.0",
      "com.typesafe" % "config" % "1.4.2",
      "com.vladsch.flexmark" % "flexmark" % "0.64.0",
      "ch.qos.logback" % "logback-classic" % "1.4.5",
      "ch.qos.logback" % "logback-core" % "1.4.5"
    ),
    docsDir := (ThisBuild / baseDirectory).value / "target" / "docs",
    build := build.dependsOn((docs / mdoc).toTask("")).value,
    buildInfoKeys ++= Seq[BuildInfoKey](
      "docsDir" -> docsDir.value
    )
  )

val blog = project
  .in(file("."))
  .aggregate(docs, frontend, content)
  .settings(
    deploy := (content / deploy).value,
    build := (content / build).value
  )

Global / onChangedBuildSource := ReloadOnSourceChanges
