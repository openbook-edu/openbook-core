import org.scalatest._
import Matchers._
import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.Organization
import ca.shiftfocus.krispii.core.repositories.{OrganizationRepository, UserRepository}
import ca.shiftfocus.krispii.core.services.OrganizationServiceDefault
import ca.shiftfocus.krispii.core.services.datasource.DB
import com.github.mauricio.async.db.Connection
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import scalaz._

import java.util.UUID
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

@RunWith(classOf[JUnitRunner])
class OrganizationServiceSpec
  extends TestEnvironment(writeToDb =false){
  val db = stub[DB]
  val mockConnection = stub[Connection]
  val organizationRepository = stub[OrganizationRepository]
  val userRepository = stub[UserRepository]

  val orgService = new OrganizationServiceDefault(db, organizationRepository, userRepository) {
    override implicit def conn: Connection = mockConnection
    override def transactional[A](f: Connection => Future[A]): Future[A] = {
      f(mockConnection)
    }
  }

  "orgService.find" should {
    inSequence {
      val organization = TestValues.testOrganizationA
      "return Organization if exists" in {
        (organizationRepository.find(_: UUID)(_: Connection)) when(organization.id, *) returns(Future.successful(\/-(organization)))

        val result = orgService.find(organization.id)

        Await.result(result, Duration.Inf) should be (\/-(organization))
      }

      "return RepositoryError if organization doesn't exist" in {
        (organizationRepository.find(_: UUID)(_: Connection)) when(organization.id, *) returns(Future.successful(-\/(RepositoryError.NoResults(""))))

        val result = orgService.find(organization.id)

        Await.result(result, Duration.Inf) should be (-\/(RepositoryError.NoResults("")))
      }
    }
  }

  "orgService.list" should {
    inSequence {
      "return list of organization" in {
        val orgList = IndexedSeq(TestValues.testOrganizationA, TestValues.testOrganizationB)
        (organizationRepository.list(_: Connection)) when(*) returns (Future.successful(\/-(orgList)))

        val result = orgService.list

        Await.result(result, Duration.Inf) should be (\/-(orgList))
      }

      "return RepositoryError.NoResults if no organization" in {
        (organizationRepository.list(_: Connection)) when(*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))

        val result = orgService.list

        Await.result(result, Duration.Inf) should be (-\/(RepositoryError.NoResults("")))
      }
    }
  }

  "orgService.listByAdmin" should {
    inSequence {
      "return list of organization if admin mail is correct" in {
        val adminEmail = TestValues.testOrganizationA.admins.head
        val orgList = IndexedSeq(TestValues.testOrganizationA, TestValues.testOrganizationB)
        (organizationRepository.listByAdmin(_: String)(_: Connection)) when(adminEmail, *) returns(Future.successful(\/-(orgList)))

        val result = orgService.listByAdmin(adminEmail)

        Await.result(result, Duration.Inf) should be (\/-(orgList))
      }

      "return RepositoryError if adminEmail is wrong" in {
        val adminEmail = TestValues.testOrganizationA.admins.head
        (organizationRepository.listByAdmin(_: String)(_: Connection)) when(adminEmail, *) returns(Future.successful(-\/(RepositoryError.NoResults(""))))

        val result = orgService.listByAdmin(adminEmail)

        Await.result(result, Duration.Inf) should be (-\/(RepositoryError.NoResults("")))
      }
    }
  }

  "orgService.listByMember" should {
    inSequence {
      "return organization(s) which member belongs" in {
        val memberEmail = TestValues.testUserB.email
        val orgList = IndexedSeq(TestValues.testOrganizationA)
        (organizationRepository.listByMember(_: String)(_: Connection)) when(memberEmail, *) returns(Future.successful(\/-(orgList)))

        val result = orgService.listByMember(memberEmail)

        Await.result(result, Duration.Inf) should be (\/-(orgList))
      }

      "return RepositoryError.NoResults if member doesn't belong any organization" in {
        val memberEmail = TestValues.testUserB.email
        (organizationRepository.listByMember(_: String)(_: Connection)) when(memberEmail, *) returns(Future.successful(-\/(RepositoryError.NoResults(""))))

        val result = orgService.listByMember(memberEmail)

        Await.result(result, Duration.Inf) should be (-\/(RepositoryError.NoResults("")))
      }
    }
  }

  "orgService.listByTag" should {
    inSequence {
      "return organization(s) whose have given tag if listed tags are distinct" in {
        val tags = IndexedSeq((TestValues.testTagA.name, TestValues.testTagA.lang))
        val orgList = IndexedSeq(TestValues.testOrganizationA, TestValues.testOrganizationB)
        (organizationRepository.listByTags(_: IndexedSeq[(String, String)], _: Boolean)(_: Connection)) when(tags, true, *) returns (Future.successful(\/-(orgList)))

        val result = orgService.listByTags(tags)

        Await.result(result, Duration.Inf) should be(\/-(orgList))
      }

      "return organization(s) whose have at least one of listed tag if listed tags aren't distinct" in {
        val tags = IndexedSeq((TestValues.testTagA.name, TestValues.testTagB.lang))
        val orgList = IndexedSeq(TestValues.testOrganizationA, TestValues.testOrganizationB, TestValues.testOrganizationC)
        (organizationRepository.listByTags(_: IndexedSeq[(String, String)], _: Boolean)(_: Connection)) when(tags, true, *) returns (Future.successful(\/-(orgList)))

        val result = orgService.listByTags(tags)

        Await.result(result, Duration.Inf) should be(\/-(orgList))
      }

      "return RepositoryError.NoResults if no organization has given tag" in {
        val tags = IndexedSeq((TestValues.testTagX.name, TestValues.testTagX.lang))
        (organizationRepository.listByTags(_: IndexedSeq[(String, String)], _: Boolean)(_: Connection)) when(tags, true, *) returns (Future.successful(-\/(RepositoryError.NoResults(""))))

        val result = orgService.listByTags(tags)

        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("")))
      }
    }
  }

  "OrgService.listMembers" should {
    inSequence {
      "return all members of listed organization" in {
        val listedOrg = IndexedSeq(TestValues.testOrganizationA, TestValues.testOrganizationB)
        val membersList = IndexedSeq(TestValues.testUserB, TestValues.testUserC, TestValues.testUserD)
        (userRepository.listOrganizationMembers(_: IndexedSeq[Organization])(_: Connection)) when(listedOrg, *) returns(Future.successful(\/-(membersList)))

        val result = orgService.listMembers(listedOrg)

        Await.result(result, Duration.Inf) should be (\/-(membersList))
      }

      "return RepositoryError.BadParam if organization has any member" in {
        val listedOrg = IndexedSeq(TestValues.testOrganizationC)
        (userRepository.listOrganizationMembers(_: IndexedSeq[Organization])(_: Connection)) when(listedOrg, *) returns(Future.successful(-\/(RepositoryError.BadParam("core.UserRepositoryPostgres.searchOrganizationTeammate.org.empty"))))

        val result = orgService.listMembers(listedOrg)

        Await.result(result, Duration.Inf) should be (-\/(RepositoryError.BadParam("core.UserRepositoryPostgres.searchOrganizationTeammate.org.empty")))
      }
    }
  }

  "orgService.addMember" should {
    inSequence {
      val organization = TestValues.testOrganizationB
      val memberEmailToAdd = TestValues.testUserA.email
      "return organization with added member in members" in {
        val newMembersList = IndexedSeq(TestValues.testUserC.email, TestValues.testUserD.email, memberEmailToAdd)
        val updatedOrganization = organization.copy(members = newMembersList)
        (organizationRepository.addMember(_: Organization, _: String)(_: Connection)) when(organization, memberEmailToAdd, *) returns(Future.successful(\/-(updatedOrganization)))

        val result = orgService.addMember(organization, memberEmailToAdd)
        val \/-(fOrganization) = Await.result(result, Duration.Inf)

        fOrganization.id should be (updatedOrganization.id)
        fOrganization.admins should be (updatedOrganization.admins)
        fOrganization.members should be (updatedOrganization.members)
      }

      "return organization with added member in members when adding member by using organization id" in {
        val newMembersList = IndexedSeq(TestValues.testUserC.email, TestValues.testUserD.email, memberEmailToAdd)
        val updatedOrganization = organization.copy(members = newMembersList)
        (organizationRepository.find(_: UUID)(_: Connection)) when(organization.id, *) returns (Future.successful(\/-(organization)))
        (organizationRepository.addMember(_: Organization, _: String)(_: Connection)) when(organization, memberEmailToAdd, *) returns(Future.successful(\/-(updatedOrganization)))

        val result = orgService.addMember(organization.id, memberEmailToAdd)
        val \/-(fOrganization) = Await.result(result, Duration.Inf)

        fOrganization.id should be (updatedOrganization.id)
        fOrganization.admins should be (updatedOrganization.admins)
        fOrganization.members should be (updatedOrganization.members)
      }

      "return RepositoryError if organisation doesn't exist" in {
        (organizationRepository.addMember(_: Organization, _: String)(_: Connection)) when(organization, memberEmailToAdd, *) returns(Future.successful(-\/(RepositoryError.NoResults(s"No ResultSet was returned. Could not add member to organization"))))

        val result = orgService.addMember(organization, memberEmailToAdd)

        Await.result(result, Duration.Inf) should be (-\/(RepositoryError.NoResults(s"No ResultSet was returned. Could not add member to organization")))
      }
    }
  }

  "orgService.deleteMember" should {
    inSequence {
      val organization = TestValues.testOrganizationB
      val memberMailToDelete = TestValues.testUserD.email
      "return organization without deleted member if organization exists" in {
        val updatedOrganization = organization.copy(members = IndexedSeq(TestValues.testUserC.email))
        (organizationRepository.deleteMember(_: Organization, _: String)(_: Connection)) when(organization, memberMailToDelete, *) returns(Future.successful(\/-(updatedOrganization)))

        val result = orgService.deleteMember(organization, memberMailToDelete)
        val \/-(fOrganization) = Await.result(result, Duration.Inf)

        fOrganization.members.contains(memberMailToDelete) should be (false)
        fOrganization.id should be (updatedOrganization.id)
        fOrganization.admins should be (updatedOrganization.admins)
        fOrganization.members should be (updatedOrganization.members)
      }

      "return RepositoryError if organisation doesn't exist or list of members is empty" in {
        (organizationRepository.addMember(_: Organization, _: String)(_: Connection)) when(organization, memberMailToDelete, *) returns(Future.successful(-\/(RepositoryError.NoResults(s"No ResultSet was returned. Could not remove member from organization"))))

        val result = orgService.addMember(organization, memberMailToDelete)

        Await.result(result, Duration.Inf) should be (-\/(RepositoryError.NoResults(s"No ResultSet was returned. Could not remove member from organization")))
      }
    }
  }

  "orgService.addAdmin" should {
    inSequence {
      val organization = TestValues.testOrganizationA
      val adminEmailToAdd = TestValues.testUserB.email
      "return organization with added admin if organization exists" in {
        val newAdminList = IndexedSeq(TestValues.testUserA.email, adminEmailToAdd)
        val updatedOrganization = organization.copy(admins = newAdminList)

        (organizationRepository.addAdmin(_: Organization, _: String)(_: Connection)) when(organization, adminEmailToAdd, *) returns(Future.successful(\/-(updatedOrganization)))

        val result = orgService.addAdmin(organization, adminEmailToAdd)
        val \/-(fOrganization) = Await.result(result, Duration.Inf)

        fOrganization.id should be (updatedOrganization.id)
        fOrganization.admins should be (updatedOrganization.admins)
        fOrganization.members should be (updatedOrganization.members)
        fOrganization.admins.contains(adminEmailToAdd) should be (true)
      }

      "return RepositoryError if organisation doesn't exist" in {
        (organizationRepository.addMember(_: Organization, _: String)(_: Connection)) when(organization, adminEmailToAdd, *) returns(Future.successful(-\/(RepositoryError.NoResults(s"No ResultSet was returned. Could not add admin to organization"))))

        val result = orgService.addMember(organization, adminEmailToAdd)

        Await.result(result, Duration.Inf) should be (-\/(RepositoryError.NoResults(s"No ResultSet was returned. Could not add admin to organization")))
      }
    }
  }

  "orgService.deleteAdmin" should {
    inSequence {
      val organization = TestValues.testOrganizationA
      val adminEmailToDelete = TestValues.testUserA.email
      "return organization without deleted member if organization exists" in {
        val updatedOrganization = organization.copy(admins = IndexedSeq())
        (organizationRepository.deleteAdmin(_: Organization, _: String)(_: Connection)) when(organization, adminEmailToDelete, *) returns(Future.successful(\/-(updatedOrganization)))

        val result = orgService.deleteAdmin(organization, adminEmailToDelete)
        val \/-(fOrganization) = Await.result(result, Duration.Inf)

        fOrganization.admins.contains(adminEmailToDelete) should be (false)
        fOrganization.id should be (updatedOrganization.id)
        fOrganization.admins should be (updatedOrganization.admins)
        fOrganization.members should be (updatedOrganization.members)
      }

      "return RepositoryError if organisation doesn't exist or list of members is empty" in {
        (organizationRepository.deleteAdmin(_: Organization, _: String)(_: Connection)) when(organization, adminEmailToDelete, *) returns(Future.successful(-\/(RepositoryError.NoResults(s"No ResultSet was returned. Could not remove member from organization"))))

        val result = orgService.deleteAdmin(organization, adminEmailToDelete)

        Await.result(result, Duration.Inf) should be (-\/(RepositoryError.NoResults(s"No ResultSet was returned. Could not remove member from organization")))
      }
    }
  }

  "orgService.update" should {
    val title = "limoilou grande allee"
    val version = 2L
    val organization = TestValues.testOrganizationA
    inSequence {
      "return updated organization if exists" in {
        val updatedOrganization = organization.copy(
          version = version,
          title = title)
        (organizationRepository.find(_: UUID)(_: Connection)) when(organization.id, *) returns(Future.successful(\/-(organization)))
        (organizationRepository.update(_: Organization)(_: Connection)) when(organization, *) returns(Future.successful(\/-(updatedOrganization)))

        val result = orgService.update(organization.id, organization.version, Option(title))

        Await.result(result, Duration.Inf) should be (\/-(updatedOrganization))
      }

      "return RepositoryError if organisation doesn't exist" in {
        (organizationRepository.find(_: UUID)(_: Connection)) when(organization.id, *) returns(Future.successful(-\/(RepositoryError.NoResults(""))))
        (organizationRepository.update(_: Organization)(_: Connection)) when(organization, *) returns(Future.successful(-\/(RepositoryError.NoResults(""))))

        val result = orgService.update(organization.id, version, Option(title))

        Await.result(result, Duration.Inf) should be (-\/(RepositoryError.NoResults("")))
      }
    }
  }

  "orgService.delete" should {
    inSequence {
      "return deleted organization if exists" in {
        val organization = TestValues.testOrganizationA
        (organizationRepository.find(_: UUID)(_: Connection)) when(organization.id, *) returns(Future.successful(\/-(organization)))
        (organizationRepository.delete(_: Organization)(_: Connection)) when(organization, *) returns(Future.successful(\/-(organization)))

        val fOrganization = orgService.delete(organization.id, organization.version)

        Await.result(fOrganization, Duration.Inf) should be (\/-(organization))
      }

      "return RepositoryError if organization doesn't exist" in {
        val organization = TestValues.testOrganizationA
        (organizationRepository.find(_: UUID)(_: Connection)) when(organization.id, *) returns(Future.successful(-\/(RepositoryError.NoResults(""))))
        (organizationRepository.delete(_: Organization)(_: Connection)) when(organization, *) returns(Future.successful(-\/(RepositoryError.NoResults(""))))

        val fOrganization = orgService.delete(organization.id, organization.version)

        Await.result(fOrganization, Duration.Inf) should be (-\/(RepositoryError.NoResults("")))
      }
    }
  }
}
