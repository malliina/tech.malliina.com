scalaVersion := "2.12.20"

val utilsVersion = "1.6.55"

lazy val root = project
  .in(file("."))
  .settings(
    Seq(
      "com.typesafe.play" % "sbt-plugin" % "2.9.2",
      "com.malliina" % "sbt-utils-maven" % utilsVersion,
      "com.malliina" % "sbt-nodejs" % utilsVersion,
      "com.malliina" % "sbt-revolver-rollup" % utilsVersion,
      "org.scalameta" % "sbt-mdoc" % "2.7.1",
      "org.scalameta" % "sbt-scalafmt" % "2.5.4",
      "com.eed3si9n" % "sbt-buildinfo" % "0.13.1"
    ) map addSbtPlugin
  )
