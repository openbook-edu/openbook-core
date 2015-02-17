//import java.awt.Color
//import java.io.File
//import ca.shiftfocus.krispii.core.models.Course
//import ca.shiftfocus.krispii.core.repositories.{UserRepositoryComponent, CourseRepositoryPostgresComponent}
//import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
//import ca.shiftfocus.uuid.UUID
//import com.github.mauricio.async.db.RowData
//import grizzled.slf4j.Logger
//import org.scalamock.scalatest.MockFactory
//import org.scalatest.{MustMatchers, WordSpec, BeforeAndAfterAll, Suite}
//import org.scalatest._
//import Matchers._
//import scala.concurrent.ExecutionContext.Implicits.global
//
//import scala.concurrent.Await
//import scala.concurrent.duration.Duration
//
//trait CourseRepoTestEnvironment
//  extends CourseRepositoryPostgresComponent
//  with UserRepositoryComponent
//  with Suite
//  with BeforeAndAfterAll
//  with PostgresDB {
//
//  val logger = Logger[this.type]
//
//  implicit val connection = db.pool
//
//  val project_path = new File(".").getAbsolutePath()
//  val create_schema_path = s"${project_path}/src/test/resources/schemas/create_schema.sql"
//  val drop_schema_path = s"${project_path}/src/test/resources/schemas/drop_schema.sql"
//  val data_schema_path = s"${project_path}/src/test/resources/schemas/data_schema.sql"
//
//  /**
//   * Implements query from schema file
//   * @param path Path to schema file
//   */
//  def load_schema(path: String): Unit = {
//    val sql_schema_file = scala.io.Source.fromFile(path)
//    val query = sql_schema_file.getLines().mkString
//    sql_schema_file.close()
//    val result = db.pool.sendQuery(query)
//    Await.result(result, Duration.Inf)
//  }
//
//  // Before test
//  override def beforeAll(): Unit = {
//    // DROP tables
//    load_schema(drop_schema_path)
//    // CREATE tables
//    load_schema(create_schema_path)
//    // Insert data into tables
//    load_schema(data_schema_path)
//  }
//
//  // After test
//  override def afterAll(): Unit = {
//    // DROP tables
//    load_schema(drop_schema_path)
//  }
//
//  // TODO change on *
//  val SelectOne = """
//     SELECT classes.id as id, classes.version as version, classes.teacher_id as teacher_id,
//     classes.name as name, classes.color as color, classes.created_at as created_at, classes.updated_at as updated_at
//     FROM classes
//     WHERE classes.id = ?
//                  """
//
//  val countUserInCourse = """
//      SELECT *
//      FROM users_classes
//      WHERE user_id = ?
//      AND class_id = ?
//        """
//
//  val countProjectInCourse = """
//      SELECT *
//      FROM classes_projects
//      WHERE project_id = ?
//      AND class_id = ?
//                            """
//}
//
//class CourseRepositorySpec
//  extends WordSpec
//  with MustMatchers
//  with MockFactory
//  with CourseRepoTestEnvironment {
//
//  override val userRepository = stub[UserRepository]
//
//  "CourseRepository.list" should {
//    inSequence {
//      "list all classes" in {
//        val result = courseRepository.list
//
//        val classes = Await.result(result, Duration.Inf)
//
//        classes.toString should be(Vector(TestValues.testCourseA, TestValues.testCourseB, TestValues.testCourseD, TestValues.testCourseF).toString)
//
//        Map[Int, Course](0 -> TestValues.testCourseA, 1 -> TestValues.testCourseB, 2 -> TestValues.testCourseD, 3 -> TestValues.testCourseF).foreach {
//          case (key, clas: Course) => {
//            classes(key).id should be(clas.id)
//            classes(key).teacherId should be(clas.teacherId)
//            classes(key).name should be(clas.name)
//            classes(key).color should be(clas.color)
//          }
//        }
//      }
//      "return class by its project" in {
//        val result = courseRepository.list(TestValues.testProjectA)
//
//        val classes = Await.result(result, Duration.Inf)
//
//        classes.toString should be(Vector(TestValues.testCourseA).toString)
//
//        Map[Int, Course](0 -> TestValues.testCourseA).foreach {
//          case (key, clas: Course) => {
//            classes(key).id should be(clas.id)
//            classes(key).teacherId should be(clas.teacherId)
//            classes(key).name should be(clas.name)
//            classes(key).color should be(clas.color)
//          }
//        }
//      }
//      "return empty Vector() for unexisting project" in {
//        val result = courseRepository.list(TestValues.testProjectD)
//        val classes = Await.result(result, Duration.Inf)
//
//        classes should be (Vector())
//      }
//      "select classes if user is not teacher for any class and has record in 'users_classes' table and (asTeacher = FALSE)" in {
//        val result = courseRepository.list(TestValues.testUserE, false)
//
//        val classes = Await.result(result, Duration.Inf)
//
//        Map[Int, Course](0 -> TestValues.testCourseB).foreach {
//          case (key, clas: Course) => {
//            classes(key).id should be(clas.id)
//            classes(key).teacherId should be(clas.teacherId)
//            classes(key).name should be(clas.name)
//            classes(key).color should be(clas.color)
//          }
//        }
//      }
//      "return empty Vector() if user is not teacher for any class and has record in 'users_classes' table and (asTeacher = TRUE)" in {
//        val result = courseRepository.list(TestValues.testUserE, true)
//        val classes = Await.result(result, Duration.Inf)
//
//        classes should be(Vector())
//      }
//      "select classes if user is teacher for any class and has record in 'users_classes' table and (asTeacher = TRUE)" in {
//        val result = courseRepository.list(TestValues.testUserA, true)
//
//        val classes = Await.result(result, Duration.Inf)
//
//        Map[Int, Course](0 -> TestValues.testCourseA).foreach {
//          case (key, clas: Course) => {
//            classes(key).id should be(clas.id)
//            classes(key).teacherId should be(clas.teacherId)
//            classes(key).name should be(clas.name)
//            classes(key).color should be(clas.color)
//          }
//        }
//      }
//      "select classes if user is teacher for any class table and (asTeacher = TRUE)" in {
//        val result = courseRepository.list(TestValues.testUserA, true)
//
//        val classes = Await.result(result, Duration.Inf)
//
//        Map[Int, Course](0 -> TestValues.testCourseA).foreach {
//          case (key, clas: Course) => {
//            classes(key).id should be(clas.id)
//            classes(key).teacherId should be(clas.teacherId)
//            classes(key).name should be(clas.name)
//            classes(key).color should be(clas.color)
//          }
//        }
//      }
//      "return empty Vector() if user is teacher for any class and hasn't a record in 'users_classes' table and (asTeacher = FALSE)" in {
//        val result = courseRepository.list(TestValues.testUserF, false)
//        val classes = Await.result(result, Duration.Inf)
//
//        classes should be(Vector())
//      }
//      "return empty Vector() if user unexists and (asTeacher = FALSE)" in {
//        val result = courseRepository.list(TestValues.testUserD, false)
//        val classes = Await.result(result, Duration.Inf)
//
//        classes should be(Vector())
//      }
//      "return empty Vector() if user unexists and (asTeacher = TRUE)" in {
//        val result = courseRepository.list(TestValues.testUserD, true)
//        val classes = Await.result(result, Duration.Inf)
//
//        classes should be(Vector())
//      }
//      "list the classes associated with a users" in {
//        val result = courseRepository.list(Vector(TestValues.testUserA, TestValues.testUserB))
//        val classes = Await.result(result, Duration.Inf)
//
//        val classesUserA = classes(TestValues.testUserA.id)
//        val classesUserB = classes(TestValues.testUserB.id)
//
//        Map[Int, Course](0 -> TestValues.testCourseA).foreach {
//          case (key, clas: Course) => {
//            classesUserA(key).id should be(clas.id)
//            classesUserA(key).teacherId should be(clas.teacherId)
//            classesUserA(key).name should be(clas.name)
//            classesUserA(key).color should be(clas.color)
//          }
//        }
//        Map[Int, Course](0 -> TestValues.testCourseB).foreach {
//          case (key, clas: Course) => {
//            classesUserB(key).id should be(clas.id)
//            classesUserB(key).teacherId should be(clas.teacherId)
//            classesUserB(key).name should be(clas.name)
//            classesUserB(key).color should be(clas.color)
//          }
//        }
//      }
//      "return empty Vector() if user doesn't exist" in {
//        val result = courseRepository.list(Vector(TestValues.testUserD))
//        val classes = Await.result(result, Duration.Inf)
//
//        val classesUserD = classes(TestValues.testUserD.id)
//
//        classesUserD should be (Vector())
//      }
//      "return empty Vector() if user is teacher and doesn't have references in users_classes table" in {
//        val result = courseRepository.list(Vector(TestValues.testUserF))
//        val classes = Await.result(result, Duration.Inf)
//
//        val classesUserF = classes(TestValues.testUserF.id)
//
//        classesUserF should be (Vector())
//      }
//    }
//  }
//
//  "CourseRepository.find" should {
//    inSequence {
//      "find a single entry by ID" in {
//        val result = courseRepository.find(TestValues.testCourseA.id).map(_.get)
//
//        val clas = Await.result(result, Duration.Inf)
//        clas.id should be(TestValues.testCourseA.id)
//        clas.version should be(TestValues.testCourseA.version)
//        clas.name should be(TestValues.testCourseA.name)
//        clas.createdAt.toString should be(TestValues.testCourseA.createdAt.toString)
//        clas.updatedAt.toString should be(TestValues.testCourseA.updatedAt.toString)
//      }
//      "be NONE if entry wasn't found by ID" in {
//        val result = courseRepository.find(UUID("f9aadc67-5e8b-48f3-b0a2-20a0d7d88477"))
//
//        Await.result(result, Duration.Inf) should be (None)
//      }
//    }
//  }
//
//  "CourseRepository.findUserForTeacher" should {
//    inSequence {
//      "find student in teacher's class" in {
//        val result = courseRepository.findUserForTeacher(TestValues.testUserE, TestValues.testUserB).map(_.get)
//
//        val user = Await.result(result, Duration.Inf)
//        user should be (TestValues.testUserE)
//      }
//      "return NONE if student isn't in the teacher's class" in {
//        val result = courseRepository.findUserForTeacher(TestValues.testUserE, TestValues.testUserA)
//
//        Await.result(result, Duration.Inf) should be (None)
//      }
//    }
//  }
//
//  "CourseRepository.addUser" should {
//    inSequence {
//      "add user to a class" in {
//        val result = courseRepository.addUser(TestValues.testUserC, TestValues.testCourseA)
//        Await.result(result, Duration.Inf) should be(true)
//
//        // Check
//        val queryResult = db.pool.sendPreparedStatement(countUserInCourse, Array[Any](TestValues.testUserC.id.bytes, TestValues.testCourseA.id.bytes))
//        val rowsAffected = Await.result(queryResult, Duration.Inf).rowsAffected
//
//        rowsAffected should be (1)
//      }
//      "throw a GenericDatabaseException if user is already in the class" in {
//        val result = courseRepository.addUser(TestValues.testUserE, TestValues.testCourseB)
//
//        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
//      }
//      "throw a GenericDatabaseException if user unexists" in {
//        val result = courseRepository.addUser(TestValues.testUserD, TestValues.testCourseB)
//
//        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
//      }
//      "throw a GenericDatabaseException if class unexists" in {
//        val result = courseRepository.addUser(TestValues.testUserE, TestValues.testCourseC)
//
//        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
//      }
//    }
//  }
//
//  "CourseRepository.removeUser" should {
//    inSequence {
//      "remove a user from a class " in {
//        // Check if user present
//        val queryResultIs = db.pool.sendPreparedStatement(countUserInCourse, Array[Any](TestValues.testUserA.id.bytes, TestValues.testCourseA.id.bytes))
//        val rowsAffectedIs = Await.result(queryResultIs, Duration.Inf).rowsAffected
//
//        rowsAffectedIs should be (1)
//
//        val result = courseRepository.removeUser(TestValues.testUserA, TestValues.testCourseA)
//        Await.result(result, Duration.Inf) should be(true)
//
//        // Check
//        val queryResult = db.pool.sendPreparedStatement(countUserInCourse, Array[Any](TestValues.testUserA.id.bytes, TestValues.testCourseA.id.bytes))
//        val rowsAffected = Await.result(queryResult, Duration.Inf).rowsAffected
//
//        rowsAffected should be (0)
//      }
//      "be FALSE if user unexists" in {
//        val result = courseRepository.removeUser(TestValues.testUserD, TestValues.testCourseB)
//
//        Await.result(result, Duration.Inf) should be(false)
//      }
//      "be FALSE if class unexists" in {
//        val result = courseRepository.removeUser(TestValues.testUserA, TestValues.testCourseC)
//
//        Await.result(result, Duration.Inf) should be(false)
//      }
//    }
//  }
//
//  "CourseRepository.hasProject" should {
//    inSequence {
//      "verify if this user has access to this project through any of his classes" in {
//        val result = courseRepository.hasProject(TestValues.testUserE, TestValues.testProjectB)
//
//        Await.result(result, Duration.Inf) should be(true)
//      }
//      "be FALSE if user is not in a project's class" in {
//        val result = courseRepository.hasProject(TestValues.testUserE, TestValues.testProjectA)
//
//        Await.result(result, Duration.Inf) should be(false)
//      }
//      "be FALSE if user unexists" in {
//        val result = courseRepository.hasProject(TestValues.testUserD, TestValues.testProjectA)
//
//        Await.result(result, Duration.Inf) should be(false)
//      }
//      "be FALSE if project unexists" in {
//        val result = courseRepository.hasProject(TestValues.testUserD, TestValues.testProjectD)
//
//        Await.result(result, Duration.Inf) should be(false)
//      }
//    }
//  }
//
//  "CourseRepository.addUsers" should {
//    inSequence {
//      "add users to a `class`" in {
//        val result = courseRepository.addUsers(TestValues.testCourseD, Vector(TestValues.testUserA, TestValues.testUserB))
//
//        Await.result(result, Duration.Inf) should be(true)
//
//        // Check User A in class D
//        val queryResultUserA = db.pool.sendPreparedStatement(countUserInCourse, Array[Any](TestValues.testUserA.id.bytes, TestValues.testCourseD.id.bytes))
//        val rowsAffectedUserA = Await.result(queryResultUserA, Duration.Inf).rowsAffected
//
//        rowsAffectedUserA should be (1)
//
//        // Check User B in class D
//        val queryResultUserB = db.pool.sendPreparedStatement(countUserInCourse, Array[Any](TestValues.testUserB.id.bytes, TestValues.testCourseD.id.bytes))
//        val rowsAffectedUserB = Await.result(queryResultUserB, Duration.Inf).rowsAffected
//
//        rowsAffectedUserB should be (1)
//      }
//      "throw a GenericDatabaseException if user is already in the class" in {
//        val result = courseRepository.addUsers(TestValues.testCourseB, Vector(TestValues.testUserE))
//
//        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
//      }
//      "throw a GenericDatabaseException if user unexists" in {
//        val result = courseRepository.addUsers(TestValues.testCourseB, Vector(TestValues.testUserD))
//
//        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
//      }
//      "throw a GenericDatabaseException if class unexists" in {
//        val result = courseRepository.addUsers(TestValues.testCourseC, Vector(TestValues.testUserE))
//
//        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
//      }
//    }
//  }
//
//  "CourseRepository.removeUsers" should {
//    inSequence {
//      "Remove users from a `class`" in {
//        // Check User B in class B
//        val queryResultIsUserB = db.pool.sendPreparedStatement(countUserInCourse, Array[Any](TestValues.testUserB.id.bytes, TestValues.testCourseB.id.bytes))
//        val rowsAffectedIsUserB = Await.result(queryResultIsUserB, Duration.Inf).rowsAffected
//
//        rowsAffectedIsUserB should be (1)
//
//        // Check User E in class B
//        val queryResultIsUserE = db.pool.sendPreparedStatement(countUserInCourse, Array[Any](TestValues.testUserE.id.bytes, TestValues.testCourseB.id.bytes))
//        val rowsAffectedIsUserE = Await.result(queryResultIsUserE, Duration.Inf).rowsAffected
//
//        rowsAffectedIsUserE should be (1)
//
//        val result = courseRepository.removeUsers(TestValues.testCourseB, Vector(TestValues.testUserB, TestValues.testUserE))
//
//        Await.result(result, Duration.Inf) should be(true)
//
//        // Check User B in class B
//        val queryResultUserB = db.pool.sendPreparedStatement(countUserInCourse, Array[Any](TestValues.testUserB.id.bytes, TestValues.testCourseB.id.bytes))
//        val rowsAffectedUserB = Await.result(queryResultUserB, Duration.Inf).rowsAffected
//
//        rowsAffectedUserB should be (0)
//
//        // Check User E in class B
//        val queryResultUserE = db.pool.sendPreparedStatement(countUserInCourse, Array[Any](TestValues.testUserE.id.bytes, TestValues.testCourseB.id.bytes))
//        val rowsAffectedUserE = Await.result(queryResultUserE, Duration.Inf).rowsAffected
//
//        rowsAffectedUserE should be (0)
//      }
//      "be FALSE if user is not in the class" in {
//        val result = courseRepository.removeUsers(TestValues.testCourseA, Vector(TestValues.testUserE))
//
//        Await.result(result, Duration.Inf) should be(false)
//      }
//      "be FALSE if user unexists" in {
//        val result = courseRepository.removeUsers(TestValues.testCourseB, Vector(TestValues.testUserD))
//
//        Await.result(result, Duration.Inf) should be(false)
//      }
//      "be FALSE if class unexists" in {
//        val result = courseRepository.removeUsers(TestValues.testCourseC, Vector(TestValues.testUserE))
//
//        Await.result(result, Duration.Inf) should be(false)
//      }
//    }
//  }
//
//  "CourseRepository.removeAllUsers" should {
//    inSequence {
//      "remove all users from a `class`" in {
//        // Check User G in class B
//        val queryResultIsUserG = db.pool.sendPreparedStatement(countUserInCourse, Array[Any](TestValues.testUserG.id.bytes, TestValues.testCourseB.id.bytes))
//        val rowsAffectedIsUserG = Await.result(queryResultIsUserG, Duration.Inf).rowsAffected
//
//        rowsAffectedIsUserG should be (1)
//
//        // Check User H in class B
//        val queryResultIsUserH = db.pool.sendPreparedStatement(countUserInCourse, Array[Any](TestValues.testUserH.id.bytes, TestValues.testCourseB.id.bytes))
//        val rowsAffectedIsUserH = Await.result(queryResultIsUserH, Duration.Inf).rowsAffected
//
//        rowsAffectedIsUserH should be (1)
//
//
//        val result = courseRepository.removeAllUsers(TestValues.testCourseB)
//
//        Await.result(result, Duration.Inf) should be(true)
//
//        // Check User G in class B
//        val queryResultUserG = db.pool.sendPreparedStatement(countUserInCourse, Array[Any](TestValues.testUserG.id.bytes, TestValues.testCourseB.id.bytes))
//        val rowsAffectedUserG = Await.result(queryResultUserG, Duration.Inf).rowsAffected
//
//        rowsAffectedUserG should be (0)
//
//        // Check User H in class B
//        val queryResultUserH = db.pool.sendPreparedStatement(countUserInCourse, Array[Any](TestValues.testUserH.id.bytes, TestValues.testCourseB.id.bytes))
//        val rowsAffectedUserH = Await.result(queryResultUserH, Duration.Inf).rowsAffected
//
//        rowsAffectedUserH should be (0)
//      }
//      "be FALSE if class unexists" in {
//        val result = courseRepository.removeAllUsers(TestValues.testCourseC)
//
//        Await.result(result, Duration.Inf) should be(false)
//      }
//    }
//  }
//
//  "CourseRepository.insert" should {
//    inSequence {
//      "insert new class" in {
//        val result = courseRepository.insert(TestValues.testCourseE)
//
//        val newCourse = Await.result(result, Duration.Inf)
//
//        newCourse.id should be (TestValues.testCourseE.id)
//        newCourse.teacherId should be (TestValues.testCourseE.teacherId)
//        newCourse.version should be (1L)
//        newCourse.name should be (TestValues.testCourseE.name)
//        newCourse.color should be (TestValues.testCourseE.color)
//
//        // Check
//        val queryResult = db.pool.sendPreparedStatement(SelectOne, Array[Any](TestValues.testCourseE.id.bytes)).map { queryResult =>
//          val classList = queryResult.rows.get.map {
//            item: RowData => Course(item)
//          }
//          classList
//        }
//
//        val classList = Await.result(queryResult, Duration.Inf)
//
//        classList(0).id should be(TestValues.testCourseE.id)
//        classList(0).teacherId should be(TestValues.testCourseE.teacherId)
//        classList(0).version should be(1L)
//        classList(0).name should be(TestValues.testCourseE.name)
//        classList(0).color should be(TestValues.testCourseE.color)
//      }
//      "throw a GenericDatabaseException if class has unexisting teacher id" in {
//        val result = courseRepository.insert(TestValues.testCourseC.copy(
//          teacherId = Option(UUID("9d8ed645-b055-4a69-ab7d-387791c1e064"))
//        ))
//
//        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
//      }
//      "throw a GenericDatabaseException if class already exists" in {
//        val result = courseRepository.insert(TestValues.testCourseA)
//
//        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
//      }
//    }
//  }
//
//  "CourseRepository.update" should {
//    inSequence {
//      "update existing class" in {
//        val result = courseRepository.update(TestValues.testCourseA.copy(
//          teacherId = Option(TestValues.testUserG.id),
//          name = "new test class A name",
//          color = new Color(78, 40, 23)
//        ))
//
//        val updatedCourse = Await.result(result, Duration.Inf)
//
//        updatedCourse.id should be (TestValues.testCourseA.id)
//        updatedCourse.teacherId should be (Some(TestValues.testUserG.id)) // TODO - check why SOME?
//        updatedCourse.version should be (TestValues.testCourseA.version + 1)
//        updatedCourse.name should be ("new test class A name")
//        updatedCourse.color should be (new Color(78, 40, 23))
//
//        // Check
//        val queryResult = db.pool.sendPreparedStatement(SelectOne, Array[Any](TestValues.testCourseA.id.bytes)).map { queryResult =>
//          val classList = queryResult.rows.get.map {
//            item: RowData => Course(item)
//          }
//          classList
//        }
//
//        val classList = Await.result(queryResult, Duration.Inf)
//
//        classList(0).id should be(TestValues.testCourseA.id)
//        classList(0).teacherId should be(Some(TestValues.testUserG.id))
//        classList(0).version should be(TestValues.testCourseA.version + 1)
//        classList(0).name should be("new test class A name")
//        classList(0).color should be(new Color(78, 40, 23))
//      }
//      "throw a NoSuchElementException when update an existing Course with wrong version" in {
//        val result = courseRepository.update(TestValues.testCourseA.copy(
//          version = 99L,
//          name = "new test class A name"
//        ))
//
//        an[java.util.NoSuchElementException] should be thrownBy Await.result(result, Duration.Inf)
//      }
//      "throw a NoSuchElementException when update an unexisting Course" in {
//        val result = courseRepository.update(Course(
//          teacherId = Option(TestValues.testUserG.id),
//          name = "new test class A name",
//          color = new Color(8, 4, 3)
//        ))
//
//        an[java.util.NoSuchElementException] should be thrownBy Await.result(result, Duration.Inf)
//      }
//    }
//  }
//
//  "CourseRepository.delete" should {
//    inSequence {
//      "delete class if class has no references" in {
//        val result = courseRepository.delete(TestValues.testCourseF)
//
//        Await.result(result, Duration.Inf) should be(true)
//
//        // Check
//        val queryResult = db.pool.sendPreparedStatement(SelectOne, Array[Any](TestValues.testCourseF.id.bytes)).map { queryResult =>
//          val classList = queryResult.rows.get.map {
//            item: RowData => Course(item)
//          }
//          classList
//        }
//
//        Await.result(queryResult, Duration.Inf) should be(Vector())
//      }
//      "delete class if class has references in users_classes table" in {
//        val result = courseRepository.delete(TestValues.testCourseD)
//
//        Await.result(result, Duration.Inf) should be(true)
//
//        // Check
//        val queryResult = db.pool.sendPreparedStatement(SelectOne, Array[Any](TestValues.testCourseD.id.bytes)).map { queryResult =>
//          val classList = queryResult.rows.get.map {
//            item: RowData => Course(item)
//          }
//          classList
//        }
//
//        Await.result(queryResult, Duration.Inf) should be(Vector())
//      }
//      "throw a GenericDatabaseException if class has references in projects table" in {
//        val result = courseRepository.delete(TestValues.testCourseB)
//
//        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
//      }
//      "return FALSE if Course hasn't been found" in {
//        val result = courseRepository.delete(Course(
//          teacherId = Option(TestValues.testUserG.id),
//          name = "new test class A name",
//          color = new Color(8, 4, 3)
//        ))
//
//        Await.result(result, Duration.Inf) should be(false)
//      }
//    }
//  }
//
//  // TODO - delete
//  "CourseRepository.enablePart"  + Console.RED + Console.BOLD + " (NOTE: Method is deprecated, talbe CLASSES_PROJECTS will be deleted) " + Console.RESET should {
//    inSequence {
//      "enable a particular project part for this Section's users" in {
//
//      }
//    }
//  }
//
//  // TODO - delete
//  "CourseRepository.disablePart" + Console.RED + Console.BOLD + " (NOTE: Method is deprecated, talbe CLASSES_PROJECTS will be deleted) " + Console.RESET should {
//    inSequence {
//      "disable a particular project part for this Section's users" in {
//
//      }
//    }
//  }
//}
