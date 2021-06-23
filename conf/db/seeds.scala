import scala.concurrent.ExecutionContext.Implicits.global

import org.joda.time.LocalDateTime
import play.api.inject.guice.GuiceApplicationBuilder

import matchmaking.BattleQueue.MatchOutput
import models.Schema._
import models._

val app = new GuiceApplicationBuilder().build()
val usersRepo = app.injector.instanceOf[Users]
val robotsRepo = app.injector.instanceOf[Robots]
val battlesRepo = app.injector.instanceOf[Battles]
val boardsRepo = app.injector.instanceOf[Boards]
val seasonsRepo = app.injector.instanceOf[Seasons]

def createBattle(
    boardId: BoardId,
    r1Id: RobotId,
    pr1Id: PRobotId,
    r2Id: RobotId,
    pr2Id: PRobotId
) = {
  battlesRepo.create(
    MatchOutput(
      boardId.id,
      r1Id.id,
      pr1Id.id,
      0,
      r2Id.id,
      pr2Id.id,
      0,
      Some(Team.R1),
      false,
      Array()
    ),
    1000,
    100,
    1000,
    100
  )
}
for {
  season1 <- seasonsRepo.create(
    "The first season",
    "the-first-season",
    LocalDateTime.now().minusDays(1),
    LocalDateTime.now().plusDays(30),
    "<b>Season!</b>"
  )
  board1 <- boardsRepo.create(None, "one", None, None, Some(season1.id))
  board2 <- boardsRepo.create(None, "two", None, None, Some(season1.id))
  (user, _) <- usersRepo.create("test@test.com", "test", "password1")
  Some(r1) <- robotsRepo.create(user.id, "r1", Lang.Python, true)
  Some(Right(pr1Id)) <- boardsRepo.publish(r1.id, board1)
  Some(r2) <- robotsRepo.create(user.id, "r2", Lang.Python, true)
  Some(Right(pr2Id)) <- boardsRepo.publish(r2.id, board1)
  Some(r3) <- robotsRepo.create(user.id, "r3", Lang.Python, true)
  Some(Right(pr3Id)) <- boardsRepo.publish(r3.id, board1)
  Some(r4) <- robotsRepo.create(user.id, "r4", Lang.Python, true)
  Some(Right(pr4Id)) <- boardsRepo.publish(r4.id, board1)
  b1 <- createBattle(board1.id, r1.id, pr1Id, r2.id, pr2Id)
  b2 <- createBattle(board1.id, r2.id, pr2Id, r3.id, pr3Id)
  b2 <- createBattle(board1.id, r2.id, pr2Id, r4.id, pr4Id)
  b2 <- createBattle(board1.id, r4.id, pr4Id, r2.id, pr2Id)
} yield ()

