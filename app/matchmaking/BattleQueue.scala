package matchmaking

import akka.NotUsed
import akka.stream.scaladsl.{Sink, Source}
import models.Schema.{GameMode, Lang, Team}
import play.api.libs.functional.syntax._

trait BattleQueue {
  val sink: Sink[BattleQueue.MatchInput, NotUsed]
  val source: Source[BattleQueue.MatchOutput, NotUsed]
}

object BattleQueue {
  import play.api.libs.json._

  implicit val matchInputWrites = new Writes[MatchInput] {
    def writes(matchInput: MatchInput) = Json.obj(
      "turn_num" -> matchInput.turnNum,
      "board_id" -> matchInput.boardId,
      "r1_id" -> matchInput.r1Id,
      "pr1_id" -> matchInput.pr1Id,
      "r1_code" -> matchInput.r1Code,
      "r1_lang" -> matchInput.r1Lang,
      "r2_id" -> matchInput.r2Id,
      "pr2_id" -> matchInput.pr2Id,
      "r2_code" -> matchInput.r2Code,
      "r2_lang" -> matchInput.r2Lang,
      "game_mode" -> matchInput.gameMode,
    )
  }

  implicit val matchOutputReads: Reads[MatchOutput] = (
    (JsPath \ "board_id").read[Long] and
      (JsPath \ "r1_id").read[Long] and
      (JsPath \ "pr1_id").read[Long] and
      (JsPath \ "r1_time").read[Float] and
      (JsPath \ "r2_id").read[Long] and
      (JsPath \ "pr2_id").read[Long] and
      (JsPath \ "r2_time").read[Float] and
      (JsPath \ "winner").readNullable[Team] and
      (JsPath \ "errored").read[Boolean] and
      (JsPath \ "data").read[String].map(_.getBytes)
  )(MatchOutput.apply _)

  case class MatchInput(
      turnNum: Long,
      boardId: Long,
      r1Id: Long,
      pr1Id: Long,
      r1Code: String,
      r1Lang: Lang,
      r2Id: Long,
      pr2Id: Long,
      r2Code: String,
      r2Lang: Lang,
      gameMode: GameMode
  )

  case class MatchOutput(
      boardId: Long,
      r1Id: Long,
      pr1Id: Long,
      r1Time: Float,
      r2Id: Long,
      pr2Id: Long,
      r2Time: Float,
      winner: Option[Team],
      errored: Boolean,
      data: Array[Byte],
  )
}
