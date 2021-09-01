package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.{Tag, TaggableEntities}
import com.github.mauricio.async.db.{Connection, RowData}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scalaz.{-\/, \/, \/-}

class TagRepositoryPostgres(val cacheRepository: CacheRepository) extends TagRepository with PostgresRepository[Tag] {

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

  private val Table = "tags"
  private val Fields = "id, version, is_admin, is_hidden, name, lang, frequency"
  def FieldsWithTable(table: String = Table): String = Fields.split(", ").map({ field => s"${table}." + field }).mkString(", ")
  private val QMarks = Fields.split(", ").map({ field => "?" }).mkString(", ")
  private val Organizational = """
      |INNER JOIN organization_tags AS ot
      |ON ot.tag_id = t.id
    """.stripMargin

  private val Insert = s"""
                  |INSERT INTO $Table ($Fields, category_id)
                  |VALUES ($QMarks, (SELECT id FROM tag_categories WHERE name = ? AND lang = ?))
                  |RETURNING $Fields, (SELECT name FROM tag_categories WHERE name = ? AND lang = ?) as category_name
                  """.stripMargin

  private val Delete = s"""
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

  def ListByPlan(tagType: String = ""): String = {
    val condition = tagType match {
      case "organizational" => Organizational
      case "admin" => "WHERE t.is_admin = true"
      case _ => ""
    }

    s"""
      SELECT t.id, t.version, t.is_admin, t.is_hidden, t.name, t.lang, (SELECT name FROM tag_categories WHERE id = t.category_id) AS category_name, t.frequency FROM $Table t
      JOIN stripe_plan_tags spt
      ON (spt.tag_id = t.id AND spt.plan_id = ?)
      $condition
    """.stripMargin
  }

  private val ListByCategory = s"""
    SELECT $Fields, (SELECT name FROM tag_categories WHERE id = $Table.category_id) as category_name FROM $Table
    WHERE category_id = (SELECT id FROM tag_categories WHERE name = ? AND lang = ?) AND lang = ?
  """.stripMargin

  private val SelectOneByName = s"""
    SELECT $Fields, (SELECT name FROM tag_categories WHERE id = $Table.category_id) AS category_name FROM $Table
    WHERE name = ? AND lang = ?
  """.stripMargin

  private val SelectOneById = s"""
    SELECT $Fields, (SELECT name FROM tag_categories WHERE id = $Table.category_id) AS category_name FROM $Table
    WHERE id = ?
  """.stripMargin

  def SelectAllPopular(lang: String, limit: String, skipedCategories: IndexedSeq[String]) = {
    var inClause = ""

    if (skipedCategories.nonEmpty) {
      val length = skipedCategories.length
      inClause = "AND tc.name NOT IN ("
      skipedCategories.zipWithIndex.map {
        case (category, index) =>
          inClause += s"'${category}'"
          inClause = {
            if (index != (length - 1)) inClause + ", "
            else inClause + ")"
          }
      }
    }

    s"""
      |WITH pr_freq AS (
      | SELECT parent_id, count(*) AS frequency
      | FROM projects
      | WHERE parent_id IS NOT NULL
      | GROUP BY parent_id
      | ORDER BY count(*) DESC
      |),
      |popular_pr AS (
      | SELECT frequency, id
      | FROM pr_freq
      | JOIN projects
      |   ON projects.id = pr_freq.parent_id
      | WHERE enabled = true
      |   AND (status IS NULL OR status = '')
      |   AND is_master = true
      | ORDER BY pr_freq.frequency DESC
      |),
      |popular_tags AS (
      | SELECT max(popular_pr.frequency) as max_frequency, tag_id
      | FROM project_tags
      |  JOIN popular_pr
      |  ON popular_pr.id = project_tags.project_id
      | GROUP BY tag_id
      | ORDER BY max_frequency DESC
      |)
      |SELECT ${FieldsWithTable()}, tc.name AS category_name
      |FROM tags
      |RIGHT JOIN popular_tags
      | ON $Table.id = popular_tags.tag_id
      |LEFT JOIN tag_categories as tc
      | ON $Table.category_id = tc.id
      |WHERE $Table.is_hidden = false
      | AND $Table.frequency > 0
      | AND $Table.lang = '$lang'
      | ${inClause}
      |LIMIT $limit
    """.stripMargin
  }

  private val SelectAllByKey = s"""
     |SELECT $Fields, category_name
     |FROM (SELECT $Fields, (SELECT name FROM tag_categories WHERE id = $Table.category_id) AS category_name, name <-> ? AS dist
     |  FROM $Table
     |  WHERE is_admin = false
     |  ORDER BY dist LIMIT 10) as sub
     |WHERE dist < 0.9;
  """.stripMargin

  private val SelectAllAdminByKey = s"""
     |SELECT $Fields, category_name
     |FROM (SELECT $Fields, (SELECT name FROM tag_categories WHERE id = $Table.category_id) AS category_name, name <-> ? AS dist
     |  FROM $Table
     |  WHERE is_admin = true
     |  ORDER BY dist LIMIT 10) as sub
     |WHERE dist < 0.9;
    """.stripMargin

  private val SelectAllAdminByKeyForUser = s"""
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

