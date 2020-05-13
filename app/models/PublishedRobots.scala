package models

import java.time.LocalDate

import javax.inject.Inject
import services.Db

object PublishedRobots {

  private def createData(robotId: Long, code: String): Data = {
    Data(robotId = robotId, code = code)
  }

  case class Data(
      id: Long = -1,
      created: LocalDate = LocalDate.now(),
      robotId: Long,
      code: String,
  )

  class Repo @Inject()(val db: Db, val usersRepo: Users.Repo) {

    import db.ctx._

    val schema = quote(querySchema[Data]("published_robots"))

    def find(robotId: Long): Option[Data] =
      run(
        schema
          .filter(_.robotId == lift(robotId))
          .sortBy(_.created)(Ord.desc)
      ).headOption

    def create(robotId: Long, code: String) = {
      val data = createData(robotId, code)
      val id = run(schema.insert(lift(data)).returningGenerated(_.id))
      data.copy(id = id)
    }
  }

}
