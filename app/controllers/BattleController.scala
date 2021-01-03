package controllers

import controllers.Auth.{LoggedIn, LoggedOut, Visitor}
import javax.inject._

import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json.Json
import play.api.mvc._
import forms.{CreateRobotForm, UpdateRobotCodeForm}
import models._
import models.Schema._
import play.api.Configuration

import scala.concurrent.duration.FiniteDuration
import services.JodaUtils._

class BattleController @Inject()(
    cc: ControllerComponents,
    assetsFinder: AssetsFinder,
    auth: Auth.AuthAction,
    battlesRepo: Battles,
    robotsRepo: Robots
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
