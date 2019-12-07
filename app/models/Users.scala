package models

import javax.inject.Inject
import services.Db

case class User(username: String, password: String, id: Long)

class Users @Inject()(val db: Db) {

  import db.ctx._

  val users: db.ctx.Quoted[db.ctx.EntityQuery[User]] = quote(querySchema[User]("users"))

  def find(username: String): Option[User] = run(users.filter(c => c.username == lift(username))).headOption

  def create(user: User): User = user.copy(id = run(users.insert(lift(user)).returning(_.id)))
}
