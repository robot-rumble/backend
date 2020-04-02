package controllers

import javax.inject._
import play.api.mvc._

@Singleton
class HomeController @Inject()(cc: ControllerComponents, assetsFinder: AssetsFinder)
  extends AbstractController(cc) {

  def index: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.index(assetsFinder))
  }
}
