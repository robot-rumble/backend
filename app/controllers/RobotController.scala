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
    repo: Robots.Repo,
    usersRepo: Users.Repo,
    publishedRobotRepo: PublishedRobots.Repo,
    matchesRepo: Battles.Repo
) extends MessagesAbstractController(cc) {

  def warehouse: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.robot.warehouse(repo.findAll(), assetsFinder))
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
          repo.find(user, name) match {
            case Some(_) =>
              BadRequest(
                views.html.robot.create(
                  CreateRobotForm.form
                    .fill(data)
                    .withGlobalError("Robot with this name already exists"),
                  assetsFinder
                )
              )
            case None => {
              val robot = repo.create(user, name)
              Redirect(routes.RobotController.view(user.username, robot.name))
            }
          }
        }
      )
  }

  def view(user: String, robot: String): Action[AnyContent] = Action {
    implicit request =>
      (for {
        user <- usersRepo.find(user)
        robot <- repo.find(user, robot)
      } yield (user, robot)) match {
        case Some((user, robot)) =>
          val publishedRobot = publishedRobotRepo.find(robot)
          Ok(
            views.html.robot
              .view(
                user,
                robot,
                publishedRobot,
                matchesRepo.findForRobot(robot),
                assetsFinder
              )
          )
        case None => NotFound("Robot not found")
      }
  }

  def edit(user: String, robot: String): Action[AnyContent] =
    authAction(parse.anyContent) { authUser => implicit request =>
      (for {
        user <- usersRepo.find(user) if user == authUser
        robot <- repo.find(user, robot)
      } yield (user, robot)) match {
        case Some((user, robot)) =>
          Ok(views.html.robot.edit(user, robot, assetsFinder))
        case None => NotFound("Robot not found")
      }
    }

  def update(user: String, robot: String): Action[JsValue] =
    authAction(parse.json) { authUser => implicit request =>
      if (user == authUser.username) {
        repo.find(authUser, robot) match {
          case Some(robot) => {
            request.body \ "code" match {
              case JsDefined(code: JsString) => {
                repo.update(robot, code.value)
                Ok("Code updated")
              }
              case _ => BadRequest("Invalid 'code' field")
            }
          }
          case None => NotFound("User does not exist")
        }
      } else Forbidden("")
    }

  def viewMatch(id: String): Action[AnyContent] = TODO

  def viewCode(user: String, robot: String): Action[AnyContent] = Action {
    implicit request =>
      (for {
        user <- usersRepo.find(user)
        robot <- repo.find(user, robot)
        publishedRobot <- publishedRobotRepo.find(robot)
      } yield (robot, publishedRobot)) match {
        case Some((robot, publishedRobot)) =>
          Ok(views.html.robot.viewCode(robot, publishedRobot, assetsFinder))
        case None => NotFound("No published code found.")
      }
  }

  def challenge(user: String, robot: String): Action[AnyContent] = TODO

  def publish(user: String, robot: String): Action[AnyContent] =
    authAction(parse.anyContent) { authUser => implicit request =>
      (for {
        user <- usersRepo.find(user) if user == authUser
        robot <- repo.find(user, robot)
      } yield robot) match {
        case Some(robot) =>
          Ok(views.html.robot.publish(robot, assetsFinder))
        case None => NotFound("No published code found.")
      }
    }

  def postPublish(robot_id: Long): Action[AnyContent] =
    authAction(parse.anyContent) { authUser => implicit request =>
      (for {
        robot <- repo.find_by_id(robot_id)
        user <- usersRepo.find_by_id(robot.id) if user == authUser
      } yield (user, robot)) match {
        case Some((user, robot)) =>
          publishedRobotRepo.create(robot)
          Redirect(routes.RobotController.view(user.username, robot.name))
        case None => NotFound("404")
      }
    }
}
