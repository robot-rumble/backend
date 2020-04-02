package controllers

import javax.inject._
import models.Users
import play.api.mvc._

@Singleton
class UserController @Inject()(cc: MessagesControllerComponents, repo: Users.Repo, assetsFinder: AssetsFinder)
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
        val user = Users.Data(username = data.username, password = data.password, id = 0)
        repo.find(user.username) match {
          case Some(_) => BadRequest(views.html.signup(SignupForm.form.fill(data).withGlobalError("Username taken"), assetsFinder))
          case None => {
            repo.create(user)
            Redirect(routes.UserController.profile(user.username))
              .flashing("info" -> "Account created!")
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
              .flashing("info" -> "Logged in!")
              .withSession("USERNAME" -> data.username)
          case None =>
            Forbidden(views.html.login(LoginForm.form.withGlobalError("Incorrect username or password."), assetsFinder))
        }
      }
    )
  }

  def logout: Action[AnyContent] = Action { implicit request =>
    Redirect(routes.HomeController.index())
      .withNewSession
      .flashing("info" -> "Logged out!")
  }

  def profile(username: String): Action[AnyContent] = Action { implicit request =>
    repo.find(username) match {
      case Some(user) => Ok(views.html.profile(user, assetsFinder))
      case None => NotFound("User does not exist!")
    }
  }
}
