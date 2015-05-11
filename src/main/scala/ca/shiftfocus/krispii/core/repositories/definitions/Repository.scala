package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.lib.concurrent.Lifting
import ca.shiftfocus.uuid.UUID
import scalacache._
import scalacache.redis._
import scala.concurrent.duration._
import scala.concurrent.Future
import scalacache.ScalaCache
import scalaz.{-\/, \/}
import scala.concurrent.ExecutionContext.Implicits.global

trait Repository extends Lifting[RepositoryError.Fail] {
  val ttl = Some(15.minutes)
  def cacheUserKey(id: UUID): String = s"user[${id.string}]"
  def cacheUsernameKey(username: String): String = s"userId[$username]"

  def cacheRoleKey(id: UUID): String = s"role[${id.string}]"
  def cacheRoleNameKey(name: String): String = s"roleId[$name]"
  def cacheRolesKey(userId: UUID): String = s"roles[${userId.string}]"

  def cacheStudentsKey(id: UUID): String = s"students[${id.string}]"
  def cacheCoursesKey(id: UUID): String = s"courses[${id.string}]"
  def cacheCourseKey(id: UUID): String = s"course[${id.string}]"
  def cacheCourseSlugKey(slug: String): String = s"courseId[$slug]"
  def cacheTeachingKey(id: UUID): String = s"teaching[${id.string}]"

  def cacheTaskKey(id: UUID): String = s"task[${id.string}]"
  def cacheTaskPosKey(projectId: UUID, partId: UUID, taskNum: Int): String = s"taskId[${projectId.string}}, ${partId.string}}, $taskNum]"
  def cacheTasksKey(id: UUID): String = s"tasks[${id.string}]"

  def cachePartKey(id: UUID): String = s"part[${id.string}]"
  def cachePartPosKey(projectId: UUID, partNum: Int): String = s"partId[${projectId.string},$partNum]"
  def cachePartsKey(projectId: UUID): String = s"parts[${projectId.string}]"

  def cacheProjectKey(id: UUID): String = s"project[${id.string}]"
  def cacheProjectsKey(courseId: UUID): String = s"projects[${courseId.string}]"
  def cacheProjectSlugKey(slug: String): String = s"projectId[$slug]"

  def cacheComponentKey(compId: UUID): String = s"component[${compId.string}]"
  def cacheComponentsKey(id: UUID): String = s"components[${id.string}]"

  def cacheScheduleKey(id: UUID): String = s"schedule[${id.string}]"
  def cacheSchedulesKey(courseId: UUID): String = s"schedules[${courseId.string}]"

  def cacheExceptionKey(id: UUID): String = s"exception[${id.string}]"
  def cacheExceptionsKey(courseId: UUID): String = s"exceptions[${courseId.string}]"
  def cacheExceptionsKey(courseId: UUID, userId: UUID): String = s"exceptions[${courseId.string},${userId.string}]"

  def getCached[V](key: String)(implicit cache: ScalaCache): Future[\/[RepositoryError.Fail, V]] = {
    get[V](key).map {
      case Some(entity) => \/.right[RepositoryError.Fail, V](entity)
      case None => \/.left[RepositoryError.Fail, V](RepositoryError.NoResults)
    }.recover {
      case failed: RuntimeException => -\/(RepositoryError.DatabaseError("Failed to read from cache.", Some(failed)))
      case exception: Throwable => throw exception
    }
  }

  def putCache[V](key: String)(value: V, ttl: Option[Duration] = None)(implicit cache: ScalaCache): Future[\/[RepositoryError.Fail, Unit]] = {
    put[V](key)(value, ttl).map({unit => \/.right[RepositoryError.Fail, Unit](unit)}).recover {
      case failed: RuntimeException => -\/(RepositoryError.DatabaseError("Failed to write to cache.", Some(failed)))
      case exception: Throwable => throw exception
    }
  }

  def removeCached(key: String)(implicit cache: ScalaCache): Future[\/[RepositoryError.Fail, Unit]] = {
    remove(key).map({unit => \/.right[RepositoryError.Fail, Unit](unit)}).recover {
      case failed: RuntimeException => -\/(RepositoryError.DatabaseError("Failed to remove from cache.", Some(failed)))
      case exception: Throwable => throw exception
    }
  }
}