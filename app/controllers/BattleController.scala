package controllers

import controllers.Auth.{LoggedIn, LoggedOut}
import models.Schema._
import models._
import play.api.mvc._

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

class BattleController @Inject()(
    cc: ControllerComponents,
    assetsFinder: AssetsFinder,
    auth: Auth.AuthAction,
    boardsRepo: Boards,
)(implicit ec: ExecutionContext)
    extends AbstractController(cc) {
  def view(battleId: Long) = auth.action { visitor => implicit request =>
    boardsRepo.findBattle(BattleId(battleId))(visitor) flatMap {
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

        boardsRepo.findBare(b.boardId)(visitor) map {
          case Some(board) =>
            Ok(views.html.battle.view(fullBattle, userTeam, userOwnsOpponent, board.gameMode, assetsFinder))
          case None => InternalServerError("500")
        }

      case None => Future successful NotFound("404")
    }
  }
}
