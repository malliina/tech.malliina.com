import com.malliina.build.FileIO
import play.sbt.PlayImport

import java.nio.file.Path

val versions = new {
  val scala213 = "2.13.16"
  val scala3 = "3.6.2"
  val doobie = "1.0.0-RC6"
  val logback = "1.5.18"
  val http4s = "0.23.30"
  val munit = "1.1.0"
  val testContainers = "0.43.0"
}

val docsDir = settingKey[File]("Docs target dir")

val code = project
  .in(file("code"))
  .enablePlugins(PlayScala)
  .settings(
    scalaVersion := versions.scala213,
    libraryDependencies ++= Seq("ember-server", "dsl").map { m =>
      "org.http4s" %% s"http4s-$m" % versions.http4s
    } ++ Seq("doobie-core", "doobie-hikari").map { d =>
      "org.tpolecat" %% d % versions.doobie
    } ++ Seq(
      PlayImport.ws,
      "com.dimafeng" %% "testcontainers-scala-mysql" % versions.testContainers % Test,
      "org.scalameta" %% "munit" % versions.munit % Test
    )
  )

val code3 = project
  .in(file("code3"))
  .settings(
    scalaVersion := versions.scala3,
    libraryDependencies ++= Seq("ember-server", "dsl").map { m =>
      "org.http4s" %% s"http4s-$m" % versions.http4s
    } ++ Seq("doobie-core", "doobie-hikari").map { d =>
      "org.tpolecat" %% d % versions.doobie
    } ++ Seq(
      "ch.qos.logback" % "logback-classic" % versions.logback,
      "com.dimafeng" %% "testcontainers-scala-mysql" % versions.testContainers % Test,
      "org.scalameta" %% "munit" % versions.munit % Test
    )
  )

val docs = project
  .in(file("mdoc"))
  .enablePlugins(MdocPlugin, PlayScala)
  .dependsOn(code, code % "compile->test")
  .settings(
    organization := "com.malliina",
    scalaVersion := versions.scala213,
    publish / skip := true,
    mdocVariables := Map("NAME" -> name.value, "VERSION" -> version.value),
    mdocOut := (ThisBuild / baseDirectory).value / "target" / "docs"
  )

val docs3 = project
  .in(file("mdoc3"))
  .enablePlugins(MdocPlugin)
  .dependsOn(code3, code3 % "compile->test")
  .settings(
    organization := "com.malliina",
    scalaVersion := versions.scala3,
    publish / skip := true,
    mdocVariables := Map("NAME" -> name.value, "VERSION" -> version.value),
    mdocIn := (ThisBuild / baseDirectory).value / "docs-scala3",
    mdocOut := (ThisBuild / baseDirectory).value / "target" / "docs"
  )

val copyHighlightScript = taskKey[Boolean]("Copies file")

val frontend = project
  .in(file("frontend"))
  .enablePlugins(NodeJsPlugin, RollupPlugin)
  .settings(
    scalaVersion := versions.scala3,
    copyHighlightScript := FileIO.copyIfChanged(
      (Compile / resourceDirectory).value.toPath.resolve("highlight.js"),
      (Compile / npmRoot).value.resolve("highlight.js")
    )
  )

val watchMarkdown = taskKey[Seq[Path]]("Lists files.")
val highlight = taskKey[Unit]("Highlights generated HTML")

val content = project
  .in(file("content"))
  .enablePlugins(NetlifyApiPlugin, GeneratorPlugin)
  .settings(
    scalajsProject := frontend,
    copyFolders += ((Compile / resourceDirectory).value / "public").toPath,
    scalaVersion := versions.scala3,
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % versions.logback,
      "com.malliina" %% "primitives" % "3.7.7",
      "com.lihaoyi" %% "scalatags" % "0.13.1",
      "com.typesafe" % "config" % "1.4.3",
      "com.vladsch.flexmark" % "flexmark" % "0.64.8"
    ),
    docsDir := (ThisBuild / baseDirectory).value / "target" / "docs",
    build := build
      .dependsOn(
        (docs / mdoc).toTask(""),
        (docs3 / mdoc).toTask(""),
        frontend / copyHighlightScript
      )
      .value,
    build / fileInputs ++= Seq((docs3 / mdocIn).value, (docs / mdocIn).value)
      .map(d => d.toGlob / "*.md"),
    buildInfoKeys ++= Seq[BuildInfoKey](
      "docsDir" -> docsDir.value,
      "npmRoot" -> (frontend / npmRoot).value.toFile
    ),
    watchMarkdown / fileInputs += (docs3 / mdocIn).value.toGlob / "*.md",
    watchMarkdown := watchMarkdown.inputFiles,
    build := build.triggeredBy(watchMarkdown).value,
    highlight :=
      RollupPlugin
        .process(Seq("npm", "run", "highlight"), (frontend / npmRoot).value, streams.value.log)
  )

val blog = project
  .in(file("."))
  .aggregate(docs, frontend, content)
  .settings(
    deploy := (content / deploy).value,
    build := (content / build).value
  )

Global / onChangedBuildSource := ReloadOnSourceChanges
