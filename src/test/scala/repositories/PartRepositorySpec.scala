import java.io.File

import ca.shiftfocus.krispii.core.models.Part
import ca.shiftfocus.krispii.core.repositories.{TaskRepositoryComponent, PartRepositoryPostgresComponent}
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
import grizzled.slf4j.Logger
import org.scalamock.scalatest.MockFactory
import org.scalatest.{MustMatchers, WordSpec, BeforeAndAfterAll, Suite}
import org.scalatest._
import Matchers._
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration

trait PartRepoTestEnvironment
  extends PartRepositoryPostgresComponent
  with TaskRepositoryComponent
  with Suite
  with BeforeAndAfterAll
  with MustMatchers
  with MockFactory
  with PostgresDB {

  /* START MOCK */
  val taskRepository = stub[TaskRepository]
  /* END MOCK */

  val logger = Logger[this.type]

  implicit val connection = db.pool

  val project_path = new File(".").getAbsolutePath()
  val create_schema_path = s"${project_path}/src/test/resources/schemas/create_schema.sql"
  val drop_schema_path = s"${project_path}/src/test/resources/schemas/drop_schema.sql"
  val data_schema_path = s"${project_path}/src/test/resources/schemas/data_schema.sql"

  /**
   * Implements query from schema file
   * @param path Path to schema file
   */
  def load_schema(path: String): Unit = {
    val sql_schema_file = scala.io.Source.fromFile(path)
    val query = sql_schema_file.getLines().mkString
    sql_schema_file.close()
    val result = db.pool.sendQuery(query)
    Await.result(result, Duration.Inf)
  }

  // Before test
  override def beforeAll(): Unit = {
    // DROP tables
    load_schema(drop_schema_path)
    // CREATE tables
    load_schema(create_schema_path)
    // Insert data into tables
    load_schema(data_schema_path)
  }

  // After test
  override def afterAll(): Unit = {
    // DROP tables
    load_schema(drop_schema_path)
  }
}

