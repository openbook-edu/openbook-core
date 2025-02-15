package ca.shiftfocus.krispii.core.repositories

import java.awt.Color
import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.group.Exam
import ca.shiftfocus.krispii.core.models.user.User
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
      new Color(row("color").asInstanceOf[Int]),
      row("slug").asInstanceOf[String],
      Option(row("orig_rubric_id").asInstanceOf[UUID]) match {
        case Some(origRubricId) => Some(origRubricId)
        case _ => None
      },
      row("enabled").asInstanceOf[Boolean],
      row("scheduling_enabled").asInstanceOf[Boolean],
      row("archived").asInstanceOf[Boolean],
      row("deleted").asInstanceOf[Boolean],
      None, // teams
      None, // tests
      row("created_at").asInstanceOf[DateTime],
      row("updated_at").asInstanceOf[DateTime]
    )
  }

  val Table: String = "exams"
  val Fields: String = "id, version, coordinator_id, name, color, slug, orig_rubric_id, enabled, scheduling_enabled, archived, deleted, created_at, updated_at"
  val FieldsWithTable: String = Fields.split(", ").map({ field => s"$Table." + field }).mkString(", ")
  val OrderBy: String = s"$Table.name ASC"

  // User CRUD operations
  val SelectAll: String =
    s"""
       |SELECT $Fields
       |FROM $Table
       |ORDER BY $OrderBy
  """.stripMargin

  val SelectOne: String =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE id = ?
       |LIMIT 1
     """.stripMargin

  val SelectOneBySlug: String =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE slug = ?
       |LIMIT 1
     """.stripMargin

  // Using here get_slug custom postgres function to generate unique slug if slug already exists
  val Insert: String = {
    s"""
       |INSERT INTO $Table ($Fields)
       |VALUES (?, ?, ?, ?, ?, get_slug(?, '$Table', ?), ?, ?, ?, false, false, ?, ?)
       |RETURNING $Fields
    """.stripMargin
  }

  // Using here get_slug custom postgres function to generate unique slug if slug already exists
  val Update: String =
    s"""
       |UPDATE $Table
       |SET version = ?, coordinator_id = ?, name = ?, color = ?, slug = get_slug(?, '$Table', ?),
       |    orig_rubric_id = ?, enabled = ?, scheduling_enabled = ?, archived = ?, updated_at = ?
       |WHERE id = ?
       |  AND version = ?
       |RETURNING $Fields
     """.stripMargin

  val Delete: String =
    s"""
       |UPDATE $Table
       |SET deleted = true
       |WHERE id = ?
       |RETURNING $Fields
     """.stripMargin

  val ListByCoordinatorId: String =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE coordinator_id = ?
       |  AND NOT $Table.deleted
       |ORDER BY $OrderBy
     """.stripMargin

  val ListByScorerId: String =
    s"""
       |SELECT $FieldsWithTable
       |FROM $Table, teams t, teams_scorers ts
       |WHERE ts.scorer_id = ?
       |  AND ts.team_id = t.id
       |  AND t.exam_id = $Table.id
       |  AND NOT $Table.deleted
       |  AND NOT $Table.archived
       |  AND NOT ts.deleted
       |  AND NOT ts.archived
       |ORDER BY $OrderBy
     """.stripMargin

  /**
   * List all exams.
   * Will only be called from API after check for administrator privileges!
   * @return an array of Exams
   */
  override def list(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Exam]]] = {
    queryList(SelectAll)
  }

  /**
   * Select exams based on the given user.
   *
   * @param user the coordinator to search by
   * @param isScorer: whether to treat the user as scorer or coordinator
   * @return the found exams or an error
   */
  override def list(user: User, isScorer: Boolean) // format: OFF
                   (implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Exam]]] = { // format: ON

    // val key = if (isScorer) cacheScorerExamsKey(user.id) else cacheCoordinatorExamsKey(user.id)
    val query = if (isScorer) ListByScorerId else ListByCoordinatorId
    /* cacheRepository.cacheSeqExam.getCached(key).flatMap {
      case \/-(examList) => Future successful \/-(examList)
      case -\/(_: RepositoryError.NoResults) => for {
        examList <- lift( */ queryList(query, Seq[Any](user.id)) /*)
        _ <- lift(cacheRepository.cacheSeqExam.putCache(key)(examList, ttl))
      } yield examList
      case -\/(error) => Future successful -\/(error)
    } */
  }
  /**
   * Find a single exam by ID.
   *
   * @param id the 128-bit UUID, as a byte array, to search for.
   * @return the found exam or an error
   */
  override def find(id: UUID)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Exam]] = {
    val directKey = cacheExamKey(id)
    cacheRepository.cacheExam.getCached(directKey).flatMap {
      case \/-(exam) => Future successful \/-(exam)
      case -\/(_: RepositoryError.NoResults) =>
        for {
          exam <- lift(queryOne(SelectOne, Array[Any](id)))
          _ <- lift(cacheRepository.cacheExam.putCache(directKey)(exam, ttl))
          _ <- lift(cacheRepository.cacheUUID.putCache(cacheExamSlugKey(exam.slug))(exam.id, ttl))
        } yield exam
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Find a single exam by slug.
   *
   * @param slug the exam's slug (unambiguous name compatible with URLs)
   * @return the found exam or an error
   */
  override def find(slug: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Exam]] = {
    val key = cacheExamSlugKey(slug)
    cacheRepository.cacheUUID.getCached(key).flatMap {
      case \/-(examId) =>
        find(examId) // will already read from and place the full exam into cache
      case -\/(_: RepositoryError.NoResults) =>
        for {
          exam <- lift(queryOne(SelectOneBySlug, Seq[Any](slug)))
          _ <- lift(cacheRepository.cacheUUID.putCache(key)(exam.id, ttl))
          _ <- lift(cacheRepository.cacheExam.putCache(cacheExamKey(exam.id))(exam, ttl))
        } yield exam
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Insert an Exam row.
   *
   * @param exam Exam to insert
   * @param conn implicit database connection
   * @return
   */
  def insert(exam: Exam)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Exam]] = {
    val params = Seq[Any](
      exam.id, 1, exam.ownerId, exam.name, exam.color.getRGB, exam.slug, exam.id,
      exam.origRubricId, exam.enabled, exam.schedulingEnabled, new DateTime, new DateTime
    )
    for {
      inserted <- lift(queryOne(Insert, params))
      // _ <- lift(cacheRepository.cacheSeqExam.removeCached(cacheCoordinatorExamsKey(exam.ownerId)))
    } yield inserted
  }

  /**
   * Update an Exam row
   *
   * @param exam Exam to update
   * @param conn implicit database connection
   * @return
   */
  override def update(exam: Exam) // format: OFF
                     (implicit conn: Connection): Future[\/[RepositoryError.Fail, Exam]] = { // format: ON
    val params = Seq[Any](
      exam.version + 1, exam.ownerId, exam.name, exam.color.getRGB, exam.slug, exam.id,
      exam.origRubricId, exam.enabled, exam.schedulingEnabled, exam.archived, new DateTime, exam.id, exam.version
    )
    for {
      updated <- lift(queryOne(Update, params))
      _ <- lift(cacheRepository.cacheUUID.removeCached(cacheExamSlugKey(updated.slug)))
      _ <- lift(cacheRepository.cacheExam.removeCached(cacheExamKey(updated.id)))
      // _ <- lift(cacheRepository.cacheSeqExam.removeCached(cacheCoordinatorExamsKey(updated.ownerId)))
    } yield updated
  }

  /**
   * Delete an Exam row.
   *
   * @param exam Exam to delete
   * @param conn implicit database connection
   * @return
   */
  override def delete(exam: Exam)  // format: OFF
                     (implicit conn: Connection): Future[\/[RepositoryError.Fail, Exam]] = { // format: ON
    for {
      deleted <- lift(queryOne(Delete, Seq[Any](exam.id)))
      _ <- lift(cacheRepository.cacheUUID.removeCached(cacheExamSlugKey(deleted.slug)))
      _ <- lift(cacheRepository.cacheExam.removeCached(cacheExamKey(deleted.id)))
      // _ <- lift(cacheRepository.cacheSeqExam.removeCached(cacheCoordinatorExamsKey(deleted.ownerId)))
    } yield deleted
  }

}
