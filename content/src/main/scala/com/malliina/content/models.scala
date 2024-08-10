package com.malliina.content

import com.malliina.http.FullUrl
import com.typesafe.config.ConfigFactory
import scalatags.Text.all.*

import java.nio.file.{Files, Path}
import java.time.LocalDate
import scala.jdk.CollectionConverters.IteratorHasAsScala
import scala.language.implicitConversions
import scala.util.Try

case class SiteManifest(distDir: Path, docsDir: Path, local: Boolean)

case class Html(html: String) extends AnyVal

object Html:
  given Conversion[Html, RawFrag] = (html: Html) => RawFrag(html.html)

case class Markdown(content: String) extends AnyVal

case class PostMeta(title: String, cls: Option[String], date: LocalDate, updated: Option[LocalDate])

case class MarkdownPost(content: Markdown, meta: Option[PostMeta]):
  def title = meta.map(_.title).getOrElse("Tech")

object MarkdownPost:
  def apply(file: Path): MarkdownPost =
    val lines = Files.lines(file).iterator().asScala.toList
    val (header, content) =
      if lines.headOption.exists(l => l.trim == "---") then
        val (h, c) = lines.drop(1).span(l => l.trim != "---")
        (h, c.drop(1))
      else (Nil, lines)
    val config = ConfigFactory.parseString(header.mkString("\n"))
    def get(key: String) = Try(config.getString(key)).toOption
    val meta = for
      title <- get("title")
      date <- get("date").map(d => LocalDate.parse(d))
      updated = get("updated").map(u => LocalDate.parse(u))
    yield PostMeta(title, get("cls"), date, updated)
    MarkdownPost(Markdown(content.mkString("\n")), meta)

case class MarkdownPage(
  file: Path,
  title: String,
  url: FullUrl,
  date: LocalDate,
  updated: Option[LocalDate]
):
  val name = file.getFileName.toString
  val dotIdx = name.lastIndexOf(".")
  val noExt = if dotIdx == -1 then name else name.substring(0, dotIdx)
  val uri = s"/$noExt"
