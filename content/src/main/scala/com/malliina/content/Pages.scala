package com.malliina.content

import com.malliina.content.Pages._
import com.malliina.http.FullUrl
import com.malliina.live.LiveReload
import com.malliina.assets.HashedAssets
import scalatags.Text.all._
import scalatags.text.Builder

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object Pages {
  def apply(local: Boolean): Pages = new Pages(local)

  val domain = FullUrl.https("tech.malliina.com", "")
  implicit val fullUrl: AttrValue[FullUrl] = attrType[FullUrl](_.url)

  val time = tag("time")
  val titleTag = tag("title")

  val datetime = attr("datetime")
  val property = attr("property")

  def attrType[T](stringify: T => String): AttrValue[T] = (t: Builder, a: Attr, v: T) =>
    t.setAttr(a.name, Builder.GenericAttrValueSource(stringify(v)))
}

class Pages(local: Boolean) {
  val listFile = "list.html"
  val remoteListUri = "list"
  val listUri = if (local) "list.html" else remoteListUri

  val globalDescription = "Posts on Scala, programming, and other tech topics."

  def page(title: String, url: FullUrl, content: Html): TagPage =
    index(title, url)(div(`class` := "content")(content), footer(a(href := listUri)("Archive")))

  def list(title: String, url: FullUrl, pages: Seq[MarkdownPage]) = index(title, url)(
    div(`class` := "content")(
      h1("Posts"),
      ul(
        pages.map { page =>
          val itemUri = if (local) page.name else page.noExt
          li(`class` := "post-item")(a(href := itemUri)(page.title), format(page.date))
        }
      )
    ),
    footer(
      a(href := "https://github.com/malliina")("GitHub"),
      a(href := "https://twitter.com/kungmalle")("Twitter")
    )
  )

  def index(titleText: String, url: FullUrl)(contents: Modifier*): TagPage = TagPage(
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
        meta(name := "twitter:card", content := "summary"),
        meta(name := "twitter:site", content := "@kungmalle"),
        meta(name := "twitter:creator", content := "@kungmalle"),
        meta(name := "og:image", content := (Pages.domain / findAsset("img/jag.jpg"))),
        meta(property := "og:title", content := titleText),
        meta(property := "og:description", content := globalDescription),
        link(rel := "canonical", href := url),
        link(
          rel := "shortcut icon",
          `type` := "image/jpeg",
          href := inlineOrAsset("img/jag.jpg")
        ),
        styleAt("fonts.css"),
        styleAt("styles.css"),
        if (local) script(src := LiveReload.script) else modifier()
      ),
      body(
        contents :+ scriptAt("frontend.js", defer)
      )
    )
  )

  def format(date: LocalDate) = {
    val localDate = DateTimeFormatter.ISO_LOCAL_DATE.format(date)
    time(datetime := localDate)(localDate)
  }

  def styleAt(file: String) =
    link(rel := "stylesheet", href := findAsset(file))

  def scriptAt(file: String, modifiers: Modifier*) = script(src := findAsset(file), modifiers)

  def inlineOrAsset(file: String) = HashedAssets.dataUris.getOrElse(file, findAsset(file))

  def findAsset(file: String): String =
    HashedAssets.assets.get(file).map(p => s"/$p").getOrElse(fail(s"Not found: '$file'."))

  def fail(message: String) = throw new Exception(message)
}
