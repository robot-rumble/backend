package services

import akka.actor.ActorSystem
import akka.stream.alpakka.sqs.scaladsl.{SqsAckSink, SqsPublishSink, SqsSource}
import akka.stream.alpakka.sqs.{MessageAction, SqsSourceSettings}
import akka.stream.scaladsl.Flow
import com.github.matsluni.akkahttpspi.AkkaHttpClient
import javax.inject._
import play.api.Configuration
import play.api.inject.ApplicationLifecycle
import play.api.libs.json.Json
import services.BattleQueue.{MatchInput, MatchOutput}
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.{Message, SendMessageRequest}

import scala.concurrent.Future

class AwsQueue @Inject()(
    implicit system: ActorSystem,
    config: Configuration,
    lifecycle: ApplicationLifecycle
) extends BattleQueue {
  val inputQueueUrl = config.get[String]("aws.matchQueueInUrl")
  val outputQueueUrl = config.get[String]("aws.matchQueueOutUrl")
  val credentialsProvider = DefaultCredentialsProvider.create()
  implicit val awsSqsClient = SqsAsyncClient
    .builder()
    .credentialsProvider(credentialsProvider)
    .region(Region.US_EAST_1)
    .httpClient(AkkaHttpClient.builder().withActorSystem(system).build())
    .build()
  lifecycle.addStopHook { () =>
    awsSqsClient.close()
    Future.successful(())
  }

  val sink = Flow[MatchInput]
    .map(matchInput => {
      SendMessageRequest
        .builder()
        .messageBody(Json.toJson(matchInput).toString)
        .build()
    })
    .to(SqsPublishSink.messageSink(inputQueueUrl))

  val source = SqsSource(
    outputQueueUrl,
    SqsSourceSettings().withCloseOnEmptyReceive(false)
  ).alsoTo(
      Flow[Message]
        .map(MessageAction.Delete(_))
        .to(SqsAckSink(outputQueueUrl))
    )
    .map(message => Json.parse(message.body()).as[MatchOutput])
}
