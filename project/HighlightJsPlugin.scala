import sbt.Keys.baseDirectory
import sbt.{AutoPlugin, Def, taskKey}

import java.nio.file.Path

class HighlightJsPlugin extends AutoPlugin {
  val writeScript = taskKey[Path]("Writes highlight.js script for node.js")
  override val projectSettings: Seq[Def.Setting[?]] = Seq(
    writeScript := baseDirectory.value.toPath
  )
}
