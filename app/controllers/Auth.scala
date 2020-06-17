package controllers

import javax.inject._
import models.Users
import play.api.mvc._

import scala.concurrent.{Future, ExecutionContext}

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
)(implicit ec: ExecutionContext)
    extends MessagesAbstractController(cc) {
  def actionForce(
      f: Users.Data => MessagesRequest[AnyContent] => Future[Result]
  ): Action[AnyContent] =
    action(
      authUser =>
        implicit request => {
          authUser match {
            case Some(user) => f(user)(request)
            case None       => Future.successful(Forbidden("Not logged in"))
          }
      }
    )

  def action(
      f: Option[Users.Data] => MessagesRequest[AnyContent] => Future[Result]
  ): Action[AnyContent] =
    Action.async { implicit request =>
      request.session.get(Auth.KEY) match {
        case Some(username) =>
          usersRepo.find(username) flatMap {
            case Some(user) => f(Some(user))(request)
            case None =>
              Future.successful(Forbidden("Invalid session").withNewSession)
          }
        case None =>
          f(None)(request)
      }
    }
}
