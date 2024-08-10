import play.sbt.PlayImport

val scala213 = "2.13.14"
val scala3 = "3.4.2"

val docsDir = settingKey[File]("Docs target dir")

val code = project
  .in(file("code"))
  .enablePlugins(PlayScala)
  .settings(
    scalaVersion := scala213,
    libraryDependencies ++= Seq("ember-server", "dsl").map { m =>
      "org.http4s" %% s"http4s-$m" % "0.23.27"
    } ++ Seq("doobie-core", "doobie-hikari").map { d =>
      "org.tpolecat" %% d % "1.0.0-RC5"
    } ++ Seq(
      PlayImport.ws,
      "com.dimafeng" %% "testcontainers-scala-mysql" % "0.41.4" % Test,
      "org.scalameta" %% "munit" % "1.0.0" % Test
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )

val docs = project
  .in(file("mdoc"))
  .enablePlugins(MdocPlugin, PlayScala)
  .dependsOn(code, code % "compile->test")
  .settings(
    organization := "com.malliina",
    scalaVersion := scala213,
    publish / skip := true,
    mdocVariables := Map("NAME" -> name.value, "VERSION" -> version.value),
    mdocOut := (ThisBuild / baseDirectory).value / "target" / "docs"
  )

val docs3 = project
  .in(file("mdoc3"))
  .enablePlugins(MdocPlugin)
  .settings(
    organization := "com.malliina",
    scalaVersion := scala3,
    publish / skip := true,
    mdocVariables := Map("NAME" -> name.value, "VERSION" -> version.value),
    mdocIn := (ThisBuild / baseDirectory).value / "docs-scala3",
    mdocOut := (ThisBuild / baseDirectory).value / "target" / "docs"
  )

val frontend = project
  .in(file("frontend"))
  .enablePlugins(NodeJsPlugin, RollupPlugin)
  .settings(
    scalaVersion := scala3
  )

val content = project
  .in(file("content"))
  .enablePlugins(NetlifyPlugin)
  .settings(
    scalajsProject := frontend,
    copyFolders += ((Compile / resourceDirectory).value / "public").toPath,
    scalaVersion := scala3,
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.5.6",
      "com.malliina" %% "primitives" % "3.7.3",
      "com.lihaoyi" %% "scalatags" % "0.13.1",
      "com.typesafe" % "config" % "1.4.3",
      "com.vladsch.flexmark" % "flexmark" % "0.64.8"
    ),
    docsDir := (ThisBuild / baseDirectory).value / "target" / "docs",
    build := build.dependsOn((docs / mdoc).toTask(""), (docs3 / mdoc).toTask("")).value,
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
