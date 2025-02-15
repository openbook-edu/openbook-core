package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.UserPreference
import com.github.mauricio.async.db.{Connection, RowData}
import play.api.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.{-\/, \/, \/-}

class UserPreferenceRepositoryPostgres extends UserPreferenceRepository with PostgresRepository[UserPreference] {
  override val entityName = "UserPreference"

  override def constructor(row: RowData): UserPreference = {
    UserPreference(
      userId = row("user_id").asInstanceOf[UUID],
      prefName = row("machine_name").asInstanceOf[String],
      state = row("state").asInstanceOf[String]
    )
  }

  val Table = "users_preferences"
  val Fields = "user_id, pref_id, state"

  val SelectByUser =
    s"""
       |SELECT $Fields, machine_name
       |FROM $Table
       |JOIN preferences p
       |  ON p.id = pref_id
       |WHERE user_id = ?
       |  AND p.machine_name = ?
     """.stripMargin

  val SelectAllByUser =
    s"""
       |SELECT $Fields, machine_name
       |FROM $Table
       |JOIN preferences p
       |  ON p.id = pref_id
       |WHERE user_id = ?
     """.stripMargin

  val Update = {
    s"""
       |WITH p AS (SELECT id, machine_name FROM preferences WHERE machine_name = ?),
       |     pv AS (SELECT pref_state
       |            FROM preferences_allowed_values
       |            WHERE pref_id = (SELECT id FROM p)
       |              AND pref_state = ?),
       |     up AS (UPDATE $Table
       |           SET state = ?
       |           WHERE user_id = ?
       |            AND pref_id = (SELECT id FROM p)
       |            AND EXISTS(SELECT 1 FROM pv)
       |           RETURNING $Fields)
       |SELECT up.pref_id, up.user_id, up.state, p.machine_name
       |FROM p, pv, up
     """.stripMargin
  }

  val Insert = {
    s"""
       |WITH p AS (SELECT id, machine_name
       |           FROM preferences
       |           WHERE machine_name = ?),
       |     pv AS (SELECT pref_state
       |            FROM preferences_allowed_values
       |            WHERE pref_id = (SELECT id FROM p)
       |              AND pref_state = ?),
       |     up AS (INSERT INTO $Table ($Fields)
       |            SELECT ?, (SELECT id FROM p), ?
       |            WHERE EXISTS(SELECT 1 FROM pv)
       |            RETURNING $Fields)
       |SELECT up.pref_id, up.user_id, up.state, p.machine_name
       |FROM p, pv, up
     """.stripMargin
  }

  val DeleteAllForUser =
    s"""
       |DELETE FROM $Table
       |USING
       |  preferences
       |WHERE user_id = ?
       | AND preferences.id = $Table.pref_id
       |RETURNING $Fields, machine_name
     """.stripMargin

  def get(userId: UUID, pref: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, UserPreference]] = {
    queryOne(SelectByUser, Seq[Any](userId, pref))
  }

  def list(userId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[UserPreference]]] = {
    queryList(SelectAllByUser, Seq[Any](userId))
  }

  // Upsert for user preferences. If preference name or preference allowed value don't exist then NoResults will be returned
  def set(userPreference: UserPreference)(implicit conn: Connection): Future[\/[RepositoryError.Fail, UserPreference]] = {
    // Logger.debug("Now setting user preferences")
    queryOne(Update, Seq[Any](userPreference.prefName, userPreference.state, userPreference.state, userPreference.userId)).flatMap {
      case \/-(preference) => Future successful \/-({
        Logger.info(s"Successfully set ${userPreference.prefName} to ${userPreference.state} for user no. ${userPreference.userId}")
        preference
      })
      case -\/(error: RepositoryError.NoResults) => {
        Logger.info(s"User no. ${userPreference.userId} had no preference for ${userPreference.prefName}; will now set it to ${userPreference.state}...")
        for {
          insert <- lift(queryOne(Insert, Seq[Any](userPreference.prefName, userPreference.state, userPreference.userId, userPreference.state)))
          _ = Logger.info(s"User no. ${userPreference.userId} had no preference for ${userPreference.prefName}; now setting it to ${userPreference.state}...")
        } yield insert
      }
      case -\/(error) => Future successful -\/({
        Logger.error(s"${error} while setting ${userPreference.prefName} to ${userPreference.state} for user no. ${userPreference.userId}")
        error
      })
    }
  }

  /**
   * Delete all for a user
   *
   * @param userId
   * @param conn
   * @return
   */
  def delete(userId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[UserPreference]]] = {
    queryList(DeleteAllForUser, Seq[Any](userId))
  }
}
