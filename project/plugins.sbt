scalaVersion := "2.12.16"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.9.3",
  "com.malliina" %% "primitives" % "3.2.0"
)

Seq(
  "com.typesafe.play" % "sbt-plugin" % "2.8.15",
  "com.malliina" % "sbt-utils-maven" % "1.2.14",
  "com.malliina" % "live-reload" % "0.3.1",
  "org.scalameta" % "sbt-mdoc" % "2.3.2",
  "org.scalameta" % "sbt-scalafmt" % "2.4.6",
  "com.eed3si9n" % "sbt-buildinfo" % "0.11.0"
) map addSbtPlugin
