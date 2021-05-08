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
    auth: Auth.AuthAction,
)(implicit ec: ExecutionContext)
    extends AbstractController(cc) {

  def index = auth.action { visitor => implicit request =>
    boardsRepo.findAllBare(visitor) map { boards =>
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
