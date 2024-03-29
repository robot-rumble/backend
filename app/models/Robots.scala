package models

import controllers.Auth.{LoggedIn, LoggedOut, Visitor}
import io.getquill.{EntityQuery, Ord}
import models.Schema.{DeactivationReason, _}
import org.joda.time.LocalDateTime
import play.api.Configuration
import services.JodaUtils._
import services.Markdown

import javax.inject.Inject
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class Robots @Inject()(
    val config: Configuration,
    val schema: Schema,
    val usersRepo: Users,
    val markdown: Markdown
)(
    implicit ec: ExecutionContext
) {
  val ERROR_LIMIT = config.get[Long]("queue.errorLimit")
  val INACTIVITY_TIMEOUT = config.get[FiniteDuration]("queue.inactivityTimeout")
  val INACTIVITY_RATING_CUTOFF = config.get[Int]("queue.inactivityRatingCutoff")
  val MAX_RATING_CHANGE = config.get[Int]("queue.maxRatingChange")

  import schema._
  import schema.ctx._

  val BUILTIN_USER = UserId(config.get[Long]("site.builtinUserId"))

  def robotsAuth(visitor: Visitor): Quoted[EntityQuery[Robot]] = {
    visitor match {
      case LoggedIn(user) =>
        robots.filter(
          r => r.published || r.userId == lift(BUILTIN_USER) || r.userId == lift(user.id)
        )
      case LoggedOut() => robots.filter(r => r.published || r.userId == lift(BUILTIN_USER))
    }
  }

  def find(id: RobotId)(visitor: Visitor): Future[Option[FullRobot]] =
    run(robotsAuth(visitor).by(id).withUser()).map(_.headOption).map(_.map(FullRobot.tupled))

  def findBare(id: RobotId)(visitor: Visitor): Future[Option[Robot]] =
    run(robotsAuth(visitor).by(id)).map(_.headOption)

  def findLatestPr(id: RobotId, boardId: BoardId): Future[Option[PRobot]] = {
    run(publishedRobots.by(boardId).by(id).latest).map(_.headOption)
  }

  def findAllPr(id: RobotId): Future[Seq[PRobot]] =
    run(publishedRobots.by(id))

  def findAllLatestPr(id: RobotId): Future[Seq[PRobot]] =
    run(publishedRobots.by(id).latest)

  def findBare(userId: UserId, name: String)(visitor: Visitor): Future[Option[Robot]] =
    run(robotsAuth(visitor).by(userId).filter(_.name == lift(name)))
      .map(_.headOption)

  def find(username: String, name: String)(visitor: Visitor): Future[Option[FullRobot]] = {
    val query = quote {
      robotsAuth(visitor).withUser().filter {
        case (r, u) =>
          u.username == lift(username) && r.name == lift(name)
      }
    }
    run(query).map(_.headOption).map(_.map(FullRobot.tupled))
  }

  def findAllBoardIds(id: RobotId): Future[Option[(Robot, Seq[BoardId])]] = {
    findBare(id)(LoggedOut()).flatMap {
      case Some(robot) =>
        findAllPr(id) map { pRobots =>
          Some(robot, pRobots.map(_.boardId).distinct)
        }
      case None => Future successful None
    }
  }

  def findAll(userId: UserId)(visitor: Visitor): Future[Seq[Robot]] =
    run(robotsAuth(visitor).by(userId))

  def findAllLatestByBoardPaged(
      boardId: BoardId,
      page: Long,
      numPerPage: Int
  ): Future[Seq[FullBoardRobot]] = {
    val query = quote {
      for {
        pr <- publishedRobots.by(boardId).latest.sortBy(_.rating)(Ord.desc)
        (r, u) <- robots.withUser().filter(_._1.id == pr.rId)
      } yield (r, pr, u)
    }
    run(
      query
        .paginate(page, numPerPage)
    ).map(_.map(FullBoardRobot.tupled))
  }

  def findAllLatestPrForActive(): Future[Seq[(Robot, PRobot)]] = {
    run(publishedRobots.latest) flatMap { latestPrs =>
      run(robots.active()) map { latestRs =>
        latestRs.flatMap(r => latestPrs.filter(_.rId == r.id).map(pr => (r, pr)))
      }
    }
  }

  def create(
      userId: UserId,
      name: String,
      lang: Lang,
      openSource: Boolean,
      bio: String
  ): Future[Option[Robot]] = {
    usersRepo.find(userId) flatMap {
      case Some(user) if user.verified =>
        val robot = Robot(userId, name, lang, openSource, bio, markdown)
        run(robots.insert(lift(robot)).returningGenerated(_.id)).map(robot.copy(_)).map(Some(_))
      case _ => Future successful None
    }
  }

  def updateDevCode(id: RobotId, devCode: String): Future[Long] =
    if (devCode.nonEmpty)
      run(robots.by(id).update(_.devCode -> lift(devCode)))
    else throw new Exception("Updating robot with empty code.")

  def update(id: RobotId, name: String, bio: String, openSource: Boolean): Future[Long] =
    run(robots.by(id).update(_.name -> lift(name), _.bio -> lift(bio), _.renderedBio -> lift(markdown.render(bio)), _.openSource -> lift(openSource)))

  def deactivate(id: RobotId): Future[Long] =
    run(robots.by(id).update(_.active -> false, _.deactivationReason -> lift(Some(DeactivationReason.Manual): Option[DeactivationReason])))

  def updateAfterBattle(
      id: RobotId,
      prId: PRobotId,
      glickoSettings: GlickoSettings,
      oldRating: Int,
      errored: Boolean
  ): Future[Int] = {
    val ratingChange = Math.min(Math.abs(glickoSettings.rating - oldRating), MAX_RATING_CHANGE)
    val ratingConstrained = if (glickoSettings.rating - oldRating > 0) { oldRating + ratingChange } else {
      oldRating - ratingChange
    }
    val rating = Math.max(ratingConstrained, 0)

    run(
      publishedRobots
        .by(prId)
        .update(
          _.rating -> lift(rating),
          _.deviation -> lift(glickoSettings.deviation),
          _.volatility -> lift(glickoSettings.volatility)
        )
        .returning(pr => (pr.created, pr.rating))
    ) flatMap { case (created, rating) =>
      if (created.plus(INACTIVITY_TIMEOUT).isBefore(LocalDateTime.now()) && rating < INACTIVITY_RATING_CUTOFF) {
        run(
          robots
            .by(id)
            .update(_.active -> false, _.deactivationReason -> lift(Some(DeactivationReason.Inactivity): Option[DeactivationReason]))
        )
      } else if (errored)
        run(
          robots
            .by(id)
            .update(r => (r.errorCount -> (r.errorCount + 1)))
            .returning(_.errorCount)
        ) flatMap { errorCount =>
          if (errorCount >= ERROR_LIMIT) {
            run(
              robots
                .by(id)
                .update(_.active -> false, _.deactivationReason -> lift(Some(DeactivationReason.Errored): Option[DeactivationReason]))
            )
          } else {
            Future successful 1L
          }
        } else run(robots.by(id).update(_.errorCount -> 0))
    } map (_ => rating)
  }

  def getLatestPublishedCode(id: RobotId)(visitor: Visitor): Future[Option[String]] = {
    findBare(id)(visitor) flatMap {
      case Some(robot) if robot.openSource =>
        println(robot)
        run(publishedRobots.by(id).sortBy(_.created)(Ord.desc).map(_.code)).map(_.headOption)
      case _ =>
        Future successful None
    }
  }
}
