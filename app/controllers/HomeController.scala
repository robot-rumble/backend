package controllers

import javax.inject._
import play.api.mvc._
import play.i18n.I18nComponents
@Singleton
class HomeController @Inject()(
    cc: ControllerComponents,
    assetsFinder: AssetsFinder
) extends AbstractController(cc) {

  def index = Action { implicit request =>
    Ok(views.html.index(assetsFinder))
  }

  def rules = TODO
}
