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
        ClassRepositoryComponent with
        UserRepositoryComponent with
        RoleRepositoryComponent with
        ProjectRepositoryComponent with
        PartRepositoryComponent with
        TaskRepositoryComponent with
        TaskResponseRepositoryComponent with
        DB =>

  override val schoolService: SchoolService = new SchoolServiceImpl

  private class SchoolServiceImpl extends SchoolService {

    /**
     * List all [[Course]]s in the system.
     *
     * @return a vector of [[Course]]
     */
    override def listCourses: Future[IndexedSeq[Course]] = {
      courseRepository.list
    }

    /**
     * Find a specific [[Course]] by its UUID.
     *
     * @param id the unique ID of the course to find
     * @return an optional [[Course]]
     */
    override def findCourse(id: UUID): Future[Option[Course]] = {
      courseRepository.find(id)
    }

    /**
     * Create a new [[Course]] in the system.
     *
     * @param name the name for the new course
     * @return the created [[Course]]
     */
    override def createCourse(name: String): Future[Course] = {
      courseRepository.insert(Course(name = name))(db.pool)
    }

    /**
     * Update an existing [[Course]] in the system.
     *
     * @param id the unique ID of the [[Course]] to update
     * @param version the latest version of the [[Course]] for O.O.L.
     * @param name the new name to assign this [[Course]]
     * @return the updated [[Course]]
     */
    override def updateCourse(id: UUID, version: Long, name: String): Future[Course] = {
      transactional { implicit connection =>
        for {
          existingCourse <- courseRepository.find(id).map(_.get.copy(version = version, name = name))
          updatedCourse <- courseRepository.update(existingCourse)
        } yield updatedCourse
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
      }.recover {
        case exception => throw exception
      }
    }


    /*
     * Methods for Sections
     */

    /**
     * List all sections.
     *
     * @return an [[IndexedSeq]] of [[Class]]
     */
    override def listSections: Future[IndexedSeq[Class]] = {
      classRepository.list
    }

    /**
     * List all sections associated with a specific user.
     *
     * This finds sections for which the given id is set as a
     * student of the section via an association table.
     *
     * @param userId the [[UUID]] of the [[User]] to search for.
     * @param an [[IndexedSeq]] of [[Class]]
     */
    override def listSectionsByUser(userId: UUID): Future[IndexedSeq[Class]] = {
      for {
        user <- userRepository.find(userId).map(_.get)
        sectionsInt <- classRepository.list(user, false)
        sections <- Future sequence sectionsInt.map { section =>
          projectRepository.list(section).map { classProjects =>
            section.copy(projects = Some(classProjects))
          }
        }
      }
      yield sections
    }.recover {
      case exception => throw exception
    }

    /**
     * List all sections taught by a specific user.
     *
     * This finds sections for which the given id is set as the teacherID
     * parameter.
     *
     * @param userId the [[UUID]] of the [[User]] to search for.
     * @param an [[IndexedSeq]] of [[Class]]
     */
    override def listSectionsByTeacher(userId: UUID): Future[IndexedSeq[Class]] = {
      for {
        user <- userRepository.find(userId).map(_.get)
        sectionsInt <- classRepository.list(user, true)
        sections <- Future sequence sectionsInt.map { section =>
          projectRepository.list(section).map { classProjects =>
            section.copy(projects = Some(classProjects))
          }
        }
      }
      yield sections
    }.recover {
      case exception => throw exception
    }

    /**
     * List all sections that have a specific project.
     *
     * @param projectId the [[UUID]] of the [[Project]] to filter by
     * @return an [[IndexedSeq]] of [[Class]]
     */
    override def listSectionsByProject(projectId: UUID): Future[IndexedSeq[Class]] = {
      for {
        project <- projectRepository.find(projectId).map(_.get)
        sectionsInt <- classRepository.list(project)
        sections <- Future sequence sectionsInt.map { section =>
          projectRepository.list(section).map { classProjects =>
            section.copy(projects = Some(classProjects))
          }
        }
      }
      yield sections
    }.recover {
      case exception => throw exception
    }

    /**
     * Find a specific section by id.
     *
     * @param id the [[UUID]] of the [[Class]] to find.
     * @return an optional [[Class]]
     */
    override def findSection(id: UUID): Future[Option[Class]] = {
      classRepository.find(id)
    }

    /**
     * Create a new section.
     *
     * @param courseId the [[UUID]] of the [[Course]] this section belongs to
     * @param teacherId the optional [[UUID]] of the [[User]] teaching this section
     * @param name the name of this section
     * @return the newly created [[Class]]
     */
    override def createSection(teacherId: Option[UUID], name: String, color: Color): Future[Class] = {
      transactional { implicit connection =>
        val foTeacher = teacherId match {
          case Some(id) => userRepository.find(id)
          case None => Future.successful(None)
        }

        for {
          oTeacher <- foTeacher
          newSection <- classRepository.insert(Class(
            teacherId = oTeacher match {
              case Some(teacher) => Some(teacher.id)
              case None => None
            },
            name = name,
            color = color
          ))
        }
        yield newSection
      }.recover {
        case exception => throw exception
      }
    }

    /**
     * Create a new section.
     *
     * @param courseId the [[UUID]] of the [[Course]] this section belongs to
     * @param teacherId the optional [[UUID]] of the [[User]] teaching this section
     * @param name the name of this section
     * @return the newly created [[Class]]
     */
    override def updateSection(id: UUID, version: Long, teacherId: Option[UUID], name: String, color: Color): Future[Class] = {
      transactional { implicit connection =>
        val fExistingSection = classRepository.find(id).map(_.get.copy(version = version))
        val foTeacher = teacherId match {
          case Some(id) => userRepository.find(id)
          case None => Future.successful(None)
        }

        for {
          existingSection <- fExistingSection
          oTeacher <- foTeacher
          updatedSection <- classRepository.update(existingSection.copy(
            teacherId = oTeacher match {
              case Some(teacher) => Some(teacher.id)
              case None => None
            },
            name = name,
            color = color
          ))
        }
        yield updatedSection
      }.recover {
        case exception => throw exception
      }
    }

    /**
     * Delete a [[Class]] from the system.
     *
     * @param id the unique ID of the [[Class]] to update
     * @param version the latest version of the [[Class]] for O.O.L.
     * @return a boolean indicating success or failure
     */
    override def deleteSection(id: UUID, version: Long): Future[Boolean] = {
      transactional { implicit connection =>
        for {
          existingSection <- classRepository.find(id).map(_.get)
          wasDeleted <- classRepository.delete(existingSection)
        } yield wasDeleted
      }
    }

    // Utility functions for section management, assuming you already found a section object.

    // TODO - delete
    /**
     * Enable a part for a section.
     *
     * @param classId the [[UUID]] of the [[Class]] to enable a [[Part]] for
     * @param partId the [[UUID]] of the [[Part]] to enable
     * @return a [[Boolean]] indicating success or failure
     */
    override def enablePart(classId: UUID, partId: UUID): Future[Boolean] = {
      transactional { implicit connection =>
        for {
          section <- classRepository.find(classId).map(_.get)
          part <- partRepository.find(partId).map(_.get)
          wasEnabled <- classRepository.enablePart(section, part)
        }
        yield wasEnabled
      }
    }

    // TODO - delete
    /**
     * Disable a part for a section.
     *
     * @param classId the [[UUID]] of the [[Class]] to disable a [[Part]] for
     * @param partId the [[UUID]] of the [[Part]] to disable
     * @return a [[Boolean]] indicating success or failure
     */
    override def disablePart(classId: UUID, partId: UUID): Future[Boolean] = {
      transactional { implicit connection =>
        for {
          section <- classRepository.find(classId).map(_.get)
          part <- partRepository.find(partId).map(_.get)
          wasEnabled <- classRepository.disablePart(section, part)
        }
        yield wasEnabled
      }
    }

    /**
     * List all students registered to a section.
     */
    override def listStudents(section: Class): Future[IndexedSeq[User]] = {
      userRepository.list(section)
    }

    /**
     * List all projects belonging to a section.
     */
    override def listProjects(section: Class): Future[IndexedSeq[Project]] = {
      projectRepository.list(section)
    }

    /**
     * List all project parts that have been enabled for a section.
     *
     * @param projectId the [[UUID]] of the [[Project]] to list parts from
     * @param classId the [[UUID]] of the [[Class]] to list parts for
     * @return an [[IndexedSeq]] of [[Part]].
     */
    override def listEnabledParts(projectId: UUID, classId: UUID): Future[IndexedSeq[Part]] = {
      val fProject = projectRepository.find(projectId).map(_.get)
      val fSection = classRepository.find(classId).map(_.get)
      for {
        project <- fProject
        section <- fSection
        enabledParts <- partRepository.listEnabled(project, section)
      }
      yield enabledParts
    }.recover {
      case exception => throw exception
    }

    /**
     * List all students registered to a section.
     */
    override def listStudents(classId: UUID): Future[IndexedSeq[User]] = {
      val studentList = for {
        section <- classRepository.find(classId).map(_.get)
        students <- userRepository.listForSections(IndexedSeq(section))
      }
      yield students

      studentList.recover {
        case exception => throw exception
      }
    }

    /**
     * List all projects belonging to a section.
     */
    override def listProjects(classId: UUID): Future[IndexedSeq[Project]] = {
      for {
        section <- classRepository.find(classId).map(_.get)
        projects <- projectRepository.list(section)
      }
      yield projects
    }.recover {
      case exception => throw exception
    }

    /**
     * Add students to a section.
     *
     * @param section the [[Class]] to add users to
     * @param userIds an [[IndexedSeq]] of [[UUID]] representing the [[User]]s to be added.
     * @param a [[Boolean]] indicating success or failure.
     */
    override def addUsers(section: Class, userIds: IndexedSeq[UUID]): Future[Boolean] = {
      transactional { implicit connection =>
        for {
          users <- Future.sequence(userIds.map(userRepository.find)).map(_.map(_.get))
          wereAdded <- classRepository.addUsers(section, users)
        }
        yield wereAdded
      }
    }

    /**
     * Remove students from a section.
     *
     * @param section the [[Class]] to remove users from
     * @param userIds an [[IndexedSeq]] of [[UUID]] representing the [[User]]s to be removed.
     * @param a [[Boolean]] indicating success or failure.
     */
    override def removeUsers(section: Class, userIds: IndexedSeq[UUID]): Future[Boolean] = {
      transactional { implicit connection =>
        for {
          users <- Future.sequence(userIds.map(userRepository.find)).map(_.map(_.get))
          wereRemoved <- classRepository.removeUsers(section, users)
        }
        yield wereRemoved
      }
    }

    /**
     * Given a user and teacher, finds whether this user belongs to any of that teacher's classes.
     *
     * @param userId
     * @param teacherId
     * @return
     */
    override def findUserForTeacher(userId: UUID, teacherId: UUID): Future[Option[UserInfo]] = {
      for {
        user <- userRepository.find(userId).map(_.get)
        teacher <- userRepository.find(teacherId).map(_.get)
        maybeStudent <- classRepository.findUserForTeacher(user, teacher).flatMap {
          case Some(student) => {
            for {
              roles <- roleRepository.list(student)
              classes <- classRepository.list(student)
            } yield Some(UserInfo(student, roles, classes))
          }
          case _ => Future successful None
        }
      } yield maybeStudent
    }.recover {
      case exception => throw exception
    }

    /**
     * Force complete all responses for a given task in a given section.
     *
     * @param taskId the [[UUID]] of the task to be force-completed
     * @param classId the [[UUID]] of the section whose users will
     *                  have their responses force-completed
     * @return a boolean indicating success or failure
     */
    override def forceComplete(taskId: UUID, classId: UUID): Future[Boolean] = {
      transactional { implicit connection =>
        for {
          task <- taskRepository.find(taskId).map(_.get)
          section <- classRepository.find(classId).map(_.get)
          forcedComplete <- taskResponseRepository.forceComplete(task, section)
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
        hasProject <- classRepository.hasProject(user, project)(db.pool)
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
          case Some(project) => partRepository.listEnabled(project, user)
          case None => Future.successful(IndexedSeq[Part]())
        }}
      } yield partsList
    }.recover {
      case exception => throw exception
    }

    override def isPartEnabledForUser(partId: UUID, userId: UUID): Future[Boolean] = {
      for {
        part <- partRepository.find(partId).map(_.get)
        user <- userRepository.find(userId).map(_.get)
        isEnabled <- partRepository.isEnabled(part, user)
      }
      yield isEnabled
    }

    override def isPartEnabledForSection(partId: UUID, classId: UUID): Future[Boolean] = {
      for {
        part <- partRepository.find(partId).map(_.get)
        section <- classRepository.find(classId).map(_.get)
        isEnabled <- partRepository.isEnabled(part, section)
      }
      yield isEnabled
    }
  }

}
