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

  def create(email: String, username: String, password: String): Future[User] = {
    val data = User(email, username, password)
    run(users.insert(lift(data)).returningGenerated(_.id)).map(data.copy(_))
  }

  def updatePassword(id: UserId, password: String): Future[Long] =
    run(users.filter(_.id == lift(id)).update(_.password -> lift(password.bcrypt)))
}
