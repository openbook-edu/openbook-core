package ca.shiftfocus.krispii.core.repositories

// import java.io.{PrintWriter, StringWriter}
import ca.shiftfocus.krispii.core.error.RepositoryError
import com.github.mauricio.async.db.{Connection, RowData}
import org.joda.time.DateTime
import play.api.libs.json.{JsValue, Json}
import scalaz.\/

import scala.concurrent.Future

/**
 * Work with database tables: stripe_events
 */
class StripeEventRepositoryPostgres extends StripeEventRepository with PostgresRepository[JsValue] {
  override val entityName = "Stripe"
  override def constructor(row: RowData): JsValue = {
    Json.parse(row("data").asInstanceOf[String])
  }

  //------ EVENTS ------------------------------------------------------------------------------------------------------

  val GetEvent =
    """
       |SELECT event as data
       |FROM stripe_events
       |WHERE id = ?
     """.stripMargin

  val InsertEvent =
    """
       |INSERT INTO stripe_events (id, type, event, created_at)
       |VALUES (?, ?, ?, ?)
       |RETURNING event as data
     """.stripMargin

  //--------------------------------------------------------------------------------------------------------------------

  // EVENTS

  def getEvent(eventId: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, JsValue]] = {
    queryOne(GetEvent, Seq[Any](eventId))
  }

  def createEvent(eventId: String, eventType: String, event: JsValue)(implicit conn: Connection): Future[\/[RepositoryError.Fail, JsValue]] = {
    queryOne(InsertEvent, Seq[Any](eventId, eventType, event, new DateTime()))
  }
}
