package forms

import forms.Validators._
import play.api.data.Form
import play.api.data.Forms._

object LoginForm {

  val form: Form[Data] = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> password
    )(Data.apply)(Data.unapply)
  )

  case class Data(username: String, password: String)

}
