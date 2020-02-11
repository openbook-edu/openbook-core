package ca.shiftfocus.krispii.core.repositories

import java.util.UUID
import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.TagCategory
import com.github.mauricio.async.db.{Connection, RowData}
import scala.concurrent.Future
import scalaz.\/

class TagCategoryRepositoryPostgres extends TagCategoryRepository with PostgresRepository[TagCategory] {
  override val entityName = "TagCategory"

  override def constructor(row: RowData): TagCategory = {
    TagCategory(
      row("id").asInstanceOf[UUID],
      row("version").asInstanceOf[Long],
      row("name").asInstanceOf[String],
      row("lang").asInstanceOf[String]
    )
  }

  val Fields = "id, version, name, lang"
  val QMarks = Fields.split(", ").map({ field => "?" }).mkString(", ")
  val Table = "tag_categories"

  val SelectOneById =
    s"""
      |SELECT $Fields
      |FROM $Table
      |WHERE id = ?
    """.stripMargin

  val SelectOneByName =
    s"""
      |SELECT $Fields
      |FROM $Table
      |WHERE name = ?
      | AND lang = ?
    """.stripMargin

  val SelectAllByLang =
    s"""
      |SELECT $Fields FROM $Table
      |WHERE lang = ?
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
      |SET version = ?, name = ?
      | WHERE id = ?
      |RETURNING $Fields
    """.stripMargin

  val Delete =
    s"""
      |DELETE FROM $Table
      |WHERE id = ?
      | AND version = ?
      |RETURNING $Fields
    """.stripMargin

  def find(tagCategoryId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, TagCategory]] = {
    queryOne(SelectOneById, Seq[Any](tagCategoryId))
  }

  def findByName(name: String, lang: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, TagCategory]] = {
    queryOne(SelectOneByName, Seq[Any](name, lang))
  }

  override def listByLanguage(lang: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[TagCategory]]] = {
    queryList(SelectAllByLang, Seq[Any](lang))
  }

  override def create(tagCategory: TagCategory)(implicit conn: Connection): Future[\/[RepositoryError.Fail, TagCategory]] = {
    queryOne(Insert, Seq[Any](tagCategory.id, tagCategory.version, tagCategory.name, tagCategory.lang))
  }

  override def update(tagCategory: TagCategory)(implicit conn: Connection): Future[\/[RepositoryError.Fail, TagCategory]] = {
    queryOne(Update, Seq[Any](tagCategory.id, tagCategory.version, tagCategory.name))
  }

  override def delete(tagCategory: TagCategory)(implicit conn: Connection): Future[\/[RepositoryError.Fail, TagCategory]] = {
    queryOne(Delete, Seq[Any](tagCategory.id, tagCategory.version))
  }
}