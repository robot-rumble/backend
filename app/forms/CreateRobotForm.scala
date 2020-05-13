package forms

object CreateRobotForm {

  import play.api.data.Form
  import play.api.data.Forms._

  val form: Form[Data] = Form(
    mapping(
      "name" -> nonEmptyText,
      "lang" -> nonEmptyText
    )(Data.apply)(Data.unapply)
  )

  case class Data(name: String, lang: String)

}