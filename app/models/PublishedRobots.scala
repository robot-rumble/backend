package models

import java.time.LocalDate

import javax.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import db.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}

object PublishedRobots {

  class DataTable(tag: Tag) extends Table[Data](tag, "published_robots") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def created = column[LocalDate]("created")
    def robotId = column[Long]("robot_id")
    def code = column[String]("code")
    def * =
      (id, created, robotId, code) <> (Data.tupled, Data.unapply)

    def robot =
      foreignKey("robot_fk", robotId, TableQuery[Robots.DataTable])(_.id)
  }

  case class Data(
      id: Long = -1,
      created: LocalDate,
      robotId: Long,
      code: String,
  )

  private def createData(robotId: Long, code: String): Data = {
    Data(created = LocalDate.now(), robotId = robotId, code = code)
  }

  class Repo @Inject()(
      protected val dbConfigProvider: DatabaseConfigProvider,
      val usersRepo: Users.Repo,
  )(
      implicit ec: ExecutionContext
  ) extends HasDatabaseConfigProvider[JdbcProfile] {

    val schema = TableQuery[DataTable]

    def findLatest(robotId: Long): Future[Option[Data]] =
      db.run(
        schema
          .filter(_.robotId === robotId)
          .sortBy(_.created.desc)
          .result
          .headOption
      )

    def create(robotId: Long, code: String): Future[Data] = {
      val data = createData(robotId, code)
      val id = db.run((schema returning schema.map(_.id)) += data)
      id.map(id => data.copy(id = id))
    }
  }

}
