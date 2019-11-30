// @GENERATOR:play-routes-compiler
// @SOURCE:/home/anton/code/web/untitled1/conf/routes
// @DATE:Fri Nov 15 16:07:21 CST 2019


package router {
  object RoutesPrefix {
    private var _prefix: String = "/"
    def setPrefix(p: String): Unit = {
      _prefix = p
    }
    def prefix: String = _prefix
    val byNamePrefix: Function0[String] = { () => prefix }
  }
}
