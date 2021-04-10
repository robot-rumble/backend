package controllers

import controllers.Auth.LoggedIn
import forms.PublishForm
import models.Schema._
import models._
import play.api.mvc._

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BoardController @Inject()(
    cc: ControllerComponents,
    assetsFinder: AssetsFinder,
    boardsRepo: Boards,
    robotsRepo: Robots,
    seasonsRepo: Seasons,
    auth: Auth.AuthAction,
)(implicit ec: ExecutionContext)
    extends AbstractController(cc) {

  def index() = Action.async { implicit request =>
    seasonsRepo.findAll() flatMap { seasons =>
      boardsRepo.findAllBare map { boards =>
        Ok(views.html.board.index(seasons, boards, assetsFinder))
      }
    }
  }

  def view(id: Long, page: Long = 0) = Action.async { implicit request =>
    val (numRobotsPerPage, numBattlesPerPage) = if (page == 0) (10, 10) else (20, 0)
    boardsRepo.findWithBattles(BoardId(id), page, numRobotsPerPage, numBattlesPerPage) map {
      case Some(fullBoardWithBattles) =>
        Ok(views.html.board.view(fullBoardWithBattles, page, assetsFinder))
      case None => NotFound("404")
    }
  }

  def viewBattles(id: Long, page: Long = 0) = Action.async { implicit request =>
    boardsRepo.findBareWithBattles(BoardId(id), page, 30) map {
      case Some(boardWithBattles) =>
        Ok(
          views.html.board.battles(boardWithBattles, page, assetsFinder)
        )
      case None => NotFound("404")
    }
  }

  def viewRobotBattles(id: Long, robotId: Long, page: Long = 0) =
    Action.async { implicit request =>
      boardsRepo.findBareWithBattlesForRobot(BoardId(id), RobotId(robotId), page, 30) map {
        case Some((robot, boardWithBattles)) =>
          Ok(
            views.html.board.robot(
              boardWithBattles,
              robot,
              page,
              assetsFinder
            )
          )
        case None => NotFound("404")
      }
    }

  private def getRobotOptions(user: User): Future[Seq[(String, String)]] =
    robotsRepo
      .findAll(user.id)(LoggedIn(user))
      .map(_.map(robot => (robot.id.id.toString, robot.name)))

  def publish(id: Long) =
    auth.actionForceLI { user => implicit request =>
      boardsRepo.findBare(BoardId(id)) flatMap {
        case Some(board) =>
          getRobotOptions(user) map { robotOptions =>
            Ok(
              views.html.board
                .publish(
                  PublishForm.form,
                  board,
                  robotOptions,
                  None,
                  assetsFinder
                )
            )
          }
        case None => Future successful NotFound("404")
      }
    }

  def postPublish(id: Long) =
    auth.actionForceLI { user => implicit request =>
      PublishForm.form.bindFromRequest.fold(
        _formWithErrors => {
          Future.successful(InternalServerError("Unexpected form input"))
        },
        data => {
          boardsRepo.findBare(BoardId(id)) flatMap {
            case Some(board) =>
              robotsRepo.publish(RobotId(data.robotId), board) flatMap {
                case None => Future successful NotFound("404")
                case Some(result) =>
                  getRobotOptions(user) map { robotOptions =>
                    Ok(
                      views.html.board.publish(
                        PublishForm.form,
                        board,
                        robotOptions,
                        Some(result),
                        assetsFinder
                      )
                    )
                  }
              }
            case None => Future successful NotFound("404")
          }
        }
      )
    }
}
