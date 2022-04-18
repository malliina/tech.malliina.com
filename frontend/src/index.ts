import "./main.scss"
import "./monokai.scss"
import * as scala from "./scala"
import "./images/jag.jpg"
const hljs = require('highlight.js/lib/core')

hljs.registerLanguage("scala", scala)
hljs.highlightAll()
