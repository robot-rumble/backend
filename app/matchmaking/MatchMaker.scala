package matchmaking

import akka.actor._
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import com.github.esap120.scala_elo._
import javax.inject._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import org.joda.time.{Duration, LocalDateTime}
import models._
import services.JodaUtils._
import models.Schema.{BoardId, RobotId, Team}
import play.api.Configuration
import play.api.Logger

@Singleton
class MatchMaker @Inject()(
    config: Configuration,
    robotsRepo: Robots,
    battlesRepo: Battles,
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

  val COOLDOWN =
    if (USE_MOCK) Duration.ZERO
    else Duration.millis(config.get[FiniteDuration]("queue.cooldown").toMillis)

  val CHECK_EVERY =
    if (USE_MOCK) 2.seconds
    else config.get[FiniteDuration]("queue.checkEvery")

  val INITIAL_OPPONENT_NUM = config.get[Int]("queue.initialOpponentNum")
  val RECURRENT_OPPONENT_NUM = config.get[Int]("queue.recurrentOpponentNum")

  logger.debug(
    s"Starting with USE_MOCK ${USE_MOCK}, RECENT_OPPONENT_LIMIT ${RECENT_OPPONENT_LIMIT}, COOLDOWN ${COOLDOWN}, CHECK_EVERY ${CHECK_EVERY}"
  )

  def prepareMatches(): Future[Iterable[MatchInput]] = {
    logger.debug("Preparing matches...")
    robotsRepo.findAllWithPr() flatMap { allRobots =>
      battlesRepo.findOpponents() map { allOpponentsMap =>
        val recentOpponentsMap = allOpponentsMap.mapValues(_.take(RECENT_OPPONENT_LIMIT))

        allRobots
          .flatMap {
            case (r, pr) =>
              val opponentNum = {
                val publishedWithinWindow =
                  pr.created.isAfter(LocalDateTime.now().minus(CHECK_EVERY))
                if (publishedWithinWindow) INITIAL_OPPONENT_NUM
                else {
                  val cooldownExpired = recentOpponentsMap.get(r.id) match {
                    case None | Some(Seq()) => true
                    case Some(opponents) =>
                      opponents
                        .map(_.created)
                        .max
                        .isBefore(LocalDateTime.now().minus(COOLDOWN))
                  }
                  if (cooldownExpired) RECURRENT_OPPONENT_NUM
                  else 0
                }
              }
              allRobots
                .filter {
                  case (r2, pr2) =>
                    r.id != r2.id && pr.boardId == pr2.boardId && (recentOpponentsMap.get(r.id) match {
                      case None | Some(Seq()) => true
                      case Some(opponents)    => opponents.forall(_.rId != r2.id)
                    })
                }
                .sortBy { case (_, pr2) => (pr2.rating - pr.rating).abs }
                .take(opponentNum)
                .map { o =>
                  ((r, pr), o)
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
