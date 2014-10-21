package com.shiftfocus.krispii.common.services

import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import com.shiftfocus.krispii.common.models._
import com.shiftfocus.krispii.common.repositories._
import com.shiftfocus.krispii.common.services.datasource._
import com.shiftfocus.krispii.lib.UUID
import play.api.Logger
import scala.concurrent.Future

trait SchoolServiceImplComponent extends SchoolServiceComponent {
  self: CourseRepositoryComponent with
        SectionRepositoryComponent with
        UserRepositoryComponent with
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
     * @return an [[IndexedSeq]] of [[Section]]
     */
    override def listSections: Future[IndexedSeq[Section]] = {
      sectionRepository.list
    }

    /**
     * List all sections associated with a specific user.
     *
     * This finds sections for which the given id is set as a
     * student of the section via an association table.
     *
     * @param userId the [[UUID]] of the [[User]] to search for.
     * @param an [[IndexedSeq]] of [[Section]]
     */
    override def listSectionsByUser(userId: UUID): Future[IndexedSeq[Section]] = {
      for {
        user <- userRepository.find(userId).map(_.get)
        sections <- sectionRepository.list(user, false)
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
     * @param an [[IndexedSeq]] of [[Section]]
     */
    override def listSectionsByTeacher(userId: UUID): Future[IndexedSeq[Section]] = {
      for {
        user <- userRepository.find(userId).map(_.get)
        sections <- sectionRepository.list(user, true)
      }
      yield sections
    }.recover {
      case exception => throw exception
    }

    /**
     * List all sections that have a specific project.
     *
     * @param projectId the [[UUID]] of the [[Project]] to filter by
     * @return an [[IndexedSeq]] of [[Section]]
     */
    override def listSectionsByProject(projectId: UUID): Future[IndexedSeq[Section]] = {
      for {
        project <- projectRepository.find(projectId).map(_.get)
        sections <- sectionRepository.list(project)
      }
      yield sections
    }.recover {
      case exception => throw exception
    }

    /**
     * Find a specific section by id.
     *
     * @param id the [[UUID]] of the [[Section]] to find.
     * @return an optional [[Section]]
     */
    override def findSection(id: UUID): Future[Option[Section]] = {
      sectionRepository.find(id)
    }

    /**
     * Create a new section.
     *
     * @param courseId the [[UUID]] of the [[Course]] this section belongs to
     * @param teacherId the optional [[UUID]] of the [[User]] teaching this section
     * @param name the name of this section
     * @return the newly created [[Section]]
     */
    override def createSection(courseId: UUID, teacherId: Option[UUID], name: String): Future[Section] = {
      transactional { implicit connection =>
        val fCourse = courseRepository.find(courseId).map(_.get)
        val foTeacher = teacherId match {
          case Some(id) => userRepository.find(id)
          case None => Future.successful(None)
        }

        for {
          course <- fCourse
          oTeacher <- foTeacher
          newSection <- sectionRepository.insert(Section(
            courseId = course.id,
            teacherId = oTeacher match {
              case Some(teacher) => Some(teacher.id)
              case None => None
            },
            name = name
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
     * @return the newly created [[Section]]
     */
    override def updateSection(id: UUID, version: Long, courseId: UUID, teacherId: Option[UUID], name: String): Future[Section] = {
      transactional { implicit connection =>
        val fExistingSection = sectionRepository.find(id).map(_.get.copy(version = version))
        val fCourse = courseRepository.find(courseId).map(_.get)
        val foTeacher = teacherId match {
          case Some(id) => userRepository.find(id)
          case None => Future.successful(None)
        }

        for {
          existingSection <- fExistingSection
          course <- fCourse
          oTeacher <- foTeacher
          updatedSection <- sectionRepository.update(existingSection.copy(
            courseId = course.id,
            teacherId = oTeacher match {
              case Some(teacher) => Some(teacher.id)
              case None => None
            },
            name = name
          ))
        }
        yield updatedSection
      }.recover {
        case exception => throw exception
      }
    }

    /**
     * Delete a [[Section]] from the system.
     *
     * @param id the unique ID of the [[Section]] to update
     * @param version the latest version of the [[Section]] for O.O.L.
     * @return a boolean indicating success or failure
     */
    override def deleteSection(id: UUID, version: Long): Future[Boolean] = {
      transactional { implicit connection =>
        for {
          existingSection <- sectionRepository.find(id).map(_.get)
          wasDeleted <- sectionRepository.delete(existingSection)
        } yield wasDeleted
      }
    }

    // Utility functions for section management, assuming you already found a section object.

    /**
     * Enable a part for a section.
     *
     * @param sectionId the [[UUID]] of the [[Section]] to enable a [[Part]] for
     * @param partId the [[UUID]] of the [[Part]] to enable
     * @return a [[Boolean]] indicating success or failure
     */
    override def enablePart(sectionId: UUID, partId: UUID): Future[Boolean] = {
      transactional { implicit connection =>
        for {
          section <- sectionRepository.find(sectionId).map(_.get)
          part <- partRepository.find(partId).map(_.get)
          wasEnabled <- sectionRepository.enablePart(section, part)
        }
        yield wasEnabled
      }
    }

    /**
     * Disable a part for a section.
     *
     * @param sectionId the [[UUID]] of the [[Section]] to disable a [[Part]] for
     * @param partId the [[UUID]] of the [[Part]] to disable
     * @return a [[Boolean]] indicating success or failure
     */
    override def disablePart(sectionId: UUID, partId: UUID): Future[Boolean] = {
      transactional { implicit connection =>
        for {
          section <- sectionRepository.find(sectionId).map(_.get)
          part <- partRepository.find(partId).map(_.get)
          wasEnabled <- sectionRepository.disablePart(section, part)
        }
        yield wasEnabled
      }
    }

    /**
     * List all students registered to a section.
     */
    override def listStudents(section: Section): Future[IndexedSeq[User]] = {
      userRepository.list(section)
    }

    /**
     * List all projects belonging to a section.
     */
    override def listProjects(section: Section): Future[IndexedSeq[Project]] = {
      projectRepository.list(section)
    }

    /**
     * List all project parts that have been enabled for a section.
     *
     * @param projectId the [[UUID]] of the [[Project]] to list parts from
     * @param sectionId the [[UUID]] of the [[Section]] to list parts for
     * @return an [[IndexedSeq]] of [[Part]].
     */
    override def listEnabledParts(projectId: UUID, sectionId: UUID): Future[IndexedSeq[Part]] = {
      val fProject = projectRepository.find(projectId).map(_.get)
      val fSection = sectionRepository.find(sectionId).map(_.get)
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
    override def listStudents(sectionId: UUID): Future[IndexedSeq[User]] = {
      val studentList = for {
        section <- sectionRepository.find(sectionId).map(_.get)
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
    override def listProjects(sectionId: UUID): Future[IndexedSeq[Project]] = {
      for {
        section <- sectionRepository.find(sectionId).map(_.get)
        projects <- projectRepository.list(section)
      }
      yield projects
    }.recover {
      case exception => throw exception
    }

    /**
     * Add projects to a section.
     */
    override def addProjects(section: Section, projectIds: IndexedSeq[UUID]): Future[Boolean] = {
      transactional { implicit connection =>
        for {
          projects <- Future.sequence(projectIds.map(projectRepository.find)).map(_.map(_.get))
          wereAdded <- sectionRepository.addProjects(section, projects)
        }
        yield wereAdded
      }
    }

    /**
     * Remove specified projects from the section.
     *
     * @param section the [[Section]] to remove projects from
     * @param projectIds an [[IndexedSeq]] of [[UUID]] representing the [[Project]]s to remove
     * @param a [[Boolean]] indicating success or failure.
     */
    override def removeProjects(section: Section, projectIds: IndexedSeq[UUID]): Future[Boolean] = {
      transactional { implicit connection =>
        for {
          projects <- Future.sequence(projectIds.map(projectRepository.find)).map(_.map(_.get))
          wereAdded <- sectionRepository.removeProjects(section, projects)
        }
        yield wereAdded
      }
    }

    /**
     * Remove all projects from the section.
     *
     * @param section the [[Section]] to remove projects from
     * @param a [[Boolean]] indicating success or failure.
     */
    override def removeAllProjects(section: Section): Future[Boolean] = {
      transactional { implicit connection =>
        for {
          wereRemoved <- sectionRepository.removeAllProjects(section)
        }
        yield wereRemoved
      }
    }

    /**
     * Add students to a section.
     *
     * @param section the [[Section]] to add users to
     * @param userIds an [[IndexedSeq]] of [[UUID]] representing the [[User]]s to be added.
     * @param a [[Boolean]] indicating success or failure.
     */
    override def addUsers(section: Section, userIds: IndexedSeq[UUID]): Future[Boolean] = {
      transactional { implicit connection =>
        for {
          users <- Future.sequence(userIds.map(userRepository.find)).map(_.map(_.get))
          wereAdded <- sectionRepository.addUsers(section, users)
        }
        yield wereAdded
      }
    }

    /**
     * Remove students from a section.
     *
     * @param section the [[Section]] to remove users from
     * @param userIds an [[IndexedSeq]] of [[UUID]] representing the [[User]]s to be removed.
     * @param a [[Boolean]] indicating success or failure.
     */
    override def removeUsers(section: Section, userIds: IndexedSeq[UUID]): Future[Boolean] = {
      transactional { implicit connection =>
        for {
          users <- Future.sequence(userIds.map(userRepository.find)).map(_.map(_.get))
          wereRemoved <- sectionRepository.removeUsers(section, users)
        }
        yield wereRemoved
      }
    }

    /**
     * Force complete all responses for a given task in a given section.
     *
     * @param taskId the [[UUID]] of the task to be force-completed
     * @param sectionId the [[UUID]] of the section whose users will
     *                  have their responses force-completed
     * @return a boolean indicating success or failure
     */
    override def forceComplete(taskId: UUID, sectionId: UUID): Future[Boolean] = {
      transactional { implicit connection =>
        for {
          task <- taskRepository.find(taskId).map(_.get)
          section <- sectionRepository.find(sectionId).map(_.get)
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
        hasProject <- sectionRepository.hasProject(user, project)(db.pool)
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

    override def isPartEnabledForSection(partId: UUID, sectionId: UUID): Future[Boolean] = {
      for {
        part <- partRepository.find(partId).map(_.get)
        section <- sectionRepository.find(sectionId).map(_.get)
        isEnabled <- partRepository.isEnabled(part, section)
      }
      yield isEnabled
    }
  }

}
