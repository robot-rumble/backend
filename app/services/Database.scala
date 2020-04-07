package services

import com.google.inject.Inject
import com.zaxxer.hikari.HikariDataSource
import io.getquill.{PostgresDialect, PostgresJdbcContext, SnakeCase}
import io.getquill.context.jdbc.JdbcContext
import play.api.db.Database

trait Db {
  val ctx : JdbcContext[PostgresDialect, SnakeCase]
}

// https://stackoverflow.com/questions/49075152/conflict-between-hikari-quill-and-postgres-in-the-conf-file-for-play-2-6
class Postgres @Inject()(db: Database) extends Db {
  val ctx = new PostgresJdbcContext(SnakeCase, db.dataSource.asInstanceOf[HikariDataSource])
}

