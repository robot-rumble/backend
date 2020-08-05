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
import models.Schema.Team
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

  val USE_MOCK = config.get[Boolean]("queue.useMock")

  val RECENT_OPPONENT_LIMIT =
    if (USE_MOCK) 0
    else config.get[Int]("queue.recentOpponentLimit")

  val COOLDOWN =
    if (USE_MOCK) Duration.ZERO
    else Duration.millis(config.get[FiniteDuration]("queue.cooldown").toMillis)

  def prepareMatches(): Future[Iterable[MatchInput]] = {
    robotsRepo.findAllPr() flatMap { allRobots =>
      battlesRepo.allOpponents() map { allOpponentsMap =>
        val recentOpponentsMap = allOpponentsMap.mapValues(_.take(RECENT_OPPONENT_LIMIT))

        allRobots
          .filter {
            case (r, _) =>
              recentOpponentsMap.get(r.id) match {
                case None | Some(Seq()) => true
                case Some(opponents) =>
                  opponents
                    .map(_.created)
                    .max
                    .isBefore(LocalDateTime.now().minus(COOLDOWN))
              }
          }
          .flatMap {
            case (r, pr) =>
              allRobots
                .sortBy {
                  case (r2, _) => (r2.rating - r.rating).abs
                }
                .find {
                  case (r2, _) =>
                    r.id != r2.id && (recentOpponentsMap.get(r.id) match {
                      case None | Some(Seq()) => true
                      case Some(opponents)    => opponents.forall(_.rId != r2.id)
                    })
                }
                .map { o =>
                  ((r, pr), o)
                }
          }
          .map {
            case ((r1, pr1), (r2, pr2)) =>
              MatchInput(
                r1.id,
                pr1.id,
                pr1.code,
                r1.lang,
                r2.id,
                pr2.id,
                pr2.code,
                r2.lang,
              )
          }
      }
    }
  }

  val CHECK_EVERY =
    if (USE_MOCK) 2.seconds
    else config.get[FiniteDuration]("queue.checkEvery")

  Source
    .tick(0.seconds, CHECK_EVERY, "tick")
    .mapAsyncUnordered(1)(_ => prepareMatches())
    .mapConcat(_.toList)
    .alsoTo(Sink.foreach(matchInput => logger.debug("Sending: " + matchInput.toString)))
    .runWith(battleQueue.sink)

  def processMatches(matchOutput: MatchOutput) = {
    logger.debug("Received: " + matchOutput.copy(data = "TRUNCATED").toString)

    val getRobotInfo = (id: Long) => {
      (for {
        robot <- robotsRepo.find(id)(controllers.Auth.LoggedOut())
        games <- battlesRepo.findAllForRobot(robot.get.id)
      } yield (robot, games)) map {
        case (robot, games) =>
          val rPlayer =
            new Player(rating = robot.get.rating, startingGameCount = games.length)
          (robot.get, rPlayer)
      }
    }

    (for {
      r1Info <- getRobotInfo(matchOutput.r1Id)
      r2Info <- getRobotInfo(matchOutput.r2Id)
    } yield (r1Info, r2Info)) map {
      case ((r1, r1Player), (r2, r2Player)) =>
        matchOutput.winner match {
          case Some(Team.R1) => r1Player wins r2Player
          case Some(Team.R2) => r2Player wins r1Player
          case None          => r1Player draws r2Player
        }

        r1Player.updateRating(KFactor.USCF)
        r2Player.updateRating(KFactor.USCF)
        for {
          _ <- robotsRepo.updateRating(r1.id, r1Player.rating)
          _ <- robotsRepo.updateRating(r2.id, r2Player.rating)
          _ <- battlesRepo.create(matchOutput, r1Player.rating, r2Player.rating)
        } yield ()
    }
  }

  battleQueue.source.runForeach(processMatches) onComplete {
    case Success(_) => logger.debug("BattleQueue exited.")
    case Failure(e) => logger.debug("BattleQueue error: " + e.getMessage)
  }
}
