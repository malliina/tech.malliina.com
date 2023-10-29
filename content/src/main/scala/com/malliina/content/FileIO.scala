package com.malliina.content

import java.io.{FileInputStream, FileOutputStream, IOException, InputStream, OutputStream}
import java.nio.charset.StandardCharsets
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{FileVisitResult, Files, Path, SimpleFileVisitor, StandardCopyOption, StandardOpenOption}
import java.util.zip.GZIPOutputStream

object FileIO:
  val log = AppLogger(getClass)

  def writeLines(lines: Seq[String], to: Path): Path =
    write(lines.mkString("\n").getBytes(StandardCharsets.UTF_8), to)

  def write(bytes: Array[Byte], to: Path): Path =
    if !Files.isRegularFile(to) then
      val dir = to.getParent
      if !Files.isDirectory(dir) then Files.createDirectories(dir)
      Files.createFile(to)
    log.info(s"Writing ${to.toAbsolutePath}...")

    Files.write(to, bytes, StandardOpenOption.TRUNCATE_EXISTING)
    log.info(s"Wrote ${to.toAbsolutePath}.")
    to

  def copy(from: Path, to: Path): Unit =
    val dir = to.getParent
    if !Files.isDirectory(dir) then Files.createDirectories(dir)
    Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING)
    log.info(s"Copied ${from.toAbsolutePath} to ${to.toAbsolutePath}.")

  def gzip(src: Path, dest: Path): Unit =
    using(new FileInputStream(src.toFile)): in =>
      using(new FileOutputStream(dest.toFile)): out =>
        using(new GZIPOutputStream(out, 8192)): gzip =>
          copyStream(in, gzip)
          gzip.finish()

  // Adapted from sbt-io
  private def copyStream(in: InputStream, out: OutputStream): Unit =
    val buffer = new Array[Byte](8192)

    def read(): Unit =
      val byteCount = in.read(buffer)
      if byteCount >= 0 then
        out.write(buffer, 0, byteCount)
        read()

    read()

  // https://stackoverflow.com/a/27917071
  def deleteDirectory(dir: Path): Path =
    if Files.exists(dir) then
      Files.walkFileTree(
        dir,
        new SimpleFileVisitor[Path]:
          override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult =
            Files.delete(file)
            FileVisitResult.CONTINUE

          override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult =
            Files.delete(dir)
            FileVisitResult.CONTINUE
      )
    else dir

  def using[T <: AutoCloseable, U](res: T)(code: T => U): U =
    try code(res)
    finally res.close()
