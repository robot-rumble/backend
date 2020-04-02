import javax.inject._
import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent.Future

class AuthAction @Inject()(parser: BodyParsers.Default)(implicit ec: scala.concurrent.ExecutionContext)
  extends ActionBuilderImpl(parser) {
  override def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] = {
    val maybeUsername = request.session.get("USERNAME")
    maybeUsername match {
      case None =>
        Future.successful(Forbidden("Not logged in."))
      case Some(u) =>
        block(request)
    }
  }
}
