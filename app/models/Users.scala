package models

import com.github.t3hnar.bcrypt._
import models.Schema._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class Users @Inject()(schema: Schema)(implicit ec: ExecutionContext) {
  import schema._
  import schema.ctx._

  def find(username: String): Future[Option[User]] =
    run(users.by(username)).map(_.headOption)

  def find(id: UserId): Future[Option[User]] =
    run(users.by(id)).map(_.headOption)

  def findByEmail(email: String): Future[Option[User]] =
    run(users.filter(_.email == lift(email))).map(_.headOption)

  def findByEmailOrUsername(username: String): Future[Option[User]] =
    run(users.filter(u => u.username == lift(username) || u.email == lift(username)))
      .map(_.headOption)

  def create(
      email: String,
      username: String,
      password: String
  ): Future[(User, AccountVerification)] = {
    val data = User(email, username, password)
    run(users.insert(lift(data)).returningGenerated(_.id)).map(data.copy(_)) flatMap { user =>
      createAccountVerification(user.id) map { accountVerification =>
        (user, accountVerification)
      }
    }
  }

  def createAccountVerification(id: UserId): Future[AccountVerification] = {
    val data = AccountVerification(id)
    run(accountVerifications.insert(lift(data)).returningGenerated(_.id)).map(data.copy(_))
  }

  def updatePassword(id: UserId, password: String): Future[Long] =
    run(users.by(id).update(_.password -> lift(password.bcrypt)))

  def verify(id: UserId, token: String): Future[Option[User]] =
    run(accountVerifications.filter(v => v.userId == lift(id) && v.token == lift(token)))
      .map(_.headOption) flatMap {
      case Some(_) => run(users.by(id).update(_.verified -> true).returning(u => u)).map(Some(_))
      case None    => Future successful None
    }
}
