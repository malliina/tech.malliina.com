package com.malliina.content

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}

import com.malliina.content.netlify.{NetlifyClient, RedirectEntry}
import play.api.libs.json.Json

import scala.collection.JavaConverters.asScalaIteratorConverter

case class SiteManifest(distDir: Path, docsDir: Path, local: Boolean)

object SiteManifest {
  implicit val pf = Formats.pathFormat
  implicit val json = Json.format[SiteManifest]
}

object Generator {
  private val log = AppLogger(getClass)

  def main(args: Array[String]): Unit = {
    val manifestPath = Paths.get(args(0))
    val manifest = Json.parse(Files.readAllBytes(manifestPath)).as[SiteManifest]
    run(manifest)
  }

  def run(manifest: SiteManifest): Path = {
    val pages = Pages(manifest.local)
    val distDir = manifest.distDir
    val ext = ".md"
    val mds =
      Files
        .list(manifest.docsDir)
        .iterator()
        .asScala
        .filter(_.getFileName.toString.endsWith(ext))
        .toList
    val domain = Pages.domain
    // Only includes markdown files with metadata
    val markdownPages = mds.flatMap { markdownFile =>
      val post = MarkdownPost(markdownFile)
      val html = MarkdownConverter.toHtml(post.content)
      val noExt = markdownFile.getFileName.toString.dropRight(ext.length)
      val out = distDir.resolve(s"$noExt.html")
      val url = domain / noExt
      val htmlFile = pages.page(post.title, url, html).write(out)
      post.meta.map { meta =>
        MarkdownPage(htmlFile, meta.title, url, meta.date)
      }
    }
    val newestFirst = markdownPages.sortBy(_.date.toEpochDay).reverse
    val listUrl = domain / pages.remoteListUri
    pages.list("Archive", listUrl, newestFirst).write(distDir.resolve(pages.listFile))
    SEO.write(markdownPages.map(_.url) :+ listUrl, domain, distDir)
    NetlifyClient.writeRedirects(Seq(RedirectEntry("/*", pages.listUri, 302)), distDir)
    NetlifyClient.writeHeaders(distDir)
  }

  def fileToString(file: Path) = {
    val bytes = Files.readAllBytes(file)
    new String(bytes, StandardCharsets.UTF_8)
  }
}
