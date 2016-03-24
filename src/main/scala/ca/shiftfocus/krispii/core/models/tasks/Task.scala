package ca.shiftfocus.krispii.core.models.tasks

import java.util.UUID
import ca.shiftfocus.krispii.core.models.tasks.questions.Question
import ca.shiftfocus.krispii.core.models.{ VideoComponent, TextComponent, AudioComponent }
import com.github.mauricio.async.db.RowData
import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * The supertype for tasks. A task is identified by its position
 * within a part, which is in turn identifed by its position within
 * a project.
 *
 */
sealed trait Task {
  val id: UUID
  val partId: UUID
  val position: Int
  val version: Long
  val settings: CommonTaskSettings
  val taskType: Int
  val createdAt: DateTime
  val updatedAt: DateTime
}

object Task {

  /*
   * Defining the available task types.
   */
  val Document = 0
  val Question = 1
  val Media = 2

  // Media type
  val AnyMedia = 0
  val Audio = 1
  val Video = 2
  val Image = 3
  /**
   * An apply method that allows instantiation of empty tasks.
   *
   * Supply the correct task type and it will return an instance of that type. Useful
   * for creating a task when you're not sure what type will be created.
   *
   * @param id
   * @param partId
   * @param position
   * @param version
   * @param settings
   * @param taskType
   * @param createdAt
   * @param updatedAt
   * @return
   */
  def apply(
    id: UUID = UUID.randomUUID,
    partId: UUID,
    position: Int,
    version: Long = 1L,
    settings: CommonTaskSettings = CommonTaskSettings(),
    taskType: Int,
    createdAt: DateTime = new DateTime,
    updatedAt: DateTime = new DateTime
  ): Task =
    {
      val newTask = taskType match {
        case Task.Document => DocumentTask(
          id = id,
          partId = partId,
          position = position,
          version = version,
          settings = settings,
          createdAt = createdAt,
          updatedAt = updatedAt
        )
        case Task.Question => QuestionTask(
          id = id,
          partId = partId,
          position = position,
          version = version,
          settings = settings,
          questions = IndexedSeq(),
          createdAt = createdAt,
          updatedAt = updatedAt
        )
        case Task.Media => MediaTask(
          id = id,
          partId = partId,
          position = position,
          version = version,
          settings = settings,
          mediaType = AnyMedia,
          createdAt = createdAt,
          updatedAt = updatedAt
        )
      }
      newTask
    }

  /**
   * A writer to serialize Tasks to JSON by detecting task type and calling
   * the appropriate writer.
   */
  implicit val jsonWrites = new Writes[Task] {
    def writes(aTask: Task): JsValue = aTask match {
      case task: DocumentTask => Json.toJson(task).as[JsObject].deepMerge(Json.obj(
        "taskType" -> Document
      ))
      case task: QuestionTask => Json.toJson(task).as[JsObject].deepMerge(Json.obj(
        "taskType" -> Question
      ))
      case task: MediaTask => Json.toJson(task).as[JsObject].deepMerge(Json.obj(
        "taskType" -> Media
      ))
    }
  }
}

/**
 * A long answer task is one in which the student is expected to write more than
 * a few words in response. This task type gives them free reign to enter as much
 * text as they desire.
 *
 * @param id The task's UUID.
 * @param partId The part to which this task belongs.
 * @param position The order in the part in which this task falls.
 * @param version The version of the task entity, for offline locking.
 * @param settings An object containing common settings for tasks.
 * @param createdAt When the entity was created.
 * @param updatedAt When the entity was last updated.
 */
final case class DocumentTask(
    // Primary Key
    id: UUID = UUID.randomUUID,
    // Combination must be unique
    partId: UUID,
    position: Int,
    // Additional data
    version: Long = 1L,
    settings: CommonTaskSettings = CommonTaskSettings(),
    dependencyId: Option[UUID] = None,
    createdAt: DateTime = new DateTime,
    updatedAt: DateTime = new DateTime
) extends Task {

  /**
   * Which type of task this is. Hard-coded value per class!
   */
  override val taskType: Int = Task.Document

  override def equals(other: Any): Boolean = {
    other match {
      case otherLongAnswerTask: DocumentTask => {
        this.id == otherLongAnswerTask.id &&
          this.partId == otherLongAnswerTask.partId &&
          this.position == otherLongAnswerTask.position &&
          this.version == otherLongAnswerTask.version &&
          this.settings.toString == otherLongAnswerTask.settings.toString &&
          this.dependencyId == otherLongAnswerTask.dependencyId
      }
      case _ => false
    }
  }

  override def hashCode: Int = 41 * this.id.hashCode
}

