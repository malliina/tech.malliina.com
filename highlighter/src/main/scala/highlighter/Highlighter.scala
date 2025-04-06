package highlighter

import org.scalajs.dom.HTMLDocument

import scala.scalajs.js
import scala.scalajs.js.annotation.*
import scala.scalajs.js.Dynamic.literal

@js.native
@JSImport("fs", "readFileSync")
def readFileSync(path: String, charset: String): String = js.native

@js.native
@JSImport("fs", JSImport.Default)
object fs extends js.Object:
  def readFileSync(path: String, charset: String): String = js.native
  def writeFileSync(path: String, content: String): String = js.native

@js.native
trait HighlightOptions extends js.Object:
  def language: String = js.native

object HighlightOptions:
  def apply(language: String): HighlightOptions =
    literal(language = language).asInstanceOf[HighlightOptions]

@js.native
trait HighlightResult extends js.Object:
  def value: String = js.native

@js.native
@JSImport("highlight.js/lib/core", JSImport.Default)
object hljs extends js.Object:
  def registerLanguage(name: String, lang: js.Any): Unit = js.native
  def highlightAll(): Unit = js.native
  def highlight(code: String, options: HighlightOptions): HighlightResult = js.native

@js.native
@JSImport("highlight.js/lib/languages/scala", JSImport.Default)
object hljsScala extends js.Object

@js.native
@JSImport("node-html-parser", JSImport.Default)
object parser extends js.Object:
  def parse(data: String): HTMLDocument = js.native

object Highlighter:
  def main(args: Array[String]): Unit =
    val files = fs.readFileSync("pages.txt", "utf-8").split("\n").toList
    files.foreach(file => highlight(file))

  private def highlight(file: String): Unit =
    val content = fs.readFileSync(file, "utf-8")
    hljs.registerLanguage("scala", hljsScala)
    val doc = parser.parse(content)
    val elems = doc.querySelectorAll("pre")
    elems.foreach: e =>
      val codeElem = parser.parse(e.innerHTML)
      Option(codeElem.querySelector("code")).foreach: code =>
        val highlighted = hljs.highlight(code.textContent, HighlightOptions("scala"))
        e.innerHTML = s"<code class='language-scala hljs'>${highlighted.value}</code>"
    fs.writeFileSync(file, doc.toString)
    println(s"Highlighted $file.")
