package forms

import play.api.data.Form
import play.api.data.Forms._

object SignupForm {

  val form: Form[Data] = Form(
    mapping(
      "email" -> email,
      "username" -> nonEmptyText(1, 15),
      "password" -> nonEmptyText(10, 60)
    )(Data.apply)(Data.unapply)
  )

  case class Data(email: String, username: String, password: String)

}
