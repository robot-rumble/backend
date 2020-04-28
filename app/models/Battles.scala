package models

import scala.languageFeature.implicitConversions

import javax.inject.Inject
import play.api.libs.json.{Reads, Writes}
import services.Db

import Robots.dataToBasic

object Battles {
  def didR1Win(
      battle: Data,
      r1Id: Long,
  ): Option[Boolean] = {
    battle.winner match {
      case Winner.R1 | Winner.R2 =>
        Some(battle.winner == Winner.R1 && battle.r1_id == r1Id)
      case Winner.Draw => None
    }
  }

  case class Data(
      id: Long,
      r1_id: Long,
      r2_id: Long,
      ranked: Boolean,
      winner: Winner.Value,
      errored: Boolean,
      r1_rating: Int,
      r2_rating: Int,
      r1_time: Float,
      r2_time: Float,
      data: String
  )

  class Repo @Inject()(
      val db: Db,
      val usersRepo: Users.Repo,
      val robotsRepo: Robots.Repo
  ) {

    import db.ctx._

    implicit val decoderSource: Decoder[Winner.Value] = decoder(
      (index, row) => Winner.serialize(row.getObject(index).toString)
    )

    implicit val encoderSource: Encoder[Winner.Value] =
      encoder(
        java.sql.Types.VARCHAR,
        (index, value, row) => row.setString(index, value.toString)
      )

    val robotSchema =
      robotsRepo.schema.asInstanceOf[Quoted[EntityQuery[Robots.Data]]]
    val schema = quote(querySchema[Data]("battles"))

    def find(id: Long): Option[Data] =
      run(schema.filter(_.id == lift(id))).headOption

    def findForRobot(robotId: Long): List[(Data, Robots.BasicData)] = {
      run(
        for {
          m <- schema
          other_r <- robotSchema
          if (
            (m.r1_id == lift(robotId) && m.r2_id == other_r.id)
              || (m.r2_id == lift(robotId) && m.r1_id == other_r.id)
          )
        } yield (m, other_r)
      ).map(tuple => (tuple._1, dataToBasic(tuple._2)))
    }

    def findAll(): List[(Data, Robots.BasicData, Robots.BasicData)] = {
      run(
        for {
          battle <- schema
          r1 <- robotSchema if battle.r1_id == r1.id
          r2 <- robotSchema if battle.r2_id == r1.id
        } yield (battle, r1, r2)
      ).map(tuple => (tuple._1, dataToBasic(tuple._2), dataToBasic(tuple._3)))
    }
  }

  //noinspection TypeAnnotation
  object Winner extends Enumeration {
    val R1 = Value("R1")
    val R2 = Value("R2")
    val Draw = Value("Draw")

    def serialize(s: String): Value = values.find(_.toString == s).get

    implicit val winnerReads = Reads.enumNameReads(Winner)
    implicit val winnerWrites = Writes.enumNameWrites
  }
}
