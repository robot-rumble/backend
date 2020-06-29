package robotCode

import java.io.File

import models.Schema.Lang

import scala.io.Source

object LoadCode {
  def loadCode(path: String) = {
    val source = Source.fromFile("app/robotCode/lang-support/" + path)
    try source.mkString
    finally source.close()
  }

  val PYTHON = loadCode("python/default.py")
  val JAVASCRIPT = loadCode("javascript/default.js")

  def apply(lang: Lang): String = {
    lang match {
      case Lang.Python     => PYTHON
      case Lang.Javascript => JAVASCRIPT
    }
  }
}
