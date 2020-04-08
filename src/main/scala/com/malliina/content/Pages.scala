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
        link(
          rel := "stylesheet",
          href := "https://fonts.googleapis.com/css2?family=Source+Code+Pro&display=swap"
        ),
        link(rel := "stylesheet", href := "css/site.css"),
        link(rel := "stylesheet", href := "css/monokai.css"),
        script(src := "js/highlight.pack.js"),
        script("hljs.initHighlightingOnLoad();")
      ),
      body(
        content
      )
    )
  )
}
