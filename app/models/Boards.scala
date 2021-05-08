package models

import controllers.Auth
import controllers.Auth.{LoggedIn, LoggedOut, Visitor}
import models.Schema._
import play.api.Configuration
import services.JodaUtils._

import javax.inject.Inject
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class Boards @Inject()(
    schema: Schema,
    robotsRepo: Robots,
    battlesRepo: Battles,
    config: Configuration
)(
    implicit ec: ExecutionContext
) {

  import schema._
  import schema.ctx._

  def isMember(
      boardId: BoardId,
      userId: UserId
  ): Future[Boolean] = {
    run(boardMemberships.by(boardId).by(userId)).map(_.nonEmpty)
  }

  def isMember(boardId: BoardId)(visitor: Visitor): Future[Boolean] =
    visitor match {
      case LoggedIn(user) => isMember(boardId, user.id)
      case LoggedOut()    => run(boards.by(boardId).filter(_.password.isEmpty)).map(_.nonEmpty)
    }

  def allMembershipBoards(userId: UserId): Future[Seq[Board]] = {
    run(boardMemberships.by(userId)) map (_.map(m => m.boardId)) flatMap { memberships =>
      findAllBareNoMembershipCheck() map { boards =>
        boards.filter(bo => bo.password.isEmpty || memberships.contains(bo.id))
      }
    }
  }

  def allMembershipBoards(visitor: Visitor): Future[Seq[Board]] =
    visitor match {
      case LoggedIn(user) => allMembershipBoards(user.id)
      case LoggedOut()    => run(boards.filter(_.password.isEmpty))
    }

  def allMemberships(visitor: Visitor): Future[Seq[BoardId]] =
    allMembershipBoards(visitor).map(_.map(b => b.id))

  def addMembership(boardId: BoardId, userId: UserId): Future[BoardMembership] = {
    val boardMembership = BoardMembership(boardId = boardId, userId = userId)
    run(boardMemberships.insert(lift(boardMembership)).returningGenerated(_.id))
      .map(boardMembership.copy(_))
  }

  private def attachRobot(board: Board, page: Long, numPerBoard: Int): Future[FullBoard] =
    robotsRepo.findAllLatestByBoardPaged(board.id, page, numPerBoard) map { robots =>
      FullBoard(board, robots)
    }

  private def attachRobots(boards: Seq[Board], numPerBoard: Int): Future[Seq[FullBoard]] =
    Future.sequence(boards.map(attachRobot(_, 0, numPerBoard)))

  def findAll(numPerBoard: Int)(visitor: Visitor): Future[Seq[FullBoard]] =
    allMembershipBoards(visitor).flatMap(attachRobots(_, numPerBoard))

  def findAll(seasonId: SeasonId, numPerBoard: Int): Future[Seq[FullBoard]] =
    run(boards.by(seasonId)) flatMap (attachRobots(_, numPerBoard))

  def findAllBareNoMembershipCheck(): Future[Seq[Board]] =
    run(boards)

  def findAllBare(visitor: Visitor): Future[Seq[Board]] = {
    allMembershipBoards(visitor)
  }

  def find(id: BoardId, page: Long, numPerPage: Int)(
      visitor: Visitor
  ): Future[Option[FullBoard]] = {
    findBare(id)(visitor) flatMap {
      case Some(board) => attachRobot(board, page, numPerPage) map Some.apply
      case None        => Future successful None
    }
  }

  def findWithBattles(id: BoardId, page: Long, numRobotsPerPage: Int, numBattlesPerPage: Int)(
      visitor: Visitor
  ): Future[Option[FullBoardWithBattles]] = {
    findBare(id)(visitor) flatMap {
      case Some(board) =>
        attachRobot(board, page, numRobotsPerPage) flatMap {
          case FullBoard(board, robots) =>
            battlesRepo.findByBoardPaged(id, page, numBattlesPerPage) map { battles =>
              val boardBattles = battles map {
                case (battle, r1, r2) => FullBattle(battle, r1, r2)
              }
              Some(FullBoardWithBattles(board, robots, boardBattles))
            }
        }
      case None => Future successful None
    }
  }

  def findBareNoMembershipCheck(id: BoardId): Future[Option[Board]] = {
    run(boards.by(id)).map(_.headOption)
  }

  def findBare(id: BoardId)(visitor: Visitor): Future[Option[Board]] = {
    isMember(id)(visitor) flatMap {
      case true  => run(boards.by(id)).map(_.headOption)
      case false => Future successful None
    }
  }

  def findBareByPassword(password: String): Future[Option[Board]] = {
    run(boards.filter(_.password.contains(lift(password)))).map(_.headOption)
  }

  def findAllBareByAdmin(userId: UserId): Future[Seq[Board]] = {
    run(boards.filter(_.adminId.contains(lift(userId))))
  }

  def findBattle(battleId: BattleId)(visitor: Visitor): Future[Option[FullBattle]] = {
    battlesRepo.find(battleId) flatMap {
      case Some(fullBattle) =>
        isMember(fullBattle.b.boardId)(visitor) map {
          case true  => Some(fullBattle)
          case false => None
        }
      case None => Future successful None
    }
  }

  def findBareWithBattles(id: BoardId, page: Long, numPerPage: Int)(
      visitor: Visitor
  ): Future[Option[BoardWithBattles]] = {
    findBare(id)(visitor).flatMap {
      case Some(board) =>
        battlesRepo.findByBoardPaged(id, page, numPerPage) map { robots =>
          Some(BoardWithBattles(board, robots map {
            case (battle, r1, r2) => FullBattle(battle, r1, r2)
          }))
        }
      case None => Future successful None
    }
  }

  private def findBareWithBattlesForRobot_(
      id: BoardId,
      robot: Robot,
      page: Long,
      numPerBoard: Int,
      memberships: Seq[BoardId]
  ): Future[Option[BoardWithBattles]] = {
    // no visitor check here to be more efficient and only call database once with the membership list
    findBareNoMembershipCheck(id) flatMap {
      case Some(board) =>
        if (memberships.contains(board.id))
          battlesRepo.findByBoardForRobotPaged(id, robot.id, page, numPerBoard) map { robots =>
            Some(
              BoardWithBattles(
                board,
                robots map {
                  case (battle, opponent) if battle.r1Id == robot.id =>
                    FullBattle(battle, robot, opponent)
                  case (battle, opponent) => FullBattle(battle, opponent, robot)
                }
              )
            )
          } else Future successful None
      case None => Future successful None
    }
  }

  def findBareWithBattlesForRobot(id: BoardId, robotId: RobotId, page: Long, numPerBoard: Int)(
      visitor: Visitor
  ): Future[Option[(Robot, BoardWithBattles)]] = {
    allMemberships(visitor) flatMap { memberships =>
      robotsRepo
        .findBare(robotId)(Auth.LoggedOut())
        .flatMap {
          case Some(robot) =>
            findBareWithBattlesForRobot_(id, robot, page, numPerBoard, memberships).map(
              _.map(boardWithBattles => (robot, boardWithBattles))
            )
          case None => Future successful None
        }
    }
  }

  def findAllBareWithBattlesForRobot(robotId: RobotId, page: Long, numPerBoard: Int)(
      visitor: Visitor
  ): Future[Seq[(PRobot, BoardWithBattles)]] = {
    allMemberships(visitor) flatMap { memberships =>
      robotsRepo
        .findAllBoardIds(robotId)
        .flatMap {
          case Some((robot, boardIds)) =>
            Future
              .sequence(boardIds.map { boardId =>
                findBareWithBattlesForRobot_(boardId, robot, page, numPerBoard, memberships) flatMap {
                  case Some(board) =>
                    if (memberships.contains(board.board.id))
                      robotsRepo.findLatestPr(robotId, boardId) map {
                        case Some(pr) => Some(pr, board)
                        case None     => None
                      } else Future successful None
                  case None => Future successful None
                }
              })
              .map(_.flatten)
          case None => Future successful Seq()
        }
    }
  }

  def create(
      adminId: Option[UserId],
      name: String,
      bio: Option[String],
      password: Option[String],
      seasonId: Option[SeasonId] = None
  ): Future[Board] = {
    val board = Board(
      adminId = adminId,
      name = name,
      bio = bio,
      seasonId = seasonId,
      password = password,
      publishingEnabled = true,
      matchmakingEnabled = true,
      publishCooldown = config.get[FiniteDuration]("queue.defaultPublishCooldown"),
      publishBattleNum = config.get[Int]("queue.defaultPublishBattleNum"),
      battleCooldown = config.get[FiniteDuration]("queue.defaultBattleCooldown"),
      recurrentBattleNum = config.get[Int]("queue.defaultRecurrentBattleNum")
    )
    run(boards.insert(lift(board)).returningGenerated(_.id)).map(board.copy(_)) flatMap { board =>
      adminId match {
        case Some(adminId) =>
          addMembership(board.id, adminId) map (_ => board)
        case None =>
          Future successful board
      }
    }
  }

  def publish(
      robotId: RobotId,
      board: Board,
  ): Future[Option[PublishResult]] = {
    if (board.publishingEnabled) {
      run(
        robots
          .by(robotId)
          .leftJoin(publishedRobots.by(board.id).latest)
          .on((r, pr) => r.id == pr.rId)
      ).map(_.headOption) flatMap {
        case Some((r, prOption)) =>
          isMember(board.id, r.userId) flatMap {
            case true =>
              if (r.devCode.isEmpty) {
                Future successful Some(Left("Your robot code is empty!"))
              } else {
                prOption match {
                  case Some(pr) if !board.publishCooldownExpired(pr.created) =>
                    Future successful Some(
                      Left(s"Your robot was recently published. You can publish again only after ${board
                        .formatNextPublishTime(pr.created)}")
                    )
                  case pr =>
                    val glickoSettings = pr match {
                      case Some(pr) => GlickoSettings(pr.rating, pr.deviation, pr.volatility)
                      case None =>
                        GlickoSettings(
                          config.get[Int]("queue.initialRating"),
                          config.get[Double]("queue.initialDeviation"),
                          config.get[Double]("queue.initialVolatility")
                        )
                    }
                    for {
                      prId <- run(
                        publishedRobots
                          .insert(lift(PRobot(r.id, board.id, r.devCode, glickoSettings)))
                          .returningGenerated(_.id)
                      )
                      _ <- run(
                        robots
                          .by(robotId)
                          .update(_.published -> true, _.active -> true, _.errorCount -> 0)
                      )
                      // a robot's active status is reset on every publish
                    } yield Some(Right(prId))
                }
              }
            case false =>
              Future successful None
          }
        case _ =>
          Future successful None
      }
    } else
      Future successful None
  }
}
