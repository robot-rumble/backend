package models

import javax.inject.Inject
import matchmaking.BattleQueue.MatchOutput

import scala.concurrent.{ExecutionContext, Future}
import Schema._
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

  def findAllPaged(page: Long, numPerPage: Int): Future[Seq[(Battle, Robot, Robot)]] =
    run(battles.withRobots().paginate(page, numPerPage))

  def create(matchOutput: MatchOutput, r1Rating: Int, r2Rating: Int) = {
    val battle = Battle(matchOutput, r1Rating, r2Rating)
    run(battles.insert(lift(battle)).returningGenerated(_.id)).map(battle.copy(_))
  }

  private val involvesR = quote { (b: Battle, rId: Long) =>
    b.r1Id == rId || b.r2Id == rId
  }

  private val involvesPr = quote { (b: Battle, prId: Long) =>
    b.pr1Id == prId || b.pr2Id == prId
  }

  def findAllForRobot_(robotId: Long) = quote {
    for {
      b <- battles if involvesR(b, lift(robotId))
      opponentR <- robots if involvesR(b, opponentR.id)
    } yield (b, opponentR)
  }

  def findAllForRobot(robotId: Long): Future[Seq[(Battle, Robot)]] =
    run(findAllForRobot_(robotId))

  def findAllForRobotPaged(
      robotId: Long,
      page: Long,
      numPerPage: Int
  ): Future[Seq[(Battle, Robot)]] =
    run(findAllForRobot_(robotId).paginate(page, numPerPage))

  def findLatestForRobot(robotId: Long, num: Int): Future[Seq[(Battle, Robot)]] = {
    run(findAllForRobot_(robotId).sortBy(_._1.created).take(lift(num)))
  }

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
