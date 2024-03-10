package forms

import forms.Validators._
import play.api.data.Form
import play.api.data.Forms._

object UpdateRobotForm {

  val form: Form[Data] = Form(
    mapping(
      "name" -> name,
      "bio" -> bio,
      "openSource" -> boolean
    )(Data.apply)(Data.unapply)
  )

  case class Data(name: String, bio: String, openSource: Boolean)

}
