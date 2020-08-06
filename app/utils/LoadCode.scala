package utils

import models.Schema.Lang

object LoadCode {
  val PYTHON =
    """
      |def robot(state, unit):
      |    if state.turn % 2 == 0:
      |        return Action.move(Direction.East)
      |    else:
      |        return Action.attack(Direction.South)
      |""".stripMargin
  val JAVASCRIPT =
    """
      |function robot(state, unit) {
      |  if (state.turn % 2 === 0) {
      |    return Action.move(Direction.East)
      |  } else {
      |    return Action.attack(Direction.South)
      |  }
      |}
      |""".stripMargin

  def apply(lang: Lang): String = {
    lang match {
      case Lang.Python     => PYTHON
      case Lang.Javascript => JAVASCRIPT
    }
  }
}
