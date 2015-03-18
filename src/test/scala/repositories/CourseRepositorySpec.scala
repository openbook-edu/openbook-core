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
           3 -> TestValues.testCourseF,
           4 -> TestValues.testCourseG
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
           1 -> TestValues.testCourseF,
           2 -> TestValues.testCourseG
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

   "CourseRepository.addUser" should {
     inSequence {
       "add user to a course" in {
         val testUser = TestValues.testUserE
         val testCourse = TestValues.testCourseA

         val result = courseRepository.addUser(testUser, testCourse)
         Await.result(result, Duration.Inf) should be(\/-( () ))
       }
       "return RepositoryError.PrimaryKeyConflict if user is already in the course" in {
         val testUser = TestValues.testUserE
         val testCourse = TestValues.testCourseB

         val result = courseRepository.addUser(testUser, testCourse)
         Await.result(result, Duration.Inf) should be(-\/(RepositoryError.PrimaryKeyConflict))
       }
       "return RepositoryError.ForeignKeyConflict if user doesn't exist" in {
         val testUser = TestValues.testUserD
         val testCourse = TestValues.testCourseA

         val result = courseRepository.addUser(testUser, testCourse)
         Await.result(result, Duration.Inf) should be(-\/(RepositoryError.ForeignKeyConflict("user_id", "users_courses_user_id_fkey")))
       }
       "return RepositoryError.ForeignKeyConflict if course doesn't exist" in {
         val testUser = TestValues.testUserE
         val testCourse = TestValues.testCourseC

         val result = courseRepository.addUser(testUser, testCourse)
         Await.result(result, Duration.Inf) should be(-\/(RepositoryError.ForeignKeyConflict("course_id", "users_courses_course_id_fkey")))
       }
     }
   }

   "CourseRepository.removeUser" should {
     inSequence {
       "remove a user from a course" in {
         val testUser   = TestValues.testUserC
         val testCourse = TestValues.testCourseA

         val result = courseRepository.removeUser(testUser, testCourse)
         Await.result(result, Duration.Inf) should be(\/-( () ))
       }
       "be FALSE if user doesn't attend the course" in {
         val testUser   = TestValues.testUserC
         val testCourse = TestValues.testCourseF

         val result = courseRepository.removeUser(testUser, testCourse)
         Await.result(result, Duration.Inf) should be(-\/(RepositoryError.DatabaseError("The query succeeded but the user could not be removed from the course.")))
       }
       "be FALSE if user unexists" in {
         val testUser   = TestValues.testUserD
         val testCourse = TestValues.testCourseA

         val result = courseRepository.removeUser(testUser, testCourse)
         Await.result(result, Duration.Inf) should be(-\/(RepositoryError.DatabaseError("The query succeeded but the user could not be removed from the course.")))
       }
       "be FALSE if course unexists" in {
         val testUser   = TestValues.testUserC
         val testCourse = TestValues.testCourseC

         val result = courseRepository.removeUser(testUser, testCourse)
         Await.result(result, Duration.Inf) should be(-\/(RepositoryError.DatabaseError("The query succeeded but the user could not be removed from the course.")))
       }
     }
   }

   "CourseRepository.hasProject" should {
     inSequence {
       "verify if this user has access to this project through any of his courses" in {
         val testProject = TestValues.testProjectB
         val testUser    = TestValues.testUserE

         val result = courseRepository.hasProject(testUser, testProject)
         Await.result(result, Duration.Inf) should be(\/-(true))
       }
       "be FALSE if user doesn't have access to this project through any of his courses" in {
         val testProject = TestValues.testProjectA
         val testUser    = TestValues.testUserE

         val result = courseRepository.hasProject(testUser, testProject)
         Await.result(result, Duration.Inf) should be(\/-(false))
       }
       "be FALSE if user unexists" in {
         val testProject = TestValues.testProjectB
         val testUser    = TestValues.testUserD

         val result = courseRepository.hasProject(testUser, testProject)
         Await.result(result, Duration.Inf) should be(\/-(false))
       }
       "be FALSE if project unexists" in {
         val testProject = TestValues.testProjectD
         val testUser    = TestValues.testUserE

         val result = courseRepository.hasProject(testUser, testProject)
         Await.result(result, Duration.Inf) should be(\/-(false))
       }
     }
   }

   "CourseRepository.addUsers" should {
     inSequence {
       "add users to a course" in {
         val testCourse = TestValues.testCourseD
         val testUserList =  Vector(
           TestValues.testUserC,
           TestValues.testUserE
         )

         val result = courseRepository.addUsers(testCourse, testUserList)
         Await.result(result, Duration.Inf) should be(\/-( () ))
       }
       "return RepositoryError.PrimaryKeyConflict if one of the users is already in the course" in {
         val testCourse = TestValues.testCourseF
         val testUserList =  Vector(
           TestValues.testUserE,
           TestValues.testUserG
         )

         val result = courseRepository.addUsers(testCourse, testUserList)
         Await.result(result, Duration.Inf) should be(-\/(RepositoryError.PrimaryKeyConflict))
       }
       "return RepositoryError.ForeignKeyConflict if one of the users doesn't exist" in {
         val testCourse = TestValues.testCourseF
         val testUserList =  Vector(
           TestValues.testUserE,
           TestValues.testUserD
         )

         val result = courseRepository.addUsers(testCourse, testUserList)
         Await.result(result, Duration.Inf) should be(-\/(RepositoryError.ForeignKeyConflict("user_id", "users_courses_user_id_fkey")))
       }
       "return RepositoryError.ForeignKeyConflict if course doesn't exist" in {
         val testCourse = TestValues.testCourseE
         val testUserList =  Vector(
           TestValues.testUserE,
           TestValues.testUserG
         )

         val result = courseRepository.addUsers(testCourse, testUserList)
         Await.result(result, Duration.Inf) should be(-\/(RepositoryError.ForeignKeyConflict("course_id", "users_courses_course_id_fkey")))
       }
     }
   }

   "CourseRepository.removeUsers" should {
     inSequence {
       "Remove users from a course" in {
         val testCourse = TestValues.testCourseB
         val testUserList =  Vector(
           TestValues.testUserE,
           TestValues.testUserC
         )

         val result = courseRepository.removeUsers(testCourse, testUserList)
         Await.result(result, Duration.Inf) should be(\/-( () ))
       }
       "return RepositoryError.DatabaseError if one of the users is not in the course" in {
         val testCourse = TestValues.testCourseB
         val testUserList =  Vector(
           TestValues.testUserE,
           TestValues.testUserG
         )

         val result = courseRepository.removeUsers(testCourse, testUserList)
         Await.result(result, Duration.Inf) should be(-\/(RepositoryError.DatabaseError("The query succeeded but the users could not be removed from the course.", None)))
       }
       "return RepositoryError.DatabaseError if one of the users doesn't exist" in {
         val testCourse = TestValues.testCourseB
         val testUserList =  Vector(
           TestValues.testUserE,
           TestValues.testUserD
         )

         val result = courseRepository.removeUsers(testCourse, testUserList)
         Await.result(result, Duration.Inf) should be(-\/(RepositoryError.DatabaseError("The query succeeded but the users could not be removed from the course.", None)))
       }
       "return RepositoryError.DatabaseError if course unexists" in {
         val testCourse = TestValues.testCourseC
         val testUserList =  Vector(
           TestValues.testUserE,
           TestValues.testUserC
         )

         val result = courseRepository.removeUsers(testCourse, testUserList)
         Await.result(result, Duration.Inf) should be(-\/(RepositoryError.DatabaseError("The query succeeded but the users could not be removed from the course.", None)))
       }
     }
   }

   "CourseRepository.removeAllUsers" should {
     inSequence {
       "remove all users from a course" in {
         val testCourse = TestValues.testCourseB

         val result = courseRepository.removeAllUsers(testCourse)
         Await.result(result, Duration.Inf) should be(\/-( () ))
       }
       "be FALSE if course doesn't have users" in {
         val testCourse = TestValues.testCourseG

         val result = courseRepository.removeAllUsers(testCourse)
         Await.result(result, Duration.Inf) should be(-\/(RepositoryError.DatabaseError("No rows were affected", None)))
       }
       "be FALSE if course doesn't exist" in {
         val testCourse = TestValues.testCourseE

         val result = courseRepository.removeAllUsers(testCourse)
         Await.result(result, Duration.Inf) should be(-\/(RepositoryError.DatabaseError("No rows were affected", None)))
       }
     }
   }

   "CourseRepository.insert" should {
     inSequence {
       "insert new course" in {
         val testCourse = TestValues.testCourseE

         val result = courseRepository.insert(testCourse)
         val eitherCourse = Await.result(result, Duration.Inf)
         val \/-(course) = eitherCourse

         course.id should be (testCourse.id)
         course.teacherId should be (testCourse.teacherId)
         course.version should be (1L)
         course.name should be (testCourse.name)
         course.color should be (testCourse.color)
       }
       "return RepositoryError.ForeignKeyConflict if course has unexisting teacher id" in {
         val testCourse = TestValues.testCourseE.copy(
           teacherId = UUID("9d8ed645-b055-4a69-ab7d-387791c1e064")
         )

         val result = courseRepository.insert(testCourse)
         Await.result(result, Duration.Inf) should be(-\/(RepositoryError.ForeignKeyConflict("teacher_id", "courses_teacher_id_fkey")))
       }
       "return RepositoryError.PrimaryKeyConflict if course already exists" in {
         val testCourse = TestValues.testCourseA

         val result = courseRepository.insert(testCourse)
         Await.result(result, Duration.Inf) should be(-\/(RepositoryError.PrimaryKeyConflict))
       }
     }
   }

   "CourseRepository.update" should {
     inSequence {
       "update existing course" in {
         val testCourse    = TestValues.testCourseA
         val updatedCourse = testCourse.copy(
           teacherId = TestValues.testCourseB.teacherId,
           name      = "new test course name",
           color     = new Color(78, 40, 23)
         )

         val result = courseRepository.update(updatedCourse)
         val eitherCourse = Await.result(result, Duration.Inf)
         val \/-(course) = eitherCourse

         course.id should be (updatedCourse.id)
         course.teacherId should be (updatedCourse.teacherId)
         course.version should be (updatedCourse.version + 1)
         course.name should be (updatedCourse.name)
         course.color should be (updatedCourse.color)
       }
       "return RepositoryError.NoResults when update an existing Course with wrong version" in {
         val testCourse    = TestValues.testCourseA
         val updatedCourse = testCourse.copy(
           teacherId = TestValues.testCourseB.teacherId,
           name      = "new test course name",
           color     = new Color(78, 40, 23),
           version   = 99L
         )

         val result = courseRepository.update(updatedCourse)
         Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
       }
       "return RepositoryError.NoResults when update a Course that doesn't exist" in {
         val testCourse    = TestValues.testCourseC
         val updatedCourse = testCourse.copy(
           teacherId = TestValues.testCourseB.teacherId,
           name      = "new test course name",
           color     = new Color(78, 40, 23)
         )

         val result = courseRepository.update(updatedCourse)
         Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
       }
     }
   }

   "CourseRepository.delete" should {
     inSequence {
       "delete course if course has no references" in {
         val testCourse = TestValues.testCourseG

         val result = courseRepository.delete(testCourse)
         val eitherCourse = Await.result(result, Duration.Inf)
         val \/-(course) = eitherCourse

         course.id should be (testCourse.id)
         course.teacherId should be (testCourse.teacherId)
         course.version should be (testCourse.version)
         course.name should be (testCourse.name)
         course.color should be (testCourse.color)
       }
       "delete course if course has references only in users_courses table" in {
         val testCourse = TestValues.testCourseF

         val result = courseRepository.delete(testCourse)
         val eitherCourse = Await.result(result, Duration.Inf)
         val \/-(course) = eitherCourse

         course.id should be (testCourse.id)
         course.teacherId should be (testCourse.teacherId)
         course.version should be (testCourse.version)
         course.name should be (testCourse.name)
         course.color should be (testCourse.color)
       }
       "return RepositoryError.ForeignKeyConflict if course has references in projects table" in {
         val testCourse = TestValues.testCourseA

         val result = courseRepository.delete(testCourse)
         Await.result(result, Duration.Inf) should be(-\/(RepositoryError.ForeignKeyConflict("course_id", "projects_course_id_fkey")))
       }
       "return RepositoryError.NoResults if Course hasn't been found" in {
         val testCourse = TestValues.testCourseE

         val result = courseRepository.delete(testCourse)
         Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
       }
     }
   }
}

