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
  def cacheRolesKey(userId: UUID): String = s"roles[$userId]"

  def cacheCoursesKey(id: UUID): String = s"courses[$id]"
  def cacheCourseKey(id: UUID): String = s"group[$id]"
  def cacheCourseSlugKey(slug: String): String = s"groupId[$slug]"
  def cacheTeachingKey(id: UUID): String = s"teaching[$id]"
  def cacheStudentsKey(id: UUID): String = s"students[$id]"

  def cacheTaskKey(id: UUID): String = s"task[$id]"
  def cacheTaskPosKey(projectId: UUID, partId: UUID, taskNum: Int): String = s"taskId[$projectId,$partId,$taskNum]"
  def cacheTasksKey(id: UUID): String = s"tasks[$id]"

  def cachePartKey(id: UUID): String = s"part[$id]"
  def cachePartPosKey(projectId: UUID, partNum: Int): String = s"partId[$projectId,$partNum]"
  def cachePartsKey(projectId: UUID): String = s"parts[${projectId.toString}]"

  def cacheProjectKey(id: UUID): String = s"project[$id]"
  def cacheProjectsKey(courseId: UUID): String = s"projects[$courseId]"
  def cacheProjectSlugKey(slug: String): String = s"projectId[$slug]"

  def cacheExamKey(id: UUID): String = s"exam[$id]"
  // def cacheExamsKey(id: UUID): String = s"exams[$id]"
  // we prefer to keep coordinator and scorer roles for separate users, but separate keys just in case
  def cacheExamSlugKey(slug: String): String = s"examId[$slug]"
  def cacheCoordinatorExamsKey(ownerId: UUID): String = s"coordinatorExams[$ownerId]"
  def cacheScorerExamsKey(scorerId: UUID): String = s"scorerExams[$scorerId]"

  def cacheTeamKey(id: UUID): String = s"team[$id]"
  def cacheTeamsKey(id: UUID): String = s"teams[$id]"
  // we prefer to keep coordinator and scorer roles separate, but separate keys just in case
  def cacheCoordinatorTeamsKey(ownerId: UUID): String = s"coordinatorTeams[$ownerId]"
  def cacheScorerTeamsKey(scorerId: UUID): String = s"scorerTeams[$scorerId]"
  def cacheTeamScorersKey(teamId: UUID): String = s"teamScorers[$teamId]"

  def cacheTestKey(id: UUID): String = s"test[$id]"
  def cacheTestNameKey(name: String, examId: UUID): String = s"testId[$name,$examId]"
  // no practical risk of having the same UUID identify both an exam and a team
  def cacheTestsKey(id: UUID): String = s"tests[$id]"

  def cacheScoreKey(id: UUID): String = s"score[$id]"
  /* No practical risk of having the same UUID identify more than one entity(exam, team, test) at the same time.
     However, ScoreRepositoryPostgres and TestRepositoryPostgres can't mutually instantiate each other,
     so lists of scores for exam and for team cannot be implemented.
   */
  def cacheScorerKey(entityId: UUID, scorerId: UUID): String = s"scorer[$entityId,$scorerId]"
  def cacheScoresKey(id: UUID): String = s"scores[$id]"

  def cacheComponentKey(id: UUID): String = s"component[$id]"
  def cacheComponentsKey(id: UUID): String = s"components[$id]"

  def cacheScheduleKey(id: UUID): String = s"schedule[$id]"
  def cacheSchedulesKey(courseId: UUID): String = s"schedules[${courseId.toString}]"

  def cacheExceptionKey(id: UUID): String = s"exception[$id]"
  def cacheExceptionsKey(courseId: UUID): String = s"exceptions[$courseId]"
  def cacheExceptionsKey(courseId: UUID, userId: UUID): String = s"exceptions[$courseId,$userId]"

  def cachePopularTagsKey(lang: String): String = s"popular_tags[$lang]"
}
