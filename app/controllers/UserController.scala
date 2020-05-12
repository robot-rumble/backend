package controllers

import com.github.t3hnar.bcrypt._
import javax.inject._
import models.{Robots, Users}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc._

@Singleton
class UserController @Inject()(
    cc: MessagesControllerComponents,
    usersRepo: Users.Repo,
    robotRepo: Robots.Repo,
    assetsFinder: AssetsFinder,
    auth: Auth,
) extends MessagesAbstractController(cc) {
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
        usersRepo.find(username) match {
          case Some(_) =>
            BadRequest(
              views.html.signup(
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

  def login: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.login(LoginForm.form, assetsFinder))
  }

  def postLogin: Action[AnyContent] = Action { implicit request =>
    LoginForm.form.bindFromRequest.fold(
      formWithErrors => {
        Forbidden(views.html.login(formWithErrors, assetsFinder))
      },
      data => {
        loginOnSuccess(data) match {
          case Left(user) =>
            Auth.login(user.username)(
              Redirect(routes.UserController.profile(user.username))
            )
          case Right(error) =>
            Forbidden(
              views.html.login(
                LoginForm.form
                  .withGlobalError(error),
                assetsFinder
              )
            )
        }
      }
    )
  }

  def apiLogin: Action[AnyContent] = Action { implicit request =>
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

  def logout: Action[AnyContent] = Action { implicit request =>
    Auth.logout(Redirect(routes.HomeController.index()))
  }

  def apiLogout: Action[AnyContent] = Action { implicit request =>
    Auth.logout(Ok(""))
  }

  def profile(username: String): Action[AnyContent] =
    auth.action(parse.anyContent) { authUser => implicit request =>
      usersRepo.find(username) match {
        case Some(user) =>
          val robots = robotRepo.findAllForUser(user)
          Ok(
            views.html
              .profile(
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
