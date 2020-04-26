package services

import akka.NotUsed
import akka.actor._
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import com.github.esap120.scala_elo._
import javax.inject._
import models.Battles.Winner
import models.{Battles, PublishedRobots, Robots}
import play.api.Configuration
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.mvc._
import services.MatchMaker.{MatchInput, MatchOutput}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import services.AwsQueue
import software.amazon.awssdk.services.sqs.model.SendMessageRequest

trait BattleQueue {
  def createStreams(): (Sink[MatchInput, NotUsed], Source[MatchOutput, NotUsed])
}

object MatchMaker {
  import play.api.libs.json._

  implicit val matchInputWrites = new Writes[MatchInput] {
    def writes(matchInput: MatchInput) = Json.obj(
      "r1_id" -> matchInput.r1Id,
      "r1_code" -> matchInput.r1Code,
      "r2_id" -> matchInput.r2Id,
      "r2_code" -> matchInput.r2Code,
    )
  }

  implicit val matchOutputReads: Reads[MatchOutput] = (
    (JsPath \ "r1_id").read[Long] and
      (JsPath \ "r1_time").read[Float] and
      (JsPath \ "r2_id").read[Long] and
      (JsPath \ "r2_time").read[Float] and
      (JsPath \ "winner").read[Winner.Value] and
      (JsPath \ "errored").read[Boolean] and
      (JsPath \ "data").read[String]
  )(MatchOutput.apply _)

  def inputToOutput(matchInput: MatchInput): MatchOutput = {
    MatchOutput(0, 0, 0, 0, Winner.Draw, false, "")
  }

  case class MatchInput(r1Id: Long, r1Code: String, r2Id: Long, r2Code: String)

  case class MatchOutput(
      r1Id: Long,
      r1Time: Float,
      r2Id: Long,
      r2Time: Float,
      winner: Winner.Value,
      errored: Boolean,
      data: String
  )
}
@Singleton
class MatchMaker @Inject()(
    implicit system: ActorSystem,
    ec: ExecutionContext,
    mat: Materializer,
    cc: ControllerComponents,
    config: Configuration,
    robotsRepo: Robots.Repo,
    battlesRepo: Battles.Repo,
    publishedRobotsRepo: PublishedRobots.Repo,
) extends AbstractController(cc) {
  import MatchMaker._

  val (sink, source) =
    if (true) new MockQueue().createStreams()
    else new AwsQueue().createStreams()

  def prepareMatches(): Future[List[MatchInput]] = {
    val matchInputs = List.range(0, 5).flatMap { _ =>
      val robots = for {
        r1 <- robotsRepo.random()
        r1Published <- publishedRobotsRepo.find(r1)
        r2 <- robotsRepo.random()
        r2Published <- publishedRobotsRepo.find(r2)
      } yield ((r1, r1Published), (r2, r2Published))

      robots.map {
        case ((r1, r1p), (r2, r2p)) =>
          Future.successful(
            MatchInput(r1p.robot_id, r1p.code, r2p.robot_id, r2p.code)
          )
      }
    }
    Future.sequence(matchInputs)
  }

  Source
    .tick(0.seconds, 5.second, "tick")
    .mapAsync(1) { _ =>
      prepareMatches()
    }
    .mapConcat(identity)
    .runWith(sink)

  def processMatches(matchOutput: MatchOutput): Future[Unit] = {
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

    r1Player.updateRating()
    robotsRepo.updateRating(r1, r1Player.rating)
    r2Player.updateRating()
    robotsRepo.updateRating(r2, r2Player.rating)

    Future.successful(())
  }

  source.runWith(Sink.foreachAsync(1)(processMatches))
}
