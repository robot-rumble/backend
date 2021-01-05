package models

import org.joda.time.{DateTime, DateTimeZone, Duration, LocalDateTime}
import com.github.t3hnar.bcrypt._
import enumeratum._
import io.getquill.{EntityQuery, Query}
import javax.inject.Inject
import play.api.libs.json.{Json, Writes}
import utils.LoadCode
import matchmaking.BattleQueue.MatchOutput
import org.joda.time.format.{DateTimeFormatterBuilder, PeriodFormatterBuilder}
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

  case class UserId(id: Long)

  case class Email(email: String)

  object Email {
    def apply(email: String) =
      new Email(email.toLowerCase)
  }

  case class User(
      id: UserId = UserId(-1),
      email: Email,
      username: String,
      password: String,
      created: LocalDateTime
  )

  object User {
    def apply(email: Email, username: String, password: String) =
      new User(
        email = email,
        username = username,
        password = password.bcrypt,
        created = LocalDateTime.now()
      )
  }

  case class RobotId(id: Long)

  case class Robot(
      id: RobotId = RobotId(-1),
      userId: UserId,
      name: String,
      devCode: String,
      automatch: Boolean = true,
      lang: Lang,
      created: LocalDateTime = LocalDateTime.now(),
      published: Boolean = false,
  )

  case class FullRobot(robot: Robot, user: User)

  case class FullBoardRobot(robot: Robot, pRobot: PRobot, user: User)

  // if publish cooldown hasn't expired, return the probot. otherwise return the new inserted id
  type PublishResult = Either[PRobot, PRobotId]

  object Robot {
    def apply(userId: UserId, name: String, lang: Lang) =
      new Robot(
        userId = userId,
        name = name,
        devCode = LoadCode(lang),
        lang = lang,
      )
  }

  case class PRobotId(id: Long)

  case class PRobot(
      id: PRobotId = PRobotId(-1),
      rId: RobotId,
      boardId: BoardId,
      created: LocalDateTime = LocalDateTime.now(),
      code: String,
      rating: Int = 1000,
  )

  implicit val robotWrites = new Writes[Robot] {
    def writes(robot: Robot) = Json.obj(
      "id" -> robot.id.id,
      "userId" -> robot.userId.id,
      "name" -> robot.name,
      "lang" -> robot.lang,
      "published" -> robot.published
    )
  }

  case class BattleId(id: Long)

  case class Battle(
      id: BattleId = BattleId(-1),
      boardId: BoardId,
      r1Id: RobotId,
      r2Id: RobotId,
      pr1Id: PRobotId,
      pr2Id: PRobotId,
      ranked: Boolean = true,
      winner: Option[Team],
      errored: Boolean,
      pr1Rating: Int,
      pr2Rating: Int,
      pr1RatingChange: Int,
      pr2RatingChange: Int,
      r1Time: Float,
      r2Time: Float,
      data: Array[Byte],
      created: LocalDateTime = LocalDateTime.now(),
  ) {
    def didRobotWin(rId: RobotId): Option[Boolean] = {
      winner.map {
        case Team.R1 => rId == r1Id
        case Team.R2 => rId == r2Id
      }
    }

    val createdFormatter = new DateTimeFormatterBuilder()
      .appendMonthOfYear(2)
      .appendLiteral('/')
      .appendDayOfMonth(2)
      .toFormatter

    def formatCreated(): String = {
      createdFormatter.print(created)
    }

    val createdTimeFormatter = new DateTimeFormatterBuilder()
      .appendMonthOfYear(2)
      .appendLiteral('/')
      .appendDayOfMonth(2)
      .appendLiteral(' ')
      .appendHourOfDay(2)
      .appendLiteral(':')
      .appendMinuteOfHour(2)
      .toFormatter

    def formatCreatedTime(): String = {
      createdTimeFormatter.print(created)
    }

    def decompressData(): String = {
      utils.Gzip.decompress(data)
    }
  }

  case class FullBattle(b: Battle, r1: Robot, r2: Robot)

  object FullBattle {
    def tupled(t: (Battle, Robot, Robot)) = FullBattle(t._1, t._2, t._3)
  }

  object Battle {
    def apply(
        matchOutput: MatchOutput,
        pr1Rating: Int,
        pr1RatingChange: Int,
        pr2Rating: Int,
        pr2RatingChange: Int
    ) =
      new Battle(
        boardId = BoardId(matchOutput.boardId),
        r1Id = RobotId(matchOutput.r1Id),
        pr1Id = PRobotId(matchOutput.pr1Id),
        r2Id = RobotId(matchOutput.r2Id),
        pr2Id = PRobotId(matchOutput.pr2Id),
        winner = matchOutput.winner,
        errored = matchOutput.errored,
        r1Time = matchOutput.r1Time,
        r2Time = matchOutput.r2Time,
        data = utils.Gzip.compress(matchOutput.data),
        pr1Rating = pr1Rating,
        pr2Rating = pr2Rating,
        pr1RatingChange = pr1RatingChange,
        pr2RatingChange = pr2RatingChange
      )
  }

  case class BoardId(id: Long)

  case class Board(
      id: BoardId = BoardId(-1),
      name: String,
      bio: Option[String],
      seasonId: Option[SeasonId],
      adminId: Option[UserId],
      password: Option[String],
      publishingEnabled: Boolean,
      matchmakingEnabled: Boolean,
      publishCooldown: Duration,
      publishBattleNum: Int,
      battleCooldown: Duration,
      recurrentBattleNum: Int
  ) {
    val publishCooldownFormatter = new PeriodFormatterBuilder()
      .printZeroRarelyFirst()
      .appendHours()
      .appendSuffix(" hour", " hours")
      .appendSeparator(" ")
      .appendMinutes()
      .appendSuffix(" minute", " minutes")
      .toFormatter

    def formatPublishCooldown(): String = {
      publishCooldownFormatter.print(publishCooldown.toPeriod())
    }

    def publishCooldownExpired(time: LocalDateTime): Boolean =
      time.plus(publishCooldown).isBefore(LocalDateTime.now())

    val publishTimeFormatter = new DateTimeFormatterBuilder()
      .appendHourOfDay(1)
      .appendLiteral(':')
      .appendMinuteOfHour(2)
      .appendLiteral(" ")
      .appendTimeZoneShortName()
      .appendLiteral(" on ")
      .appendDayOfWeekText()
      .toFormatter

    def formatNextPublishTime(time: LocalDateTime): String = {
      publishTimeFormatter.print(
        time.toDateTime(DateTimeZone.forID("US/Eastern")).plus(publishCooldown)
      )
    }
  }

  case class FullBoard(board: Board, robots: Seq[FullBoardRobot])

  case class BoardWithBattles(board: Board, battles: Seq[FullBattle])

  case class SeasonId(id: Long)

  case class Season(
      id: SeasonId = SeasonId(-1),
      name: String,
      slug: String,
      bio: String,
      start: LocalDateTime,
      end: LocalDateTime,
  ) {
    val dateFormatter = new DateTimeFormatterBuilder()
      .appendMonthOfYear(2)
      .appendLiteral('/')
      .appendDayOfMonth(2)
      .toFormatter

    def isActive(): Boolean = {
      LocalDateTime.now().isAfter(start) && LocalDateTime.now().isBefore(end)
    }

    def formatStart(): String = {
      dateFormatter.print(start)
    }

    def formatEnd(): String = {
      dateFormatter.print(`end`)
    }
  }

  case class FullSeason(season: Season, boards: Seq[FullBoard])

  case class PasswordReset(
      id: Long = -1,
      created: LocalDateTime = LocalDateTime.now(),
      token: String = scala.util.Random.alphanumeric.take(15).mkString(""),
      userId: UserId
  )

  object PasswordReset {
    def apply(userId: UserId) =
      new PasswordReset(userId = userId)
  }
  class Schema @Inject()(db: Database)(implicit ec: scala.concurrent.ExecutionContext) {
    val ctx = db.ctx
    import ctx._

    implicit val encodeUserId = MappedEncoding[UserId, Long](_.id)
    implicit val decodeUserId = MappedEncoding[Long, UserId](UserId.apply)
    implicit val encodeRobotId = MappedEncoding[RobotId, Long](_.id)
    implicit val decodeRobotId = MappedEncoding[Long, RobotId](RobotId.apply)
    implicit val encodePRobotId = MappedEncoding[PRobotId, Long](_.id)
    implicit val decodePRobotId = MappedEncoding[Long, PRobotId](PRobotId.apply)
    implicit val encodeBattleId = MappedEncoding[BattleId, Long](_.id)
    implicit val decodeBattleId = MappedEncoding[Long, BattleId](BattleId.apply)
    implicit val encodeBoardId = MappedEncoding[BoardId, Long](_.id)
    implicit val decodeBoardId = MappedEncoding[Long, BoardId](BoardId.apply)
    implicit val encodeSeasonId = MappedEncoding[SeasonId, Long](_.id)
    implicit val decodeSeasonId = MappedEncoding[Long, SeasonId](SeasonId.apply)
    implicit val encodeEmail = MappedEncoding[Email, String](_.email)
    implicit val decodeEmail = MappedEncoding[String, Email](Email.apply)
    implicit val encodeDuration = MappedEncoding[Duration, Int](_.getStandardMinutes.toInt)
    implicit val decodeDuration = MappedEncoding[Int, Duration](Duration.standardSeconds(_))

    val users = quote(querySchema[User]("users"))
    // column renaming fixes a really weird issue:
    // (Message, column r.user_id does not exist), (Hint, Perhaps you meant to reference the column "r.userid".)
    val robots = quote(
      querySchema[Robot](
        "robots",
        _.userId -> "user_id",
      )
    )
    val publishedRobots = quote(querySchema[PRobot]("published_robots", _.boardId -> "board_id"))
    val battles = quote(
      querySchema[Battle](
        "battles",
        _.boardId -> "board_id",
        _.r1Id -> "r1_id",
        _.r2Id -> "r2_id",
        _.pr1Id -> "pr1_id",
        _.pr2Id -> "pr2_id"
      )
    )
    val boards = quote(
      querySchema[Board](
        "boards",
        _.seasonId -> "season_id",
        _.adminId -> "admin_id"
      )
    )
    val seasons = quote(
      querySchema[Season](
        "seasons",
        _.end -> "end_"
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

    implicit class UserEntityQuery(query: Quoted[EntityQuery[User]]) {
      def by(id: UserId): Quoted[EntityQuery[User]] =
        query.filter(_.id == lift(id))

      def by(username: String): Quoted[EntityQuery[User]] =
        query.filter(_.username == lift(username))

      def by(email: Email): Quoted[EntityQuery[User]] =
        query.filter(_.email == lift(email))
    }

    implicit class RobotEntityQuery(query: Quoted[EntityQuery[Robot]]) {
      def by(id: RobotId): Quoted[EntityQuery[Robot]] =
        query.filter(_.id == lift(id))

      def by(id: UserId): Quoted[EntityQuery[Robot]] =
        query.filter(_.userId == lift(id))

      def withUser(): Quoted[Query[(Robot, User)]] =
        for {
          r <- query
          u <- users if r.userId == u.id
        } yield (r, u)
    }

    implicit class PRobotEntityQuery(query: Quoted[EntityQuery[PRobot]]) {
      def by(id: PRobotId): Quoted[EntityQuery[PRobot]] =
        query.filter(_.id == lift(id))

      def by(id: RobotId): Quoted[EntityQuery[PRobot]] =
        query.filter(_.rId == lift(id))

      def by(id: BoardId): Quoted[EntityQuery[PRobot]] =
        query.filter(_.boardId == lift(id))

      def latest: Quoted[Query[PRobot]] = {
        val latestDateForBoard = quote {
          publishedRobots.groupBy(pr => (pr.boardId, pr.rId)).map {
            case (ids, pRobots) =>
              (ids, pRobots.map(_.created).max)
          }
        }
        for {
          pr <- query
          ((boardId, rId), latestDate) <- latestDateForBoard
          if pr.boardId == boardId && pr.rId == rId && latestDate.contains(pr.created)
        } yield pr
      }
    }

    implicit class BattleEntityQuery(query: Quoted[EntityQuery[Battle]]) {
      def by(id: BattleId): Quoted[EntityQuery[Battle]] = query.filter(_.id == lift(id))

      def by(id: BoardId): Quoted[EntityQuery[Battle]] =
        query.filter(_.boardId == lift(id))

      def withRobots(): Quoted[Query[(Battle, Robot, Robot)]] =
        for {
          b <- query
          r1 <- robots if b.r1Id == r1.id
          r2 <- robots if b.r2Id == r2.id
        } yield (b, r1, r2)
    }

    implicit class BoardEntityQuery(query: Quoted[EntityQuery[Board]]) {
      def by(id: BoardId): Quoted[EntityQuery[Board]] = query.filter(_.id == lift(id))

      def by(seasonId: SeasonId): Quoted[EntityQuery[Board]] =
        query.filter(_.seasonId.contains(lift(seasonId)))
    }

    implicit class DateQuotes(left: LocalDateTime) {
      def >(right: LocalDateTime) = quote(infix"$left > $right".as[Boolean])

      def <(right: LocalDateTime) = quote(infix"$left < $right".as[Boolean])
    }
  }
}
