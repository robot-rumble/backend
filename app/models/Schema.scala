package models

import org.joda.time.LocalDateTime
import com.github.t3hnar.bcrypt._
import enumeratum._
import io.getquill.{EntityQuery, Ord, Query}
import javax.inject.Inject
import play.api.libs.json.{Json, Writes}
import robotCode.LoadCode
import matchmaking.BattleQueue.MatchOutput
import org.joda.time.format.DateTimeFormatterBuilder
import services.Database

object Schema {
  sealed trait Lang extends EnumEntry

  case object Lang extends PlayEnum[Lang] with QuillEnum[Lang] {
    case object Python extends Lang
    case object Javascript extends Lang
    val values = findValues
  }

  sealed trait Team extends EnumEntry

  case object Team extends PlayEnum[Team] with QuillEnum[Team] {
    case object R1 extends Team
    case object R2 extends Team
    val values = findValues
  }

  case class User(
      id: Long = -1,
      email: String,
      username: String,
      password: String,
      created: LocalDateTime
  )

  object User {
    def apply(email: String, username: String, password: String) =
      new User(
        email = email,
        username = username,
        password = password.bcrypt,
        created = LocalDateTime.now()
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
      created: LocalDateTime = LocalDateTime.now()
  ) {
    def isPublished = prId.isDefined
  }

  object Robot {
    def apply(userId: Long, name: String, lang: Lang) =
      new Robot(userId = userId, name = name, devCode = LoadCode(lang), lang = lang)
  }

  case class PublishedRobot(
      id: Long = -1,
      created: LocalDateTime = LocalDateTime.now(),
      code: String
  )

  implicit val robotWrites = new Writes[Robot] {
    def writes(robot: Robot) = Json.obj(
      "id" -> robot.id,
      "userId" -> robot.userId,
      "name" -> robot.name,
      "rating" -> robot.rating,
      "lang" -> robot.lang,
      "published" -> robot.prId.isDefined
    )
  }

  case class Battle(
      id: Long = -1,
      r1Id: Long,
      r2Id: Long,
      pr1Id: Long,
      pr2Id: Long,
      ranked: Boolean = true,
      winner: Option[Team],
      errored: Boolean,
      r1Rating: Int,
      r2Rating: Int,
      r1Time: Float,
      r2Time: Float,
      data: String,
      created: LocalDateTime = LocalDateTime.now(),
  ) {
    def didR1Win(r1Id: Long): Option[Boolean] = {
      winner.map { w =>
        w == Team.R1 && r1Id == r1Id
      }
    }

    val createdFormatter = new DateTimeFormatterBuilder()
      .appendMonthOfYearText()
      .appendLiteral(' ')
      .appendDayOfMonth(1)
      .toFormatter

    def formatCreated(): String = {
      createdFormatter.print(created)
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

  case class PasswordReset(
      id: Long = -1,
      created: LocalDateTime = LocalDateTime.now(),
      token: String = scala.util.Random.alphanumeric.take(15).mkString(""),
      userId: Long
  )

  object PasswordReset {
    def apply(userId: Long) =
      new PasswordReset(userId = userId)
  }

  class Schema @Inject()(db: Database)(implicit ec: scala.concurrent.ExecutionContext) {
    val ctx = db.ctx
    import ctx._

    implicit val userIM = insertMeta[User](_.id)
    implicit val robotIM = insertMeta[Robot](_.id)
    implicit val publishedRobotIM = insertMeta[PublishedRobot](_.id)
    implicit val battleIM = insertMeta[Battle](_.id)

    val users = quote(querySchema[User]("users"))
    // column renaming fixes a really weird issue:
    // (Message, column r.user_id does not exist), (Hint, Perhaps you meant to reference the column "r.userid".)
    val robots = quote(
      querySchema[Robot](
        "robots",
        _.userId -> "user_id",
        _.prId -> "pr_id",
      )
    )
    val publishedRobots = quote(querySchema[PublishedRobot]("published_robots"))
    val battles = quote(
      querySchema[Battle](
        "battles",
        _.r1Id -> "r1_id",
        _.r2Id -> "r2_id",
        _.pr1Id -> "pr1_id",
        _.pr2Id -> "pr2_id"
      )
    )
    val passwordResets = quote(
      querySchema[PasswordReset]("password_reset_tokens", _.userId -> "user_id")
    )

    implicit class RichQuotedQuery[T](query: Quoted[Query[T]]) {
      def paginate(page: Long, numPerPage: Int): Quoted[Query[T]] = quote {
        query.drop(lift(page.toInt * numPerPage)).take(lift(numPerPage))
      }
    }
    implicit class RichQuery[T](query: Query[T]) {
      def arrayAgg = quote {
        infix"array_agg($query)".as[Seq[T]]
      }
    }

    implicit class RobotQuery(query: Quoted[EntityQuery[Robot]]) {
      def withPr(): Quoted[Query[(Robot, PublishedRobot)]] =
        query.join(publishedRobots).on { case (r, pr) => r.prId.contains(pr.id) }

      def withUser(): Quoted[Query[(Robot, User)]] =
        query.join(users).on(_.userId == _.id)

      def byId(id: Long): Quoted[EntityQuery[Robot]] =
        query.filter(_.id == lift(id))

      def byUserId(userId: Long): Quoted[EntityQuery[Robot]] =
        query.filter(_.userId == lift(userId))
    }

    implicit class BattleEntityQuery(query: Quoted[EntityQuery[Battle]]) {
      def byId(id: Long): Quoted[EntityQuery[Battle]] = query.filter(_.id == lift(id))
    }

    implicit class BattleQuery(query: Quoted[Query[Battle]]) {
      def withRobots(): Quoted[Query[(Battle, Robot, Robot)]] =
        for {
          b <- query
          r1 <- robots if b.r1Id == r1.id
          r2 <- robots if b.r2Id == r2.id
        } yield (b, r1, r2)

      def latestFirst(): Quoted[Query[Battle]] =
        query.sortBy(_.created)(Ord.desc)
    }

    implicit class DateQuotes(left: LocalDateTime) {
      def >(right: LocalDateTime) = quote(infix"$left > $right".as[Boolean])

      def <(right: LocalDateTime) = quote(infix"$left < $right".as[Boolean])
    }
  }
}
