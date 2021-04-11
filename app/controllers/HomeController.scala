package controllers

import models._
import play.api.Configuration
import play.api.mvc._

import javax.inject._
import scala.concurrent.ExecutionContext

@Singleton
class HomeController @Inject()(
    cc: ControllerComponents,
    assetsFinder: AssetsFinder,
    config: Configuration,
    boardsRepo: Boards,
)(implicit ec: ExecutionContext)
    extends AbstractController(cc) {

  def index = Action.async { implicit request =>
    boardsRepo.findAllBare map { boards =>
      Ok(views.html.index(boards, assetsFinder))
    }
  }

  def rules = Action { implicit request =>
    Redirect(config.get[String]("site.docsUrl"))
  }

  def demo = Action { implicit request =>
    Ok(views.html.robot.demo(assetsFinder))
  }
}
