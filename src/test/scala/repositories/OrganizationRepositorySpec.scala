import java.util.UUID

import ca.shiftfocus.krispii.core.models.Organization
import ca.shiftfocus.krispii.core.repositories._
import org.joda.time.DateTime
import org.scalatest.Matchers._

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scalaz._

class OrganizationRepositorySpec
    extends TestEnvironment {
  val organizationRepository = new OrganizationRepositoryPostgres()

  "OrganizationRepository.list" should {
    inSequence {
      "list organizations" in {
        val result = organizationRepository.list
        val eitherOrganizations = Await.result(result, Duration.Inf)
        val \/-(organizations) = eitherOrganizations

        1 should be(1)
      }
    }
  }

  "OrganizationRepository.find" should {
    inSequence {
      "find organization" in {
        val result = organizationRepository.find(UUID.randomUUID())
        val eitherOrganization = Await.result(result, Duration.Inf)
        val \/-(organization) = eitherOrganization

        1 should be(1)
      }
    }
  }

  "OrganizationRepository.insert" should {
    inSequence {
      "insert organization" in {
        val result = organizationRepository.insert(
          Organization(
            title = "TestOrganizationA"
          )
        )
        val eitherOrganization = Await.result(result, Duration.Inf)
        val \/-(organization) = eitherOrganization

        1 should be(1)
      }
    }
  }

  "OrganizationRepository.update" should {
    inSequence {
      "update organization" in {
        val result = organizationRepository.update(
          Organization(
            title = "TestOrganizationA"
          )
        )
        val eitherOrganization = Await.result(result, Duration.Inf)
        val \/-(organization) = eitherOrganization

        1 should be(1)
      }
    }
  }

  "OrganizationRepository.delete" should {
    inSequence {
      "delete organization" in {
        val result = organizationRepository.delete(
          Organization(
            title = "TestOrganizationA"
          )
        )
        val eitherOrganization = Await.result(result, Duration.Inf)
        val \/-(organization) = eitherOrganization

        1 should be(1)
      }
    }
  }
}