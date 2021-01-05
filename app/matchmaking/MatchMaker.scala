package matchmaking

import akka.actor._
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import com.github.esap120.scala_elo._
import models.Schema.{BoardId, RobotId, Team}
import models._
import org.joda.time.{Duration, LocalDateTime}
import play.api.{Configuration, Logger}
import services.JodaUtils._

import javax.inject._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class MatchMaker @Inject()(
    config: Configuration,
    robotsRepo: Robots,
    battlesRepo: Battles,
    boardsRepo: Boards,
    battleQueue: BattleQueue
)(
    implicit system: ActorSystem,
    ec: ExecutionContext,
    mat: Materializer,
) {
  import BattleQueue._

  val logger: Logger = Logger(this.getClass)

  val TURN_NUM = config.get[Long]("queue.turnNum")

  val USE_MOCK = config.get[Boolean]("queue.useMock")

  val RECENT_OPPONENT_LIMIT =
    if (USE_MOCK) 0
    else config.get[Int]("queue.recentOpponentLimit")

  val CHECK_EVERY =
    if (USE_MOCK) 2.seconds
    else config.get[FiniteDuration]("queue.checkEvery")

  logger.debug(
    s"Starting with USE_MOCK ${USE_MOCK}, RECENT_OPPONENT_LIMIT ${RECENT_OPPONENT_LIMIT}, CHECK_EVERY ${CHECK_EVERY}"
  )

  def prepareMatches(): Future[Iterable[MatchInput]] = {
    boardsRepo.findAllBare() flatMap { allBoards =>
      val boardsMap = allBoards.filter(_.matchmakingEnabled).map(board => (board.id, board)).toMap

      robotsRepo.findAllWithPr() flatMap { allRobots =>
        battlesRepo.findOpponents() map { allOpponentsMap =>
          val recentOpponentsMap = allOpponentsMap.mapValues(_.take(RECENT_OPPONENT_LIMIT))

          allRobots
            .flatMap {
              case (r, pr) =>
                boardsMap.get(pr.boardId) match {
                  case Some(board) =>
                    val battleCooldown = if (USE_MOCK) Duration.ZERO else board.battleCooldown
                    val opponentNum = {
                      val publishedWithinWindow =
                        pr.created.isAfter(LocalDateTime.now().minus(CHECK_EVERY))
                      if (publishedWithinWindow) board.publishBattleNum
                      else {
                        val cooldownExpired = recentOpponentsMap.get(r.id) match {
                          case None | Some(Seq()) => true
                          case Some(opponents) =>
                            opponents
                              .map(_.created)
                              .max
                              .plus(battleCooldown)
                              .isBefore(LocalDateTime.now())
                        }
                        if (cooldownExpired) board.recurrentBattleNum
                        else 0
                      }
                    }
                    allRobots
                      .filter {
                        case (r2, pr2) =>
                          r.id != r2.id && pr.boardId == pr2.boardId && (recentOpponentsMap.get(
                            r.id
                          ) match {
                            case None | Some(Seq()) => true
                            case Some(opponents)    => opponents.forall(_.rId != r2.id)
                          })
                      }
                      .sortBy { case (_, pr2) => (pr2.rating - pr.rating).abs }
                      .take(opponentNum)
                      .map { o =>
                        ((r, pr), o)
                      }
                  case None => Seq()
                }
            }
            .map {
              case ((r1, pr1), (r2, pr2)) =>
                MatchInput(
                  TURN_NUM,
                  pr1.boardId.id,
                  r1.id.id,
                  pr1.id.id,
                  pr1.code,
                  r1.lang,
                  r2.id.id,
                  pr2.id.id,
                  pr2.code,
                  r2.lang,
                )
            }
        }
      }
    }
  }

  Source
    .tick(0.seconds, CHECK_EVERY, "tick")
    .mapAsyncUnordered(1)(_ => prepareMatches())
    .mapConcat(_.toList)
    .alsoTo(
      Sink.foreach(
        matchInput =>
          logger.debug(
            "Sending: " + matchInput.copy(r1Code = "TRUNCATED", r2Code = "TRUNCATED").toString
        )
      )
    )
    .runWith(battleQueue.sink)

  def processMatches(matchOutput: MatchOutput) = {
    logger.debug("Received: " + matchOutput.copy(data = "TRUNCATED").toString)

    val getRobotInfo = (id: RobotId) => {
      (for {
        pr <- robotsRepo.findLatestPr(id, BoardId(matchOutput.boardId))
        games <- battlesRepo.findBoardForRobot(BoardId(matchOutput.boardId), pr.get.rId)
      } yield (pr, games)) map {
        case (Some(pr), games) =>
          val rPlayer =
            new Player(rating = pr.rating, startingGameCount = games.length)
          (pr, rPlayer)
        case _ => throw new Exception(s"matchOutput robotId $id didn't return a robot")
      }
    }

    (for {
      r1Info <- getRobotInfo(RobotId(matchOutput.r1Id))
      r2Info <- getRobotInfo(RobotId(matchOutput.r2Id))
    } yield (r1Info, r2Info)) map {
      case ((pr1, r1Player), (pr2, r2Player)) =>
        matchOutput.winner match {
          case Some(Team.R1) => r1Player wins r2Player
          case Some(Team.R2) => r2Player wins r1Player
          case None          => r1Player draws r2Player
        }

        r1Player.updateRating(KFactor.USCF)
        r2Player.updateRating(KFactor.USCF)
        for {
          _ <- robotsRepo.updateRating(pr1.id, r1Player.rating)
          _ <- robotsRepo.updateRating(pr2.id, r2Player.rating)
          _ <- battlesRepo.create(
            matchOutput,
            pr1.rating,
            r1Player.rating - pr1.rating,
            pr2.rating,
            r2Player.rating - pr2.rating
          )
        } yield ()
    }
  }

  battleQueue.source.runForeach(processMatches) onComplete {
    case Success(_) => logger.debug("BattleQueue exited.")
    case Failure(e) => logger.debug("BattleQueue error: " + e.getMessage)
  }
}
