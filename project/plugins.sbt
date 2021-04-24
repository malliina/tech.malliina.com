scalaVersion := "2.12.13"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.9.2",
  "com.malliina" %% "primitives" % "1.17.0"
)

Seq(
  "com.typesafe.play" % "sbt-plugin" % "2.8.8",
  "com.malliina" % "sbt-utils-maven" % "1.2.3",
  "org.scalameta" % "sbt-mdoc" % "2.2.20",
  "ch.epfl.scala" % "sbt-bloop" % "1.4.8",
  "org.scalameta" % "sbt-scalafmt" % "2.4.2"
) map addSbtPlugin
