import java.io.File
import ca.shiftfocus.krispii.core.models.ClassSchedule
import ca.shiftfocus.krispii.core.repositories.ClassScheduleRepositoryPostgresComponent
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.RowData
import grizzled.slf4j.Logger
import org.joda.time.{LocalDate, LocalTime, DateTimeZone, DateTime}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{MustMatchers, WordSpec, BeforeAndAfterAll, Suite}
import org.scalatest._
import Matchers._
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Await
import scala.concurrent.duration.Duration

trait ClassScheduleRepoTestEnvironment
  extends ClassScheduleRepositoryPostgresComponent
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
//    load_schema(drop_schema_path)
  }

  val SelectOne = """
     SELECT *
     FROM class_schedules
     WHERE id = ?
                  """
}

class ClassScheduleRepositorySpec
  extends WordSpec
  with MustMatchers
  with MockFactory
  with ClassScheduleRepoTestEnvironment {

  // Find anything on indicated day at indicated time
  "ClassScheduleRepository.isAnythingScheduledForUser"   + Console.RED + Console.BOLD + " (NOTE: Please check Javadoc for this method) " + Console.RESET should {
    inSequence {
      "be TRUE if anything is scheduled for a user on indicated day and at indicated time" in {
        val result = sectionScheduleRepository.isAnythingScheduledForUser(TestValues.testUserA, TestValues.testClassScheduleA.day, TestValues.testClassScheduleA.startTime)

        val classSchedule = Await.result(result, Duration.Inf)

        classSchedule should be(true)
      }
      "be FALSE if nothing is scheduled for a user on indicated day and at indicated time" in {
        val result = sectionScheduleRepository.isAnythingScheduledForUser(TestValues.testUserA, (new DateTime(2000, 1, 15, 14, 1, 19, 545, DateTimeZone.forID("-04"))).toLocalDate(), (new DateTime(2015, 1, 15, 13, 38, 19, 545, DateTimeZone.forID("-04"))).toLocalTime())

        val classSchedule = Await.result(result, Duration.Inf)

        classSchedule should be(false)
      }
    }
  }

  "ClassScheduleRepository.isProjectScheduledForUser" + Console.RED + Console.BOLD + " (NOTE: Please check Javadoc for this method) " + Console.RESET should {
    inSequence {
      "be TRUE if project is scheduled for a user on indicated day and at indicated time" in {
        val result = sectionScheduleRepository.isProjectScheduledForUser(TestValues.testProjectA, TestValues.testUserA, TestValues.testClassScheduleA.day, TestValues.testClassScheduleA.startTime)

        val classSchedule = Await.result(result, Duration.Inf)

        classSchedule should be(true)
      }
      "be FALSE if class! is not scheduled on indicated day and at indicated time" in {
        val result = sectionScheduleRepository.isProjectScheduledForUser(TestValues.testProjectA, TestValues.testUserA, (new DateTime(2000, 1, 15, 14, 1, 19, 545, DateTimeZone.forID("-04"))).toLocalDate(), (new DateTime(2015, 1, 15, 13, 38, 19, 545, DateTimeZone.forID("-04"))).toLocalTime())

        val classSchedule = Await.result(result, Duration.Inf)

        classSchedule should be(false)
      }
      "be FALSE if project is from another class" in {
        val result = sectionScheduleRepository.isProjectScheduledForUser(TestValues.testProjectB, TestValues.testUserA, TestValues.testClassScheduleA.day, TestValues.testClassScheduleA.startTime)

        val classSchedule = Await.result(result, Duration.Inf)

        classSchedule should be(false)
      }
    }
  }

  "ClassScheduleRepository.list" should {
    inSequence {
      "list all schedules" in {
        val result = sectionScheduleRepository.list

        val classSchedules = Await.result(result, Duration.Inf)

        classSchedules.toString() should be(Vector(TestValues.testClassScheduleA, TestValues.testClassScheduleB, TestValues.testClassScheduleC).toString())
      }
      "list all schedules for a given class" in {
        val result = sectionScheduleRepository.list(TestValues.testClassB)

        val classSchedules = Await.result(result, Duration.Inf)

        classSchedules.toString() should be(Vector(TestValues.testClassScheduleB, TestValues.testClassScheduleC).toString())
      }
      "return empty Vector() for unexisting class" in {
        val result = sectionScheduleRepository.list(TestValues.testClassE)
        val classSchedules = Await.result(result, Duration.Inf)

        classSchedules should be (Vector())
      }
    }
  }

  "ClassScheduleRepository.find" should {
    inSequence {
      "find a single entry by ID" in {
        val result = sectionScheduleRepository.find(TestValues.testClassScheduleA.id).map(_.get)

        val classSchedule = Await.result(result, Duration.Inf)

        classSchedule.toString() should be(TestValues.testClassScheduleA.toString())
      }
      "be NONE if entry wasn't found by ID" in {
        val result = sectionScheduleRepository.find(UUID("f9aadc67-5e8b-48f3-b0a2-20a0d7d88477"))

        Await.result(result, Duration.Inf) should be (None)
      }
    }
  }

  "ClassScheduleRepository.insert" should {
    inSequence {
      "create new schedule" in {
        val result = sectionScheduleRepository.insert(TestValues.testClassScheduleD)

        val classSchedule = Await.result(result, Duration.Inf)

        classSchedule.id should be(TestValues.testClassScheduleD.id)
        classSchedule.version should be(1L)
        classSchedule.classId should be(TestValues.testClassScheduleD.classId)
        classSchedule.day should be(TestValues.testClassScheduleD.day)
        classSchedule.startTime should be(TestValues.testClassScheduleD.startTime)
        classSchedule.endTime should be(TestValues.testClassScheduleD.endTime)
        classSchedule.description should be(TestValues.testClassScheduleD.description)

        // Check
        val queryResult = db.pool.sendPreparedStatement(SelectOne, Array[Any](TestValues.testClassScheduleD.id.bytes)).map { queryResult =>
          val classScheduleList = queryResult.rows.get.map {
            item: RowData => ClassSchedule(item)
          }
          classScheduleList
        }

        val classScheduleList = Await.result(queryResult, Duration.Inf)

        classScheduleList(0).id should be(TestValues.testClassScheduleD.id)
        classScheduleList(0).version should be(1L)
        classScheduleList(0).classId should be(TestValues.testClassScheduleD.classId)
        classScheduleList(0).day should be(TestValues.testClassScheduleD.day)
        classScheduleList(0).startTime should be(TestValues.testClassScheduleD.startTime)
        classScheduleList(0).endTime should be(TestValues.testClassScheduleD.endTime)
        classScheduleList(0).description should be(TestValues.testClassScheduleD.description)
      }
      "throw a GenericDatabaseException if schedule contains unexisting class id" in {
        val result = sectionScheduleRepository.insert(TestValues.testClassScheduleE.copy(
          classId = UUID("41010a6e-9ccc-4c36-b92c-4a6b45ec0655")
        ))

        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
      }
      "throw a GenericDatabaseException if schedule already exists" in {
        val result = sectionScheduleRepository.insert(TestValues.testClassScheduleA)

        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
      }
    }
  }

  "ClassScheduleRepository.update" should {
    inSequence {
      "update" in {
        val result = sectionScheduleRepository.update(TestValues.testClassScheduleC.copy(
          classId = UUID("94cc65bb-4542-4f62-8e08-d58522e7b5f1"), // Class D
          day = new LocalDate(2015, 1, 18),
          startTime = new LocalTime(16, 38, 19),
          endTime = new LocalTime(17, 38, 19),
          description = "new test ClassSchedule C description"
        ))

        val classSchedule = Await.result(result, Duration.Inf)

        classSchedule.id should be(TestValues.testClassScheduleC.id)
        classSchedule.version should be(TestValues.testClassScheduleC.version + 1)
        classSchedule.classId should be(TestValues.testClassD.id)
        classSchedule.day should be(new LocalDate(2015, 1, 18))
        classSchedule.startTime should be(new LocalTime(16, 38, 19))
        classSchedule.endTime should be(new LocalTime(17, 38, 19))
        classSchedule.description should be("new test ClassSchedule C description")

        // Check
        val queryResult = db.pool.sendPreparedStatement(SelectOne, Array[Any](TestValues.testClassScheduleC.id.bytes)).map { queryResult =>
          val classScheduleList = queryResult.rows.get.map {
            item: RowData => ClassSchedule(item)
          }
          classScheduleList
        }

        val classScheduleList = Await.result(queryResult, Duration.Inf)

        classScheduleList(0).id should be(TestValues.testClassScheduleC.id)
        classScheduleList(0).version should be(TestValues.testClassScheduleC.version + 1)
        classScheduleList(0).classId should be(TestValues.testClassD.id)
        classScheduleList(0).day should be(new LocalDate(2015, 1, 18))
        classScheduleList(0).startTime should be(new LocalTime(16, 38, 19))
        classScheduleList(0).endTime should be(new LocalTime(17, 38, 19))
        classScheduleList(0).description should be("new test ClassSchedule C description")
      }
      "throw a NoSuchElementException when update an existing ClassSchedule with wrong version" in {
        val result = sectionScheduleRepository.update(TestValues.testClassScheduleC.copy(
          version = 99L,
          classId = UUID("94cc65bb-4542-4f62-8e08-d58522e7b5f1"), // Class D
          day = new LocalDate(2015, 1, 18),
          startTime = new LocalTime(16, 38, 19),
          endTime = new LocalTime(17, 38, 19),
          description = "new test ClassSchedule C description"
        ))

        an[java.util.NoSuchElementException] should be thrownBy Await.result(result, Duration.Inf)
      }
      "throw a NoSuchElementException when update an unexisting ClassSchedule" in {
        val result = sectionScheduleRepository.update(ClassSchedule(
          classId = UUID("94cc65bb-4542-4f62-8e08-d58522e7b5f1"), // Class D
          day = new LocalDate(2015, 1, 18),
          startTime = new LocalTime(16, 38, 19),
          endTime = new LocalTime(17, 38, 19),
          description = "new test ClassSchedule C description"
        ))

        an[java.util.NoSuchElementException] should be thrownBy Await.result(result, Duration.Inf)
      }
    }
  }

  "ClassScheduleRepository.delete" should {
    inSequence {
      "return TRUE and delete a schedule" in {
        val result = sectionScheduleRepository.delete(TestValues.testClassScheduleA)

        Await.result(result, Duration.Inf) should be(true)

        // Check
        val queryResult = db.pool.sendPreparedStatement(SelectOne, Array[Any](TestValues.testClassScheduleA.id.bytes)).map { queryResult =>
          val classScheduleList = queryResult.rows.get.map {
            item: RowData => ClassSchedule(item)
          }
          classScheduleList
        }

        Await.result(queryResult, Duration.Inf) should be(Vector())
      }
      "return FALSE if ClassSchedule hasn't been found" in {
        val result = sectionScheduleRepository.delete(ClassSchedule(
          classId = UUID("94cc65bb-4542-4f62-8e08-d58522e7b5f1"), // Class D
          day = new LocalDate(2015, 1, 18),
          startTime = new LocalTime(16, 38, 19),
          endTime = new LocalTime(17, 38, 19),
          description = "new test ClassSchedule C description"
        ))

        Await.result(result, Duration.Inf) should be(false)
      }
    }
  }
}
