package com.shiftfocus.krispii.core.repositories

import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import com.shiftfocus.krispii.core.lib._
import com.shiftfocus.krispii.core.models._
import scala.concurrent.Future

trait SectionScheduleExceptionRepositoryComponent {
  val sectionScheduleExceptionRepository: SectionScheduleExceptionRepository

  trait SectionScheduleExceptionRepository {
    def list(section: Section): Future[IndexedSeq[SectionScheduleException]]
    def list(user: User, section: Section): Future[IndexedSeq[SectionScheduleException]]
    def find(id: UUID): Future[Option[SectionScheduleException]]
    def insert(sectionSchedule: SectionScheduleException)(implicit conn: Connection): Future[SectionScheduleException]
    def update(sectionSchedule: SectionScheduleException)(implicit conn: Connection): Future[SectionScheduleException]
    def delete(sectionSchedule: SectionScheduleException)(implicit conn: Connection): Future[Boolean]
  }
}
