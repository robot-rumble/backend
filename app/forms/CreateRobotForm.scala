package forms

import models.Schema.Lang
import play.api.data.Form
import play.api.data.Forms._

object CreateRobotForm {

  val form: Form[Data] = Form(
    mapping(
      "name" -> nonEmptyText(1, 15),
      "lang" -> Lang.formField
    )(Data.apply)(Data.unapply)
  )

  case class Data(name: String, lang: Lang)

}
