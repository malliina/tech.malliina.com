scalaVersion := "2.12.16"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.9.3",
  "com.malliina" %% "primitives" % "3.4.0"
)

val utilsVersion = "1.6.5"

Seq(
  "com.typesafe.play" % "sbt-plugin" % "2.8.15",
  "com.malliina" % "sbt-utils-maven" % utilsVersion,
  "com.malliina" % "sbt-nodejs" % utilsVersion,
  "com.malliina" % "sbt-revolver-rollup" % utilsVersion,
  "org.scalameta" % "sbt-mdoc" % "2.3.2",
  "org.scalameta" % "sbt-scalafmt" % "2.4.6",
  "com.eed3si9n" % "sbt-buildinfo" % "0.11.0"
) map addSbtPlugin
