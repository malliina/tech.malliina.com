import com.malliina.build.FileIO
import play.sbt.PlayImport

import java.nio.file.Path

val versions = new {
  val scala213 = "2.13.16"
  val scala3 = "3.7.1"
  val config = "1.4.3"
  val doobie = "1.0.0-RC6"
  val flexmark = "0.64.8"
  val logback = "1.5.18"
  val http4s = "0.23.30"
  val munit = "1.1.1"
  val primitives = "3.7.10"
  val scalajsDom = "2.8.0"
  val scalatags = "0.13.1"
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

val highlighter = project
  .in(file("highlighter"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    scalaVersion := versions.scala3,
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % versions.scalajsDom
    )
  )

val frontend = project
  .in(file("frontend"))
  .enablePlugins(NodeJsPlugin, RollupPlugin)
  .settings(
    scalaVersion := versions.scala3,
    copyHighlightScript := FileIO.copyIfChanged(
      (highlighter / Compile / fastLinkJSOutput).value.toPath.resolve("main.js"),
      (Compile / npmRoot).value.resolve("highlighter.js")
    )
  )

val watchMarkdown = taskKey[Seq[Path]]("Lists files.")

val content = project
  .in(file("content"))
  .enablePlugins(NetlifyApiPlugin, GeneratorPlugin)
  .settings(
    scalajsProject := frontend,
    copyFolders += ((Compile / resourceDirectory).value / "public").toPath,
    scalaVersion := versions.scala3,
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % versions.logback,
      "com.malliina" %% "primitives" % versions.primitives,
      "com.lihaoyi" %% "scalatags" % versions.scalatags,
      "com.typesafe" % "config" % versions.config,
      "com.vladsch.flexmark" % "flexmark" % versions.flexmark
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
    build := build.triggeredBy(watchMarkdown).value
  )

val blog = project
  .in(file("."))
  .aggregate(docs, frontend, content)
  .settings(
    deploy := (content / deploy).value,
    build := (content / build).value
  )

Global / onChangedBuildSource := ReloadOnSourceChanges
