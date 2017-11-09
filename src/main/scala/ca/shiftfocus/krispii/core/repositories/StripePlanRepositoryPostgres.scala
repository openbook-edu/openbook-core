package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.stripe.StripePlan
import com.github.mauricio.async.db.{Connection, RowData}
import org.joda.time.DateTime

import scala.concurrent.Future
import scalaz.\/

class StripePlanRepositoryPostgres extends StripePlanRepository with PostgresRepository[StripePlan] {
  override val entityName = "StripePlan"

  override def constructor(row: RowData): StripePlan = {
    StripePlan(
      row("id").asInstanceOf[UUID],
      row("version").asInstanceOf[Long],
      row("stripe_id").asInstanceOf[String],
      row("title").asInstanceOf[String],
      row("created_at").asInstanceOf[DateTime],
      row("updated_at").asInstanceOf[DateTime]
    )
  }

  val Fields = "id, version, stripe_id, title, created_at, updated_at"
  val QMarks = Fields.split(", ").map({ field => "?" }).mkString(", ")
  val FieldsWithQMarks = Fields.split(", ").map({ field => s"${field} = ?" }).mkString(", ")
  val Table = "stripe_plans"

  val SelectOneById =
    s"""
      |SELECT $Fields
      |FROM $Table
      |WHERE id = ?
    """.stripMargin

  val SelectOneByStripeId =
    s"""
      |SELECT $Fields
      |FROM $Table
      |WHERE stripe_id = ?
    """.stripMargin

  val Insert =
    s"""
      |INSERT INTO $Table ($Fields)
      |VALUES ($QMarks)
      |RETURNING $Fields
    """.stripMargin

  val Update =
    s"""
      |UPDATE $Table
      |SET (version = ?, title = ?, updated_at = ?)
      |WHERE id = ?
      |  AND version = ?
      |RETURNING $Fields
    """.stripMargin

  val Delete =
    s"""
      |DELETE FROM $Table
      |WHERE id = ?
      | AND version = ?
      |RETURNING $Fields
    """.stripMargin

  def find(id: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, StripePlan]] = {
    queryOne(SelectOneById, Seq[Any](id))
  }

  def find(stripeId: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, StripePlan]] = {
    queryOne(SelectOneByStripeId, Seq[Any](stripeId))
  }

  def create(stripePlan: StripePlan)(implicit conn: Connection): Future[\/[RepositoryError.Fail, StripePlan]] = {
    val params = Seq[Any](
      stripePlan.id, stripePlan.version, stripePlan.stripeId, stripePlan.title, stripePlan.createdAt, stripePlan.updatedAt
    )

    queryOne(Insert, params)
  }

  def update(stripePlan: StripePlan)(implicit conn: Connection): Future[\/[RepositoryError.Fail, StripePlan]] = {
    val params = Seq[Any](
      stripePlan.version + 1, stripePlan.title, new DateTime(),
      stripePlan.id, stripePlan.version
    )

    queryOne(Update, params)
  }

  def delete(stripePlan: StripePlan)(implicit conn: Connection): Future[\/[RepositoryError.Fail, StripePlan]] = {
    queryOne(Delete, Seq[Any](stripePlan.id, stripePlan.version))
  }
}
