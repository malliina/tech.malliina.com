package com.malliina.content

import java.nio.file.{Path, Paths}

import play.api.libs.json.{Format, Json, Reads, Writes}

object Formats {
  implicit val pathFormat: Format[Path] = Format[Path](
    Reads(json => json.validate[String].map(s => Paths.get(s))),
    Writes(p => Json.toJson(p.toAbsolutePath.toString))
  )
}
