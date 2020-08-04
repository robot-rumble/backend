package controllers

import controllers.Auth.{LoggedIn, LoggedOut, Visitor}
import javax.inject._

import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json.Json
import play.api.mvc._
import forms.{CreateRobotForm, UpdateRobotCodeForm}
import models._
import models.Schema._

@Singleton
class RobotController @Inject()(
    cc: MessagesControllerComponents,
    assetsFinder: AssetsFinder,
    auth: Auth.AuthAction,
    robotsRepo: Robots,
    battlesRepo: Battles,
    usersRepo: Users
)(implicit ec: ExecutionContext)
    extends MessagesAbstractController(cc) {

  def warehouse(page: Long = 0) = Action.async { implicit request =>
    robotsRepo.findAllPublishedPaged(page, 30) map { robots =>
      Ok(
        views.html.robot.warehouse(
          robots,
          page,
          assetsFinder
        )
      )
    }
  }

  def battles(page: Long = 0) = Action.async { implicit request =>
    battlesRepo.findAllPaged(page, 30) map { battles =>
      Ok(
        views.html.robot.battles(battles, page, assetsFinder)
      )
    }
  }

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
    robotsRepo.find(user.id, name)(LoggedIn(user)) flatMap {
      case Some(_) =>
        Future successful Right("Robot with this name already exists")
      case None =>
        robotsRepo.create(user.id, name, data.lang).map(Left.apply)
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
    robotsRepo.find(id)(visitor) flatMap {
      case Some(robot) =>
        usersRepo.find(robot.userId) map {
          case Some(user) => Redirect(routes.RobotController.view(user.username, robot.name))
          case None       => NotFound("404")
        }
      case None => Future successful NotFound("404")
    }
  }

  def view(username: String, robot: String, page: Long = 0) =
    auth.action { visitor => implicit request =>
      robotsRepo.find(username, robot)(visitor) flatMap {
        case Some((robot, user)) =>
          battlesRepo.findAllForRobotPaged(robot.id, page, 10) map { battles =>
            Ok(
              views.html.robot.view(
                user,
                Visitor.isLIAsUser(visitor, user),
                robot,
                battles,
                page,
                assetsFinder
              )
            )
          }
        case None => Future successful NotFound("404")
      }
    }

  def edit(_username: String, robot: String) =
    auth.actionForceLI { user => implicit request =>
      robotsRepo.find(user.id, robot)(LoggedIn(user)) map {
        case Some(robot) =>
          Ok(views.html.robot.edit(user, robot, assetsFinder))
        case None => NotFound("404")
      }
    }

  def apiUpdate(robotId: Long) =
    auth.actionForceLI { visitor => implicit request =>
      robotsRepo.find(robotId)(LoggedIn(visitor)) flatMap {
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

  def viewBattle(battleId: Long) = auth.action { visitor => implicit request =>
    battlesRepo.findWithRobots(battleId) map {
      case Some((battle, r1, r2)) =>
        val userTeam = visitor match {
          case LoggedIn(user) =>
            if (r1.userId == user.id) {
              Some("Blue")
            } else if (r2.userId == user.id) {
              Some("Red")
            } else { None }
          case LoggedOut() => None
        }
        Ok(views.html.robot.battle(battle, r1, r2, userTeam, assetsFinder))
      case None => NotFound("404")
    }
  }

  def viewPublishedCode(robotId: Long) = Action.async { implicit request =>
    robotsRepo.getPublishedCode(robotId) map {
      case Some(code) =>
        Ok(views.html.robot.viewCode(code, assetsFinder))
      case None => NotFound("404")
    }
  }

  def challenge(user: String, robot: String) = TODO

  def publish(robotId: Long) =
    auth.actionForceLI { user => implicit request =>
      robotsRepo.find(robotId)(LoggedIn(user)) map {
        case Some(robot) =>
          Ok(views.html.robot.publish(robot, assetsFinder))
        case None => NotFound("404")
      }
    }

  def postPublish(robotId: Long) =
    auth.actionForceLI { user => implicit request =>
      robotsRepo.find(robotId)(LoggedIn(user)) map {
        case Some(robot) =>
          robotsRepo.publish(robot.id)
          Redirect(
            routes.RobotController.view(user.username, robot.name)
          )
        case None => NotFound("404")
      }
    }

  def apiGetRobotCode(robotId: Long) =
    Action.async { implicit request =>
      robotsRepo.getPublishedCode(robotId) map {
        case Some(code) => Ok(Json.toJson(code))
        case None       => NotFound("404")
      }
    }

  def apiGetRobot(user: String, robot: String) =
    auth.action { visitor => implicit request =>
      robotsRepo.find(user, robot)(visitor) map {
        case Some((robot, _)) =>
          Ok(Json.toJson(robot))
        case _ => NotFound("404")
      }
    }

}
