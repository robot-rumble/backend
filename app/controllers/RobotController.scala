package controllers

import forms.{CreateRobotForm, UpdateRobotCodeForm}
import javax.inject._
import models.{Battles, PublishedRobots, Robots, Users}
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RobotController @Inject()(
    cc: MessagesControllerComponents,
    assetsFinder: AssetsFinder,
    auth: Auth,
    robotsRepo: Robots.Repo,
    battlesRepo: Battles.Repo
)(implicit ec: ExecutionContext)
    extends MessagesAbstractController(cc) {
  import db.PostgresEnums.Langs

  def warehouse(page: Long = 0) = Action.async { implicit request =>
    robotsRepo.findAll(page, 30) map { robots =>
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
    battlesRepo.findAll(page, 30) map { battles =>
      Ok(
        views.html.robot.battles(battles, page, assetsFinder)
      )
    }
  }

  def create =
    auth.actionForce { _ => implicit request =>
      Future successful Ok(
        views.html.robot
          .create(CreateRobotForm.form, assetsFinder)
      )
    }

  private def createOnSuccess(
      user: Users.Data,
      data: CreateRobotForm.Data
  ): Future[Either[Robots.Data, String]] = {
    val name = data.name.trim()
    robotsRepo.find(user.id, name) flatMap {
      case Some(_) =>
        Future successful Right("Robot with this name already exists")
      case None =>
        robotsRepo
          .create(user.id, name, Langs.withName(data.lang))
          .map(Left.apply)
    }
  }

  def postCreate =
    auth.actionForce { user => implicit request =>
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
    auth.actionForce { user => implicit request =>
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

  def view(username: String, robot: String, page: Long = 0) =
    auth.action { authUser => implicit request =>
      robotsRepo.findWithUser(username, robot) flatMap {
        case Some((user, robot)) if robot.isPublished || authUser.exists(_.id == user.id) =>
          battlesRepo.findAllForRobot(robot.id, page, 10) map { battles =>
            Ok(
              views.html.robot.view(
                user,
                authUser.exists(_.id == user.id),
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
    auth.actionForce { authUser => implicit request =>
      robotsRepo.find(authUser.id, robot) map {
        case Some(robot) =>
          Ok(views.html.robot.edit(authUser, robot, assetsFinder))
        case None => NotFound("404")
      }
    }

  def apiUpdate(robotId: Long) =
    auth.actionForce { authUser => implicit request =>
      robotsRepo.find(robotId) flatMap {
        case Some(robot) if robot.userId == authUser.id =>
          UpdateRobotCodeForm.form.bindFromRequest.fold(
            formWithErrors => {
              Future successful BadRequest(formWithErrors.errorsAsJson)
            },
            data => {
              robotsRepo.update(robot.id, data.code) map { _ =>
                Ok("")
              }
            }
          )
        case None => Future successful NotFound("404")
      }
    }

  def viewBattle(battleId: Long) = Action.async { implicit request =>
    battlesRepo.findWithRobots(battleId) map {
      case Some((battle, r1, r2)) =>
        Ok(views.html.robot.battle(battle, r1, r2, assetsFinder))
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

  def publish(_username: String, robot: String) =
    auth.actionForce { authUser => implicit request =>
      robotsRepo.find(authUser.id, robot) map {
        case Some(robot) =>
          Ok(views.html.robot.publish(robot, assetsFinder))
        case None => NotFound("404")
      }
    }

  def postPublish(robotId: Long) =
    auth.actionForce { authUser => implicit request =>
      robotsRepo.find(robotId) map {
        case Some(robot) if robot.userId == authUser.id =>
          robotsRepo.publish(robot.id)
          Redirect(
            routes.RobotController.view(authUser.username, robot.name)
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
    auth.action { authUser => implicit request =>
      robotsRepo.findWithUser(user, robot) map {
        // TODO: no need for user
        case Some((user, robot)) if robot.isPublished || authUser.exists(_.id == user.id) =>
          Ok(Json.toJson(robot))
        case _ => NotFound("404")
      }
    }

  def apiGetUserRobots(user: String) =
    auth.action { authUser => implicit request =>
      robotsRepo.findAll(user) map { robots =>
        val filteredRobots = robots.filter(
          robot => robot.isPublished || authUser.exists(_.id == robot.userId)
        )
        Ok(Json.toJson(filteredRobots))
      }
    }
}
