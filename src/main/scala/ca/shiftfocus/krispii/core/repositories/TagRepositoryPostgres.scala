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
      row("is_admin").asInstanceOf[Boolean],
      row("is_hidden").asInstanceOf[Boolean],
      row("name").asInstanceOf[String],
      row("lang").asInstanceOf[String],
      Option(row("category_name").asInstanceOf[String]),
      row("frequency").asInstanceOf[Int]
    )
  }

  val Fields = "id, version, is_admin, is_hidden, name, lang, frequency"
  val QMarks = Fields.split(", ").map({ field => "?" }).mkString(", ")
  val Table = "tags"
  val Organizational =
    s"""
      |INNER JOIN organization_tags AS ot
      |ON ot.tag_id = t.id
    """.stripMargin

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

  def ListByProject(tagType: String = ""): String = {
    val condition = tagType match {
      case "organizational" => Organizational
      case "admin" => "WHERE t.is_admin = true"
      case _ => ""
    }

    s"""
      SELECT t.id, t.version, t.is_admin, t.is_hidden, t.name, t.lang, (SELECT name FROM tag_categories WHERE id = t.category_id) AS category_name, t.frequency FROM $Table t
      JOIN project_tags pt
      ON (pt.tag_id = t.id AND pt.project_id = ?)
      $condition
    """.stripMargin
  }

  def ListByOrganization(tagType: String = ""): String = {
    val condition = tagType match {
      case "admin" => "WHERE t.is_admin = true"
      case _ => ""
    }

    s"""
      SELECT t.id, t.version, t.is_admin, t.is_hidden, t.name, t.lang, (SELECT name FROM tag_categories WHERE id = t.category_id) AS category_name, t.frequency FROM $Table t
      JOIN organization_tags ot
      ON (ot.tag_id = t.id AND ot.organization_id = ?);
      $condition
    """.stripMargin
  }

  def ListByUser(tagType: String = ""): String = {
    val condition = tagType match {
      case "organizational" => Organizational
      case "admin" => "WHERE t.is_admin = true"
      case _ => ""
    }

    s"""
      SELECT t.id, t.version, t.is_admin, t.is_hidden, t.name, t.lang, (SELECT name FROM tag_categories WHERE id = t.category_id) AS category_name, t.frequency FROM $Table t
      JOIN user_tags ut
      ON (ut.tag_id = t.id AND ut.user_id = ?)
      $condition
    """.stripMargin
  }

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
                       |  WHERE is_admin = false
                       |  ORDER BY dist LIMIT 10) as sub
                       |WHERE dist < 0.9;
                        """.stripMargin

  val SelectAllAdminByKey = s"""
                       |SELECT $Fields, category_name
                       |FROM (SELECT $Fields, (SELECT name FROM tag_categories WHERE id = $Table.category_id) AS category_name, name <-> ? AS dist
                       |  FROM $Table
                       |  WHERE is_admin = true
                       |  ORDER BY dist LIMIT 10) as sub
                       |WHERE dist < 0.9;
                        """.stripMargin

  val SelectAllAdminByKeyForUser = s"""
                       |SELECT $Fields, category_name
                       |FROM (SELECT $Fields, (SELECT name FROM tag_categories WHERE id = $Table.category_id) AS category_name, name <-> ? AS dist
                       |  FROM $Table
                       |  JOIN user_tags
                       |    ON tag_id = tags.id
                       |    AND user_id = ?
                       |  WHERE is_admin = true
                       |  ORDER BY dist LIMIT 10) as sub
                       |WHERE dist < 0.9;
                        """.stripMargin

  val IsOrganizational =
    s"""
      |SELECT t.id
      |FROM $Table AS t
      |$Organizational
      |WHERE t.name = ?
      | AND t.lang = ?
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

  val UntagUser = s"""
                  |DELETE FROM user_tags
                  |WHERE user_id = ?
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

  val TagUser =
    s"""
       |INSERT INTO user_tags(user_id, tag_id)
       |VALUES (?, (SELECT id FROM tags WHERE name = ? AND lang = ? ))
     """.stripMargin

  val Update = s"""
                  UPDATE $Table
                  SET version = ?, is_admin = ?, is_hidden = ?, name = ?, lang = ?, category_id = (SELECT id FROM tag_categories WHERE name = ? AND lang = ?), frequency = ?
                  WHERE id = ?
                    AND version = ?
                  RETURNING $Fields, (SELECT name FROM tag_categories WHERE id = $Table.category_id) AS category_name
                """

  override def create(tag: Tag)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Tag]] = {
    queryOne(Insert, Seq[Any](tag.id, tag.version, tag.isAdmin, tag.isHidden, tag.name, tag.lang, tag.frequency, tag.category, tag.lang, tag.category, tag.lang))
  }

  override def update(tag: Tag)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Tag]] = {
    queryOne(Update, Seq[Any]((tag.version + 1), tag.isAdmin, tag.isHidden, tag.name, tag.lang, tag.category, tag.lang, tag.frequency, tag.id, tag.version))
  }
  override def delete(tag: Tag)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Tag]] = {
    queryOne(Delete, Seq[Any](tag.id, tag.version))
  }

  override def listByEntity(entityId: UUID, entityType: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Tag]]] = {
    entityType match {
      case TaggableEntities.project => queryList(ListByProject(), Seq[Any](entityId))
      case TaggableEntities.organization => queryList(ListByOrganization(), Seq[Any](entityId))
      case TaggableEntities.user => queryList(ListByUser(), Seq[Any](entityId))
      case _ => Future successful -\/(RepositoryError.BadParam("core.TagRepositoryPostgres.listByEntity.wrong.entity.type"))
    }
  }

  /**
   * List tags for entity that are also used to tag organizations
   * For organizations it will behave the same as listByEntity
   *
   * @param entityId
   * @param entityType
   * @param conn
   * @return
   */
  override def listOrganizationalByEntity(entityId: UUID, entityType: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Tag]]] = {
    entityType match {
      case TaggableEntities.project => queryList(ListByProject("organizational"), Seq[Any](entityId))
      case TaggableEntities.organization => queryList(ListByOrganization(), Seq[Any](entityId))
      case TaggableEntities.user => queryList(ListByUser("organizational"), Seq[Any](entityId))
      case _ => Future successful -\/(RepositoryError.BadParam("core.TagRepositoryPostgres.listByEntity.wrong.entity.type"))
    }
  }

  override def listAdminByEntity(entityId: UUID, entityType: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Tag]]] = {
    entityType match {
      case TaggableEntities.project => queryList(ListByProject("admin"), Seq[Any](entityId))
      case TaggableEntities.organization => queryList(ListByOrganization("admin"), Seq[Any](entityId))
      case TaggableEntities.user => queryList(ListByUser("admin"), Seq[Any](entityId))
      case _ => Future successful -\/(RepositoryError.BadParam("core.TagRepositoryPostgres.listByEntity.wrong.entity.type"))
    }
  }

  override def find(name: String, lang: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Tag]] = {
    queryOne(SelectOneByName, Seq[Any](name, lang))
  }

  override def find(tagId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Tag]] = {
    queryOne(SelectOneById, Seq[Any](tagId))
  }

  override def isOrganizational(tagName: String, tagLang: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Boolean]] = {
    conn.sendPreparedStatement(IsOrganizational, Seq[Any](tagName, tagLang)).map { result =>
      if (result.rows.get.length > 0) {
        \/-(true)
      }
      else {
        \/-(false)
      }
    }.recover {
      case exception: Throwable => throw exception
    }
  }

  override def untag(entityId: UUID, entityType: String, tagName: String, tagLang: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]] = {
    for {
      query <- lift {
        entityType match {
          case TaggableEntities.project => Future successful \/-(UntagProject)
          case TaggableEntities.organization => Future successful \/-(UntagOrganization)
          case TaggableEntities.user => Future successful \/-(UntagUser)
          case _ => Future successful -\/(RepositoryError.BadParam("core.TagRepositoryPostgres.untag.wrong.entity.type"))
        }
      }
      result <- lift {
        queryNumRows(query, Array[Any](entityId, tagName, tagLang))(_ == 1).map {
          case \/-(true) => \/-(())
          case \/-(false) => -\/(RepositoryError.NoResults(s"Could not remove the tag"))
          case -\/(error) => -\/(error)
        }
      }
    } yield result
  }

  /**
   * Search by trigrams for autocomplete
   * @param key
   * @param conn
   */
  override def trigramSearch(key: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Tag]]] = {
    queryList(SelectAllByKey, Seq[Any](key))
  }

  override def trigramSearchAdmin(key: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Tag]]] = {
    queryList(SelectAllAdminByKey, Seq[Any](key))
  }

  /**
   * Search for admin tags, with which user is tagged
   *
   * @param key
   * @param userId
   * @param conn
   * @return
   */
  override def trigramSearchAdmin(key: String, userId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Tag]]] = {
    queryList(SelectAllAdminByKeyForUser, Seq[Any](key, userId))
  }

  override def tag(entityId: UUID, entityType: String, tagName: String, tagLang: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]] = {
    for {
      query <- lift {
        entityType match {
          case TaggableEntities.project => Future successful \/-(TagProject)
          case TaggableEntities.organization => Future successful \/-(TagOrganization)
          case TaggableEntities.user => Future successful \/-(TagUser)
          case _ => Future successful -\/(RepositoryError.BadParam("core.TagRepositoryPostgres.tag.wrong.entity.type"))
        }
      }
      result <- lift {
        queryNumRows(query, Array[Any](entityId, tagName, tagLang))(_ == 1).map {
          case \/-(true) => \/-(())
          case \/-(false) => -\/(RepositoryError.NoResults(s"Could not add the tag"))
          case -\/(error) => -\/(error)
        }
      }
    } yield result
  }

  def listByCategory(category: String, lang: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Tag]]] = {
    queryList(ListByCategory, Seq[Any](category, lang, lang))
  }
}