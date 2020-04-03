package controllers

import javax.inject._
import models.Users
import play.api.mvc._
//
//// https://www.playframework.com/documentation/2.8.x/ScalaActionsComposition#Composing-actions
//
//case class Auth[A](action: Action[A]) extends Action[A] {
//  def apply(request: Request[A]): Future[Result] = {
//    val maybeUsername = request.session.get("USERNAME")
//    maybeUsername match {
//      case None =>
//        Future.successful(Forbidden("Not logged in."))
//      case Some(u) =>
//        action(request)
//    }
//  }
//
//  override def parser: BodyParser[A] = action.parser
//
//  override def executionContext: ExecutionContext = action.executionContext
//}

//class AuthAction @Inject()(parser: BodyParsers.Default, messages: MessagesApi, usersRepo: Users.Repo)(implicit ec: scala.concurrent.ExecutionContext)
//  extends MessagesActionBuilderImpl(parser, messages) {
//  def invokeBlock[A](request: Request[A], block: (Request[A], Users.Data) => Future[Result]): Future[Result] = {
//    request.session.get("USERNAME") match {
//      case Some(username) =>
//        usersRepo.find(username) match {
//          case Some(user) => block(request, user)
//          case None => Future.successful(Forbidden("Invalid session").withNewSession)
//        }
//      case None =>
//        Future.successful(Forbidden("Not logged in"))
//    }
//  }
//}

class AuthAction @Inject()(cc: MessagesControllerComponents, usersRepo: Users.Repo) extends MessagesAbstractController(cc) {
  def apply(f: Users.Data => MessagesRequest[AnyContent] => Result): Action[AnyContent] =
    Action { implicit request: MessagesRequest[AnyContent] =>
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

