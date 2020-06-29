package db

import org.joda.time.LocalDateTime

object JodaUtils {
  implicit def localDateTimeOrdering: Ordering[LocalDateTime] = Ordering.fromLessThan(_ isBefore _)
}
