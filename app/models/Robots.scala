package models

import javax.inject.Inject
import services.Db

object Robots {

  case class Data(name: String, code: String, id: Long, user_id: Long)

  class Repo @Inject()(val db: Db, val usersRepo: Users.Repo) {

    import db.ctx._

    val schema: db.ctx.Quoted[db.ctx.EntityQuery[Data]] = quote(querySchema[Data]("robots"))

    def find(user: Users.Data, robot: String): Option[Data] = run(schema.filter(_.user_id == lift(user.id))).headOption

    def create(user: Users.Data, name: String): Data = {
      val data = Data(name, "", -1, user.id)
      data.copy(id = run(schema.insert(lift(data)).returningGenerated(_.id)))
    }

    def update(robot: Data, code: String): Result[RunActionResult] = {
      val newRobot = robot.copy(code = code)
      run(schema.update(newRobot))
    }
  }
}
