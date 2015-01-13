import java.io.File
import scala.concurrent.ExecutionContext.Implicits.global
import ca.shiftfocus.krispii.core.repositories.ClassScheduleExceptionRepositoryPostgresComponent
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
import grizzled.slf4j.Logger
import org.scalamock.scalatest.MockFactory
import org.scalatest.{MustMatchers, WordSpec, BeforeAndAfterAll, Suite}
import org.scalatest._
import Matchers._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

trait ClassScheduleExceptionRepoTestEnvironment
  extends ClassScheduleExceptionRepositoryPostgresComponent
  with Suite
  with BeforeAndAfterAll
  with PostgresDB {

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

class ClassScheduleExceptionRepositorySpec
  extends WordSpec
  with MustMatchers
  with MockFactory
  with ClassScheduleExceptionRepoTestEnvironment {

  "ClassScheduleExceptionRepository.list" should {
    inSequence {
      "find all scheduling exceptions for one student in one section" in {
        val result = sectionScheduleExceptionRepository.list(TestValues.testUserA, TestValues.testClassA)

        val sectionScheduleException = Await.result(result, Duration.Inf)
        sectionScheduleException.toString should be(Vector(TestValues.testSectionScheduleExceptionA).toString)
      }
      "return empty Vector if unexisting user" in {
        val result = sectionScheduleExceptionRepository.list(TestValues.testUserD, TestValues.testClassA)

        Await.result(result, Duration.Inf) should be(Vector())
      }
      "return empty Vector if unexisting class" in {
        val result = sectionScheduleExceptionRepository.list(TestValues.testUserA, TestValues.testClassD)

        Await.result(result, Duration.Inf) should be(Vector())
      }
      "find all schedule exceptions for a given section" in {
        val result = sectionScheduleExceptionRepository.list(TestValues.testClassB)

        val sectionScheduleException = Await.result(result, Duration.Inf)
        sectionScheduleException.toString should be(Vector(TestValues.testSectionScheduleExceptionB, TestValues.testSectionScheduleExceptionC).toString)
      }
      "return empty Vector if unexisting class for all schedule exceptions" in {
        val result = sectionScheduleExceptionRepository.list(TestValues.testClassD)

        Await.result(result, Duration.Inf) should be(Vector())
      }

    }
  }

  "ClassScheduleExceptionRepository.find" should {
    inSequence {
      "find a single entry by ID" in {
        val result = sectionScheduleExceptionRepository.find(TestValues.testSectionScheduleExceptionB.id).map(_.get)

        val sectionScheduleException = Await.result(result, Duration.Inf)

        sectionScheduleException.id should be(TestValues.testSectionScheduleExceptionB.id)
        sectionScheduleException.userId should be(TestValues.testSectionScheduleExceptionB.userId)
        sectionScheduleException.classId should be(TestValues.testSectionScheduleExceptionB.classId)
        sectionScheduleException.version should be(TestValues.testSectionScheduleExceptionB.version)
        sectionScheduleException.day should be(TestValues.testSectionScheduleExceptionB.day)
        sectionScheduleException.startTime should be(TestValues.testSectionScheduleExceptionB.startTime)
        sectionScheduleException.endTime should be(TestValues.testSectionScheduleExceptionB.endTime)
        sectionScheduleException.createdAt.toString should be(TestValues.testSectionScheduleExceptionB.createdAt.toString)
        sectionScheduleException.updatedAt.toString should be(TestValues.testSectionScheduleExceptionB.updatedAt.toString)
      }
    }
  }
}
