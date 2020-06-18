package models

import java.time.LocalDate

import db.PostgresProfile.api._

object PublishedRobots {

  // format: off
  class DataTable(tag: Tag) extends Table[Data](tag, "published_robots") {
    def * = (id, created, code) <> (Data.tupled, Data.unapply)
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def created = column[LocalDate]("created")
    def code = column[String]("code")
  }

  // format: on
  case class Data(id: Long = -1, created: LocalDate, code: String)

  private def createData(code: String): Data = {
    Data(created = LocalDate.now(), code = code)
  }

  class Repo {
    val schema = TableQuery[DataTable]

    private val write = schema returning schema.map(_.id) into ((data, id) => data.copy(id))

    def create(code: String): DBIO[Data] = write += createData(code)
  }

}
