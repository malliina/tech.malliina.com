import com.malliina.live.LiveReloadPlugin
import com.malliina.live.LiveReloadPlugin.autoImport.liveReloadRoot
import sbt.{AutoPlugin, Global, Plugins, Setting, settingKey}
import sbtbuildinfo.BuildInfoPlugin
import sbtbuildinfo.BuildInfoPlugin.autoImport.{BuildInfoKey, buildInfoKeys}

object GeneratorPlugin extends AutoPlugin {
  override def requires: Plugins = BuildInfoPlugin && LiveReloadPlugin
  object autoImport {
    val mode = settingKey[Mode]("Build mode, dev or prod")
    val isProd = settingKey[Boolean]("true if in prod mode, false otherwise")

    val DevMode = Mode.Dev
    val ProdMode = Mode.Prod
  }
  import autoImport._
  import GeneratorKeys._

  override def projectSettings: Seq[Setting[_]] = Seq(
    isProd := ((Global / mode).value == Mode.Prod),
    liveReloadRoot := siteDir.value.toPath,
    buildInfoKeys ++= Seq[BuildInfoKey](
      "siteDir" -> siteDir.value,
      "isProd" -> isProd.value,
      "mode" -> (Global / mode).value
    )
  )
  override def globalSettings: Seq[Setting[_]] = Seq(
    mode := Mode.Dev
  )
}
