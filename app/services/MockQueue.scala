package services

import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import javax.inject.Inject
import services.BattleQueue.{MatchInput, MatchOutput, inputToOutput}

class MockQueue @Inject()(
    implicit materializer: Materializer
) extends BattleQueue {
  val sink = Sink.onComplete(_ => ())
  val source = Source.empty

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
}
