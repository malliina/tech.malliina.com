package com.malliina.content

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}

import scala.collection.JavaConverters.asScalaIteratorConverter

object Generator {
  def main(args: Array[String]): Unit = run()

  def run() = {
    val current = Paths.get("target").resolve("docs")
    val ext = ".md"
    val mds =
      Files.list(current).iterator().asScala.filter(_.getFileName.toString.endsWith(ext)).toList
    mds.map { markdownFile =>
      val html = Markdown.toHtml(markdownFile)
      val noExt = markdownFile.getFileName.toString.dropRight(ext.length)
      val out = Paths.get("dist").resolve(s"$noExt.html")
      Pages.index(html).write(out)
    }
  }

  def fileToString(file: Path) = {
    val bytes = Files.readAllBytes(file)
    new String(bytes, StandardCharsets.UTF_8)
  }
}
