package models

import java.time.LocalDate

import javax.inject.Inject
import services.BattleQueue.MatchOutput

import scala.concurrent.{ExecutionContext, Future}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import db.PostgresProfile.api._

object Battles {
  import db.PostgresEnums.Winners.Winner
  import db.PostgresEnums.Winners

  class DataTable(tag: Tag) extends Table[Data](tag, "battles") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def r1Id = column[Long]("r1_id")
    def r2Id = column[Long]("r2_id")
    def ranked = column[Boolean]("ranked")
    def winner = column[Winner]("winner")
    def errored = column[Boolean]("errored")
    def r1Rating = column[Int]("r1_rating")
    def r2Rating = column[Int]("r2_rating")
    def r1Time = column[Float]("r1_time")
    def r2Time = column[Float]("r2_time")
    def data = column[String]("data")
    def created = column[LocalDate]("created")
    def * =
      (
        id,
        r1Id,
        r2Id,
        ranked,
        winner,
        errored,
        r1Rating,
        r2Rating,
        r1Time,
        r2Time,
        data,
        created
      ) <> (Data.tupled, Data.unapply)
  }

  case class Data(
      id: Long = -1,
      r1Id: Long,
      r2Id: Long,
      ranked: Boolean = true,
      winner: Winner,
      errored: Boolean,
      r1Rating: Int,
      r2Rating: Int,
      r1Time: Float,
      r2Time: Float,
      data: String,
      created: LocalDate,
  )

  def didR1Win(
      battle: Data,
      r1Id: Long,
  ): Option[Boolean] = {
    battle.winner match {
      case Winners.R1 | Winners.R2 =>
        Some(battle.winner == Winners.R1 && battle.r1Id == r1Id)
      case Winners.Draw => None
    }
  }

  private def createData(
      matchOutput: MatchOutput,
      r1Rating: Int,
      r2Rating: Int
  ): Data = {
    Data(
      r1Id = matchOutput.r1Id,
      r2Id = matchOutput.r2Id,
      winner = matchOutput.winner,
      errored = matchOutput.errored,
      r1Time = matchOutput.r1Time,
      r2Time = matchOutput.r2Time,
      data = matchOutput.data,
      r1Rating = r1Rating,
      r2Rating = r2Rating,
      created = LocalDate.now()
    )
  }

  class Repo @Inject()(
      protected val dbConfigProvider: DatabaseConfigProvider,
      val usersRepo: Users.Repo,
      val robotsRepo: Robots.Repo
  )(
      implicit ec: ExecutionContext
  ) extends HasDatabaseConfigProvider[JdbcProfile] {

    val schema = TableQuery[DataTable]

    def find(id: Long): Future[Option[Data]] =
      db.run(schema.filter(_.id === id).result.headOption)

    def findWithRobots(
        id: Long
    ): Future[Option[(Data, Robots.Data, Robots.Data)]] = {
      val query = for {
        b <- schema if b.id === id
        r1 <- robotsRepo.schema if r1.id === b.r1Id
        r2 <- robotsRepo.schema if r2.id === b.r2Id
      } yield (b, r1, r2)
      db.run(query.result.headOption)
    }

    def findAllForRobot(
        robotId: Long,
        page: Long,
        numPerPage: Int
    ): Future[Seq[(Data, Robots.Data)]] = {
      val query = for {
        b <- schema
        otherR <- robotsRepo.schema
        if (
          (b.r1Id === robotId && b.r2Id === otherR.id)
            || (b.r2Id === robotId && b.r1Id === otherR.id)
        )
      } yield (b, otherR)
      db.run(Utils.paginate(query, page, numPerPage).result)
    }

    def findAll(
        page: Long,
        numPerPage: Int
    ): Future[Seq[(Data, Robots.Data, Robots.Data)]] = {
      val query =
        for {
          b <- schema
          r1 <- robotsRepo.schema if b.r1Id === r1.id
          r2 <- robotsRepo.schema if b.r2Id === r2.id
        } yield (b, r1, r2)
      db.run(Utils.paginate(query, page, numPerPage).result)
    }

    def create(matchOutput: MatchOutput, r1Rating: Int, r2Rating: Int) = {
      val data = createData(matchOutput, r1Rating, r2Rating)
      val id = db.run((schema returning schema.map(_.id)) += data)
      id.map(id => data.copy(id = id))
    }
  }
}
