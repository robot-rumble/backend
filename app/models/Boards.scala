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

  def findBare(id: BoardId): Future[Option[Board]] =
    run(boards.by(id)).map(_.headOption)

  private def findWithBattlesForRobot_(
      boardId: BoardId,
      robot: Robot,
      page: Long,
      numPerBoard: Int
  ): Future[Option[BoardWithBattles]] = {
    findBare(boardId).flatMap {
      case Some(board) =>
        battlesRepo.findBoardForRobotPaged(boardId, robot.id, page, numPerBoard) map { robots =>
          Some(BoardWithBattles(board, robots map {
            case (battle, opponent) => FullBattle(battle, robot, opponent)
          }))
        }
      case None => Future successful None
    }
  }

  def findWithBattlesForRobot(
      boardId: BoardId,
      robotId: RobotId,
      page: Long,
      numPerBoard: Index
  ): Future[Option[(Robot, BoardWithBattles)]] = {
    robotsRepo
      .findBare(robotId)(Auth.LoggedOut())
      .flatMap {
        case Some(robot) =>
          findWithBattlesForRobot_(boardId, robot, page, numPerBoard).map(
            _.map(boardWithBattles => (robot, boardWithBattles))
          )
        case None => Future successful None
      }
  }

  def findAllWithBattlesForRobot(
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
              findWithBattlesForRobot_(boardId, robot, page, numPerBoard) flatMap {
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
