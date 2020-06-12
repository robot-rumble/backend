package models

import java.time.LocalDate

import com.github.t3hnar.bcrypt._
import javax.inject.Inject
import services.Db

object Users {
  private def createData(username: String, password: String): Data = {
    Data(username, password.bcrypt, created = LocalDate.now())
  }

  case class Data(
      username: String,
      password: String,
      id: Long = -1,
      created: LocalDate
  )

  class Repo @Inject()(val db: Db) {

    import db.ctx._

    val schema = quote(querySchema[Data]("users"))

    def find(username: String): Option[Data] =
      run(schema.filter(_.username == lift(username))).headOption

    def findById(id: Long): Option[Data] =
      run(schema.filter(_.id == lift(id))).headOption

    def create(username: String, password: String) = {
      val data = createData(username, password)
      val id = run(schema.insert(lift(data)).returningGenerated(_.id))
      data.copy(id = id)
    }
  }

}
