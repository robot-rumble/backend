package models

import io.getquill.Ord
import models.Schema._
import org.joda.time.LocalDateTime

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class Seasons @Inject()(schema: Schema, boardsRepo: Boards)(
    implicit ec: ExecutionContext
) {
  import schema._
  import schema.ctx._

  def findAll(): Future[Seq[Season]] =
    run(seasons.sortBy(_.start)(Ord.asc))

  def find(slug: String, numPerBoard: Int): Future[Option[FullSeason]] =
    run(seasons.filter(_.slug == lift(slug)))
      .map(_.headOption)
      .flatMap {
        case Some(season) =>
          boardsRepo.findAll(season.id, numPerBoard) map { fullBoards =>
            Some(FullSeason(season, fullBoards))
          }
        case None => Future successful None
      }

  def create(
      name: String,
      slug: String,
      start: LocalDateTime,
      end: LocalDateTime,
      bio: String
  ): Future[Season] = {
    val season = Season(
      name = name,
      slug = slug,
      start = start,
      end = end,
      bio = bio
    )
    run(seasons.insert(lift(season)).returningGenerated(_.id)).map(season.copy(_))
  }
}
