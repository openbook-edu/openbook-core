package ca.shiftfocus.krispii.core.services

import java.awt.Color

import ca.shiftfocus.krispii.core.fail.{BadInput, NoResults, GenericFail, Fail}
import ca.shiftfocus.krispii.core.lib.FutureMonad
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.krispii.core.services.datasource._
import ca.shiftfocus.uuid.UUID
import play.api.Logger
import scala.concurrent.Future
import scalaz.{\/-, -\/, \/}

trait SchoolServiceImplComponent extends SchoolServiceComponent {
  self: AuthServiceComponent with
        ProjectServiceComponent with
        CourseRepositoryComponent with
        CourseRepositoryComponent with
        UserRepositoryComponent with
        WorkRepositoryComponent with
        DB =>

  override val schoolService: SchoolService = new SchoolServiceImpl

  private class SchoolServiceImpl extends SchoolService {

    /*
     * Methods for Courses
     */

    /**
     * List all courses.
     *
     * @return an [[IndexedSeq]] of [[Course]]
     */
    override def listCourses: Future[\/[Fail, IndexedSeq[Course]]] = {
      courseRepository.list
    }
    
    /**
     * List all courses associated with a specific user.
     *
     * This finds courses for which the given id is set as a
     * student of the course via an association table.
     *
     * @param userId the [[UUID]] of the [[User]] to search for.
     * @return an [[IndexedSeq]] of [[Course]]
     */
    override def listCoursesByUser(userId: UUID): Future[\/[Fail, IndexedSeq[Course]]] = {
      (for {
        userInfo <- lift(authService.find(userId))
        courses <- lift(courseRepository.list(userInfo.user, false))
        coursesWithProjects <- liftSeq { courses.map { course =>
          (for {
            projects <- lift(projectService.list(course.id))
            courseWithProjects = course.copy(projects = Some(projects))
          } yield courseWithProjects).run
        }}
      }
      yield coursesWithProjects).run
    }

    /**
     * List all courses taught by a specific user.
     *
     * This finds courses for which the given id is set as the teacherID
     * parameter.
     *
     * @param userId the [[UUID]] of the [[User]] to search for.
     * @param an [[IndexedSeq]] of [[Course]]
     */
    override def listCoursesByTeacher(userId: UUID): Future[\/[Fail, IndexedSeq[Course]]] = {
      (for {
        userInfo <- lift(authService.find(userId))
        coursesInt <- lift(courseRepository.list(userInfo.user, true))
        courses <- liftSeq { coursesInt.map { course =>
          (for {
            projects <- lift(projectService.list(course.id))
            courseWithProjects = course.copy(projects = Some(projects))
          } yield courseWithProjects).run
        }}
      } yield courses).run
    }

    /**
     * List all courses that have a specific project.
     *
     * @param projectId the [[UUID]] of the [[Project]] to filter by
     * @return an [[IndexedSeq]] of [[Course]]
     */
    override def listCoursesByProject(projectId: UUID): Future[\/[Fail, IndexedSeq[Course]]] = {
      (for {
        project <- lift(projectService.find(projectId))
        coursesInt <- lift(courseRepository.list(project))
        courses <- liftSeq { coursesInt.map { course =>
          (for {
            projects <- lift(projectService.list(course.id))
            courseWithProjects = course.copy(projects = Some(projects))
          } yield courseWithProjects).run
        }}
      }
      yield courses).run
    }

    /**
     * Find a specific course by id.
     *
     * @param id the [[UUID]] of the [[Course]] to find.
     * @return an optional [[Course]]
     */
    override def findCourse(id: UUID): Future[\/[Fail, Course]] = {
      (for {
        course <- lift(courseRepository.find(id))
        projects <- lift(projectService.list(course.id))
      } yield course.copy(projects = Some(projects))).run
    }

    /**
     * Create a new course.
     *
     * @param courseId the [[UUID]] of the [[Course]] this course belongs to
     * @param teacherId the optional [[UUID]] of the [[User]] teaching this course
     * @param name the name of this course
     * @return the newly created [[Course]]
     */
    override def createCourse(teacherId: UUID, name: String, color: Color): Future[\/[Fail, Course]] = {
      transactional { implicit connection =>
        (for {
          teacher <- lift(authService.find(teacherId))
          _ <- predicate (teacher.roles.map(_.name).contains("teacher")) (GenericFail("Tried to create a course for a user who isn't a teacher."))
          newCourse = Course(
            teacherId = teacher.user.id,
            name = name,
            color = color
          )
          createdCourse <- lift(courseRepository.insert(newCourse))
        }
        yield newCourse).run
      }
    }

