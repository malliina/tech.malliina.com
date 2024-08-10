scalaVersion := "2.12.19"

val utilsVersion = "1.6.40"

Seq(
  "com.typesafe.play" % "sbt-plugin" % "2.9.2",
  "com.malliina" % "sbt-utils-maven" % utilsVersion,
  "com.malliina" % "sbt-nodejs" % utilsVersion,
  "com.malliina" % "sbt-revolver-rollup" % utilsVersion,
  "org.scalameta" % "sbt-mdoc" % "2.5.4",
  "org.scalameta" % "sbt-scalafmt" % "2.5.2",
  "com.eed3si9n" % "sbt-buildinfo" % "0.12.0"
) map addSbtPlugin
