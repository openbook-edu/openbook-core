import java.awt.Color
import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.{ Course, User }
import ca.shiftfocus.krispii.core.repositories._
import org.scalatest.Matchers._
import org.scalatest._

import scala.collection._
import scala.collection.immutable.TreeMap
import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }
import scalaz.{ -\/, \/- }

class CourseRepositorySpec
    extends TestEnvironment {
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
        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))

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
        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))

        val testStudent = TestValues.testUserE

        val result = courseRepository.list(testStudent, true)
        Await.result(result, Duration.Inf) should be(\/-(Vector()))
      }
      "select courses if user is a teacher (hasn't record in 'users_courses' table) and (asTeacher = TRUE)" in {
        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))

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
        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))

        val testTeacher = TestValues.testUserA

        val result = courseRepository.list(testTeacher, false)
        Await.result(result, Duration.Inf) should be(\/-(Vector()))
      }
      "return empty Vector() if user doesn't exist and (asTeacher = FALSE)" in {
        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))

        val unexistingUser = User(
          email = "unexisting_email@example.com",
          username = "unexisting_username",
          givenname = "unexisting_givenname",
          surname = "unexisting_surname"
        )

        val result = courseRepository.list(unexistingUser, false)
        Await.result(result, Duration.Inf) should be(\/-(Vector()))
      }
      "return empty Vector() if user doesn't exist and (asTeacher = TRUE)" in {
        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))

        val unexistingUser = User(
          email = "unexisting_email@example.com",
          username = "unexisting_username",
          givenname = "unexisting_givenname",
          surname = "unexisting_surname"
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
            TestValues.testCourseA,
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
            email = "unexisting_email@example.com",
            username = "unexisting_username",
            givenname = "unexisting_givenname",
            surname = "unexisting_surname"
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
        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))

        val testCourse = TestValues.testCourseA

        val result = courseRepository.find(testCourse.id)
        val eitherCourse = Await.result(result, Duration.Inf)
        val \/-(course) = eitherCourse

        eitherCourse.toString should be(\/-(testCourse).toString)

        course.id should be(testCourse.id)
        course.version should be(testCourse.version)
        course.name should be(testCourse.name)
        course.createdAt.toString should be(testCourse.createdAt.toString)
        course.updatedAt.toString should be(testCourse.updatedAt.toString)
      }
      "return RepositoryError.NoResults if entry wasn't found by ID" in {
        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))

        val testCourse = TestValues.testCourseC

        val result = courseRepository.find(testCourse.id)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Course")))
      }
    }
  }

  "CourseRepository.addUser" should {
    inSequence {
      "add user to a course" in {
        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val testUser = TestValues.testUserE
        val testCourse = TestValues.testCourseF

        val result = courseRepository.addUser(testUser, testCourse)
        Await.result(result, Duration.Inf) should be(\/-(()))
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
        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val testUser = TestValues.testUserC
        val testCourse = TestValues.testCourseA

        val result = courseRepository.removeUser(testUser, testCourse)
        Await.result(result, Duration.Inf) should be(\/-(()))
      }
      "be FALSE if user doesn't attend the course" in {
        val testUser = TestValues.testUserC
        val testCourse = TestValues.testCourseF

        val result = courseRepository.removeUser(testUser, testCourse)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.DatabaseError("The query succeeded but the user could not be removed from the course.")))
      }
      "be FALSE if user unexists" in {
        val testUser = TestValues.testUserD
        val testCourse = TestValues.testCourseA

        val result = courseRepository.removeUser(testUser, testCourse)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.DatabaseError("The query succeeded but the user could not be removed from the course.")))
      }
      "be FALSE if course unexists" in {
        val testUser = TestValues.testUserC
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
        val testUser = TestValues.testUserE

        val result = courseRepository.hasProject(testUser, testProject)
        Await.result(result, Duration.Inf) should be(\/-(true))
      }
      "be FALSE if user doesn't have access to this project through any of his courses" in {
        val testProject = TestValues.testProjectA
        val testUser = TestValues.testUserG

        val result = courseRepository.hasProject(testUser, testProject)
        Await.result(result, Duration.Inf) should be(\/-(false))
      }
      "be FALSE if user unexists" in {
        val testProject = TestValues.testProjectB
        val testUser = TestValues.testUserD

        val result = courseRepository.hasProject(testUser, testProject)
        Await.result(result, Duration.Inf) should be(\/-(false))
      }
      "be FALSE if project unexists" in {
        val testProject = TestValues.testProjectD
        val testUser = TestValues.testUserE

        val result = courseRepository.hasProject(testUser, testProject)
        Await.result(result, Duration.Inf) should be(\/-(false))
      }
    }
  }

  "CourseRepository.addUsers" should {
    inSequence {
      "add users to a course" in {
        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val testCourse = TestValues.testCourseD
        val testUserList = Vector(
          TestValues.testUserC,
          TestValues.testUserE
        )

        val result = courseRepository.addUsers(testCourse, testUserList)
        Await.result(result, Duration.Inf) should be(\/-(()))
      }
      "return RepositoryError.PrimaryKeyConflict if one of the users is already in the course" in {
        val testCourse = TestValues.testCourseF
        val testUserList = Vector(
          TestValues.testUserE,
          TestValues.testUserG
        )

        val result = courseRepository.addUsers(testCourse, testUserList)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.PrimaryKeyConflict))
      }
      "return RepositoryError.ForeignKeyConflict if one of the users doesn't exist" in {
        val testCourse = TestValues.testCourseF
        val testUserList = Vector(
          TestValues.testUserE,
          TestValues.testUserD
        )

        val result = courseRepository.addUsers(testCourse, testUserList)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.ForeignKeyConflict("user_id", "users_courses_user_id_fkey")))
      }
      "return RepositoryError.ForeignKeyConflict if course doesn't exist" in {
        val testCourse = TestValues.testCourseE
        val testUserList = Vector(
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
        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val testCourse = TestValues.testCourseB
        val testUserList = Vector(
          TestValues.testUserE,
          TestValues.testUserC
        )
        val testUserDeleteList = Vector(
          testUserList(0)
        )

        // Check if users are in the course
        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))

        testUserList.foreach(user => {
          var \/-(courseList) = Await.result(courseRepository.list(user), Duration.Inf)
          courseList.exists(_.id == testCourse.id) should be(true)
        })

        val result = courseRepository.removeUsers(testCourse, testUserDeleteList)
        Await.result(result, Duration.Inf) should be(\/-(()))

        // Check if users where deleted from the course
        testUserList.foreach(user => {
          if (testUserDeleteList.contains(user)) {
            var \/-(courseList) = Await.result(courseRepository.list(user), Duration.Inf)
            courseList.exists(_.id == testCourse.id) should be(false)
          }
        })
      }
      "return RepositoryError.DatabaseError if one of the users is not in the course" in {
        val testCourse = TestValues.testCourseB
        val testUserList = Vector(
          TestValues.testUserE,
          TestValues.testUserG
        )

        val result = courseRepository.removeUsers(testCourse, testUserList)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.DatabaseError("The query succeeded but the users could not be removed from the course.", None)))
      }
      "return RepositoryError.DatabaseError if one of the users doesn't exist" in {
        val testCourse = TestValues.testCourseB
        val testUserList = Vector(
          TestValues.testUserE,
          TestValues.testUserD
        )

        val result = courseRepository.removeUsers(testCourse, testUserList)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.DatabaseError("The query succeeded but the users could not be removed from the course.", None)))
      }
      "return RepositoryError.DatabaseError if course unexists" in {
        val testCourse = TestValues.testCourseC
        val testUserList = Vector(
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
        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val testCourse = TestValues.testCourseB

        val result = courseRepository.removeAllUsers(testCourse)
        Await.result(result, Duration.Inf) should be(\/-(()))
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
        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val testCourse = TestValues.testCourseE

        val result = courseRepository.insert(testCourse)
        val eitherCourse = Await.result(result, Duration.Inf)
        val \/-(course) = eitherCourse

        course.id should be(testCourse.id)
        course.teacherId should be(testCourse.teacherId)
        course.version should be(1L)
        course.name should be(testCourse.name)
        course.color should be(testCourse.color)
      }
      "insert new course with slug + '-1' if slug exists" in {
        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))
        val existingSlug = TestValues.testCourseA.slug
        val testCourse = TestValues.testCourseE.copy(
          slug = existingSlug
        )

        val result = courseRepository.insert(testCourse)
        val eitherCourse = Await.result(result, Duration.Inf)
        val \/-(course) = eitherCourse

        course.id should be(testCourse.id)
        course.teacherId should be(testCourse.teacherId)
        course.version should be(1L)
        course.name should be(testCourse.name)
        course.slug should be(testCourse.slug + "-1")
        course.color should be(testCourse.color)
      }
      "return RepositoryError.ForeignKeyConflict if course has unexisting teacher id" in {
        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val testCourse = TestValues.testCourseE.copy(
          teacherId = UUID.fromString("9d8ed645-b055-4a69-ab7d-387791c1e064")
        )

        val result = courseRepository.insert(testCourse)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.ForeignKeyConflict("teacher_id", "courses_teacher_id_fkey")))
      }
      "return RepositoryError.PrimaryKeyConflict if course already exists" in {
        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val testCourse = TestValues.testCourseA

        val result = courseRepository.insert(testCourse)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.PrimaryKeyConflict))
      }
    }
  }

  "CourseRepository.update" should {
    inSequence {
      "update existing course" in {
        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))
        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val testCourse = TestValues.testCourseA
        val updatedCourse = testCourse.copy(
          teacherId = TestValues.testCourseB.teacherId,
          name = "new test course name",
          color = new Color(78, 40, 23)
        )

        val result = courseRepository.update(updatedCourse)
        val eitherCourse = Await.result(result, Duration.Inf)
        val \/-(course) = eitherCourse

        course.id should be(updatedCourse.id)
        course.teacherId should be(updatedCourse.teacherId)
        course.version should be(updatedCourse.version + 1)
        course.name should be(updatedCourse.name)
        course.color should be(updatedCourse.color)
        course.createdAt.toString should be(updatedCourse.createdAt.toString)
        course.updatedAt.toString should not be (updatedCourse.updatedAt.toString)
      }
      "update existing course with slug + '-1' if slug exists" in {
        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))
        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val existingSlug = TestValues.testCourseB.slug
        val testCourse = TestValues.testCourseA
        val updatedCourse = testCourse.copy(
          teacherId = TestValues.testCourseB.teacherId,
          name = "new test course name",
          slug = existingSlug,
          color = new Color(78, 40, 23)
        )

        val result = courseRepository.update(updatedCourse)
        val eitherCourse = Await.result(result, Duration.Inf)
        val \/-(course) = eitherCourse

        course.id should be(updatedCourse.id)
        course.teacherId should be(updatedCourse.teacherId)
        course.version should be(updatedCourse.version + 1)
        course.name should be(updatedCourse.name)
        course.slug should be(updatedCourse.slug + "-1")
        course.color should be(updatedCourse.color)
        course.createdAt.toString should be(updatedCourse.createdAt.toString)
        course.updatedAt.toString should not be (updatedCourse.updatedAt.toString)
      }
      "return RepositoryError.NoResults when update an existing Course with wrong version" in {
        val testCourse = TestValues.testCourseA
        val updatedCourse = testCourse.copy(
          teacherId = TestValues.testCourseB.teacherId,
          name = "new test course name",
          color = new Color(78, 40, 23),
          version = 99L
        )

        val result = courseRepository.update(updatedCourse)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Course")))
      }
      "return RepositoryError.NoResults when update a Course that doesn't exist" in {
        val testCourse = TestValues.testCourseC
        val updatedCourse = testCourse.copy(
          teacherId = TestValues.testCourseB.teacherId,
          name = "new test course name",
          color = new Color(78, 40, 23)
        )

        val result = courseRepository.update(updatedCourse)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Course")))
      }
    }
  }

  "CourseRepository.delete" should {
    inSequence {
      "delete course if course has no references" in {
        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))
        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val testCourse = TestValues.testCourseG

        val result = courseRepository.delete(testCourse)
        val eitherCourse = Await.result(result, Duration.Inf)
        val \/-(course) = eitherCourse

        course.id should be(testCourse.id)
        course.teacherId should be(testCourse.teacherId)
        course.version should be(testCourse.version)
        course.name should be(testCourse.name)
        course.color should be(testCourse.color)
      }
      "delete course if course has references only in users_courses table" in {
        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))
        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val testCourse = TestValues.testCourseF

        val result = courseRepository.delete(testCourse)
        val eitherCourse = Await.result(result, Duration.Inf)
        val \/-(course) = eitherCourse

        course.id should be(testCourse.id)
        course.teacherId should be(testCourse.teacherId)
        course.version should be(testCourse.version)
        course.name should be(testCourse.name)
        course.color should be(testCourse.color)
      }
      "return RepositoryError.ForeignKeyConflict if course has references in projects table" in {
        val testCourse = TestValues.testCourseA

        val result = courseRepository.delete(testCourse)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.ForeignKeyConflict("course_id", "projects_course_id_fkey")))
      }
      "return RepositoryError.NoResults if Course hasn't been found" in {
        val testCourse = TestValues.testCourseE

        val result = courseRepository.delete(testCourse)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Course")))
      }
      "return RepositoryError.NoResults if Course version is wrong" in {
        val testCourse = TestValues.testCourseG.copy(
          version = 99L
        )

        val result = courseRepository.delete(testCourse)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Course")))
      }
    }
  }
}

