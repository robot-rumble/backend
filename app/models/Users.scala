package models

import javax.inject.Inject
import services.Db

object Users {

  case class Data(username: String, password: String, id: Long)

  class Repo @Inject()(val db: Db) {

    import db.ctx._

    val schema: db.ctx.Quoted[db.ctx.EntityQuery[Data]] = quote(querySchema[Data]("users"))

    def find(username: String): Option[Data] = run(schema.filter(_.username == lift(username))).headOption

    def create(username: String, password: String): Data = {
      val data = Data(username, password, -1)
      data.copy(id = run(schema.insert(lift(data)).returning(_.id)))
    }
  }

}


