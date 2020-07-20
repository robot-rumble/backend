package controllers

import javax.inject._
import com.github.t3hnar.bcrypt._

import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json.Json
import play.api.mvc._
import forms.{LoginForm, PasswordResetForm, SignupForm, UpdatePasswordForm}
import models._
import Auth.Visitor
import play.api.Configuration
import services.Mail

@Singleton
class UserController @Inject()(
    cc: MessagesControllerComponents,
    usersRepo: Users,
    robotRepo: Robots,
    assetsFinder: AssetsFinder,
    auth: Auth.AuthAction,
    passwordResetRepo: PasswordResets,
    mail: Mail,
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
        (for {
          usernameUser <- usersRepo.find(username)
          emailUser <- usersRepo.findByEmail(data.email)
        } yield (usernameUser, emailUser)) flatMap {
          case (None, None) =>
            usersRepo.create(data.email, username, data.password).map { _ =>
              Redirect(routes.UserController.profile(username))
                .withSession("USERNAME" -> username)
            }
          case _ =>
            Future.successful(
              BadRequest(
                views.html.user.signup(
                  SignupForm.form.fill(data).withGlobalError("Username or email taken"),
                  assetsFinder
                )
              )
            )
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
            Auth.login(user.username)(Redirect(routes.UserController.profile(user.username)))
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

  def passwordReset = Action { implicit request =>
    Ok(views.html.user.passwordReset(PasswordResetForm.form, assetsFinder))
  }

  def postPasswordReset = Action.async { implicit request =>
    PasswordResetForm.form.bindFromRequest.fold(
      formWithErrors => {
        Future successful Forbidden(views.html.user.passwordReset(formWithErrors, assetsFinder))
      },
      data => {
        usersRepo.findByEmail(data.email) flatMap {
          case Some(user) =>
            passwordResetRepo.create(user.id) flatMap {
              passwordReset =>
                mail.mail(
                  user.email,
                  "Robot Rumble password reset",
                  s"""
                  |Hello ${user.username},
                  |
                  |You requested a robotrumble.org password reset. Please go to
                  |https://robotrumble.org${routes.UserController
                       .updatePassword()} and inputs this token exactly:
                  |${passwordReset.token}""".stripMargin
                )
            } map { _ =>
              Redirect(routes.UserController.passwordReset())
                .flashing("success" -> "Email sent!")
            }
          case None =>
            Future successful Forbidden(
              views.html.user.passwordReset(
                PasswordResetForm.form
                  .withGlobalError("User with this email does not exist"),
                assetsFinder
              )
            )
        }
      }
    )
  }

  def updatePassword = Action { implicit request =>
    Ok(views.html.user.updatePassword(UpdatePasswordForm.form, assetsFinder))
  }

  def postUpdatePassword = Action.async { implicit request =>
    UpdatePasswordForm.form.bindFromRequest.fold(
      formWithErrors => {
        Future successful Forbidden(views.html.user.updatePassword(formWithErrors, assetsFinder))
      },
      data => {
        passwordResetRepo.complete(data.token, data.password) map {
          case Some(_) =>
            Redirect(routes.UserController.updatePassword())
              .flashing("success" -> "Password updated!")
          case None =>
            Forbidden(
              views.html.user.updatePassword(
                UpdatePasswordForm.form
                  .withGlobalError("Invalid token!"),
                assetsFinder
              )
            )
        }
      }
    )
  }
}
