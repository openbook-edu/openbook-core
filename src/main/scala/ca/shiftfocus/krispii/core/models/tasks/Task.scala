package ca.shiftfocus.krispii.core.models.tasks

import java.util.UUID
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
  val createdAt: DateTime
  val updatedAt: DateTime
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
  def apply(id: UUID = UUID.randomUUID,
             partId: UUID,
             position: Int,
             version: Long = 1L,
             settings: CommonTaskSettings = CommonTaskSettings(),
             taskType: Int,
             createdAt: DateTime = new DateTime,
             updatedAt: DateTime = new DateTime): Task =
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
   * A writer to serialize Tasks to JSON by detecting task type and calling
   * the appropriate writer.
   */
  implicit val jsonWrites = new Writes[Task] {
    def writes(aTask: Task): JsValue = aTask match {
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
