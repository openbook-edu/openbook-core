package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import com.github.mauricio.async.db.{ Connection, ResultSet, RowData }
import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.{ Component, Tag }
import ca.shiftfocus.krispii.core.repositories.{ PostgresRepository, TagRepository }
import com.github.mauricio.async.db.Connection

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.{ \/, -\/, \/-, EitherT }

class TagRepositoryPostgres extends TagRepository with PostgresRepository[Tag] {

  override val entityName = "Tag"

  override def constructor(row: RowData): Tag = {
    Tag(
      row("name").asInstanceOf[String],
      row("lang").asInstanceOf[String],
      row("category").asInstanceOf[String],
      row("frequency").asInstanceOf[Int]
    )
  }

  val Fields = "name, lang, category, frequency"
  val QMarks = "?, ?, ?, ?"

  val Table = "tags"

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

  val ListByProject = s"""
                        SELECT t.name, t.lang, t.category, t.frequency FROM $Table t
                        JOIN project_tags pt
                        ON (pt.tag_name = t.name AND pt.tag_lang = t.lang AND pt.project_id = ?);
                        """.stripMargin

  val ListByCategory = s"""
                              SELECT $Fields FROM $Table
                              WHERE category = ? AND lang = ?
                            """.stripMargin
  val SelectOneByName = s"""
                              SELECT $Fields FROM $Table
                              WHERE name = ? AND lang = ?
                            """.stripMargin

  val SelectAllByKey = s"""
                       SELECT $Fields from (SELECT $Fields, name <-> ? AS dist
                       |FROM $Table
                       |ORDER BY dist LIMIT 10) as sub  where dist < 0.9;
                        """.stripMargin

  val Untag = s"""
                  |DELETE FROM project_tags
                  |WHERE project_id = ?
                  |AND tag_name = ?
                  |AND tag_lang = ?
                """.stripMargin
  val TagProject =
    s"""
       |INSERT INTO project_tags(project_id, tag_name, tag_lang)
       |VALUES (?, ?, ?)
     """.stripMargin

  val Update = s"""
                  UPDATE $Table
                  SET  lang = ?, category = ?, frequency = ?
                  WHERE name = ? AND lang = ?
                  RETURNING $Fields"""

  override def create(tag: Tag)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Tag]] = {
    queryOne(Insert, Seq[Any](tag.name, tag.lang, tag.category, tag.frequency))
  }

  override def update(tag: Tag)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Tag]] = {
    queryOne(Update, Seq[Any](tag.lang, tag.category, tag.frequency, tag.name, tag.lang))
  }
  override def delete(tagName: String, tagLang: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Tag]] = {
    queryOne(Delete, Seq[Any](tagName))
  }

  override def listByProjectId(projectId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Tag]]] = {
    queryList(ListByProject, Seq[Any](projectId))
  }

  override def find(name: String, lang: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Tag]] = {
    queryOne(SelectOneByName, Seq[Any](name, lang))
  }
  override def untag(projectId: UUID, tagName: String, tagLang: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]] = {
    for {
      _ <- lift(queryNumRows(Untag, Array[Any](projectId, tagName, tagLang))(_ == 1).map {
        case \/-(true) => \/-(())
        case \/-(false) => -\/(RepositoryError.NoResults(s"Could not remove the tag"))
        case -\/(error) => -\/(error)
      })
    } yield ()
  }

  /**
   * Search by trigrams for autocomplete
   * @param key
   * @param conn
   */
  override def trigramSearch(key: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Tag]]] = {
    queryList(SelectAllByKey, Seq[Any](key))
  }

  override def tag(projectId: UUID, tagName: String, tagLang: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]] = {
    for {
      _ <- lift(queryNumRows(TagProject, Array[Any](projectId, tagName, tagLang))(_ == 1).map {
        case \/-(true) => \/-(())
        case \/-(false) => -\/(RepositoryError.NoResults(s"Could not remove the tag"))
        case -\/(error) => -\/(error)
      })
    } yield ()
  }

  def listByCategory(category: String, lang: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Tag]]] = {
    queryList(ListByCategory, Seq[Any](category, lang))
  }

}