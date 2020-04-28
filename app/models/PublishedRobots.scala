package models

import java.time.LocalDate

import javax.inject.Inject
import services.Db

object PublishedRobots {

  private def createData(robot: Robots.Data): Data = {
    Data(robot_id = robot.id, code = robot.devCode)
  }

  case class Data(
      id: Long = -1,
      // TODO: make sure postgres sets this
      created: LocalDate = LocalDate.MIN,
      robot_id: Long,
      code: String,
  )

  class Repo @Inject()(val db: Db, val usersRepo: Users.Repo) {

    import db.ctx._

    val schema = quote(querySchema[Data]("published_robots"))

    def create(robot: Robots.Data): Data = {
      val data = createData(robot)
      val (id, created) = run(
        schema
          .insert(lift(data))
          .returningGenerated(row => (row.id, row.created))
      )
      data.copy(id = id, created = created)
    }

    def find(robot: Robots.Data): Option[Data] = {
      run(
        schema
          .filter(_.robot_id == lift(robot.id))
          .sortBy(_.created)(Ord.desc)
      ).headOption
    }
  }

}
