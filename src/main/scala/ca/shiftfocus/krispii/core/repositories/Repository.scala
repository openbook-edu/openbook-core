package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.lib.concurrent.Lifting
import java.util.UUID
import scala.concurrent.duration._

trait Repository extends Lifting[RepositoryError.Fail] {
  val ttl = Some(15.minutes)
  def cacheUserKey(id: UUID): String = s"user[${id.toString}]"
  def cacheUsernameKey(username: String): String = s"userId[$username]"

  def cacheAccountKey(id: UUID): String = s"account[${id.toString}]"
  def cacheAccountUserKey(id: UUID): String = s"accountUser[${id.toString}]"

  def cacheRoleKey(id: UUID): String = s"role[${id.toString}]"
  def cacheRoleNameKey(name: String): String = s"roleId[$name]"
  def cacheRolesKey(userId: UUID): String = s"roles[${userId.toString}]"

  def cacheCoursesKey(id: UUID): String = s"courses[${id.toString}]"
  def cacheCourseKey(id: UUID): String = s"course[${id.toString}]"
  def cacheCourseSlugKey(slug: String): String = s"courseId[$slug]"
  def cacheTeachingKey(id: UUID): String = s"teaching[${id.toString}]"
  def cacheStudentsKey(id: UUID): String = s"students[${id.toString}]"

  def cacheTaskKey(id: UUID): String = s"task[${id.toString}]"
  def cacheTaskPosKey(projectId: UUID, partId: UUID, taskNum: Int): String = s"taskId[${projectId.toString}}, ${partId.toString}}, $taskNum]"
  def cacheTasksKey(id: UUID): String = s"tasks[${id.toString}]"

  def cachePartKey(id: UUID): String = s"part[${id.toString}]"
  def cachePartPosKey(projectId: UUID, partNum: Int): String = s"partId[${projectId.toString},$partNum]"
  def cachePartsKey(projectId: UUID): String = s"parts[${projectId.toString}]"

  def cacheProjectKey(id: UUID): String = s"project[${id.toString}]"
  def cacheProjectsKey(courseId: UUID): String = s"projects[${courseId.toString}]"
  def cacheProjectSlugKey(slug: String): String = s"projectId[$slug]"

  def cacheExamsKey(id: UUID): String = s"exams[${id.toString}]"
  def cacheExamKey(id: UUID): String = s"exam[${id.toString}]"
  def cacheExamSlugKey(slug: String): String = s"projectId[$slug]"
  def cacheScorersKey(id: UUID): String = s"scorers[${id.toString}]"

  def cacheTeamsKey(id: UUID): String = s"teams[${id.toString}]"
  def cacheTeamKey(id: UUID): String = s"team[${id.toString}]"

  def cacheTestsKey(id: UUID): String = s"tests[${id.toString}]"
  def cacheTestKey(id: UUID): String = s"test[${id.toString}]"

  def cacheScoresKey(id: UUID): String = s"scores[${id.toString}]"
  def cacheScoreKey(id: UUID): String = s"score[${id.toString}]"

  def cacheComponentKey(compId: UUID): String = s"component[${compId.toString}]"
  def cacheComponentsKey(id: UUID): String = s"components[${id.toString}]"

  def cacheScheduleKey(id: UUID): String = s"schedule[${id.toString}]"
  def cacheSchedulesKey(courseId: UUID): String = s"schedules[${courseId.toString}]"

  def cacheExceptionKey(id: UUID): String = s"exception[${id.toString}]"
  def cacheExceptionsKey(courseId: UUID): String = s"exceptions[${courseId.toString}]"
  def cacheExceptionsKey(courseId: UUID, userId: UUID): String = s"exceptions[${courseId.toString},${userId.toString}]"

  def cachePopularTagsKey(lang: String): String = s"popular_tags[${lang}]"
}
