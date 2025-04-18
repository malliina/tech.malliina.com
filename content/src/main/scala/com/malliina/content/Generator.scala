package com.malliina.content

import buildinfo.BuildInfo

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import com.malliina.content.netlify.{NetlifyClient, RedirectEntry}

import scala.jdk.CollectionConverters.IteratorHasAsScala

object Generator:
  def main(args: Array[String]): Unit =
    val manifest =
      SiteManifest(BuildInfo.siteDir.toPath, BuildInfo.docsDir.toPath, !BuildInfo.isProd)
    run(manifest)

  def run(manifest: SiteManifest): Path =
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
    val written = mds.map: markdownFile =>
      val post = MarkdownPost(markdownFile)
      val html = MarkdownConverter.toHtml(post.content)
      val noExt = markdownFile.getFileName.toString.dropRight(ext.length)
      val out = distDir.resolve(s"$noExt.html")
      val url = domain / noExt
      val page = pages.page(post.title, post.meta.flatMap(_.cls), url, html)
      val htmlFile = page.write(out)
      // Pages without a meta section will be included in the site but excluded from the list of pages
      val mdPage = post.meta.map: meta =>
        MarkdownPage(htmlFile, meta.title, url, meta.date, meta.updated)
      (htmlFile, mdPage)
    FileIO.writeLines(
      written.map((file, _) => file.toAbsolutePath.toString),
      BuildInfo.npmRoot.toPath.resolve("pages.txt")
    )
    // Overwrites the HTML with a highlighted version
    val highlighted = IO.run(s"npm run highlighter", cwd = BuildInfo.npmRoot.toPath)
    val markdownPages = written.flatMap((_, md) => md)
    val newestFirst = markdownPages.sortBy(_.date.toEpochDay).reverse
    val listUrl = domain / pages.remoteListUri
    val listPage = pages.list("Posts", listUrl, newestFirst)
    listPage.write(distDir.resolve(pages.listFile))
    if manifest.local then listPage.write(distDir.resolve("index.html"))
    SEO.write(markdownPages.map(_.url) :+ listUrl, domain, distDir)
    NetlifyClient.writeRedirects(Seq(RedirectEntry("/*", s"/${pages.listUri}", 302)), distDir)
    NetlifyClient.writeHeaders(distDir)

  def fileToString(file: Path) =
    val bytes = Files.readAllBytes(file)
    new String(bytes, StandardCharsets.UTF_8)
