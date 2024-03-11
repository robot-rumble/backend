package controllers

import controllers.Auth.{LoggedIn, LoggedOut, Visitor}
import forms.{CreateRobotForm, UpdateRobotCodeForm, UpdateRobotForm}
import models.Schema._
import models._
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc._

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RobotController @Inject()(
    cc: MessagesControllerComponents,
    assetsFinder: AssetsFinder,
    auth: Auth.AuthAction,
    robotsRepo: Robots,
    boardsRepo: Boards,
    config: Configuration,
)(implicit ec: ExecutionContext)
    extends MessagesAbstractController(cc) {

  val BUILTIN_USER = UserId(config.get[Long]("site.builtinUserId"))

  def create =
    auth.actionForceLI { _ => implicit request =>
      Future successful Ok(
        views.html.robot
          .create(
            CreateRobotForm.form.fill(CreateRobotForm.Data("", Lang.Python, true, "")),
            assetsFinder
          )
      )
    }

  private def createOnSuccess(
      user: Schema.User,
      data: CreateRobotForm.Data
  ): Future[Either[Robot, String]] = {
    robotsRepo.findBare(user.id, data.name)(LoggedIn(user)) flatMap {
      case Some(_) =>
        Future successful Right("Robot with this name already exists")
      case None =>
        robotsRepo.create(user.id, data.name, data.lang, data.openSource, data.bio) map {
          case Some(robot) => Left(robot)
          case None        => Right("You must be verified to create a robot")
        }
    }
  }

  def postCreate =
    auth.actionForceLI { user => implicit request =>
      CreateRobotForm.form.bindFromRequest.fold(
        formWithErrors => {
          Future successful BadRequest(
            views.html.robot.create(formWithErrors, assetsFinder)
          )
        },
        data => {
          createOnSuccess(user, data) map {
            case Left(robot) =>
              Redirect(
                routes.RobotController.view(user.username, robot.name)
              )
            case Right(error) =>
              BadRequest(
                views.html.robot.create(
                  CreateRobotForm.form
                    .fill(data)
                    .withGlobalError(error),
                  assetsFinder
                )
              )
          }
        }
      )
    }

  def apiCreate =
    auth.actionForceLI { user => implicit request =>
      CreateRobotForm.form.bindFromRequest.fold(
        formWithErrors => {
          Future successful BadRequest(formWithErrors.errorsAsJson)
        },
        data => {
          createOnSuccess(user, data) map {
            case Left(robot) =>
              Ok(Json.toJson(robot))
            case Right(error) =>
              BadRequest(
                CreateRobotForm.form
                  .withGlobalError(error)
                  .errorsAsJson
              )
          }
        }
      )
    }

  def update(_username: String, name: String) =
    auth.actionForceLI { user => implicit request =>
      robotsRepo.findBare(user.id, name)(LoggedIn(user)) map {
        case Some(robot) =>
          Ok(
            views.html.robot.update(UpdateRobotForm.form.fill(UpdateRobotForm.Data(robot.name, robot.bio, robot.openSource)), robot, assetsFinder)
          )
        case None =>
          NotFound("404")
      }
    }

  def postUpdate(id: Long) =
    auth.actionForceLI { user => implicit request =>
      robotsRepo.findBare(RobotId(id))(LoggedIn(user)) flatMap {
        case Some(robot) =>
          UpdateRobotForm.form.bindFromRequest.fold(
            formWithErrors => {
              Future successful BadRequest(
                views.html.robot.update(formWithErrors, robot, assetsFinder)
              )
            },
            data => {
              val success = () =>
                robotsRepo.update(robot.id, data.name, data.bio, data.openSource) map { _ =>
                  Redirect(
                    routes.RobotController.view(user.username, data.name)
                  )
                }
              if (data.name != robot.name) {
                robotsRepo.findBare(user.id, data.name)(LoggedIn(user)) flatMap {
                  case Some(_) =>
                    Future successful BadRequest(
                      views.html.robot.update(
                        UpdateRobotForm.form
                          .fill(data)
                          .withGlobalError("Robot with this name already exists"),
                        robot,
                        assetsFinder
                      )
                    )
                  case None =>
                    success()
                }
              } else {
                success()
              }
            }
          )
        case None =>
          Future successful NotFound("404")
      }
    }

  def deactivate(_username: String, name: String) =
    auth.actionForceLI { user => implicit request =>
      robotsRepo.findBare(user.id, name)(LoggedIn(user)) map {
        case Some(robot) =>
          Ok(
            views.html.robot.deactivate(robot, assetsFinder)
          )
        case None =>
          NotFound("404")
      }
    }

  def postDeactivate(id: Long) =
    auth.actionForceLI { user => implicit request =>
      robotsRepo.findBare(RobotId(id))(LoggedIn(user)) flatMap {
        case Some(robot) =>
          robotsRepo.deactivate(robot.id) map { _ =>
            Redirect(
              routes.RobotController.view(user.username, robot.name)
            )
          }
        case None =>
          Future successful NotFound("404")
      }
    }

  def viewById(id: Long) = auth.action { visitor => implicit request =>
    robotsRepo.find(RobotId(id))(visitor) map {
      case Some(FullRobot(robot, user)) =>
        Redirect(routes.RobotController.view(user.username, robot.name))
      case None => NotFound("404")
    }
  }

  def editById(id: Long) = auth.action { visitor => implicit request =>
    robotsRepo.find(RobotId(id))(visitor) map {
      case Some(FullRobot(robot, user)) =>
        Redirect(routes.RobotController.edit(user.username, robot.name))
      case None => NotFound("404")
    }
  }

  def view(username: String, name: String) =
    auth.action { visitor => implicit request =>
      robotsRepo.find(username, name)(visitor) flatMap {
        case Some(fullRobot) =>
          boardsRepo.findAllBareWithBattlesForRobot(fullRobot.robot.id, 0, 6)(visitor) map {
            boardsWithBattles =>
              Ok(
                views.html.robot.view(
                  fullRobot,
                  boardsWithBattles,
                  Visitor.isLIAsUser(visitor, fullRobot.user),
                  assetsFinder
                )
              )
          }
        case None => Future successful NotFound("404")
      }
    }

  def edit(_username: String, name: String) =
    auth.actionForceLI { user => implicit request =>
      robotsRepo.findBare(user.id, name)(LoggedIn(user)) map {
        case Some(robot) =>
          Ok(views.html.robot.edit(user, robot, assetsFinder))
        case None => NotFound("404")
      }
    }

  def apiUpdate(robotId: Long) =
    auth.actionForceLI { user => implicit request =>
      robotsRepo.findBare(RobotId(robotId))(LoggedIn(user)) flatMap {
        case Some(robot) =>
          UpdateRobotCodeForm.form.bindFromRequest.fold(
            formWithErrors => {
              Future successful BadRequest(formWithErrors.errorsAsJson)
            },
            data => {
              robotsRepo.updateDevCode(robot.id, data.code) map { _ =>
                Ok("")
              }
            }
          )
        case None => Future successful NotFound("404")
      }
    }

  def viewPublishedCode(username: String, name: String) = auth.action {
    visitor => implicit request =>
      robotsRepo.find(username, name)(LoggedOut()) flatMap {
        case Some(FullRobot(robot, _)) =>
          if (robot.userId == BUILTIN_USER) {
            Future successful Ok(views.html.robot.viewCode(robot.devCode, assetsFinder))
          } else {
            robotsRepo.getLatestPublishedCode(robot.id)(visitor) map {
              case Some(code) =>
                Ok(views.html.robot.viewCode(code, assetsFinder))
              case None => NotFound("404")
            }
          }
        case None =>
          Future successful NotFound("404")
      }
  }

  def apiGetDevCode(robotId: Long) =
    auth.actionForceLI { user => implicit request =>
      robotsRepo.find(RobotId(robotId))(LoggedIn(user)) map {
        case Some(fullRobot) =>
          Ok(Json.toJson(fullRobot.robot.devCode))
        case _ => NotFound("404")
      }
    }

  def apiGetPublishedCode(robotId: Long) =
    auth.action { visitor => implicit request =>
      robotsRepo.findBare(RobotId(robotId))(visitor) flatMap {
        case Some(r) if r.userId == BUILTIN_USER =>
          Future successful Ok(Json.toJson(r.devCode))
        case Some(_) =>
          robotsRepo.getLatestPublishedCode(RobotId(robotId))(visitor) map {
            case Some(code) => Ok(Json.toJson(code))
            case None       => NotFound("404")
          }
        case None => Future successful NotFound("404")
      }
    }

  def apiGetRobot(user: String, name: String) =
    auth.action { visitor => implicit request =>
      robotsRepo.find(user, name)(visitor) map {
        case Some(fullRobot) =>
          Ok(Json.toJson(fullRobot.robot))
        case _ => NotFound("404")
      }
    }

}
