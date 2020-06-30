package ca.shiftfocus.krispii.core.repositories

import java.awt.Color
import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.User
import ca.shiftfocus.krispii.core.models.course.Exam
import com.github.mauricio.async.db.{Connection, RowData}
import org.joda.time.DateTime
import scalaz.{-\/, \/, \/-}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ExamRepositoryPostgres(
  // val userRepository: UserRepository,
  val cacheRepository: CacheRepository
)
    extends ExamRepository with PostgresRepository[Exam] {

  override val entityName = "Exam"

  def constructor(row: RowData): Exam = {
    Exam(
      row("id").asInstanceOf[UUID],
      row("version").asInstanceOf[Long],
      row("coordinator_id").asInstanceOf[UUID], // in SQL, not in model
      row("name").asInstanceOf[String],
      new Color(Option(row("color").asInstanceOf[Int]).getOrElse(0)),
      row("slug").asInstanceOf[String],
      Option(row("orig_rubric_id").asInstanceOf[UUID]) match {
        case Some(origRubricId) => Some(origRubricId)
        case _ => None
      },
      row("enabled").asInstanceOf[Boolean],
      row("archived").asInstanceOf[Boolean],
      row("deleted").asInstanceOf[Boolean],
      None, // teams
      None, // tests
      row("created_at").asInstanceOf[DateTime],
      row("updated_at").asInstanceOf[DateTime]
    )
  }

  val Table = "exams"
  val Fields = "id, version, coordinator_id, name, color, slug, orig_rubric_id, enabled, archived, deleted, created_at, updated_at"
  val FieldsWithTable = Fields.split(", ").map({ field => s"${Table}." + field }).mkString(", ")
  val OrderBy = s"${Table}.name ASC"

  // User CRUD operations
  val SelectAll =
    s"""
       |SELECT $Fields
       |FROM $Table
       |ORDER BY $OrderBy
  """.stripMargin

  val SelectOne =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE id = ?
       |LIMIT 1
     """.stripMargin

  val SelectOneBySlug =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE slug = ?
       |LIMIT 1
     """.stripMargin

  // Using here get_slug custom postgres function to generate unique slug if slug already exists
  val Insert = {
    s"""
       |INSERT INTO $Table ($Fields)
       |VALUES (?, ?, ?, ?, ?, get_slug(?, '$Table', ?), ?, ?, false, false, ?, ?)
       |RETURNING $Fields
    """.stripMargin
  }

  // Using here get_slug custom postgres function to generate unique slug if slug already exists
  val Update =
    s"""
       |UPDATE $Table
       |SET version = ?, coordinator_id = ?, name = ?, color = ?, slug = get_slug(?, '$Table', ?),
       |    orig_rubric_id = ?, enabled = ?, archived = ?, updated_at = ?
       |WHERE id = ?
       |  AND version = ?
       |RETURNING $Fields
     """.stripMargin

  val Delete =
    s"""
       |UPDATE $Table
       |SET is_deleted = true
       |WHERE id = ?
       |RETURNING $Fields
     """.stripMargin

  val ListByCoordinatorId =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE coordinator_id = ?
       |ORDER BY $OrderBy
     """.stripMargin

  /**
   * List all exams.
   *
   * @return an array of Exams
   */
  override def list(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Exam]]] = {
    queryList(SelectAll)
  }

  /**
   * Select exams based on the given user.
   *
   * @param user the coordinator to search by
   * TODO: implement for scorers, too, but restrict resulting exams to their team
   * TODO: need to specify if searching for coordinator or scorer to avoid retrieving bad cached values
   * @return the found exams
   */
  override def list(user: User) // format: OFF
                   (implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Exam]]] = { // format: ON

    val key = cacheCoordinatingKey(user.id)
    cacheRepository.cacheSeqExam.getCached(key).flatMap {
      case \/-(examList) => Future successful \/-(examList)
      case -\/(noResults: RepositoryError.NoResults) =>
        for {
          examList <- lift(queryList(ListByCoordinatorId, Seq[Any](user.id)))
          _ <- lift(cacheRepository.cacheSeqExam.putCache(key)(examList, ttl))
        } yield examList
      case -\/(error) => Future successful -\/(error)
    }

  }
  /**
   * Find a single entry by ID.
   *
   * @param id the 128-bit UUID, as a byte array, to search for.
   * @return an optional RowData object containing the results
   */
  override def find(id: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Exam]] = {
    cacheRepository.cacheExam.getCached(cacheExamKey(id)).flatMap {
      case \/-(exam) => Future successful \/-(exam)
      case -\/(noResults: RepositoryError.NoResults) =>
        for {
          exam <- lift(queryOne(SelectOne, Array[Any](id)))
          _ <- lift(cacheRepository.cacheUUID.putCache(cacheExamSlugKey(exam.slug))(exam.id, ttl))
          _ <- lift(cacheRepository.cacheExam.putCache(cacheExamKey(exam.id))(exam, ttl))
        } yield exam
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Find a single entry by slug.
   *
   * @param slug the exam's slug
   * @return an optional RowData object containing the results
   */
  override def find(slug: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Exam]] = {
    cacheRepository.cacheUUID.getCached(cacheExamSlugKey(slug)).flatMap {
      case \/-(examId) => {
        for {
          _ <- lift(cacheRepository.cacheUUID.putCache(cacheExamSlugKey(slug))(examId, ttl))
          exam <- lift(find(examId)) // will already read from and place the full exam into cache
        } yield exam
      }
      case -\/(noResults: RepositoryError.NoResults) => {
        for {
          exam <- lift(queryOne(SelectOneBySlug, Seq[Any](slug)))
          _ <- lift(cacheRepository.cacheUUID.putCache(cacheExamSlugKey(slug))(exam.id, ttl))
          _ <- lift(cacheRepository.cacheExam.putCache(cacheExamKey(exam.id))(exam, ttl))
        } yield exam
      }
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Insert an Exam row.
   *
   * @param exam
   * @param conn
   * @return
   */
  def insert(exam: Exam)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Exam]] = {
    val params = Seq[Any](
      exam.id, 1, exam.ownerId, exam.name, exam.color.getRGB, exam.slug, exam.id,
      exam.origRubricId, exam.enabled, new DateTime, new DateTime
    )

    for {
      inserted <- lift(queryOne(Insert, params))
      _ <- lift(cacheRepository.cacheSeqUser.removeCached(cacheTeachingKey(exam.ownerId)))
    } yield inserted
  }

  /**
   * Update an Exam row
   *
   * @param exam
   * @param conn
   * @return
   */
  override def update(exam: Exam) // format: OFF
                     (implicit conn: Connection): Future[\/[RepositoryError.Fail, Exam]] = { // format: ON
    val params = Seq[Any](
      exam.version + 1, exam.ownerId, exam.name, exam.color.getRGB, exam.slug, exam.id,
      exam.origRubricId, exam.enabled, exam.archived, new DateTime, exam.id, exam.version
    )
    for {
      updated <- lift(queryOne(Update, params))
      _ <- lift(cacheRepository.cacheExam.removeCached(cacheExamKey(updated.id)))
      _ <- lift(cacheRepository.cacheUUID.removeCached(cacheExamSlugKey(updated.slug)))
    } yield updated
  }

  /**
   * Delete an Exam row.
   *
   * @param exam
   * @param conn
   * @return
   */
  override def delete(exam: Exam)  // format: OFF
                     (implicit conn: Connection): Future[\/[RepositoryError.Fail, Exam]] = { // format: ON
    for {
      deleted <- lift(queryOne(Delete, Array(exam.id)))
      _ <- lift(cacheRepository.cacheExam.removeCached(cacheExamKey(deleted.id)))
      _ <- lift(cacheRepository.cacheUUID.removeCached(cacheExamSlugKey(deleted.slug)))
    } yield deleted
  }

}
