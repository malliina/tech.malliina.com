scalaVersion := "2.12.15"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.9.2",
  "com.malliina" %% "primitives" % "3.1.3"
)

Seq(
  "com.typesafe.play" % "sbt-plugin" % "2.8.15",
  "com.malliina" % "sbt-utils-maven" % "1.2.14",
  "com.malliina" % "live-reload" % "0.3.1",
  "org.scalameta" % "sbt-mdoc" % "2.3.2",
  "org.scalameta" % "sbt-scalafmt" % "2.4.6"
) map addSbtPlugin
