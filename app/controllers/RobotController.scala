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
    matchesRepo: Battles.Repo
) extends MessagesAbstractController(cc) {

  def warehouse = Action { implicit request =>
    Ok(views.html.robot.warehouse(robotsRepo.findAll(), assetsFinder))
  }

  def battles = Action { implicit request =>
    Ok(views.html.robot.battles(matchesRepo.findAll(), assetsFinder))
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
          val name = data.name.trim()
          robotsRepo.find(user, name) match {
            case Some(_) =>
              BadRequest(
                views.html.robot.create(
                  CreateRobotForm.form
                    .fill(data)
                    .withGlobalError("Robot with this name already exists"),
                  assetsFinder
                )
              )
            case None =>
              QuillUtils.serialize(Robots.Lang, data.lang) match {
                case Some(lang) =>
                  robotsRepo.create(user.id, name, lang)
                  Redirect(routes.RobotController.view(user.username, name))
                case None =>
                  BadRequest("Invalid lang field value.")
              }
          }
        }
      )
    }

  def view(user: String, robot: String) =
    auth.action { authUser => implicit request =>
      (for {
        user <- usersRepo.find(user)
        robot <- robotsRepo.find(user, robot)
      } yield (user, robot)) match {
        case Some((user, robot))
            if robot.isPublished || authUser.forall(_.id == user.id) =>
          Ok(
            views.html.robot.view(
              user,
              authUser.forall(_.id == user.id),
              robot,
              matchesRepo.findForRobot(robot.id),
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

  def viewMatch(id: String) = TODO

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
          Redirect(routes.RobotController.view(user.username, robot.name))
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
            if robot.isPublished || authUser.forall(_.id == user.id) =>
          val code = if (authUser.forall(_.id == user.id)) {
            robotsRepo.getDevCode(robotId)
          } else robotsRepo.getPublishedCode(robotId)
          Ok(Json.toJson(code.get))
        case None => NotFound("404")
      }
    }

  def apiGetRobotSlug(user: String, robot: String) =
    auth.action { authUser => implicit request =>
      (for {
        user <- usersRepo.find(user)
        robot <- robotsRepo.find(user, robot)
      } yield robot) match {
        case Some(robot) if robot.isPublished || authUser.forall(_.id == robot.userId) =>
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
              robot => robot.isPublished || authUser.forall(_.id == user.id)
            )
          Ok(Json.toJson(robots))
        case None => NotFound("404")
      }
    }
}
