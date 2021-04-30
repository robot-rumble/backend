package matchmaking

import akka.actor.ActorSystem
import akka.stream.alpakka.sqs.scaladsl.{SqsAckSink, SqsPublishSink, SqsSource}
import akka.stream.alpakka.sqs.{MessageAction, SqsSourceSettings}
import akka.stream.scaladsl.{Flow, RestartSink, RestartSource}
import com.github.matsluni.akkahttpspi.AkkaHttpClient
import matchmaking.BattleQueue.{MatchInput, MatchOutput}
import play.api.libs.json.Json
import play.api.{Configuration, Logger}
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.{Message, SendMessageRequest}

import java.util.Base64
import javax.inject._
import scala.concurrent.duration._

class AwsQueue @Inject()(
    implicit system: ActorSystem,
    config: Configuration,
) extends BattleQueue {
  val logger = Logger(this.getClass)

  val inputQueueUrl = config.get[String]("aws.battleQueueInUrl")
  val outputQueueUrl = config.get[String]("aws.battleQueueOutUrl")
  val credentialsProvider = DefaultCredentialsProvider.create()
  implicit val awsSqsClient = SqsAsyncClient
    .builder()
    .credentialsProvider(credentialsProvider)
    .region(Region.US_EAST_1)
    .httpClient(AkkaHttpClient.builder().withActorSystem(system).build())
    .build()
  system.registerOnTermination(awsSqsClient.close())

  val minBackoff = 5.seconds
  val maxBackoff = 30.seconds
  val randomFactor = 0.2

  val sink = Flow[MatchInput]
    .map(matchInput => {
      SendMessageRequest
        .builder()
        .messageBody(Json.toJson(matchInput).toString)
        .build()
    })
    .to(
      RestartSink.withBackoff(minBackoff, maxBackoff, randomFactor)(
        () => {
          logger.debug("Starting AWS input sink")
          SqsPublishSink.messageSink(inputQueueUrl)
        }
      )
    )

  val source =
    RestartSource
      .withBackoff(minBackoff, maxBackoff, randomFactor)(() => {
        logger.debug("Starting AWS output source...")
        SqsSource(
          outputQueueUrl,
          SqsSourceSettings().withCloseOnEmptyReceive(false).withWaitTimeSeconds(20)
        ).alsoTo(
            Flow[Message]
              .map(MessageAction.Delete(_))
              .to(
                RestartSink.withBackoff(minBackoff, maxBackoff, randomFactor)(
                  () => {
                    logger.debug("Starting AWS output sink...")
                    SqsAckSink(outputQueueUrl)
                  }
                )
              )
          )
          .map(
            message => {
              logger.debug(s"Got message ${message.messageId()}")
              val matchOutput = Json.parse(message.body).as[MatchOutput]
              matchOutput.copy(data = Base64.getDecoder.decode(matchOutput.data))
            }
          )
      })
}
