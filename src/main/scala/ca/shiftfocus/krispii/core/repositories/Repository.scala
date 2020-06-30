package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.lib.concurrent.Lifting
import java.util.UUID
import scala.concurrent.duration._

trait Repository extends Lifting[RepositoryError.Fail] {
  val ttl = Some(15.minutes)
  def cacheUserKey(id: UUID): String = s"user[$id]"
  def cacheUsernameKey(username: String): String = s"userId[$username]"

  def cacheAccountKey(id: UUID): String = s"account[$id]"
  def cacheAccountUserKey(id: UUID): String = s"accountUser[$id]"

  def cacheRoleKey(id: UUID): String = s"role[$id]"
  def cacheRoleNameKey(name: String): String = s"roleId[$name]"
  def cacheRolesKey(userId: UUID): String = s"roles[${userId.toString}]"

  def cacheCoursesKey(id: UUID): String = s"courses[$id]"
  def cacheCourseKey(id: UUID): String = s"course[$id]"
  def cacheCourseSlugKey(slug: String): String = s"courseId[$slug]"
  def cacheTeachingKey(id: UUID): String = s"teaching[$id]"
  def cacheStudentsKey(id: UUID): String = s"students[$id]"

  def cacheTaskKey(id: UUID): String = s"task[$id]"
  def cacheTaskPosKey(projectId: UUID, partId: UUID, taskNum: Int): String = s"taskId[${projectId.toString}}, ${partId.toString}}, $taskNum]"
  def cacheTasksKey(id: UUID): String = s"tasks[$id]"

  def cachePartKey(id: UUID): String = s"part[$id]"
  def cachePartPosKey(projectId: UUID, partNum: Int): String = s"partId[${projectId.toString},$partNum]"
  def cachePartsKey(projectId: UUID): String = s"parts[${projectId.toString}]"

  def cacheProjectKey(id: UUID): String = s"project[$id]"
  def cacheProjectsKey(courseId: UUID): String = s"projects[$courseId]"
  def cacheProjectSlugKey(slug: String): String = s"projectId[$slug]"

  def cacheExamsKey(id: UUID): String = s"exams[$id]"
  def cacheExamKey(id: UUID): String = s"exam[$id]"
  def cacheExamSlugKey(slug: String): String = s"projectId[$slug]"
  def cacheCoordinatingKey(ownerId: UUID): String = s"coordinating[$ownerId]"
  def cacheScoringExamsKey(scorerId: UUID): String = s"scoring[$scorerId]"

  def cacheTeamsKey(id: UUID): String = s"teams[$id]"
  def cacheTeamKey(id: UUID): String = s"team[$id]"
  def cacheTeamScorersKey(id: UUID): String = s"scorers[$id]"
  // def cacheScoringTeamsKey(scorerId: UUID): String = s"scoring[$scorerId]"

  def cacheTestsKey(id: UUID): String = s"tests[$id]"
  def cacheTestKey(id: UUID): String = s"test[$id]"

  def cacheScoresKey(id: UUID): String = s"scores[$id]"
  def cacheScoreKey(id: UUID): String = s"score[$id]"

  def cacheComponentKey(id: UUID): String = s"component[$id]"
  // def cacheComponentsKey(id: UUID): String = s"components[$id]"

  def cacheScheduleKey(id: UUID): String = s"schedule[$id]"
  def cacheSchedulesKey(courseId: UUID): String = s"schedules[${courseId.toString}]"

  def cacheExceptionKey(id: UUID): String = s"exception[$id]"
  def cacheExceptionsKey(courseId: UUID): String = s"exceptions[$courseId]"
  def cacheExceptionsKey(courseId: UUID, userId: UUID): String = s"exceptions[$courseId,$userId]"

  def cachePopularTagsKey(lang: String): String = s"popular_tags[$lang]"
}
