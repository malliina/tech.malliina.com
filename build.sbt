import java.nio.file.Path
import complete.DefaultParsers.spaceDelimited

import play.sbt.PlayImport

val scala212 = "2.12.15"
val scala213 = "2.13.8"

val npm = taskKey[NPM]("NPM interface")
val npmBuild = taskKey[Unit]("npm run build")
val npmKillNode = taskKey[Unit]("Kills node with force")
val frontendDirectory = settingKey[File]("frontend base dir")
ThisBuild / frontendDirectory := baseDirectory.value / "frontend"
val cleanSite = taskKey[Unit]("Empties the target site dir")
val cleanDocs = taskKey[Unit]("Empties the target docs dir")
val siteDir = settingKey[File]("Site target dir")
val docsDir = settingKey[File]("Docs target dir")
val prepDirs = taskKey[Unit]("Creates directories")
val writeManifest = inputKey[Path]("Writes the manifest file")
val build = taskKey[Unit]("Builds the site")
val deploy = inputKey[Unit]("Deploys the site")
val deployDraft = taskKey[Unit]("Deploys the draft site")
val deployProd = taskKey[Unit]("Deploys the prod site")
val Dev = config("dev")

val http4sModules = Seq("blaze-server", "dsl")

val code = project
  .in(file("code"))
  .enablePlugins(PlayScala)
  .settings(
    scalaVersion := scala212,
    libraryDependencies ++= http4sModules.map { m =>
      "org.http4s" %% s"http4s-$m" % "0.23.11"
    } ++ Seq("doobie-core", "doobie-hikari").map { d =>
      "org.tpolecat" %% d % "1.0.0-RC2"
    } ++ Seq(
      PlayImport.ws,
      "com.dimafeng" %% "testcontainers-scala-mysql" % "0.40.5" % Test,
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
    scalaVersion := scala212,
    crossScalaVersions -= scala213,
    publish / skip := true,
    mdocVariables := Map("NAME" -> name.value, "VERSION" -> version.value),
    mdocOut := (ThisBuild / baseDirectory).value / "target" / "docs"
  )

val content = project
  .in(file("content"))
  .enablePlugins(LiveReloadPlugin)
  .settings(
    crossScalaVersions := scala213 :: scala212 :: Nil,
    scalaVersion := scala212,
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-json" % "2.9.2",
      "com.malliina" %% "primitives" % "3.1.3",
      "com.lihaoyi" %% "scalatags" % "0.11.1",
      "com.typesafe" % "config" % "1.4.2",
      "com.vladsch.flexmark" % "flexmark" % "0.64.0",
      "org.slf4j" % "slf4j-api" % "1.7.36",
      "ch.qos.logback" % "logback-classic" % "1.2.11",
      "ch.qos.logback" % "logback-core" % "1.2.11"
    ),
    npm := new NPM(
      (ThisBuild / frontendDirectory).value,
      target.value,
      streams.value.log
    ),
    npmBuild := npm.value.build(),
    watchSources := watchSources.value ++ Seq(
      WatchSource(
        (ThisBuild / frontendDirectory).value / "src",
        "*.ts" || "*.scss",
        HiddenFileFilter
      ),
      WatchSource((docs / mdocIn).value)
    ),
    siteDir := (ThisBuild / baseDirectory).value / "target" / "site",
    liveReloadRoot := siteDir.value.toPath,
    refreshBrowsers := refreshBrowsers.triggeredBy(Dev / build).value,
    cleanSite := FileIO.deleteDirectory(siteDir.value),
    docsDir := (ThisBuild / baseDirectory).value / "target" / "docs",
    cleanDocs := FileIO.deleteDirectory(docsDir.value),
    prepDirs := {
      // Ownership problems if webpack generates these, apparently
      val assetsDir = siteDir.value / "assets"
      Seq(assetsDir / "css", assetsDir / "fonts").map(_.mkdirs())
    },
    npmKillNode := npm.value.stop(),
    writeManifest := {
      val args: Seq[String] = spaceDelimited("<arg>").parsed
      FileIO.writeJson(
        SiteManifest(
          siteDir.value.toPath,
          docsDir.value.toPath,
          local = args.map(_.trim).contains("dev")
        ),
        (target.value / "manifest.json").toPath
      )
    },
    Dev / build := Def.taskDyn {
      (Compile / run)
        .dependsOn((docs / mdoc).toTask(""), npmBuild)
        .dependsOn(prepDirs)
        .toTask(s" ${writeManifest.toTask(" dev").value}")
        .dependsOn(Def.task(reloader.value.start()))
    }.value,
    build := Def.taskDyn {
      (Compile / run)
        .dependsOn((docs / mdoc).toTask(""), npmBuild)
        .dependsOn(prepDirs)
        .dependsOn(cleanSite, cleanDocs)
        .toTask(s" ${writeManifest.toTask(" prod").value}")
    }.value,
    deploy := {
      val args = spaceDelimited("<arg>").parsed
      NPM
        .runProcessSync(
          args.mkString(" "),
          (ThisBuild / baseDirectory).value,
          streams.value.log
        )
    },
    deploy := deploy.dependsOn(build).evaluated,
    deployProd := deploy.toTask(" netlify deploy --prod").value,
    deployDraft := deploy.toTask(" netlify deploy").value
  )

val blog = project
  .in(file("."))
  .aggregate(code, docs, content)
  .settings(
    deployProd := (content / deployProd).value,
    build := (content / build).value
  )

Global / onChangedBuildSource := ReloadOnSourceChanges
