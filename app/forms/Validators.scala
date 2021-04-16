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
    regexConstraint("lowerecase", "^[^A-Z]+$", "Must be all lowercase")
  val noWhitespaceConstraint: Constraint[String] =
    regexConstraint("whitespace", "^[^\\s]+$", "Must not have any whitespace")
  val emailConstraint: Constraint[String] = regexConstraint(
    "email",
    "^\\w+([-+.']\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$",
    "Must be a valid email"
  )

  val username =
    Forms.nonEmptyText(5, 15).verifying(alphanumericConstraint).verifying(allLowercaseConstraint)
  val name =
    Forms.nonEmptyText(5, 15).verifying(namingConstraint).verifying(allLowercaseConstraint)
  val password = Forms.nonEmptyText(10, 60).verifying(noWhitespaceConstraint)
  val email = Forms.email.verifying(emailConstraint).verifying(allLowercaseConstraint)
}
