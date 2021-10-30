package ca.shiftfocus.krispii.core.services

import java.util.UUID

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.user.User
import ca.shiftfocus.krispii.core.repositories.{OrganizationRepository, UserRepository}
import ca.shiftfocus.krispii.core.services.datasource.DB
import com.github.mauricio.async.db.Connection

import scala.concurrent.Future
import scalaz.\/

class OrganizationServiceDefault(
    val db: DB,
    val organizationRepository: OrganizationRepository,
    val userRepository: UserRepository
) extends OrganizationService {

  implicit def conn: Connection = db.pool

  override def find(organizationId: UUID): Future[\/[ErrorUnion#Fail, Organization]] =
    organizationRepository.find(organizationId)

  override def list: Future[\/[ErrorUnion#Fail, IndexedSeq[Organization]]] =
    organizationRepository.list

  override def listByAdmin(adminEmail: String): Future[\/[ErrorUnion#Fail, IndexedSeq[Organization]]] =
    organizationRepository.listByAdmin(adminEmail)

  override def listByMember(memberEmail: String): Future[\/[ErrorUnion#Fail, IndexedSeq[Organization]]] =
    organizationRepository.listByMember(memberEmail)

  /**
   * List organizations by tags
   *
   * @param tags (tagName:String, tagLang:String)
   * @param distinct Boolean If true each organization should have all listed tags,
   *                 if false organization should have at least one listed tag
   */
  override def listByTags(tags: IndexedSeq[(String, String)], distinct: Boolean = true): Future[\/[ErrorUnion#Fail, IndexedSeq[Organization]]] =
    organizationRepository.listByTags(tags, distinct)

  /**
   * List all members of all organizations on the list
   * @param organizationList IndexedSeq of organizations
   * @return IndexedSeq of Users, or an error
   */
  override def listMembers(organizationList: IndexedSeq[Organization]): Future[\/[ErrorUnion#Fail, IndexedSeq[User]]] =
    userRepository.listOrganizationMembers(organizationList)

  override def searchMembers(key: String, organizationList: IndexedSeq[Organization]): Future[\/[ErrorUnion#Fail, IndexedSeq[User]]] =
    userRepository.searchOrganizationMembers(key, organizationList)

  override def addMember(organization: Organization, memberEmail: String): Future[\/[ErrorUnion#Fail, Organization]] =
    organizationRepository.addMember(organization, memberEmail)

  override def addMember(organizationId: UUID, memberEmail: String): Future[\/[ErrorUnion#Fail, Organization]] =
    for {
      existingOrganization <- lift(organizationRepository.find(organizationId))
      organizationWithMember <- lift(organizationRepository.addMember(existingOrganization, memberEmail))
    } yield organizationWithMember

  override def deleteMember(organization: Organization, memberEmail: String): Future[\/[ErrorUnion#Fail, Organization]] =
    organizationRepository.deleteMember(organization, memberEmail)

  override def deleteMember(organizationId: UUID, memberEmail: String): Future[\/[ErrorUnion#Fail, Organization]] =
    for {
      existingOrganization <- lift(organizationRepository.find(organizationId))
      organizationWithoutMember <- lift(organizationRepository.deleteMember(existingOrganization, memberEmail))
    } yield organizationWithoutMember

  override def addAdmin(organization: Organization, adminEmail: String): Future[\/[ErrorUnion#Fail, Organization]] =
    organizationRepository.addAdmin(organization, adminEmail)

  override def addAdmin(organizationId: UUID, adminEmail: String): Future[\/[ErrorUnion#Fail, Organization]] =
    for {
      existingOrganization <- lift(organizationRepository.find(organizationId))
      organizationWithMember <- lift(organizationRepository.addAdmin(existingOrganization, adminEmail))
    } yield organizationWithMember

  override def deleteAdmin(organization: Organization, adminEmail: String): Future[\/[ErrorUnion#Fail, Organization]] =
    organizationRepository.deleteAdmin(organization, adminEmail)

  override def deleteAdmin(organizationId: UUID, adminEmail: String): Future[\/[ErrorUnion#Fail, Organization]] =
    for {
      existingOrganization <- lift(organizationRepository.find(organizationId))
      organizationWithoutMember <- lift(organizationRepository.deleteAdmin(existingOrganization, adminEmail))
    } yield organizationWithoutMember

  override def create(title: String): Future[\/[ErrorUnion#Fail, Organization]] =
    organizationRepository.insert(Organization(
      title = title
    ))

  override def update(id: UUID, version: Long, title: Option[String]): Future[\/[ErrorUnion#Fail, Organization]] =
    transactional { implicit conn =>
      (for {
        existingOrganization <- lift(organizationRepository.find(id))
        _ <- predicate(existingOrganization.version == version)(ServiceError.OfflineLockFail)
        updated <- lift(organizationRepository.update(existingOrganization.copy(
          title = title.getOrElse(existingOrganization.title)
        )))
      } yield updated).run
    }

  override def delete(id: UUID, version: Long): Future[\/[ErrorUnion#Fail, Organization]] =
    for {
      existingOrganization <- lift(organizationRepository.find(id))
      _ <- predicate(existingOrganization.version == version)(ServiceError.OfflineLockFail)
      deleted <- lift(organizationRepository.delete(existingOrganization))
    } yield deleted

}