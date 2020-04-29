package services

import akka.actor._
import akka.stream.Materializer
import akka.stream.scaladsl.Keep
import akka.stream.scaladsl.{Sink, Source}
import com.github.esap120.scala_elo._
import javax.inject._
import models.Battles.Winner
import models.{Battles, PublishedRobots, Robots}
import play.api.mvc._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MatchMaker @Inject()(
    implicit system: ActorSystem,
    ec: ExecutionContext,
    mat: Materializer,
    cc: ControllerComponents,
    robotsRepo: Robots.Repo,
    battlesRepo: Battles.Repo,
    publishedRobotsRepo: PublishedRobots.Repo,
    battleQueue: BattleQueue
) {
  import BattleQueue._

  def prepareMatches(): List[MatchInput] = {
    val matchInputs = List.range(0, 3).flatMap { _ =>
      val robots = for {
        r1 <- robotsRepo.random()
        r1Published <- publishedRobotsRepo.find(r1)
        r2 <- robotsRepo.random()
        r2Published <- publishedRobotsRepo.find(r2)
      } yield ((r1, r1Published), (r2, r2Published))

      robots.map {
        case ((r1, r1p), (r2, r2p)) =>
          MatchInput(r1p.robot_id, r1p.code, r2p.robot_id, r2p.code)
      }
    }
    println("Sending", matchInputs)
    matchInputs
  }

  Source
    .tick(1.seconds, 1.seconds, "tick")
    .map(_ => prepareMatches())
    .mapConcat(identity)
    .runWith(battleQueue.sink)

  def processMatches(matchOutput: MatchOutput) = {
    println("Received", matchOutput)

    val getRobotInfo = (id: Long) => {
      val r = robotsRepo.findById(id).get
      val rGames = battlesRepo.findForRobot(r)
      val rPlayer =
        new Player(rating = r.rating, startingGameCount = rGames.length)
      (r, rPlayer)
    }
    val ((r1, r1Player), (r2, r2Player)) =
      (getRobotInfo(matchOutput.r1Id), getRobotInfo(matchOutput.r2Id))

    matchOutput.winner match {
      case Winner.R1   => r1Player wins r2Player
      case Winner.R2   => r2Player wins r1Player
      case Winner.Draw => r1Player draws r2Player
    }

    r1Player.updateRating(KFactor.USCF)
    robotsRepo.updateRating(r1, r1Player.rating)
    r2Player.updateRating(KFactor.USCF)
    robotsRepo.updateRating(r2, r2Player.rating)

    battlesRepo.create(matchOutput, r1Player.rating, r2Player.rating)
  }

  battleQueue.source.runForeach(processMatches)
}
