package forms

import play.api.data.Form
import play.api.data.Forms._

object PasswordResetForm {

  val form: Form[Data] = Form(
    mapping(
      "email" -> email,
    )(Data.apply)(Data.unapply)
  )

  case class Data(email: String)

}
