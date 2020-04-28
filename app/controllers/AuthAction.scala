package controllers

import javax.inject._
import models.Users
import play.api.mvc._

class AuthAction @Inject()(
    cc: MessagesControllerComponents,
    usersRepo: Users.Repo
) extends MessagesAbstractController(cc) {
  def apply[T](
      parser: BodyParser[T]
  )(f: Option[Users.Data] => MessagesRequest[T] => Result): Action[T] =
    Action(parser) { implicit request =>
      request.session.get("USERNAME") match {
        case Some(username) =>
          usersRepo.find(username) match {
            case Some(user) => f(Some(user))(request)
            case None       => Forbidden("Invalid session").withNewSession
          }
        case None =>
          f(None)(request)
      }
    }

  def force[T](
      parser: BodyParser[T]
  )(f: Users.Data => MessagesRequest[T] => Result): Action[T] =
    apply(parser)(
      authUser =>
        implicit request => {
          authUser match {
            case Some(user) => f(user)(request)
            case None       => Forbidden("Not logged in")
          }
      }
    )
}
