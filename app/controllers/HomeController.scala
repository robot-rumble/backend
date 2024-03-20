package controllers

import controllers.Auth.LoggedOut
import models.Schema.BoardId
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

  def index = Action.async { implicit request =>
    boardsRepo.find(BoardId(config.get[Long]("site.leaderboardId")), 0, 10)(LoggedOut()) map {
      leaderboard =>
        Ok(views.html.index(leaderboard, assetsFinder))
    }
  }

  def rules = Action { implicit request =>
    Redirect(config.get[String]("site.docsUrl"))
  }

  def demo = Action { implicit request =>
    Ok(views.html.robot.demo(assetsFinder))
  }

  def tutorialHome = Action { implicit request =>
    Ok(views.html.tutorialHome(assetsFinder))
  }

  def tutorial(part: Int) = Action { implicit request =>
    if (part == 1 || part == 2) {
      Ok(views.html.robot.tutorial(assetsFinder, part))
    } else NotFound
  }
}
