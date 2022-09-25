import java.io.{File, IOException}
import java.nio.charset.StandardCharsets
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes

import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.{Format, Json, Reads, Writes}

sealed abstract class Mode(val name: String)

object Mode {
  case object Prod extends Mode("prod")
  case object Dev extends Mode("dev")
}

object AppLogger {
  def apply(cls: Class[_]): Logger = {
    val name = cls.getName.reverse.dropWhile(_ == '$').reverse
    LoggerFactory.getLogger(name)
  }
}

object FileIO {
  private val log = AppLogger(getClass)

  implicit val pathFormat: Format[Path] = Format[Path](
    Reads(json => json.validate[String].map(s => Paths.get(s))),
    Writes(p => Json.toJson(p.toAbsolutePath.toString))
  )

  def writeJson[T: Writes](t: T, to: Path): Path =
    write(Json.prettyPrint(Json.toJson(t)).getBytes(StandardCharsets.UTF_8), to)

  def write(bytes: Array[Byte], to: Path): Path = {
    if (!Files.isRegularFile(to)) {
      val dir = to.getParent
      if (!Files.isDirectory(dir))
        Files.createDirectories(dir)
      Files.createFile(to)
    }
    Files.write(to, bytes, StandardOpenOption.TRUNCATE_EXISTING)
    log.info(s"Wrote ${to.toAbsolutePath}.")
    to
  }

  // https://stackoverflow.com/a/27917071
  def deleteDirectory(dir: File): Path = deleteDirectory(dir.toPath)

  def deleteDirectory(dir: Path): Path = {
    if (Files.exists(dir)) {
      Files.walkFileTree(
        dir,
        new SimpleFileVisitor[Path] {
          override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
            Files.delete(file)
            FileVisitResult.CONTINUE
          }

          override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult = {
//            Files.delete(dir)
            FileVisitResult.CONTINUE
          }
        }
      )
    } else {
      dir
    }
  }
}
