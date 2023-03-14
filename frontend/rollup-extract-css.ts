import type {Plugin} from "rollup"
import {createFilter} from "@rollup/pluginutils"
import autoprefixer from "autoprefixer"
import cssnano from "cssnano"
import path from "path"
import postcss from "postcss"
import postcssUrl from "postcss-url"
import postcssNesting from "postcss-nesting"

// Inspiration from https://github.com/egoist/rollup-plugin-postcss/blob/master/src/index.js

function importOrder(id, getInfo): string[] {
  return getInfo(id).importedIds.flatMap(imported => {
    return [imported].concat(importOrder(imported, getInfo))
  }).filter((v, idx, arr) => arr.indexOf(v) === idx)
}

export interface ExtractOptions {
  outDir: string
  include?: string
  exclude?: string
  minimize?: boolean
  urlOptions: any
}

export default function extractcss(options: ExtractOptions): Plugin {
  const filter = createFilter(options.include || "**/*.css", options.exclude)
  const minimize = options.minimize || false
  const basicPlugins = [postcssNesting(), autoprefixer, postcssUrl(options.urlOptions)]
  const extraPlugins = minimize ? [cssnano()] : []
  const plugins = basicPlugins.concat(extraPlugins)
  const processed = new Map<string, string>()
  return {
    name: "rollup-plugin-extract-css",
    async transform(code, id) {
      if (!filter(id)) return
      const result = await postcss(plugins)
        .process(code, {from: id, to: path.resolve(options.outDir, "frontend.css")})
      processed.set(id, result.css)
      return {code: "", map: undefined}
    },
    augmentChunkHash(chunkInfo) {
      // JSON stringifies a Map. Go JavaScript.
      const ids = importOrder(chunkInfo.facadeModuleId, this.getModuleInfo)
      const obj = Array.from(processed).reduce((obj, [key, value]) => {
        if (ids.includes(key)) {
          obj[key] = value
        }
        return obj
      }, {})
      return JSON.stringify(obj)
    },
    async generateBundle(opts, bundle) {
      if (processed.size === 0) return
      Object.keys(bundle).forEach(entry => {
        const b = bundle[entry]
        if (b.type == "chunk" && b.isEntry) {
          const facade = b.facadeModuleId
          const orderedIds = importOrder(facade, this.getModuleInfo)
          const contents = orderedIds.map(id => processed.get(id)).filter(c => c)
          const content = "".concat(...contents)
          const name = path.parse(entry).name
          const ref = this.emitFile({
            name: b.name,
            fileName: `${name}.css`,
            type: "asset",
            source: content
          })
          console.log(`Created ${this.getFileName(ref)} from ${b.name}`)
        }
      })
    }
  }
}
