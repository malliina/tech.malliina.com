scalaVersion := "2.12.12"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.9.2",
  "com.malliina" %% "primitives" % "1.17.0"
)

Seq(
  "com.typesafe.play" % "sbt-plugin" % "2.8.7",
  "com.malliina" % "sbt-utils-maven" % "1.0.0",
  "org.scalameta" % "sbt-mdoc" % "2.2.16",
  "ch.epfl.scala" % "sbt-bloop" % "1.4.6",
  "org.scalameta" % "sbt-scalafmt" % "2.4.2"
) map addSbtPlugin
