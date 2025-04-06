import fs from 'node:fs';
import hljs from 'highlight.js';
hljs.configure({ignoreUnescapedHTML: true, throwUnescapedHTML: true});
import parser from 'node-html-parser';
import scala from 'highlight.js/lib/languages/scala';
hljs.registerLanguage("scala", scala);
const inputFile = process.argv.at(2)
const data = fs.readFileSync(inputFile, 'utf8');
const doc = parser.parse(data);
const elems = doc.querySelectorAll('pre')
elems.forEach(e => {
  const codeElem = parser.parse(e.innerHTML);
  const code = codeElem.querySelector("code");
  if (code) {
    const highlighted = hljs.highlight(code.textContent, {language: 'scala'}).value;
    e.innerHTML = `<code class='language-scala hljs'>${highlighted}</code>`;
  }
});
const highlightedCode = doc.toString();
fs.writeFileSync(`${inputFile}`, highlightedCode);
console.log(`Highlighted ${inputFile}`)
