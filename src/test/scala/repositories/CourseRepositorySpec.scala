import java.awt.Color
import java.io.File
import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.{User, Course}
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.RowData
import grizzled.slf4j.Logger
import org.scalamock.scalatest.MockFactory
import org.scalatest.{MustMatchers, WordSpec, BeforeAndAfterAll, Suite}
import org.scalatest._
import Matchers._
import scala.collection._
import scala.collection.immutable.TreeMap
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scalaz.{-\/, \/-}


class CourseRepositorySpec
  extends TestEnvironment
{

  val userRepository = new UserRepositoryPostgres
  val courseRepository = new CourseRepositoryPostgres(userRepository)

   "CourseRepository.list" should {
     inSequence {
       "list all courses" in {
         val testCoursesList = TreeMap[Int, Course](
           0 -> TestValues.testCourseA,
           1 -> TestValues.testCourseB,
           2 -> TestValues.testCourseD,
           3 -> TestValues.testCourseF
         )

         val result = courseRepository.list
         val eitherCourses = Await.result(result, Duration.Inf)
         val \/-(courses) = eitherCourses

         testCoursesList.foreach {
           case (key, course: Course) => {
             courses(key).id should be(course.id)
             courses(key).teacherId should be(course.teacherId)
             courses(key).name should be(course.name)
             courses(key).color should be(course.color)
           }
         }

         courses.size should be(testCoursesList.size)
       }
       "return course by its project" in {
         val testProject = TestValues.testProjectC
         val testCoursesList = TreeMap[Int, Course](
           0 -> TestValues.testCourseB
         )

         val result = courseRepository.list(testProject)
         val eitherCourses = Await.result(result, Duration.Inf)
         val \/-(courses) = eitherCourses

         testCoursesList.foreach {
           case (key, course: Course) => {
             courses(key).id should be(course.id)
             courses(key).teacherId should be(course.teacherId)
             courses(key).name should be(course.name)
             courses(key).color should be(course.color)
           }
         }

         courses.size should be(testCoursesList.size)
       }
       "return empty Vector() if project doesn't exist" in {
         val testProject = TestValues.testProjectD

         val result = courseRepository.list(testProject)
         Await.result(result, Duration.Inf) should be(\/-(Vector()))
       }
       "select courses if user is a student (has record in 'users_courses' table) and (asTeacher = FALSE)" in {
         val testStudent = TestValues.testUserH
         val testCoursesList = TreeMap[Int, Course](
           0 -> TestValues.testCourseD,
           1 -> TestValues.testCourseF
         )

         val result = courseRepository.list(testStudent, false)
         val eitherCourses = Await.result(result, Duration.Inf)
         val \/-(courses) = eitherCourses

         testCoursesList.foreach {
           case (key, course: Course) => {
             courses(key).id should be(course.id)
             courses(key).teacherId should be(course.teacherId)
             courses(key).name should be(course.name)
             courses(key).color should be(course.color)
           }
         }

         courses.size should be(testCoursesList.size)
       }
       "return empty Vector() if user is a student (has record in 'users_courses' table) and (asTeacher = TRUE)" in {
         val testStudent = TestValues.testUserE

         val result = courseRepository.list(testStudent, true)
         Await.result(result, Duration.Inf) should be(\/-(Vector()))
       }
       "select courses if user is a teacher (hasn't record in 'users_courses' table) and (asTeacher = TRUE)" in {
         val testTeacher = TestValues.testUserF
         val testCoursesList = TreeMap[Int, Course](
           0 -> TestValues.testCourseD,
           1 -> TestValues.testCourseF
         )

         val result = courseRepository.list(testTeacher, true)
         val eitherCourses = Await.result(result, Duration.Inf)
         val \/-(courses) = eitherCourses

         testCoursesList.foreach {
           case (key, course: Course) => {
             courses(key).id should be(course.id)
             courses(key).teacherId should be(course.teacherId)
             courses(key).name should be(course.name)
             courses(key).color should be(course.color)
           }
         }

         courses.size should be(testCoursesList.size)
       }
       "return empty Vector() if user is a teacher (hasn't a record in 'users_courses' table) and (asTeacher = FALSE)" in {
         val testTeacher = TestValues.testUserA

         val result = courseRepository.list(testTeacher, false)
         Await.result(result, Duration.Inf) should be(\/-(Vector()))
       }
       "return empty Vector() if user doesn't exist and (asTeacher = FALSE)" in {
         val unexistingUser = User(
           email     = "unexisting_email@example.com",
           username  = "unexisting_username",
           givenname = "unexisting_givenname",
           surname   = "unexisting_surname"
         )

         val result = courseRepository.list(unexistingUser, false)
         Await.result(result, Duration.Inf) should be(\/-(Vector()))
       }
       "return empty Vector() if user doesn't exist and (asTeacher = TRUE)" in {
         val unexistingUser = User(
           email     = "unexisting_email@example.com",
           username  = "unexisting_username",
           givenname = "unexisting_givenname",
           surname   = "unexisting_surname"
         )

         val result = courseRepository.list(unexistingUser, true)
         Await.result(result, Duration.Inf) should be(\/-(Vector()))
       }
       "list the courses associated with a users" in {
         val testStudentList = TreeMap[Int, User](
           0 -> TestValues.testUserC,
           1 -> TestValues.testUserE
         )

         val testCoursesList = Map[UUID, Vector[Course]](
           testStudentList(0).id -> Vector(
             TestValues.testCourseA,
             TestValues.testCourseB
           ),
           testStudentList(1).id -> Vector(
             TestValues.testCourseB
           )
         )

         val result = courseRepository.list(testStudentList.map(_._2)(breakOut))
         val eitherCourses = Await.result(result, Duration.Inf)
         val \/-(courses) = eitherCourses

         testCoursesList.foreach {
           case (userId: UUID, coursesList: Vector[Course]) => {
             var key = 0
             for (course: Course <- coursesList) {
               courses(userId)(key).id should be(course.id)
               courses(userId)(key).teacherId should be(course.teacherId)
               courses(userId)(key).name should be(course.name)
               courses(userId)(key).color should be(course.color)
               key = key + 1
             }
           }
         }

         courses.size should be(testCoursesList.size)
       }
       "return empty Vector() if user doesn't exist" in {
         val testUsersList = Vector(
           User(
             email     = "unexisting_email@example.com",
             username  = "unexisting_username",
             givenname = "unexisting_givenname",
             surname   = "unexisting_surname"
           )
         )

         val result = courseRepository.list(testUsersList)
         Await.result(result, Duration.Inf) should be(\/-(Map(
           testUsersList(0).id -> Vector()
         )))
       }
       "return empty Vector() if user is teacher (doesn't have references in users_courses table)" in {
         val testTeacherList = Vector(
           TestValues.testUserF
         )

         val result = courseRepository.list(testTeacherList)
         Await.result(result, Duration.Inf) should be(\/-(Map(
           TestValues.testUserF.id -> Vector()
         )))
       }
     }
   }

   "CourseRepository.find" should {
     inSequence {
       "find a single entry by ID" in {
         val testCourse = TestValues.testCourseA

         val result       = courseRepository.find(testCourse.id)
         val eitherCourse = Await.result(result, Duration.Inf)
         val \/-(course)  = eitherCourse

         eitherCourse.toString should be(\/-(testCourse).toString)

         course.id should be(testCourse.id)
         course.version should be(testCourse.version)
         course.name should be(testCourse.name)
         course.createdAt.toString should be(testCourse.createdAt.toString)
         course.updatedAt.toString should be(testCourse.updatedAt.toString)
       }
       "return RepositoryError.NoResults if entry wasn't found by ID" in {
         val testCourse = TestValues.testCourseC

         val result = courseRepository.find(testCourse.id)
         Await.result(result, Duration.Inf) should be (-\/(RepositoryError.NoResults))
       }
     }
   }
//
//   "CourseRepository.addUser" should {
//     inSequence {
//       "add user to a course" in {
//         val result = courseRepository.addUser(TestValues.testUserC, TestValues.testCourseA)
//         Await.result(result, Duration.Inf) should be(true)
//
//         // Check
//         val queryResult = db.pool.sendPreparedStatement(countUserInCourse, Array[Any](TestValues.testUserC.id.bytes, TestValues.testCourseA.id.bytes))
//         val rowsAffected = Await.result(queryResult, Duration.Inf).rowsAffected
//
//         rowsAffected should be (1)
//       }
//       "throw a GenericDatabaseException if user is already in the course" in {
//         val result = courseRepository.addUser(TestValues.testUserE, TestValues.testCourseB)
//
//         an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
//       }
//       "throw a GenericDatabaseException if user unexists" in {
//         val result = courseRepository.addUser(TestValues.testUserD, TestValues.testCourseB)
//
//         an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
//       }
//       "throw a GenericDatabaseException if course unexists" in {
//         val result = courseRepository.addUser(TestValues.testUserE, TestValues.testCourseC)
//
//         an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
//       }
//     }
//   }
//
//   "CourseRepository.removeUser" should {
//     inSequence {
//       "remove a user from a course" in {
//         // Check if user present
//         val queryResultIs = db.pool.sendPreparedStatement(countUserInCourse, Array[Any](TestValues.testUserA.id.bytes, TestValues.testCourseA.id.bytes))
//         val rowsAffectedIs = Await.result(queryResultIs, Duration.Inf).rowsAffected
//
//         rowsAffectedIs should be (1)
//
//         val result = courseRepository.removeUser(TestValues.testUserA, TestValues.testCourseA)
//         Await.result(result, Duration.Inf) should be(true)
//
//         // Check
//         val queryResult = db.pool.sendPreparedStatement(countUserInCourse, Array[Any](TestValues.testUserA.id.bytes, TestValues.testCourseA.id.bytes))
//         val rowsAffected = Await.result(queryResult, Duration.Inf).rowsAffected
//
//         rowsAffected should be (0)
//       }
//       "be FALSE if user unexists" in {
//         val result = courseRepository.removeUser(TestValues.testUserD, TestValues.testCourseB)
//
//         Await.result(result, Duration.Inf) should be(false)
//       }
//       "be FALSE if course unexists" in {
//         val result = courseRepository.removeUser(TestValues.testUserA, TestValues.testCourseC)
//
//         Await.result(result, Duration.Inf) should be(false)
//       }
//     }
//   }
//
//   "CourseRepository.hasProject" should {
//     inSequence {
//       "verify if this user has access to this project through any of his courses" in {
//         val result = courseRepository.hasProject(TestValues.testUserE, TestValues.testProjectB)
//
//         Await.result(result, Duration.Inf) should be(true)
//       }
//       "be FALSE if user is not in a project's course" in {
//         val result = courseRepository.hasProject(TestValues.testUserE, TestValues.testProjectA)
//
//         Await.result(result, Duration.Inf) should be(false)
//       }
//       "be FALSE if user unexists" in {
//         val result = courseRepository.hasProject(TestValues.testUserD, TestValues.testProjectA)
//
//         Await.result(result, Duration.Inf) should be(false)
//       }
//       "be FALSE if project unexists" in {
//         val result = courseRepository.hasProject(TestValues.testUserD, TestValues.testProjectD)
//
//         Await.result(result, Duration.Inf) should be(false)
//       }
//     }
//   }
//
//   "CourseRepository.addUsers" should {
//     inSequence {
//       "add users to a `course`" in {
//         val result = courseRepository.addUsers(TestValues.testCourseD, Vector(TestValues.testUserA, TestValues.testUserB))
//
//         Await.result(result, Duration.Inf) should be(true)
//
//         // Check User A in course D
//         val queryResultUserA = db.pool.sendPreparedStatement(countUserInCourse, Array[Any](TestValues.testUserA.id.bytes, TestValues.testCourseD.id.bytes))
//         val rowsAffectedUserA = Await.result(queryResultUserA, Duration.Inf).rowsAffected
//
//         rowsAffectedUserA should be (1)
//
//         // Check User B in course D
//         val queryResultUserB = db.pool.sendPreparedStatement(countUserInCourse, Array[Any](TestValues.testUserB.id.bytes, TestValues.testCourseD.id.bytes))
//         val rowsAffectedUserB = Await.result(queryResultUserB, Duration.Inf).rowsAffected
//
//         rowsAffectedUserB should be (1)
//       }
//       "throw a GenericDatabaseException if user is already in the course" in {
//         val result = courseRepository.addUsers(TestValues.testCourseB, Vector(TestValues.testUserE))
//
//         an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
//       }
//       "throw a GenericDatabaseException if user unexists" in {
//         val result = courseRepository.addUsers(TestValues.testCourseB, Vector(TestValues.testUserD))
//
//         an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
//       }
//       "throw a GenericDatabaseException if course unexists" in {
//         val result = courseRepository.addUsers(TestValues.testCourseC, Vector(TestValues.testUserE))
//
//         an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
//       }
//     }
//   }
//
//   "CourseRepository.removeUsers" should {
//     inSequence {
//       "Remove users from a course" in {
//         // Check User B in course B
//         val queryResultIsUserB = db.pool.sendPreparedStatement(countUserInCourse, Array[Any](TestValues.testUserB.id.bytes, TestValues.testCourseB.id.bytes))
//         val rowsAffectedIsUserB = Await.result(queryResultIsUserB, Duration.Inf).rowsAffected
//
//         rowsAffectedIsUserB should be (1)
//
//         // Check User E in course B
//         val queryResultIsUserE = db.pool.sendPreparedStatement(countUserInCourse, Array[Any](TestValues.testUserE.id.bytes, TestValues.testCourseB.id.bytes))
//         val rowsAffectedIsUserE = Await.result(queryResultIsUserE, Duration.Inf).rowsAffected
//
//         rowsAffectedIsUserE should be (1)
//
//         val result = courseRepository.removeUsers(TestValues.testCourseB, Vector(TestValues.testUserB, TestValues.testUserE))
//
//         Await.result(result, Duration.Inf) should be(true)
//
//         // Check User B in course B
//         val queryResultUserB = db.pool.sendPreparedStatement(countUserInCourse, Array[Any](TestValues.testUserB.id.bytes, TestValues.testCourseB.id.bytes))
//         val rowsAffectedUserB = Await.result(queryResultUserB, Duration.Inf).rowsAffected
//
//         rowsAffectedUserB should be (0)
//
//         // Check User E in course B
//         val queryResultUserE = db.pool.sendPreparedStatement(countUserInCourse, Array[Any](TestValues.testUserE.id.bytes, TestValues.testCourseB.id.bytes))
//         val rowsAffectedUserE = Await.result(queryResultUserE, Duration.Inf).rowsAffected
//
//         rowsAffectedUserE should be (0)
//       }
//       "be FALSE if user is not in the course" in {
//         val result = courseRepository.removeUsers(TestValues.testCourseA, Vector(TestValues.testUserE))
//
//         Await.result(result, Duration.Inf) should be(false)
//       }
//       "be FALSE if user unexists" in {
//         val result = courseRepository.removeUsers(TestValues.testCourseB, Vector(TestValues.testUserD))
//
//         Await.result(result, Duration.Inf) should be(false)
//       }
//       "be FALSE if course unexists" in {
//         val result = courseRepository.removeUsers(TestValues.testCourseC, Vector(TestValues.testUserE))
//
//         Await.result(result, Duration.Inf) should be(false)
//       }
//     }
//   }
//
//   "CourseRepository.removeAllUsers" should {
//     inSequence {
//       "remove all users from a course" in {
//         // Check User G in course B
//         val queryResultIsUserG = db.pool.sendPreparedStatement(countUserInCourse, Array[Any](TestValues.testUserG.id.bytes, TestValues.testCourseB.id.bytes))
//         val rowsAffectedIsUserG = Await.result(queryResultIsUserG, Duration.Inf).rowsAffected
//
//         rowsAffectedIsUserG should be (1)
//
//         // Check User H in course B
//         val queryResultIsUserH = db.pool.sendPreparedStatement(countUserInCourse, Array[Any](TestValues.testUserH.id.bytes, TestValues.testCourseB.id.bytes))
//         val rowsAffectedIsUserH = Await.result(queryResultIsUserH, Duration.Inf).rowsAffected
//
//         rowsAffectedIsUserH should be (1)
//
//
//         val result = courseRepository.removeAllUsers(TestValues.testCourseB)
//
//         Await.result(result, Duration.Inf) should be(true)
//
//         // Check User G in course B
//         val queryResultUserG = db.pool.sendPreparedStatement(countUserInCourse, Array[Any](TestValues.testUserG.id.bytes, TestValues.testCourseB.id.bytes))
//         val rowsAffectedUserG = Await.result(queryResultUserG, Duration.Inf).rowsAffected
//
//         rowsAffectedUserG should be (0)
//
//         // Check User H in course B
//         val queryResultUserH = db.pool.sendPreparedStatement(countUserInCourse, Array[Any](TestValues.testUserH.id.bytes, TestValues.testCourseB.id.bytes))
//         val rowsAffectedUserH = Await.result(queryResultUserH, Duration.Inf).rowsAffected
//
//         rowsAffectedUserH should be (0)
//       }
//       "be FALSE if course unexists" in {
//         val result = courseRepository.removeAllUsers(TestValues.testCourseC)
//
//         Await.result(result, Duration.Inf) should be(false)
//       }
//     }
//   }
//
//   "CourseRepository.insert" should {
//     inSequence {
//       "insert new course" in {
//         val result = courseRepository.insert(TestValues.testCourseE)
//
//         val newCourse = Await.result(result, Duration.Inf)
//
//         newCourse.id should be (TestValues.testCourseE.id)
//         newCourse.teacherId should be (TestValues.testCourseE.teacherId)
//         newCourse.version should be (1L)
//         newCourse.name should be (TestValues.testCourseE.name)
//         newCourse.color should be (TestValues.testCourseE.color)
//
//         // Check
//         val queryResult = db.pool.sendPreparedStatement(SelectOne, Array[Any](TestValues.testCourseE.id.bytes)).map { queryResult =>
//           val courseList = queryResult.rows.get.map {
//             item: RowData => Course(item)
//           }
//           courseList
//         }
//
//         val courseList = Await.result(queryResult, Duration.Inf)
//
//         courseList(0).id should be(TestValues.testCourseE.id)
//         courseList(0).teacherId should be(TestValues.testCourseE.teacherId)
//         courseList(0).version should be(1L)
//         courseList(0).name should be(TestValues.testCourseE.name)
//         courseList(0).color should be(TestValues.testCourseE.color)
//       }
//       "throw a GenericDatabaseException if course has unexisting teacher id" in {
//         val result = courseRepository.insert(TestValues.testCourseC.copy(
//           teacherId = Option(UUID("9d8ed645-b055-4a69-ab7d-387791c1e064"))
//         ))
//
//         an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
//       }
//       "throw a GenericDatabaseException if course already exists" in {
//         val result = courseRepository.insert(TestValues.testCourseA)
//
//         an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
//       }
//     }
//   }
//
//   "CourseRepository.update" should {
//     inSequence {
//       "update existing course" in {
//         val result = courseRepository.update(TestValues.testCourseA.copy(
//           teacherId = Option(TestValues.testUserG.id),
//           name = "new test course A name",
//           color = new Color(78, 40, 23)
//         ))
//
//         val updatedCourse = Await.result(result, Duration.Inf)
//
//         updatedCourse.id should be (TestValues.testCourseA.id)
//         updatedCourse.teacherId should be (Some(TestValues.testUserG.id)) // TODO - check why SOME?
//         updatedCourse.version should be (TestValues.testCourseA.version + 1)
//         updatedCourse.name should be ("new test course A name")
//         updatedCourse.color should be (new Color(78, 40, 23))
//
//         // Check
//         val queryResult = db.pool.sendPreparedStatement(SelectOne, Array[Any](TestValues.testCourseA.id.bytes)).map { queryResult =>
//           val courseList = queryResult.rows.get.map {
//             item: RowData => Course(item)
//           }
//           courseList
//         }
//
//         val courseList = Await.result(queryResult, Duration.Inf)
//
//         courseList(0).id should be(TestValues.testCourseA.id)
//         courseList(0).teacherId should be(Some(TestValues.testUserG.id))
//         courseList(0).version should be(TestValues.testCourseA.version + 1)
//         courseList(0).name should be("new test course A name")
//         courseList(0).color should be(new Color(78, 40, 23))
//       }
//       "throw a NoSuchElementException when update an existing Course with wrong version" in {
//         val result = courseRepository.update(TestValues.testCourseA.copy(
//           version = 99L,
//           name = "new test course A name"
//         ))
//
//         an[java.util.NoSuchElementException] should be thrownBy Await.result(result, Duration.Inf)
//       }
//       "throw a NoSuchElementException when update an unexisting Course" in {
//         val result = courseRepository.update(Course(
//           teacherId = Option(TestValues.testUserG.id),
//           name = "new test course A name",
//           color = new Color(8, 4, 3)
//         ))
//
//         an[java.util.NoSuchElementException] should be thrownBy Await.result(result, Duration.Inf)
//       }
//     }
//   }
//
//   "CourseRepository.delete" should {
//     inSequence {
//       "delete course if course has no references" in {
//         val result = courseRepository.delete(TestValues.testCourseF)
//
//         Await.result(result, Duration.Inf) should be(true)
//
//         // Check
//         val queryResult = db.pool.sendPreparedStatement(SelectOne, Array[Any](TestValues.testCourseF.id.bytes)).map { queryResult =>
//           val courseList = queryResult.rows.get.map {
//             item: RowData => Course(item)
//           }
//           courseList
//         }
//
//         Await.result(queryResult, Duration.Inf) should be(Vector())
//       }
//       "delete course if course has references in users_courses table" in {
//         val result = courseRepository.delete(TestValues.testCourseD)
//
//         Await.result(result, Duration.Inf) should be(true)
//
//         // Check
//         val queryResult = db.pool.sendPreparedStatement(SelectOne, Array[Any](TestValues.testCourseD.id.bytes)).map { queryResult =>
//           val courseList = queryResult.rows.get.map {
//             item: RowData => Course(item)
//           }
//           courseList
//         }
//
//         Await.result(queryResult, Duration.Inf) should be(Vector())
//       }
//       "throw a GenericDatabaseException if course has references in projects table" in {
//         val result = courseRepository.delete(TestValues.testCourseB)
//
//         an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
//       }
//       "return FALSE if Course hasn't been found" in {
//         val result = courseRepository.delete(Course(
//           teacherId = Option(TestValues.testUserG.id),
//           name = "new test course A name",
//           color = new Color(8, 4, 3)
//         ))
//
//         Await.result(result, Duration.Inf) should be(false)
//       }
//     }
//   }
//
//   // TODO - delete
//   "CourseRepository.enablePart"  + Console.RED + Console.BOLD + " (NOTE: Method is deprecated, talbe COURSES_PROJECTS will be deleted) " + Console.RESET should {
//     inSequence {
//       "enable a particular project part for this Section's users" in {
//
//       }
//     }
//   }
//
//   // TODO - delete
//   "CourseRepository.disablePart" + Console.RED + Console.BOLD + " (NOTE: Method is deprecated, talbe COURSES_PROJECTS will be deleted) " + Console.RESET should {
//     inSequence {
//       "disable a particular project part for this Section's users" in {
//
//       }
//     }
//   }
}

