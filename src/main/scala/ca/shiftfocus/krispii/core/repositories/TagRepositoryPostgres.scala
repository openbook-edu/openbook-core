package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import com.github.mauricio.async.db.RowData
import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.{ Tag, TaggableEntities }
import com.github.mauricio.async.db.Connection

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.{ -\/, \/, \/- }

class TagRepositoryPostgres extends TagRepository with PostgresRepository[Tag] {

  override val entityName = "Tag"

  override def constructor(row: RowData): Tag = {
    Tag(
      row("id").asInstanceOf[UUID],
      row("version").asInstanceOf[Long],
      row("name").asInstanceOf[String],
      row("lang").asInstanceOf[String],
      Option(row("category_name").asInstanceOf[String]),
      row("frequency").asInstanceOf[Int]
    )
  }

  val Fields = "id, version, name, lang, frequency"
  val QMarks = Fields.split(", ").map({ field => "?" }).mkString(", ")

  val Table = "tags"

  val Insert = s"""
                  |INSERT INTO $Table ($Fields, category_id)
                  |VALUES ($QMarks, (SELECT id FROM tag_categories WHERE name = ? AND lang = ?))
                  |RETURNING $Fields, (SELECT name FROM tag_categories WHERE name = ? AND lang = ?) as category_name
                  """.stripMargin

  val Delete = s"""
                  |DELETE FROM $Table as t
                  |WHERE id = ?
                  | AND version = ?
                  |RETURNING $Fields, (SELECT name FROM tag_categories WHERE id = t.category_id) AS category_name
                  """.stripMargin

  val ListByProject = s"""
                        SELECT t.id, t.version, t.name, t.lang, (SELECT name FROM tag_categories WHERE id = t.category_id) AS category_name, t.frequency FROM $Table t
                        JOIN project_tags pt
                        ON (pt.tag_id = t.id AND pt.project_id = ?);
                        """.stripMargin

  val ListByOrganization = s"""
                        SELECT t.id, t.version, t.name, t.lang, (SELECT name FROM tag_categories WHERE id = t.category_id) AS category_name, t.frequency FROM $Table t
                        JOIN organization_tags ot
                        ON (ot.tag_id = t.id AND ot.organization_id = ?);
                        """.stripMargin

  val ListByCategory = s"""
                              SELECT $Fields, (SELECT name FROM tag_categories WHERE id = $Table.category_id) as category_name FROM $Table
                              WHERE category_id = (SELECT id FROM tag_categories WHERE name = ? AND lang = ?) AND lang = ?
                            """.stripMargin
  val SelectOneByName = s"""
                              SELECT $Fields, (SELECT name FROM tag_categories WHERE id = $Table.category_id) AS category_name FROM $Table
                              WHERE name = ? AND lang = ?
                            """.stripMargin

  val SelectOneById = s"""
                              SELECT $Fields, (SELECT name FROM tag_categories WHERE id = $Table.category_id) AS category_name FROM $Table
                              WHERE id = ?
                            """.stripMargin

  val SelectAllByKey = s"""
                       |SELECT $Fields, category_name
                       |FROM (SELECT $Fields, (SELECT name FROM tag_categories WHERE id = $Table.category_id) AS category_name, name <-> ? AS dist
                       |  FROM $Table
                       |  ORDER BY dist LIMIT 10) as sub
                       |WHERE dist < 0.9;
                        """.stripMargin

  val UntagProject = s"""
                  |DELETE FROM project_tags
                  |WHERE project_id = ?
                  | AND tag_id = (SELECT id FROM tags WHERE name = ? AND lang = ?)
                """.stripMargin

  val UntagOrganization = s"""
                  |DELETE FROM organization_tags
                  |WHERE organization_id = ?
                  | AND tag_id = (SELECT id FROM tags WHERE name = ? AND lang = ?)
                """.stripMargin

  val TagProject =
    s"""
       |INSERT INTO project_tags(project_id, tag_id)
       |VALUES (?, (SELECT id FROM tags WHERE name = ? AND lang = ? ))
     """.stripMargin

