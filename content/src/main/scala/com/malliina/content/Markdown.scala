package com.malliina.content

import java.nio.file.Path

import com.malliina.content.Generator.fileToString
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.options.MutableDataSet

object Markdown extends Markdown

class Markdown {
  val options = new MutableDataSet
  // uncomment to set optional extensions
  //options.set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create(), StrikethroughExtension.create()));
  // uncomment to convert soft-breaks to hard breaks
//  options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");
  val parser = Parser.builder(options).build
  val renderer = HtmlRenderer.builder(options).build

  def toHtml(file: Path): Html = {
    // You can re-use parser and renderer instances
    val document = parser.parse(fileToString(file))
    Html(renderer.render(document))
  }
}
