package com.malliina.tech

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object TechApp:
  val fontsCss = FontsCss
  val mainCss = MainCss
  val monokaiCss = MonokaiCss

  def main(args: Array[String]): Unit =
    println("Hej")

@js.native
@JSImport("./css/main.css", JSImport.Namespace)
object MainCss extends js.Object

@js.native
@JSImport("./css/fonts.css", JSImport.Namespace)
object FontsCss extends js.Object

@js.native
@JSImport("./css/monokai.css", JSImport.Namespace)
object MonokaiCss extends js.Object
