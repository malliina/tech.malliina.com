package com.malliina.content

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, StandardOpenOption}

import scalatags.Text

case class TagPage(tags: Text.TypedTag[String]) {
  override def toString = tags.toString()
  def render = toString
  def write(to: Path) = TagPage.write(this, to)
}

object TagPage {
  val log = AppLogger(getClass)
  val DocTypeTag = "<!DOCTYPE html>"

  def write(page: TagPage, to: Path): Path = {
    val bytes = (DocTypeTag + page.render).getBytes(StandardCharsets.UTF_8)
    Files.write(
      to,
      bytes,
      StandardOpenOption.TRUNCATE_EXISTING,
      StandardOpenOption.CREATE
    )
    val size = Files.size(to)
    log.info(s"Wrote $size bytes to '${to.toAbsolutePath}'.")
    to
  }
}
