package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.TagCategory
import com.github.mauricio.async.db.{ Connection, RowData }
import scala.concurrent.Future
import scalaz.\/

class TagCategoryRepositoryPostgres extends TagCategoryRepository with PostgresRepository[TagCategory] {
  override val entityName = "TagCategory"

  override def constructor(row: RowData): TagCategory = {
    TagCategory(
      row("name").asInstanceOf[String],
      row("lang").asInstanceOf[String]
    )
  }

  val Fields = "name, lang"
  val QMarks = "?, ?"
  val Table = "tag_categories"

  val Insert = s"""
                  |INSERT INTO $Table ($Fields)
                  |VALUES ($QMarks)
                  |RETURNING $Fields
                  """.stripMargin

  val Delete = s"""
                  |DELETE FROM $Table
                  |WHERE name = ?
                  |RETURNING $Fields
                  """.stripMargin

  val SelectAllByLang = s"""
                              SELECT $Fields FROM $Table
                              WHERE lang = ?
                            """.stripMargin

  override def create(tagCategory: TagCategory)(implicit conn: Connection): Future[\/[RepositoryError.Fail, TagCategory]] = {
    queryOne(Insert, Seq[Any](tagCategory.name, tagCategory.lang))
  }

  override def delete(tagCategoryName: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, TagCategory]] = {
    queryOne(Delete, Seq[Any](tagCategoryName))
  }

  override def listByLanguage(lang: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[TagCategory]]] = {
    queryList(SelectAllByLang, Seq[Any](lang))
  }
}