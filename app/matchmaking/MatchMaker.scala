package matchmaking

import akka.actor._
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import com.github.andriykuba.scala.glicko2.scala.Glicko2
import com.github.andriykuba.scala.glicko2.scala.Glicko2.{Loss, Player, Win}
import models.Schema.{BoardId, GlickoSettings, RobotId, Team}
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

  val VOLATILITY_CUTOFF = config.get[Double]("queue.volatilityCutoff")
  val NON_VOLATILE_BATTLE_COOLDOWN =
    config.get[FiniteDuration]("queue.nonVolatileBattleCooldown").toDuration
  val NON_VOLATILE_BATTLE_NUM = config.get[Int]("queue.nonVolatileBattleNum")

  logger.debug(
    s"Starting with USE_MOCK $USE_MOCK, RECENT_OPPONENT_LIMIT $RECENT_OPPONENT_LIMIT, CHECK_EVERY $CHECK_EVERY"
  )

  def prepareMatches(): Future[Iterable[MatchInput]] = {
    boardsRepo.findAllBareNoMembershipCheck() flatMap { allBoards =>
      val boardsMap = allBoards.filter(_.matchmakingEnabled).map(board => (board.id, board)).toMap

      robotsRepo.findAllLatestPrForActive() flatMap { allRobots =>
        battlesRepo.findOpponents() map { allOpponentsMap =>
          val recentOpponentsMap =
            allOpponentsMap.mapValues(
              opponents =>
                opponents
                  .sortBy(_.created)(localDateTimeOrdering.reverse)
                  .take(RECENT_OPPONENT_LIMIT)
            )

          allRobots
            .flatMap {
              case (r, pr) =>
                boardsMap.get(pr.boardId) match {
                  case Some(board) =>
                    val battleCooldown =
                      if (USE_MOCK) Duration.ZERO
                      else if (pr.volatility < VOLATILITY_CUTOFF) NON_VOLATILE_BATTLE_COOLDOWN
                      else board.battleCooldown
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
                        if (!cooldownExpired) 0
                        else if (pr.volatility < VOLATILITY_CUTOFF) NON_VOLATILE_BATTLE_NUM
                        else board.recurrentBattleNum
                      }
                    }
                    allRobots
                      .filter {
                        case (r2, pr2) =>
                          r.id != r2.id && pr.boardId == pr2.boardId
                      }
                      .filter {
                        case (r2, _) =>
                          recentOpponentsMap.get(r.id) match {
                            case None | Some(Seq()) => true
                            // check that is not recent opponent
                            case Some(opponents) => opponents.forall(_.rId != r2.id)
                          }
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
    logger.debug("Received: " + matchOutput.copy(data = Array()).toString)

    val getRobotInfo = (id: RobotId) => {
      robotsRepo.findLatestPr(id, BoardId(matchOutput.boardId)) map {
        case Some(pr) =>
          val rPlayer = Player(pr.rating, pr.deviation, pr.volatility)
          (pr, rPlayer)
        case None => throw new Exception(s"matchOutput robotId $id didn't return a robot")
      }
    }

    (for {
      r1Info <- getRobotInfo(RobotId(matchOutput.r1Id))
      r2Info <- getRobotInfo(RobotId(matchOutput.r2Id))
    } yield (r1Info, r2Info)) map {
      case ((pr1, r1Player), (pr2, r2Player)) =>
        val (r1Settings, r2Settings) = ((matchOutput.winner match {
          case Some(Team.R1) => Some((Win(r2Player), Loss(r1Player)))
          case Some(Team.R2) => Some((Loss(r2Player), Win(r1Player)))
          case None          => None
        }) match {
          case Some((r1Game, r2Game)) =>
            (Glicko2.update(r1Player, Seq(r1Game)), Glicko2.update(r2Player, Seq(r2Game)))
          case None =>
            (r1Player, r2Player)
        }) match { case (r1, r2) => (GlickoSettings(r1), GlickoSettings(r2)) }

        val (r1Errored, r2Errored) =
          (matchOutput.errored, matchOutput.winner) match {
            case (false, _)            => (false, false)
            case (true, Some(Team.R1)) => (false, true)
            case (true, Some(Team.R2)) => (true, false)
            case (true, None)          => (true, true)
          }

        for {
          _ <- robotsRepo.updateAfterBattle(pr1.rId, pr1.id, r1Settings, r1Errored)
          _ <- robotsRepo.updateAfterBattle(pr2.rId, pr2.id, r2Settings, r2Errored)
          _ <- battlesRepo.create(
            matchOutput,
            r1Settings.rating,
            r1Settings.rating - pr1.rating,
            r2Settings.rating,
            r2Settings.rating - pr2.rating
          )
        } yield ()
    }
  }

  battleQueue.source.runForeach(processMatches) onComplete {
    case Success(_) => logger.debug("BattleQueue exited.")
    case Failure(e) => logger.debug("BattleQueue error: " + e.getMessage)
  }
}
