import play.api.inject.guice.GuiceApplicationBuilder

import scala.concurrent.ExecutionContext.Implicits.global
import models._
import Schema._
import services.BattleQueue.MatchOutput

val app = new GuiceApplicationBuilder().build()
val usersRepo = app.injector.instanceOf[Users]
val robotsRepo = app.injector.instanceOf[Robots]
val battlesRepo = app.injector.instanceOf[Battles]

def createBattle(r1Id: Long, pr1Id: Long, r2Id: Long, pr2Id: Long) = {
  battlesRepo.create(MatchOutput(r1Id, pr1Id, 0, r2Id, pr2Id, 0, Winner.R1, false, ""), 100, 100)
}

for {
  user <- usersRepo.create("test", "test")
  r1 <- robotsRepo.create(user.id, "r1", Lang.Python)
  pr1Id <- robotsRepo.publish(r1.id)
  r2 <- robotsRepo.create(user.id, "r2", Lang.Python)
  pr2Id <- robotsRepo.publish(r2.id)
  r3 <- robotsRepo.create(user.id, "r3", Lang.Python)
  pr3Id <- robotsRepo.publish(r3.id)
  r4 <- robotsRepo.create(user.id, "r4", Lang.Python)
  pr4Id <- robotsRepo.publish(r4.id)
  _ <- createBattle(r1.id, pr1Id, r2.id, pr2Id)
  _ <- createBattle(r3.id, pr3Id, r2.id, pr2Id)
  _ <- createBattle(r2.id, pr2Id, r4.id, pr4Id)
  _ <- createBattle(r4.id, pr4Id, r1.id, pr1Id)
  _ <- createBattle(r2.id, pr2Id, r3.id, pr3Id)
} yield ()
