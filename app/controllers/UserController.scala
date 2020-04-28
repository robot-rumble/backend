package controllers

import com.github.t3hnar.bcrypt._
import javax.inject._
import models.{Robots, Users}
import play.api.libs.json.Json
import play.api.mvc._

@Singleton
class UserController @Inject()(
    cc: MessagesControllerComponents,
    usersRepo: Users.Repo,
    robotRepo: Robots.Repo,
    assetsFinder: AssetsFinder,
    authAction: AuthAction,
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

  def login: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.login(LoginForm.form, assetsFinder))
  }

  def postLogin: Action[AnyContent] = Action { implicit request =>
    LoginForm.form.bindFromRequest.fold(
      formWithErrors => {
        Forbidden(views.html.login(formWithErrors, assetsFinder))
      },
      data => {
        usersRepo.find(data.username) match {
          case Some(user) if data.password.isBcrypted(user.password) =>
            Redirect(routes.UserController.profile(data.username))
              .withSession("USERNAME" -> user.username)
          case _ =>
            Forbidden(
              views.html.login(
                LoginForm.form
                  .withGlobalError("Incorrect username or password."),
                assetsFinder
              )
            )
        }
      }
    )
  }

  def logout: Action[AnyContent] = Action { implicit request =>
    Redirect(routes.HomeController.index()).withNewSession
  }

  def profile(username: String): Action[AnyContent] =
    authAction(parse.anyContent) { authUser => implicit request =>
      usersRepo.find(username) match {
        case Some(user) =>
          val robots = robotRepo.findAllForUser(user)
          Ok(
            views.html
              .profile(user, authUser.forall(_ == user), robots, assetsFinder)
          )
        case None => NotFound("404")
      }
    }
}
