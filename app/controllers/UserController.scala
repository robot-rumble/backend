package controllers

import javax.inject._
import models.{User, Users}
import play.api.data._
import play.api.mvc._

@Singleton
class UserController @Inject()(cc: MessagesControllerComponents, repo: Users, assetsFinder: AssetsFinder)
  extends MessagesAbstractController(cc) {

  import LoginForm._

  def create: Action[AnyContent] = Action { implicit request =>
    val successFunction = { data: Data =>
      val user = User(username = data.username, password = data.password, id = 0)
      repo.create(user)
      Redirect(routes.UserController.profile(user.username)).flashing("info" -> "Account created!")
    }

    val errorFunction = { formWithErrors: Form[Data] =>
      BadRequest(views.html.signup(formWithErrors, assetsFinder))
    }

    val formValidationResult = form.bindFromRequest
    formValidationResult.fold(errorFunction, successFunction)
  }

  def profile(username: String): Action[AnyContent] = Action { implicit request =>
    repo.find(username) match {
      case Some(user) => Ok(views.html.profile(user, assetsFinder))
      case None => NotFound("User does not exist!")
    }
  }
}
