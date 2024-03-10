package forms

import forms.Validators._
import play.api.data.Form
import play.api.data.Forms._

object UpdateUserForm {

  val form: Form[Data] = Form(
    mapping(
      "bio" -> bio
    )(Data.apply)(Data.unapply)
  )

  case class Data(bio: String)

}
