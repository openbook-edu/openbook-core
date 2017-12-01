package ca.shiftfocus.krispii.core.services

import java.awt.Color
import java.util.UUID

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.lib.ScalaCachePool
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.krispii.core.services.datasource.DB
import com.github.mauricio.async.db.Connection

import scala.concurrent.Future
import scalaz.\/

class DemoServiceDefault(
    val db: DB,
    val scalaCache: ScalaCachePool,
    val demoCourseRepository: DemoCourseRepository,
    val demoProjectRepository: DemoProjectRepository
) extends DemoService {

  implicit def conn: Connection = db.pool
  implicit def cache: ScalaCachePool = scalaCache

  // COURSES
  def getCourse(lang: String): Future[\/[ErrorUnion#Fail, DemoCourse]] = {
    demoCourseRepository.find(lang)
  }

  def createCourse(name: String, lang: String, color: Color): Future[\/[ErrorUnion#Fail, DemoCourse]] = {
    demoCourseRepository.insert(DemoCourse(
      name = name,
      lang = lang,
      color = color
    ))
  }

  def updateCourse(id: UUID, name: Option[String], color: Option[Color]): Future[\/[ErrorUnion#Fail, DemoCourse]] = {
    for {
      demoCourse <- lift(demoCourseRepository.find(id))
      result <- lift(demoCourseRepository.update(demoCourse.copy(
        name = name.getOrElse(demoCourse.name),
        color = color.getOrElse(demoCourse.color)
      )))
    } yield result
  }

  def deleteCourse(id: UUID, version: Long): Future[\/[ErrorUnion#Fail, DemoCourse]] = {
    for {
      demoCourse <- lift(demoCourseRepository.find(id))
      _ <- predicate(demoCourse.version == version)(RepositoryError.OfflineLockFail)
      result <- lift(demoCourseRepository.delete(demoCourse))
    } yield result
  }

  // PROJECTS
  def listProject(lang: String): Future[\/[ErrorUnion#Fail, IndexedSeq[DemoProject]]] = {
    demoProjectRepository.list(lang)
  }

  def createProject(projectId: UUID, lang: String): Future[\/[ErrorUnion#Fail, DemoProject]] = {
    demoProjectRepository.insert(DemoProject(
      projectId = projectId,
      lang = lang
    ))
  }

  def deleteProject(projectId: UUID, lang: String): Future[\/[ErrorUnion#Fail, DemoProject]] = {
    for {
      demoProject <- lift(demoProjectRepository.find(projectId, lang))
      result <- lift(demoProjectRepository.delete(demoProject))
    } yield result
  }
}
