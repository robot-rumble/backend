package db

import com.github.tminglei.slickpg._
import com.github.tminglei.slickpg.agg.PgAggFuncSupport.GeneralAggFunctions._
import slick.ast._
import slick.compiler.CompilerState

// format: off

trait PostgresProfile extends slick.jdbc.PostgresProfile with PgEnumSupport with PgDate2Support {
  override val api: API = new API {}

  trait API extends super.API with DateTimeImplicits {
    // GROSS! WTF!
    // https://github.com/tminglei/slick-pg/blob/9b1e0dae0dab164998d7da9faa81af7891be9876/src/test/scala/com/github/tminglei/slickpg/PgEnumSupportSuite.scala#L54
    
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
  
  // https://github.com/tminglei/slick-pg/blob/9b1e0dae0dab164998d7da9faa81af7891be9876/core/src/main/scala/com/github/tminglei/slickpg/ExPostgresProfile.scala#L67

  class QueryBuilder(tree: Node, state: CompilerState) extends super.QueryBuilder(tree, state) {
    import slick.util.MacroSupport.macroSupportInterpolation
    override def expr(n: Node, skipParens: Boolean = false) = n match {
      case agg.AggFuncExpr(func, params, orderBy, filter, distinct, forOrdered) =>
        if (func == Library.CountAll) b"${func.name}"
        else {
          b"${func.name}("
          if (distinct) b"distinct "
          b.sep(params, ",")(expr(_, true))
          if (orderBy.nonEmpty && !forOrdered) buildOrderByClause(orderBy)
          b")"
        }
        if (orderBy.nonEmpty && forOrdered) { b" within group ("; buildOrderByClause(orderBy); b")" }
        if (filter.isDefined) { b" filter ("; buildWhereClause(filter); b")" }
      case _ => super.expr(n, skipParens)
    }
  }
}

object PostgresProfile extends PostgresProfile
class MyPostgresProfile extends PostgresProfile
