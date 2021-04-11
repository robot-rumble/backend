package models

import controllers.Auth
import models.Schema._
import org.joda.time.Duration

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class Boards @Inject()(schema: Schema, robotsRepo: Robots, battlesRepo: Battles)(
    implicit ec: ExecutionContext
) {

  import schema._
  import schema.ctx._

  private def attachRobot(board: Board, page: Long, numPerBoard: Int): Future[FullBoard] =
    robotsRepo.findAllLatestByBoardPaged(board.id, page, numPerBoard) map { robots =>
      FullBoard(board, robots)
    }

  private def attachRobots(boards: Seq[Board], numPerBoard: Int): Future[Seq[FullBoard]] =
    Future.sequence(boards.map(attachRobot(_, 0, numPerBoard)))

  def findAll(numPerBoard: Int): Future[Seq[FullBoard]] =
    run(boards).flatMap(attachRobots(_, numPerBoard))

  def findAll(seasonId: SeasonId, numPerBoard: Int): Future[Seq[FullBoard]] =
    run(boards.by(seasonId)) flatMap (attachRobots(_, numPerBoard))

  def findAllBare(): Future[Seq[Board]] =
    run(boards)

  def find(id: BoardId, page: Long, numPerPage: Int): Future[Option[FullBoard]] =
    run(boards.by(id))
      .map(_.headOption)
      .flatMap {
        case Some(board) => attachRobot(board, page, numPerPage) map Some.apply
        case None        => Future successful None
      }

  def findWithBattles(
      id: BoardId,
      page: Long,
      numRobotsPerPage: Int,
      numBattlesPerPage: Int
  ): Future[Option[FullBoardWithBattles]] = {
    run(boards.by(id)).map(_.headOption) flatMap {
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

  def findBare(id: BoardId): Future[Option[Board]] =
    run(boards.by(id)).map(_.headOption)

  def findBareWithBattles(
      id: BoardId,
      page: Long,
      numPerPage: Int
  ): Future[Option[BoardWithBattles]] = {
    findBare(id).flatMap {
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
      numPerBoard: Int
  ): Future[Option[BoardWithBattles]] = {
    findBare(id).flatMap {
      case Some(board) =>
        battlesRepo.findByBoardForRobotPaged(id, robot.id, page, numPerBoard) map { robots =>
          Some(BoardWithBattles(board, robots map {
            case (battle, opponent) => FullBattle(battle, robot, opponent)
          }))
        }
      case None => Future successful None
    }
  }

  def findBareWithBattlesForRobot(
      id: BoardId,
      robotId: RobotId,
      page: Long,
      numPerBoard: Int
  ): Future[Option[(Robot, BoardWithBattles)]] = {
    robotsRepo
      .findBare(robotId)(Auth.LoggedOut())
      .flatMap {
        case Some(robot) =>
          findBareWithBattlesForRobot_(id, robot, page, numPerBoard).map(
            _.map(boardWithBattles => (robot, boardWithBattles))
          )
        case None => Future successful None
      }
  }

  def findAllBareWithBattlesForRobot(
      robotId: RobotId,
      page: Long,
      numPerBoard: Int
  ): Future[Seq[(PRobot, BoardWithBattles)]] = {
    robotsRepo
      .findAllBoardIds(robotId)
      .flatMap {
        case Some((robot, boardIds)) =>
          Future
            .sequence(boardIds.map { boardId =>
              findBareWithBattlesForRobot_(boardId, robot, page, numPerBoard) flatMap {
                case Some(board) =>
                  robotsRepo.findLatestPr(robotId, boardId) map {
                    case Some(pr) => Some(pr, board)
                    case None     => None
                  }
                case None => Future successful None
              }
            })
            .map(_.flatten)
        case None => Future successful Seq()
      }
  }

  def create(name: String, seasonId: Option[SeasonId]): Future[Board] = {
    val board = Board(
      name = name,
      bio = None,
      seasonId = seasonId,
      adminId = None,
      password = None,
      publishingEnabled = true,
      matchmakingEnabled = true,
      publishCooldown = Duration.standardHours(6),
      publishBattleNum = 1,
      battleCooldown = Duration.standardHours(12),
      recurrentBattleNum = 1
    )
    run(boards.insert(lift(board)).returningGenerated(_.id)).map(board.copy(_))
  }
}
