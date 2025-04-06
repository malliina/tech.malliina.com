const fs = require('node:fs');
const hljs = require('highlight.js');
hljs.configure({ignoreUnescapedHTML: true, throwUnescapedHTML: true});
const parser = require('node-html-parser');
hljs.registerLanguage("scala", require("highlight.js/lib/languages/scala"));
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