  val TagOrganization =
    s"""
       |INSERT INTO organization_tags(organization_id, tag_id)
       |VALUES (?, (SELECT id FROM tags WHERE name = ? AND lang = ? ))
     """.stripMargin

  val Update = s"""
                  UPDATE $Table
                  SET version = ?, name = ?, lang = ?, category_id = (SELECT id FROM tag_categories WHERE name = ? AND lang = ?), frequency = ?
                  WHERE id = ?
                    AND version = ?
                  RETURNING $Fields, (SELECT name FROM tag_categories WHERE id = $Table.category_id) AS category_name
                """

  override def create(tag: Tag)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Tag]] = {
    queryOne(Insert, Seq[Any](tag.id, tag.version, tag.name, tag.lang, tag.frequency, tag.category, tag.lang, tag.category, tag.lang))
  }

  override def update(tag: Tag)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Tag]] = {
    queryOne(Update, Seq[Any]((tag.version + 1), tag.name, tag.lang, tag.category, tag.lang, tag.frequency, tag.id, tag.version))
  }
  override def delete(tag: Tag)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Tag]] = {
    queryOne(Delete, Seq[Any](tag.id, tag.version))
  }

  override def listByEntity(entityId: UUID, entityType: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Tag]]] = {
    entityType match {
      case TaggableEntities.project => queryList(ListByProject, Seq[Any](entityId))
      case TaggableEntities.organization => queryList(ListByOrganization, Seq[Any](entityId))
      case _ => Future successful -\/(RepositoryError.BadParam("core.TagRepositoryPostgres.tag.wrong.entity.type"))
    }
  }

  override def find(name: String, lang: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Tag]] = {
    queryOne(SelectOneByName, Seq[Any](name, lang))
  }

  override def find(tagId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Tag]] = {
    queryOne(SelectOneById, Seq[Any](tagId))
  }

  override def untag(entityId: UUID, entityType: String, tagName: String, tagLang: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]] = {
    entityType match {
      case TaggableEntities.project => queryNumRows(UntagProject, Array[Any](entityId, tagName, tagLang))(_ == 1).map {
        case \/-(true) => \/-(())
        case \/-(false) => -\/(RepositoryError.NoResults(s"Could not remove the tag"))
        case -\/(error) => -\/(error)
      }
      case TaggableEntities.organization => queryNumRows(UntagOrganization, Array[Any](entityId, tagName, tagLang))(_ == 1).map {
        case \/-(true) => \/-(())
        case \/-(false) => -\/(RepositoryError.NoResults(s"Could not remove the tag"))
        case -\/(error) => -\/(error)
      }
      case _ => Future successful -\/(RepositoryError.BadParam("core.TagRepositoryPostgres.tag.wrong.entity.type"))
    }
  }

  /**
   * Search by trigrams for autocomplete
   * @param key
   * @param conn
   */
  override def trigramSearch(key: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Tag]]] = {
    queryList(SelectAllByKey, Seq[Any](key))
  }

  override def tag(entityId: UUID, entityType: String, tagName: String, tagLang: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]] = {
    entityType match {
      case TaggableEntities.project => queryNumRows(TagProject, Array[Any](entityId, tagName, tagLang))(_ == 1).map {
        case \/-(true) => \/-(())
        case \/-(false) => -\/(RepositoryError.NoResults(s"Could not add the tag"))
        case -\/(error) => -\/(error)
      }
      case TaggableEntities.organization => queryNumRows(TagOrganization, Array[Any](entityId, tagName, tagLang))(_ == 1).map {
        case \/-(true) => \/-(())
        case \/-(false) => -\/(RepositoryError.NoResults(s"Could not add the tag"))
        case -\/(error) => -\/(error)
      }
      case _ => Future successful -\/(RepositoryError.BadParam("core.TagRepositoryPostgres.tag.wrong.entity.type"))
    }
  }

  def listByCategory(category: String, lang: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Tag]]] = {
    queryList(ListByCategory, Seq[Any](category, lang, lang))
  }
}