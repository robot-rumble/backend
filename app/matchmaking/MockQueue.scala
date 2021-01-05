package matchmaking

import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import matchmaking.BattleQueue.{MatchInput, MatchOutput}
import models.Schema.Team
import play.api.libs.json.{JsObject, JsString, Json}

import javax.inject.Inject
import scala.sys.process._

class MockQueue @Inject()(
    implicit materializer: Materializer
) extends BattleQueue {

  def inputToOutput(input: MatchInput): MatchOutput = {
    val jsonOutput = Seq(
      "../cli/target/release/rumblebot",
      "run",
      "term",
      s"inline:${input.r1Lang};${input.r1Code}",
      s"inline:${input.r2Lang};${input.r2Code}",
      "-t",
      input.turnNum.toString,
      "--raw",
    ).!!
    val winner = (Json.parse(jsonOutput) \ "winner").get
    val errored = (Json.parse(jsonOutput) \ "errors").get != JsObject(Seq())
    MatchOutput(
      input.boardId,
      input.r1Id,
      input.pr1Id,
      0,
      input.r2Id,
      input.pr2Id,
      0,
      if (winner == JsString("Blue")) Some(Team.R1)
      else if (winner == JsString("Red")) Some(Team.R2)
      else None,
      errored,
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
