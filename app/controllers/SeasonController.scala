package controllers

import models._
import play.api.mvc._

import javax.inject._
import scala.concurrent.ExecutionContext

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
