package models

import java.time.LocalDate

import javax.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.json.{Json, Writes}
import slick.jdbc.JdbcProfile
import db.PostgresProfile.api._
import scala.language.higherKinds

import scala.concurrent.{ExecutionContext, Future}

object Robots {
  import db.PostgresEnums.Langs.Lang

  // format: off
  class DataTable(tag: Tag) extends Table[Data](tag, "robots") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def userId = column[Long]("user_id")
    def prId = column[Long]("pr_id")
    def name = column[String]("name")
    def devCode = column[String]("dev_code")
    def automatch = column[Boolean]("automatch")
    def rating = column[Int]("rating")
    def lang = column[Lang]("lang")
    def created = column[LocalDate]("created")
    def * =
      (id, userId, prId.?, name, devCode, automatch, rating, lang, created) <> (Data.tupled, Data.unapply)

    def user = foreignKey("robots_user_id_fkey", userId, TableQuery[Users.DataTable])(_.id)
    def pr = foreignKey("robots_pr_id_fkey", prId, TableQuery[PublishedRobots.DataTable])(_.id)
  }
  // format: on

  private def createData(userId: Long, name: String, lang: Lang): Data = {
    Data(name = name, userId = userId, lang = lang, created = LocalDate.now())
  }

  case class Data(
      id: Long = -1,
      userId: Long,
      prId: Option[Long] = None,
      name: String,
      devCode: String = "",
      automatch: Boolean = true,
      rating: Int = 1000,
      lang: Lang,
      created: LocalDate
  ) {
    def isPublished: Boolean = prId.isDefined
  }

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

    implicit class QueryEnrichment[C[_]](q: Query[DataTable, Data, C]) {
      def withPr(): Query[(DataTable, PublishedRobots.DataTable), (Data, PublishedRobots.Data), C] =
        q.join(publishedRobotsRepo.schema).on(_.prId === _.id)

      def findById(id: Long): Query[DataTable, Data, C] = {
        q.filter(_.id === id)
      }
    }

    val schema = TableQuery[DataTable]

    def find(id: Long): Future[Option[Data]] =
      db.run(
        schema.findById(id).result.headOption
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

    private val write = schema returning schema.map(_.id) into ((data, id) => data.copy(id))

    def create(userId: Long, name: String, lang: Lang): Future[Data] = {
      db.run(write += createData(userId, name, lang))
    }

    def update(id: Long, devCode: String) = {
      db.run(
        schema.findById(id).map(_.devCode).update(devCode)
      )
    }

    def publish(id: Long) = {
      db.run(
        for {
          code <- schema.findById(id).map(_.devCode).result.head
          pr <- publishedRobotsRepo.create(code)
          _ <- schema.findById(id).map(_.prId).update(pr.id)
        } yield ()
      )
    }

    def getPublishedCode(id: Long): Future[Option[String]] = {
      db.run(
        schema
          .findById(id)
          .withPr()
          .map(_._2.code)
          .result
          .headOption
      )
    }

    def updateRating(id: Long, rating: Int) = {
      db.run(
        schema.findById(id).map(_.rating).update(rating)
      )
    }
  }
}
