package services

import org.joda.time.{LocalDateTime, Duration}

import scala.concurrent.duration.FiniteDuration
import scala.language.implicitConversions

object JodaUtils {
  implicit def localDateTimeOrdering: Ordering[LocalDateTime] = Ordering.fromLessThan(_ isBefore _)

  implicit def finiteDurationToJoda(duration: FiniteDuration): Duration = {
    Duration.millis(duration.toMillis)
  }
}
