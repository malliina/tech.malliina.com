import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.malliina.netlify.Netlify
import com.malliina.netlify.Netlify.SiteId
import com.malliina.rollup.CommonKeys.build
import com.malliina.values.{AccessToken, ErrorMessage}
import sbt.Keys.streams
import sbt.{AutoPlugin, Setting, settingKey, taskKey}

import java.nio.file.Path

object NetlifyApiPlugin extends AutoPlugin {
  object autoImport {
    val deployNetlify = taskKey[Unit]("Deploys the site")
    val netlifyRoot = settingKey[Path]("Netlify site root")
  }
  import autoImport.*

  override def projectSettings: Seq[Setting[?]] = Seq(
    deployNetlify := {
      val io = Netlify
        .deploy[IO](
          fs2.io.file.Path.fromNioPath(netlifyRoot.value),
          env[SiteId]("NETLIFY_SITE_ID"),
          env[AccessToken]("NETLIFY_AUTH_TOKEN")
        )
      val id = io.unsafeRunSync()
      streams.value.log.info(s"Deployment '$id' done.")
    },
    deployNetlify := deployNetlify.dependsOn(build).value
  )

  private def env[T](key: String)(implicit r: com.malliina.values.Readable[T]): T = {
    val result = for {
      str <- sys.env.get(key).toRight(ErrorMessage(s"Please define environment variable '$key'."))
      t <- r.read(str)
    } yield t
    result.fold(err => sys.error(err.message), identity)
  }
}
