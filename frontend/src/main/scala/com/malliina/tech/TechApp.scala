package com.malliina.tech

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@js.native
@JSImport("./css/app", JSImport.Namespace)
object AppCss extends js.Object

object TechApp:
  private val appCss = AppCss

  def main(args: Array[String]): Unit = ()
