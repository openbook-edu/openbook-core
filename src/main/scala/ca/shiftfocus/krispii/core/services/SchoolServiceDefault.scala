package ca.shiftfocus.krispii.core.services

import java.awt.Color

import org.joda.time.DateTime
import play.api.Logger
import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.services.datasource.DB
import com.github.mauricio.async.db.Connection
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.repositories._
import java.util.UUID

import ca.shiftfocus.krispii.core.models.group.Course

import scala.collection.IndexedSeq
import scala.concurrent.Future
import scalaz.{-\/, \/, \/-}

import scala.concurrent.ExecutionContext.Implicits.global

class SchoolServiceDefault(
  val db: DB,
  val authService: AuthService,
  val userRepository: UserRepository,
  val courseRepository: CourseRepository,
  val chatRepository: ChatRepository,
  val wordRepository: WordRepository,
  val linkRepository: LinkRepository,
  val limitRepository: LimitRepository,
  val tagRepository: TagRepository,
  val organizationRepository: OrganizationRepository
)
    extends SchoolService {

  implicit def conn: Connection = db.pool

  /*
   * Methods for Courses
   */

  /**
   * List all courses.
   *
   * @return an IndexedSeq of group
   */
  override def listCourses: Future[\/[ErrorUnion#Fail, IndexedSeq[Course]]] = {
    courseRepository.list
  }

  /**
   * List all courses associated with a specific user.
   *
   * This finds courses for which the given id is set as a
   * student of the group via an association table.
   *
   * @param userId the UUID of the user to search for.
   * @param isDeleted defaults to false so it doesn't retrieve deleted courses, if set to true it will retrieve only deleted courses
   * @return an IndexedSeq of group
   */
  override def listCoursesByUser(userId: UUID, isDeleted: Boolean = false): Future[\/[ErrorUnion#Fail, IndexedSeq[Course]]] = {
    for {
      user <- lift(authService.find(userId))
      courses <- lift(listCoursesByUser(user))
    } yield courses.filter(course => course.deleted == isDeleted)
  }

  /**
   * List the courses or the given user
   * @param user the UUID of the user to search for.
   * @return an IndexedSeq of group
   */
  override def listCoursesByUser(user: User): Future[\/[ErrorUnion#Fail, IndexedSeq[Course]]] = {
    for {
      courses <- lift(courseRepository.list(user, false))
    } yield courses
  }

  /**
   * List all courses taught by a specific user.
   *
   * This finds courses for which the given id is set as the teacherID
   * parameter.
   *
   * @param userId the UUID of the user to search for.
   * @return an IndexedSeq of group
   */
  override def listCoursesByTeacher(userId: UUID, isDeleted: Boolean = false): Future[\/[ErrorUnion#Fail, IndexedSeq[Course]]] = {
    for {
      user <- lift(authService.find(userId))
      courses <- lift(courseRepository.list(user, true))
    } yield courses.filter(course => course.deleted == isDeleted)
  }

  /**
   * Find a specific group by id.
   *
   * @param id the UUID of the group to find.
   * @return an optional group
   */
  override def findCourse(id: UUID): Future[\/[ErrorUnion#Fail, Course]] = {
    courseRepository.find(id)
  }

  /**
   * Find a specific group by slug.
   *
   * @param slug the string of the group to find.
   * @return an optional group
   */
  override def findCourse(slug: String): Future[\/[ErrorUnion#Fail, Course]] = {
    courseRepository.find(slug)
  }

  /**
   * Create a new group.
   *
   * @param teacherId the optional UUID of the user teaching this group
   * @param name the name of this group
   * @return the newly created group
   */
  override def createCourse(teacherId: UUID, name: String, color: Color, slug: String): Future[\/[ErrorUnion#Fail, Course]] = {
    transactional { implicit conn =>
      for {
        teacher <- lift(authService.find(teacherId))
        _ <- predicate(teacher.roles.map(_.name).contains("teacher"))(ServiceError.BadPermissions("Tried to create a group for a user who isn't a teacher."))
        _ <- lift(isValidSlug(slug))
        newCourse = Course(
          ownerId = teacher.id,
          name = name,
          color = color,
          slug = slug
        )
        createdCourse <- lift(courseRepository.insert(newCourse))
      } yield createdCourse
    }
  }

  /**
   * Update group.
   *
   * @param id the UUID of the group this group belongs to
   * @param teacherId the optional UUID of the user teaching this group
   * @param name the name of this group
   * @return the newly created group
   */
  override def updateCourse(
    id: UUID,
    version: Long,
    teacherId: Option[UUID],
    name: Option[String],
    slug: Option[String],
    color: Option[Color],
    enabled: Option[Boolean],
    archived: Option[Boolean],
    schedulingEnabled: Option[Boolean],
    theaterMode: Option[Boolean],
    lastProjectId: Option[Option[UUID]],
    chatEnabled: Option[Boolean]
  ): Future[\/[ErrorUnion#Fail, Course]] = {
    transactional { implicit conn =>
      for {
        existingCourse <- lift(courseRepository.find(id))
        _ <- predicate(existingCourse.version == version)(ServiceError.OfflineLockFail)
        tId = teacherId.getOrElse(existingCourse.ownerId)
        teacher <- lift(authService.find(tId))
        _ <- predicate(slug.isEmpty || !existingCourse.enabled)(ServiceError.BusinessLogicFail("You can only change the slug for disabled courses."))
        _ <- predicate(teacher.roles.map(_.name).contains("teacher"))(ServiceError.BadPermissions("Tried to update a group for a user who isn't a teacher."))
        toUpdate = existingCourse.copy(
          ownerId = teacherId.getOrElse(existingCourse.ownerId),
          name = name.getOrElse(existingCourse.name),
          slug = slug.getOrElse(existingCourse.slug),
          color = color.getOrElse(existingCourse.color),
          enabled = enabled.getOrElse(existingCourse.enabled),
          archived = archived.getOrElse(existingCourse.archived),
          schedulingEnabled = schedulingEnabled.getOrElse(existingCourse.schedulingEnabled),
          theaterMode = theaterMode.getOrElse(existingCourse.theaterMode),
          lastProjectId = lastProjectId match {
            case Some(Some(lastProjectId)) => Some(lastProjectId)
            case Some(None) => None
            case None => existingCourse.lastProjectId
          },
          chatEnabled = chatEnabled.getOrElse(existingCourse.chatEnabled)
        )
        updatedCourse <- lift(courseRepository.update(toUpdate))
      } yield updatedCourse
    }
  }

  /**
   * Mark a group as deleted.
   *
   * @param id the unique ID of the group to update
   * @param version the latest version of the group for O.O.L.
   * @return group
   */
  override def deleteCourse(id: UUID, version: Long): Future[\/[ErrorUnion#Fail, Course]] = {
    transactional { implicit conn =>
      for {
        existingCourse <- lift(courseRepository.find(id))
        _ <- predicate(existingCourse.version == version)(ServiceError.OfflineLockFail)
        deletedCourse <- lift(courseRepository.delete(existingCourse))
      } yield deletedCourse
    }
  }

  // Utility functions for group management, assuming you already found a group object.

  /**
   * List all students registered to a group.
   */
  override def listStudents(courseId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[User]]] = {
    for {
      course <- lift(courseRepository.find(courseId))
      students <- lift(listStudents(course))
    } yield students
  }

  override def listStudents(course: Course): Future[\/[ErrorUnion#Fail, IndexedSeq[User]]] = {
    userRepository.list(course)
  }

  // TODO - to remove
  //  /**
  //   * List all projects belonging to a group.
  //   */
  //  override def listProjects(courseId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Project]]] = {
  //    for {
  //      group <- lift(courseRepository.find(courseId))
  //      projects <- lift(listProjects(group))
  //    } yield projects
  //  }

  /**
   * Add students to a group.
   *
   * @param course the group to add users to
   * @param userIds an IndexedSeq of UUID representing the users to be added.
   * @return a boolean indicating success or failure.
   */
  override def addUsers(course: Course, userIds: IndexedSeq[UUID]): Future[\/[ErrorUnion#Fail, IndexedSeq[User]]] = {
    transactional { implicit conn =>
      for {
        users <- lift(userRepository.list(userIds))
        wereAdded <- lift(courseRepository.addUsers(course, users))
        usersInCourse <- lift(userRepository.list(course))
      } yield usersInCourse
    }
  }

  /**
   * Remove students from a group.
   *
   * @param course the group to remove users from
   * @param userIds an IndexedSeq of UUID representing the users to be removed.
   * @return boolean indicating success or failure.
   */
  override def removeUsers(course: Course, userIds: IndexedSeq[UUID]): Future[\/[ErrorUnion#Fail, IndexedSeq[User]]] = {
    transactional { implicit conn =>
      for {
        users <- lift(userRepository.list(userIds))
        wereRemoved <- lift(courseRepository.removeUsers(course, users))
        usersInCourse <- lift(userRepository.list(course))
      } yield usersInCourse
    }
  }

  override def removeUser(course: Course, userId: UUID): Future[\/[ErrorUnion#Fail, User]] = {
    transactional { implicit conn =>
      for {
        user <- lift(userRepository.find(userId))
        wasRemoved <- lift(courseRepository.removeUser(user, course))
      } yield user
    }
  }

  /**
   * Given a user and teacher, finds whether this user belongs to any of that teacher's courses.
   *
   * @param userId
   * @param teacherId
   * @return
   */
  override def findUserForTeacher(userId: UUID, teacherId: UUID): Future[\/[ErrorUnion#Fail, User]] = {
    for {
      user <- lift(authService.find(userId))
      teacher <- lift(authService.find(teacherId))
      userCourses <- lift(courseRepository.list(user, false))
      filteredCourses = userCourses.filter(_.ownerId == teacherId)
      _ <- predicate(filteredCourses.nonEmpty)(RepositoryError.NoResults(s"User ${userId.toString} is not in any courses with teacher ${teacherId.toString}"))
    } yield user
  }

  /**
   * Set chatEnabled to a new value
   * @param courseId
   * @param chatEnabled
   * @return
   */
  def toggleCourseChat(courseId: UUID, chatEnabled: Boolean): Future[\/[ErrorUnion#Fail, Course]] =
    for {
      old <- lift(courseRepository.find(courseId))
      updated <- lift(courseRepository.update(old.copy(chatEnabled = chatEnabled)))
    } yield updated

  /**
   * List all chats for a group.
   *
   * @param courseId
   * @return
   */
  override def listChats(courseId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Chat]]] = {
    for {
      course <- lift(findCourse(courseId))
      chats <- lift(chatRepository.list(course))
    } yield chats
  }

  /**
   * List a slice of chats for a group.
   *
   * @param courseId
   * @param num
   * @param offset
   * @return
   */
  override def listChats(courseId: UUID, num: Long, offset: Long): Future[\/[ErrorUnion#Fail, IndexedSeq[Chat]]] = {
    for {
      course <- lift(findCourse(courseId))
      _ <- predicate(num > 0 && offset > 0)(ServiceError.BadInput("num, and offset parameters must be positive long integers"))
      chats <- lift(chatRepository.list(course, num, offset))
    } yield chats
  }

  /**
   * List all of one user's chats in a group.
   *
   * @param courseId
   * @param userId
   * @return
   */
  override def listChats(courseId: UUID, userId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[Chat]]] = {
    for {
      course <- lift(findCourse(courseId))
      user <- lift(userRepository.find(userId))
      chats <- lift(chatRepository.list(course, user))
    } yield chats
  }

  /**
   * List a slice of one user's chats in a group.
   *
   * @param courseId
   * @param userId
   * @param num
   * @param offset
   * @return
   */
  override def listChats(courseId: UUID, userId: UUID, num: Long, offset: Long): Future[\/[ErrorUnion#Fail, IndexedSeq[Chat]]] = {
    for {
      course <- lift(findCourse(courseId))
      user <- lift(userRepository.find(userId))
      _ <- predicate(num > 0 && offset > 0)(ServiceError.BadInput("num, and offset parameters must be positive long integers"))
      chats <- lift(chatRepository.list(course, user, num, offset))
    } yield chats
  }

  /**
   * Find a specific chat message.
   *
   * @param courseId
   * @param messageNum
   * @return
   */
  override def findChat(courseId: UUID, messageNum: Long): Future[\/[ErrorUnion#Fail, Chat]] = {
    for {
      course <- lift(findCourse(courseId))
      chat <- lift(chatRepository.find(course, messageNum))
    } yield chat
  }

  /**
   * Insert a new chat message.
   *
   * @param courseId
   * @param userId
   * @param message
   * @return
   */
  override def insertChat(courseId: UUID, userId: UUID, message: String): Future[\/[ErrorUnion#Fail, Chat]] = {
    val newChat = Chat(courseId = courseId, userId = userId, message = message)
    chatRepository.insert(newChat)
  }

  /**
   * Update an existing chat message's hidden status.
   *
   * @param courseId
   * @param messageNum
   * @param hidden
   * @return
   */
  override def updateChat(courseId: UUID, messageNum: Long, hidden: Boolean): Future[\/[ErrorUnion#Fail, Chat]] = {
    transactional { implicit conn =>
      for {
        existingChat <- lift(findChat(courseId, messageNum))
        newChat = existingChat.copy(hidden = hidden)
        updatedChat <- lift(chatRepository.update(newChat))
      } yield updatedChat
    }
  }
  override def getRandomWord(lang: String): Future[\/[ErrorUnion#Fail, LinkWord]] = {
    transactional { implicit conn =>
      wordRepository.get(lang)
    }
  }

  /**
   * Creating a new link for student registration.
   *
   * @param lang user's language
   * @param courseId group id
   * @return the link for students
   */
  override def createLink(lang: String, courseId: UUID): Future[\/[ErrorUnion#Fail, Link]] = {
    transactional { implicit conn =>
      for {
        word <- lift(wordRepository.get(lang))
        _ = Logger.debug("Creating link for registration: " + word.toString)
        link <- lift(linkRepository.create(Link(word.word, courseId, new DateTime())))

      } yield link
    }
  }

  /**
   * Find a link by link name
   *
   * @param link
   * @return
   */
  override def findLink(link: String): Future[\/[ErrorUnion#Fail, Link]] = {
    transactional { implicit conn =>
      linkRepository.find(link)
    }
  }

  /**
   * Delete a link based on group id. it will save us some parsing XD
   *
   * @param courseId
   * @return
   */
  override def deleteLink(courseId: UUID): Future[\/[ErrorUnion#Fail, Link]] = {
    transactional { implicit conn =>
      linkRepository.deleteByCourse(courseId)
    }
  }

  /**
   * Find a link by group id
   * @param courseId
   * @return
   */
  override def findLinkByCourse(courseId: UUID): Future[\/[ErrorUnion#Fail, Link]] = {
    transactional { implicit conn =>
      linkRepository.findByCourse(courseId)
    }
  }

  /**
   * Get number of courses that teacher is allowed to have
   *
   * @param teacherId
   * @return
   */
  override def getCourseLimit(teacherId: UUID): Future[\/[ErrorUnion#Fail, Int]] = {
    limitRepository.getCourseLimit(teacherId)
  }

  /**
   * Get number of courses that teacher is allowed to have within indicated plan
   *
   * @param planId
   * @return
   */
  override def getPlanCourseLimit(planId: String): Future[\/[ErrorUnion#Fail, Int]] = {
    limitRepository.getPlanCourseLimit(planId)
  }

  /**
   * Get storage (in GB) limit that teacher is allowed to have
   *
   * @param teacherId
   * @return MB
   */
  override def getStorageLimit(teacherId: UUID): Future[\/[ErrorUnion#Fail, Float]] = {
    limitRepository.getStorageLimit(teacherId)
  }

  /**
   * Get storage (in GB) limit that teacher is allowed to have within indicated plan
   *
   * @param planId
   * @return MB
   */
  override def getPlanStorageLimit(planId: String): Future[\/[ErrorUnion#Fail, Float]] = {
    limitRepository.getPlanStorageLimit(planId)
  }

  /**
   * Get storage (in GB) that is used by teacher
   *
   * @param teacherId
   * @return GB
   */
  override def getStorageUsed(teacherId: UUID): Future[\/[ErrorUnion#Fail, Float]] = {
    limitRepository.getStorageUsed(teacherId)
  }

  /**
   * Number of students that are allowed for teacher per group
   *
   * @param teacherId
   * @return
   */
  override def getTeacherStudentLimit(teacherId: UUID): Future[\/[ErrorUnion#Fail, Int]] = {
    limitRepository.getTeacherStudentLimit(teacherId)
  }

  /**
   * Get number of students that group is allowed to have
   * Limit by group has priority, if that is empty, then we get limit by teacher.
   *
   * @param courseId
   * @return
   */
  override def getCourseStudentLimit(courseId: UUID): Future[\/[ErrorUnion#Fail, Int]] = {
    limitRepository.getCourseStudentLimit(courseId).flatMap {
      case \/-(limit) => Future successful \/-(limit)
      case -\/(error: RepositoryError.NoResults) => {
        for {
          course <- lift(courseRepository.find(courseId))
          result <- lift(limitRepository.getTeacherStudentLimit(course.ownerId))
        } yield result
      }
      case -\/(error) => Future successful -\/(error)
    }
  }

  /**
   * Get number of students that courses are allowed to have within indicated plan
   *
   * @param planId
   * @return
   */
  override def getPlanStudentLimit(planId: String): Future[\/[ErrorUnion#Fail, Int]] = {
    limitRepository.getPlanStudentLimit(planId)
  }

  /**
   * Insert or update number of courses that teacher is allowed to have
   */
  override def setCourseLimit(teacherId: UUID, limit: Int): Future[\/[ErrorUnion#Fail, Int]] = {
    transactional { implicit conn =>
      for {
        limit <- limitRepository.setCourseLimit(teacherId, limit)
      } yield limit
    }
  }

  /**
   * Insert or update teacher storage limit
   */
  override def setStorageLimit(teacherId: UUID, limit: Float): Future[\/[ErrorUnion#Fail, Float]] = {
    transactional { implicit conn =>
      for {
        limit <- limitRepository.setStorageLimit(teacherId, limit)
      } yield limit
    }
  }

  /**
   * Set Number of students that are allowed for teacher per group
   *
   * @param teacherId
   * @param limit
   * @return
   */
  override def setTeacherStudentLimit(teacherId: UUID, limit: Int): Future[\/[ErrorUnion#Fail, Int]] = {
    transactional { implicit conn =>
      for {
        limit <- limitRepository.setTeacherStudentLimit(teacherId, limit)
      } yield limit
    }
  }

  /**
   * Insert or update number of courses that teacher is allowed to have
   */
  override def setCourseStudentLimit(courseId: UUID, limit: Int): Future[\/[ErrorUnion#Fail, Int]] = {
    transactional { implicit conn =>
      for {
        limit <- limitRepository.setCourseStudentLimit(courseId, limit)
      } yield limit
    }
  }

  override def deleteCourseStudentLimit(courseId: UUID): Future[\/[ErrorUnion#Fail, Unit]] = {
    transactional { implicit conn =>
      for {
        result <- limitRepository.deleteCourseStudentLimit(courseId)
      } yield result
    }
  }

  /**
   * Insert or update plan limit values
   */
  override def setPlanStorageLimit(palnId: String, limitValue: Float): Future[\/[ErrorUnion#Fail, Float]] = {
    limitRepository.setPlanStorageLimit(palnId, limitValue)
  }

  /**
   * Insert or update plan limit values
   */
  override def setPlanCourseLimit(palnId: String, limitValue: Int): Future[\/[ErrorUnion#Fail, Int]] = {
    limitRepository.setPlanCourseLimit(palnId, limitValue)
  }

  /**
   * Insert or update plan limit values
   */
  override def setPlanStudentLimit(palnId: String, limitValue: Int): Future[\/[ErrorUnion#Fail, Int]] = {
    limitRepository.setPlanStudentLimit(palnId, limitValue)
  }

  // Organization
  def getOrganizationStudentLimit(organizationId: UUID): Future[\/[ErrorUnion#Fail, Int]] = {
    limitRepository.getOrganizationStudentLimit(organizationId)
  }

  def getOrganizationCourseLimit(organizationId: UUID): Future[\/[ErrorUnion#Fail, Int]] = {
    limitRepository.getOrganizationCourseLimit(organizationId)
  }
  def getOrganizationStorageLimit(organizationtId: UUID): Future[\/[ErrorUnion#Fail, Float]] = {
    limitRepository.getOrganizationStorageLimit(organizationtId)
  }
  def getOrganizationDateLimit(organizationtId: UUID): Future[\/[ErrorUnion#Fail, DateTime]] = {
    limitRepository.getOrganizationDateLimit(organizationtId)
  }
  def getOrganizationMemberLimit(organizationtId: UUID): Future[\/[ErrorUnion#Fail, Int]] = {
    limitRepository.getOrganizationMemberLimit(organizationtId)
  }

  def setOrganizationStorageLimit(organizationId: UUID, limit: Float): Future[\/[ErrorUnion#Fail, Float]] = {
    limitRepository.setOrganizationStorageLimit(organizationId, limit)
  }
  def setOrganizationCourseLimit(organizationId: UUID, limit: Int): Future[\/[ErrorUnion#Fail, Int]] = {
    limitRepository.setOrganizationCourseLimit(organizationId, limit)
  }
  def setOrganizationStudentLimit(organizationId: UUID, limit: Int): Future[\/[ErrorUnion#Fail, Int]] = {
    limitRepository.setOrganizationStudentLimit(organizationId, limit)
  }
  def setOrganizationDateLimit(organizationId: UUID, limit: DateTime): Future[\/[ErrorUnion#Fail, DateTime]] = {
    limitRepository.setOrganizationDateLimit(organizationId, limit)
  }
  def setOrganizationMemberLimit(organizationtId: UUID, limit: Int): Future[\/[ErrorUnion#Fail, Int]] = {
    limitRepository.setOrganizationMemberLimit(organizationtId, limit)
  }

  /**
   * Check if a slug is of the valid format.
   *
   * @param slug the slug to be checked
   * @return a future disjunction containing either the slug, or a failure
   */
  private def isValidSlug(slug: String): Future[\/[ErrorUnion#Fail, String]] = Future successful {
    if ("""[A-Za-z0-9\-]+""".r.unapplySeq(slug).isDefined) \/-(slug)
    else -\/(ServiceError.BadInput(s"$slug is not a valid slug format."))
  }

  /**
   * Delete a chat message
   * @param courseId
   * @param messageNum
   * @return
   */
  override def deleteChat(courseId: UUID, messageNum: Long): Future[\/[ErrorUnion#Fail, Chat]] = {
    chatRepository.delete(courseId, messageNum)
  }

}
