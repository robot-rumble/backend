package controllers

import javax.inject._
import models.{User, Users}
import play.api.mvc._

@Singleton
class UserController @Inject()(cc: MessagesControllerComponents, repo: Users, assetsFinder: AssetsFinder)
  extends MessagesAbstractController(cc) {

  import LoginForm._

  def create: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.signup(form, assetsFinder))
  }

  def postCreate: Action[AnyContent] = Action { implicit request =>
    form.bindFromRequest.fold(
      formWithErrors => {
        BadRequest(views.html.signup(formWithErrors, assetsFinder))
      },
      data => {
        val user = User(username = data.username, password = data.password, id = 0)
        repo.create(user)
        Redirect(routes.UserController.profile(user.username)).flashing("info" -> "Account created!")
      }
    )
  }

  def profile(username: String): Action[AnyContent] = Action { implicit request =>
    repo.find(username) match {
      case Some(user) => Ok(views.html.profile(user, assetsFinder))
      case None => NotFound("User does not exist!")
    }
  }
}
