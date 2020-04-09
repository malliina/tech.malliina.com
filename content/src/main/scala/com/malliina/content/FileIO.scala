package com.malliina.content

import java.io.{FileInputStream, FileOutputStream, InputStream, OutputStream}
import java.nio.file.{Files, Path, StandardCopyOption}
import java.util.zip.GZIPOutputStream

object FileIO {
  val log = AppLogger(getClass)

  def copy(from: Path, to: Path): Unit = {
    val dir = to.getParent
    if (!Files.isDirectory(dir))
      Files.createDirectories(dir)
    Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING)
    log.info(s"Copied ${from.toAbsolutePath} to ${to.toAbsolutePath}.")
  }

  def gzip(src: Path, dest: Path): Unit =
    using(new FileInputStream(src.toFile)) { in =>
      using(new FileOutputStream(dest.toFile)) { out =>
        using(new GZIPOutputStream(out, 8192)) { gzip =>
          copyStream(in, gzip)
          gzip.finish()
        }
      }
    }

  // Adapted from sbt-io
  private def copyStream(in: InputStream, out: OutputStream): Unit = {
    val buffer = new Array[Byte](8192)

    def read(): Unit = {
      val byteCount = in.read(buffer)
      if (byteCount >= 0) {
        out.write(buffer, 0, byteCount)
        read()
      }
    }

    read()
  }

  def using[T <: AutoCloseable, U](res: T)(code: T => U): U =
    try code(res)
    finally res.close()
}
