import com.malliina.nodejs.IO
import com.malliina.rollup.CommonKeys.{build, isProd}
import com.malliina.rollup.GeneratorPlugin
import sbt.Keys.{baseDirectory, streams}
import sbt.{AutoPlugin, Plugins, Setting, ThisBuild, taskKey}

object NetlifyPlugin extends AutoPlugin {
  override def requires: Plugins = GeneratorPlugin

  object autoImport {
    val deploy = taskKey[Unit]("Deploys the site")
  }
  import autoImport.*

  override def projectSettings: Seq[Setting[?]] = Seq(
    deploy := {
      val cmd = Seq("netlify", "deploy")
      val params = if (isProd.value) List("--prod") else Nil
      IO.runProcessSync(
        cmd ++ params,
        (ThisBuild / baseDirectory).value,
        streams.value.log
      )
    },
    deploy := deploy.dependsOn(build).value
  )
}
