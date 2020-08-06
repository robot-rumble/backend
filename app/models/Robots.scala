package models

import javax.inject.Inject

import scala.concurrent.{ExecutionContext, Future}
import Schema._
import controllers.Auth.{LoggedIn, LoggedOut, Visitor}
import io.getquill.EntityQuery

class Robots @Inject()(
    val schema: Schema,
)(
    implicit ec: ExecutionContext
) {
  import schema.ctx._
  import schema._

  def robotsAuth(visitor: Visitor): Quoted[EntityQuery[Robot]] = {
    visitor match {
      case LoggedIn(user) => robots.filter(r => r.prId.isDefined || r.userId == lift(user.id))
      case LoggedOut()    => robots.filter(r => r.prId.isDefined)
    }
  }

  def find(id: Long)(visitor: Visitor): Future[Option[Robot]] =
    run(robotsAuth(visitor).byId(id)).map(_.headOption)

  def find(userId: Long, name: String)(visitor: Visitor): Future[Option[Robot]] =
    run(robotsAuth(visitor).byUserId(userId).filter(_.name == lift(name.toLowerCase)))
      .map(_.headOption)

  def find(username: String, name: String)(visitor: Visitor): Future[Option[(Robot, User)]] = {
    val query = quote {
      robotsAuth(visitor).withUser().filter {
        case (r, u) => u.username == lift(username.toLowerCase) && r.name == lift(name.toLowerCase)
      }
    }
    run(query).map(_.headOption)
  }

  def findAllPr(): Future[Seq[(Robot, PublishedRobot)]] =
    run(robotsAuth(LoggedOut()).withPr())

  def findAll(userId: Long)(visitor: Visitor): Future[Seq[Robot]] =
    run(robotsAuth(visitor).byUserId(userId))

  def findAllPublishedPaged(page: Long, numPerPage: Int): Future[Seq[(Robot, User)]] =
    run(robotsAuth(LoggedOut()).withUser().paginate(page, numPerPage))

  def create(userId: Long, name: String, lang: Lang): Future[Robot] = {
    val robot = Robot(userId, name.toLowerCase, lang)
    run(robots.insert(lift(robot)).returningGenerated(_.id)).map(robot.copy(_))
  }

  def updateDevCode(id: Long, devCode: String): Future[Long] =
    if (!devCode.isEmpty)
      run(robots.byId(id).update(_.devCode -> lift(devCode)))
    else throw new Exception("Updating robot with empty code.")

  def updateRating(id: Long, rating: Int): Future[Long] =
    run(robots.byId(id).update(_.rating -> lift(rating)))

  def publish(id: Long): Future[Long] =
    for {
      code <- run(robots.byId(id).map(_.devCode)).map(_.head)
      prId <- run(
        publishedRobots.insert(lift(PublishedRobot(code = code))).returningGenerated(_.id)
      )
      _ <- run(robots.byId(id).update(_.prId -> lift(Option(prId))))
    } yield prId

  def getPublishedCode(id: Long): Future[Option[String]] =
    run(robots.byId(id).withPr().map(_._2.code)).map(_.headOption)

  def sortByRating(): Future[Seq[Robot]] =
    run(robots.sortBy(_.rating))
}