    /**
     * Create a new course.
     *
     * @param id the [[UUID]] of the [[Course]] this course belongs to
     * @param teacherId the optional [[UUID]] of the [[User]] teaching this course
     * @param name the name of this course
     * @return the newly created [[Course]]
     */
    override def updateCourse(id: UUID, version: Long, teacherId: Option[UUID], name: Option[String], color: Option[Color]): Future[\/[Fail, Course]] = {
      transactional { implicit connection =>
        (for {
          existingCourse <- lift(courseRepository.find(id))
          tId = teacherId.getOrElse(existingCourse.teacherId)
          teacher <- lift(authService.find(tId))
          _ <- predicate (teacher.roles.map(_.name).contains("teacher")) (GenericFail("Tried to update a course for a user who isn't a teacher."))
          toUpdate = existingCourse.copy(
            teacherId = teacherId.getOrElse(existingCourse.teacherId),
            name = name.getOrElse(existingCourse.name),
            color = color.getOrElse(existingCourse.color)
          )
          updatedCourse <- lift(courseRepository.update(toUpdate))
        }
        yield updatedCourse).run
      }
    }

    /**
     * Delete a [[Course]] from the system.
     *
     * @param id the unique ID of the [[Course]] to update
     * @param version the latest version of the [[Course]] for O.O.L.
     * @return a boolean indicating success or failure
     */
    override def deleteCourse(id: UUID, version: Long): Future[\/[Fail, Course]] = {
      transactional { implicit connection =>
        (for {
          existingCourse <- lift(courseRepository.find(id))
          toDelete = existingCourse.copy(version = version)
          wasDeleted <- lift(courseRepository.delete(toDelete))
        } yield wasDeleted).run
      }
    }

    // Utility functions for course management, assuming you already found a course object.

    /**
     * List all students registered to a course.
     */
    override def listStudents(courseId: UUID): Future[\/[Fail, IndexedSeq[UserInfo]]] = {
      (for {
        course <- lift(courseRepository.find(courseId))
        students <- lift(listStudents(course))
      } yield students).run
    }

    override def listStudents(course: Course): Future[\/[Fail, IndexedSeq[UserInfo]]] = {
      authService.list(None, Some(IndexedSeq(course.id)))
    }

    /**
     * List all projects belonging to a course.
     */
    override def listProjects(courseId: UUID): Future[\/[Fail, IndexedSeq[Project]]] = {
      (for {
        course <- lift(courseRepository.find(courseId))
        projects <- lift(listProjects(course))
      } yield projects).run
    }

    override def listProjects(course: Course): Future[\/[Fail, IndexedSeq[Project]]] = {
      projectService.list(course.id)
    }

    /**
     * Add students to a course.
     *
     * @param course the [[Course]] to add users to
     * @param userIds an [[IndexedSeq]] of [[UUID]] representing the [[User]]s to be added.
     * @param a [[Boolean]] indicating success or failure.
     */
    override def addUsers(course: Course, userIds: IndexedSeq[UUID]): Future[\/[Fail, IndexedSeq[User]]] = {
      transactional { implicit connection =>
        (for {
          users <- lift(userRepository.list(userIds))
          wereAdded <- lift(courseRepository.addUsers(course, users))
          usersInCourse <- lift(userRepository.list(course))
        } yield usersInCourse).run
      }
    }

    /**
     * Remove students from a course.
     *
     * @param course the [[Course]] to remove users from
     * @param userIds an [[IndexedSeq]] of [[UUID]] representing the [[User]]s to be removed.
     * @param a [[Boolean]] indicating success or failure.
     */
    override def removeUsers(course: Course, userIds: IndexedSeq[UUID]): Future[\/[Fail, IndexedSeq[User]]] = {
      transactional { implicit connection =>
        (for {
          users <- lift(userRepository.list(userIds))
          wereRemoved <- lift(courseRepository.removeUsers(course, users))
          usersInCourse <- lift(userRepository.list(course))
        } yield usersInCourse).run
      }
    }

    /**
     * Given a user and teacher, finds whether this user belongs to any of that teacher's courses.
     *
     * @param userId
     * @param teacherId
     * @return
     */
    override def findUserForTeacher(userId: UUID, teacherId: UUID): Future[\/[Fail, UserInfo]] = {
      (for {
        user <- lift(authService.find(userId))
        teacher <- lift(authService.find(teacherId))
        maybeStudent <- lift(courseRepository.findUserForTeacher(user.user, teacher.user))
        student <- lift(authService.find(maybeStudent.id))
      } yield student).run
    }

    /**
     * Checks if a user has access to a project.
     *
     * @param userId the [[UUID]] of the user to check
     * @param projectSlug the slug of the project to look for
     * @return a boolean indicating success or failure
     */
    override def userHasProject(userId: UUID, projectSlug: String): Future[\/[Fail, Boolean]] = {
      (for {
        user <- lift(authService.find(userId))
        project <- lift(projectService.find(projectSlug))
        hasProject <- lift(courseRepository.hasProject(user.user, project)(db.pool))
      }
      yield hasProject).run
    }
  }
}
