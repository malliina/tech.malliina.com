import com.malliina.live.LiveReloadPlugin
import sbt.{AutoPlugin, Setting, settingKey, Plugins}
import sbtbuildinfo.BuildInfoPlugin

object GeneratorPlugin extends AutoPlugin {
  override def requires: Plugins = BuildInfoPlugin && LiveReloadPlugin
  object autoImport {
    val mode = settingKey[Mode]("Build mode, dev or prod")

    val DevMode = Mode.Dev
    val ProdMode = Mode.Prod
  }
  import autoImport._

  override def projectSettings: Seq[Setting[_]] = Seq(
    )
  override def globalSettings: Seq[Setting[_]] = Seq(
    mode := Mode.Dev
  )
}
