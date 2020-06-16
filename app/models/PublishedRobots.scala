package models

import java.time.LocalDate

import javax.inject.Inject
import scala.concurrent.duration._
import services.Db

object PublishedRobots {

  private def createData(robotId: Long, code: String): Data = {
    Data(robotId = robotId, code = code)
  }

  case class Data(
      id: Long = -1,
      created: LocalDate = LocalDate.now(),
      robotId: Long,
      code: String,
  )

  class Repo @Inject()(
      val db: Db,
      val usersRepo: Users.Repo,
//      val battlesRepo: Battles.Repo
  ) {

    import db.ctx._

    val schema = quote(querySchema[Data]("published_robots"))
//    val battlesSchema =
//      battlesRepo.schema.asInstanceOf[Quoted[EntityQuery[Battles.Data]]]

    def find(robotId: Long): Option[Data] =
      run(
        schema
          .filter(_.robotId == lift(robotId))
          .sortBy(_.created)(Ord.desc)
      ).headOption

    def create(robotId: Long, code: String) = {
      val data = createData(robotId, code)
      val id = run(schema.insert(lift(data)).returningGenerated(_.id))
      data.copy(id = id)
    }

//    def findAllStale(): List[Data] = {
//      val joinOnPr = quote { pr: PublishedRobots.Data => b: Battles.Data =>
//        b.pr1Id == pr.robotId || b.pr2Id == pr.robotId
//      }
//
//      val noBattles = quote {
//        for {
//          pr <- schema
//          battle <- battlesSchema.leftJoin(joinOnPr(pr)) if battle.isEmpty
//        } yield pr
//      }
//
//      val staleBattles = quote {
//        for {
//          pr <- schema.distinct
//          battle <- battlesSchema.join(joinOnPr(pr))
//          if battle.created.isBefore(LocalDate.now().minusDays(1))
//        } yield pr
//      }
//
//      run(noBattles union staleBattles)
//    }
//
//    def findAllBattles(prId: Long): List[Data] = {
//      for {
//        battle <- battlesSchema
//      }
//    }
  }
}
