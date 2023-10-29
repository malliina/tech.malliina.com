package com.malliina.content

import java.nio.file.{Path, Paths}
import io.circe.*

object Formats:
  given Codec[Path] = Codec.from(
    Decoder.decodeString.map(s => Paths.get(s)),
    Encoder.encodeString.contramap(p => p.toAbsolutePath.toString)
  )
