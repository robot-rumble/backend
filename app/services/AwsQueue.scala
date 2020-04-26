package services

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.alpakka.sqs.scaladsl.{SqsAckSink, SqsPublishSink, SqsSource}
import akka.stream.alpakka.sqs.{MessageAction, SqsSourceSettings}
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.github.matsluni.akkahttpspi.AkkaHttpClient
import javax.inject._
import play.api.Configuration
import play.api.libs.json.Json
import services.MatchMaker.{MatchInput, MatchOutput}
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.{Message, SendMessageRequest}

class AwsQueue @Inject()(
    implicit system: ActorSystem,
    config: Configuration
) extends BattleQueue {
  def createStreams()
    : (Sink[MatchInput, NotUsed], Source[MatchOutput, NotUsed]) = {
    val inputQueueUrl = config.get[String]("aws.matchQueueInUrl")
    val outputQueueUrl = config.get[String]("aws.matchQueueOutUrl")
    val credentialsProvider = DefaultCredentialsProvider.create()
    implicit val awsSqsClient = SqsAsyncClient
      .builder()
      .credentialsProvider(credentialsProvider)
      .region(Region.US_EAST_1)
      .httpClient(AkkaHttpClient.builder().withActorSystem(system).build())
      .build()
    system.registerOnTermination(awsSqsClient.close())

    (
      Flow[MatchInput]
        .map(matchInput => {
          SendMessageRequest
            .builder()
            .messageBody(Json.toJson(matchInput).toString)
            .build()
        })
        .to(SqsPublishSink.messageSink(inputQueueUrl)),
      SqsSource(
        outputQueueUrl,
        SqsSourceSettings().withCloseOnEmptyReceive(false)
      ).alsoTo(
          Flow[Message]
            .map(MessageAction.Delete(_))
            .to(SqsAckSink(outputQueueUrl))
        )
        .map(message => Json.parse(message.body()).as[MatchOutput])
    )
  }
}
