package ca.shiftfocus.krispii.core.services

import java.util.UUID

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.repositories.OrganizationRepository
import ca.shiftfocus.krispii.core.services.datasource.DB
import com.github.mauricio.async.db.Connection

import scala.concurrent.Future
import scalaz.\/

class OrganizationServiceDefault(
    val db: DB,
    val organizationRepository: OrganizationRepository
) extends OrganizationService {

  implicit def conn: Connection = db.pool

  def find(organizationId: UUID): Future[\/[ErrorUnion#Fail, Organization]] = {
    organizationRepository.find(organizationId)
  }

  def list: Future[\/[ErrorUnion#Fail, IndexedSeq[Organization]]] = {
    organizationRepository.list
  }

  def create(title: String): Future[\/[ErrorUnion#Fail, Organization]] = {
    organizationRepository.insert(Organization(
      title = title
    ))
  }

  def update(
    id: UUID,
    version: Long,
    title: Option[String],
    adminEmail: Option[Option[String]]
  ): Future[\/[ErrorUnion#Fail, Organization]] = {
    transactional { implicit conn =>
      (for {
        existingOrganization <- lift(organizationRepository.find(id))
        _ <- predicate(existingOrganization.version == version)(ServiceError.OfflineLockFail)
        updated <- lift(organizationRepository.update(existingOrganization.copy(
          title = title.getOrElse(existingOrganization.title),
          adminEmail = adminEmail match {
            case Some(Some(adminEmail)) => Some(adminEmail)
            case Some(None) => None
            case None => existingOrganization.adminEmail
          }
        )))
      } yield updated).run
    }
  }

  def delete(id: UUID, version: Long): Future[\/[ErrorUnion#Fail, Organization]] = {
    (for {
      existingOrganization <- lift(organizationRepository.find(id))
      _ <- predicate(existingOrganization.version == version)(ServiceError.OfflineLockFail)
      deleted <- lift(organizationRepository.delete(existingOrganization))
    } yield deleted).run
  }
}