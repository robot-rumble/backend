package forms

import play.api.data.Form
import play.api.data.Forms._

object SignupForm {

  val form: Form[Data] = Form(
    mapping(
      "email" -> email,
      "username" -> nonEmptyText,
      "password" -> nonEmptyText
    )(Data.apply)(Data.unapply)
  )

  case class Data(email: String, username: String, password: String)

}
