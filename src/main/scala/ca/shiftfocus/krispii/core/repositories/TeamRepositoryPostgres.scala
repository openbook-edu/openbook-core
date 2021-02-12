package ca.shiftfocus.krispii.core.repositories

import java.awt.Color
import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.group.{Exam, Team}
import ca.shiftfocus.krispii.core.models.user.{Scorer, User}
import ca.shiftfocus.krispii.core.models.work.Test
import com.github.mauricio.async.db.{Connection, RowData}
import org.joda.time.DateTime
import play.api.Logger
import scalaz.{-\/, \/, \/-}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TeamRepositoryPostgres(
  val scorerRepository: ScorerRepository,
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
      row("coordinator_id").asInstanceOf[UUID], // coordinator_id in SQL
      row("name").asInstanceOf[String],
      row("slug").asInstanceOf[String],
      new Color(row("color").asInstanceOf[Int]),
      row("enabled").asInstanceOf[Boolean],
      row("scheduling_enabled").asInstanceOf[Boolean],
      row("chat_enabled").asInstanceOf[Boolean],
      row("archived").asInstanceOf[Boolean],
      row("deleted").asInstanceOf[Boolean],
      IndexedSeq.empty[Scorer],
      IndexedSeq.empty[Test],
      row("created_at").asInstanceOf[DateTime],
      row("updated_at").asInstanceOf[DateTime]
    )

  val Table: String = "teams"
  // names and number of fields in SQL
  val Fields: String = "id, version, exam_id, coordinator_id, name, slug, color, " +
    "enabled, scheduling_enabled, chat_enabled, archived, deleted, created_at, updated_at"
  def FieldsWithTable(fields: String = Fields, table: String = Table): String =
    fields.split(", ").map({ field => s"$table." + field }).mkString(", ")
  val TeamsFields: String = FieldsWithTable(Fields, Table)
  val OrderBy: String = s"$Table.name ASC"

  // User CRUD operations
  val SelectAll: String =
    s"""
       |SELECT $Fields
       |FROM $Table
     """.stripMargin

  val SelectOne: String =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE id = ?
    """.stripMargin

  val SelectOneBySlug: String =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE slug = ?
     """.stripMargin

  val ListByExam: String =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE exam_id = ?
       |ORDER BY $OrderBy
     """.stripMargin

  val ListByCoordinatorId: String =
    s"""
       |SELECT $TeamsFields
       |FROM $Table
       |WHERE coordinator_id = ?
       |  AND NOT $Table.deleted
       |ORDER BY $OrderBy
     """.stripMargin

  val ListByScorerId: String =
    s"""
       |SELECT $TeamsFields
       |FROM $Table, teams_scorers
       |WHERE $Table.id = teams_scorers.team_id
       |  AND teams_scorers.scorer_id = ?
       |  AND NOT $Table.deleted
       |  AND NOT $Table.archived
       |  AND NOT teams_scorers.deleted
       |  AND NOT teams_scorers.archived
       |ORDER BY $OrderBy
     """.stripMargin

  val Insert: String =
    s"""
       |INSERT INTO $Table ($Fields)
       |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
       |RETURNING $Fields
    """.stripMargin

  val Update: String =
    s"""
       |UPDATE $Table
       |SET version = ?, exam_id = ?, coordinator_id = ?, name = ?, slug = ?, color= ?, enabled = ?, scheduling_enabled = ?, chat_enabled = ?, archived = ?, deleted = ?, updated_at = ?
       |WHERE id = ?
       |  AND version = ?
       |RETURNING $Fields
    """.stripMargin

  val Delete: String =
    s"""
       |UPDATE $Table
       |SET deleted = true
       |WHERE id = ?
       |RETURNING $Fields
     """.stripMargin

  val CompletelyDelete: String =
    s"""
       |DELETE
       |FROM $Table
       |WHERE id = ?
       | AND version = ?
       |RETURNING $Fields
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
        scorerList <- lift(scorerRepository.list(team))
        testList <- lift(testRepository.list(team))
        result = team.copy(
          tests = testList.sortBy(_.name),
          scorers = scorerList.sortBy(s => (s.givenname, s.surname))
        )
        // _ = Logger.debug(s"enrichTeam: after adding scorers and tests, team is $result")
      } yield result
    else
      for {
        scorerList <- lift(scorerRepository.list(team))
        result = team.copy(scorers = scorerList.sortBy(s => (s.givenname, s.surname)))
        // _ = Logger.debug(s"enrichTeams: after adding scorers, team is $result")
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
          scorerList <- lift(scorerRepository.list(team))
          testList <- lift(testRepository.list(team))
          result = team.copy(tests = testList, scorers = scorerList.sortBy(s => (s.givenname, s.surname)))
          // _ = Logger.debug(s"enrichTeams: after adding scorers and tests, team is $result")
        } yield result).run
      })
    else
      liftSeq(teamList.map { team =>
        (for {
          scorerList <- lift(scorerRepository.list(team))
          result = team.copy(scorers = scorerList.sortBy(s => (s.givenname, s.surname)))
          // _ = Logger.debug(s"enrichTeams: after adding scorers, team is $result")
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
      case -\/(_: RepositoryError.NoResults) =>
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
    cacheRepository.cacheSeqTeam.getCached(key).flatMap {
      case \/-(teamList) => enrichTeams(teamList, fetchTests)
      case -\/(_: RepositoryError.NoResults) =>
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
      case -\/(_: RepositoryError.NoResults) =>
        for {
          team <- lift(queryOne(SelectOne, Seq[Any](id)))
          _ <- lift(cacheRepository.cacheTeam.putCache(key)(team, ttl))
          result <- lift(enrichTeam(team, fetchTests))
        } yield result
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
      team.id, 1, team.examId, team.ownerId, team.name, team.slug, team.color.getRGB, team.enabled, team.schedulingEnabled, team.chatEnabled, false, false, new DateTime, new DateTime
    )

    for {
      inserted <- lift(queryOne(Insert, params))
      _ <- lift(cacheRepository.cacheSeqTeam.removeCached(cacheTeamsKey(team.examId)))
      _ <- lift(cacheRepository.cacheSeqTeam.removeCached(cacheCoordinatorTeamsKey(team.ownerId)))
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
      team.version + 1, team.examId, team.ownerId, team.name, team.slug, team.color.getRGB, team.enabled, team.schedulingEnabled,
      team.chatEnabled, team.archived, team.deleted, new DateTime, team.id, team.version
    )

    (for {
      updatedTeam <- lift(queryOne(Update, params))
      oldTests = team.tests
      _ <- lift(cacheRepository.cacheTeam.removeCached(cacheTeamKey(team.id)))
      _ <- lift(cacheRepository.cacheSeqTeam.removeCached(cacheTeamsKey(team.examId)))
      _ <- lift(cacheRepository.cacheSeqTeam.removeCached(cacheCoordinatorTeamsKey(team.ownerId)))
      scorerList <- lift(scorerRepository.list(updatedTeam))
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
      deletedTeam <- lift(queryOne(Delete, Seq[Any](team.id)))
      oldTests = team.tests
      _ <- lift(cacheRepository.cacheTeam.removeCached(cacheTeamKey(team.id)))
      _ <- lift(cacheRepository.cacheSeqTeam.removeCached(cacheTeamsKey(team.examId)))
      _ <- lift(cacheRepository.cacheSeqTeam.removeCached(cacheCoordinatorTeamsKey(team.ownerId)))
      scorerList <- lift(scorerRepository.list(deletedTeam))
      _ <- lift(serializedT(scorerList)(scorer =>
        cacheRepository.cacheSeqTeam.removeCached(cacheScorerTeamsKey(scorer.id))))
    } yield deletedTeam.copy(tests = oldTests)).run
  }

}
