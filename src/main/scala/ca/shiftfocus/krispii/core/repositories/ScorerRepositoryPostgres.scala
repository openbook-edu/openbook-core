package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.group.Team
import ca.shiftfocus.krispii.core.models.user.Scorer
import com.github.mauricio.async.db.{Connection, RowData}
import org.joda.time.DateTime
import scalaz.{-\/, \/, \/-}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import play.api.Logger

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
      includedAt = row("updated_at").asInstanceOf[DateTime]
    )

  def FieldsWithTable(fields: String, table: String): String = fields.split(", ").map({ field => s"${table}." + field }).mkString(", ")

  val UserFields = "id, version, created_at, updated_at, username, email, givenname, surname, alias, account_type"
  val TeamsScorerFields = "leader, deleted, archived, created_at"
  val ScorerFields = FieldsWithTable(UserFields, "users") + ", " + FieldsWithTable(TeamsScorerFields, "teams_scorers") + " as included_at"

  val SelectFromTeam =
    s"""
       |SELECT $ScorerFields
       |FROM teams, users, teams_scorers
       |WHERE teams.id = teams_scorers.team_id
       |  AND teams_scorers.scorer_id = users.id
       |  AND teams.id = ?
       |  AND users.is_deleted = FALSE
       |ORDER BY teams_scorers.created_at
    """.stripMargin

  /**
   * List scorers on a given team.
   *
   * @return a future disjunction containing either the scorers, or a failure
   */
  override def list(team: Team)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Scorer]]] = {
    Logger.debug(s"List scorers command:\n$SelectFromTeam")
    val key = cacheTeamScorersKey(team.id)
    cacheRepository.cacheSeqScorer.getCached(key).flatMap {
      case \/-(scorerList) => Future successful \/-(scorerList)
      case -\/(noResults: RepositoryError.NoResults) =>
        for {
          scorerList <- lift(queryList(SelectFromTeam, Seq[Any](team.id)))
          _ <- lift(cacheRepository.cacheSeqScorer.putCache(key)(scorerList, ttl))
        } yield scorerList
      case -\/(error) => Future successful -\/(error)
    }
  }

}
