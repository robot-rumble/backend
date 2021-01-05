package controllers

import play.api.Configuration
import play.api.mvc._

import javax.inject._

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