object DocumentTask {
  /**
   * Serialize a LongAnswerTask to JSON.
   */
  implicit val taskWrites: Writes[DocumentTask] = (
    (__ \ "id").write[UUID] and
    (__ \ "partId").write[UUID] and
    (__ \ "position").write[Int] and
    (__ \ "version").write[Long] and
    (__ \ "settings").write[CommonTaskSettings] and
    (__ \ "dependencyId").writeNullable[UUID] and
    (__ \ "createdAt").write[DateTime] and
    (__ \ "updatedAt").write[DateTime]
  )(unlift(DocumentTask.unapply))
}

/**
 * Question Tasks are tasks comprising several small questions. Short answers, fill-in-the-blanks, multiple
 * choice, etc.
 *
 * @param id
 * @param partId
 * @param position
 * @param version
 * @param settings
 * @param questions
 * @param createdAt
 * @param updatedAt
 */
final case class QuestionTask(
    // Primary Key
    id: UUID = UUID.randomUUID,
    // Combination must be unique
    partId: UUID,
    position: Int,
    // Additional data
    version: Long = 1L,
    settings: CommonTaskSettings = CommonTaskSettings(),
    questions: IndexedSeq[Question],
    createdAt: DateTime = new DateTime(),
    updatedAt: DateTime = new DateTime()
) extends Task {
  override val taskType = Task.Question

  override def equals(other: Any): Boolean = {
    other match {
      case otherLongAnswerTask: QuestionTask => {
        this.id == otherLongAnswerTask.id &&
          this.partId == otherLongAnswerTask.partId &&
          this.position == otherLongAnswerTask.position &&
          this.version == otherLongAnswerTask.version &&
          this.settings.toString == otherLongAnswerTask.settings.toString &&
          this.questions.toString == otherLongAnswerTask.questions.toString
      }
      case _ => false
    }
  }
}

object QuestionTask {
  implicit val taskReads: Reads[QuestionTask] = (
    (__ \ "id").read[UUID] and
    (__ \ "partId").read[UUID] and
    (__ \ "position").read[Int] and
    (__ \ "version").read[Long] and
    (__ \ "settings").read[CommonTaskSettings] and
    (__ \ "questions").read[IndexedSeq[Question]] and
    (__ \ "createdAt").read[DateTime] and
    (__ \ "updatedAt").read[DateTime]
  )(QuestionTask.apply _)

  implicit val taskWrites: Writes[QuestionTask] = (
    (__ \ "id").write[UUID] and
    (__ \ "partId").write[UUID] and
    (__ \ "position").write[Int] and
    (__ \ "version").write[Long] and
    (__ \ "settings").write[CommonTaskSettings] and
    (__ \ "questions").write[IndexedSeq[Question]] and
    (__ \ "createdAt").write[DateTime] and
    (__ \ "updatedAt").write[DateTime]
  )(unlift(QuestionTask.unapply))
}

final case class MediaTask(
    // Primary Key
    id: UUID = UUID.randomUUID,
    // Combination must be unique
    partId: UUID,
    position: Int,
    // Additional data
    version: Long = 1L,
    settings: CommonTaskSettings = CommonTaskSettings(),
    mediaType: Int,
    createdAt: DateTime = new DateTime,
    updatedAt: DateTime = new DateTime
) extends Task {

  /**
   * Which type of task this is. Hard-coded value per class!
   */
  override val taskType: Int = Task.Media

  override def equals(other: Any): Boolean = {
    other match {
      case otherMediaTask: MediaTask => {
        this.id == otherMediaTask.id &&
          this.partId == otherMediaTask.partId &&
          this.position == otherMediaTask.position &&
          this.version == otherMediaTask.version &&
          this.settings.toString == otherMediaTask.settings.toString &&
          this.mediaType == otherMediaTask.mediaType
      }
      case _ => false
    }
  }

  override def hashCode: Int = 41 * this.id.hashCode
}

object MediaTask {
  /**
   * Serialize a MediaTask to JSON.
   */
  implicit val taskWrites: Writes[MediaTask] = (
    (__ \ "id").write[UUID] and
    (__ \ "partId").write[UUID] and
    (__ \ "position").write[Int] and
    (__ \ "version").write[Long] and
    (__ \ "settings").write[CommonTaskSettings] and
    (__ \ "mediaType").write[Int] and
    (__ \ "createdAt").write[DateTime] and
    (__ \ "updatedAt").write[DateTime]
  )(unlift(MediaTask.unapply))
}
