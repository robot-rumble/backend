package controllers

import controllers.Auth.LoggedOut
import javax.inject._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import models._
import models.Schema._

@Singleton
class BoardController @Inject()(
    cc: ControllerComponents,
    assetsFinder: AssetsFinder,
    boardsRepo: Boards,
)(implicit ec: ExecutionContext)
    extends AbstractController(cc) {

  def index = Action.async { implicit request =>
    boardsRepo.findAll(10) map { boards =>
      Ok(views.html.board.index(boards, assetsFinder))
    }
  }

  def view(id: Long, page: Long = 0) = Action.async { implicit request =>
    boardsRepo.find(BoardId(id), page, 10) map {
      case Some(board) => Ok(views.html.board.view(board, assetsFinder))
      case None        => NotFound("404")
    }
  }

  def viewRobotBattles(id: Long, robotId: Long, page: Long = 0) =
    Action.async { implicit request =>
      boardsRepo.findWithBattlesForRobot(BoardId(id), RobotId(robotId), page, 30) map {
        case Some(boardWithBattles) =>
          Ok(
            views.html.board.robot(
              boardWithBattles,
              RobotId(robotId),
              page,
              assetsFinder
            )
          )
        case None => NotFound("404")
      }
    }
}
