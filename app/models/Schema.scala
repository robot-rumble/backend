package models

import java.time.LocalDate

import com.github.t3hnar.bcrypt._
import enumeratum._
import javax.inject.Inject
import play.api.libs.json.{Json, Writes}
import services.BattleQueue.MatchOutput
import services.Database

object Schema {
  sealed trait Lang extends EnumEntry

  case object Lang extends PlayEnum[Lang] with QuillEnum[Lang] {
    case object Python extends Lang
    case object Javascript extends Lang
    val values = findValues
  }

  sealed trait Winner extends EnumEntry

  case object Winner extends PlayEnum[Winner] with QuillEnum[Winner] {
    case object R1 extends Winner
    case object R2 extends Winner
    case object Draw extends Winner
    val values = findValues
  }

  case class User(
      id: Long = -1,
      username: String,
      password: String,
      created: LocalDate
  )

  object User {
    def apply(username: String, password: String) =
      new User(
        username = username,
        password = password.bcrypt,
        created = LocalDate.now()
      )
  }

  case class Robot(
      id: Long = -1,
      userId: Long,
      prId: Option[Long] = None,
      name: String,
      devCode: String,
      automatch: Boolean = true,
      rating: Int = 1000,
      lang: Lang,
      created: LocalDate = LocalDate.now()
  ) {
    def isPublished = prId.isDefined
  }

  object Robot {
    def apply(userId: Long, name: String, lang: Lang) =
      new Robot(userId = userId, name = name, devCode = DefaultCode(lang), lang = lang)
  }

  case class PublishedRobot(id: Long = -1, created: LocalDate = LocalDate.now(), code: String)

  implicit val robotWrites = new Writes[Robot] {
    def writes(robot: Robot) = Json.obj(
      "id" -> robot.id,
      "userId" -> robot.userId,
      "name" -> robot.name,
      "rating" -> robot.rating,
      "lang" -> robot.lang
    )
  }

  case class Battle(
      id: Long = -1,
      r1Id: Long,
      r2Id: Long,
      pr1Id: Long,
      pr2Id: Long,
      ranked: Boolean = true,
      winner: Winner,
      errored: Boolean,
      r1Rating: Int,
      r2Rating: Int,
      r1Time: Float,
      r2Time: Float,
      data: String,
      created: LocalDate = LocalDate.now(),
  ) {
    def didR1Win(r1Id: Long): Option[Boolean] = {
      winner match {
        case Winner.R1 | Winner.R2 =>
          Some(winner == Winner.R1 && r1Id == r1Id)
        case Winner.Draw => None
      }
    }
  }

  object Battle {
    def apply(matchOutput: MatchOutput, r1Rating: Int, r2Rating: Int) =
      new Battle(
        r1Id = matchOutput.r1Id,
        pr1Id = matchOutput.pr1Id,
        r2Id = matchOutput.r2Id,
        pr2Id = matchOutput.pr2Id,
        winner = matchOutput.winner,
        errored = matchOutput.errored,
        r1Time = matchOutput.r1Time,
        r2Time = matchOutput.r2Time,
        data = matchOutput.data,
        r1Rating = r1Rating,
        r2Rating = r2Rating
      )
  }

  class Schema @Inject()(db: Database) {
    val ctx = db.ctx
    import ctx._

    implicit val userIM = insertMeta[User](_.id)
    implicit val robotIM = insertMeta[Robot](_.id)
    implicit val publishedRobotIM = insertMeta[PublishedRobot](_.id)
    implicit val battleIM = insertMeta[Battle](_.id)

    val users = quote(querySchema[User]("users"))
    val robots = quote(querySchema[Robot]("robots"))
    val publishedRobots = quote(querySchema[PublishedRobot]("published_robots"))
    val battles = quote(querySchema[Battle]("battles"))

    implicit class RichQuery[T](query: Quoted[Query[T]]) {
      def paginate(page: Long, numPerPage: Int): Quoted[Query[T]] = quote {
        query.drop(lift(page.toInt * numPerPage)).take(lift(numPerPage))
      }
    }
  }
}
