package models

import TwitterConverters._

import java.time.LocalDate

import javax.inject.Inject
import services.BattleQueue.MatchOutput

import scala.concurrent.{ExecutionContext, Future}
import Schema._

class Battles @Inject()(
    val schema: Schema,
    val usersRepo: Users,
    val robotsRepo: Robots
)(
    implicit ec: ExecutionContext
) {
  import schema.ctx._
  import schema._

  implicit class BattleQuery(query: Quoted[EntityQuery[Battle]]) {
    def withRobots(): Quoted[Query[(Battle, Robot, Robot)]] =
      for {
        b <- battles
        r1 <- robots if b.r1Id == r1.id
        r2 <- robots if b.r2Id == r2.id
      } yield (b, r1, r2)

    def byId(id: Long): Quoted[EntityQuery[Battle]] =
      query.filter(_.id == lift(id))
  }

  def find(id: Long): Future[Option[Battle]] =
    run(battles.byId(id)).map(_.headOption)

  def findWithRobots(id: Long): Future[Option[(Battle, Robot, Robot)]] =
    run(battles.byId(id).withRobots()).map(_.headOption)

  def findAllPaged(page: Long, numPerPage: Int): Future[Seq[(Battle, Robot, Robot)]] =
    run(battles.withRobots().paginate(page, numPerPage))

  def create(matchOutput: MatchOutput, r1Rating: Int, r2Rating: Int) = {
    val battle = Battle(matchOutput, r1Rating, r2Rating)
    run(battles.insert(lift(battle)).returningGenerated(_.id)).map(battle.copy(_))
  }

//    def groupByB[F](f: ((Rep[Long], Query[BattleTable, Battles.Battle, Seq])) => F) = {
//      val r1Results = battles.groupBy(_.r1Id).map(f)
//      val r2Results = battles.groupBy(_.r2Id).map(f)
//      r1Results union r2Results
//    }
//
//    def withLatestOpponents(): Future[Seq[(Battle, Seq[Long])]] = {
//      val numOfOpponents = 5
//      run(
//        groupByB({
//          case (id, q) => (id, q.sortBy(_.created).arrayAgg)
//        }).result
//      )
//    }

//  private val withPr = for { r <- robotsRepo.battles; pr <- r.pr } yield (r, pr)
//
  private val involvesR = quote { (b: Battle, rId: Long) =>
    b.r1Id == rId || b.r2Id == rId
  }

  private val involvesPr = quote { (b: Battle, prId: Long) =>
    b.pr1Id == prId || b.pr2Id == prId
  }

  def findAllForRobotPaged(
      robotId: Long,
      page: Long,
      numPerPage: Int
  ): Future[Seq[(Battle, Robot)]] = {
    val query = quote {
      for {
        b <- battles if involvesR(b, lift(robotId))
        r <- robots if involvesR(b, r.id)
      } yield (b, r)
    }
    run(query.paginate(page, numPerPage))
  }

  //
//  def findAllStaleRobots(): Future[Seq[Robot]] = {
//    val noBattles = {
//      for {
//        (pr, b) <- withPr joinLeft battles on ((rs, b) => involvesPr(rs._2, b))
//        if b.isEmpty
//      } yield pr
//    }
//
//    val staleBattles = {
//      for {
//        (pr, b) <- withPr join battles on ((rs, b) => involvesPr(rs._2, b))
//        if b.created < LocalDate.now().minusDays(1)
//      } yield pr
//    }
//
//    run((noBattles ++ staleBattles).map(_._1).result)
//  }
}
