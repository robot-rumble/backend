package controllers

import models.Schema._
import models.Users
import play.api.mvc._

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

object Auth {
  val KEY = "USERNAME"

  def login(username: String)(result: Result): Result = {
    result.withSession(KEY -> username)
  }

  def logout(result: Result): Result = {
    result.withNewSession
  }

  sealed trait Visitor
  final case class LoggedIn(user: User) extends Visitor
  final case class LoggedOut() extends Visitor

  object Visitor {
    def asOption(visitor: Visitor): Option[User] =
      visitor match {
        case LoggedIn(user) => Some(user)
        case LoggedOut()    => None
      }

    def isLIAsUser(visitor: Visitor, user: User): Boolean =
      Visitor.asOption(visitor).exists(_.id == user.id)
  }

  class AuthAction @Inject()(
      cc: MessagesControllerComponents,
      usersRepo: Users
  )(implicit ec: ExecutionContext)
      extends MessagesAbstractController(cc) {
    def actionForceLI(
        f: User => MessagesRequest[AnyContent] => Future[Result]
    ): Action[AnyContent] =
      action(
        visitor =>
          implicit request => {
            visitor match {
              case LoggedIn(user) => f(user)(request)
              case LoggedOut()    => Future.successful(Forbidden("Not logged in"))
            }
        }
      )

    def action(
        f: Visitor => MessagesRequest[AnyContent] => Future[Result]
    ): Action[AnyContent] =
      Action.async { implicit request =>
        request.session.get(Auth.KEY) match {
          case Some(username) =>
            usersRepo.find(username) flatMap {
              case Some(user) => f(LoggedIn(user))(request)
              case None =>
                Future.successful(Forbidden("Invalid session").withNewSession)
            }
          case None =>
            f(LoggedOut())(request)
        }
      }
  }
}