  private val IsOrganizational = s"""
      |SELECT t.id
      |FROM $Table AS t
      |$Organizational
      |WHERE t.name = ?
      | AND t.lang = ?
    """.stripMargin

  private val UntagProject = """
    |DELETE FROM project_tags
    |WHERE project_id = ?
    | AND tag_id = (SELECT id FROM tags WHERE name = ? AND lang = ?)
  """.stripMargin

  private val UntagOrganization = """
    |DELETE FROM organization_tags
    |WHERE organization_id = ?
    | AND tag_id = (SELECT id FROM tags WHERE name = ? AND lang = ?)
  """.stripMargin

  private val UntagUser = """
    |DELETE FROM user_tags
    |WHERE user_id = ?
    | AND tag_id = (SELECT id FROM tags WHERE name = ? AND lang = ?)
  """.stripMargin

  private val UntagPlan = """
    |DELETE FROM stripe_plan_tags
    |WHERE plan_id = ?
    | AND tag_id = (SELECT id FROM tags WHERE name = ? AND lang = ?)
  """.stripMargin

  private val TagProject = """
     |INSERT INTO project_tags(project_id, tag_id)
     |VALUES (?, (SELECT id FROM tags WHERE name = ? AND lang = ? ))
   """.stripMargin

  private val TagOrganization = """
     |INSERT INTO organization_tags(organization_id, tag_id)
     |VALUES (?, (SELECT id FROM tags WHERE name = ? AND lang = ? ))
   """.stripMargin

  private val TagUser = """
     |INSERT INTO user_tags(user_id, tag_id)
     |VALUES (?, (SELECT id FROM tags WHERE name = ? AND lang = ? ))
   """.stripMargin

  private val TagPlan = """
     |INSERT INTO stripe_plan_tags(plan_id, tag_id)
     |VALUES (?, (SELECT id FROM tags WHERE name = ? AND lang = ? ))
   """.stripMargin

  private val Update = s"""
    |UPDATE $Table
    |SET version = ?, is_admin = ?, is_hidden = ?, name = ?, lang = ?,
    | category_id = (SELECT id FROM tag_categories WHERE name = ? AND lang = ?),
    | frequency = ?
    |WHERE id = ?
    |  AND version = ?
    |RETURNING $Fields, (SELECT name FROM tag_categories WHERE id = $Table.category_id) AS category_name
  """.stripMargin

