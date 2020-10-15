package ca.shiftfocus.krispii.core.repositories

import java.awt.Color
import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.course.Exam
import ca.shiftfocus.krispii.core.models.work.Test
import ca.shiftfocus.krispii.core.models.{Team, User}
import com.github.mauricio.async.db.{Connection, RowData}
import org.joda.time.DateTime
import play.api.Logger
import scalaz.{-\/, \/, \/-}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TeamRepositoryPostgres(
  val userRepository: UserRepository,
  val examRepository: ExamRepository,
  val testRepository: TestRepository,
  val cacheRepository: CacheRepository
)
    extends TeamRepository with PostgresRepository[Team] {

  override val entityName = "Team"

  // names and number of fields as in core and API and in JSON communication
  def constructor(row: RowData): Team =
    Team(
      row("id").asInstanceOf[UUID],
      row("version").asInstanceOf[Long],
      row("exam_id").asInstanceOf[UUID],
      Option(row("color").asInstanceOf[Int]) match {
        case Some(rgb) => Some(new Color(rgb))
        case _ => None
      },
      row("enabled").asInstanceOf[Boolean],
      row("chat_enabled").asInstanceOf[Boolean],
      IndexedSeq.empty[User],
      IndexedSeq.empty[Test],
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
       |SELECT $FieldsWithTable
       |FROM $Table, exams
       |WHERE $Table.exam_id = exams.id
       |AND exams.coordinator_id = ?
       |ORDER BY $OrderBy
     """.stripMargin

  val ListByScorerId =
    s"""
       |SELECT $FieldsWithTable
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
   * Should only be called from API if the user has administrator rights.
   *
   * @return a vector of the returned Teams (without their associated Tests) or an error
   */
  override def list(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Team]]] =
    queryList(SelectAll)

  /**
   * Enrich a given team with scorers, and optionally with tests
   *
   * @param team: bare team
   * @param fetchTests: whether to also add tests
   */
  def enrichTeam(team: Team, fetchTests: Boolean)(implicit conn: Connection): Future[RepositoryError.Fail \/ Team] =
    if (fetchTests)
      for {
        scorerList <- lift(userRepository.list(team))
        testList <- lift(testRepository.list(team))
        result = team.copy(tests = testList, scorers = scorerList)
        _ = Logger.debug(s"enrichTeam: after adding scorers and tests, team is $result")
      } yield result
    else
      for {
        scorerList <- lift(userRepository.list(team))
        result = team.copy(scorers = scorerList)
        _ = Logger.debug(s"enrichTeams: after adding scorers, team is $result")
      } yield result

  /**
   * Enrich a given team list with scorers, and optionally with tests
   *
   * @param teamList: vector of bare teams
   * @param fetchTests: whether to also add tests
   */
  def enrichTeams(teamList: IndexedSeq[Team], fetchTests: Boolean)(implicit conn: Connection): Future[RepositoryError.Fail \/ IndexedSeq[Team]] =
    if (fetchTests)
      liftSeq(teamList.map { team =>
        (for {
          scorerList <- lift(userRepository.list(team))
          testList <- lift(testRepository.list(team))
          result = team.copy(tests = testList, scorers = scorerList)
          _ = Logger.debug(s"enrichTeams: after adding scorers and tests, team is $result")
        } yield result).run
      })
    else
      liftSeq(teamList.map { team =>
        (for {
          scorerList <- lift(userRepository.list(team))
          result = team.copy(scorers = scorerList)
          _ = Logger.debug(s"enrichTeams: after adding scorers, team is $result")
        } yield result).run
      })

  /**
   * Find all Teams assigned to a given Exam.
   *
   * @param exam The Exam where the Teams were created.
   * @return a vector of the returned Teams, each including any Scorers and Tests, or an error
   */
  override def list(exam: Exam)(implicit conn: Connection): Future[RepositoryError.Fail \/ IndexedSeq[Team]] =
    list(exam, fetchTests = true)

  /**
   * Find all Teams assigned to a given exam.
   *
   * @param exam The Exam where the Teams were created.
   * @param fetchTests Whether to return a Team together with all the Tests assigned to it
   * @return a vector of the returned Teams, each including any Scorers (and optionally Tests), or an error
   */
  override def list(exam: Exam, fetchTests: Boolean)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Team]]] = {
    val key = cacheTeamsKey(exam.id)
    cacheRepository.cacheSeqTeam.getCached(key).flatMap {
      case \/-(teamList) => enrichTeams(teamList, fetchTests)
      case -\/(noResults: RepositoryError.NoResults) =>
        for {
          teamList <- lift(queryList(ListByExam, Seq[Any](exam.id)))
          _ <- lift(cacheRepository.cacheSeqTeam.putCache(key)(teamList, ttl))
          _ = Logger.debug(s"In exam ${exam.name}, bare teams $teamList")
          enrichedTeamList <- lift(enrichTeams(teamList, fetchTests))
          _ = Logger.debug(s"In exam ${exam.name}, enriched teams $enrichedTeamList")
        } yield enrichedTeamList

      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Find all Teams relevant for a given scorer
   *
   * @param user A scorer assigned to the team
   * @return a vector of the returned Teams (not including their tests), or an error
   */
  override def list(user: User)(implicit conn: Connection): Future[RepositoryError.Fail \/ IndexedSeq[Team]] =
    list(user: User, isScorer = true, fetchTests = false)

  /**
   * Find all Teams relevant for a given user.
   *
   * @param user     A user associated with the team
   * @param isScorer Whether the user is a scorer (or else a coordinator)
   * TODO: either remove the fetchTests option, or skip the cache reading step when fetchTests is true
   * @return a vector of the returned Teams (not including their tests), or an error
   */
  override def list(user: User, isScorer: Boolean, fetchTests: Boolean)(implicit conn: Connection): Future[RepositoryError.Fail \/ IndexedSeq[Team]] = {
    val (key, queryType) = if (isScorer)
      (cacheScorerTeamsKey(user.id), ListByScorerId)
    else
      (cacheCoordinatorTeamsKey(user.id), ListByCoordinatorId)
    Logger.debug(s"List teams for ${user.email}: redis key $key")
    cacheRepository.cacheSeqTeam.getCached(key).flatMap {
      case \/-(teamList) => enrichTeams(teamList, fetchTests)
      case -\/(noResults: RepositoryError.NoResults) =>
        Logger.debug(s"No teams for ${user.email} (ID ${user.id}) in redis cache, search in PostgreSQL with query $queryType")
        for {
          teamList <- lift(queryList(queryType, Seq[Any](user.id)))
          _ <- lift(cacheRepository.cacheSeqTeam.putCache(key)(teamList, ttl))
          enrichedTeamList <- lift(enrichTeams(teamList, fetchTests))
        } yield enrichedTeamList
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Find a single team including scorers and tests.
   *
   * @param id the 128-bit UUID, as a byte array, to search for.
   * @return a team (including any scorers and tests) if one was found, or an error
   */
  override def find(id: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Team]] =
    find(id: UUID, fetchTests = true)

  /**
   * Find a single entry by ID.
   *
   * @param id       the 128-bit UUID, as a byte array, to search for.
   * @param fetchTests whether to include the tests associated with the team
   * TODO: either remove the fetchTests option, or skip the cache reading step when fetchTests is true
   * @return a team (including any scorers and optionally tests) if one was found, or an error
   */
  override def find(id: UUID, fetchTests: Boolean)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Team]] = {
    val key = cacheTeamKey(id)
    cacheRepository.cacheTeam.getCached(key).flatMap {
      case \/-(team) => enrichTeam(team, fetchTests)
      case -\/(noResults: RepositoryError.NoResults) =>
        for {
          team <- lift(queryOne(SelectOne, Array[Any](id)))
          _ <- lift(cacheRepository.cacheTeam.putCache(key)(team, ttl))
          result <- lift(enrichTeam(team, fetchTests))
        } yield (result)
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Save a Team to the database.
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
      exam <- lift(examRepository.find(team.examId))
      _ <- lift(cacheRepository.cacheSeqTeam.removeCached(cacheCoordinatorTeamsKey(exam.ownerId)))
      /* A newly created team has no scorers yet!
      scorerList <- lift(userRepository.list(inserted))
      _ <- lift(serializedT(scorerList)(scorer =>
        cacheRepository.cacheSeqTeam.removeCached(cacheScorerTeamsKey(scorer.id)))) */
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
      exam <- lift(examRepository.find(updatedTeam.examId))
      _ <- lift(cacheRepository.cacheSeqTeam.removeCached(cacheCoordinatorTeamsKey(exam.ownerId)))
      scorerList <- lift(userRepository.list(updatedTeam))
      _ <- lift(serializedT(scorerList)(scorer =>
        cacheRepository.cacheSeqTeam.removeCached(cacheScorerTeamsKey(scorer.id))))
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
      exam <- lift(examRepository.find(deletedTeam.examId))
      _ <- lift(cacheRepository.cacheSeqTeam.removeCached(cacheCoordinatorTeamsKey(exam.ownerId)))
      scorerList <- lift(userRepository.list(deletedTeam))
      _ <- lift(serializedT(scorerList)(scorer =>
        cacheRepository.cacheSeqTeam.removeCached(cacheScorerTeamsKey(scorer.id))))
    } yield deletedTeam.copy(tests = oldTests)).run
  }

  /**
   * Add a scorer to the team.
   * TODO: move to omsService
   * @param team
   * @param scorer
   * @param conn
   * @return
   */
  override def addScorer(team: Team, scorer: User)(implicit conn: Connection): Future[RepositoryError.Fail \/ Unit] = {
    val params = Seq[Any](team.id, scorer.id, new DateTime)

    for {
      _ <- lift(queryNumRows(AddScorer, params)(_ == 1).map {
        case \/-(true) =>
          cacheRepository.cacheSeqTeam.removeCached(cacheScorerTeamsKey(scorer.id))
          cacheRepository.cacheSeqUser.removeCached(cacheTeamScorersKey(team.id))
          \/-(Unit)
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
      _ <- lift(queryNumRows(RemoveScorer, Seq(team.id, scorer.id))(_ == 1).map {
        case \/-(true) =>
          cacheRepository.cacheSeqTeam.removeCached(cacheScorerTeamsKey(scorer.id))
          cacheRepository.cacheSeqUser.removeCached(cacheTeamScorersKey(team.id))
          \/-(Unit)
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
        case \/-(true) =>
          cacheRepository.cacheSeqUser.removeCached(cacheTeamScorersKey(team.id))
          serializedT(scorerList)(scorer => cacheRepository.cacheSeqTeam.removeCached(cacheScorerTeamsKey(scorer.id)))
          \/-(())
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
        case \/-(true) =>
          cacheRepository.cacheSeqUser.removeCached(cacheTeamScorersKey(team.id))
          serializedT(scorerList)(scorer => cacheRepository.cacheSeqTeam.removeCached(cacheScorerTeamsKey(scorer.id)))
          \/-(())
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
