# tech.malliina.com

This is the site [tech.malliina.com](https://tech.malliina.com).

## Publishing

1. Add Markdown files to [docs](docs).
1. [mdoc](mdoc) typechecks Scala code embedded in the Markdown.
1. [flexmark-java](https://github.com/vsch/flexmark-java) converts (typesafe) Markdown to HTML.
1. [ScalaTags](https://www.lihaoyi.com/scalatags/) embeds the HTML into a web page, so we get one page per Markdown file.
1. Webpack generates CSS and JS assets for the HTML.
1. [Netlify](https://www.netlify.com) deploys the HTML, CSS and JS as a static website to [tech.malliina.com](https://tech.malliina.com).

## Development

To run locally:

    sbt
    ~build

Navigate to http://localhost:10101/list.html.

## Deployments

To deploy to [tech.malliina.com](https://tech.malliina.com) hosted at Netlify, push to the `master` branch.
