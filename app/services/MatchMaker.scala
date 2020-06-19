package services

import akka.actor._
import akka.stream.Materializer
import akka.stream.scaladsl.Keep
import akka.stream.scaladsl.{Sink, Source}
import com.github.esap120.scala_elo._
import javax.inject._
import play.api.mvc._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

import models._
import models.Schema._

@Singleton
class MatchMaker @Inject()(
    implicit system: ActorSystem,
    ec: ExecutionContext,
    mat: Materializer,
    cc: ControllerComponents,
    robotsRepo: Robots,
    battlesRepo: Battles,
    battleQueue: BattleQueue
) {
  import BattleQueue._

  def prepareMatches(): List[MatchInput] = {
    var pairs = Vector.empty
//
//    robotsRepo.sortByRating() flatMap { sortedRobots =>
//      val sortedRobotsVec = sortedRobots.toVector
//      for (r <- robotsRepo.findAllStale()) {
//        val lastGames =
//      }
//    }
//
//    val matchInputs = List.range(0, 3).flatMap { _ =>
//      val robots = for {
//        r1 <- robotsRepo.random()
//        pr1 <- publishedRobotsRepo.find(r1.id)
//        r2 <- robotsRepo.random()
//        pr2 <- publishedRobotsRepo.find(r2.id)
//      } yield ((r1, pr1), (r2, pr2))
//
//      robots.map {
//        case ((r1, pr1), (r2, pr2)) =>
//          MatchInput(
//            r1.id,
//            pr1.id,
//            pr1.code,
//            r1.lang,
//            r2.id,
//            pr2.id,
//            pr2.code,
//            r2.lang,
//          )
//      }
//    }
//    println("Sending", matchInputs)
//    matchInputs
    List.empty
  }

  Source
    .tick(1.seconds, 1.seconds, "tick")
    .map(_ => prepareMatches())
    .mapConcat(identity)
    .runWith(battleQueue.sink)

  def processMatches(matchOutput: MatchOutput) = {
    println("Received", matchOutput)

//    val getRobotInfo = (id: Long) => {
//      val r = robotsRepo.find(id).get
//      // TODO: don't use a hack
//      val rGamesNum = battlesRepo.findAllForRobot(r.id, 0, 1)
//      val rPlayer =
//        new Player(rating = r.rating, startingGameCount = rGamesNum.toInt)
//      (r, rPlayer)
//    }
//    val ((r1, r1Player), (r2, r2Player)) =
//      (getRobotInfo(matchOutput.r1Id), getRobotInfo(matchOutput.r2Id))
//
//    matchOutput.winner match {
//      case Winners.R1   => r1Player wins r2Player
//      case Winners.R2   => r2Player wins r1Player
//      case Winners.Draw => r1Player draws r2Player
//    }
//
//    r1Player.updateRating(KFactor.USCF)
//    robotsRepo.updateRating(r1.id, r1Player.rating)
//    r2Player.updateRating(KFactor.USCF)
//    robotsRepo.updateRating(r2.id, r2Player.rating)
//
//    battlesRepo.create(matchOutput, r1Player.rating, r2Player.rating)
  }

  battleQueue.source.runForeach(processMatches) onComplete {
    case Success(_) => println("BattleQueue exited.")
    case Failure(t) => println("BattleQueue error: " + t.getMessage)
  }
}
