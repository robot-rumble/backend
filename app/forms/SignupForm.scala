package forms

import forms.Validators.{email, _}
import play.api.data.Form
import play.api.data.Forms._

object SignupForm {

  val form: Form[Data] = Form(
    mapping(
      "email" -> email,
      "username" -> username,
      "password" -> password
    )(Data.apply)(Data.unapply)
  )

  case class Data(email: String, username: String, password: String)

}
