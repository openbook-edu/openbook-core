package ca.shiftfocus.krispii.core.services

import java.util.UUID

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.models._

import scala.concurrent.Future
import scalaz.\/

trait OrganizationService extends Service[ErrorUnion#Fail] {

  def find(organizationId: UUID): Future[\/[ErrorUnion#Fail, Organization]]
  def list: Future[\/[ErrorUnion#Fail, IndexedSeq[Organization]]]
  def list(adminEmail: String): Future[\/[ErrorUnion#Fail, IndexedSeq[Organization]]]
  def addMember(organizationId: UUID, memberEmail: String): Future[\/[ErrorUnion#Fail, Organization]]
  def deleteMember(organizationId: UUID, memberEmail: String): Future[\/[ErrorUnion#Fail, Organization]]
  def create(title: String): Future[\/[ErrorUnion#Fail, Organization]]
  def update(id: UUID, version: Long, title: Option[String], adminEmail: Option[Option[String]]): Future[\/[ErrorUnion#Fail, Organization]]
  def delete(id: UUID, version: Long): Future[\/[ErrorUnion#Fail, Organization]]
}