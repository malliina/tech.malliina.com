crossScalaVersions := Seq("3.4.2", "2.12.20")

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "2.0.16",
  "co.fs2" %% "fs2-io" % "3.11.0",
  "com.malliina" %% "okclient-io" % "3.7.6",
  "commons-codec" % "commons-codec" % "1.18.0",
  "org.scalameta" %% "munit" % "1.1.0" % Test,
  "org.typelevel" %% "munit-cats-effect-3" % "1.0.7" % Test
)

//releaseCrossBuild := true

Global / onChangedBuildSource := ReloadOnSourceChanges
