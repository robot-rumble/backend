package models

import io.getquill.{EntityQuery, Ord, Query}
import matchmaking.BattleQueue.MatchOutput
import models.Schema._
import org.joda.time.LocalDateTime

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class Battles @Inject()(
    val schema: Schema,
    val usersRepo: Users,
    val robotsRepo: Robots
)(
    implicit ec: ExecutionContext
) {
  import schema._
  import schema.ctx._

  def find(id: BattleId): Future[Option[FullBattle]] =
    run(battles.by(id).withRobots()).map(_.headOption).map(_.map(FullBattle.tupled))

  def create(
      matchOutput: MatchOutput,
      r1Rating: Int,
      r1RatingChange: Int,
      r2Rating: Int,
      r2RatingChange: Int
  ): Future[Battle] = {
    val battle = Battle(matchOutput, r1Rating, r1RatingChange, r2Rating, r2RatingChange)
    run(battles.insert(lift(battle)).returningGenerated(_.id)).map(battle.copy(_))
  }

  def findByBoardPaged(
      boardId: BoardId,
      page: Long,
      numPerPage: Int
  ): Future[Seq[(Battle, Robot, Robot)]] = {
    val query = quote {
      (for {
        b <- battles.by(boardId)
        r1 <- robots if r1.id == b.r1Id
        r2 <- robots if r2.id == b.r2Id
      } yield (b, r1, r2)).sortBy(_._1.created)(Ord.desc)
    }

    run(query.paginate(page, numPerPage))
  }

  implicit class BattleEntityQueryExtras(query: Quoted[EntityQuery[Battle]]) {
    def findByBoardForRobot(
        boardId: BoardId,
        rId: RobotId
    ): schema.ctx.Quoted[Query[(Battle, Robot)]] =
      quote {
        val r1Battles =
          for {
            b <- query.by(boardId) if b.r1Id == lift(rId)
            opponentR <- robots if opponentR.id == b.r2Id
          } yield (b, opponentR)

        val r2Battles =
          for {
            b <- query.by(boardId) if b.r2Id == lift(rId)
            opponentR <- robots if opponentR.id == b.r1Id
          } yield (b, opponentR)

        (r1Battles union r2Battles).sortBy(_._1.created)(Ord.desc)
      }
  }

  def findByBoardForRobot(boardId: BoardId, robotId: RobotId): Future[Seq[(Battle, Robot)]] =
    run(battles.findByBoardForRobot(boardId, robotId))

  def findByBoardForRobotPaged(
      boardId: BoardId,
      robotId: RobotId,
      page: Long,
      numPerPage: Int
  ): Future[Seq[(Battle, Robot)]] =
    run(battles.findByBoardForRobot(boardId, robotId).paginate(page, numPerPage))

  case class Opponent(bId: BattleId, rId: RobotId, created: LocalDateTime)

  def findOpponents(): Future[Map[RobotId, Seq[Opponent]]] = {
    val byR1 = quote {
      battles
        .groupBy(_.r1Id)
        .map {
          case (rId, q) =>
            (
              rId,
              unquote(q.map(_.id).arrayAgg),
              unquote(q.map(_.r2Id).arrayAgg),
              unquote(q.map(_.created).arrayAgg),
            )
        }
    }
    val byR2 = quote {
      battles
        .groupBy(_.r2Id)
        .map {
          case (rId, q) =>
            (
              rId,
              unquote(q.map(_.id).arrayAgg),
              unquote(q.map(_.r1Id).arrayAgg),
              unquote(q.map(_.created).arrayAgg),
            )
        }
    }
    run(byR1 union byR2) map { result =>
      result
        .foldLeft[Map[RobotId, List[Opponent]]](Map.empty) {
          case (acc, (rId, bIds, opponentIds, timestamps)) =>
            val opponentList = bIds.zip(opponentIds.zip(timestamps)) map {
              case (bId, (oId, timestamp)) => Opponent(bId, oId, timestamp)
            }
            acc.updated(rId, opponentList.toList ::: acc.getOrElse(rId, List.empty))
        }
    }
  }
}
