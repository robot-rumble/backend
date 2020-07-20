package services

import io.getquill.{PostgresJAsyncContext, SnakeCase}

trait Database {
  val ctx: PostgresJAsyncContext[SnakeCase]
}

class Postgres extends Database {
  val ctx = new PostgresJAsyncContext(SnakeCase, "db.default_")
}
