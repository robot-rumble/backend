package services

import akka.NotUsed
import akka.stream.{Materializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import javax.inject.Inject
import models.Battles.Winner
import services.BattleQueue.{MatchInput, MatchOutput}

import scala.concurrent.duration._
import scala.util.Random

class MockQueue @Inject()(
    implicit materializer: Materializer
) extends BattleQueue {

  def inputToOutput(input: MatchInput): MatchOutput = {
    MatchOutput(
      input.r1Id,
      0,
      input.r2Id,
      0,
      if (Random.nextBoolean()) Winner.R1 else Winner.R2,
      false,
      ""
    )
  }

  // https://discuss.lightbend.com/t/create-source-from-sink-and-vice-versa/605/4
  val in = Sink.asPublisher[MatchOutput](false)
  val out = Source.asSubscriber[MatchOutput]

  val (_sink, _source) = out
    .toMat(in)(Keep.both)
    .mapMaterializedValue {
      case (sub, pub) => (Sink.fromSubscriber(sub), Source.fromPublisher(pub))
    }
    .run()

  val sink = Flow[MatchInput].map(inputToOutput).to(_sink)
  val source = _source
}
