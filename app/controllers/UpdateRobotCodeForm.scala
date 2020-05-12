package controllers

object UpdateRobotCodeForm {
  import play.api.data.Form
  import play.api.data.Forms._

  val form: Form[Data] = Form(
    mapping(
      "code" -> nonEmptyText
    )(Data.apply)(Data.unapply)
  )

  case class Data(code: String)
}
