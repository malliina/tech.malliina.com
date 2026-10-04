import com.malliina.build.FileIO
import com.malliina.rollup.EsbuildPlugin.autoImport.npmRoot
import sbt.internal.FileChangesMacro.inputFiles
import java.nio.file.Path

val docsDir = settingKey[File]("Docs target dir")

inThisBuild(
  Seq(
    scalaVersion := versions.scala3
  )
)

val code = project
  .in(file("code"))
  .settings(
    scalaVersion := versions.scala213,
    libraryDependencies ++= Seq("ember-server", "dsl").map { m =>
      "org.http4s" %% s"http4s-$m" % versions.http4s
    } ++ Seq("doobie-core", "doobie-hikari").map { d =>
      "org.tpolecat" %% d % versions.doobie
    } ++ Seq(
      "com.typesafe.play" %% "play" % versions.play,
      "com.typesafe.play" %% "play-ahc-ws" % versions.play,
      "com.typesafe.play" %% "play-test" % versions.play % Test,
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
  .enablePlugins(MdocPlugin)
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
      "org.scala-js" %% "scalajs-dom" % versions.scalajsDom
    )
  )

val frontend = project
  .in(file("frontend"))
  .enablePlugins(NodeJsPlugin, EsbuildPlugin)
  .settings(
    scalaVersion := versions.scala3,
    copyHighlightScript := FileIO.copyIfChanged(
      (highlighter / Compile / fastLinkJSOutput).value.toPath.resolve("main.js"),
      (Compile / com.malliina.rollup.EsbuildPlugin.autoImport.npmRoot).value
        .resolve("highlighter.js")
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
    writeFileTree := Def.uncached(writeFileTree.dependsOn(frontend / build).value),
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
      "npmRoot" -> (frontend / com.malliina.rollup.EsbuildPlugin.autoImport.npmRoot).value.toFile
    ),
    watchMarkdown / fileInputs += (docs3 / mdocIn).value.toGlob / "*.md",
    watchMarkdown := Def.uncached(watchMarkdown.inputFiles),
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
