package controllers

import javax.inject._
import models.{Battles, PublishedRobots, Robots, Users}
import play.api.libs.json.{JsDefined, JsString, JsValue}
import play.api.mvc._

@Singleton
class RobotController @Inject()(
    cc: MessagesControllerComponents,
    assetsFinder: AssetsFinder,
    authAction: AuthAction,
    robotsRepo: Robots.Repo,
    usersRepo: Users.Repo,
    publishedRobotRepo: PublishedRobots.Repo,
    matchesRepo: Battles.Repo
) extends MessagesAbstractController(cc) {

  def warehouse: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.robot.warehouse(robotsRepo.findAll(), assetsFinder))
  }

  def battles: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.robot.battles(matchesRepo.findAll(), assetsFinder))
  }

  def create: Action[AnyContent] = authAction(parse.anyContent) {
    _ => implicit request =>
      Ok(views.html.robot.create(CreateRobotForm.form, assetsFinder))
  }

  def postCreate: Action[AnyContent] = authAction(parse.anyContent) {
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
              val robot = robotsRepo.create(user, name)
              Redirect(routes.RobotController.view(user.username, robot.name))
          }
        }
      )
  }

  def view(user: String, robot: String): Action[AnyContent] = Action {
    implicit request =>
      (for {
        user <- usersRepo.find(user)
        robot <- robotsRepo.find(user, robot)
      } yield (user, robot)) match {
        case Some((user, robot)) =>
          val publishedRobot = publishedRobotRepo.find(robot)
          Ok(
            views.html.robot.view(
              user,
              robot,
              publishedRobot,
              matchesRepo.findForRobot(robot),
              assetsFinder
            )
          )
        case None => NotFound("404")
      }
  }

  def edit(user: String, robot: String): Action[AnyContent] =
    authAction(parse.anyContent) { authUser => implicit request =>
      (for {
        user <- usersRepo.find(user) if user == authUser
        robot <- robotsRepo.find(user, robot)
      } yield (user, robot)) match {
        case Some((user, robot)) =>
          Ok(views.html.robot.edit(user, robot, assetsFinder))
        case None => NotFound("404")
      }
    }

  def update(user: String, robot: String): Action[JsValue] =
    authAction(parse.json) { authUser => implicit request =>
      (for {
        user <- usersRepo.find(user) if user == authUser
        robot <- robotsRepo.find(user, robot)
      } yield robot) match {
        case Some(robot) =>
          request.body \ "code" match {
            case JsDefined(code: JsString) =>
              robotsRepo.update(robot, code.value)
              Ok("Code updated")
            case _ => BadRequest("Invalid 'code' field")
          }
        case None => NotFound("404")
      }
    }

  def viewMatch(id: String): Action[AnyContent] = TODO

  def viewCode(user: String, robot: String): Action[AnyContent] = Action {
    implicit request =>
      (for {
        user <- usersRepo.find(user)
        robot <- robotsRepo.find(user, robot)
        publishedRobot <- publishedRobotRepo.find(robot)
      } yield (robot, publishedRobot)) match {
        case Some((robot, publishedRobot)) =>
          Ok(views.html.robot.viewCode(robot, publishedRobot, assetsFinder))
        case None => NotFound("404")
      }
  }

  def challenge(user: String, robot: String): Action[AnyContent] = TODO

  def publish(user: String, robot: String): Action[AnyContent] =
    authAction(parse.anyContent) { authUser => implicit request =>
      (for {
        user <- usersRepo.find(user) if user == authUser
        robot <- robotsRepo.find(user, robot)
      } yield robot) match {
        case Some(robot) =>
          Ok(views.html.robot.publish(robot, assetsFinder))
        case None => NotFound("404")
      }
    }

  def postPublish(robotId: Long): Action[AnyContent] =
    authAction(parse.anyContent) { authUser => implicit request =>
      (for {
        robot <- robotsRepo.findById(robotId)
        user <- usersRepo.findById(robot.id) if user == authUser
      } yield (user, robot)) match {
        case Some((user, robot)) =>
          publishedRobotRepo.create(robot)
          Redirect(routes.RobotController.view(user.username, robot.name))
        case None => NotFound("404")
      }
    }
}
