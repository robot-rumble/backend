package utils

object Text {
  def pluralize[T](name: String, v: Int): String =
    s"$v $name${if (v > 1) "s" else ""}"

}
