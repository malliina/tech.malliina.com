import play.sbt.PlayImport

val scala212 = "2.12.10"
val scala213 = "2.13.1"

val npm = taskKey[NPM]("NPM interface")
val npmBuild = taskKey[Unit]("npm run build")
val frontendDirectory = settingKey[File]("frontend base dir")
frontendDirectory in ThisBuild := baseDirectory.value / "frontend"
val cleanSite = taskKey[Unit]("Deletes the site dir")

val code = project
  .in(file("code"))
  .enablePlugins(PlayScala)
  .settings(
    scalaVersion := scala212,
    libraryDependencies ++= Seq(
      PlayImport.ws,
      "org.scalameta" %% "munit" % "0.7.1" % Test
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
    skip.in(publish) := true,
    mdocVariables := Map("NAME" -> name.value, "VERSION" -> version.value),
    mdocOut := (baseDirectory in ThisBuild).value / "target" / "docs"
  )

val content = project
  .in(file("content"))
  .settings(
    crossScalaVersions := scala213 :: scala212 :: Nil,
    scalaVersion := scala212,
    libraryDependencies ++= Seq(
      "com.malliina" %% "primitives" % "1.13.0",
      "com.lihaoyi" %% "scalatags" % "0.8.5",
      "com.vladsch.flexmark" % "flexmark" % "0.40.34", // mdoc uses 0.40.34,
      "org.slf4j" % "slf4j-api" % "1.7.25",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "ch.qos.logback" % "logback-core" % "1.2.3"
    ),
    npm := new NPM(
      (frontendDirectory in ThisBuild).value,
      target.value,
      streams.value.log
    ),
    npmBuild := npm.value.build(),
    watchSources := watchSources.value ++ Seq(
//      WatchSource((frontendDirectory in ThisBuild).value),
      WatchSource(
        (frontendDirectory in ThisBuild).value / "src",
        "*.ts" || "*.scss",
        HiddenFileFilter
      ),
      WatchSource((mdocIn in docs).value)
    ),
    cleanSite := FileIO.deleteDirectory((baseDirectory in ThisBuild).value / "target" / "site"),
    run := (run in Compile)
    //      .dependsOn((mdoc in docs).toTask(""), npmBuild.dependsOn(cleanSite))
      .dependsOn((mdoc in docs).toTask(""), npmBuild)
      .evaluated
  )

val blog = project.in(file(".")).aggregate(code, docs, content)

Global / onChangedBuildSource := ReloadOnSourceChanges
