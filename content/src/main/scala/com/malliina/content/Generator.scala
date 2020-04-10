package com.malliina.content

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}

import com.malliina.content.netlify.{NetlifyClient, RedirectEntry}

import scala.collection.JavaConverters.asScalaIteratorConverter

object Generator {
  private val log = AppLogger(getClass)

  def main(args: Array[String]): Unit = run()

  def run() = {
    val targetDir = Paths.get("target")
    val distDir = targetDir.resolve("site")
    val current = targetDir.resolve("docs")
    val ext = ".md"
    val mds =
      Files.list(current).iterator().asScala.filter(_.getFileName.toString.endsWith(ext)).toList
    val pages = mds.flatMap { markdownFile =>
      val post = MarkdownPost(markdownFile)
      val html = MarkdownConverter.toHtml(post.content)
      val noExt = markdownFile.getFileName.toString.dropRight(ext.length)
      val out = distDir.resolve(s"$noExt.html")
      val htmlFile = Pages.page(post.title, html).write(out)
      post.meta.map { meta =>
        MarkdownPage(htmlFile, meta.title, meta.date)
      }
    }
    val newestFirst = pages.sortBy(_.date.toEpochDay).reverse
    Pages.list("Archive", newestFirst).write(distDir.resolve(Pages.listFile))
    newestFirst.headOption.foreach { newest =>
      NetlifyClient.writeRedirects(Seq(RedirectEntry("/*", newest.uri, 302)), distDir)
    }
    NetlifyClient.writeHeaders(distDir)
  }

  def fileToString(file: Path) = {
    val bytes = Files.readAllBytes(file)
    new String(bytes, StandardCharsets.UTF_8)
  }
}
