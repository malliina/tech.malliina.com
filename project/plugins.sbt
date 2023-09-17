scalaVersion := "2.12.16"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.9.4",
  "com.malliina" %% "primitives" % "3.4.5"
)

val utilsVersion = "1.6.19"

Seq(
  "com.typesafe.play" % "sbt-plugin" % "2.8.20",
  "com.malliina" % "sbt-utils-maven" % utilsVersion,
  "com.malliina" % "sbt-nodejs" % utilsVersion,
  "com.malliina" % "sbt-revolver-rollup" % utilsVersion,
  "org.scalameta" % "sbt-mdoc" % "2.3.7",
  "org.scalameta" % "sbt-scalafmt" % "2.5.0",
  "com.eed3si9n" % "sbt-buildinfo" % "0.11.0"
) map addSbtPlugin
