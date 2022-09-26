import sbt.{File, settingKey, taskKey}

object GeneratorKeys {
  val build = taskKey[Unit]("Builds app")
  val siteDir = settingKey[File]("Site directory")
}
