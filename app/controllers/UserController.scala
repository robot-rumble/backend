package controllers

import com.github.t3hnar.bcrypt._
import controllers.Auth.Visitor
import forms.{LoginForm, PasswordResetForm, SignupForm, UpdatePasswordForm, UpdateUserForm}
import models.Schema._
import models._
import play.api.libs.json.Json
import play.api.mvc._
import services.Mail

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

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
      data =>
        usersRepo.find(data.username) flatMap { usernameUser =>
          usersRepo.findByEmail(data.email) flatMap {
            passwordUser =>
              (usernameUser, passwordUser) match {
                case (None, None) =>
                  usersRepo.create(data.email, data.username, data.password, data.bio) flatMap {
                    case (user, accountVerification) =>
                      val link =
                        s"https://robotrumble.org${routes.UserController.verify(user.id.id, accountVerification.token)}"
                      mail.mail(
                        user.email,
                        "Robot Rumble account verification",
                        s"""
                         |Hello ${user.username},
                         |
                         |Welcome to Robot Rumble! Please go to the following link to verify your account:
                         |
                         |$link
                         |
                         |Thank you,
                         |The Robot Rumble team
                         """.stripMargin
                      ) map { _ =>
                        Redirect(routes.UserController.create())
                          .flashing(
                            "success" -> "Your account has been created! Please verify it by clicking the link sent to your inbox."
                          )
                      }
                  }
                case _ =>
                  Future successful BadRequest(
                    views.html.user.signup(
                      SignupForm.form.fill(data).withGlobalError("Username or email taken"),
                      assetsFinder
                    )
                  )
              }
          }
      }
    )
  }

  private def loginOnSuccess(
      data: LoginForm.Data
  ): Future[Either[Schema.User, String]] = {
    usersRepo.findByEmailOrUsername(data.username) map {
      case Some(user) if data.password.isBcrypted(user.password) =>
        Left(user)
      case _ =>
        Right("Incorrect email/username or password.")
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
            Auth.login(user.username)(Redirect(routes.UserController.view(user.username)))
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
      Future successful Ok(Json.toJson((user.username, user.id.id)))
    }

  def logout = Action { implicit request =>
    Auth.logout(Redirect(routes.HomeController.index()))
  }

  def view(username: String) =
    auth.action { visitor => implicit request =>
      usersRepo.find(username) flatMap {
        case Some(user) =>
          robotRepo.findAll(user.id)(visitor) map { robots =>
            Ok(
              views.html.user.view(
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
                       .updatePassword()} and input this token exactly:
                  |
                  |${passwordReset.token}
                  |
                  |Thank you,
                  |The Robot Rumble team
                  |""".stripMargin
                )
            } map { _ =>
              Redirect(routes.UserController.passwordReset())
                .flashing("success" -> "Email sent!")
            }
          case None =>
            Future successful Forbidden(
              views.html.user.passwordReset(
                PasswordResetForm.form
                  .withError("email", "User with this email does not exist"),
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

  def verify(id: Long, token: String) = Action.async { implicit request =>
    usersRepo.verify(UserId(id), token) map {
      case Some(user) => Ok(views.html.user.verified(user, assetsFinder))
      case None =>
        NotFound(
          "Incorrect token! This shouldn't happen. Please reach out to antonoutkine At gmail Dot com."
        )
    }
  }

  def update =
    auth.actionForceLI { user => implicit request =>
        Future successful Ok(
          views.html.user.update(UpdateUserForm.form.fill(UpdateUserForm.Data(user.bio)), assetsFinder)
        )
    }

  def postUpdate =
    auth.actionForceLI { user => implicit request =>
        UpdateUserForm.form.bindFromRequest.fold(
          formWithErrors => {
            Future successful BadRequest(
              views.html.user.update(formWithErrors, assetsFinder)
            )
          },
          data => {
              usersRepo.update(user.id, data.bio) map { _ =>
                Redirect(
                  routes.UserController.view(user.username)
                )
              }
          }
        )
    }

}
