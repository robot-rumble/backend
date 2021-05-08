package forms

import forms.Validators._
import models.Schema.Lang
import play.api.data.Form
import play.api.data.Forms._

object CreateRobotForm {

  val form: Form[Data] = Form(
    mapping(
      "name" -> name,
      "lang" -> Lang.formField,
      "openSource" -> boolean,
    )(Data.apply)(Data.unapply)
  )

  case class Data(name: String, lang: Lang, openSource: Boolean)

}
