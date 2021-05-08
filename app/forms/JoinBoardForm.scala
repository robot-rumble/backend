package forms

import play.api.data.Form
import play.api.data.Forms._

import Validators._

object JoinBoardForm {

  val form: Form[Data] = Form(
    mapping(
      "password" -> boardPassword,
    )(Data.apply)(Data.unapply)
  )

  case class Data(password: String)

}
