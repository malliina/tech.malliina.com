package com.malliina.content.netlify

import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{FileVisitResult, Files, Path, Paths, SimpleFileVisitor}

import scala.collection.JavaConverters.asScalaIteratorConverter
import com.malliina.content.{CacheControl, CacheControls, FileIO, IO}

case class NetlifyHeader(path: String, headers: Map[String, String]) {
  // https://docs.netlify.com/routing/headers/#syntax-for-the-headers-file
  def asString: String = {
    val headerList = headers.map { case (k, v) => s"$k: $v" }
      .mkString("\n  ", "\n  ", "\n")
    s"$path$headerList"
  }
}

object NetlifyHeader {
  def forall(headers: Map[String, String]) = apply("/*", headers)

  def security = forall(
    Map("X-Frame-Options" -> "DENY", "X-XSS-Protection" -> "1; mode=block")
  )
}

case class WebsiteFile(file: Path, cacheControl: CacheControl) {
  def uri = file.toString.replace("\\", "/")
}

object NetlifyClient extends NetlifyClient

class NetlifyClient {
  def deploy() = IO.run("netlify deploy --prod")

  def writeHeaders(dir: Path): Path =
    writeHeadersFile(cached(dir), dir.resolve("_headers"))

  def cached(dir: Path): Seq[WebsiteFile] = {
    Files.walk(dir).iterator().asScala.toList.map { p =>
      val relative = dir.relativize(p)
      val cache =
        if (relative.startsWith(Paths.get("assets"))) CacheControls.eternalCache
        else CacheControls.defaultCacheControl
      WebsiteFile(relative, cache)
    }
  }

  private def writeHeadersFile(files: Seq[WebsiteFile], to: Path): Path = {
    val netlifyHeaders = NetlifyHeader.security +: files.map { file =>
      NetlifyHeader(
        s"/${file.uri}",
        Map(
          CacheControl.headerName -> file.cacheControl.value
//          "Content-Type" -> file.contentType.value
        )
      )
    }
    FileIO.write(
      netlifyHeaders.map(_.asString).mkString.getBytes(StandardCharsets.UTF_8),
      to
    )
  }
}
