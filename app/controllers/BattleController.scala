package controllers

import controllers.Auth.{LoggedIn, LoggedOut}
import models.Schema._
import models._
import play.api.mvc._

import javax.inject._
import scala.concurrent.ExecutionContext

class BattleController @Inject()(
    cc: ControllerComponents,
    assetsFinder: AssetsFinder,
    auth: Auth.AuthAction,
    battlesRepo: Battles,
)(implicit ec: ExecutionContext)
    extends AbstractController(cc) {
  def view(battleId: Long) = auth.action { visitor => implicit request =>
    battlesRepo.find(BattleId(battleId)) map {
      case Some(fullBattle @ FullBattle(b, r1, r2)) =>
        val (userTeam, userOwnsOpponent) = visitor match {
          case LoggedIn(user) =>
            if (user.id == r1.userId && user.id == r2.userId) {
              (Some("Blue"), true)
            } else if (r1.userId == user.id) {
              (Some("Blue"), false)
            } else if (r2.userId == user.id) {
              (Some("Red"), false)
            } else {
              (None, false)
            }
          case LoggedOut() =>
            (None, false)

        }
        Ok(views.html.battle.view(fullBattle, userTeam, userOwnsOpponent, assetsFinder))
      case None => NotFound("404")
    }
  }
}
