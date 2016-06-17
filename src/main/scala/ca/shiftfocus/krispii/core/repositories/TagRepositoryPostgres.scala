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
      row("id").asInstanceOf[UUID],
      row("name").asInstanceOf[String]
    )
  }

  val Fields = "id, name"
  val QMarks = "?, ?"

  val Table = "tags"

  val Insert = s"""
                  |INSERT INTO $Table ($Fields)
                  |VALUES ($QMarks)
                  |RETURNING $Fields
                  """.stripMargin

  val Delete = s"""
                  |DELETE FROM $Table
                  |WHERE id = ?
                  |RETURNING $Fields
                  """.stripMargin

  val ListByProject = s"""
                        SELECT t.name, t.id FROM $Table t
                        JOIN project_tags pt
                        ON (pt.tag_id = t.id AND pt.project_id = ?);
                        """.stripMargin

  val SelectOneByName = s"""
                              SELECT $Fields FROM $Table
                              WHERE name = ?
                            """.stripMargin

  val SelectAllByKey = s"""
                       SELECT $Fields from (SELECT $Fields, name <-> ? AS dist
                       |FROM $Table
                       |ORDER BY dist LIMIT 10) as sub  where dist < 0.9;
                        """.stripMargin

  val Untag = s"""
                  |DELETE FROM project_tags
                  |WHERE project_id = ?
                  |AND tag_id = ?
                """.stripMargin
  val TagProject =
    s"""
       |INSERT INTO project_tags(project_id, tag_id)
       |VALUES (?, ?)
     """.stripMargin
  override def create(tag: Tag)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Tag]] = {
    queryOne(Insert, Seq[Any](tag.id, tag.name))
  }

  override def delete(tagId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Tag]] = {
    queryOne(Delete, Seq[Any](tagId))
  }

  override def listByProjectId(projectId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Tag]]] = {
    queryList(ListByProject, Seq[Any](projectId))
  }

  override def find(name: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Tag]] = {
    queryOne(SelectOneByName, Seq[Any](name))
  }
  override def untag(projectId: UUID, tagId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]] = {
    for {
      _ <- lift(queryNumRows(Untag, Array[Any](projectId, tagId))(_ == 1).map {
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

  override def tag(projectId: UUID, tagId: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Unit]] = {
    for {
      _ <- lift(queryNumRows(TagProject, Array[Any](projectId, tagId))(_ == 1).map {
        case \/-(true) => \/-(())
        case \/-(false) => -\/(RepositoryError.NoResults(s"Could not remove the tag"))
        case -\/(error) => -\/(error)
      })
    } yield ()
  }
}
/**
 * *
 * create table tags(id uuid primary key, name varchar(150));
 * create table project_tags(project_id uuid references projects(id), tag_id uuid references tags(id));
 * *
 *
 * need:
 * tag something
 * untag
 * list tags for project
 * find tag
 * possibly type ahead tag
 * *
 * insert into tags(id, name) values('02d6304b98504c9291a94d12654b33bb', 'vanille');
 * insert into tags(id, name) values('871b525067124e548ab60784cae0bc64', 'pacificsound3003');
 * *
 * insert into project_tags(project_id, tag_id) values ('4f8aecf2-ac12-497d-b8b6-077f7eb9b4c4','871b525067124e548ab60784cae0bc64');
 * insert into project_tags(project_id, tag_id) values ('4f8aecf2-ac12-497d-b8b6-077f7eb9b4c4','02d6304b98504c9291a94d12654b33bb');
 * insert into project_tags(project_id, tag_id) values ('634b47ba-2ccd-465e-a21b-040ce9c092be','02d6304b98504c9291a94d12654b33bb');
 * *
 *
 * methods:
 * *
 * tag(projectId, tagId){
 * insert into project_tags(project_id, tag_id) values ('4f8aecf2-ac12-497d-b8b6-077f7eb9b4c4','871b525067124e548ab60784cae0bc64');
 * *
 * }
 * *
 * insert(name){
 * insert into tags(id, name) values('02d6304b98504c9291a94d12654b33bb', 'vanille');
 * *
 * }
 * * *
 * list(projectId){
 * select t.name, t.id from tags t join project_tags pt on (pt.tag_id = t.id and pt.project_id = ?);
 * }
 * *
 * delete from tags where id = ?
 *
 */

