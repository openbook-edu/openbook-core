package ca.shiftfocus.krispii.core.services

import java.awt.Color

import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.krispii.core.services.datasource._
import ca.shiftfocus.uuid.UUID
import play.api.Logger
import scala.concurrent.Future

trait SchoolServiceImplComponent extends SchoolServiceComponent {
  self: CourseRepositoryComponent with
        CourseRepositoryComponent with
        UserRepositoryComponent with
        RoleRepositoryComponent with
        ProjectRepositoryComponent with
        PartRepositoryComponent with
        TaskRepositoryComponent with
        TaskResponseRepositoryComponent with
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
    override def listCourses: Future[IndexedSeq[Course]] = {
      courseRepository.list
    }

    /**
     * List all courses associated with a specific user.
     *
     * This finds courses for which the given id is set as a
     * student of the course via an association table.
     *
     * @param userId the [[UUID]] of the [[User]] to search for.
     * @param an [[IndexedSeq]] of [[Course]]
     */
    override def listCoursesByUser(userId: UUID): Future[IndexedSeq[Course]] = {
      for {
        user <- userRepository.find(userId).map(_.get)
        coursesInt <- courseRepository.list(user, false)
        courses <- Future sequence coursesInt.map { course =>
          projectRepository.list(course).map { courseProjects =>
            course.copy(projects = Some(courseProjects))
          }
        }
      }
      yield courses
    }.recover {
      case exception => throw exception
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
    override def listCoursesByTeacher(userId: UUID): Future[IndexedSeq[Course]] = {
      for {
        user <- userRepository.find(userId).map(_.get)
        coursesInt <- courseRepository.list(user, true)
        courses <- Future sequence coursesInt.map { course =>
          projectRepository.list(course).map { courseProjects =>
            course.copy(projects = Some(courseProjects))
          }
        }
      }
      yield courses
    }.recover {
      case exception => throw exception
    }

    /**
     * List all courses that have a specific project.
     *
     * @param projectId the [[UUID]] of the [[Project]] to filter by
     * @return an [[IndexedSeq]] of [[Course]]
     */
    override def listCoursesByProject(projectId: UUID): Future[IndexedSeq[Course]] = {
      for {
        project <- projectRepository.find(projectId).map(_.get)
        coursesInt <- courseRepository.list(project)
        courses <- Future sequence coursesInt.map { course =>
          projectRepository.list(course).map { courseProjects =>
            course.copy(projects = Some(courseProjects))
          }
        }
      }
      yield courses
    }.recover {
      case exception => throw exception
    }

    /**
     * Find a specific course by id.
     *
     * @param id the [[UUID]] of the [[Course]] to find.
     * @return an optional [[Course]]
     */
    override def findCourse(id: UUID): Future[Option[Course]] = {
      courseRepository.find(id)
    }

    /**
     * Create a new course.
     *
     * @param courseId the [[UUID]] of the [[Course]] this course belongs to
     * @param teacherId the optional [[UUID]] of the [[User]] teaching this course
     * @param name the name of this course
     * @return the newly created [[Course]]
     */
    override def createCourse(teacherId: Option[UUID], name: String, color: Color): Future[Course] = {
      transactional { implicit connection =>
        val foTeacher = teacherId match {
          case Some(id) => userRepository.find(id)
          case None => Future.successful(None)
        }

        for {
          oTeacher <- foTeacher
          newCourse <- courseRepository.insert(Course(
            teacherId = oTeacher match {
              case Some(teacher) => Some(teacher.id)
              case None => None
            },
            name = name,
            color = color
          ))
        }
        yield newCourse
      }.recover {
        case exception => throw exception
      }
    }

    /**
     * Create a new course.
     *
     * @param courseId the [[UUID]] of the [[Course]] this course belongs to
     * @param teacherId the optional [[UUID]] of the [[User]] teaching this course
     * @param name the name of this course
     * @return the newly created [[Course]]
     */
    override def updateCourse(id: UUID, version: Long, teacherId: Option[UUID], name: String, color: Color): Future[Course] = {
      transactional { implicit connection =>
        val fExistingCourse = courseRepository.find(id).map(_.get.copy(version = version))
        val foTeacher = teacherId match {
          case Some(id) => userRepository.find(id)
          case None => Future.successful(None)
        }

        for {
          existingCourse <- fExistingCourse
          oTeacher <- foTeacher
          updatedCourse <- courseRepository.update(existingCourse.copy(
            teacherId = oTeacher match {
              case Some(teacher) => Some(teacher.id)
              case None => None
            },
            name = name,
            color = color
          ))
        }
        yield updatedCourse
      }.recover {
        case exception => throw exception
      }
    }

    /**
     * Delete a [[Course]] from the system.
     *
     * @param id the unique ID of the [[Course]] to update
     * @param version the latest version of the [[Course]] for O.O.L.
     * @return a boolean indicating success or failure
     */
    override def deleteCourse(id: UUID, version: Long): Future[Boolean] = {
      transactional { implicit connection =>
        for {
          existingCourse <- courseRepository.find(id).map(_.get)
          wasDeleted <- courseRepository.delete(existingCourse)
        } yield wasDeleted
      }
    }

    // Utility functions for course management, assuming you already found a course object.

    /**
     * List all students registered to a course.
     */
    override def listStudents(course: Course): Future[IndexedSeq[User]] = {
      userRepository.list(course)
    }

    /**
     * List all projects belonging to a course.
     */
    override def listProjects(course: Course): Future[IndexedSeq[Project]] = {
      projectRepository.list(course)
    }

    // TODO rewrite
    /**
     * List all project parts that have been enabled for a course.
     *
     * @param projectId the [[UUID]] of the [[Project]] to list parts from
     * @param courseId the [[UUID]] of the [[Course]] to list parts for
     * @return an [[IndexedSeq]] of [[Part]].
     */
//    override def listEnabledParts(projectId: UUID, classId: UUID): Future[IndexedSeq[Part]] = {
//      val fProject = projectRepository.find(projectId).map(_.get)
//      val fSection = classRepository.find(classId).map(_.get)
//      for {
//        project <- fProject
//        section <- fSection
//        enabledParts <- partRepository.listEnabled(project, section)
//      }
//      yield enabledParts
//    }.recover {
//      case exception => throw exception
//    }

    override def listEnabledParts(projectId: UUID): Future[IndexedSeq[Part]] = {
      val fProject = projectRepository.find(projectId).map(_.get)
      for {
        project <- fProject
        enabledParts <- {
          Future successful project.parts.filter(_.enabled)
        }

      } yield enabledParts
    }.recover {
      case exception => throw exception
    }

    /**
     * List all students registered to a course.
     */
    override def listStudents(courseId: UUID): Future[IndexedSeq[User]] = {
      val studentList = for {
        course <- courseRepository.find(courseId).map(_.get)
        students <- userRepository.listForCourses(IndexedSeq(course))
      }
      yield students

      studentList.recover {
        case exception => throw exception
      }
    }

    /**
     * List all projects belonging to a course.
     */
    override def listProjects(courseId: UUID): Future[IndexedSeq[Project]] = {
      for {
        course <- courseRepository.find(courseId).map(_.get)
        projects <- projectRepository.list(course)
      }
      yield projects
    }.recover {
      case exception => throw exception
    }

    /**
     * Add students to a course.
     *
     * @param course the [[Course]] to add users to
     * @param userIds an [[IndexedSeq]] of [[UUID]] representing the [[User]]s to be added.
     * @param a [[Boolean]] indicating success or failure.
     */
    override def addUsers(course: Course, userIds: IndexedSeq[UUID]): Future[Boolean] = {
      transactional { implicit connection =>
        for {
          users <- Future.sequence(userIds.map(userRepository.find)).map(_.map(_.get))
          wereAdded <- courseRepository.addUsers(course, users)
        }
        yield wereAdded
      }
    }

    /**
     * Remove students from a course.
     *
     * @param course the [[Course]] to remove users from
     * @param userIds an [[IndexedSeq]] of [[UUID]] representing the [[User]]s to be removed.
     * @param a [[Boolean]] indicating success or failure.
     */
    override def removeUsers(course: Course, userIds: IndexedSeq[UUID]): Future[Boolean] = {
      transactional { implicit connection =>
        for {
          users <- Future.sequence(userIds.map(userRepository.find)).map(_.map(_.get))
          wereRemoved <- courseRepository.removeUsers(course, users)
        }
        yield wereRemoved
      }
    }

    /**
     * Given a user and teacher, finds whether this user belongs to any of that teacher's courses.
     *
     * @param userId
     * @param teacherId
     * @return
     */
    override def findUserForTeacher(userId: UUID, teacherId: UUID): Future[Option[UserInfo]] = {
      for {
        user <- userRepository.find(userId).map(_.get)
        teacher <- userRepository.find(teacherId).map(_.get)
        maybeStudent <- courseRepository.findUserForTeacher(user, teacher).flatMap {
          case Some(student) => {
            for {
              roles <- roleRepository.list(student)
              courses <- courseRepository.list(student)
            } yield Some(UserInfo(student, roles, courses))
          }
          case _ => Future successful None
        }
      } yield maybeStudent
    }.recover {
      case exception => throw exception
    }

    /**
     * Force complete all responses for a given task in a given course.
     *
     * @param taskId the [[UUID]] of the task to be force-completed
     * @param courseId the [[UUID]] of the course whose users will
     *                  have their responses force-completed
     * @return a boolean indicating success or failure
     */
    override def forceComplete(taskId: UUID, courseId: UUID): Future[Boolean] = {
      transactional { implicit connection =>
        for {
          task <- taskRepository.find(taskId).map(_.get)
          course <- courseRepository.find(courseId).map(_.get)
          forcedComplete <- taskResponseRepository.forceComplete(task, course)
        }
        yield forcedComplete
      }
    }

    /**
     * Checks if a user has access to a project.
     *
     * @param userId the [[UUID]] of the user to check
     * @param projectSlug the slug of the project to look for
     * @return a boolean indicating success or failure
     */
    override def userHasProject(userId: UUID, projectSlug: String): Future[Boolean] = {
      for {
        user <- userRepository.find(userId).map(_.get)
        project <- projectRepository.find(projectSlug).map(_.get)
        hasProject <- courseRepository.hasProject(user, project)(db.pool)
      }
      yield hasProject
    }.recover {
      case exception => throw exception
    }

    /**
     * List a user's enabled parts.
     *
     * @param projectSlug the project's slug
     * @param userId the [[UUID]] of the [[User]] to list parts for
     * @return an [[IndexedSeq[Part]]]
     */
    override def listEnabledParts(projectSlug: String, userId: UUID): Future[IndexedSeq[Part]] = {
      for {
        projectOption <- projectRepository.find(projectSlug)
        user <- userRepository.find(userId).map(_.get)
        partsList <- { projectOption match {
          case Some(project) => Future successful project.parts.filter(_.enabled)
          case None => Future.successful(IndexedSeq[Part]())
        }}
      } yield partsList
    }.recover {
      case exception => throw exception
    }

    // TODO remove
//    override def isPartEnabledForUser(partId: UUID, userId: UUID): Future[Boolean] = {
//      for {
//        part <- partRepository.find(partId).map(_.get)
//        user <- userRepository.find(userId).map(_.get)
//        isEnabled <- partRepository.isEnabled(part, user)
//      }
//      yield isEnabled
//    }
//
//    override def isPartEnabledForSection(partId: UUID, classId: UUID): Future[Boolean] = {
//      for {
//        part <- partRepository.find(partId).map(_.get)
//        section <- classRepository.find(classId).map(_.get)
//        isEnabled <- partRepository.isEnabled(part, section)
//      }
//      yield isEnabled
//    }

    override def isPartEnabledForCourse(partId: UUID, courseId: UUID): Future[Boolean] = {
      for {
        part <- partRepository.find(partId).map(_.get)
        course <- courseRepository.find(courseId).map(_.get)
        isEnabled <- partRepository.isEnabled(part, course)
      }
      yield isEnabled
    }
  }
}
