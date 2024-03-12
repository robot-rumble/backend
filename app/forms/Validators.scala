package forms

import play.api.data.Forms
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}

object Validators {
  private def regexConstraint(name: String, regex: String, message: String): Constraint[String] =
    Constraint(s"constraints.$name")({ string =>
      if (string.matches(regex)) Valid
      else Invalid(Seq(ValidationError(message)))
    })

  val alphanumericConstraint: Constraint[String] = regexConstraint(
    "alphanumeric",
    "^[a-zA-Z0-9]+$",
    "Must only contain letters or numbers"
  )
  val namingConstraint: Constraint[String] = regexConstraint(
    "naming",
    "^[a-zA-Z0-9-]+$",
    "Must only contain letters, numbers, or a dash"
  )
  val allLowercaseConstraint: Constraint[String] =
    regexConstraint("lowercase", "^[^A-Z]+$", "Must be all lowercase")
  val noWhitespaceConstraint: Constraint[String] =
    regexConstraint("whitespace", "^[^\\s]+$", "Must not have any whitespace")

  val bio = Forms.text

  val username =
    Forms.nonEmptyText(3, 15).verifying(alphanumericConstraint).verifying(allLowercaseConstraint)
  val email = Forms.email.verifying(allLowercaseConstraint)

  val name =
    Forms.nonEmptyText(3, 15).verifying(namingConstraint).verifying(allLowercaseConstraint)
  val password = Forms.nonEmptyText(10, 60).verifying(noWhitespaceConstraint)

  val boardName = Forms.nonEmptyText(3, 30)
  val boardPassword = Forms.nonEmptyText(5, 30).verifying(noWhitespaceConstraint)
}
