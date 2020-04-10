package com.malliina.content

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}

import com.malliina.content.netlify.NetlifyClient

import scala.collection.JavaConverters.asScalaIteratorConverter

object Generator {
  def main(args: Array[String]): Unit = run()

  def run(): Seq[Path] = {
    val targetDir = Paths.get("target")
    val distDir = targetDir.resolve("site")
    val current = targetDir.resolve("docs")
    val ext = ".md"
    val mds =
      Files.list(current).iterator().asScala.filter(_.getFileName.toString.endsWith(ext)).toList
    val writtenHtml = mds.map { markdownFile =>
      val html = Markdown.toHtml(markdownFile)
      val noExt = markdownFile.getFileName.toString.dropRight(ext.length)
      val out = distDir.resolve(s"$noExt.html")
      Pages.index(html).write(out)
    }
    val headersFile = NetlifyClient.writeHeaders(distDir)
    writtenHtml :+ headersFile
  }

  def fileToString(file: Path) = {
    val bytes = Files.readAllBytes(file)
    new String(bytes, StandardCharsets.UTF_8)
  }
}
