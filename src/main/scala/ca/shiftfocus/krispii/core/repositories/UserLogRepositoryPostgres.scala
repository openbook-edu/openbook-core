package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models._
import com.github.mauricio.async.db.{Connection, RowData}
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import scala.concurrent.Future
import scalaz.\/

class UserLogRepositoryPostgres extends UserLogRepository with PostgresRepository[UserLog] {
  override val entityName = "User Log"

  override def constructor(row: RowData): UserLog = {
    UserLog(
      id = row("id").asInstanceOf[UUID],
      userId = row("user_id").asInstanceOf[UUID],
      logType = row("log_type").asInstanceOf[String],
      data = Option(row("data").asInstanceOf[String]) match {
      case Some(data) => Some(data)
      case _ => None
    },
      createdAt = row("created_at").asInstanceOf[DateTime]
    )
  }

  val Table = "users_logs"
  val Fields = "id, user_id, log_type, data, created_at"
  val QMarks = "?, ?, ?, ?, ?"
  val OrderBy = s"${Table}.created_at ASC"

  def Insert(suffix: String): String = {
    s"""
       |INSERT INTO ${Table}_${suffix} ($Fields)
       |VALUES ($QMarks)
       |RETURNING $Fields
    """.stripMargin
  }

  def insert(userLog: UserLog)(implicit conn: Connection): Future[\/[RepositoryError.Fail, UserLog]] = {
    val createdDate = userLog.createdAt
    val formatSuffix = DateTimeFormat.forPattern("YYYYMM")
    val suffix = formatSuffix.print(createdDate)

    val params = Seq[Any](
      userLog.id, userLog.userId, userLog.logType, userLog.data, userLog.createdAt
    )

    queryOne(Insert(suffix), params)
  }
}
