package controllers

object SignupForm {

  import play.api.data.Form
  import play.api.data.Forms._

  val form: Form[Data] = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText
    )(Data.apply)(Data.unapply)
  )

  case class Data(username: String, password: String)

}
