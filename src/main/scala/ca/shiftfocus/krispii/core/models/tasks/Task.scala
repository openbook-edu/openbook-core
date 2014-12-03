package ca.shiftfocus.krispii.core.models.tasks

import ca.shiftfocus.uuid.UUID
import ca.shiftfocus.krispii.core.models.{VideoComponent, TextComponent, AudioComponent}
import com.github.mauricio.async.db.RowData
import org.joda.time.DateTime
import play.api.libs.json._

/**
 * The supertype for tasks. A task is identified by its position
 * within a part, which is in turn identifed by its position within
 * a project.
 *
 */
trait Task {
  val id: UUID
  val partId: UUID
  val position: Int
  val version: Long
  val settings: CommonTaskSettings
  val taskType: Int
  val createdAt: Option[DateTime]
  val updatedAt: Option[DateTime]
}

object Task {

  /*
   * Defining the available task types.
   */
  val LongAnswer = 0
  val ShortAnswer = 1
  val MultipleChoice = 2
  val Ordering = 3
  val Matching = 4

  val NotStarted = 0
  val Incomplete = 1
  val Complete = 2

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
  def apply(id: UUID = UUID.random,
             partId: UUID,
             position: Int,
             version: Long = 0,
             settings: CommonTaskSettings = CommonTaskSettings(),
             taskType: Int,
             createdAt: Option[DateTime] = None,
             updatedAt: Option[DateTime] = None): Task =
  {
    val newTask = taskType match {
      case Task.LongAnswer => LongAnswerTask(
        id = id,
        partId = partId,
        position = position,
        version = version,
        settings = settings,
        createdAt = createdAt,
        updatedAt = updatedAt
      )
      case Task.ShortAnswer => ShortAnswerTask(
        id = id,
        partId = partId,
        position = position,
        version = version,
        settings = settings,
        createdAt = createdAt,
        updatedAt = updatedAt
      )
      case Task.MultipleChoice => MultipleChoiceTask(
        id = id,
        partId = partId,
        position = position,
        version = version,
        settings = settings,
        createdAt = createdAt,
        updatedAt = updatedAt
      )
      case Task.Ordering => OrderingTask(
        id = id,
        partId = partId,
        position = position,
        version = version,
        settings = settings,
        createdAt = createdAt,
        updatedAt = updatedAt
      )
      case Task.Matching => MatchingTask(
        id = id,
        partId = partId,
        position = position,
        version = version,
        settings = settings,
        createdAt = createdAt,
        updatedAt = updatedAt
      )
    }
    newTask
  }

  /**
   * Build a [[Task]] object from a database row by reading which type
   * of task this is and calling the appropriate apply method.
   *
   * @param row a result row from a database SELECT or RETURNING query.
   * @return an instance of a class that implements [[Task]].
   */
  def apply(row: RowData): Task = {
    row("task_type").asInstanceOf[Int] match {
      case LongAnswer => LongAnswerTask(row)
      case ShortAnswer => ShortAnswerTask(row)
      case MultipleChoice => MultipleChoiceTask(row)
      case Ordering => OrderingTask(row)
      case Matching => MatchingTask(row)
      case _ => throw new Exception("Retrieved an unknown task type from the database. You dun messed up now!")
    }
  }

  /**
   * A reader to unserialize JSON to a Task object by detecting task
   * type and calling the appropriate reader.
   */
  implicit val jsonReads = new Reads[Task] {
    def reads(js: JsValue) = {
      (js \ "taskType").as[Int] match {
        case LongAnswer => LongAnswerTask.jsonReads.reads(js)
        case ShortAnswer => ShortAnswerTask.jsonReads.reads(js)
        case MultipleChoice => MultipleChoiceTask.jsonReads.reads(js)
        case Ordering => OrderingTask.jsonReads.reads(js)
        case Matching => MatchingTask.jsonReads.reads(js)
        case _ => JsError("Tried to read an invalid task type from JSON.")
      }
    }
  }

  /**
   * A writer to serialize Tasks to JSON by detecting task type and calling
   * the appropriate writer.
   */
  implicit val jsonWrites = new Writes[Task] {
    def writes(task: Task): JsValue = task match {
      case task: LongAnswerTask => Json.toJson(task).as[JsObject].deepMerge(Json.obj(
        "taskType" -> LongAnswer
      ))
      case task: ShortAnswerTask => Json.toJson(task).as[JsObject].deepMerge(Json.obj(
        "taskType" -> ShortAnswer
      ))
      case task: MultipleChoiceTask => Json.toJson(task).as[JsObject].deepMerge(Json.obj(
        "taskType" -> MultipleChoice
      ))
      case task: OrderingTask => Json.toJson(task).as[JsObject].deepMerge(Json.obj(
        "taskType" -> Ordering
      ))
      case task: MatchingTask => Json.toJson(task).as[JsObject].deepMerge(Json.obj(
        "taskType" -> Matching
      ))
      case _ => throw new Exception("Tried to write an invalid task. I'm not sure how you even did this, but you get a gold star. A gold star for failure.")
    }
  }
}