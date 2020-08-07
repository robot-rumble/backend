package models

import javax.inject.Inject
import matchmaking.BattleQueue.MatchOutput

import scala.concurrent.{ExecutionContext, Future}
import Schema._
import io.getquill.Ord
import org.joda.time.LocalDateTime

class Battles @Inject()(
    val schema: Schema,
    val usersRepo: Users,
    val robotsRepo: Robots
)(
    implicit ec: ExecutionContext
) {
  import schema.ctx._
  import schema._

  def find(id: Long): Future[Option[Battle]] =
    run(battles.byId(id)).map(_.headOption)

  def findWithRobots(id: Long): Future[Option[(Battle, Robot, Robot)]] =
    run(battles.byId(id).withRobots()).map(_.headOption)

  def findAllPaged(page: Long, numPerPage: Int): Future[Seq[(Battle, Robot, Robot)]] = {
    val sortedBattles = quote(battles.withRobots().sortBy(_._1.created)(Ord.desc))
    run(sortedBattles.paginate(page, numPerPage))
  }

  def create(
      matchOutput: MatchOutput,
      r1Rating: Int,
      r1RatingChange: Int,
      r2Rating: Int,
      r2RatingChange: Int
  ) = {
    val battle = Battle(matchOutput, r1Rating, r1RatingChange, r2Rating, r2RatingChange)
    run(battles.insert(lift(battle)).returningGenerated(_.id)).map(battle.copy(_))
  }

  def findAllForRobot_(robotId: Long) = quote {
    val r1Battles =
      for {
        b <- battles if b.r1Id == lift(robotId)
        opponentR <- robots if opponentR.id == b.r2Id
      } yield (b, opponentR)

    val r2Battles =
      for {
        b <- battles if b.r2Id == lift(robotId)
        opponentR <- robots if opponentR.id == b.r1Id
      } yield (b, opponentR)

    (r1Battles union r2Battles).sortBy(_._1.created)(Ord.desc)
  }

  def findAllForRobot(robotId: Long): Future[Seq[(Battle, Robot)]] =
    run(findAllForRobot_(robotId))

  def findAllForRobotPaged(
      robotId: Long,
      page: Long,
      numPerPage: Int
  ): Future[Seq[(Battle, Robot)]] =
    run(findAllForRobot_(robotId).paginate(page, numPerPage))

  case class Opponent(bId: Long, rId: Long, created: LocalDateTime)

  def allOpponents(): Future[Map[Long, Seq[Opponent]]] = {
    val byP1 = quote {
      battles
        .join(robots)
        .on((b, r) => r.prId.contains(b.pr1Id))
        .groupBy { case (b, r) => (b.r1Id, r.id) }
        .map {
          case ((_pr1Id, r1Id), q) =>
            (
              r1Id,
              unquote(q.map(_._1.id).arrayAgg),
              unquote(q.map(_._1.r2Id).arrayAgg),
              unquote(q.map(_._1.created).arrayAgg),
            )
        }
    }
    val byP2 = quote {
      battles
        .join(robots)
        .on((b, r) => r.prId.contains(b.pr2Id))
        .groupBy { case (b, r) => (b.r2Id, r.id) }
        .map {
          case ((_pr1Id, r1Id), q) =>
            (
              r1Id,
              unquote(q.map(_._1.id).arrayAgg),
              unquote(q.map(_._1.r1Id).arrayAgg),
              unquote(q.map(_._1.created).arrayAgg),
            )
        }
    }
    run(byP1 union byP2) map { result =>
      result
        .foldLeft[Map[Long, List[Opponent]]](Map.empty) {
          case (acc, (rId, bIds, opponentIds, timestamps)) =>
            val opponentList = bIds.zip(opponentIds.zip(timestamps)) map {
              case (bId, (oId, timestamp)) => Opponent(bId, oId, timestamp)
            }
            acc.updated(rId, opponentList.toList ::: acc.getOrElse(rId, List.empty))
        }
    }
  }
}
