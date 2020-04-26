package services

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import javax.inject.Inject
import services.MatchMaker.{MatchInput, MatchOutput, inputToOutput}

class MockQueue @Inject()(
    implicit materializer: Materializer
) extends BattleQueue {
  def createStreams()
    : (Sink[MatchInput, NotUsed], Source[MatchOutput, NotUsed]) = {
    // https://discuss.lightbend.com/t/create-source-from-sink-and-vice-versa/605/4
//    Source
//      .asSubscriber[MatchOutput]
//      .toMat(
//        Flow[MatchInput]
//          .map(inputToOutput)
//          .to(Sink.asPublisher[MatchOutput](fanout = false))
//      )(Keep.both)
//      .mapMaterializedValue {
//        case (sub, pub) => (Sink.fromSubscriber(sub), Source.fromPublisher(pub))
//      }
//      .run()

    (Sink.onComplete(_ => ()), Source.empty)
  }
}
