import java.io.File
import ca.shiftfocus.krispii.core.models.SectionScheduleException
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.RowData
import org.joda.time.{DateTimeZone, DateTime, LocalTime, LocalDate}

import scala.concurrent.ExecutionContext.Implicits.global
import ca.shiftfocus.krispii.core.repositories.ClassScheduleExceptionRepositoryPostgresComponent
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
import grizzled.slf4j.Logger
import org.scalamock.scalatest.MockFactory
import org.scalatest.{MustMatchers, WordSpec, BeforeAndAfterAll, Suite}
import org.scalatest._
import org.scalatest.Matchers._

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

  val SelectOne = """
     SELECT *
     FROM class_schedule_exceptions
     WHERE id = ?
                  """
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
      "return empty Vector if we have unexisting class for all schedule exceptions" in {
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
      "be NONE if entry wasn't found by ID" in {
        val result = sectionScheduleExceptionRepository.find(UUID("f9aadc67-5e8b-48f3-b0a2-20a0d7d88477"))

        Await.result(result, Duration.Inf) should be (None)
      }
    }
  }

  // TODO insert with unexisting user id, class id
  "ClassScheduleExceptionRepository.insert" should {
    inSequence {
      "create a new section schedule exception" in {
        val result = sectionScheduleExceptionRepository.insert(TestValues.testSectionScheduleExceptionE)

        val newSectionScheduleException = Await.result(result, Duration.Inf)

        newSectionScheduleException.id should be(TestValues.testSectionScheduleExceptionE.id)
        newSectionScheduleException.userId should be(TestValues.testSectionScheduleExceptionE.userId)
        newSectionScheduleException.classId should be(TestValues.testSectionScheduleExceptionE.classId)
        newSectionScheduleException.version should be(1L)
        newSectionScheduleException.day should be(TestValues.testSectionScheduleExceptionE.day)
        newSectionScheduleException.startTime should be(TestValues.testSectionScheduleExceptionE.startTime)
        newSectionScheduleException.endTime should be(TestValues.testSectionScheduleExceptionE.endTime)

        // Check
        val queryResult = db.pool.sendPreparedStatement(SelectOne, Array[Any](TestValues.testSectionScheduleExceptionE.id.bytes)).map { queryResult =>
          val sectionScheduleExceptionRepositoryList = queryResult.rows.get.map {
            item: RowData => SectionScheduleException(item)
          }
          sectionScheduleExceptionRepositoryList
        }

        val sectionScheduleExceptionRepositoryList = Await.result(queryResult, Duration.Inf)

        sectionScheduleExceptionRepositoryList(0).id should be(TestValues.testSectionScheduleExceptionE.id)
        sectionScheduleExceptionRepositoryList(0).userId should be(TestValues.testSectionScheduleExceptionE.userId)
        sectionScheduleExceptionRepositoryList(0).classId should be(TestValues.testSectionScheduleExceptionE.classId)
        sectionScheduleExceptionRepositoryList(0).version should be(1L)
        sectionScheduleExceptionRepositoryList(0).day should be(TestValues.testSectionScheduleExceptionE.day)
        sectionScheduleExceptionRepositoryList(0).startTime should be(TestValues.testSectionScheduleExceptionE.startTime)
        sectionScheduleExceptionRepositoryList(0).endTime should be(TestValues.testSectionScheduleExceptionE.endTime)
      }
      "throw a GenericDatabaseException if class already exists" in {
        val result = sectionScheduleExceptionRepository.insert(TestValues.testSectionScheduleExceptionA)

        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
      }
    }
  }

  "ClassScheduleExceptionRepository.update"  + Console.RED + Console.BOLD + " (NOTE: Please check Javadoc for this method) " + Console.RESET should {
    inSequence {
      "update section schedule exception" in {
        val result = sectionScheduleExceptionRepository.update(TestValues.testSectionScheduleExceptionC.copy(
          userId = UUID("36c8c0ca-50aa-4806-afa5-916a5e33a81f"), // User E -> User A
          classId = UUID("217c5622-ff9e-4372-8e6a-95fb3bae300b"), // Class B -> Class A
          day = new LocalDate(2014, 8, 8),
          startTime = new LocalTime(12, 1, 19),
          endTime = new LocalTime(13, 1, 19)
        ))

        val sectionScheduleException = Await.result(result, Duration.Inf)

        sectionScheduleException.id should be(TestValues.testSectionScheduleExceptionC.id)
        sectionScheduleException.userId should be(UUID("36c8c0ca-50aa-4806-afa5-916a5e33a81f"))
        sectionScheduleException.classId should be(UUID("217c5622-ff9e-4372-8e6a-95fb3bae300b"))
        sectionScheduleException.version should be(TestValues.testSectionScheduleExceptionC.version + 1)
        sectionScheduleException.day should be(new LocalDate(2014, 8, 8))
        sectionScheduleException.startTime should be(new LocalTime(12, 1, 19))
        sectionScheduleException.endTime should be(new LocalTime(13, 1, 19))

        // Check
        val queryResult = db.pool.sendPreparedStatement(SelectOne, Array[Any](TestValues.testSectionScheduleExceptionC.id.bytes)).map { queryResult =>
          val sectionScheduleExceptionRepositoryList = queryResult.rows.get.map {
            item: RowData => SectionScheduleException(item)
          }
          sectionScheduleExceptionRepositoryList
        }

        val sectionScheduleExceptionRepositoryList = Await.result(queryResult, Duration.Inf)

        sectionScheduleExceptionRepositoryList(0).id should be(TestValues.testSectionScheduleExceptionC.id)
        sectionScheduleExceptionRepositoryList(0).userId should be(UUID("36c8c0ca-50aa-4806-afa5-916a5e33a81f"))
        sectionScheduleExceptionRepositoryList(0).classId should be(UUID("217c5622-ff9e-4372-8e6a-95fb3bae300b"))
        sectionScheduleExceptionRepositoryList(0).version should be(TestValues.testSectionScheduleExceptionC.version + 1)
        sectionScheduleExceptionRepositoryList(0).day should be(new LocalDate(2014, 8, 8))
        sectionScheduleExceptionRepositoryList(0).startTime should be(new LocalTime(12, 1, 19))
        sectionScheduleExceptionRepositoryList(0).endTime should be(new LocalTime(13, 1, 19))
      }
      "throw a NoSuchElementException when update an existing sectionScheduleException with wrong version" in {
        val result = sectionScheduleExceptionRepository.update(TestValues.testSectionScheduleExceptionC.copy(
          version = 99L,
          userId = UUID("36c8c0ca-50aa-4806-afa5-916a5e33a81f"), // User E -> User A
          classId = UUID("217c5622-ff9e-4372-8e6a-95fb3bae300b"), // Class B -> Class A
          day = new LocalDate(2014, 8, 8),
          startTime = new LocalTime(12, 1, 19),
          endTime = new LocalTime(13, 1, 19)
        ))

        an[java.util.NoSuchElementException] should be thrownBy Await.result(result, Duration.Inf)
      }
      "throw a NoSuchElementException when update an unexisting sectionScheduleException" in {
        val result = sectionScheduleExceptionRepository.update(SectionScheduleException(
          userId = UUID("36c8c0ca-50aa-4806-afa5-916a5e33a81f"), // User E -> User A
          classId = UUID("217c5622-ff9e-4372-8e6a-95fb3bae300b"), // Class B -> Class A
          day = new LocalDate(2014, 8, 8),
          startTime = new LocalTime(12, 1, 19),
          endTime = new LocalTime(13, 1, 19)
        ))

        an[java.util.NoSuchElementException] should be thrownBy Await.result(result, Duration.Inf)
      }
    }
  }

  "ClassScheduleExceptionRepository.delete"  + Console.RED + Console.BOLD + " (NOTE: Please check Javadoc for this method) " + Console.RESET should {
    inSequence {
      "delete section schedule exception" in {
        val result = sectionScheduleExceptionRepository.delete(TestValues.testSectionScheduleExceptionA)

        Await.result(result, Duration.Inf) should be (true)

        // Check
        val queryResult = db.pool.sendPreparedStatement(SelectOne, Array[Any](TestValues.testSectionScheduleExceptionA.id.bytes)).map { queryResult =>
          val sectionScheduleExceptionRepositoryList = queryResult.rows.get.map {
            item: RowData => SectionScheduleException(item)
          }
          sectionScheduleExceptionRepositoryList
        }

        Await.result(queryResult, Duration.Inf) should be(Vector())
      }
      "return FALSE if section schedule exception hasn't been found" in {
        val result = sectionScheduleExceptionRepository.delete(SectionScheduleException(
          userId = UUID("36c8c0ca-50aa-4806-afa5-916a5e33a81f"), // User E -> User A
          classId = UUID("217c5622-ff9e-4372-8e6a-95fb3bae300b"), // Class B -> Class A
          day = new LocalDate(2014, 8, 8),
          startTime = new LocalTime(12, 1, 19),
          endTime = new LocalTime(13, 1, 19)
        ))

        Await.result(result, Duration.Inf) should be(false)
      }
    }
  }
}
