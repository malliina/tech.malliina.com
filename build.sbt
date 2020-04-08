val scala212 = "2.12.10"
val scala213 = "2.13.1"

val npm = taskKey[NPM]("NPM interface")
val npmBuild = taskKey[Unit]("npm run build")

val docs = project
  .in(file("mdoc"))
  .enablePlugins(MdocPlugin)
  //  .dependsOn(content)
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
      "com.lihaoyi" %% "scalatags" % "0.8.5",
      "com.vladsch.flexmark" % "flexmark" % "0.40.34", // mdoc uses 0.40.34,
      "org.slf4j" % "slf4j-api" % "1.7.25",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "ch.qos.logback" % "logback-core" % "1.2.3"
    ),
    npm := new NPM(
      (baseDirectory in ThisBuild).value / "frontend",
      target.value,
      streams.value.log
    ),
    npmBuild := npm.value.build(),
    run := (run in Compile).dependsOn((mdoc in docs).toTask(""), npmBuild).evaluated
  )

val blog = project.in(file(".")).aggregate(docs, content)

Global / onChangedBuildSource := ReloadOnSourceChanges
