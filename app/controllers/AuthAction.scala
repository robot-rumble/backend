package controllers

import javax.inject._
import models.Users
import play.api.mvc._

class AuthAction @Inject()(cc: MessagesControllerComponents, usersRepo: Users.Repo) extends MessagesAbstractController(cc) {
  def apply[T](parser: BodyParser[T])(f: Users.Data => MessagesRequest[T] => Result): Action[T] =
    Action(parser) { implicit request =>
      request.session.get("USERNAME") match {
        case Some(username) =>
          usersRepo.find(username) match {
            case Some(user) => f(user)(request)
            case None => Forbidden("Invalid session").withNewSession
          }
        case None =>
          Forbidden("Not logged in")
      }
    }
}

