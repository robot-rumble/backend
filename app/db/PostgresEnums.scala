package db

import play.api.libs.json.{Reads, Writes}

object PostgresEnums {
  object Langs extends Enumeration {
    type Lang = Value
    val PYTHON = Value("PYTHON")
    val JAVASCRIPT = Value("JAVASCRIPT")

    implicit val winnerReads = Reads.enumNameReads(Langs)
    implicit val winnerWrites = Writes.enumNameWrites
  }

  object Winners extends Enumeration {
    type Winner = Value
    val R1 = Value("R1")
    val R2 = Value("R2")
    val Draw = Value("Draw")

    implicit val winnerReads = Reads.enumNameReads(Winners)
    implicit val winnerWrites = Writes.enumNameWrites
  }
}
