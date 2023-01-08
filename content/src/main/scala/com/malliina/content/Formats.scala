package com.malliina.content

import java.nio.file.{Path, Paths}
import io.circe._

object Formats {
  implicit val pathCodec: Codec[Path] = Codec.from(
    Decoder.decodeString.map(s => Paths.get(s)),
    Encoder.encodeString.contramap(p => p.toAbsolutePath.toString)
  )
}
