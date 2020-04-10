package models

import javax.inject.Inject
import services.Db

object Users {
  private def createData(username: String, password: String): Data = {
    Data(username, password)
  }

  case class Data(username: String, password: String, id: Long = -1)

  class Repo @Inject()(val db: Db) {

    import db.ctx._

    val schema: Quoted[EntityQuery[Data]] = quote(querySchema[Data]("users"))

    def find(username: String): Option[Data] = run(schema.filter(_.username == lift(username))).headOption

    def create(username: String, password: String): Data = {
      val data = createData(username, password)
      data.copy(id = run(schema.insert(lift(data)).returningGenerated(_.id)))
    }
  }

}


