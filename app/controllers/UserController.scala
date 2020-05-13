package controllers

import com.github.t3hnar.bcrypt._
import forms.{LoginForm, SignupForm}
import javax.inject._
import models.{Robots, Users}
import play.api.mvc._

@Singleton
class UserController @Inject()(
    cc: MessagesControllerComponents,
    usersRepo: Users.Repo,
    robotRepo: Robots.Repo,
    assetsFinder: AssetsFinder,
    auth: Auth,
) extends MessagesAbstractController(cc) {
  def create = Action { implicit request =>
    Ok(views.html.user.signup(SignupForm.form, assetsFinder))
  }

  def postCreate = Action { implicit request =>
    SignupForm.form.bindFromRequest.fold(
      formWithErrors => {
        BadRequest(views.html.user.signup(formWithErrors, assetsFinder))
      },
      data => {
        val username = data.username.trim()
        usersRepo.find(username) match {
          case Some(_) =>
            BadRequest(
              views.html.user.signup(
                SignupForm.form.fill(data).withGlobalError("Username taken"),
                assetsFinder
              )
            )
          case None =>
            usersRepo.create(username, data.password)
            Redirect(routes.UserController.profile(username))
              .withSession("USERNAME" -> username)
        }
      }
    )
  }

  private def loginOnSuccess(
      data: LoginForm.Data
  ): Either[Users.Data, String] = {
    usersRepo.find(data.username) match {
      case Some(user) if data.password.isBcrypted(user.password) =>
        Left(user)
      case _ =>
        Right("Incorrect username or password.")
    }
  }

  def login = Action { implicit request =>
    Ok(views.html.user.login(LoginForm.form, assetsFinder))
  }

  def postLogin = Action { implicit request =>
    LoginForm.form.bindFromRequest.fold(
      formWithErrors => {
        Forbidden(views.html.user.login(formWithErrors, assetsFinder))
      },
      data => {
        loginOnSuccess(data) match {
          case Left(user) =>
            Auth.login(user.username)(
              Redirect(routes.UserController.profile(user.username))
            )
          case Right(error) =>
            Forbidden(
              views.html.user.login(
                LoginForm.form
                  .withGlobalError(error),
                assetsFinder
              )
            )
        }
      }
    )
  }

  def apiLogin = Action { implicit request =>
    LoginForm.form.bindFromRequest.fold(
      formWithErrors => {
        BadRequest(formWithErrors.errorsAsJson)
      },
      data => {
        loginOnSuccess(data) match {
          case Left(user) =>
            Auth.login(user.username)(Ok(""))
          case Right(error) =>
            Forbidden(
              LoginForm.form
                .withGlobalError(error)
                .errorsAsJson
            )
        }
      }
    )
  }

  def logout = Action { implicit request =>
    Auth.logout(Redirect(routes.HomeController.index()))
  }

  def profile(username: String) =
    auth.action { authUser => implicit request =>
      usersRepo.find(username) match {
        case Some(user) =>
          val robots = robotRepo.findAllForUser(user)
          Ok(
            views.html.user.profile(
              user,
              authUser.forall(_.id == user.id),
              robots,
              assetsFinder
            )
          )
        case None => NotFound("404")
      }
    }
}
