package models

import Schema.Lang

object DefaultCode {
  def apply(lang: Lang): String = {
    lang match {
      case Lang.Python =>
        """
          |def _robot(state, unit, debug):
          |    if state.turn % 2 == 0:
          |        return move(Direction.East)
          |    else:
          |        return attack(Direction.South)
          |""".stripMargin
      case Lang.Javascript =>
        """
          |function _robot(state, unit, debug) {
          |  if (state.turn % 2 === 0) {
          |    return move(Direction.East)
          |  } else {
          |    return attack(Direction.South)
          |  }
          |}
          |""".stripMargin
    }
  }
}
