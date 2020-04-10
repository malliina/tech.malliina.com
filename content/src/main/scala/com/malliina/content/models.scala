package com.malliina.content

import java.nio.file.{Files, Path}
import java.time.LocalDate

import com.typesafe.config.ConfigFactory
import scalatags.Text.all._

import scala.collection.JavaConverters.asScalaIteratorConverter
import scala.language.implicitConversions
import scala.util.Try

case class Html(html: String) extends AnyVal

object Html {
  implicit def html(content: Html): RawFrag = RawFrag(content.html)
}

case class Markdown(content: String) extends AnyVal

case class PostMeta(title: String, date: LocalDate)

case class MarkdownPost(content: Markdown, meta: Option[PostMeta]) {
  def title = meta.map(_.title).getOrElse("Tech")
}

object MarkdownPost {
  def apply(file: Path): MarkdownPost = {
    val lines = Files.lines(file).iterator().asScala.toList
    val (header, content) = {
      if (lines.headOption.exists(l => l.trim == "---")) {
        val (h, c) = lines.drop(1).span(l => l.trim != "---")
        (h, c.drop(1))
      } else {
        (Nil, lines)
      }
    }
    val config = ConfigFactory.parseString(header.mkString("\n"))
    def get(key: String) = Try(config.getString(key)).toOption
    val meta = for {
      title <- get("title")
      date <- get("date").map(d => LocalDate.parse(d))
    } yield PostMeta(title, date)
    MarkdownPost(Markdown(content.mkString("\n")), meta)
  }
}

case class MarkdownPage(file: Path, title: String, date: LocalDate) {
  val name = file.getFileName.toString
  val dotIdx = name.lastIndexOf(".")
  val noExt = if (dotIdx == -1) name else name.substring(0, dotIdx)
  val uri = s"/$noExt"
}
