val utilsVersion = "2.0.10"

libraryDependencies ++= Seq(
  "com.malliina" %% "primitives" % "6.17.0",
  "com.malliina" %% "okclient-io" % "6.17.0"
)

Seq(
  "com.malliina" % "sbt-utils-maven" % utilsVersion,
  "com.malliina" % "sbt-nodejs" % utilsVersion,
  "com.malliina" % "sbt-revolver-rollup" % utilsVersion,
  "org.scalameta" % "sbt-mdoc" % "2.9.2",
  "org.scalameta" % "sbt-scalafmt" % "2.6.1",
  "com.eed3si9n" % "sbt-buildinfo" % "0.13.1"
) map addSbtPlugin
