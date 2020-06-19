package forms

import play.api.data.Form
import play.api.data.Forms._

import models.Schema.Lang

object CreateRobotForm {

  val form: Form[Data] = Form(
    mapping(
      "name" -> nonEmptyText,
      "lang" -> Lang.formField
    )(Data.apply)(Data.unapply)
  )

  case class Data(name: String, lang: Lang)

}
