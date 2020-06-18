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

  // format: off
  class DataTable(tag: Tag) extends Table[Data](tag, "battles") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def r1Id = column[Long]("r1_id")
    def r2Id = column[Long]("r2_id")
    def pr1Id = column[Long]("pr1_id")
    def pr2Id = column[Long]("pr2_id")
    def ranked = column[Boolean]("ranked")
    def winner = column[Winner]("winner")
    def errored = column[Boolean]("errored")
    def r1Rating = column[Int]("r1_rating")
    def r2Rating = column[Int]("r2_rating")
    def r1Time = column[Float]("r1_time")
    def r2Time = column[Float]("r2_time")
    def data = column[String]("data")
    def created = column[LocalDate]("created")
    def * = (id, r1Id, r2Id, pr1Id, pr2Id, ranked, winner, errored, r1Rating, r2Rating, r1Time, r2Time, data, created) <> (Data.tupled, Data.unapply)

    def r1 = foreignKey("battles_r1_id_fkey", r1Id, TableQuery[Robots.DataTable])(_.id)
    def r2 = foreignKey("battles_r2_id_fkey", r2Id, TableQuery[Robots.DataTable])(_.id)
    def pr1 = foreignKey("battles_pr1_id_fkey", r1Id, TableQuery[PublishedRobots.DataTable])(_.id)
    def pr2 = foreignKey("battles_pr2_id_fkey", r2Id, TableQuery[PublishedRobots.DataTable])(_.id)
  }
  // format: on

  case class Data(
      id: Long = -1,
      r1Id: Long,
      r2Id: Long,
      pr1Id: Long,
      pr2Id: Long,
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
      pr1Id = matchOutput.pr1Id,
      r2Id = matchOutput.r2Id,
      pr2Id = matchOutput.pr2Id,
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
        r1 <- b.r1
        r2 <- b.r2
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
        r <- robotsRepo.schema
        if (
          (b.r1Id === robotId && b.r2Id === r.id)
            || (b.r2Id === robotId && b.r1Id === r.id)
        )
      } yield (b, r)
      db.run(Utils.paginate(query, page, numPerPage).result)
    }

    def findAll(
        page: Long,
        numPerPage: Int
    ): Future[Seq[(Data, Robots.Data, Robots.Data)]] = {
      val query =
        for {
          b <- schema
          r1 <- b.r1
          r2 <- b.r2
        } yield (b, r1, r2)
      db.run(Utils.paginate(query, page, numPerPage).result)
    }

    val write = schema returning schema.map(_.id) into ((data, id) => data.copy(id))

    def create(matchOutput: MatchOutput, r1Rating: Int, r2Rating: Int) =
      db.run(write += createData(matchOutput, r1Rating, r2Rating))

//    def groupByB[F](f: ((Rep[Long], Query[Battles.DataTable, Battles.Data, Seq])) => F) = {
//      val r1Results = schema.groupBy(_.r1Id).map(f)
//      val r2Results = schema.groupBy(_.r2Id).map(f)
//      r1Results union r2Results
//    }
//
//    def withLatestOpponents(): Future[Seq[(Data, Seq[Long])]] = {
//      val numOfOpponents = 5
//      db.run(
//        groupByB({
//          case (id, q) => (id, q.sortBy(_.created).arrayAgg)
//        }).result
//      )
//    }

    private val withPr = for { r <- robotsRepo.schema; pr <- r.pr } yield (r, pr)

    private val bJoinOnR = (
        r: DataTable,
        b: Battles.DataTable
    ) => b.r1Id === r.id || b.r2Id === r.id

    val bJoinOnPr = (
        pr: PublishedRobots.DataTable,
        b: Battles.DataTable
    ) => b.pr1Id === pr.id || b.pr2Id === pr.id

    def findAllStaleRobots(): Future[Seq[Robots.Data]] = {
      val noBattles = {
        for {
          (pr, b) <- withPr joinLeft schema on ((rs, b) => bJoinOnPr(rs._2, b))
          if b.isEmpty
        } yield pr
      }

      val staleBattles = {
        for {
          (pr, b) <- withPr join schema on ((rs, b) => bJoinOnPr(rs._2, b))
          if b.created < LocalDate.now().minusDays(1)
        } yield pr
      }

      db.run((noBattles ++ staleBattles).map(_._1).result)
    }
  }
}
