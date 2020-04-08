const MiniCssExtractPlugin = require('mini-css-extract-plugin')
const isDevelopment = false
const path = require('path');
const rootDir = path.resolve(__dirname);
const cssDir = path.resolve(rootDir, 'src');

module.exports = {
  entry: {
    main: [path.resolve(cssDir, './index.ts')],
    fonts: [path.resolve(cssDir, './fonts.ts')],
  },
  output: {
    path: path.resolve(rootDir, '../dist/assets')
  },
  mode: "production",
  // Enable sourcemaps for debugging webpack's output.
  devtool: "source-map",

  resolve: {
    // Add '.ts' and '.tsx' as resolvable extensions.
    extensions: [".ts", ".scss"]
  },

  plugins: [
    new MiniCssExtractPlugin({
      filename: 'css/styles-[name].css',
      chunkFilename: 'styles-[id].css'
    })
  ],

  module: {
    rules: [
      {
        test: /\.ts(x?)$/,
        exclude: /node_modules/,
        use: [
          {
            loader: "ts-loader"
          }
        ]
      },
      // All output '.js' files will have any sourcemaps re-processed by 'source-map-loader'.
      {
        enforce: "pre",
        test: /\.js$/,
        loader: "source-map-loader"
      },
      {
        test: /\.s(a|c)ss$/,
        exclude: /\.module.(s(a|c)ss)$/,
        loader: [
          isDevelopment ? 'style-loader' : MiniCssExtractPlugin.loader,
          'css-loader',
          {
            loader: 'sass-loader',
            options: {
              sourceMap: isDevelopment
            }
          }
        ]
      },
      {
        test: /\.module\.s(a|c)ss$/,
        loader: [
          isDevelopment ? 'style-loader' : MiniCssExtractPlugin.loader,
          {
            loader: 'css-loader',
            options: {
              modules: true,
              sourceMap: isDevelopment
            }
          },
          {
            loader: 'sass-loader',
            options: {
              sourceMap: isDevelopment
            }
          }
        ]
      },
      // Inlines any font file less than 256KB; larger go hashed to fonts/...
      {
        test: /\.(png|woff|woff2|eot|ttf|svg)$/,
        use: [
          {loader: 'url-loader', options: {limit: 262144, name: 'fonts/[name]-[hash].[ext]'}}
        ]
      }
    ]
  }
};
