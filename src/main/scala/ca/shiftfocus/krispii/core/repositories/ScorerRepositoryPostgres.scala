package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.group.Team
import ca.shiftfocus.krispii.core.models.user.{Scorer, User, FutureScorer}
import com.github.mauricio.async.db.{Connection, RowData}
import org.joda.time.DateTime
import play.api.Logger
import scalaz.{-\/, \/, \/-}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ScorerRepositoryPostgres(
    val cacheRepository: CacheRepository
) extends ScorerRepository with PostgresRepository[Scorer] {

  override val entityName = "Scorer"

  def constructor(row: RowData): Scorer =
    Scorer(
      id = row("id").asInstanceOf[UUID],
      version = row("version").asInstanceOf[Long],
      username = row("username").asInstanceOf[String],
      email = row("email").asInstanceOf[String],
      givenname = row("givenname").asInstanceOf[String],
      surname = row("surname").asInstanceOf[String],
      alias = Option(row("alias").asInstanceOf[String]) match {
      case Some(alias) => Some(alias)
      case _ => None
    },
      accountType = row("account_type").asInstanceOf[String],
      leader = row("leader").asInstanceOf[Boolean],
      isDeleted = row("deleted").asInstanceOf[Boolean],
      isArchived = row("archived").asInstanceOf[Boolean],
      createdAt = row("created_at").asInstanceOf[DateTime],
      updatedAt = row("updated_at").asInstanceOf[DateTime],
      includedAt = row("included_at").asInstanceOf[DateTime]
    )

  private def FieldsWithTable(fields: String, table: String): String =
    fields.split(", ")
      .map({ field => s"$table." + field })
      .mkString(", ")

  /* There is no "scorers" table in SQL: scorers are created on the fly from users and teams_scorers.
     Extending PostgresRepository[Scorer] means queryOne etc. expect ALL Scorer fields
     to be returned from each SQL query, even if the returned values are just discarded. */

  private val UserFields = "id, version, created_at, updated_at, username, email, givenname, surname, alias, account_type"
  private val UserFieldsTable = FieldsWithTable(UserFields, "users")
  private val AddFields = "team_id, deleted, archived, created_at"
  private val ScorerFields = "scorer_id, " + AddFields + ", coalesce(leader, FALSE) AS leader"
  private val annex = " AS included_at, coalesce(leader, FALSE) AS leader"
  private val ScorerFieldsTable = FieldsWithTable(AddFields, "teams_scorers") + annex
  private val UpdateFieldsTable = FieldsWithTable(AddFields, "updated") + annex
  private val ReadFields = UserFieldsTable + ", " + ScorerFieldsTable
  private val Fields = UserFieldsTable + ", " + UpdateFieldsTable

  private val SelectFromTeam =
    s"""
       |SELECT $ReadFields
       |FROM teams, users, teams_scorers
       |WHERE teams.id = teams_scorers.team_id
       |  AND teams_scorers.scorer_id = users.id
       |  AND teams.id = ?
       |  AND users.is_deleted = FALSE
       |ORDER BY leader, included_at
    """.stripMargin

  /**
   * The Boolean fields archived and deleted will always be default (false) on creation.
   * The scorer_id and team_id fields will never be changed. Therefore, it doesn't seem necessary to have a version.
   */
  private val AddScorer =
    s"""
       |WITH updated AS (
       |	INSERT INTO teams_scorers (team_id, scorer_id, leader, created_at)
       |	VALUES (?, ?, ?, ?)
       |	RETURNING $ScorerFields)
       |SELECT $UserFieldsTable, $UpdateFieldsTable
       |FROM updated, users
       |WHERE updated.scorer_id = users.id;
  """.stripMargin

  private val ToggleTeamLeader =
    s"""
       |WITH updated AS (
       |	UPDATE teams_scorers
       |	SET leader = ?
       |	WHERE team_id = ?
       |	  AND scorer_id = ?
       |	RETURNING $ScorerFields)
       |SELECT $UserFieldsTable, $UpdateFieldsTable
       |FROM updated, users
       |WHERE updated.scorer_id = users.id;
  """.stripMargin

  private val ToggleArchivedScorer =
    s"""
       |WITH updated AS (
       |	UPDATE teams_scorers
       |	SET archived = ?
       |	WHERE team_id = ?
       |	  AND scorer_id = ?
       |	RETURNING $ScorerFields)
       |SELECT $Fields
       |FROM updated, users
       |WHERE updated.scorer_id = users.id;
  """.stripMargin

  /**
   * "Removing" a scorer from a team means "updating" with deleted=true.
   */
  private val toggleDeletedScorer =
    s"""
       |WITH updated AS (
       |	UPDATE teams_scorers
       |	SET deleted = ?
       |	WHERE team_id = ?
       |	  AND scorer_id = ?
       |	RETURNING $ScorerFields)
       |SELECT $UserFieldsTable, $UpdateFieldsTable
       |FROM updated, users
       |WHERE updated.scorer_id = users.id;
  """.stripMargin

  /**
   * This should, if at all, only be used by top-level administrators.
   */
  private val CompletelyRemoveScorer =
    s"""
       |WITH updated AS (
       |	DELETE FROM team_scorers
       |	WHERE team_id = ?
       |	  AND scorer_id = ?
       |	RETURNING $ScorerFields)
       |SELECT $Fields
       |FROM updated, users
       |WHERE updated.scorer_id = users.id;
   """.stripMargin

  // AddScorersStart is the first fragment of a statement
  private val AddScorersStart =
    """
      |INSERT INTO teams_scorers (team_id, scorer_id, leader, created_at)
      |VALUES
  """.stripMargin

  // AddScorersEnd is the last fragment of a statement
  private val AddScorersEnd =
    s"""
      |RETURNING $ScorerFields)
      |SELECT $Fields
      |FROM updated, users
      |WHERE updated.scorer_id = users.id;
  """.stripMargin

  /*private val RemoveScorersStart =
    """
      |UPDATE teams_scorers (team_id, scorer_id, deleted)
      |VALUES
  """.stripMargin

  /**
   * This should, if at all, only be used by top-level administrators.
   */
  private val CompletelyRemoveScorers =
    s"""
       |DELETE FROM teams_scorers
       |WHERE team_id = ?
       | AND ARRAY[scorer_id] <@ ?
       |RETURNING $ReadFields
  """.stripMargin*/

  /**
   * List scorers on a given team.
   *
   * @return a future disjunction containing either the scorers, or a failure
   */
  override def list(team: Team)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Scorer]]] = {
    val key = cacheTeamScorersKey(team.id)
    cacheRepository.cacheSeqScorer.getCached(key).flatMap {
      case \/-(scorerList) => Future successful \/-(scorerList)
      case -\/(_: RepositoryError.NoResults) =>
        for {
          scorerList <- lift(queryList(SelectFromTeam, Seq[Any](team.id)))
          _ <- lift(cacheRepository.cacheSeqScorer.putCache(key)(scorerList, ttl))
        } yield scorerList
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Add a scorer to the team.
   * @param team: the Team (with all of its scorers) that the new user is to be added to
   * @param scorer: a User to be transformed into a Scorer
   * @param leader: is the new scorer a team leader? default false
   * @param conn: implicit database connection
   * @return Unit, since it makes more sense to let omsService return the entire Team (teamRepository must not be referenced here to avoid circularity!)
   */
  override def add(team: Team, scorer: User, leader: Boolean = false)(implicit conn: Connection): Future[RepositoryError.Fail \/ Unit] = {
    // the SQL UNIQUE constraint for leader requires non-leaders to have leader=NULL
    val nulledLeader = if (leader) Some(true) else None
    val params = Seq[Any](team.id, scorer.id, nulledLeader, new DateTime)
    for {
      _ <- lift(queryOne(AddScorer, params).map {
        case \/-(scorer) =>
          // cacheRepository.cacheSeqExam.removeCached(cacheScorerExamsKey(scorer.id))
          cacheRepository.cacheSeqTeam.removeCached(cacheScorerTeamsKey(scorer.id))
          cacheRepository.cacheSeqUser.removeCached(cacheTeamScorersKey(team.id))
          \/-(scorer)
        case -\/(error) =>
          Logger.error(s"Scorer ${scorer.email} could not be added to team ${team.id}: $error")
          -\/(error)
      })
    } yield ()
  }

  override def update(team: Team, scorer: Scorer, leader: Option[Boolean], archived: Option[Boolean],
    deleted: Option[Boolean])(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]] =
    for {
      // _ <- lift(cacheRepository.cacheSeqExam.removeCached(cacheScorerExamsKey(scorer.id)))
      _ <- lift(cacheRepository.cacheSeqTeam.removeCached(cacheScorerTeamsKey(scorer.id)))
      _ <- lift(cacheRepository.cacheSeqUser.removeCached(cacheTeamScorersKey(team.id)))
      _ <- lift(leader match {
        case Some(true) =>
          queryOne(ToggleTeamLeader, Seq[Any](Some(true), team.id, scorer.id), debug = true)
        case Some(false) =>
          queryOne(ToggleTeamLeader, Seq[Any](None, team.id, scorer.id), debug = true)
        case None => Future successful \/-(true)
      })
      _ <- lift(archived match {
        case Some(archived) => queryOne(ToggleArchivedScorer, Seq[Any](archived, team.id, scorer.id), debug = true)
        case None => Future successful \/-(true)
      })
      _ <- lift(deleted match {
        case Some(deleted) => queryOne(toggleDeletedScorer, Seq[Any](deleted, team.id, scorer.id), debug = true)
        case None => Future successful -\/(RepositoryError.BadParam(s"Nothing to update for scorer ${scorer.email} in team ${team.id}"))
      })
    } yield ()

  override def remove(team: Team, scorerId: UUID)(implicit conn: Connection): Future[RepositoryError.Fail \/ Unit] =
    for {
      // _ <- lift(cacheRepository.cacheSeqExam.removeCached(cacheScorerExamsKey(scorerId)))
      _ <- lift(cacheRepository.cacheSeqTeam.removeCached(cacheScorerTeamsKey(scorerId)))
      _ <- lift(cacheRepository.cacheSeqUser.removeCached(cacheTeamScorersKey(team.id)))
      _ <- lift(queryOne(toggleDeletedScorer, Seq[Any](true, team.id, scorerId)))
    } yield ()

  /**
   * Should be used, if at all, only by top-level administrators.
   * @param team: the Team from which to delete the scorer
   * @param scorerId: unique ID of the scorer to be deleted
   * @param conn: implicit database connection
   * @return
   */
  def completelyRemove(team: Team, scorerId: UUID)(implicit conn: Connection): Future[RepositoryError.Fail \/ Unit] =
    queryNumRows(CompletelyRemoveScorer, Seq[Any](team.id, scorerId))(_ == 1).map {
      case \/-(true) =>
        // cacheRepository.cacheSeqExam.removeCached(cacheScorerExamsKey(scorerId))
        cacheRepository.cacheSeqTeam.removeCached(cacheScorerTeamsKey(scorerId))
        cacheRepository.cacheSeqUser.removeCached(cacheTeamScorersKey(team.id))
        \/-(())
      case \/-(false) =>
        Logger.error(s"Scorer $scorerId could not be removed from team ${team.id}.")
        -\/(RepositoryError.DatabaseError(s"Scorer $scorerId could not be removed from the team."))
      case -\/(error) =>
        Logger.error(s"Scorer $scorerId could not be removed from team ${team.id}: $error")
        -\/(error)
    }

  override def addList(team: Team, userList: IndexedSeq[FutureScorer])(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]] = {
    val cleanTeamId = team.id.toString filterNot ("-" contains _)
    val perRow = userList map (fs => {
      val cleanId = fs.userId.toString filterNot ("-" contains _)
      s"('$cleanTeamId', '$cleanId', '${if (fs.leader) Some(true) else None}', '${new DateTime()}')"
    })
    val query = AddScorersStart + perRow.mkString(",") + AddScorersEnd

    for {
      _ <- lift(queryNumRows(query, debug = true)(userList.length == _).map {
        case \/-(true) =>
          cacheRepository.cacheSeqUser.removeCached(cacheTeamScorersKey(team.id))
          serializedT(userList)(scorer => cacheRepository.cacheSeqTeam.removeCached(cacheScorerTeamsKey(scorer.userId)))
          \/-(())
        case \/-(false) =>
          Logger.error(s"At least one scorer could not be added to team ${team.id}.")
          -\/(RepositoryError.DatabaseError("At least one scorer could not be added to the team."))
        case -\/(error) =>
          Logger.error(s"At least one scorer could not be added to team ${team.id}: $error")
          -\/(error)
      })
      _ <- liftSeq(userList.map {
        user => cacheRepository.cacheSeqTeam.removeCached(cacheTeamsKey(user.userId))
      })
    } yield ()
  }

  /*def removeList(team: Team, scorerIdList: IndexedSeq[UUID])(implicit conn: Connection): Future[RepositoryError.Fail \/ Unit] = {
    val cleanTeamId = team.id.toString filterNot ("-" contains _)
    val perRow = scorerIdList.map { id =>
      val cleanUserId = id.toString filterNot ("-" contains _)
      s"('$cleanTeamId', '$cleanUserId', true)"
    }
    val query = RemoveScorersStart + perRow.mkString(",") + RemoveScorersEnd

    for {
      _ <- lift(queryNumRows(query)(scorerIdList.length == _).map {
        case \/-(true) =>
          cacheRepository.cacheSeqUser.removeCached(cacheTeamScorersKey(team.id))
          serializedT(scorerIdList)(scorerId => cacheRepository.cacheSeqTeam.removeCached(cacheScorerTeamsKey(scorerId)))
          \/-(())
        case \/-(false) =>
          Logger.error(s"At least one scorer could not be removed from team ${team.id}.")
          -\/(RepositoryError.DatabaseError(s"At least one scorer could not be removed from team ${team}."))
        case -\/(error) =>
          Logger.error(s"At least one scorer could not be removed from team ${team.id}: ${error}")
          -\/(error)
      }.recover {
        case exception: Throwable =>
          Logger.error(s"When removing scorers ${scorerIdList} from team ${team}: ${exception}")
          -\/(RepositoryError.DatabaseError(s"General error while removing scorers from team ${team}"))
      })
      _ <- liftSeq {
        scorerIdList.map {
          scorerId => cacheRepository.cacheSeqTeam.removeCached(cacheTeamsKey(scorerId))
        }
      }
    } yield ()
  } */

  /*def completelyRemoveList(team: Team, scorerList: IndexedSeq[UUID])(implicit conn: Connection): Future[RepositoryError.Fail \/ Unit] = {
    val cleanTeamId = team.id.toString filterNot ("-" contains _)
    val cleanUserIds = scorerList.map { id =>
      id.toString filterNot ("-" contains _)
    }

    for {
      _ <- lift(queryNumRows(CompletelyRemoveScorers, Seq[Any](cleanTeamId, cleanUserIds))(scorerList.length == _).map {
        case \/-(true) =>
          cacheRepository.cacheSeqUser.removeCached(cacheTeamScorersKey(team.id))
          serializedT(scorerList)(scorerId => cacheRepository.cacheSeqTeam.removeCached(cacheScorerTeamsKey(scorerId)))
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
          scorerId => cacheRepository.cacheSeqTeam.removeCached(cacheTeamsKey(scorerId))
        }
      }
    } yield ()
  }*/

}
