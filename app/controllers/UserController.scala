package controllers

import com.github.t3hnar.bcrypt._
import forms.{LoginForm, SignupForm}
import javax.inject._
import models.{Robots, Users}
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.{Future, ExecutionContext}

@Singleton
class UserController @Inject()(
    cc: MessagesControllerComponents,
    usersRepo: Users.Repo,
    robotRepo: Robots.Repo,
    assetsFinder: AssetsFinder,
    auth: Auth,
)(implicit ec: ExecutionContext)
    extends MessagesAbstractController(cc) {
  def create = Action { implicit request =>
    Ok(views.html.user.signup(SignupForm.form, assetsFinder))
  }

  def postCreate = Action.async { implicit request =>
    SignupForm.form.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(
          BadRequest(views.html.user.signup(formWithErrors, assetsFinder))
        )
      },
      data => {
        val username = data.username.trim()
        usersRepo.find(username) flatMap {
          case Some(_) =>
            Future.successful(
              BadRequest(
                views.html.user.signup(
                  SignupForm.form.fill(data).withGlobalError("Username taken"),
                  assetsFinder
                )
              )
            )
          case None =>
            usersRepo.create(username, data.password).map { _ =>
              Redirect(routes.UserController.profile(username))
                .withSession("USERNAME" -> username)
            }
        }
      }
    )
  }

  private def loginOnSuccess(
      data: LoginForm.Data
  ): Future[Either[Users.Data, String]] = {
    usersRepo.find(data.username) map {
      case Some(user) if data.password.isBcrypted(user.password) =>
        Left(user)
      case _ =>
        Right("Incorrect username or password.")
    }
  }

  def login = Action { implicit request =>
    Ok(views.html.user.login(LoginForm.form, assetsFinder))
  }

  def postLogin = Action.async { implicit request =>
    LoginForm.form.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(
          Forbidden(views.html.user.login(formWithErrors, assetsFinder))
        )
      },
      data => {
        loginOnSuccess(data) map {
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

  def apiLogin = Action.async { implicit request =>
    LoginForm.form.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      data => {
        loginOnSuccess(data) map {
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

  def apiWhoami =
    auth.actionForce { authUser => implicit request =>
      Future successful Ok(Json.toJson((authUser.username, authUser.id)))
    }

  def logout = Action { implicit request =>
    Auth.logout(Redirect(routes.HomeController.index()))
  }

  def profile(username: String) =
    auth.action { authUser => implicit request =>
      usersRepo.find(username) flatMap {
        case Some(user) =>
          robotRepo.findAll(user.id) map { robots =>
            Ok(
              views.html.user.profile(
                user,
                authUser.exists(_.id == user.id),
                robots,
                assetsFinder
              )
            )
          }
        case None => Future successful NotFound("404")
      }
    }
}
