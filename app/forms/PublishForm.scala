package forms

import models.Schema.Lang
import play.api.data.Form
import play.api.data.Forms._

object PublishForm {

  val form: Form[Data] = Form(
    mapping(
      "boardId" -> longNumber,
    )(Data.apply)(Data.unapply)
  )

  case class Data(boardId: Long)

}
