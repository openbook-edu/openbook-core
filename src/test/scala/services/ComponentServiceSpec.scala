import ca.shiftfocus.krispii.core.error.{ RepositoryError, ServiceError }
import ca.shiftfocus.krispii.core.lib.ScalaCachePool
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.krispii.core.services._
import ca.shiftfocus.krispii.core.services.datasource.DB
import com.github.mauricio.async.db.Connection
import org.scalatest.Matchers
import java.util.UUID
import org.scalatest._
import Matchers._
import scala.collection._
import scala.collection.immutable.TreeMap
import scala.concurrent.{ Future, Await }
import scala.concurrent.duration.Duration
import scala.language.postfixOps
import scalaz.{ -\/, \/- }

class ComponentServiceSpec
    extends TestEnvironment(writeToDb = false) {
  val db = stub[DB]
  val mockConnection = stub[Connection]
  val authService = stub[AuthService]
  val projectService = stub[ProjectService]
  val schoolService = stub[SchoolService]
  val componentRepository = stub[ComponentRepository]

  val componentService = new ComponentServiceDefault(db, cache, authService, projectService, schoolService, componentRepository) {
    override implicit def conn: Connection = mockConnection

    override def transactional[A](f: Connection => Future[A]): Future[A] = {
      f(mockConnection)
    }
  }

  "ComponentService.listByProject" should {
    inSequence {
      "list components by project and user, thus listing \"enabled\" components (forceAll = TRUE)" in {
        val testProject = TestValues.testProjectA

        val testPartList = TreeMap[Int, Part](
          0 -> TestValues.testPartA,
          1 -> TestValues.testPartB,
          2 -> TestValues.testPartG
        )

        val testComponentList = Map[UUID, Vector[Component]](
          testPartList(0).id -> Vector(
            TestValues.testTextComponentA,
            TestValues.testAudioComponentC
          ),
          testPartList(1).id -> Vector(
            TestValues.testTextComponentA,
            TestValues.testVideoComponentB
          ),
          testPartList(2).id -> Vector()
        )

        (projectService.find(_: UUID)) when (testProject.id) returns (Future.successful(\/-(testProject)))
        (componentRepository.list(_: Project)(_: Connection, _: ScalaCachePool)) when (testProject, *, *) returns (Future.successful(\/-(testComponentList.flatMap(_._2)(breakOut).distinct)))

        val result = componentService.listByProject(testProject.id, true)
        val eitherComponents = Await.result(result, Duration.Inf)
        val \/-(components) = eitherComponents

        components.size should be(testComponentList.flatMap(_._2)(breakOut).distinct.size)

        var key: Int = 0
        testComponentList.flatMap(_._2)(breakOut).distinct.foreach { component =>
          components(key).id should be(component.id)
          components(key).version should be(component.version)
          components(key).ownerId should be(component.ownerId)
          components(key).title should be(component.title)
          components(key).questions should be(component.questions)
          components(key).thingsToThinkAbout should be(component.thingsToThinkAbout)
          components(key).createdAt.toString should be(component.createdAt.toString)
          components(key).updatedAt.toString should be(component.updatedAt.toString)
          key += 1
        }
      }
      "list components by project and user, thus listing \"enabled\" components (forceAll = FALSE)" in {
        val testProject = TestValues.testProjectA

        val testPartList = TreeMap[Int, Part](
          0 -> TestValues.testPartA,
          1 -> TestValues.testPartB,
          2 -> TestValues.testPartG
        )

        // Return all Components for all Parts, even for disabled Parts
        val testAllComponentList = Map[UUID, Vector[Component]](
          testPartList(0).id -> Vector(
            TestValues.testTextComponentA,
            TestValues.testAudioComponentC
          ),
          testPartList(1).id -> Vector(
            TestValues.testTextComponentA,
            TestValues.testVideoComponentB
          ),
          testPartList(2).id -> Vector()
        )

        // As PartB is disabled and PartG doesn't have components, as result we should have components only for PartA
        val testResultComponentList = TreeMap[Int, Component](
          0 -> testAllComponentList(testPartList(0).id)(0),
          1 -> testAllComponentList(testPartList(0).id)(1)
        )

        (projectService.find(_: UUID)) when (testProject.id) returns (Future.successful(\/-(testProject)))

        testPartList.foreach {
          case (key, part: Part) => {
            (componentRepository.list(_: Part)(_: Connection, _: ScalaCachePool)) when (part, *, *) returns (Future.successful(\/-(testAllComponentList(part.id))))
          }
        }

        val result = componentService.listByProject(testProject.id, false)
        val eitherComponents = Await.result(result, Duration.Inf)
        val \/-(components) = eitherComponents

        components.size should be(testResultComponentList.size)

        testResultComponentList.foreach {
          case (key, component: Component) => {
            components(key).id should be(component.id)
            components(key).version should be(component.version)
            components(key).ownerId should be(component.ownerId)
            components(key).title should be(component.title)
            components(key).questions should be(component.questions)
            components(key).thingsToThinkAbout should be(component.thingsToThinkAbout)
            components(key).createdAt.toString should be(component.createdAt.toString)
            components(key).updatedAt.toString should be(component.updatedAt.toString)
          }
        }
      }
    }
  }

  "ComponentService.updateAudio" should {
    inSequence {
      "return ServiceError.OfflineLockFail if versions don't match" in {
        val testComponent = TestValues.testAudioComponentC

        (componentRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testComponent.id, *, *) returns (Future.successful(\/-(testComponent)))

        val result = componentService.updateAudio(
          testComponent.id,
          testComponent.version + 1,
          testComponent.ownerId,
          Some(testComponent.title),
          Some(testComponent.questions),
          Some(testComponent.thingsToThinkAbout),
          Some(testComponent.soundcloudId),
          None
        )
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.OfflineLockFail))
      }
      "return ServiceError.BadInput if a wrong type of component was found by ID" in {
        val testComponent = TestValues.testVideoComponentB

        (componentRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testComponent.id, *, *) returns (Future.successful(\/-(testComponent)))

        val result = componentService.updateAudio(
          testComponent.id,
          testComponent.version,
          testComponent.ownerId,
          Some(testComponent.title),
          Some(testComponent.questions),
          Some(testComponent.thingsToThinkAbout),
          None,
          None
        )
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.BadInput("Component type is not audio")))
      }
    }
  }

  "ComponentService.updateText" should {
    inSequence {
      "return ServiceError.OfflineLockFail if versions don't match" in {
        val testComponent = TestValues.testTextComponentA

        (componentRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testComponent.id, *, *) returns (Future.successful(\/-(testComponent)))

        val result = componentService.updateText(
          testComponent.id,
          testComponent.version + 1,
          testComponent.ownerId,
          Some(testComponent.title),
          Some(testComponent.questions),
          Some(testComponent.thingsToThinkAbout),
          Some(testComponent.content),
          None
        )
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.OfflineLockFail))
      }
      "return ServiceError.BadInput if a wrong type of component was found by ID" in {
        val testComponent = TestValues.testVideoComponentB

        (componentRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testComponent.id, *, *) returns (Future.successful(\/-(testComponent)))

        val result = componentService.updateText(
          testComponent.id,
          testComponent.version,
          testComponent.ownerId,
          Some(testComponent.title),
          Some(testComponent.questions),
          Some(testComponent.thingsToThinkAbout),
          None,
          None
        )
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.BadInput("Component type is not text")))
      }
    }
  }

  "ComponentService.updateVideo" should {
    inSequence {
      "return ServiceError.OfflineLockFail if versions don't match" in {
        val testComponent = TestValues.testVideoComponentB

        (componentRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testComponent.id, *, *) returns (Future.successful(\/-(testComponent)))

        val result = componentService.updateVideo(
          testComponent.id,
          testComponent.version + 1,
          testComponent.ownerId,
          Some(testComponent.title),
          Some(testComponent.questions),
          Some(testComponent.thingsToThinkAbout),
          Some(testComponent.vimeoId),
          Some(testComponent.width),
          Some(testComponent.height),
          None
        )
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.OfflineLockFail))
      }
      "return ServiceError.BadInput if a wrong type of component was found by ID" in {
        val testComponent = TestValues.testTextComponentA

        (componentRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testComponent.id, *, *) returns (Future.successful(\/-(testComponent)))

        val result = componentService.updateVideo(
          testComponent.id,
          testComponent.version,
          testComponent.ownerId,
          Some(testComponent.title),
          Some(testComponent.questions),
          Some(testComponent.thingsToThinkAbout),
          None,
          None,
          None,
          None
        )
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.BadInput("Component type is not video")))
      }
    }
  }

  "ComponentService.delete" should {
    inSequence {
      "return ServiceError.OfflineLockFail if versions don't match" in {
        val testComponent = TestValues.testVideoComponentB

        (componentRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testComponent.id, *, *) returns (Future.successful(\/-(testComponent)))

        val result = componentService.delete(
          testComponent.id,
          testComponent.version + 1
        )
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.OfflineLockFail))
      }
    }
  }

  "ComponentService.userCanAccess" should {
    inSequence {
      "give access if user is administrator" in {
        val testUser = TestValues.testUserB.copy(
          roles = IndexedSeq(
            Role(name = "developer"),
            Role(name = "administrator"),
            Role(name = "some role")
          )
        )

        val testComponent = TestValues.testTextComponentA

        val result = componentService.userCanAccess(
          testComponent,
          testUser
        )

        testComponent.ownerId should not be (testUser.id)
        Await.result(result, Duration.Inf) should be(\/-(true))
      }
      "give access if user is owner" in {
        val testUser = TestValues.testUserB.copy(
          roles = IndexedSeq(
            Role(name = "developer"),
            Role(name = "bla role"),
            Role(name = "some role")
          )
        )

        val testComponent = TestValues.testTextComponentA.copy(
          ownerId = testUser.id
        )

        val result = componentService.userCanAccess(testComponent, testUser)

        Await.result(result, Duration.Inf) should be(\/-(true))
      }
      "give access if it's in one of their projects (user - has role teacher, user is a TEACHER of the course)" in {
        val testUser = TestValues.testUserA.copy(
          roles = IndexedSeq(
            Role(name = "developer"),
            Role(name = "teacher"),
            Role(name = "some role")
          )
        )

        val testComponent = TestValues.testTextComponentG

        val testCourseList = IndexedSeq(
          TestValues.testCourseA
        )

        val testProjectList = IndexedSeq(
          TestValues.testProjectA,
          TestValues.testProjectE
        )

        val testPartList = TreeMap[Int, Part](
          0 -> TestValues.testPartA,
          1 -> TestValues.testPartB,
          2 -> TestValues.testPartG
        )

        // Return all Components for all Parts, even for disabled Parts
        val testAllComponentList = Map[UUID, Vector[Component]](
          testPartList(0).id -> Vector(
            TestValues.testTextComponentA,
            TestValues.testAudioComponentC,
            // Add component to the list
            TestValues.testTextComponentG
          ),
          testPartList(1).id -> Vector(
            TestValues.testTextComponentA,
            TestValues.testVideoComponentB
          ),
          testPartList(2).id -> Vector()
        )

        (schoolService.listCoursesByTeacher(_: UUID, _: Boolean)) when (testUser.id, false) returns (Future.successful(\/-(testCourseList)))

        testCourseList.foreach { course =>
          (projectService.list(_: UUID)) when (course.id) returns (Future.successful(\/-(testProjectList)))
        }

        testProjectList.foreach { project =>
          (projectService.find(_: UUID)) when (project.id) returns (Future.successful(\/-(project)))
        }

        testPartList.foreach {
          case (key, part: Part) => {
            (componentRepository.list(_: Part)(_: Connection, _: ScalaCachePool)) when (part, *, *) returns (Future.successful(\/-(testAllComponentList(part.id))))
          }
        }

        val result = componentService.userCanAccess(testComponent, testUser)

        testComponent.ownerId should not be (testUser.id)
        Await.result(result, Duration.Inf) should be(\/-(true))
      }
      "give access if it's in one of their projects (user - has role teacher, user is a STUDENT of the course)" in {
        val testUser = TestValues.testUserB.copy(
          roles = IndexedSeq(
            Role(name = "developer"),
            Role(name = "teacher"),
            Role(name = "some role")
          )
        )

        val testComponent = TestValues.testTextComponentG

        val testCourseList = IndexedSeq(
          TestValues.testCourseA
        )

        val testProjectList = IndexedSeq(
          TestValues.testProjectA,
          TestValues.testProjectE
        )

        val testPartList = TreeMap[Int, Part](
          0 -> TestValues.testPartA,
          1 -> TestValues.testPartB,
          2 -> TestValues.testPartG
        )

        // Return all Components for all Parts, even for disabled Parts
        val testAllComponentList = Map[UUID, Vector[Component]](
          testPartList(0).id -> Vector(
            TestValues.testTextComponentA,
            TestValues.testAudioComponentC,
            // Add component to the list
            TestValues.testTextComponentG
          ),
          testPartList(1).id -> Vector(
            TestValues.testTextComponentA,
            TestValues.testVideoComponentB
          ),
          testPartList(2).id -> Vector()
        )

        (schoolService.listCoursesByTeacher(_: UUID, _: Boolean)) when (testUser.id, false) returns (Future.successful(\/-(IndexedSeq.empty[Course])))
        (schoolService.listCoursesByUser(_: UUID, _: Boolean)) when (testUser.id, false) returns (Future.successful(\/-(testCourseList)))

        testCourseList.foreach { course =>
          (projectService.list(_: UUID)) when (course.id) returns (Future.successful(\/-(testProjectList)))
        }

        testProjectList.foreach { project =>
          (projectService.find(_: UUID)) when (project.id) returns (Future.successful(\/-(project)))
        }

        testPartList.foreach {
          case (key, part: Part) => {
            (componentRepository.list(_: Part)(_: Connection, _: ScalaCachePool)) when (part, *, *) returns (Future.successful(\/-(testAllComponentList(part.id))))
          }
        }

        val result = componentService.userCanAccess(testComponent, testUser)

        testComponent.ownerId should not be (testUser.id)
        Await.result(result, Duration.Inf) should be(\/-(true))
      }
      "give access if it's in one of their projects (user - student)" in {
        val testUser = TestValues.testUserE.copy(
          roles = IndexedSeq(
            Role(name = "developer"),
            Role(name = "student"),
            Role(name = "some role")
          )
        )

        val testComponent = TestValues.testTextComponentA

        val testCourseList = IndexedSeq(
          TestValues.testCourseA
        )

        val testProjectList = IndexedSeq(
          TestValues.testProjectA,
          TestValues.testProjectE
        )

        val testPartList = TreeMap[Int, Part](
          0 -> TestValues.testPartA,
          1 -> TestValues.testPartB,
          2 -> TestValues.testPartG
        )

        // Return all Components for all Parts, even for disabled Parts
        val testAllComponentList = Map[UUID, Vector[Component]](
          testPartList(0).id -> Vector(
            TestValues.testTextComponentA,
            TestValues.testAudioComponentC
          ),
          testPartList(1).id -> Vector(
            TestValues.testTextComponentA,
            TestValues.testVideoComponentB
          ),
          testPartList(2).id -> Vector()
        )

        (schoolService.listCoursesByUser(_: UUID, _: Boolean)) when (testUser.id, false) returns (Future.successful(\/-(testCourseList)))

        testCourseList.foreach { course =>
          (projectService.list(_: UUID)) when (course.id) returns (Future.successful(\/-(testProjectList)))
        }

        testProjectList.foreach { project =>
          (projectService.find(_: UUID)) when (project.id) returns (Future.successful(\/-(project)))
        }

        testPartList.foreach {
          case (key, part: Part) => {
            (componentRepository.list(_: Part)(_: Connection, _: ScalaCachePool)) when (part, *, *) returns (Future.successful(\/-(testAllComponentList(part.id))))
          }
        }

        val result = componentService.userCanAccess(testComponent, testUser)

        testComponent.ownerId should not be (testUser.id)
        Await.result(result, Duration.Inf) should be(\/-(true))
      }
      "restrict access if user is not administrator, owner, teacher or student" in {
        val testUser = TestValues.testUserE.copy(
          roles = IndexedSeq(
            Role(name = "developer"),
            Role(name = "bla bla role"),
            Role(name = "some role")
          )
        )

        val testComponent = TestValues.testTextComponentG

        val result = componentService.userCanAccess(testComponent, testUser)

        testComponent.ownerId should not be (testUser.id)
        Await.result(result, Duration.Inf) should be(\/-(false))
      }
      "restrict access if user - has role teacher, user is a TEACHER of the courses that do not contain the component" in {
        val testUser = TestValues.testUserA.copy(
          roles = IndexedSeq(
            Role(name = "developer"),
            Role(name = "teacher"),
            Role(name = "some role")
          )
        )

        val testComponent = TestValues.testTextComponentG

        val testCourseList = IndexedSeq(
          TestValues.testCourseA
        )

        val testProjectList = IndexedSeq(
          TestValues.testProjectA,
          TestValues.testProjectE
        )

        val testPartList = TreeMap[Int, Part](
          0 -> TestValues.testPartA,
          1 -> TestValues.testPartB,
          2 -> TestValues.testPartG
        )

        // Return all Components for all Parts, even for disabled Parts
        val testAllComponentList = Map[UUID, Vector[Component]](
          testPartList(0).id -> Vector(
            TestValues.testTextComponentA,
            TestValues.testAudioComponentC
          ),
          testPartList(1).id -> Vector(
            TestValues.testTextComponentA,
            TestValues.testVideoComponentB
          ),
          testPartList(2).id -> Vector()
        )

        (schoolService.listCoursesByTeacher(_: UUID, _: Boolean)) when (testUser.id, false) returns (Future.successful(\/-(testCourseList)))
        (schoolService.listCoursesByUser(_: UUID, _: Boolean)) when (testUser.id, false) returns (Future.successful(\/-(testCourseList)))

        testCourseList.foreach { course =>
          (projectService.list(_: UUID)) when (course.id) returns (Future.successful(\/-(testProjectList)))
        }

        testProjectList.foreach { project =>
          (projectService.find(_: UUID)) when (project.id) returns (Future.successful(\/-(project)))
        }

        testPartList.foreach {
          case (key, part: Part) => {
            (componentRepository.list(_: Part)(_: Connection, _: ScalaCachePool)) when (part, *, *) returns (Future.successful(\/-(testAllComponentList(part.id))))
          }
        }

        val result = componentService.userCanAccess(testComponent, testUser)

        testComponent.ownerId should not be (testUser.id)
        Await.result(result, Duration.Inf) should be(\/-(false))
      }
      "restrict access if user - has role teacher, user is a STUDENT of the course that do not contain the component" in {
        val testUser = TestValues.testUserB.copy(
          roles = IndexedSeq(
            Role(name = "developer"),
            Role(name = "teacher"),
            Role(name = "some role")
          )
        )

        val testComponent = TestValues.testTextComponentG

        val testCourseList = IndexedSeq(
          TestValues.testCourseA
        )

        val testProjectList = IndexedSeq(
          TestValues.testProjectA,
          TestValues.testProjectE
        )

        val testPartList = TreeMap[Int, Part](
          0 -> TestValues.testPartA,
          1 -> TestValues.testPartB,
          2 -> TestValues.testPartG
        )

        // Return all Components for all Parts, even for disabled Parts
        val testAllComponentList = Map[UUID, Vector[Component]](
          testPartList(0).id -> Vector(
            TestValues.testTextComponentA,
            TestValues.testAudioComponentC
          ),
          testPartList(1).id -> Vector(
            TestValues.testTextComponentA,
            TestValues.testVideoComponentB
          ),
          testPartList(2).id -> Vector()
        )

        (schoolService.listCoursesByTeacher(_: UUID, _: Boolean)) when (testUser.id, false) returns (Future.successful(\/-(IndexedSeq.empty[Course])))
        (schoolService.listCoursesByUser(_: UUID, _: Boolean)) when (testUser.id, false) returns (Future.successful(\/-(testCourseList)))

        testCourseList.foreach { course =>
          (projectService.list(_: UUID)) when (course.id) returns (Future.successful(\/-(testProjectList)))
        }

        testProjectList.foreach { project =>
          (projectService.find(_: UUID)) when (project.id) returns (Future.successful(\/-(project)))
        }

        testPartList.foreach {
          case (key, part: Part) => {
            (componentRepository.list(_: Part)(_: Connection, _: ScalaCachePool)) when (part, *, *) returns (Future.successful(\/-(testAllComponentList(part.id))))
          }
        }

        val result = componentService.userCanAccess(testComponent, testUser)

        testComponent.ownerId should not be (testUser.id)
        Await.result(result, Duration.Inf) should be(\/-(false))
      }
    }
  }
}

