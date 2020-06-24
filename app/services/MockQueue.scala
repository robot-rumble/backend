package services

import javax.inject.Inject
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import play.api.libs.json.{JsString, Json}
import services.BattleQueue.{MatchInput, MatchOutput}

import sys.process._

import models.Schema.Winner

class MockQueue @Inject()(
    implicit materializer: Materializer
) extends BattleQueue {

  def inputToOutput(input: MatchInput): MatchOutput = {
    val jsonOutput = Seq(
      "../target/release/rumblebot",
      "run",
      s"inline:${input.r1Lang.toString.toUpperCase};${input.r1Code}",
      s"inline:${input.r2Lang.toString.toUpperCase};${input.r2Code}",
      "--raw"
    ).!!
    val winner = (Json.parse(jsonOutput) \ "winner").get
    MatchOutput(
      input.r1Id,
      input.pr1Id,
      0,
      input.r2Id,
      input.pr2Id,
      0,
      if (winner == JsString("Red")) Winner.R1 else Winner.R2,
      false,
      jsonOutput
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
