package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.lib.{ ScalaCacheConfig, ScalaCachePool }
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.tasks.Task

case class CacheRepository(
    val scalaCacheConfig: ScalaCacheConfig
) {
  val cacheString: ScalaCachePool[String] = ScalaCachePool.buildRedis[String](scalaCacheConfig)
  val cacheSeqString: ScalaCachePool[IndexedSeq[String]] = ScalaCachePool.buildRedis[IndexedSeq[String]](scalaCacheConfig)

  val cacheInt: ScalaCachePool[Int] = ScalaCachePool.buildRedis[Int](scalaCacheConfig)
  val cacheSeqInt: ScalaCachePool[IndexedSeq[Int]] = ScalaCachePool.buildRedis[IndexedSeq[Int]](scalaCacheConfig)

  val cacheUUID: ScalaCachePool[UUID] = ScalaCachePool.buildRedis[UUID](scalaCacheConfig)
  val cacheSeqUUID: ScalaCachePool[IndexedSeq[UUID]] = ScalaCachePool.buildRedis[IndexedSeq[UUID]](scalaCacheConfig)

  val cacheCourse: ScalaCachePool[Course] = ScalaCachePool.buildRedis[Course](scalaCacheConfig)
  val cacheSeqCourse: ScalaCachePool[IndexedSeq[Course]] = ScalaCachePool.buildRedis[IndexedSeq[Course]](scalaCacheConfig)

  val cacheAccount: ScalaCachePool[Account] = ScalaCachePool.buildRedis[Account](scalaCacheConfig)
  val cacheSeqAccount: ScalaCachePool[IndexedSeq[Account]] = ScalaCachePool.buildRedis[IndexedSeq[Account]](scalaCacheConfig)

  val cacheUser: ScalaCachePool[User] = ScalaCachePool.buildRedis[User](scalaCacheConfig)
  val cacheSeqUser: ScalaCachePool[IndexedSeq[User]] = ScalaCachePool.buildRedis[IndexedSeq[User]](scalaCacheConfig)

  val cacheCourseSchedule: ScalaCachePool[CourseSchedule] = ScalaCachePool.buildRedis[CourseSchedule](scalaCacheConfig)
  val cacheSeqCourseSchedule: ScalaCachePool[IndexedSeq[CourseSchedule]] = ScalaCachePool.buildRedis[IndexedSeq[CourseSchedule]](scalaCacheConfig)

  val cacheCourseScheduleException: ScalaCachePool[CourseScheduleException] = ScalaCachePool.buildRedis[CourseScheduleException](scalaCacheConfig)
  val cacheSeqCourseScheduleException: ScalaCachePool[IndexedSeq[CourseScheduleException]] = ScalaCachePool.buildRedis[IndexedSeq[CourseScheduleException]](scalaCacheConfig)

  val cachePart: ScalaCachePool[Part] = ScalaCachePool.buildRedis[Part](scalaCacheConfig)
  val cacheSeqPart: ScalaCachePool[IndexedSeq[Part]] = ScalaCachePool.buildRedis[IndexedSeq[Part]](scalaCacheConfig)

  val cacheProject: ScalaCachePool[Project] = ScalaCachePool.buildRedis[Project](scalaCacheConfig)
  val cacheSeqProject: ScalaCachePool[IndexedSeq[Project]] = ScalaCachePool.buildRedis[IndexedSeq[Project]](scalaCacheConfig)

  val cacheRole: ScalaCachePool[Role] = ScalaCachePool.buildRedis[Role](scalaCacheConfig)
  val cacheSeqRole: ScalaCachePool[IndexedSeq[Role]] = ScalaCachePool.buildRedis[IndexedSeq[Role]](scalaCacheConfig)

  val cacheSession: ScalaCachePool[Session] = ScalaCachePool.buildRedis[Session](scalaCacheConfig)
  val cacheSeqSession: ScalaCachePool[IndexedSeq[Session]] = ScalaCachePool.buildRedis[IndexedSeq[Session]](scalaCacheConfig)

  val cacheTag: ScalaCachePool[Tag] = ScalaCachePool.buildRedis[Tag](scalaCacheConfig)
  val cacheSeqTag: ScalaCachePool[IndexedSeq[Tag]] = ScalaCachePool.buildRedis[IndexedSeq[Tag]](scalaCacheConfig)

  val cacheTask: ScalaCachePool[Task] = ScalaCachePool.buildRedis[Task](scalaCacheConfig)
  val cacheSeqTask: ScalaCachePool[IndexedSeq[Task]] = ScalaCachePool.buildRedis[IndexedSeq[Task]](scalaCacheConfig)
}
