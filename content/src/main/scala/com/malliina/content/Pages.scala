package com.malliina.content

import com.malliina.assets.{FileAssets, HashedAssets}
import com.malliina.content.Pages.{*, given}
import com.malliina.http.FullUrl
import com.malliina.live.LiveReload
import scalatags.Text.all.*
import scalatags.text.Builder

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object Pages:
  val domain = FullUrl.https("tech.malliina.com", "")
  given AttrValue[FullUrl] = attrType[FullUrl](_.url)

  val time = tag("time")
  val titleTag = tag("title")

  val datetime = attr("datetime")
  val property = attr("property")

  private def attrType[T](stringify: T => String): AttrValue[T] = (t: Builder, a: Attr, v: T) =>
    t.setAttr(a.name, Builder.GenericAttrValueSource(stringify(v)))

class Pages(local: Boolean):
  val listFile = "list.html"
  val remoteListUri = "list"
  val listUri = if local then "list.html" else remoteListUri

  private val scripts = Seq(FileAssets.main_js)

  private val globalDescription = "Posts on Scala, programming, and other tech topics."

  def page(title: String, cls: Option[String], url: FullUrl, content: Html): TagPage =
    val classes = Seq("content") ++ cls.toList
    index(title, url, Nil)(
      div(`class` := classes.mkString(" "))(content),
      footer(a(href := listUri)("Archive"))
    )

  def list(title: String, url: FullUrl, pages: Seq[MarkdownPage]) =
    val prefetches = pages.map(_.url)
    index(title, url, prefetches)(
      div(`class` := "content")(
        h1("Posts"),
        ul(
          pages.map: page =>
            val itemUri = if local then page.name else page.noExt
            li(`class` := "post-item")(
              a(href := itemUri)(page.title),
              format(page.date, page.updated)
            )
        )
      ),
      footer(
        a(href := "https://github.com/malliina")("GitHub")
      )
    )

  def index(titleText: String, url: FullUrl, prefetches: Seq[FullUrl])(
    contents: Modifier*
  ): TagPage = TagPage(
    html(lang := "en")(
      head(
        titleTag(titleText),
        meta(charset := "UTF-8"),
        meta(
          name := "viewport",
          content := "width=device-width, initial-scale=1.0"
        ),
        meta(name := "description", content := globalDescription),
        meta(
          name := "keywords",
          content := "Scala, sbt, code, programming, tech, frontend, backend"
        ),
        meta(name := "og:image", content := (Pages.domain / findAsset(FileAssets.img.jag_jpg))),
        meta(property := "og:title", content := titleText),
        meta(property := "og:description", content := globalDescription),
        link(rel := "canonical", href := url),
        prefetches.map: prefetch =>
          link(rel := "prefetch", href := prefetch),
        link(
          rel := "shortcut icon",
          `type` := "image/jpeg",
          href := inlineOrAsset(FileAssets.img.jag_jpg)
        ),
        styleAt(FileAssets.main_css),
        if local then script(src := LiveReload.script) else modifier()
      ),
      body(
        contents ++ scripts.map(file => scriptAt(file, defer))
      )
    )
  )

  def format(date: LocalDate, updated: Option[LocalDate]) =
    val localDate = DateTimeFormatter.ISO_LOCAL_DATE.format(date)
    val updateFormatted = updated.fold(modifier()): upd =>
      val updatedStr = DateTimeFormatter.ISO_LOCAL_DATE.format(upd)
      modifier(span(" updated "), time(datetime := updatedStr)(updatedStr))
    modifier(time(datetime := localDate)(localDate), updateFormatted)

  private def styleAt(file: String) =
    link(rel := "stylesheet", href := findAsset(file))

  private def scriptAt(file: String, modifiers: Modifier*) =
    script(src := findAsset(file), modifiers)

  private def inlineOrAsset(file: String) = HashedAssets.dataUris.getOrElse(file, findAsset(file))

  private def findAsset(file: String): String =
    HashedAssets.assets.get(file).map(p => s"/$p").getOrElse(fail(s"Not found: '$file'."))

  private def fail(message: String) = throw new Exception(message)
