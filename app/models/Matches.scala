package models

import javax.inject.Inject
import services.Db

object Matches {

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



  class Repo @Inject()(val db: Db, val usersRepo: Users.Repo, val robotsRepo: Robots.Repo) {

    import db.ctx._

    implicit val decoderSource: Decoder[Outcome.Value] = decoder(
      (index, row) => Outcome.serialize(row.getObject(index).toString))

    implicit val encoderSource: Encoder[Outcome.Value] =
      encoder(java.sql.Types.VARCHAR,
        (index, value, row) => row.setString(index, value.toString))

    val schema: db.ctx.Quoted[db.ctx.EntityQuery[Data]] = quote(
      querySchema[Data]("matches"))

    def find(id: Long): Option[Data] =
      run(schema.filter(_.id == lift(id))).headOption

    def findForRobot(robot: Robots.Data): List[(Data, Robots.Data, Robots.Data)] = {
      val robotSchema = robotsRepo.schema.asInstanceOf[Quoted[EntityQuery[Robots.Data]]]
      run(
        schema
          .filter(m => m.r1_id == robot.id || m.r2_id == robot.id)
          .join(robotSchema)
          .on((m, other_r) => (m.r1_id == robot.id && m.r2_id == other_r.id) || (m.r2_id == robot.id && m.r1_id == other_r.id))
      ).map({ case (m, other_r) => (m, robot, other_r) })
    }
  }

  //noinspection TypeAnnotation
  object Outcome extends Enumeration {
    val R1Won = Value("r1_won")
    val R2Won = Value("r2_won")
    val Draw = Value("draw")

    def serialize(s: String): Value = values.find(_.toString == s).get
  }

  def didR1Win(`match`: Data, r1: Robots.Data, r2: Robots.Data): Option[Boolean] = {
    `match`.outcome match {
      case Outcome.R1Won | Outcome.R2Won => {
        Some(`match`.outcome == Outcome.R1Won && `match`.r1_id == r1.id)
      }
      case Outcome.Draw => None
    }
  }

}
