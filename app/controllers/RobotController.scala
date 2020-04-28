package controllers

import javax.inject._
import models.{Battles, Robots, Users}
import play.api.libs.json.{JsDefined, JsString, JsValue, Json}
import play.api.mvc._

@Singleton
class RobotController @Inject()(
    cc: MessagesControllerComponents,
    assetsFinder: AssetsFinder,
    authAction: AuthAction,
    robotsRepo: Robots.Repo,
    usersRepo: Users.Repo,
    matchesRepo: Battles.Repo
) extends MessagesAbstractController(cc) {

  def warehouse: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.robot.warehouse(robotsRepo.findAll(), assetsFinder))
  }

  def battles: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.robot.battles(matchesRepo.findAll(), assetsFinder))
  }

  def create: Action[AnyContent] = authAction.force(parse.anyContent) {
    _ => implicit request =>
      Ok(views.html.robot.create(CreateRobotForm.form, assetsFinder))
  }

  def postCreate: Action[AnyContent] = authAction.force(parse.anyContent) {
    user => implicit request =>
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
              robotsRepo.create(user.id, name)
              Redirect(routes.RobotController.view(user.username, name))
          }
        }
      )
  }

  def view(user: String, robot: String): Action[AnyContent] =
    authAction(parse.anyContent) { authUser => implicit request =>
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

  def edit(user: String, robot: String): Action[AnyContent] =
    authAction.force(parse.anyContent) { authUser => implicit request =>
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

  def update(user: String, robot: String): Action[JsValue] =
    authAction.force(parse.json) { authUser => implicit request =>
      (for {
        user <- usersRepo.find(user) if user.id == authUser.id
        robot <- robotsRepo.find(user, robot)
      } yield robot) match {
        case Some(robot) =>
          request.body \ "code" match {
            case JsDefined(code: JsString) =>
              robotsRepo.update(robot.id, code.value)
              Ok("Code updated")
            case _ => BadRequest("Missing 'code' field")
          }
        case None => NotFound("404")
      }
    }

  def viewMatch(id: String): Action[AnyContent] = TODO

  def viewPublishedCode(robotId: Long): Action[AnyContent] = Action {
    implicit request =>
      robotsRepo.getPublishedCode(robotId) match {
        case Some(publishedCode) =>
          Ok(views.html.robot.viewCode(publishedCode, assetsFinder))
        case None => NotFound("404")
      }
  }

  def challenge(user: String, robot: String): Action[AnyContent] = TODO

  def publish(user: String, robot: String): Action[AnyContent] =
    authAction.force(parse.anyContent) { authUser => implicit request =>
      (for {
        user <- usersRepo.find(user) if user.id == authUser.id
        robot <- robotsRepo.find(user, robot)
      } yield robot) match {
        case Some(robot) =>
          Ok(views.html.robot.publish(robot, assetsFinder))
        case None => NotFound("404")
      }
    }

  def postPublish(robotId: Long): Action[AnyContent] =
    authAction.force(parse.anyContent) { authUser => implicit request =>
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

  def getRobotCode(robotId: Long): Action[AnyContent] =
    authAction(parse.anyContent) { authUser => implicit request =>
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

  def getUserRobots(user: String): Action[AnyContent] =
    authAction(parse.anyContent) { authUser => implicit request =>
      usersRepo.find(user) match {
        case Some(user) =>
          val robots = robotsRepo
            .findAllForUser(user)
            .filter(
              robot => robot.isPublished || authUser.forall(_.id == user.id)
            )
          Ok(Json.obj("robots" -> robots))
        case None => NotFound("404")
      }
    }
}
