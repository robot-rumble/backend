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
        val userTeam = visitor match {
          case LoggedIn(user) =>
            if (r1.userId == user.id) {
              Some("Blue")
            } else if (r2.userId == user.id) {
              Some("Red")
            } else { None }
          case LoggedOut() => None
        }
        Ok(views.html.battle.view(fullBattle, userTeam, assetsFinder))
      case None => NotFound("404")
    }
  }
}
