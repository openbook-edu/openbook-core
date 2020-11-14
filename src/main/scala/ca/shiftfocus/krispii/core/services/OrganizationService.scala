package ca.shiftfocus.krispii.core.services

import java.util.UUID

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.user.User

import scala.concurrent.Future
import scalaz.\/

trait OrganizationService extends Service[ErrorUnion#Fail] {

  def find(organizationId: UUID): Future[\/[ErrorUnion#Fail, Organization]]
  def list: Future[\/[ErrorUnion#Fail, IndexedSeq[Organization]]]
  def listByAdmin(adminEmail: String): Future[\/[ErrorUnion#Fail, IndexedSeq[Organization]]]
  def listByMember(memberEmail: String): Future[\/[ErrorUnion#Fail, IndexedSeq[Organization]]]
  def listByTags(tags: IndexedSeq[(String, String)], distinct: Boolean = true): Future[\/[ErrorUnion#Fail, IndexedSeq[Organization]]]
  def searchMembers(key: String, organizationList: IndexedSeq[Organization]): Future[\/[ErrorUnion#Fail, IndexedSeq[User]]]
  def addMember(organizationId: UUID, memberEmail: String): Future[\/[ErrorUnion#Fail, Organization]]
  def deleteMember(organizationId: UUID, memberEmail: String): Future[\/[ErrorUnion#Fail, Organization]]
  def addAdmin(organizationId: UUID, adminEmail: String): Future[\/[ErrorUnion#Fail, Organization]]
  def deleteAdmin(organizationId: UUID, memberAdmin: String): Future[\/[ErrorUnion#Fail, Organization]]
  def create(title: String): Future[\/[ErrorUnion#Fail, Organization]]
  def update(id: UUID, version: Long, title: Option[String]): Future[\/[ErrorUnion#Fail, Organization]]
  def delete(id: UUID, version: Long): Future[\/[ErrorUnion#Fail, Organization]]
}