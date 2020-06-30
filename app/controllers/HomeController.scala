package controllers

import javax.inject._
import play.api.Configuration
import play.api.mvc._

@Singleton
class HomeController @Inject()(
    cc: ControllerComponents,
    assetsFinder: AssetsFinder,
    config: Configuration,
) extends AbstractController(cc) {

  def index = Action { implicit request =>
    Ok(views.html.index(assetsFinder))
  }

  def rules = Action { implicit request =>
    Redirect(config.get[String]("site.docsUrl"))
  }
}
