package controllers

import javax.inject._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

import models._
import models.Schema._

@Singleton
class SeasonController @Inject()(
    cc: ControllerComponents,
    assetsFinder: AssetsFinder,
    seasonsRepo: Seasons,
)(implicit ec: ExecutionContext)
    extends AbstractController(cc) {

  def view(slug: String) = Action.async { implicit request =>
    seasonsRepo.find(slug, 10) map {
      case Some(season) => Ok(views.html.season.view(season, assetsFinder))
      case None         => NotFound("404")
    }
  }
}