class PartRepositorySpec
  extends WordSpec
  with PartRepoTestEnvironment {

  // TODO - check parts_components primary_key
  "PartRepository.list" should {
    inSequence {
      "find all parts" in {
        // Put here parts = Vector(), because after db query Project object is created without parts.
        (taskRepository.list(_: Part)) when(TestValues.testPartA.copy(tasks = Vector())) returns(Future.successful(Vector(TestValues.testLongAnswerTaskA, TestValues.testShortAnswerTaskB, TestValues.testMultipleChoiceTaskC)))
        (taskRepository.list(_: Part)) when(TestValues.testPartB.copy(tasks = Vector())) returns(Future.successful(Vector(TestValues.testOrderingTaskD, TestValues.testMatchingTaskE)))
        (taskRepository.list(_: Part)) when(TestValues.testPartC.copy(tasks = Vector())) returns(Future.successful(Vector()))

        val result = partRepository.list

        val parts = Await.result(result, Duration.Inf)

        parts.toString() should be (Vector(TestValues.testPartA, TestValues.testPartB, TestValues.testPartC).toString())

        Map[Int, Part](0 -> TestValues.testPartA, 1 -> TestValues.testPartB, 2 -> TestValues.testPartC).foreach {
          case (key, part: Part) => {
            parts(key).id should be(part.id)
            parts(key).version should be(part.version)
            parts(key).projectId should be(part.projectId)
            parts(key).name should be(part.name)
            parts(key).enabled should be(part.enabled)
            parts(key).position should be(part.position)
            parts(key).tasks should be(part.tasks)
            parts(key).createdAt.toString should be(part.createdAt.toString)
            parts(key).updatedAt.toString should be(part.updatedAt.toString)
          }
        }
      }
      "find all Parts belonging to a given Project" in {
        // Put here parts = Vector(), because after db query Project object is created without parts.
        (taskRepository.list(_: Part)) when(TestValues.testPartA.copy(tasks = Vector())) returns(Future.successful(Vector(TestValues.testLongAnswerTaskA, TestValues.testShortAnswerTaskB, TestValues.testMultipleChoiceTaskC)))
        (taskRepository.list(_: Part)) when(TestValues.testPartB.copy(tasks = Vector())) returns(Future.successful(Vector(TestValues.testOrderingTaskD, TestValues.testMatchingTaskE)))

        val result = partRepository.list(TestValues.testProjectA)

        val parts = Await.result(result, Duration.Inf)

        parts.toString() should be (Vector(TestValues.testPartA, TestValues.testPartB).toString())

        Map[Int, Part](0 -> TestValues.testPartA, 1 -> TestValues.testPartB).foreach {
          case (key, part: Part) => {
            parts(key).id should be(part.id)
            parts(key).version should be(part.version)
            parts(key).projectId should be(part.projectId)
            parts(key).name should be(part.name)
            parts(key).enabled should be(part.enabled)
            parts(key).position should be(part.position)
            parts(key).tasks should be(part.tasks)
            parts(key).createdAt.toString should be(part.createdAt.toString)
            parts(key).updatedAt.toString should be(part.updatedAt.toString)
          }
        }
      }
      "find all Parts belonging to a given Component" in {
        // Put here parts = Vector(), because after db query Project object is created without parts.
        (taskRepository.list(_: Part)) when(TestValues.testPartA.copy(tasks = Vector())) returns(Future.successful(Vector(TestValues.testLongAnswerTaskA, TestValues.testShortAnswerTaskB, TestValues.testMultipleChoiceTaskC)))
        (taskRepository.list(_: Part)) when(TestValues.testPartB.copy(tasks = Vector())) returns(Future.successful(Vector(TestValues.testOrderingTaskD, TestValues.testMatchingTaskE)))

        val result = partRepository.list(TestValues.testTextComponentA)

        val parts = Await.result(result, Duration.Inf)

        parts.toString() should be (Vector(TestValues.testPartA, TestValues.testPartB).toString())

        Map[Int, Part](0 -> TestValues.testPartA, 1 -> TestValues.testPartB).foreach {
          case (key, part: Part) => {
            parts(key).id should be(part.id)
            parts(key).version should be(part.version)
            parts(key).projectId should be(part.projectId)
            parts(key).name should be(part.name)
            parts(key).enabled should be(part.enabled)
            parts(key).position should be(part.position)
            parts(key).tasks should be(part.tasks)
            parts(key).createdAt.toString should be(part.createdAt.toString)
            parts(key).updatedAt.toString should be(part.updatedAt.toString)
          }
        }
      }
    }
  }

  "PartRepository.find" should {
    inSequence {
      "find a single entry by ID" in {
        // Put here parts = Vector(), because after db query Project object is created without parts.
        (taskRepository.list(_: Part)) when(TestValues.testPartA.copy(tasks = Vector())) returns(Future.successful(Vector(TestValues.testLongAnswerTaskA, TestValues.testShortAnswerTaskB, TestValues.testMultipleChoiceTaskC)))

        val result = partRepository.find(TestValues.testPartA.id).map(_.get)
        val part = Await.result(result, Duration.Inf)

        part.id should be(TestValues.testPartA.id)
        part.version should be(TestValues.testPartA.version)
        part.projectId should be(TestValues.testPartA.projectId)
        part.name should be(TestValues.testPartA.name)
        part.enabled should be(TestValues.testPartA.enabled)
        part.position should be(TestValues.testPartA.position)
        part.tasks should be(TestValues.testPartA.tasks)
        part.createdAt.toString should be(TestValues.testPartA.createdAt.toString)
        part.updatedAt.toString should be(TestValues.testPartA.updatedAt.toString)
      }
      "find a single entry by its position within a project" in {
        // Put here parts = Vector(), because after db query Project object is created without parts.
        (taskRepository.list(_: Part)) when(TestValues.testPartA.copy(tasks = Vector())) returns(Future.successful(Vector(TestValues.testLongAnswerTaskA, TestValues.testShortAnswerTaskB, TestValues.testMultipleChoiceTaskC)))

        val result = partRepository.find(TestValues.testProjectA, 10).map(_.get)
        val part = Await.result(result, Duration.Inf)

        part.id should be(TestValues.testPartA.id)
        part.version should be(TestValues.testPartA.version)
        part.projectId should be(TestValues.testPartA.projectId)
        part.name should be(TestValues.testPartA.name)
        part.enabled should be(TestValues.testPartA.enabled)
        part.position should be(TestValues.testPartA.position)
        part.tasks should be(TestValues.testPartA.tasks)
        part.createdAt.toString should be(TestValues.testPartA.createdAt.toString)
        part.updatedAt.toString should be(TestValues.testPartA.updatedAt.toString)
      }
    }
  }

  "PartRepository.insert" should {
    inSequence {
      "save a Part row" in {
        val result = partRepository.insert(TestValues.testPartD)
        val part = Await.result(result, Duration.Inf)

        part.id should be(TestValues.testPartD.id)
        part.version should be(1L)
        part.projectId should be(TestValues.testPartD.projectId)
        part.name should be(TestValues.testPartD.name)
        part.enabled should be(true)
        part.position should be(0)
        part.tasks should be(Vector())
        part.createdAt.toString should be(TestValues.testPartD.createdAt.toString)
        part.updatedAt.toString should be(TestValues.testPartD.updatedAt.toString)
      }
    }
  }
}