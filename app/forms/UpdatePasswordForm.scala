package forms

import play.api.data.Form
import play.api.data.Forms._

object UpdatePasswordForm {

  val form: Form[Data] = Form(
    mapping(
      "password" -> nonEmptyText,
      "token" -> nonEmptyText,
    )(Data.apply)(Data.unapply)
  )

  case class Data(password: String, token: String)

}
