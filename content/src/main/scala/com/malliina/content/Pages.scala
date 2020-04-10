package com.malliina.content

import java.nio.file.{Files, Paths}
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import scalatags.Text.all._

import scala.collection.JavaConverters.asScalaIteratorConverter

object Pages {
  def apply(local: Boolean): Pages = new Pages(local)
}

class Pages(local: Boolean) {
  val listFile = "list.html"
  val listUri = if (local) "list.html" else "list"
  val time = tag("time")
  val titleTag = tag("title")
  val datetime = attr("datetime")

  def page(title: String, content: Html): TagPage =
    index(title)(div(`class` := "content")(content), footer(a(href := listUri)("Archive")))

  def list(title: String, pages: Seq[MarkdownPage]) = index(title)(
    div(`class` := "content")(
      h1("Posts"),
      ul(
        pages.map { page =>
          val itemUri = if (local) page.name else page.noExt
          li(`class` := "post-item")(a(href := itemUri)(page.title), format(page.date))
        }
      )
    )
  )

  def index(titleText: String)(content: Modifier*): TagPage = TagPage(
    html(
      head(
        titleTag(titleText),
        styleAt("styles-fonts.css"),
        styleAt("styles-main.css"),
        scriptAt("highlight.scala.js"),
        scriptAt("main.js")
      ),
      body(
        content: _*
      )
    )
  )

  def format(date: LocalDate) = {
    val localDate = DateTimeFormatter.ISO_LOCAL_DATE.format(date)
    time(datetime := localDate)(localDate)
  }

  def styleAt(file: String) =
    link(rel := "stylesheet", href := findAsset(s"css/$file"))

  def scriptAt(file: String) = script(src := findAsset(file))

  def findAsset(file: String): String = {
    val root = Paths.get("target").resolve("site")
    val path = root.resolve("assets").resolve(file)
    val dir = path.getParent
    val candidates = Files.list(dir).iterator().asScala.toList
    val lastSlash = file.lastIndexOf("/")
    val nameStart = if (lastSlash == -1) 0 else lastSlash + 1
    val name = file.substring(nameStart)
    val dotIdx = name.lastIndexOf(".")
    val noExt = name.substring(0, dotIdx)
    val ext = name.substring(dotIdx + 1)
    val result = candidates.filter { p =>
      val candidateName = p.getFileName.toString
      candidateName.startsWith(noExt) && candidateName.endsWith(ext)
    }.sortBy { p =>
      Files.getLastModifiedTime(p)
    }.reverse.headOption
    val found = result.getOrElse(
      fail(s"Not found: '$file'. Found ${candidates.mkString(", ")}.")
    )
    root.relativize(found).toString.replace("\\", "/")
  }

  def fail(message: String) = throw new Exception(message)
}
