package models

import io.getquill.{PostgresDialect, SnakeCase}
import io.getquill.context.jdbc.JdbcContext
import org.postgresql.util.PGobject

object QuillUtils {
  // https://github.com/getquill/quill/issues/1129

  type ctx = JdbcContext[PostgresDialect, SnakeCase]

  def serialize[E <: Enumeration](
      enum: E,
      value: String
  ): Option[enum.Value] = {
    enum.values.find(_.toString == value)
  }

  def generateEnumDecoder[E <: Enumeration](
      quillCtx: ctx,
      enum: E
  ): quillCtx.Decoder[enum.Value] = {
    quillCtx.decoder(
      (index, row) => serialize(enum, row.getObject(index).toString).get
    )
  }

  def generateEnumEncoder[E <: Enumeration](
      quillCtx: ctx,
      enum: E,
      pgTypeName: String
  ): quillCtx.Encoder[enum.Value] = {
    quillCtx.encoder(
      java.sql.Types.OTHER,
      (index, value, row) => {
        val pgObj = new PGobject()
        pgObj.setType(pgTypeName)
        pgObj.setValue(value.toString)
        row.setObject(index, pgObj, java.sql.Types.OTHER)
      }
    )
  }
}
