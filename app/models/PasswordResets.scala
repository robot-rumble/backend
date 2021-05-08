package models

import models.Schema._
import org.joda.time.LocalDateTime

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PasswordResets @Inject()(schema: Schema, usersRepo: models.Users)(
    implicit ec: ExecutionContext
) {
  import schema._
  import schema.ctx._

  def create(userId: UserId): Future[PasswordReset] = {
    val data = PasswordReset(userId)
    run(passwordResets.insert(lift(data)).returningGenerated(_.id)).map(data.copy(_))
  }

  def complete(token: String, newPassword: String): Future[Option[Long]] = {
    run(
      passwordResets
        .filter(
          v => v.token == lift(token) && v.created > lift(LocalDateTime.now().minusMinutes(15))
        )
    ).map(_.headOption) flatMap {
      case Some(v) => usersRepo.updatePassword(v.userId, newPassword).map(Some(_))
      case None    => Future.successful(None)
    }
  }
}
