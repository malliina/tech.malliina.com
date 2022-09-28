const MiniCssExtractPlugin = require('mini-css-extract-plugin')
const isDevelopment = false
const path = require('path');
const rootDir = path.resolve(__dirname);
const srcDir = path.resolve(rootDir, 'src');

module.exports = {
  entry: {
    main: [path.resolve(srcDir, './index.ts')],
    fonts: [path.resolve(srcDir, './fonts.ts')],
  },
  output: {
    filename: '[name]-[contenthash].js',
    path: path.resolve(rootDir, '../target/site/assets')
  },
  mode: "production",
  devtool: "source-map",

  resolve: {
    extensions: [".ts", ".scss", ".js"]
  },

  plugins: [
    new MiniCssExtractPlugin({
      filename: 'css/styles-[name]-[fullhash].css',
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
      {
        test: /\.css$/,
        use: [
          isDevelopment ? 'style-loader' : MiniCssExtractPlugin.loader,
          'css-loader'
        ]
      },
      {
        test: /\.s(a|c)ss$/,
        exclude: /\.module.(s(a|c)ss)$/,
        use: [
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
        use: [
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
        type: 'asset',
        generator: {
          filename: 'fonts/[name]-[fullhash][ext]'
        },
        parser: {
          dataUrlCondition: {
            maxSize: 256 * 1024 // 256 KB
          }
        }
      },
      {
        test: /\.jpg$/,
        exclude: /node_modules/,
        use: [
          {
            loader: "file-loader",
            options: {
              name: "images/[name]-[fullhash].[ext]"
            }
          }
        ]
      }
    ]
  }
};
