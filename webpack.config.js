const path = require('path')
const MiniCssExtractPlugin = require('mini-css-extract-plugin')

module.exports = {
  mode: process.env.NODE_ENV,
  stats: 'minimal',
  entry: {
    app: ['@babel/polyfill', './src/app.js'],
  },
  output: {
    path: path.join(__dirname, 'dist'),
    filename: 'bundle.js',
  },
  module: {
    rules: [
      {
        test: /\.js$/,
        exclude: /node_modules/,
        use: {
          loader: 'babel-loader',
        },
      },
      {
        test: /\.(sa|sc|c)ss$/,
        use: [MiniCssExtractPlugin.loader, 'css-loader', 'sass-loader'],
      },
      {
        test: /\.elm$/,
        use: {
          loader: 'elm-webpack-loader',
          options: {},
        },
      },
    ],
  },
  plugins: [new MiniCssExtractPlugin({ filename: './css/[name].css' })],
  devServer: {
    contentBase: path.join(__dirname, 'dist'),
    historyApiFallback: true,
    stats: 'minimal',
  },
}
