package com.malliina.content

import scalatags.Text.all._

case class Html(html: String)

object Html {
  implicit def html(content: Html): RawFrag = RawFrag(content.html)
}
