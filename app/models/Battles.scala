package models

import java.time.LocalDate

import javax.inject.Inject
import services.BattleQueue.MatchOutput
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
        Some(battle.winner == Winner.R1 && battle.r1Id == r1Id)
      case Winner.Draw => None
    }
  }

  private def createData(
      matchOutput: MatchOutput,
      r1Rating: Int,
      r2Rating: Int
  ): Data = {
    Data(
      r1Id = matchOutput.r1Id,
      r2Id = matchOutput.r2Id,
      winner = matchOutput.winner,
      errored = matchOutput.errored,
      r1Time = matchOutput.r1Time,
      r2Time = matchOutput.r2Time,
      data = matchOutput.data,
      ranked = true,
      r1Rating = r1Rating,
      r2Rating = r2Rating,
      created = LocalDate.now()
    )
  }

  case class Data(
      id: Long = -1,
      r1Id: Long,
      r2Id: Long,
      ranked: Boolean,
      winner: Winner.Value,
      errored: Boolean,
      r1Rating: Int,
      r2Rating: Int,
      r1Time: Float,
      r2Time: Float,
      data: String,
      created: LocalDate,
  )

  class Repo @Inject()(
      val db: Db,
      val usersRepo: Users.Repo,
      val robotsRepo: Robots.Repo
  ) {

    import db.ctx._

    implicit val robotDecoderSource =
      robotsRepo.decoderSource.asInstanceOf[Decoder[Robots.Lang.Value]]

    implicit val decoderSource =
      QuillUtils
        .generateEnumDecoder(db.ctx, Winner)
        .asInstanceOf[Decoder[Winner.Value]]
    implicit val encoderSource =
      QuillUtils
        .generateEnumEncoder(db.ctx, Winner, "battle_outcome")
        .asInstanceOf[Encoder[Winner.Value]]

    val robotSchema =
      robotsRepo.schema.asInstanceOf[Quoted[EntityQuery[Robots.Data]]]
    val schema = quote(querySchema[Data]("battles"))

    def find(id: Long): Option[Data] =
      run(schema.filter(_.id == lift(id))).headOption

    def findForRobot(robotId: Long): List[(Data, Robots.BasicData)] = {
      run(
        for {
          m <- schema
          otherR <- robotSchema
          if (
            (m.r1Id == lift(robotId) && m.r2Id == otherR.id)
              || (m.r2Id == lift(robotId) && m.r1Id == otherR.id)
          )
        } yield (m, otherR)
      ).map(tuple => (tuple._1, dataToBasic(tuple._2)))
    }

    def count: Long = run(schema.size)

    def findAll(
        page: Long,
        numPerPage: Int
    ): List[(Data, Robots.BasicData, Robots.BasicData)] = {
      val all = quote {
        for {
          battle <- schema
          r1 <- robotSchema if battle.r1Id == r1.id
          r2 <- robotSchema if battle.r2Id == r2.id
        } yield (battle, r1, r2)
      }
      val allPaged = quote {
        all.drop(lift(page.toInt * numPerPage)).take(lift(numPerPage))
      }
      run(allPaged)
        .map(tuple => (tuple._1, dataToBasic(tuple._2), dataToBasic(tuple._3)))
    }

    def create(matchOutput: MatchOutput, r1Rating: Int, r2Rating: Int) = {
      val data = createData(matchOutput, r1Rating, r2Rating)
      val id = run(schema.insert(lift(data)).returningGenerated(_.id))
      data.copy(id = id)
    }
  }

  //noinspection TypeAnnotation
  object Winner extends Enumeration {
    val R1 = Value("R1")
    val R2 = Value("R2")
    val Draw = Value("Draw")

    implicit val winnerReads = Reads.enumNameReads(Winner)
    implicit val winnerWrites = Writes.enumNameWrites
  }
}
