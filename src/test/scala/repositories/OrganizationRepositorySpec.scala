//import java.util.UUID
//
//import ca.shiftfocus.krispii.core.error.RepositoryError
//import ca.shiftfocus.krispii.core.models.{ Organization, Tag }
//import ca.shiftfocus.krispii.core.repositories._
//import org.joda.time.DateTime
//import org.scalatest.Matchers._
//
//import scala.collection.immutable.TreeMap
//import scala.concurrent.Await
//import scala.concurrent.duration.Duration
//import scalaz._
//
//class OrganizationRepositorySpec
//    extends TestEnvironment {
//  val organizationRepository = new OrganizationRepositoryPostgres()
//
//  "OrganizationRepository.list" should {
//    inSequence {
//      "list organizations" in {
//        val organizationList = TreeMap[Int, Organization](
//          0 -> TestValues.testOrganizationA,
//          1 -> TestValues.testOrganizationC
//        )
//
//        val result = organizationRepository.list
//        val eitherOrganizations = Await.result(result, Duration.Inf)
//        val \/-(organizations) = eitherOrganizations
//
//        organizations.size should be(organizationList.size)
//        organizationList.foreach {
//          case (key, organization) => {
//            organizations(key).id should be(organization.id)
//            organizations(key).title should be(organization.title)
//            organizations(key).members should be(organization.members)
//          }
//        }
//      }
//      "list organizations by admin email" in {
//        val organizationList = TreeMap[Int, Organization](
//          0 -> TestValues.testOrganizationA,
//          1 -> TestValues.testOrganizationC
//        )
//
//        val result = organizationRepository.listByAdmin(organizationList(0).admins.head)
//        val eitherOrganizations = Await.result(result, Duration.Inf)
//        val \/-(organizations) = eitherOrganizations
//
//        organizations.size should be(organizationList.size)
//        organizationList.foreach {
//          case (key, organization) => {
//            organizations(key).id should be(organization.id)
//            organizations(key).title should be(organization.title)
//            organizations(key).members should be(organization.members)
//          }
//        }
//      }
//    }
//  }
//
//  "OrganizationRepository.listByTags" should {
//    inSequence {
//      "list organizations distinct" in {
//        val organizationList = TreeMap[Int, Organization](
//          0 -> TestValues.testOrganizationA
//        )
//
//        val testTags: IndexedSeq[Tag] = IndexedSeq(
//          TestValues.testTagA,
//          TestValues.testTagB
//        )
//
//        val result = organizationRepository.listByTags(testTags.map(tag => (tag.name, tag.lang)))
//        val eitherOrganizations = Await.result(result, Duration.Inf)
//        val \/-(organizations) = eitherOrganizations
//
//        organizations.size should be(organizationList.size)
//        organizationList.foreach {
//          case (key, organization) => {
//            organizations(key).id should be(organization.id)
//            organizations(key).title should be(organization.title)
//            organizations(key).members should be(organization.members)
//          }
//        }
//      }
//      "list organizations without distinct" in {
//        val organizationList = TreeMap[Int, Organization](
//          0 -> TestValues.testOrganizationA,
//          1 -> TestValues.testOrganizationC
//        )
//
//        val testTags: IndexedSeq[Tag] = IndexedSeq(
//          TestValues.testTagA,
//          TestValues.testTagB
//        )
//
//        val result = organizationRepository.listByTags(testTags.map(tag => (tag.name, tag.lang)), false)
//        val eitherOrganizations = Await.result(result, Duration.Inf)
//        val \/-(organizations) = eitherOrganizations
//
//        organizations.size should be(organizationList.size)
//        organizationList.foreach {
//          case (key, organization) => {
//            organizations(key).id should be(organization.id)
//            organizations(key).title should be(organization.title)
//            organizations(key).members should be(organization.members)
//          }
//        }
//      }
//      "empty organizations distinct if no tags" in {
//        val testTags: IndexedSeq[Tag] = IndexedSeq()
//
//        val result = organizationRepository.listByTags(testTags.map(tag => (tag.name, tag.lang)))
//        val eitherOrganizations = Await.result(result, Duration.Inf)
//        val \/-(organizations) = eitherOrganizations
//
//        organizations.size should be(0)
//      }
//      "empty organizations without distinct if no tags" in {
//        val testTags: IndexedSeq[Tag] = IndexedSeq()
//
//        val result = organizationRepository.listByTags(testTags.map(tag => (tag.name, tag.lang)), false)
//        val eitherOrganizations = Await.result(result, Duration.Inf)
//        val \/-(organizations) = eitherOrganizations
//
//        organizations.size should be(0)
//      }
//    }
//  }
//
//  "OrganizationRepository.find" should {
//    inSequence {
//      "find organization" in {
//        val testOrganization = TestValues.testOrganizationA
//        val result = organizationRepository.find(testOrganization.id)
//        val eitherOrganization = Await.result(result, Duration.Inf)
//        val \/-(organization) = eitherOrganization
//
//        organization.id should be(testOrganization.id)
//        organization.title should be(testOrganization.title)
//        organization.members should be(testOrganization.members)
//      }
//    }
//  }
//
//  "OrganizationRepository.addMember" should {
//    inSequence {
//      "add member email to organization" in {
//        val testOrganization = TestValues.testOrganizationA
//        val newMemberEmail = "new_member_email@example.com"
//        val result = organizationRepository.addMember(testOrganization, newMemberEmail)
//        val eitherOrganization = Await.result(result, Duration.Inf)
//        val \/-(organization) = eitherOrganization
//
//        organization.id should be(testOrganization.id)
//        organization.title should be(testOrganization.title)
//        organization.members should be(testOrganization.members ++ IndexedSeq(newMemberEmail))
//      }
//    }
//  }
//
//  "OrganizationRepository.deleteMember" should {
//    inSequence {
//      "remove member email from organization" in {
//        val testOrganization = TestValues.testOrganizationA
//
//        val result = organizationRepository.deleteMember(testOrganization, testOrganization.members.head)
//        val eitherOrganization = Await.result(result, Duration.Inf)
//        val \/-(organization) = eitherOrganization
//
//        organization.id should be(testOrganization.id)
//        organization.title should be(testOrganization.title)
//        organization.members should be(testOrganization.members.filter(_ != testOrganization.members.head))
//      }
//      "throw error if member doesn't exist" in {
//        val testOrganization = TestValues.testOrganizationA
//
//        val result = organizationRepository.deleteMember(testOrganization, "bad_memeber@example.com")
//
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not remove member from organization")))
//      }
//    }
//  }
//
//  "OrganizationRepository.insert" should {
//    inSequence {
//      "insert organization" in {
//        val testOrganization = TestValues.testOrganizationB
//
//        val result = organizationRepository.insert(testOrganization)
//        val eitherOrganization = Await.result(result, Duration.Inf)
//        val \/-(organization) = eitherOrganization
//
//        organization.id should be(testOrganization.id)
//        organization.title should be(testOrganization.title)
//        organization.members should be(testOrganization.members)
//      }
//    }
//  }
//
//  "OrganizationRepository.update" should {
//    inSequence {
//      "update organization" in {
//        val testOrganization = TestValues.testOrganizationA.copy(
//          title = "new title"
//        )
//
//        val result = organizationRepository.update(testOrganization)
//        val eitherOrganization = Await.result(result, Duration.Inf)
//        val \/-(organization) = eitherOrganization
//
//        organization.id should be(testOrganization.id)
//        organization.title should be(testOrganization.title)
//        organization.members should be(testOrganization.members)
//      }
//    }
//  }
//
//  "OrganizationRepository.delete" should {
//    inSequence {
//      "delete organization" in {
//        val testOrganization = TestValues.testOrganizationA
//        val result = organizationRepository.delete(testOrganization)
//        val eitherOrganization = Await.result(result, Duration.Inf)
//        val \/-(organization) = eitherOrganization
//
//        organization.id should be(testOrganization.id)
//        organization.title should be(testOrganization.title)
//        organization.members should be(testOrganization.members)
//      }
//    }
//  }
//}