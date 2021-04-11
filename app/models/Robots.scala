package models

import controllers.Auth.{LoggedIn, LoggedOut, Visitor}
import io.getquill.{EntityQuery, Ord}
import models.Schema._
import org.joda.time.LocalDateTime
import play.api.Configuration
import services.JodaUtils._

import javax.inject.Inject
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class Robots @Inject()(
    val config: Configuration,
    val schema: Schema,
)(
    implicit ec: ExecutionContext
) {
  val ERROR_LIMIT = config.get[Long]("queue.errorLimit")
  val INACTIVITY_TIMEOUT = config.get[FiniteDuration]("queue.inactivityTimeout")

  import schema._
  import schema.ctx._

  def robotsAuth(visitor: Visitor): Quoted[EntityQuery[Robot]] = {
    visitor match {
      case LoggedIn(user) => robots.filter(r => r.published || r.userId == lift(user.id))
      case LoggedOut()    => robots.filter(r => r.published)
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

  def findAllLatestActiveWithPr(): Future[Seq[(Robot, PRobot)]] = {
    run(for {
      pr <- publishedRobots.latest
      r <- robots.active().filter(_.id == pr.rId)
    } yield (r, pr))
  }

  def create(userId: UserId, name: String, lang: Lang): Future[Robot] = {
    val robot = Robot(userId, name, lang)
    run(robots.insert(lift(robot)).returningGenerated(_.id)).map(robot.copy(_))
  }

  def updateDevCode(id: RobotId, devCode: String): Future[Long] =
    if (devCode.nonEmpty)
      run(robots.by(id).update(_.devCode -> lift(devCode)))
    else throw new Exception("Updating robot with empty code.")

  def updateAfterBattle(id: RobotId, prId: PRobotId, rating: Int, errored: Boolean): Future[Long] =
    run(publishedRobots.by(prId).update(_.rating -> lift(rating)).returning(_.created)) flatMap {
      created =>
        if (created.plus(INACTIVITY_TIMEOUT).isBefore(LocalDateTime.now()))
          run(robots.by(id).update(_.active -> false))
        else if (errored)
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
                  .update(_.active -> false)
              )
            } else {
              Future successful 1L
            }
          } else run(robots.by(id).update(_.errorCount -> 0))

    }

  def publish(
      id: RobotId,
      board: Board,
  ): Future[Option[PublishResult]] = {
    if (board.publishingEnabled)
      run(robots.by(id).leftJoin(publishedRobots.by(board.id).latest).on((r, pr) => r.id == pr.rId))
        .map(_.headOption) flatMap {
        case Some((r, prOption)) =>
          prOption match {
            case Some(pr) if !board.publishCooldownExpired(pr.created) =>
              Future successful Some(Left(pr))
            case _ =>
              for {
                prId <- run(
                  publishedRobots
                    .insert(lift(PRobot(code = r.devCode, rId = r.id, boardId = board.id)))
                    .returningGenerated(_.id)
                )
                _ <- run(
                  robots.by(id).update(_.published -> true, _.active -> true, _.errorCount -> 0)
                )
                // a robot's active status is reset on every publish
              } yield Some(Right(prId))
          }
        case _ =>
          Future successful None
      } else
      Future successful None
  }

  def getLatestPublishedCode(id: RobotId): Future[Option[String]] =
    run(publishedRobots.by(id).sortBy(_.created)(Ord.desc).map(_.code)).map(_.headOption)
}
