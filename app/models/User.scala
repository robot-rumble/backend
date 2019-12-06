package models

import javax.inject.Inject
import services.Db

class User @Inject()(val db: Db) {
  case class User (id: Long, username: String)

  import db.ctx._

  val users = quote(querySchema[User]("users"))

  def find(id: Long) = run(users.filter(c => c.id == lift(id))).headOption

  def create(user: User) = user.copy(id = run(users.insert(lift(user)).returning(_.id)))

  def delete(user: User) = run(users.filter(_.id == lift(user.id)).delete)

  def update(user: User) = run(users.filter(_.id == lift(user.id)).update(lift(user)))
}
