package models

import db.PostgresProfile.api._

object Utils {
  def paginate[E, U](
      query: Query[E, U, Seq],
      page: Long,
      numPerPage: Int
  ): Query[E, U, Seq] = {
    query.drop(page * numPerPage).take(page)
  }
}
