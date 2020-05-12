package controllers

import javax.inject._
import models.Users
import play.api.mvc._

object Auth {
  val KEY = "USERNAME"

  def login(username: String)(result: Result): Result = {
    result.withSession(KEY -> username)
  }

  def logout(result: Result): Result = {
    result.withNewSession
  }
}

class Auth @Inject()(
    cc: MessagesControllerComponents,
    usersRepo: Users.Repo
) extends MessagesAbstractController(cc) {
  def actionForce(
      f: Users.Data => MessagesRequest[AnyContent] => Result
  ): Action[AnyContent] =
    action(
      authUser =>
        implicit request => {
          authUser match {
            case Some(user) => f(user)(request)
            case None       => Forbidden("Not logged in")
          }
      }
    )

  def action(
      f: Option[Users.Data] => MessagesRequest[AnyContent] => Result
  ): Action[AnyContent] =
    Action { implicit request =>
      request.session.get(Auth.KEY) match {
        case Some(username) =>
          usersRepo.find(username) match {
            case Some(user) => f(Some(user))(request)
            case None       => Forbidden("Invalid session").withNewSession
          }
        case None =>
          f(None)(request)
      }
    }
}
