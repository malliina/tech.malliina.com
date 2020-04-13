package com.malliina.content

import java.nio.file.Path

import com.malliina.http.FullUrl

object SEO {
  val sitemapPath = "sitemap.txt"

  def write(site: Seq[FullUrl], domain: FullUrl, dir: Path): Seq[Path] = {
    val sitemap = writeSitemap(site, dir)
    val robots = writeRobotsTxt(domain.withUri(s"/$sitemapPath"), dir)
    Seq(sitemap, robots)
  }

  def writeSitemap(uris: Seq[FullUrl], dir: Path) =
    FileIO.writeLines(uris.map(_.url), dir.resolve(sitemapPath))

  def writeRobotsTxt(sitemap: FullUrl, dir: Path) = {
    val robots = Seq("User-agent: *", "", s"Sitemap: $sitemap")
    FileIO.writeLines(robots, dir.resolve("robots.txt"))
  }
}
