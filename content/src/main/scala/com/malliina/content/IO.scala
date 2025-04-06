package com.malliina.content

import java.nio.file.{Path, Paths}
import scala.sys.process.{Process, ProcessLogger}

case class ExitValue(value: Int) extends AnyVal:
  override def toString = s"$value"

object IO:
  private val log = AppLogger(getClass)
  val isWindows = sys.props("os.name").toLowerCase().contains("win")
  val cmdPrefix = if isWindows then "cmd /c " else ""
  val userDir = Paths.get(sys.props("user.dir"))

  def run(cmd: String, cwd: Path = userDir): ExitValue =
    log.info(s"Running '$cmd' in $cwd...")
    val logger = ProcessLogger(out => log.info(out), err => log.error(err))
    val process = Process(s"$cmdPrefix$cmd", cwd.toFile).run(logger)
    // blocks
    ExitValue(process.exitValue())
