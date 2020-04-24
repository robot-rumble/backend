package services

import akka.actor._
import akka.stream.Materializer
import akka.stream.alpakka.sqs.scaladsl.{SqsAckSink, SqsPublishSink, SqsSource}
import akka.stream.alpakka.sqs.{MessageAction, SqsSourceSettings}
import akka.stream.scaladsl.{Sink, Source}
import com.github.matsluni.akkahttpspi.AkkaHttpClient
import javax.inject._
import models.{Battles, PublishedRobots, Robots}
import play.api.Configuration
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.mvc._
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.{Message, SendMessageRequest}

import scala.concurrent.Future
import scala.concurrent.duration._

object MatchMaker {
  case class MatchInput(r1Id: Long, r1Code: String, r2Id: Long, r2Code: String)

  import play.api.libs.json._

  implicit val matchInputWrites = new Writes[MatchInput] {
    def writes(matchInput: MatchInput) = Json.obj(
      "r1_id" -> matchInput.r1Id,
      "r1_code" -> matchInput.r1Code,
      "r2_id" -> matchInput.r2Id,
      "r2_code" -> matchInput.r2Code,
    )
  }

  case class MatchOutput(
      r1Id: Long,
      r1Time: Float,
      r2Id: Long,
      r2Time: Float,
      winner: Battles.Winner.Value,
      errored: Boolean,
      data: String
  )

  implicit val matchOutputReads: Reads[MatchOutput] = (
    (JsPath \ "r1_id").read[Long] and
      (JsPath \ "r1_time").read[Float] and
      (JsPath \ "r2_id").read[Long] and
      (JsPath \ "r2_time").read[Float] and
      (JsPath \ "winner").read[Battles.Winner.Value] and
      (JsPath \ "errored").read[Boolean] and
      (JsPath \ "data").read[String]
  )(MatchOutput.apply _)
}

@Singleton
class MatchMaker @Inject()(
    implicit system: ActorSystem,
    mat: Materializer,
    cc: ControllerComponents,
    config: Configuration,
    robotsRepo: Robots.Repo,
    publishedRobotsRepo: PublishedRobots.Repo,
) extends AbstractController(cc) {
  import MatchMaker._

  val credentialsProvider = DefaultCredentialsProvider.create()
  implicit val awsSqsClient = SqsAsyncClient
    .builder()
    .credentialsProvider(credentialsProvider)
    .region(Region.US_EAST_1)
    .httpClient(AkkaHttpClient.builder().withActorSystem(system).build())
    .build()

  system.registerOnTermination(awsSqsClient.close())

  Source
    .tick(0.seconds, 5.second, "tick")
    .mapAsync(1) { _ =>
      // read from DB

      val matchInputs = List.range(0, 5).flatMap { _ =>
        val robots = for {
          r1 <- robotsRepo.random()
          r1Published <- publishedRobotsRepo.find(r1)
          r2 <- robotsRepo.random()
          r2Published <- publishedRobotsRepo.find(r2)
        } yield ((r1, r1Published), (r2, r2Published))

        robots.map {
          case ((r1, r1p), (r2, r2p)) =>
            val matchInput =
              MatchInput(r1p.robot_id, r1p.code, r2p.robot_id, r2p.code)
            SendMessageRequest
              .builder()
              .messageBody(Json.toJson(matchInput).toString)
              .build()
        }
      }
      println(matchInputs)
      Future.successful(matchInputs)
    }
    .mapConcat(identity)
    .runWith(
      SqsPublishSink
        .messageSink(config.get[String]("aws.matchQueueInUrl"))
    )

  val outputQueueUrl = config.get[String]("aws.matchQueueOutUrl")
  SqsSource(outputQueueUrl, SqsSourceSettings().withCloseOnEmptyReceive(false))
    .alsoTo(Sink.foreachAsync(1) { message: Message =>
      val matchOutput =
        Json.parse(message.body()).as[MatchOutput]
      println(matchOutput)
      Future.successful(())
    })
    .map(MessageAction.Delete(_))
    .runWith(SqsAckSink(outputQueueUrl))
}
