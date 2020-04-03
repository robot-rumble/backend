package controllers

import javax.inject._
import models.{Robots, Users}
import play.api.mvc._

@Singleton
class RobotController @Inject()(cc: MessagesControllerComponents, assetsFinder: AssetsFinder, authAction: AuthAction, repo: Robots.Repo, usersRepo: Users.Repo)
  extends MessagesAbstractController(cc) {

  def create: Action[AnyContent] = authAction { _ =>
    implicit request =>
      Ok(views.html.robot.create(CreateRobotForm.form, assetsFinder))
  }

  def postCreate: Action[AnyContent] = authAction { user =>
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
}
