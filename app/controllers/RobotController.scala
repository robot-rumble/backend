package controllers

import controllers.Auth.{LoggedIn, LoggedOut, Visitor}
import forms.{CreateRobotForm, UpdateRobotCodeForm}
import models.Schema._
import models._
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
)(implicit ec: ExecutionContext)
    extends MessagesAbstractController(cc) {

  def create =
    auth.actionForceLI { _ => implicit request =>
      Future successful Ok(
        views.html.robot
          .create(CreateRobotForm.form, assetsFinder)
      )
    }

  private def createOnSuccess(
      user: Schema.User,
      data: CreateRobotForm.Data
  ): Future[Either[Robot, String]] = {
    val name = data.name.trim()
    if (name.matches("^[a-zA-Z0-9_-]+$")) {
      robotsRepo.findBare(user.id, name)(LoggedIn(user)) flatMap {
        case Some(_) =>
          Future successful Right("Robot with this name already exists")
        case None =>
          robotsRepo.create(user.id, name, data.lang).map(Left.apply)
      }
    } else {
      Future successful Right("Name cannot contain special characters")
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
          boardsRepo.findAllBareWithBattlesForRobot(fullRobot.robot.id, 0, 6) map {
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
    auth.actionForceLI { visitor => implicit request =>
      robotsRepo.findBare(RobotId(robotId))(LoggedIn(visitor)) flatMap {
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

  def viewPublishedCode(username: String, name: String) = Action.async { implicit request =>
    robotsRepo.find(username, name)(LoggedOut()) flatMap {
      case Some(FullRobot(robot, _user)) =>
        robotsRepo.getLatestPublishedCode(robot.id) map {
          case Some(code) =>
            Ok(views.html.robot.viewCode(code, assetsFinder))
          case None => NotFound("404")
        }
      case None =>
        Future successful NotFound("404")
    }
  }

  def apiGetRobotCode(robotId: Long) =
    auth.action { visitor => implicit request =>
      robotsRepo.find(RobotId(robotId))(visitor) flatMap {
        case Some(fullRobot) =>
          // if the logged in user owns this robot, return the robot's development code
          // otherwise return its most recent published code
          // I don't like that this behaviour is 'hidden under the hood', but it's what's most intuitive
          // someone working in the Garage should be able to instantly see dev code changes reflected
          // in battles against their own bots
          if (Visitor.isLIAsUser(visitor, fullRobot.user)) {
            Future successful Ok(Json.toJson(fullRobot.robot.devCode))
          } else {
            robotsRepo.getLatestPublishedCode(RobotId(robotId)) map {
              case Some(code) => Ok(Json.toJson(code))
              case None       => NotFound("404")
            }
          }
        case _ => Future successful NotFound("404")
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
