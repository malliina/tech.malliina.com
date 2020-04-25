libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.8.1",
  "com.malliina" %% "primitives" % "1.13.0"
)

Seq(
  "com.typesafe.play" % "sbt-plugin" % "2.8.1",
  "com.malliina" % "sbt-utils-maven" % "0.16.1",
  "org.scalameta" % "sbt-mdoc" % "2.1.5",
  "ch.epfl.scala" % "sbt-bloop" % "1.4.0-RC1",
  "org.scalameta" % "sbt-scalafmt" % "2.3.0"
) map addSbtPlugin
