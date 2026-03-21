package com.malliina.content

import com.malliina.values.{ErrorMessage, StringCompanion, WrappedString}

case class CacheControl(value: String) extends AnyVal with WrappedString

object CacheControl extends StringCompanion[CacheControl]:
  val headerName = "Cache-Control"

  override def build(input: String): Either[ErrorMessage, CacheControl] = Right(CacheControl(input))

object CacheControls extends CacheControls

trait CacheControls:
  val defaultCacheControl = CacheControl("public, max-age=0")
  val eternalCache = CacheControl("public, max-age=31536000")
