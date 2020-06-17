package db

import com.github.tminglei.slickpg.{PgDate2Support, PgEnumSupport}

trait PostgresProfile
    extends slick.jdbc.PostgresProfile
    with PgEnumSupport
    with PgDate2Support {
  override val api: API = new API {}

  trait API extends super.API with DateTimeImplicits {
    // GROSS! WTF!
    // https://github.com/tminglei/slick-pg/blob/9b1e0dae0dab164998d7da9faa81af7891be9876/src/test/scala/com/github/tminglei/slickpg/PgEnumSupportSuite.scala#L54
    // format: off
    
    import PostgresEnums._

    implicit val langMapper = createEnumJdbcType("lang", Langs)
    implicit val langListMapper = createEnumListJdbcType("lang", Langs)

    implicit val langBuilder = createEnumColumnExtensionMethodsBuilder(Langs)
    implicit val langOptionBuilder = createEnumOptionColumnExtensionMethodsBuilder(Langs)

    implicit val winnerMapper = createEnumJdbcType("winner", Winners)
    implicit val winnerListMapper = createEnumListJdbcType("winner", Winners)

    implicit val winnerBuilder = createEnumColumnExtensionMethodsBuilder(Winners)
    implicit val winnerOptionBuilder = createEnumOptionColumnExtensionMethodsBuilder(Winners)
  }
}

object PostgresProfile extends PostgresProfile
class MyPostgresProfile extends PostgresProfile
