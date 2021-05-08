package forms

import play.api.data.Form
import play.api.data.Forms._

import Validators._

object CreateBoardForm {

  val form: Form[Data] = Form(
    mapping(
      "name" -> boardName,
      "bio" -> bio,
      "password" -> boardPassword,
    )(Data.apply)(Data.unapply)
  )

  case class Data(name: String, bio: String, password: String)
}
