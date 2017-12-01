package ca.shiftfocus.krispii.core.services

import java.awt.Color
import java.util.UUID

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.models._

import scala.concurrent.Future
import scalaz.\/

trait DemoService extends Service[ErrorUnion#Fail] {
  // COURSES
  def getCourse(lang: String): Future[\/[ErrorUnion#Fail, DemoCourse]]
  def createCourse(name: String, lang: String, color: Color): Future[\/[ErrorUnion#Fail, DemoCourse]]
  def updateCourse(id: UUID, name: Option[String], color: Option[Color]): Future[\/[ErrorUnion#Fail, DemoCourse]]
  def deleteCourse(id: UUID, version: Long): Future[\/[ErrorUnion#Fail, DemoCourse]]

  // PROJECTS
  def listProject(lang: String): Future[\/[ErrorUnion#Fail, IndexedSeq[DemoProject]]]
  def createProject(projectId: UUID, lang: String): Future[\/[ErrorUnion#Fail, DemoProject]]
  def deleteProject(projectId: UUID, lang: String): Future[\/[ErrorUnion#Fail, DemoProject]]
}
