package models

import javax.inject.Inject
import services.Db

object Robots {

  private def createData(user: Users.Data, name: String): Data = {
    Data(name = name, userId = user.id)
  }

  case class Data(
      id: Long = -1,
      userId: Long,
      name: String,
      devCode: String = "",
      automatch: Boolean = true,
      rating: Int = 1000,
  )

  class Repo @Inject()(val db: Db, val usersRepo: Users.Repo) {

    import db.ctx._

    val schema = quote(querySchema[Data]("robots"))

    def find(user: Users.Data, robotName: String): Option[Data] =
      run(
        schema.filter(
          r => r.userId == lift(user.id) && r.name == lift(robotName)
        )
      ).headOption

    def findById(id: Long): Option[Data] =
      run(schema.filter(_.id == lift(id))).headOption

    def findAllForUser(user: Users.Data): List[Data] =
      run(schema.filter(_.userId == lift(user.id)))

    def findAll(): List[(Data, Users.Data)] = {
      val userSchema =
        usersRepo.schema.asInstanceOf[Quoted[EntityQuery[Users.Data]]]
      run(schema.join(userSchema).on(_.userId == _.id))
    }

    def create(user: Users.Data, name: String): Data = {
      val data = createData(user, name)
      data.copy(id = run(schema.insert(lift(data)).returningGenerated(_.id)))
    }

    def update(robot: Data, code: String): Result[RunActionResult] = {
      run(
        schema.filter(_.id == lift(robot.id)).update(_.devCode -> lift(code))
      )
    }

    def random(): Option[Data] = {
      run(schema.sortBy(_ => infix"RANDOM()".as[Int])(Ord.desc)).headOption
    }
  }
}
