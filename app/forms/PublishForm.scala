package forms

import models.Schema.Lang
import play.api.data.Form
import play.api.data.Forms._

object PublishForm {

  val form: Form[Data] = Form(
    mapping(
      "robotId" -> longNumber,
    )(Data.apply)(Data.unapply)
  )

  case class Data(robotId: Long)

}
