package controllers

import forms.{CreateRobotForm, UpdateRobotCodeForm}
import javax.inject._
import models.{Battles, QuillUtils, Robots, Users}
import play.api.libs.json.Json
import play.api.mvc._

@Singleton
class RobotController @Inject()(
    cc: MessagesControllerComponents,
    assetsFinder: AssetsFinder,
    auth: Auth,
    robotsRepo: Robots.Repo,
    usersRepo: Users.Repo,
    battlesRepo: Battles.Repo
) extends MessagesAbstractController(cc) {

  def warehouse(page: Long = 0) = Action { implicit request =>
    Ok(
      views.html.robot.warehouse(
        robotsRepo.findAll(page, 30),
        assetsFinder
      )
    )
  }

  def battles(page: Long = 0) = Action { implicit request =>
    Ok(
      views.html.robot.battles(
        battlesRepo.findAll(page, 30),
        assetsFinder
      )
    )
  }

  def create =
    auth.actionForce { _ => implicit request =>
      Ok(views.html.robot.create(CreateRobotForm.form, assetsFinder))
    }

  def postCreate =
    auth.actionForce { user => implicit request =>
      CreateRobotForm.form.bindFromRequest.fold(
        formWithErrors => {
          BadRequest(views.html.robot.create(formWithErrors, assetsFinder))
        },
        data => {
          createOnSuccess(user, data) match {
            case Left(robot) =>
              Redirect(
                routes.RobotController.view(user.username, robot.name, 0)
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
          BadRequest(formWithErrors.errorsAsJson)
        },
        data => {
          createOnSuccess(user, data) match {
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

  private def createOnSuccess(
      user: Users.Data,
      data: CreateRobotForm.Data
  ): Either[Robots.BasicData, String] = {
    val name = data.name.trim()
    robotsRepo.find(user, name) match {
      case Some(_) => Right("Robot with this name already exists")
      case None =>
        QuillUtils.serialize(Robots.Lang, data.lang) match {
          case Some(lang) =>
            Left(robotsRepo.create(user.id, name, lang))
          case None =>
            Right("Invalid lang field value.")
        }
    }
  }

  def view(user: String, robot: String, page: Long = 0) =
    auth.action { authUser => implicit request =>
      (for {
        user <- usersRepo.find(user)
        robot <- robotsRepo.find(user, robot)
      } yield (user, robot)) match {
        case Some((user, robot))
            if robot.isPublished || authUser.exists(_.id == user.id) =>
          Ok(
            views.html.robot.view(
              user,
              authUser.exists(_.id == user.id),
              robot,
              battlesRepo.findForRobot(robot.id, page, 10),
              assetsFinder
            )
          )
        case None => NotFound("404")
      }
    }

  def edit(user: String, robot: String) =
    auth.actionForce { authUser => implicit request =>
      (for {
        user <- usersRepo.find(user) if user.id == authUser.id
        robot <- robotsRepo.find(user, robot)
        code <- robotsRepo.getDevCode(robot.id)
      } yield (user, robot, code)) match {
        case Some((user, robot, code)) =>
          Ok(views.html.robot.edit(user, robot, code, assetsFinder))
        case None => NotFound("404")
      }
    }

  def apiUpdate(robotId: Long) =
    auth.actionForce { authUser => implicit request =>
      (for {
        robot <- robotsRepo.findById(robotId)
      } yield robot) match {
        case Some(robot) if robot.userId == authUser.id =>
          UpdateRobotCodeForm.form.bindFromRequest.fold(
            formWithErrors => {
              BadRequest(formWithErrors.errorsAsJson)
            },
            data => {
              robotsRepo.update(robot.id, data.code)
              Ok("")
            }
          )
        case None => NotFound("404")
      }
    }

  def viewBattle(battleId: Long) = Action { implicit request =>
    battlesRepo.find(battleId) match {
      case Some(battle) =>
        (for {
          r1 <- robotsRepo.findById(battle.r1Id)
          r2 <- robotsRepo.findById(battle.r2Id)
        } yield (r1, r2)) match {
          case Some((r1, r2)) =>
            Ok(views.html.robot.battle(battle, r1, r2, assetsFinder))
          case None => InternalServerError("Something bad has happened.")
        }

      case None => NotFound("404")
    }
  }

  def viewPublishedCode(robotId: Long) = Action { implicit request =>
    robotsRepo.getPublishedCode(robotId) match {
      case Some(publishedCode) =>
        Ok(views.html.robot.viewCode(publishedCode, assetsFinder))
      case None => NotFound("404")
    }
  }

  def challenge(user: String, robot: String) = TODO

  def publish(user: String, robot: String) =
    auth.actionForce { authUser => implicit request =>
      (for {
        user <- usersRepo.find(user) if user.id == authUser.id
        robot <- robotsRepo.find(user, robot)
      } yield robot) match {
        case Some(robot) =>
          Ok(views.html.robot.publish(robot, assetsFinder))
        case None => NotFound("404")
      }
    }

  def postPublish(robotId: Long) =
    auth.actionForce { authUser => implicit request =>
      (for {
        robot <- robotsRepo.findById(robotId)
        user <- usersRepo.findById(robot.userId) if user.id == authUser.id
      } yield (user, robot)) match {
        case Some((user, robot)) =>
          robotsRepo.publish(robot.id)
          Redirect(routes.RobotController.view(user.username, robot.name, 0))
        case None => NotFound("404")
      }
    }

  def apiGetRobotCode(robotId: Long) =
    auth.action { authUser => implicit request =>
      (for {
        robot <- robotsRepo.findById(robotId)
        user <- usersRepo.findById(robot.userId)
      } yield (user, robot)) match {
        case Some((user, robot))
            if robot.isPublished || authUser.exists(_.id == user.id) =>
          val code = if (authUser.exists(_.id == user.id)) {
            robotsRepo.getDevCode(robotId)
          } else robotsRepo.getPublishedCode(robotId)
          Ok(Json.toJson(code.get))
        case None => NotFound("404")
      }
    }

  def apiGetRobot(user: String, robot: String) =
    auth.action { authUser => implicit request =>
      (for {
        user <- usersRepo.find(user)
        robot <- robotsRepo.find(user, robot)
      } yield robot) match {
        case Some(robot)
            if robot.isPublished || authUser.exists(_.id == robot.userId) =>
          Ok(Json.toJson(robot))
        case _ => NotFound("404")
      }
    }

  def apiGetUserRobots(user: String) =
    auth.action { authUser => implicit request =>
      usersRepo.find(user) match {
        case Some(user) =>
          val robots = robotsRepo
            .findAllForUser(user)
            .filter(
              robot => robot.isPublished || authUser.exists(_.id == user.id)
            )
          Ok(Json.toJson(robots))
        case None => NotFound("404")
      }
    }
}
