package models

import javax.inject.Inject
import play.api.libs.json.{Json, Writes}
import services.Db

object Robots {

  private def createData(userId: Long, name: String): Data = {
    Data(name = name, userId = userId)
  }

  case class Data(
      id: Long = -1,
      userId: Long,
      name: String,
      devCode: String = "",
      automatch: Boolean = true,
      isPublished: Boolean = false,
      rating: Int = 1000,
  )

  def dataToBasic(data: Data): BasicData =
    BasicData(
      id = data.id,
      userId = data.userId,
      name = data.name,
      rating = data.rating,
      isPublished = data.isPublished
    )

  case class BasicData(
      id: Long,
      userId: Long,
      name: String,
      rating: Int,
      isPublished: Boolean
  )

  implicit val basicDataWrites = new Writes[BasicData] {
    def writes(basicData: BasicData) = Json.obj(
      "id" -> basicData.id,
      "userId" -> basicData.userId,
      "name" -> basicData.name,
      "rating" -> basicData.rating
    )
  }

  class Repo @Inject()(
      val db: Db,
      val usersRepo: Users.Repo,
      val publishedRobotsRepo: PublishedRobots.Repo
  ) {

    import db.ctx._

    val schema = quote(querySchema[Data]("robots"))

    val usersSchema =
      usersRepo.schema.asInstanceOf[Quoted[EntityQuery[Users.Data]]]

    def find(
        user: Users.Data,
        name: String
    ): Option[BasicData] =
      run(
        schema.filter(
          r => r.userId == lift(user.id) && r.name == lift(name)
        )
      ).headOption.map(dataToBasic)

    def findById(id: Long): Option[BasicData] =
      run(schema.filter(_.id == lift(id))).headOption.map(dataToBasic)

    def getDevCode(id: Long): Option[String] =
      run(schema.filter(_.id == lift(id))).headOption.map(_.devCode)

    def getPublishedCode(id: Long): Option[String] =
      publishedRobotsRepo.find(id).map(_.code)

    def findAllForUser(user: Users.Data): List[BasicData] =
      run(schema.filter(_.userId == lift(user.id))).map(dataToBasic)

    def findAll(): List[(BasicData, Users.Data)] = {
      run(schema.join(usersSchema).on(_.userId == _.id))
        .map(tuple => (dataToBasic(tuple._1), tuple._2))
    }

    def create(userId: Long, name: String) = {
      val data = createData(userId, name)
      run(schema.insert(lift(data)).returningGenerated(_.id))
    }

    def update(id: Long, devCode: String) = {
      run(
        schema.filter(_.id == lift(id)).update(_.devCode -> lift(devCode))
      )
    }

    def publish(id: Long) = {
      run(schema.filter(_.id == lift(id))).headOption.map(robot => {
        publishedRobotsRepo.create(id, robot.devCode)
        run(schema.filter(_.id == lift(id)).update(_.isPublished -> true))
      })
    }
  }
}
