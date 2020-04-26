import "./main.scss"
import "./monokai.scss"
import * as scala from "./scala"
import * as hljs from "./core"
import "./images/jag.jpg"

hljs.registerLanguage("scala", scala)
hljs.initHighlightingOnLoad()
