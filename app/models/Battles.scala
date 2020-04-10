package models

import javax.inject.Inject
import services.Db

object Battles {

  case class Data(id: Long,
                  r1_id: Long,
                  r2_id: Long,
                  ranked: Boolean,
                  outcome: Outcome.Value,
                  errored: Boolean,
                  r1_rating: Int,
                  r2_rating: Int,
                  r1_time: Float,
                  r2_time: Float,
                  r1_logs: String,
                  r2_logs: String,
                  data: String)


  def didR1Win(battle: Data, r1: Robots.Data, r2: Robots.Data): Option[Boolean] = {
    battle.outcome match {
      case Outcome.R1Won | Outcome.R2Won => {
        Some(battle.outcome == Outcome.R1Won && battle.r1_id == r1.id)
      }
      case Outcome.Draw => None
    }
  }

  //noinspection TypeAnnotation
  object Outcome extends Enumeration {
    val R1Won = Value("r1_won")
    val R2Won = Value("r2_won")
    val Draw = Value("draw")

    def serialize(s: String): Value = values.find(_.toString == s).get
  }

  class Repo @Inject()(val db: Db, val usersRepo: Users.Repo, val robotsRepo: Robots.Repo) {

    import db.ctx._

    val robotSchema: Quoted[EntityQuery[Robots.Data]] = robotsRepo.schema.asInstanceOf[Quoted[EntityQuery[Robots.Data]]]

    implicit val decoderSource: Decoder[Outcome.Value] = decoder(
      (index, row) => Outcome.serialize(row.getObject(index).toString))

    implicit val encoderSource: Encoder[Outcome.Value] =
      encoder(java.sql.Types.VARCHAR,
        (index, value, row) => row.setString(index, value.toString))

    val schema: db.ctx.Quoted[db.ctx.EntityQuery[Data]] = quote(
      querySchema[Data]("battles"))

    def find(id: Long): Option[Data] =
      run(schema.filter(_.id == lift(id))).headOption

    def findForRobot(robot: Robots.Data): List[(Data, Robots.Data, Robots.Data)] = {
      run(
        for {
          m <- schema
          other_r <- robotSchema if (m.r1_id == lift(robot.id) && m.r2_id == other_r.id) || (m.r2_id == lift(robot.id) && m.r1_id == other_r.id)
        } yield (m, other_r)
      ).map({ case (m, other_r) => (m, robot, other_r) })
    }

    def findAll(): List[(Data, Robots.Data, Robots.Data)] = {
      run(
        for {
          battle <- schema
          r1 <- robotSchema if battle.r1_id == r1.id
          r2 <- robotSchema if battle.r2_id == r1.id
        } yield (battle, r1, r2)
      )
    }
  }

}
