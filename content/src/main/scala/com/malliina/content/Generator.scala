package com.malliina.content

import buildinfo.BuildInfo

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import com.malliina.content.netlify.{NetlifyClient, RedirectEntry}

import scala.collection.JavaConverters.asScalaIteratorConverter

case class SiteManifest(distDir: Path, docsDir: Path, local: Boolean)

object Generator {
  private val log = AppLogger(getClass)

  def main(args: Array[String]): Unit = {
    val manifest =
      SiteManifest(BuildInfo.siteDir.toPath, BuildInfo.docsDir.toPath, !BuildInfo.isProd)
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
    pages.list("Posts", listUrl, newestFirst).write(distDir.resolve(pages.listFile))
    SEO.write(markdownPages.map(_.url) :+ listUrl, domain, distDir)
    NetlifyClient.writeRedirects(Seq(RedirectEntry("/*", s"/${pages.listUri}", 302)), distDir)
    NetlifyClient.writeHeaders(distDir)
  }

  def fileToString(file: Path) = {
    val bytes = Files.readAllBytes(file)
    new String(bytes, StandardCharsets.UTF_8)
  }
}
