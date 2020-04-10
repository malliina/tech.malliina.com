package com.malliina.content

import java.nio.file.{Files, Paths}

import scalatags.Text.all._

import scala.collection.JavaConverters.asScalaIteratorConverter

object Pages extends Pages

class Pages {
  def index(content: Html): TagPage = TagPage(
    html(
      head(
        styleAt("styles-fonts.css"),
        styleAt("styles-main.css"),
        scriptAt("highlight.scala.js"),
        scriptAt("main.js")
      ),
      body(
        div(`class` := "content")(
          content
        )
      )
    )
  )

  def styleAt(file: String) =
    link(rel := "stylesheet", href := findAsset(s"css/$file"))

  def scriptAt(file: String) = script(src := findAsset(file))

  def findAsset(file: String): String = {
    val root = Paths.get("target").resolve("site")
    val path = root.resolve("assets").resolve(file)
    val dir = path.getParent
    val candidates = Files.list(dir).iterator().asScala.toList
    val lastSlash = file.lastIndexOf("/")
    val nameStart = if (lastSlash == -1) 0 else lastSlash + 1
    val name = file.substring(nameStart)
    val dotIdx = name.lastIndexOf(".")
    val noExt = name.substring(0, dotIdx)
    val ext = name.substring(dotIdx + 1)
    val result = candidates.filter { p =>
      val candidateName = p.getFileName.toString
      candidateName.startsWith(noExt) && candidateName.endsWith(ext)
    }.sortBy { p =>
      Files.getLastModifiedTime(p)
    }.reverse.headOption
    val found = result.getOrElse(
      fail(s"Not found: '$file'. Found ${candidates.mkString(", ")}.")
    )
    root.relativize(found).toString.replace("\\", "/")
  }

  def fail(message: String) = throw new Exception(message)
}
