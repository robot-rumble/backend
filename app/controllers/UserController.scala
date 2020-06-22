package controllers

import javax.inject._
import com.github.t3hnar.bcrypt._
import scala.concurrent.{Future, ExecutionContext}

import play.api.libs.json.Json
import play.api.mvc._

import forms.{LoginForm, SignupForm}
import models._

import Auth.Visitor

@Singleton
class UserController @Inject()(
    cc: MessagesControllerComponents,
    usersRepo: Users,
    robotRepo: Robots,
    assetsFinder: AssetsFinder,
    auth: Auth.AuthAction
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
  ): Future[Either[Schema.User, String]] = {
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
    auth.actionForceLI { user => implicit request =>
      Future successful Ok(Json.toJson((user.username, user.id)))
    }

  def logout = Action { implicit request =>
    Auth.logout(Redirect(routes.HomeController.index()))
  }

  def profile(username: String) =
    auth.action { visitor => implicit request =>
      usersRepo.find(username) flatMap {
        case Some(user) =>
          robotRepo.findAll(user.id)(visitor) map { robots =>
            Ok(
              views.html.user.profile(
                user,
                Visitor.isLIAsUser(visitor, user),
                robots,
                assetsFinder
              )
            )
          }
        case None => Future successful NotFound("404")
      }
    }

  def apiGetUserRobots(username: String) =
    auth.action { visitor => implicit request =>
      usersRepo.find(username) flatMap {
        case Some(user) =>
          robotRepo.findAll(user.id)(visitor) map { robots =>
            Ok(Json.toJson(robots))
          }
        case None => Future successful NotFound("404")
      }
    }
}
