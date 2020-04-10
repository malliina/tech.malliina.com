# tech.malliina.com

This is the site [tech.malliina.com](https://tech.malliina.com).

## Process

1. Add Markdown files to [docs](docs).
1. [mdoc](mdoc) typechecks Scala code embedded in the Markdown.
1. [flexmark-java](https://github.com/vsch/flexmark-java) converts (typesafe) Markdown to HTML.
1. [ScalaTags](https://www.lihaoyi.com/scalatags/) embeds the HTML into a web page so we get one page per Markdown file.
1. Webpack generates CSS and JS assets for the HTML.
1. The HTML, CSS and JS is deployed as a static website to [Netlify](https://www.netlify.com).

## Deployments

Run:

    netlify deploy --prod
