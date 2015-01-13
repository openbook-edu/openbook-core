import java.awt.Color
import java.io.File
import ca.shiftfocus.krispii.core.models.Class
import ca.shiftfocus.krispii.core.repositories.{UserRepositoryComponent, ClassRepositoryPostgresComponent}
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.RowData
import grizzled.slf4j.Logger
import org.scalamock.scalatest.MockFactory
import org.scalatest.{MustMatchers, WordSpec, BeforeAndAfterAll, Suite}
import org.scalatest._
import Matchers._
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Await
import scala.concurrent.duration.Duration

trait ClassRepoTestEnvironment
  extends ClassRepositoryPostgresComponent
  with UserRepositoryComponent
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
     SELECT classes.id as id, classes.version as version, classes.teacher_id as teacher_id,
     classes.name as name, classes.color as color, classes.created_at as created_at, classes.updated_at as updated_at
     FROM classes
     WHERE classes.id = ?
                  """

  val countUserInClass = """
      SELECT *
      FROM users_classes
      WHERE user_id = ?
      AND class_id = ?
        """

  val countProjectInClass = """
      SELECT *
      FROM classes_projects
      WHERE project_id = ?
      AND class_id = ?
                            """
}

class ClassRepositorySpec
  extends WordSpec
  with MustMatchers
  with MockFactory
  with ClassRepoTestEnvironment {

  override val userRepository = stub[UserRepository]

  "ClassRepository.list" should {
    inSequence {
      "list all classes" in {
        val result = classRepository.list

        val classes = Await.result(result, Duration.Inf)

        classes.toString should be(Vector(TestValues.testClassA, TestValues.testClassB, TestValues.testClassD).toString)

        Map[Int, Class](0 -> TestValues.testClassA, 1 -> TestValues.testClassB, 2 -> TestValues.testClassD).foreach {
          case (key, clas: Class) => {
            classes(key).id should be(clas.id)
            classes(key).teacherId should be(clas.teacherId)
            classes(key).name should be(clas.name)
            classes(key).color should be(clas.color)
          }
        }
      }
      "select rows by their course ID" in {
        fail("There is no column called 'course_id' in table 'classes', also table 'courses' is not linked with any other table")
      }
      // TODO - Rewrite this method
      "select rows by their project ID"  + Console.RED + Console.BOLD + " (NOTE: Please check Javadoc for this method) " + Console.RESET in {
        fail("Method is deprecated, talbe CLASSES_PROJECTS will be deleted")
        val result = classRepository.list(TestValues.testProjectA)
      }
      "return empty Vector() for unexisting project" in {
        val result = classRepository.list(TestValues.testProjectD)
        val classes = Await.result(result, Duration.Inf)

        classes should be (Vector())
      }
      "select classes if user is not teacher for any class and has record in 'users_classes' table and (asTeacher = FALSE)" in {
        val result = classRepository.list(TestValues.testUserE, false)

        val classes = Await.result(result, Duration.Inf)

        Map[Int, Class](0 -> TestValues.testClassB).foreach {
          case (key, clas: Class) => {
            classes(key).id should be(clas.id)
            classes(key).teacherId should be(clas.teacherId)
            classes(key).name should be(clas.name)
            classes(key).color should be(clas.color)
          }
        }
      }
      "return empty Vector() if user is not teacher for any class and has record in 'users_classes' table and (asTeacher = TRUE)" in {
        val result = classRepository.list(TestValues.testUserE, true)
        val classes = Await.result(result, Duration.Inf)

        classes should be(Vector())
      }
      "select classes if user is teacher for any class and has record in 'users_classes' table and (asTeacher = TRUE)" in {
        val result = classRepository.list(TestValues.testUserA, true)

        val classes = Await.result(result, Duration.Inf)

        Map[Int, Class](0 -> TestValues.testClassA).foreach {
          case (key, clas: Class) => {
            classes(key).id should be(clas.id)
            classes(key).teacherId should be(clas.teacherId)
            classes(key).name should be(clas.name)
            classes(key).color should be(clas.color)
          }
        }
      }
      "select classes if user is teacher for any class table and (asTeacher = TRUE)" in {
        val result = classRepository.list(TestValues.testUserA, true)

        val classes = Await.result(result, Duration.Inf)

        Map[Int, Class](0 -> TestValues.testClassA).foreach {
          case (key, clas: Class) => {
            classes(key).id should be(clas.id)
            classes(key).teacherId should be(clas.teacherId)
            classes(key).name should be(clas.name)
            classes(key).color should be(clas.color)
          }
        }
      }
      "return empty Vector() if user is teacher for any class and hasn't a record in 'users_classes' table and (asTeacher = FALSE)" in {
        val result = classRepository.list(TestValues.testUserF, false)
        val classes = Await.result(result, Duration.Inf)

        classes should be(Vector())
      }
      "return empty Vector() if user unexists and (asTeacher = FALSE)" in {
        val result = classRepository.list(TestValues.testUserD, false)
        val classes = Await.result(result, Duration.Inf)

        classes should be(Vector())
      }
      "return empty Vector() if user unexists and (asTeacher = TRUE)" in {
        val result = classRepository.list(TestValues.testUserD, true)
        val classes = Await.result(result, Duration.Inf)

        classes should be(Vector())
      }
      "list the classes associated with a users" + Console.RED + Console.BOLD + " (NOTE: Please check Javadoc for this method) " + Console.RESET in {
        val result = classRepository.list(Vector(TestValues.testUserA, TestValues.testUserB))
        val classes = Await.result(result, Duration.Inf)

        val classesUserA = classes(TestValues.testUserA.id)
        val classesUserB = classes(TestValues.testUserB.id)

        Map[Int, Class](0 -> TestValues.testClassA).foreach {
          case (key, clas: Class) => {
            classesUserA(key).id should be(clas.id)
            classesUserA(key).teacherId should be(clas.teacherId)
            classesUserA(key).name should be(clas.name)
            classesUserA(key).color should be(clas.color)
          }
        }
        Map[Int, Class](0 -> TestValues.testClassB).foreach {
          case (key, clas: Class) => {
            classesUserB(key).id should be(clas.id)
            classesUserB(key).teacherId should be(clas.teacherId)
            classesUserB(key).name should be(clas.name)
            classesUserB(key).color should be(clas.color)
          }
        }
      }
      "return empty Vector() if user doesn't exist" in {
        val result = classRepository.list(Vector(TestValues.testUserD))
        val classes = Await.result(result, Duration.Inf)

        val classesUserD = classes(TestValues.testUserD.id)

        classesUserD should be (Vector())
      }
      "return empty Vector() if user is teacher and doesn't have references in users_classes table" in {
        val result = classRepository.list(Vector(TestValues.testUserF))
        val classes = Await.result(result, Duration.Inf)

        val classesUserF = classes(TestValues.testUserF.id)

        classesUserF should be (Vector())
      }
    }
  }

  "ClassRepository.find" should {
    inSequence {
      "find a single entry by ID" in {
        val result = classRepository.find(TestValues.testClassA.id).map(_.get)

        val clas = Await.result(result, Duration.Inf)
        clas.id should be(TestValues.testClassA.id)
        clas.version should be(TestValues.testClassA.version)
        clas.name should be(TestValues.testClassA.name)
        clas.createdAt.toString should be(TestValues.testClassA.createdAt.toString)
        clas.updatedAt.toString should be(TestValues.testClassA.updatedAt.toString)
      }
      "be NONE if entry wasn't found by ID" in {
        val result = classRepository.find(UUID("f9aadc67-5e8b-48f3-b0a2-20a0d7d88477"))

        Await.result(result, Duration.Inf) should be (None)
      }
    }
  }

  "ClassRepository.findUserForTeacher"  + Console.RED + Console.BOLD + " (NOTE: No Javadoc for this method) " + Console.RESET should {
    inSequence {
      "confirm that student is in teacher's class" in {
        val result = classRepository.findUserForTeacher(TestValues.testUserE, TestValues.testUserB).map(_.get)

        val user = Await.result(result, Duration.Inf)
        user should be (TestValues.testUserE)
      }
      "return NONE if student isn't in the teacher's class" in {
        val result = classRepository.findUserForTeacher(TestValues.testUserE, TestValues.testUserA)

        Await.result(result, Duration.Inf) should be (None)
      }
    }
  }

  "ClassRepository.addUser" should {
    inSequence {
      "add user to a class" in {
        val result = classRepository.addUser(TestValues.testUserC, TestValues.testClassA)
        Await.result(result, Duration.Inf) should be(true)

        // Check
        val queryResult = db.pool.sendPreparedStatement(countUserInClass, Array[Any](TestValues.testUserC.id.bytes, TestValues.testClassA.id.bytes))
        val rowsAffected = Await.result(queryResult, Duration.Inf).rowsAffected

        rowsAffected should be (1)
      }
      "throw a GenericDatabaseException if user is already in the class" in {
        val result = classRepository.addUser(TestValues.testUserE, TestValues.testClassB)

        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
      }
      "throw a GenericDatabaseException if user unexists" in {
        val result = classRepository.addUser(TestValues.testUserD, TestValues.testClassB)

        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
      }
      "throw a GenericDatabaseException if class unexists" in {
        val result = classRepository.addUser(TestValues.testUserE, TestValues.testClassC)

        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
      }
    }
  }

  "ClassRepository.removeUser" + Console.RED + Console.BOLD + " (NOTE: Please check Javadoc for this method) " + Console.RESET should {
    inSequence {
      "remove user from class " in {
        // Check if user present
        val queryResultIs = db.pool.sendPreparedStatement(countUserInClass, Array[Any](TestValues.testUserA.id.bytes, TestValues.testClassA.id.bytes))
        val rowsAffectedIs = Await.result(queryResultIs, Duration.Inf).rowsAffected

        rowsAffectedIs should be (1)

        val result = classRepository.removeUser(TestValues.testUserA, TestValues.testClassA)
        Await.result(result, Duration.Inf) should be(true)

        // Check
        val queryResult = db.pool.sendPreparedStatement(countUserInClass, Array[Any](TestValues.testUserA.id.bytes, TestValues.testClassA.id.bytes))
        val rowsAffected = Await.result(queryResult, Duration.Inf).rowsAffected

        rowsAffected should be (0)
      }
      "be FALSE if user unexists" in {
        val result = classRepository.removeUser(TestValues.testUserD, TestValues.testClassB)

        Await.result(result, Duration.Inf) should be(false)
      }
      "be FALSE if class unexists" in {
        val result = classRepository.removeUser(TestValues.testUserA, TestValues.testClassC)

        Await.result(result, Duration.Inf) should be(false)
      }
    }
  }

  // TODO - Rewrite this method
  "ClassRepository.hasProject" should {
    inSequence {
      "verify if this user has access to this project through any of his classes" in {
        fail("Method is deprecated, talbe CLASSES_PROJECTS will be deleted")
        val result = classRepository.hasProject(TestValues.testUserE, TestValues.testProjectB)

        Await.result(result, Duration.Inf) should be(true)
      }
      "be FALSE if user is not in a project's class" in {
        val result = classRepository.hasProject(TestValues.testUserE, TestValues.testProjectA)

        Await.result(result, Duration.Inf) should be(false)
      }
      "be FALSE if user unexists" in {
        val result = classRepository.hasProject(TestValues.testUserD, TestValues.testProjectA)

        Await.result(result, Duration.Inf) should be(false)
      }
      "be FALSE if project unexists" in {
        val result = classRepository.hasProject(TestValues.testUserD, TestValues.testProjectD)

        Await.result(result, Duration.Inf) should be(false)
      }
    }
  }

  "ClassRepository.addUsers" should {
    inSequence {
      "add users to a `class`" in {
        val result = classRepository.addUsers(TestValues.testClassD, Vector(TestValues.testUserA, TestValues.testUserB))

        Await.result(result, Duration.Inf) should be(true)

        // Check User A in class D
        val queryResultUserA = db.pool.sendPreparedStatement(countUserInClass, Array[Any](TestValues.testUserA.id.bytes, TestValues.testClassD.id.bytes))
        val rowsAffectedUserA = Await.result(queryResultUserA, Duration.Inf).rowsAffected

        rowsAffectedUserA should be (1)

        // Check User B in class D
        val queryResultUserB = db.pool.sendPreparedStatement(countUserInClass, Array[Any](TestValues.testUserB.id.bytes, TestValues.testClassD.id.bytes))
        val rowsAffectedUserB = Await.result(queryResultUserB, Duration.Inf).rowsAffected

        rowsAffectedUserB should be (1)
      }
      "throw a GenericDatabaseException if user is already in the class" in {
        val result = classRepository.addUsers(TestValues.testClassB, Vector(TestValues.testUserE))

        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
      }
      "throw a GenericDatabaseException if user unexists" in {
        val result = classRepository.addUsers(TestValues.testClassB, Vector(TestValues.testUserD))

        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
      }
      "throw a GenericDatabaseException if class unexists" in {
        val result = classRepository.addUsers(TestValues.testClassC, Vector(TestValues.testUserE))

        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
      }
    }
  }


  // TODO - Rewrite this method
  "ClassRepository.addProjects" should {
    inSequence {
      "add projects to a `class`" in {
        fail("Method is deprecated, talbe CLASSES_PROJECTS will be deleted")
      }
    }
  }

  "ClassRepository.removeUsers" should {
    inSequence {
      "Remove users from a `class`" in {
        // Check User B in class B
        val queryResultIsUserB = db.pool.sendPreparedStatement(countUserInClass, Array[Any](TestValues.testUserB.id.bytes, TestValues.testClassB.id.bytes))
        val rowsAffectedIsUserB = Await.result(queryResultIsUserB, Duration.Inf).rowsAffected

        rowsAffectedIsUserB should be (1)

        // Check User E in class B
        val queryResultIsUserE = db.pool.sendPreparedStatement(countUserInClass, Array[Any](TestValues.testUserE.id.bytes, TestValues.testClassB.id.bytes))
        val rowsAffectedIsUserE = Await.result(queryResultIsUserE, Duration.Inf).rowsAffected

        rowsAffectedIsUserE should be (1)

        val result = classRepository.removeUsers(TestValues.testClassB, Vector(TestValues.testUserB, TestValues.testUserE))

        Await.result(result, Duration.Inf) should be(true)

        // Check User B in class B
        val queryResultUserB = db.pool.sendPreparedStatement(countUserInClass, Array[Any](TestValues.testUserB.id.bytes, TestValues.testClassB.id.bytes))
        val rowsAffectedUserB = Await.result(queryResultUserB, Duration.Inf).rowsAffected

        rowsAffectedUserB should be (0)

        // Check User E in class B
        val queryResultUserE = db.pool.sendPreparedStatement(countUserInClass, Array[Any](TestValues.testUserE.id.bytes, TestValues.testClassB.id.bytes))
        val rowsAffectedUserE = Await.result(queryResultUserE, Duration.Inf).rowsAffected

        rowsAffectedUserE should be (0)
      }
      "be FALSE if user is not in the class" in {
        val result = classRepository.removeUsers(TestValues.testClassA, Vector(TestValues.testUserE))

        Await.result(result, Duration.Inf) should be(false)
      }
      "be FALSE if user unexists" in {
        val result = classRepository.removeUsers(TestValues.testClassB, Vector(TestValues.testUserD))

        Await.result(result, Duration.Inf) should be(false)
      }
      "be FALSE if class unexists" in {
        val result = classRepository.removeUsers(TestValues.testClassC, Vector(TestValues.testUserE))

        Await.result(result, Duration.Inf) should be(false)
      }
    }
  }

  // TODO - Rewrite this method
  "ClassRepository.removeProjects" should {
    inSequence {
      "remove projects from class" in {
        fail("Method is deprecated, talbe CLASSES_PROJECTS will be deleted")
      }
    }
  }

  "ClassRepository.removeAllUsers" should {
    inSequence {
      "remove all users from a `class`" in {
        // Check User G in class B
        val queryResultIsUserG = db.pool.sendPreparedStatement(countUserInClass, Array[Any](TestValues.testUserG.id.bytes, TestValues.testClassB.id.bytes))
        val rowsAffectedIsUserG = Await.result(queryResultIsUserG, Duration.Inf).rowsAffected

        rowsAffectedIsUserG should be (1)

        // Check User H in class B
        val queryResultIsUserH = db.pool.sendPreparedStatement(countUserInClass, Array[Any](TestValues.testUserH.id.bytes, TestValues.testClassB.id.bytes))
        val rowsAffectedIsUserH = Await.result(queryResultIsUserH, Duration.Inf).rowsAffected

        rowsAffectedIsUserH should be (1)


        val result = classRepository.removeAllUsers(TestValues.testClassB)

        Await.result(result, Duration.Inf) should be(true)

        // Check User G in class B
        val queryResultUserG = db.pool.sendPreparedStatement(countUserInClass, Array[Any](TestValues.testUserG.id.bytes, TestValues.testClassB.id.bytes))
        val rowsAffectedUserG = Await.result(queryResultUserG, Duration.Inf).rowsAffected

        rowsAffectedUserG should be (0)

        // Check User H in class B
        val queryResultUserH = db.pool.sendPreparedStatement(countUserInClass, Array[Any](TestValues.testUserH.id.bytes, TestValues.testClassB.id.bytes))
        val rowsAffectedUserH = Await.result(queryResultUserH, Duration.Inf).rowsAffected

        rowsAffectedUserH should be (0)
      }
      "be FALSE if class unexists" in {
        val result = classRepository.removeAllUsers(TestValues.testClassC)

        Await.result(result, Duration.Inf) should be(false)
      }
    }
  }

  // TODO - Rewrite this method
  "ClassRepository.removeAllProjects" should {
    inSequence {
      "remove all projects from class" in {
        fail("Method is deprecated, talbe CLASSES_PROJECTS will be deleted")
      }
    }
  }

  "ClassRepository.insert"  + Console.RED + Console.BOLD + " (NOTE: Please check Javadoc for this method) " + Console.RESET should {
    inSequence {
      "insert new class" in {
        val result = classRepository.insert(TestValues.testClassE)

        val newClass = Await.result(result, Duration.Inf)

        newClass.id should be (TestValues.testClassE.id)
        newClass.teacherId should be (TestValues.testClassE.teacherId)
        newClass.version should be (TestValues.testClassE.version)
        newClass.name should be (TestValues.testClassE.name)
        newClass.color should be (TestValues.testClassE.color)

        // Check
        val queryResult = db.pool.sendPreparedStatement(SelectOne, Array[Any](TestValues.testClassE.id.bytes)).map { queryResult =>
          val classList = queryResult.rows.get.map {
            item: RowData => Class(item)
          }
          classList
        }

        val classList = Await.result(queryResult, Duration.Inf)

        classList(0).id should be(TestValues.testClassE.id)
        classList(0).teacherId should be(TestValues.testClassE.teacherId)
        classList(0).version should be(TestValues.testClassE.version)
        classList(0).name should be(TestValues.testClassE.name)
        classList(0).color should be(TestValues.testClassE.color)
      }
      "throw a GenericDatabaseException if class already exists" in {
        val result = classRepository.insert(TestValues.testClassA)

        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
      }
    }
  }

  "ClassRepository.update"  + Console.RED + Console.BOLD + " (NOTE: Please check Javadoc for this method) " + Console.RESET should {
    inSequence {
      "update existing class" in {
        val result = classRepository.update(TestValues.testClassA.copy(
          teacherId = Option(TestValues.testUserG.id),
          name = "new test class A name",
          color = new Color(78, 40, 23)
        ))

        val updatedClass = Await.result(result, Duration.Inf)

        updatedClass.id should be (TestValues.testClassA.id)
        updatedClass.teacherId should be (Some(TestValues.testUserG.id)) // TODO - check why SOME?
        updatedClass.version should be (TestValues.testClassA.version + 1)
        updatedClass.name should be ("new test class A name")
        updatedClass.color should be (new Color(78, 40, 23))

        // Check
        val queryResult = db.pool.sendPreparedStatement(SelectOne, Array[Any](TestValues.testClassA.id.bytes)).map { queryResult =>
          val classList = queryResult.rows.get.map {
            item: RowData => Class(item)
          }
          classList
        }

        val classList = Await.result(queryResult, Duration.Inf)

        classList(0).id should be(TestValues.testClassA.id)
        classList(0).teacherId should be(Some(TestValues.testUserG.id))
        classList(0).version should be(TestValues.testClassA.version + 1)
        classList(0).name should be("new test class A name")
        classList(0).color should be(new Color(78, 40, 23))
      }
      "throw a NoSuchElementException when update an existing Class with wrong version" in {
        val result = classRepository.update(TestValues.testClassA.copy(
          version = 99L,
          name = "new test class A name"
        ))

        an[java.util.NoSuchElementException] should be thrownBy Await.result(result, Duration.Inf)
      }
      "throw a NoSuchElementException when update an unexisting Class" in {
        val result = classRepository.update(Class(
          teacherId = Option(TestValues.testUserG.id),
          name = "new test class A name",
          color = new Color(8, 4, 3)
        ))

        an[java.util.NoSuchElementException] should be thrownBy Await.result(result, Duration.Inf)
      }
    }
  }

  // TODO - add should return FALSE if hasn't been found to all tests
  "ClassRepository.delete" should {
    inSequence {
      "throw a GenericDatabaseException if class has references in other tables" in {
        val result = classRepository.delete(TestValues.testClassB)

        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
      }
      "return FALSE if Class hasn't been found" in {
        val result = classRepository.delete(Class(
          teacherId = Option(TestValues.testUserG.id),
          name = "new test class A name",
          color = new Color(8, 4, 3)
        ))

        Await.result(result, Duration.Inf) should be(false)
      }
    }
  }

  // TODO - Rewrite this method
  "ClassRepository.enablePart" should {
    inSequence {
      "enable a particular project part for this Section's users" in {
        fail("Method is deprecated, talbe SCHEDULED_CLASSES_PARTS will be deleted")
      }
    }
  }

  // TODO - Rewrite this method
  "ClassRepository.disablePart" should {
    inSequence {
      "disable a particular project part for this Section's users" in {
        fail("Method is deprecated, talbe SCHEDULED_CLASSES_PARTS will be deleted")
      }
    }
  }
}
