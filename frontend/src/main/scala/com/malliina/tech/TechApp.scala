package com.malliina.tech

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object TechApp {
  def main(args: Array[String]): Unit = {
    hljs.registerLanguage("scala", hljsScala)
    hljs.highlightAll()
  }
}

@js.native
@JSImport("highlight.js/lib/core", JSImport.Default)
object hljs extends js.Object {
  def registerLanguage(name: String, lang: js.Any): Unit = js.native
  def highlightAll(): Unit = js.native
}

@js.native
@JSImport("highlight.js/lib/languages/scala", JSImport.Default)
object hljsScala extends js.Object
