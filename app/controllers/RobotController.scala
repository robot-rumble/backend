package controllers

import javax.inject._
import models.{Robots, Users}
import play.api.libs.json.{JsDefined, JsUndefined, JsValue}
import play.api.mvc._

@Singleton
class RobotController @Inject()(cc: MessagesControllerComponents, assetsFinder: AssetsFinder, authAction: AuthAction, repo: Robots.Repo, usersRepo: Users.Repo)
  extends MessagesAbstractController(cc) {

  def create: Action[AnyContent] = authAction(parse.anyContent) { _ =>
    implicit request =>
      Ok(views.html.robot.create(CreateRobotForm.form, assetsFinder))
  }

  def postCreate: Action[CreateRobotForm.Data] = authAction(parse.form(CreateRobotForm.form)) { user =>
    implicit request =>
      CreateRobotForm.form.bindFromRequest.fold(
        formWithErrors => {
          BadRequest(views.html.robot.create(formWithErrors, assetsFinder))
        },
        data => {
          val name = data.name.trim()
          repo.find(user, name) match {
            case Some(_) =>
              BadRequest(views.html.robot.create(CreateRobotForm.form.fill(data).withGlobalError("Robot with this name already exists"), assetsFinder))
            case None => {
              val robot = repo.create(user, name)
              Redirect(routes.RobotController.view(user.username, robot.name))
                .flashing("info" -> "Robot created!")
            }
          }
        }
      )
  }

  def view(user: String, robot: String): Action[AnyContent] = Action { implicit request =>
    (for {
      user <- usersRepo.find(user)
      robot <- repo.find(user, robot)
    } yield (user, robot)) match {
      case Some((user, robot)) => Ok(views.html.robot.view(user, robot, assetsFinder))
      case None => NotFound("Robot not found")
    }
  }

  def update(user: String, robot: String): Action[JsValue] = authAction(parse.json) { authUser =>
    implicit request =>
      if (user == authUser.username) {
        repo.find(authUser, robot) match {
          case Some(robot) => {
            request.body \ "code" match {
              case JsDefined(code) => {
                repo.update(robot, code.toString())
                Ok("Code updated")
              }
              case _: JsUndefined => BadRequest("Missing 'code' field")
            }
          }
          case None => NotFound("User does not exist")
        }
      } else Forbidden("")
  }
}
