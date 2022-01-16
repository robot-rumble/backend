import com.github.andriykuba.scala.glicko2.scala.Glicko2
import com.github.andriykuba.scala.glicko2.scala.Glicko2.{Loss, Player, Win}
import models.Schema._
import models._
import play.api.inject.guice.GuiceApplicationBuilder

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
val app = new GuiceApplicationBuilder().build()
val schema = app.injector.instanceOf[Schema]
import schema._
import schema.ctx._
val usersRepo = app.injector.instanceOf[Users]
val robotsRepo = app.injector.instanceOf[Robots]
val battlesRepo = app.injector.instanceOf[Battles]
val boardsRepo = app.injector.instanceOf[Boards]
val seasonsRepo = app.injector.instanceOf[Seasons]

import java.io._

var boardId = BoardId(4)

run(battles.by(boardId).sortBy(_.created)) map { bs =>
  val pRobots = mutable.Map.empty[RobotId, PRobot]
//  val record = mutable.Map
//    .empty[RobotId, Vector[(Battle, BigDecimal, BigDecimal, BigDecimal, BigDecimal)]]
//    .withDefaultValue(Vector())
  val battles = ListBuffer[Battle]()

  val getRobotInfo = (rId: RobotId, prId: PRobotId) => {
    if (!pRobots.contains(rId)) {
      pRobots.update(rId, PRobot(rId, boardId, "", GlickoSettings(1000, 350, 0.06)))
    }
    val pr = pRobots(rId)
    pRobots.update(rId, pr.copy(id = prId))
    (pr, Player(pr.rating, pr.deviation, pr.volatility))
  }

  val updateRobot = (rId: RobotId, glickoSettings: GlickoSettings) => {
    pRobots.update(
      rId,
      pRobots(rId).copy(
        rating = glickoSettings.rating,
        deviation = glickoSettings.deviation,
        volatility = glickoSettings.volatility
      )
    )
  }
  for (b <- bs) {
    val (pr1, r1Player) = getRobotInfo(b.r1Id, b.pr1Id)
    val (pr2, r2Player) = getRobotInfo(b.r2Id, b.pr2Id)

    val (r1, r2) = ((b.winner match {
      case Some(Team.R1) => Some((Win(r2Player), Loss(r1Player)))
      case Some(Team.R2) => Some((Loss(r2Player), Win(r1Player)))
      case None          => None
    }) match {
      case Some((r1Game, r2Game)) =>
        (Glicko2.update(r1Player, Seq(r1Game)), Glicko2.update(r2Player, Seq(r2Game)))
      case None =>
        (r1Player, r2Player)
    })

    val b_ = b.copy(
      pr1Rating = r1.rating.toInt,
      pr1RatingChange = r1.rating.toInt - r1Player.rating.toInt,
      pr2Rating = r2.rating.toInt,
      pr2RatingChange = r2.rating.toInt - r2Player.rating.toInt
    )
    battles += b_

//    record.update(
//      b_.r1Id,
//      record(b_.r1Id) :+ (b_, r1.deviation, r1.volatility, r2.deviation, r2.volatility)
//    )
//    record.update(
//      b_.r2Id,
//      record(b_.r2Id) :+ (b_, r2.deviation, r2.volatility, r1.deviation, r1.volatility)
//    )
//
    updateRobot(b_.r1Id, GlickoSettings(r1))
    updateRobot(b_.r2Id, GlickoSettings(r2))
  }

  val message = (pr: PRobot) => s"${pr.rId} ${pr.id} ${pr.rating}"
  val pw = new PrintWriter(new File("/home/anton/code/rr/backend/conf/out.txt"))
  pw.write(pRobots.values.toVector.sortBy(_.rating).map(message).mkString("\n"))

//  val bMessage = (r: RobotId) =>
//    (v: (Battle, BigDecimal, BigDecimal, BigDecimal, BigDecimal)) => {
//      val b = v._1
//      val res = if (b.r1Id == r) {
//        Seq(
//          b.id,
//          b.pr1Id,
//          "|",
//          b.pr1Rating,
//          b.pr1RatingChange,
//          v._2,
//          v._3,
//          "|",
//          b.pr2Rating,
//          b.pr2RatingChange,
//          v._4,
//          v._5
//        )
//      } else {
//        Seq(
//          b.id,
//          b.pr2Id,
//          "|",
//          b.pr2Rating,
//          b.pr2RatingChange,
//          v._2,
//          v._3,
//          "|",
//          b.pr1Rating,
//          b.pr1RatingChange,
//          v._4,
//          v._5
//        )
//      }
//      res.mkString(" ")
//  }
//  val target = RobotId(181)
//  pw.write("\n\n\n")
//  pw.write(record(target).map(bMessage(target)).mkString("\n"))
  pw.close()
  (pRobots.values.toList, battles.toList)
} onComplete {
  case Success(value) => {
    println("Done assembling data")

    val battleQuery = quote {
      liftQuery(value._2).foreach { battle =>
        battles
          .filter(_.id == battle.id)
          .update(
            _.pr1Rating -> battle.pr1Rating,
            _.pr2Rating -> battle.pr2Rating,
            _.pr1RatingChange -> battle.pr1RatingChange,
            _.pr2RatingChange -> battle.pr2RatingChange
          )
      }
    }

    val pRobotsQuery = quote {
      liftQuery(value._1).foreach { pr =>
        publishedRobots
          .filter(_.id == pr.id)
          .update(
            _.rating -> pr.rating,
            _.volatility -> pr.volatility,
            _.deviation -> pr.deviation
          )
      }
    }

    run(battleQuery) flatMap { res =>
      run(pRobotsQuery)
    } onComplete {
      case Success(value) => {
        println("Done updating database!")
      }
      case Failure(e) => {
        System.err.println(e)
      }
    }
  }
  case Failure(e) => {
    System.err.println(e)
  }
}

