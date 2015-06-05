package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.lib.concurrent.Lifting
import java.util.UUID
import com.typesafe.config.ConfigFactory
import scalacache._
import scalacache.redis._
import scala.concurrent.duration._
import scala.concurrent.Future
import scalacache.ScalaCache
import scalaz.{-\/, \/}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

trait Repository extends Lifting[RepositoryError.Fail] {
  val ttl = Some(15.minutes)
  def cacheUserKey(id: UUID): String = s"user[${id.toString}]"
  def cacheUsernameKey(username: String): String = s"userId[$username]"

  def cacheRoleKey(id: UUID): String = s"role[${id.toString}]"
  def cacheRoleNameKey(name: String): String = s"roleId[$name]"
  def cacheRolesKey(userId: UUID): String = s"roles[${userId.toString}]"

  def cacheStudentsKey(id: UUID): String = s"students[${id.toString}]"
  def cacheCoursesKey(id: UUID): String = s"courses[${id.toString}]"
  def cacheCourseKey(id: UUID): String = s"course[${id.toString}]"
  def cacheCourseSlugKey(slug: String): String = s"courseId[$slug]"
  def cacheTeachingKey(id: UUID): String = s"teaching[${id.toString}]"

  def cacheTaskKey(id: UUID): String = s"task[${id.toString}]"
  def cacheTaskPosKey(projectId: UUID, partId: UUID, taskNum: Int): String = s"taskId[${projectId.toString}}, ${partId.toString}}, $taskNum]"
  def cacheTasksKey(id: UUID): String = s"tasks[${id.toString}]"

  def cachePartKey(id: UUID): String = s"part[${id.toString}]"
  def cachePartPosKey(projectId: UUID, partNum: Int): String = s"partId[${projectId.toString},$partNum]"
  def cachePartsKey(projectId: UUID): String = s"parts[${projectId.toString}]"

  def cacheProjectKey(id: UUID): String = s"project[${id.toString}]"
  def cacheProjectsKey(courseId: UUID): String = s"projects[${courseId.toString}]"
  def cacheProjectSlugKey(slug: String): String = s"projectId[$slug]"

  def cacheComponentKey(compId: UUID): String = s"component[${compId.toString}]"
  def cacheComponentsKey(id: UUID): String = s"components[${id.toString}]"

  def cacheScheduleKey(id: UUID): String = s"schedule[${id.toString}]"
  def cacheSchedulesKey(courseId: UUID): String = s"schedules[${courseId.toString}]"

  def cacheExceptionKey(id: UUID): String = s"exception[${id.toString}]"
  def cacheExceptionsKey(courseId: UUID): String = s"exceptions[${courseId.toString}]"
  def cacheExceptionsKey(courseId: UUID, userId: UUID): String = s"exceptions[${courseId.toString},${userId.toString}]"
}
