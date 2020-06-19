package services

import io.getquill.context.sql.SqlContext
import io.getquill.{FinaglePostgresContext, PostgresDialect, SnakeCase}

trait Database {
  val ctx: FinaglePostgresContext[SnakeCase]
}

class Postgres extends Database {
  val ctx = new FinaglePostgresContext(SnakeCase, "db.finagle")
}
