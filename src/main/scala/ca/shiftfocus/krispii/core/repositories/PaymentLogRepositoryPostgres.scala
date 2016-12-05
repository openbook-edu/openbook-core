package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.PaymentLog
import com.github.mauricio.async.db.{ Connection, RowData }
import org.joda.time.DateTime
import play.api.libs.json.{ JsValue, Json }

import scala.concurrent.Future
import scalaz.\/

class PaymentLogRepositoryPostgres extends PaymentLogRepository with PostgresRepository[PaymentLog] {
  override val entityName = "Payment Log"
  override def constructor(row: RowData): PaymentLog = {
    PaymentLog(
      row("user_id").asInstanceOf[UUID],
      row("log_type").asInstanceOf[String],
      row("description").asInstanceOf[String],
      row("data").asInstanceOf[String],
      row("created_at").asInstanceOf[DateTime]
    )
  }

  val Table = "payment_logs"
  val Fields = "user_id, log_type, description, data, created_at"
  val QMarks = "?, ?, ?, ?, ?"
  val OrderBy = s"${Table}.created_at DESC"

  val SelectAll =
    s"""
       |SELECT $Fields
       |FROM $Table
       |ORDER BY $OrderBy
     """.stripMargin

  val SelectAllByUser =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE user_id = ?
       |ORDER BY $OrderBy
     """.stripMargin

  val Insert = {
    s"""
       |INSERT INTO $Table ($Fields)
       |VALUES ($QMarks)
       |RETURNING $Fields
    """.stripMargin
  }

  def list()(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[PaymentLog]]] = {
    queryList(SelectAll)
  }

  def list(userId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[PaymentLog]]] = {
    queryList(SelectAllByUser, Seq[Any](userId))
  }

  def insert(paymentLog: PaymentLog)(implicit conn: Connection): Future[\/[RepositoryError.Fail, PaymentLog]] = {
    val params = Seq[Any](
      paymentLog.userId, paymentLog.logType, paymentLog.description, paymentLog.data, new DateTime()
    )
    queryOne(Insert, params)
  }
}
