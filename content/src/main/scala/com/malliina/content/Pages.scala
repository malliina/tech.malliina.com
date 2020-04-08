package com.malliina.content

import scalatags.Text.all._

object Pages extends Pages

class Pages {
  def index(content: Html): TagPage = TagPage(
    html(
      head(
        link(
          rel := "stylesheet",
          href := "https://fonts.googleapis.com/css?family=Open+Sans:400,600,300"
        ),
        styleAt("styles-fonts.css"),
        styleAt("styles-main.css"),
        scriptAt("main.js"),
        scriptAt("highlight.pack.js"),
        script("hljs.initHighlightingOnLoad();")
      ),
      body(
        content
      )
    )
  )

  def styleAt(file: String) = link(
    rel := "stylesheet",
    href := s"assets/css/$file"
  )

  def scriptAt(file: String) = script(src := s"assets/$file")
}
