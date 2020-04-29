package models

import javax.inject.Inject
import play.api.libs.json.{Reads, Writes}
import services.Db

object Battles {

  def didR1Win(
      battle: Data,
      r1: Robots.Data,
      r2: Robots.Data
  ): Option[Boolean] = {
    battle.winner match {
      case Winner.R1 | Winner.R2 =>
        Some(battle.winner == Winner.R1 && battle.r1Id == r1.id)
      case Winner.Draw => None
    }
  }

  case class Data(
      id: Long,
      r1Id: Long,
      r2Id: Long,
      ranked: Boolean,
      winner: Winner.Value,
      errored: Boolean,
      r1Rating: Int,
      r2Rating: Int,
      r1Time: Float,
      r2Time: Float,
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

    def findForRobot(
        robot: Robots.Data
    ): List[(Data, Robots.Data, Robots.Data)] = {
      run(
        for {
          m <- schema
          otherR <- robotSchema
          if (
            (m.r1Id == lift(robot.id) && m.r2Id == otherR.id)
              || (m.r2Id == lift(robot.id) && m.r1Id == otherR.id)
          )
        } yield (m, otherR)
      ).map({ case (m, other_r) => (m, robot, other_r) })
    }

    def findAll(): List[(Data, Robots.Data, Robots.Data)] = {
      run(
        for {
          battle <- schema
          r1 <- robotSchema if battle.r1Id == r1.id
          r2 <- robotSchema if battle.r2Id == r1.id
        } yield (battle, r1, r2)
      )
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
