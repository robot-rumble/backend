package controllers

import controllers.Auth.{LoggedIn, LoggedOut, Visitor}
import forms.{CreateBoardForm, JoinBoardForm, PublishForm}
import models.Schema._
import models._
import play.api.Configuration
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
    config: Configuration,
)(implicit ec: ExecutionContext)
    extends AbstractController(cc) {

  def index() = auth.action { visitor => implicit request =>
    seasonsRepo.findAll() flatMap { seasons =>
      boardsRepo.findAllBare(visitor) map { boards =>
        val isAuthenticated = Visitor.asOption(visitor).isDefined
        val isAdmin = Visitor.asOption(visitor).exists(_.admin)
        Ok(views.html.board.index(seasons, boards, isAuthenticated, isAdmin, assetsFinder))
      }
    }
  }

  def view(id: Long) = auth.action { visitor => implicit request =>
    boardsRepo.findWithBattles(BoardId(id), 0, 10, 10, visitor) flatMap {
      case Some(fullBoardWithBattles) =>
        fullBoardWithBattles.board.password match {
          case Some(_) =>
            visitor match {
              case LoggedIn(user) =>
                fullBoardWithBattles.board.adminId match {
                  case Some(adminId) if user.id == adminId =>
                    Future successful Ok(
                      views.html.board.view(fullBoardWithBattles, true, assetsFinder)
                    )
                  case _ =>
                    boardsRepo.isMember(fullBoardWithBattles.board.id, user.id) map {
                      case true =>
                        Ok(views.html.board.view(fullBoardWithBattles, false, assetsFinder))
                      case false =>
                        Forbidden("You do not have access to this board")
                    }
                }
              case LoggedOut() =>
                Future successful Forbidden("You must be logged in to view this board")
            }
          case None =>
            Future successful Ok(views.html.board.view(fullBoardWithBattles, false, assetsFinder))
        }
      case None => Future successful NotFound("404")
    }
  }

  def viewBattles(id: Long, page: Long = 0) = auth.action { visitor => implicit request =>
    boardsRepo.findBareWithBattles(BoardId(id), page, 50, visitor) map {
      case Some(boardWithBattles) =>
        Ok(
          views.html.board.battles(boardWithBattles, page, assetsFinder)
        )
      case None => NotFound("404")
    }
  }

  def viewRobots(id: Long, page: Long = 0) = auth.action { visitor => implicit request =>
    boardsRepo.find(BoardId(id), page, 50, visitor) map {
      case Some(board) =>
        Ok(views.html.board.robots(board, page, assetsFinder))
      case None => NotFound("404")
    }
  }

  def viewRobotBattles(id: Long, robotId: Long, page: Long = 0) =
    auth.action { visitor => implicit request =>
      boardsRepo.findBareWithBattlesForRobot(BoardId(id), RobotId(robotId), page, 50, visitor) map {
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
      boardsRepo.findBare(BoardId(id), LoggedIn(user)) flatMap {
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
          boardsRepo.findBare(BoardId(id), LoggedIn(user)) flatMap {
            case Some(board) =>
              boardsRepo.publish(RobotId(data.robotId), board) flatMap {
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

  def create =
    auth.actionForceLI { user => implicit request =>
      if (user.admin)
        Future successful Ok(
          views.html.board
            .create(CreateBoardForm.form, assetsFinder)
        )
      else Future successful Forbidden("Not an admin")
    }

  private val MAX_BOARD_NUM = config.get[Int]("queue.maxAdminBoardNum")

  def postCreate = auth.actionForceLI { user => implicit request =>
    if (user.admin)
      CreateBoardForm.form.bindFromRequest.fold(
        formWithErrors => {
          Future.successful(
            BadRequest(views.html.board.create(formWithErrors, assetsFinder))
          )
        },
        data => {
          boardsRepo.findAllBareByAdmin(user.id) flatMap {
            boards =>
              if (boards.length < MAX_BOARD_NUM) {
                boardsRepo.findBareByPassword(data.password) flatMap {
                  case Some(_) =>
                    Future successful BadRequest(
                      views.html.board.create(
                        CreateBoardForm.form
                          .fill(data)
                          .withGlobalError("This password already exists"),
                        assetsFinder
                      )
                    )
                  case None =>
                    val bio = if (data.bio.nonEmpty) Some(data.bio) else None
                    boardsRepo.create(Some(user.id), data.name, bio, Some(data.password)) map {
                      board =>
                        Redirect(routes.BoardController.view(board.id.id))
                    }
                }
              } else {
                Future successful BadRequest(
                  views.html.board.create(
                    CreateBoardForm.form
                      .fill(data)
                      .withGlobalError(
                        s"You've already created the maximum amount of boards: $MAX_BOARD_NUM"
                      ),
                    assetsFinder
                  )
                )
              }
          }
        }
      )
    else Future successful Forbidden("Not an admin")
  }

  def join =
    auth.actionForceLI { _ => implicit request =>
      Future successful Ok(views.html.board.join(JoinBoardForm.form, assetsFinder))
    }

  def postJoin =
    auth.actionForceLI { user => implicit request =>
      JoinBoardForm.form.bindFromRequest.fold(
        formWithErrors => {
          Future.successful(
            BadRequest(views.html.board.join(formWithErrors, assetsFinder))
          )
        },
        data => {
          boardsRepo.findBareByPassword(data.password) flatMap {
            case Some(board) =>
              boardsRepo.addMembership(board.id, user.id) map { _ =>
                Redirect(routes.BoardController.view(board.id.id))
              }
            case None =>
              Future successful BadRequest(
                views.html.board.join(
                  JoinBoardForm.form
                    .fill(data)
                    .withGlobalError("Invalid password"),
                  assetsFinder
                )
              )
          }
        }
      )
    }

}
