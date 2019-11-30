
package views.html

import _root_.play.twirl.api.TwirlFeatureImports._
import _root_.play.twirl.api.TwirlHelperImports._
import _root_.play.twirl.api.Html
import _root_.play.twirl.api.JavaScript
import _root_.play.twirl.api.Txt
import _root_.play.twirl.api.Xml
import models._
import controllers._
import play.api.i18n._
import views.html._
import play.api.templates.PlayMagic._
import play.api.mvc._
import play.api.data._

object main extends _root_.play.twirl.api.BaseScalaTemplate[play.twirl.api.HtmlFormat.Appendable,_root_.play.twirl.api.Format[play.twirl.api.HtmlFormat.Appendable]](play.twirl.api.HtmlFormat) with _root_.play.twirl.api.Template3[String,AssetsFinder,Html,play.twirl.api.HtmlFormat.Appendable] {

  /*
 * This template is called from the `index` template. This template
 * handles the rendering of the page header and body tags. It takes
 * three arguments, a `String` for the title of the page and an `Html`
 * object to insert into the body of the page and an `AssetFinder`
 * to define to reverse route static assets.
 */
  def apply/*8.2*/(title: String, assetsFinder: AssetsFinder)(content: Html):play.twirl.api.HtmlFormat.Appendable = {
    _display_ {
      {


Seq[Any](format.raw/*9.1*/("""
"""),format.raw/*10.1*/("""<!DOCTYPE html>
<html lang="en">
    <head>
        """),format.raw/*13.62*/("""
        """),format.raw/*14.9*/("""<title>"""),_display_(/*14.17*/title),format.raw/*14.22*/("""</title>
        <link rel="stylesheet" media="screen" href=""""),_display_(/*15.54*/assetsFinder/*15.66*/.path("stylesheets/main.css")),format.raw/*15.95*/("""">
        <link rel="shortcut icon" type="image/png" href=""""),_display_(/*16.59*/assetsFinder/*16.71*/.path("images/favicon.png")),format.raw/*16.98*/("""">
        <script src=""""),_display_(/*17.23*/assetsFinder/*17.35*/.path("javascripts/hello.js")),format.raw/*17.64*/("""" type="text/javascript"></script>
    </head>
    <body>
        """),format.raw/*21.32*/("""
        """),_display_(/*22.10*/content),format.raw/*22.17*/("""
    """),format.raw/*23.5*/("""</body>
</html>
"""))
      }
    }
  }

  def render(title:String,assetsFinder:AssetsFinder,content:Html): play.twirl.api.HtmlFormat.Appendable = apply(title,assetsFinder)(content)

  def f:((String,AssetsFinder) => (Html) => play.twirl.api.HtmlFormat.Appendable) = (title,assetsFinder) => (content) => apply(title,assetsFinder)(content)

  def ref: this.type = this

}


              /*
                  -- GENERATED --
                  DATE: 2019-11-15T16:07:21.816234
                  SOURCE: /home/anton/code/web/untitled1/app/views/main.scala.html
                  HASH: 53adeab63e755599f15a4ff5273d97955b4dbce5
                  MATRIX: 1067->327|1219->386|1247->387|1327->492|1363->501|1398->509|1424->514|1513->576|1534->588|1584->617|1672->678|1693->690|1741->717|1793->742|1814->754|1864->783|1958->939|1995->949|2023->956|2055->961
                  LINES: 27->8|32->9|33->10|36->13|37->14|37->14|37->14|38->15|38->15|38->15|39->16|39->16|39->16|40->17|40->17|40->17|43->21|44->22|44->22|45->23
                  -- GENERATED --
              */
          