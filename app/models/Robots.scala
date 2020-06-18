package models

import java.time.LocalDate

import javax.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.json.{Json, Reads, Writes}
import slick.jdbc.JdbcProfile
import db.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}

object Robots {
  import db.PostgresEnums.Langs.Lang

  class DataTable(tag: Tag) extends Table[Data](tag, "robots") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def userId = column[Long]("user_id")
    def name = column[String]("name")
    def devCode = column[String]("dev_code")
    def automatch = column[Boolean]("automatch")
    def isPublished = column[Boolean]("is_published")
    def rating = column[Int]("rating")
    def lang = column[Lang]("lang")
    def created = column[LocalDate]("created")
    def * =
      (id, userId, name, devCode, automatch, isPublished, rating, lang, created) <> (Data.tupled, Data.unapply)

    def user = foreignKey("user_fk", userId, TableQuery[Users.DataTable])(_.id)
  }

  private def createData(userId: Long, name: String, lang: Lang): Data = {
    Data(name = name, userId = userId, lang = lang, created = LocalDate.now())
  }

  case class Data(
      id: Long = -1,
      userId: Long,
      name: String,
      devCode: String = "",
      automatch: Boolean = true,
      isPublished: Boolean = false,
      rating: Int = 1000,
      lang: Lang,
      created: LocalDate
  )

  implicit val dataWrites = new Writes[Data] {
    def writes(data: Data) = Json.obj(
      "id" -> data.id,
      "userId" -> data.userId,
      "name" -> data.name,
      "rating" -> data.rating,
      "lang" -> data.lang
    )
  }

  class Repo @Inject()(
      protected val dbConfigProvider: DatabaseConfigProvider,
      val usersRepo: Users.Repo,
      val publishedRobotsRepo: PublishedRobots.Repo
  )(
      implicit ec: ExecutionContext
  ) extends HasDatabaseConfigProvider[JdbcProfile] {

    val schema = TableQuery[DataTable]

    def find(id: Long): Future[Option[Data]] =
      db.run(
        schema.filter(_.id === id).result.headOption
      )

    def find(userId: Long, name: String): Future[Option[Data]] =
      db.run(
        schema
          .filter(r => r.userId === userId && r.name === name)
          .result
          .headOption
      )

    def findWithUser(
        username: String,
        name: String
    ): Future[Option[(Users.Data, Data)]] = {
      val query = for {
        r <- schema if r.name === name
        u <- r.user if u.username === username
      } yield (u, r)
      db.run(query.result.headOption)
    }

    def findAll(userId: Long): Future[Seq[Data]] =
      db.run(schema.filter(_.userId === userId).result)

    def findAll(username: String): Future[Seq[Data]] = {
      val query = for {
        r <- schema
        u <- r.user if u.username === username
      } yield r
      db.run(query.result)
    }

    def findAll(
        page: Long,
        numPerPage: Int
    ): Future[Seq[(Data, Users.Data)]] = {
      val query = for {
        r <- schema
        u <- r.user
      } yield (r, u)
      db.run(Utils.paginate(query, page, numPerPage).result)
    }

    def create(userId: Long, name: String, lang: Lang): Future[Data] = {
      val data = createData(userId, name, lang)
      val id = db.run((schema returning schema.map(_.id)) += data)
      id.map(id => data.copy(id = id))
    }

    def update(id: Long, devCode: String) = {
      db.run(
        schema.filter(_.id === id).map(_.devCode).update(devCode)
      )
    }

    def publish(id: Long) = {
      db.run(
        schema.filter(_.id === id).map(_.isPublished).update(true)
          >> schema.filter(_.id === id).map(_.devCode).result.head
      ) flatMap { devCode =>
        publishedRobotsRepo.create(id, devCode)
      }
    }

    def updateRating(id: Long, rating: Int) = {
      db.run(
        schema.filter(_.id === id).map(_.rating).update(rating)
      )
    }
  }
}
