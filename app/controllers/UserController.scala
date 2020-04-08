package controllers

import javax.inject._
import models.{Robots, Users}
import play.api.mvc._

@Singleton
class UserController @Inject()(cc: MessagesControllerComponents, repo: Users.Repo, robotRepo: Robots.Repo, assetsFinder: AssetsFinder)
  extends MessagesAbstractController(cc) {

  def create: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.signup(SignupForm.form, assetsFinder))
  }

  def postCreate: Action[AnyContent] = Action { implicit request =>
    SignupForm.form.bindFromRequest.fold(
      formWithErrors => {
        BadRequest(views.html.signup(formWithErrors, assetsFinder))
      },
      data => {
        val username = data.username.trim()
        repo.find(username) match {
          case Some(_) => BadRequest(views.html.signup(SignupForm.form.fill(data).withGlobalError("Username taken"), assetsFinder))
          case None => {
            val user = repo.create(username, data.password)
            Redirect(routes.UserController.profile(user.username))
              .withSession("USERNAME" -> user.username)
          }
        }
      }
    )
  }

  def login: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.login(LoginForm.form, assetsFinder))
  }

  def postLogin: Action[AnyContent] = Action { implicit request =>
    LoginForm.form.bindFromRequest.fold(
      formWithErrors => {
        Forbidden(views.html.login(formWithErrors, assetsFinder))
      },
      data => {
        repo.find(data.username) match {
          case Some(user) =>
            Redirect(routes.UserController.profile(data.username))
              .withSession("USERNAME" -> user.username)
          case None =>
            Forbidden(views.html.login(LoginForm.form.withGlobalError("Incorrect username or password."), assetsFinder))
        }
      }
    )
  }

  def logout: Action[AnyContent] = Action { implicit request =>
    Redirect(routes.HomeController.index())
      .withNewSession
  }

  def profile(username: String): Action[AnyContent] = Action { implicit request =>
    repo.find(username) match {
      case Some(user) => {
        val robots = robotRepo.findAll(user)
        Ok(views.html.profile(user, robots, assetsFinder))
      }
      case None => NotFound("User does not exist!")
    }
  }
}
