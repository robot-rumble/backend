package services

import akka.NotUsed
import akka.stream.scaladsl.{Sink, Source}
import models.Battles.Winner
import play.api.libs.functional.syntax._
import models.Robots.Lang

trait BattleQueue {
  val sink: Sink[BattleQueue.MatchInput, NotUsed]
  val source: Source[BattleQueue.MatchOutput, NotUsed]
}

object BattleQueue {
  import play.api.libs.json._

  implicit val matchInputWrites = new Writes[MatchInput] {
    def writes(matchInput: MatchInput) = Json.obj(
      "r1_id" -> matchInput.r1Id,
      "r1_code" -> matchInput.r1Code,
      "r1_lang" -> matchInput.r1Lang,
      "r2_id" -> matchInput.r2Id,
      "r2_code" -> matchInput.r2Code,
      "r2_lang" -> matchInput.r2Lang,
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

  case class MatchInput(
    r1Id: Long, r1Code: String, r1Lang: Lang.Value,
    r2Id: Long, r2Code: String, r2Lang: Lang.Value,
  )

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
