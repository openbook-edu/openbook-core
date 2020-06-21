package ca.shiftfocus.krispii.core.repositories

import java.awt.Color
import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.course.Exam
import ca.shiftfocus.krispii.core.models.{Team, User}
import com.github.mauricio.async.db.{Connection, RowData}
import org.joda.time.DateTime
import play.api.Logger
import scalaz.{-\/, \/, \/-}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TeamRepositoryPostgres(
  val userRepository: UserRepository,
  val testRepository: TestRepository,
  val cacheRepository: CacheRepository
)
    extends TeamRepository with PostgresRepository[Team] {

  override val entityName = "Team"

  // names and number of fields in core and API and in JSON communication
  def constructor(row: RowData): Team =
    Team(
      row("id").asInstanceOf[UUID],
      row("examId").asInstanceOf[UUID],
      row("version").asInstanceOf[Long],
      new Color(Option(row("color").asInstanceOf[Int]).getOrElse(0)),
      row("enabled").asInstanceOf[Boolean],
      row("chatEnabled").asInstanceOf[Boolean],
      None, // scorers
      None, // tests
      row("created_at").asInstanceOf[DateTime],
      row("updated_at").asInstanceOf[DateTime]
    )

  val Table = "teams"
  // names and number of fields in SQL
  val Fields = "id, exam_id, version, color, enabled, chat_enabled, created_at, updated_at"
  val FieldsWithTable = Fields.split(", ").map({ field => s"${Table}." + field }).mkString(", ")
  val OrderBy = s"${Table}.created_at ASC"

  // User CRUD operations
  val SelectAll =
    s"""
       |SELECT $Fields
       |FROM $Table
     """.stripMargin

  val SelectOne =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE id = ?
    """.stripMargin

  val SelectOneBySlug =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE slug = ?
     """.stripMargin

  val ListByExam =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE exam_id = ?
       |ORDER BY $OrderBy
     """.stripMargin

  val ListByCoordinatorId =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE coordinator_id = ?
       |ORDER BY $OrderBy
     """.stripMargin

  val ListByScorerId =
    s"""
       |SELECT $Fields
       |FROM $Table, teams_scorers
       |WHERE $Table.id = teams_scorers.team_id
       |AND teams_scorers.scorer_id = ?
       |ORDER BY $OrderBy
     """.stripMargin

  val Insert =
    s"""
       |INSERT INTO $Table ($Fields)
       |VALUES (?, ?, ?, ?,  ?, ?, ?, ?)
       |RETURNING $Fields
    """.stripMargin

  val Update =
    s"""
       |UPDATE $Table
       |SET exam_id = ?, version = ?, color= ?, enabled = ?, chat_enabled = ?, updated_at = ?
       |WHERE id = ?
       |  AND version = ?
       |RETURNING $Fields
    """.stripMargin

  val Delete =
    s"""
       |DELETE
       |FROM $Table
       |WHERE id = ?
       | AND version = ?
       |RETURNING $Fields
  """.stripMargin

  /* The relation between teams and scorers is many-to-many (the only one in OMS),
     so it is necessary to add connections explicitly. 'created_at' could be set
     in PostgreSQL using TIMESTAMP default now(), but we set it everywhere
     from krispii-core, so will continue to do so here.
   */
  val AddScorer = """
                  |INSERT INTO teams_scorers (team_id, scorer_id, created_at)
                  |VALUES (?, ?, ?)
  """.stripMargin

  val RemoveScorer = """
                     |DELETE FROM teams_scorers
                     |WHERE team_id = ?
                     |  AND scorer_id = ?
  """.stripMargin

  val AddScorers = """
       |INSERT INTO teams_scorers (team_id, scorer_id, created_at)
       |VALUES
  """.stripMargin

  val RemoveScorers = """
      |DELETE FROM teams_scorers
      |WHERE team_id = ?
      | AND ARRAY[scorer_id] <@ ?
  """.stripMargin

  /**
   * Find all teams.
   *
   * @return a vector of the returned Teams (without their associated Tests)
   */
  override def list(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Team]]] =
    queryList(SelectAll)

  /**
   * Find all Teams assigned to a given Exam.
   *
   * @param exam The Exam where the Teams were created.
   * @return a vector of the returned Teams with its scorers
   */
  override def list(exam: Exam)(implicit conn: Connection): Future[RepositoryError.Fail \/ IndexedSeq[Team]] =
    list(exam, fetchTests = true)

  /**
   * Find all Teams assigned to a given exam.
   *
   * @param exam     The exam to which the team was assigned
   * @param fetchTests Whether to return a Team together with all the Tests assigned to it
   * @return a vector of the returned Teams, each including any scorers, and optionally tests
   */
  override def list(exam: Exam, fetchTests: Boolean)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Team]]] = {
    cacheRepository.cacheSeqTeam.getCached(cacheExamKey(exam.id)).flatMap {
      case \/-(teamList) => Future successful \/-(teamList)
      case -\/(noResults: RepositoryError.NoResults) =>
        for {
          teamList <- lift(queryList(ListByExam, Seq[Any](exam.id)))
          _ <- lift(cacheRepository.cacheSeqTeam.putCache(cacheTeamsKey(exam.id))(teamList, ttl))
          enrichedTeamList <- if (fetchTests)
            liftSeq(teamList.map { team =>
              (for {
                scorerList <- lift(userRepository.list(team))
                testList <- lift(testRepository.list(team))
                result = team.copy(tests = Some(testList), scorers = Some(scorerList))
              } yield result).run
            })
          else
            liftSeq(teamList.map { team =>
              (for {
                scorerList <- lift(userRepository.list(team))
                result = team.copy(scorers = Some(scorerList))
              } yield result).run
            })
        } yield enrichedTeamList

      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Find all Teams relevant for a given scorer
   *
   * @param user A scorer assigned to the team
   * @return a vector of the returned Teams (not including their tests)
   */
  override def list(user: User)(implicit conn: Connection): Future[RepositoryError.Fail \/ IndexedSeq[Team]] =
    list(user: User, isScorer = true)

  /**
   * Find all Teams relevant for a given user.
   *
   * @param user     A user associated with the team
   * @param isScorer Whether the user is a scorer (or else a coordinator)
   * @return a vector of the returned Teams (not including their tests)
   */
  override def list(user: User, isScorer: Boolean)(implicit conn: Connection): Future[RepositoryError.Fail \/ IndexedSeq[Team]] =
    if (isScorer)
      queryList(ListByScorerId)
    else
      queryList(ListByCoordinatorId)

  /**
   * Find a single entry by ID without showing scorers and tests.
   *
   * @param id the 128-bit UUID, as a byte array, to search for.
   * @return a team if one was found, or an error
   */
  override def find(id: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Team]] =
    find(id: UUID, fetchTests = true)

  /**
   * Find a single entry by ID.
   *
   * @param id       the 128-bit UUID, as a byte array, to search for.
   * @param fetchTests whether to include the tests associated with the team
   * @return a team if one was found, or an error
   */
  override def find(id: UUID, fetchTests: Boolean)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Team]] = {
    cacheRepository.cacheTeam.getCached(cacheTeamKey(id)).flatMap {
      case \/-(team) => Future successful \/-(team)
      case -\/(noResults: RepositoryError.NoResults) =>
        for {
          team <- lift(queryOne(SelectOne, Array[Any](id)))
          scorerList <- lift(userRepository.list(team))
          result <- lift(
            if (fetchTests)
              (for {
              testList <- lift(testRepository.list(team))
              teamWithTests = team.copy(tests = Some(testList), scorers = Some(scorerList))
            } yield teamWithTests).run
            else
              Future successful \/-(team.copy(scorers = Some(scorerList)))
          )
          _ <- lift(cacheRepository.cacheTeam.putCache(cacheTeamKey(team.id))(team, ttl))
        } yield (result)
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Save a Team row.
   *
   * @param team The new team to save.
   * @param conn An implicit connection object. Can be used in a transactional chain.
   * @return the new team
   */
  override def insert(team: Team)(implicit conn: Connection): Future[RepositoryError.Fail \/ Team] = {
    val params = Seq[Any](
      team.id, team.examId, 1, team.color, team.enabled, team.chatEnabled, new DateTime, new DateTime
    )

    for {
      inserted <- lift(queryOne(Insert, params))
      _ <- lift(cacheRepository.cacheSeqTeam.removeCached(cacheTeamsKey(team.examId)))
    } yield inserted
  }

  /**
   * Update a Team row.
   *
   * @param team The team to update.
   * @param conn An implicit connection object. Can be used in a transactional chain.
   * @return the updated team.
   */
  override def update(team: Team)(implicit conn: Connection): Future[RepositoryError.Fail \/ Team] = {
    val params = Seq[Any](
      team.examId, team.color, team.enabled, team.chatEnabled, team.version + 1, new DateTime, team.id, team.version
    )

    (for {
      updatedTeam <- lift(queryOne(Update, params))
      oldTests = team.tests
      _ <- lift(cacheRepository.cacheTeam.removeCached(cacheTeamKey(team.id)))
      _ <- lift(cacheRepository.cacheSeqTeam.removeCached(cacheTeamsKey(team.examId)))
    } yield updatedTeam.copy(tests = oldTests)).run
  }

  /**
   * Delete a single team.
   *
   * @param team The team to be deleted.
   * @param conn An implicit connection object. Can be used in a transactional chain.
   * @return the deleted team (if the deletion was successful) or an error
   */
  override def delete(team: Team)(implicit conn: Connection): Future[RepositoryError.Fail \/ Team] = {
    (for {
      deletedTeam <- lift(queryOne(Delete, Array(team.id, team.version)))
      oldTests = team.tests
      _ <- lift(cacheRepository.cacheTeam.removeCached(cacheTeamKey(team.id)))
      _ <- lift(cacheRepository.cacheSeqTeam.removeCached(cacheTeamsKey(team.examId)))
    } yield deletedTeam.copy(tests = oldTests)).run
  }

  override def addScorer(team: Team, scorer: User)(implicit conn: Connection): Future[RepositoryError.Fail \/ Unit] = {
    val params = Seq[Any](scorer.id, team.id, new DateTime)

    for {
      _ <- lift(queryNumRows(AddScorer, params)(_ == 1).map {
        case \/-(true) => \/-(())
        case \/-(false) =>
          Logger.error(s"Scorer ${scorer.email} could not be added to team ${team.id}.")
          -\/(RepositoryError.DatabaseError(s"Scorer ${scorer.email} could not be added to the team."))
        case -\/(error) =>
          Logger.error(s"Scorer ${scorer.email} could not be added to team ${team.id}: ${error}")
          -\/(error)
      })
      _ <- lift(cacheRepository.cacheSeqRole.removeCached(cacheRolesKey(scorer.id)))
    } yield ()
  }

  override def removeScorer(team: Team, scorer: User)(implicit conn: Connection): Future[RepositoryError.Fail \/ Unit] = {
    for {
      _ <- lift(queryNumRows(RemoveScorer, Seq(scorer.id, scorer.id))(_ == 1).map {
        case \/-(true) => \/-(())
        case \/-(false) =>
          Logger.error(s"Scorer ${scorer.email} could not be removed from team ${team.id}.")
          \/-(RepositoryError.DatabaseError(s"Scorer ${scorer.email} could not be removed from the team."))
        case -\/(error) =>
          Logger.error(s"Scorer ${scorer.email} could not be removed from team ${team.id}: ${error}")
          -\/(error)
      })
      _ <- lift(cacheRepository.cacheSeqRole.removeCached(cacheRolesKey(scorer.id)))
    } yield ()
  }

  override def addScorers(team: Team, scorerList: IndexedSeq[User])(implicit conn: Connection): Future[RepositoryError.Fail \/ Unit] = {
    val cleanTeamId = team.id.toString filterNot ("-" contains _)
    val query = AddScorers + scorerList.map { scorer =>
      val cleanScorerId = scorer.id.toString filterNot ("-" contains _)
      s"('$cleanTeamId', '$cleanScorerId', '${new DateTime()}')"
    }.mkString(",")

    for {
      _ <- lift(queryNumRows(query)(scorerList.length == _).map {
        case \/-(true) => \/-(())
        case \/-(false) =>
          Logger.error(s"At least one scorer could not be added to team ${team.id}.")
          -\/(RepositoryError.DatabaseError("At least one scorer could not be added to the team."))
        case -\/(error) =>
          Logger.error(s"At least one scorer could not be added to team ${team.id}: ${error}")
          -\/(error)
      })
      _ <- liftSeq(scorerList.map {
        user => cacheRepository.cacheSeqTeam.removeCached(cacheTeamsKey(user.id))
      })
    } yield ()
  }

  override def removeScorers(team: Team, scorerList: IndexedSeq[User])(implicit conn: Connection): Future[RepositoryError.Fail \/ Unit] = {
    val cleanTeamId = team.id.toString filterNot ("-" contains _)
    val cleanUsersId = scorerList.map { user =>
      user.id.toString filterNot ("-" contains _)
    }

    for {
      _ <- lift(queryNumRows(RemoveScorers, Array[Any](cleanTeamId, cleanUsersId))(scorerList.length == _).map {
        case \/-(true) => \/-(())
        case \/-(false) =>
          Logger.error(s"At least one scorer could not be removed from team ${team.id}.")
          -\/(RepositoryError.DatabaseError(s"At least one scorer could not be removed from team ${team}."))
        case -\/(error) =>
          Logger.error(s"At least one scorer could not be removed from team ${team.id}: ${error}")
          -\/(error)
      }.recover {
        case exception: Throwable =>
          Logger.error(s"When removing scorers ${scorerList} from team ${team}: ${exception}")
          -\/(RepositoryError.DatabaseError(s"General error while removing scorers from team ${team}"))
      })
      _ <- liftSeq {
        scorerList.map {
          user => cacheRepository.cacheSeqTeam.removeCached(cacheTeamsKey(user.id))
        }
      }
    } yield ()
  }
}