  override def create(tag: Tag)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Tag]] = {
    queryOne(Insert, Seq[Any](tag.id, tag.version, tag.isAdmin, tag.isHidden, tag.name, tag.lang, tag.frequency, tag.category, tag.lang, tag.category, tag.lang))
  }

  override def update(tag: Tag)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Tag]] = {
    queryOne(Update, Seq[Any]((tag.version + 1), tag.isAdmin, tag.isHidden, tag.name, tag.lang, tag.name, tag.lang, tag.frequency, tag.id, tag.version))
  }
  override def delete(tag: Tag)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Tag]] = {
    queryOne(Delete, Seq[Any](tag.id, tag.version))
  }

  /**
   * List popular tags. We find all popular master project, get their tags and list them.
   *
   * @param lang tags in desired language
   * @param limit Optional 0 for all, default 0
   * @param skipedCategories Optional Tags from this categories should be skiped
   * @param conn
   * @return
   */
  override def listPopular(lang: String, limit: Int = 0, skipedCategories: IndexedSeq[String] = IndexedSeq.empty[String])(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Tag]]] = {
    cacheRepository.cacheSeqTag.getCached(cachePopularTagsKey(lang)).flatMap {
      case \/-(tagList) => Future successful \/-(tagList)
      case -\/(noResults: RepositoryError.NoResults) => {
        val queryLimit = {
          if (limit == 0) "ALL"
          else limit.toString
        }

        for {
          tagList <- lift(queryList(SelectAllPopular(lang, queryLimit, skipedCategories)))
          _ <- lift(cacheRepository.cacheSeqTag.putCache(cachePopularTagsKey(lang))(tagList, Some(24.hours)))
        } yield tagList
      }
      case -\/(error) => Future successful -\/(error)
    }
  }

  override def listByEntity(entityId: String, entityType: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Tag]]] = {
    entityType match {
      case TaggableEntities.project => queryList(ListByProject(), Seq[Any](entityId))
      case TaggableEntities.organization => queryList(ListByOrganization(), Seq[Any](entityId))
      case TaggableEntities.user => queryList(ListByUser(), Seq[Any](entityId))
      case TaggableEntities.plan => queryList(ListByPlan(), Seq[Any](entityId))
      case _ => Future successful -\/(RepositoryError.BadParam("core.TagRepositoryPostgres.listByEntity.wrong.entity.type"))
    }
  }

  /**
   * List tags for entity that are also used to tag organizations
   * For organizations it will behave the same as listByEntity
   *
   * @param entityId a String because stripe plan IDs are not UUIDs; other IDs will be converted to Strings
   * @param entityType "project", "organization", "user" or "plan"
   * @param conn
   * @return either an IndexedSeq of Tags or an error
   */
  override def listOrganizationalByEntity(entityId: String, entityType: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Tag]]] = {
    entityType match {
      case TaggableEntities.project => queryList(ListByProject("organizational"), Seq[Any](entityId))
      case TaggableEntities.organization => queryList(ListByOrganization(), Seq[Any](entityId))
      case TaggableEntities.user => queryList(ListByUser("organizational"), Seq[Any](entityId))
      case TaggableEntities.plan => queryList(ListByPlan("organizational"), Seq[Any](entityId))
      case _ => Future successful -\/(RepositoryError.BadParam("core.TagRepositoryPostgres.listByEntity.wrong.entity.type"))
    }
  }

  override def listAdminByEntity(entityId: String, entityType: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Tag]]] = {
    entityType match {
      case TaggableEntities.project => queryList(ListByProject("admin"), Seq[Any](entityId))
      case TaggableEntities.organization => queryList(ListByOrganization("admin"), Seq[Any](entityId))
      case TaggableEntities.user => queryList(ListByUser("admin"), Seq[Any](entityId))
      case TaggableEntities.plan => queryList(ListByPlan("admin"), Seq[Any](entityId))
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

  override def untag(entityId: String, entityType: String, tagName: String, tagLang: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]] = {
    for {
      query <- lift {
        entityType match {
          case TaggableEntities.project => Future successful \/-(UntagProject)
          case TaggableEntities.organization => Future successful \/-(UntagOrganization)
          case TaggableEntities.user => Future successful \/-(UntagUser)
          case TaggableEntities.plan => Future successful \/-(UntagPlan)
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

  override def tag(entityId: String, entityType: String, tagName: String, tagLang: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]] = {
    for {
      query <- lift {
        entityType match {
          case TaggableEntities.project => Future successful \/-(TagProject)
          case TaggableEntities.organization => Future successful \/-(TagOrganization)
          case TaggableEntities.user => Future successful \/-(TagUser)
          case TaggableEntities.plan => Future successful \/-(TagPlan)
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