package com.malliina.tech

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object TechApp:
  val appCss = AppCss

  def main(args: Array[String]): Unit =
    println("Hej")

@js.native
@JSImport("./css/app", JSImport.Namespace)
object AppCss extends js.Object
