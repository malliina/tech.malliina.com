import NPM.{runProcessSync, buildCommand, installCommand, watchCommand}
import sbt._

import scala.sys.process.Process

object NPM {
  val isWindows = sys.props("os.name").toLowerCase().contains("win")
  val cmdPrefix = if (isWindows) "cmd /c " else ""
  val buildCommand = "npm run build"
  val installCommand = "npm install"
  val watchCommand = "npm run watch"

  def stage(base: File, log: Logger): File = {
    Seq("npm install", buildCommand).foreach { cmd =>
      runProcessSync(cmd, base, log)
    }
    base
  }

  def runProcessSync(command: String, base: File, log: Logger): Unit = {
    val actualCommand = canonical(command)
    log.info(s"Running '$actualCommand'...")
    val rc = Process(actualCommand, base).run(log).exitValue()
    if (rc != 0) {
      throw new Exception(s"$actualCommand failed with $rc")
    }
  }

  def canonical(command: String) = s"$cmdPrefix$command"
}

/** Try https://stackoverflow.com/questions/269494/how-can-i-cause-a-child-process-to-exit-when-the-parent-does
  *
  * @see https://torre.me.uk/2019/03/06/scala-play-rest-and-angular/
  * @see https://gist.github.com/jroper/387b05830044d006eb231abd1bc768e5
  */
class NPM(base: File, target: File, log: Logger) {
  private var watchProcess: Option[Process] = None

  def install(): Unit = {
    val cacheFile = target / "package-json-last-modified"
    val cacheLastModified: Long =
      if (cacheFile.exists()) {
        try {
          IO.read(cacheFile).trim.toLong
        } catch {
          case _: NumberFormatException => 0L
        }
      } else {
        0L
      }
    val lastModified = (base / "package.json").lastModified()
    // Check if package.json has changed since we last ran this
    if (cacheLastModified != lastModified) {
      runProcessSync(installCommand, base, log)
      IO.write(cacheFile, lastModified.toString)
    }
  }

  def build(): Unit = {
    install()
    runProcessSync(buildCommand, base, log)
  }

//  def watch(): Unit = {
//    val cmd = NPM.canonical(watchCommand)
//    log.info(s"Watching with '$cmd'...")
//    watchProcess = Some(Process(cmd, base).run(log))
//  }

  def stop(): Unit = {
    watchProcess.foreach(_.destroy())
    watchProcess = None
    if (NPM.isWindows) {
      // Node child processes are not properly killed with `process.destroy()` on Windows. This gets the job done.
      runProcessSync("taskkill /im node.exe /F", base, log)
    }
  }
}
