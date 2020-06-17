package models

import java.time.LocalDate

import com.github.t3hnar.bcrypt._
import javax.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import db.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}

object Users {
  class DataTable(tag: Tag) extends Table[Data](tag, "users") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def username = column[String]("username")
    def password = column[String]("password")
    def created = column[LocalDate]("created")
    def * = (id, username, password, created) <> (Data.tupled, Data.unapply)
  }

  case class Data(
      id: Long = -1,
      username: String,
      password: String,
      created: LocalDate
  )

  private def createData(username: String, password: String): Data = {
    Data(
      username = username,
      password = password.bcrypt,
      created = LocalDate.now()
    )
  }

  class Repo @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(
      implicit ec: ExecutionContext
  ) extends HasDatabaseConfigProvider[JdbcProfile] {

    val schema = TableQuery[DataTable]

    def find(username: String): Future[Option[Data]] =
      db.run(schema.filter(_.username === username).result.headOption)

    def find(id: Long): Future[Option[Data]] =
      db.run(schema.filter(_.id === id).result.headOption)

    def create(username: String, password: String): Future[Data] = {
      val data = createData(username, password)
      val id = db.run((schema returning schema.map(_.id)) += data)
      id.map(id => data.copy(id = id))
    }
  }

}
