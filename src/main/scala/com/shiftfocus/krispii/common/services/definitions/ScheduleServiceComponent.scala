package com.shiftfocus.krispii.common.services

import com.shiftfocus.krispii.lib.UUID
import com.shiftfocus.krispii.common.models._
import org.joda.time.LocalTime
import org.joda.time.LocalDate
import scala.concurrent.Future

trait ScheduleServiceComponent {

  val scheduleService: ScheduleService

  trait ScheduleService {

    def list: Future[IndexedSeq[SectionSchedule]]
    def listBySection(sectionId: UUID): Future[IndexedSeq[SectionSchedule]]
    def find(id: UUID): Future[Option[SectionSchedule]]

    def create(sectionId: UUID, day: LocalDate, startTime: LocalTime, endTime: LocalTime, description: String): Future[SectionSchedule]
    def update(id: UUID, version: Long, values: Map[String, Any]): Future[SectionSchedule]
    def delete(id: UUID, version: Long): Future[Boolean]

    def isAnythingScheduledForUser(userId: UUID, currentDay: LocalDate, currentTime: LocalTime): Future[Boolean]
    def isProjectScheduledForUser(projectSlug: String, userId: UUID, currentDay: LocalDate, currentTime: LocalTime): Future[Boolean]
  }
}

