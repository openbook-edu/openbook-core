import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.lib.ScalaCachePool
import ca.shiftfocus.krispii.core.models.Part
import ca.shiftfocus.krispii.core.repositories.{TaskRepository, PartRepositoryPostgres}
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.Connection
import org.scalatest._
import Matchers._
import scala.collection.immutable.TreeMap
import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration
import scalacache.ScalaCache
import scalaz._


class PartRepositorySpec
  extends TestEnvironment
{
  val taskRepository = stub[TaskRepository]
  val partRepository = new PartRepositoryPostgres(taskRepository)

  // TODO - check parts_components primary_key
  "PartRepository.list" should {
    inSequence {
      "list all parts" in {
        val testPartList = TreeMap[Int, Part](
          0 -> TestValues.testPartA,
          1 -> TestValues.testPartB,
          2 -> TestValues.testPartC,
          3 -> TestValues.testPartE,
          4 -> TestValues.testPartF,
          5 -> TestValues.testPartG,
          6 -> TestValues.testPartH
        )

        val testTaskList = TreeMap(
          testPartList(0).id.toString -> Vector(
            TestValues.testLongAnswerTaskA,
            TestValues.testShortAnswerTaskB,
            TestValues.testMultipleChoiceTaskC
          ),
          testPartList(1).id.toString -> Vector(
            TestValues.testOrderingTaskD,
            TestValues.testMatchingTaskE
          ),
          testPartList(2).id.toString -> Vector(),
          testPartList(3).id.toString -> Vector(
            TestValues.testMatchingTaskK
          ),
          testPartList(4).id.toString -> Vector(),
          testPartList(5).id.toString -> Vector(),
          testPartList(6).id.toString -> Vector()
        )

        // Put here tasks = Vector(), because after db query Project object is created without tasks.
        testPartList.foreach {
          case (key, part: Part) => {
            (taskRepository.list(_: Part)(_: Connection, _: ScalaCachePool)) when(part.copy(tasks = Vector()), *, *) returns(Future.successful(\/-(testTaskList(part.id.toString))))
          }
        }

        val result = partRepository.list
        val eitherParts = Await.result(result, Duration.Inf)
        val \/-(parts) = eitherParts

        parts.size should be(testPartList.size)

        testPartList.foreach {
          case (key, part: Part) => {
            parts(key).id should be(part.id)
            parts(key).version should be(part.version)
            parts(key).projectId should be(part.projectId)
            parts(key).name should be(part.name)
            parts(key).enabled should be(part.enabled)
            parts(key).position should be(part.position)
            parts(key).tasks should be(part.tasks)
            parts(key).createdAt.toString should be(part.createdAt.toString)
            parts(key).updatedAt.toString should be(part.updatedAt.toString)
          }
        }
      }
      "list all Parts belonging to a given Project" in {
        (cache.getCached(_: String)) when(*) returns(Future.successful(-\/(RepositoryError.NoResults)))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when(*, *, *) returns(Future.successful(\/-(())))

        val testProject = TestValues.testProjectA

        val testPartList = TreeMap[Int, Part](
          0 -> TestValues.testPartA,
          1 -> TestValues.testPartB,
          2 -> TestValues.testPartG
        )

        val testTaskList = TreeMap(
          testPartList(0).id.toString -> Vector(
            TestValues.testLongAnswerTaskA,
            TestValues.testShortAnswerTaskB,
            TestValues.testMultipleChoiceTaskC
          ),
          testPartList(1).id.toString -> Vector(
            TestValues.testOrderingTaskD,
            TestValues.testMatchingTaskE
          ),
          testPartList(2).id.toString -> Vector()
        )

        // Put here tasks = Vector(), because after db query Project object is created without tasks.
        testPartList.foreach {
          case (key, part: Part) => {
            (taskRepository.list(_: Part)(_: Connection, _: ScalaCachePool)) when(part.copy(tasks = Vector()), *, *) returns(Future.successful(\/-(testTaskList(part.id.toString))))
          }
        }

        val result = partRepository.list(testProject)
        val eitherParts = Await.result(result, Duration.Inf)
        val \/-(parts) = eitherParts

        parts.size should be(testPartList.size)

        testPartList.foreach {
          case (key, part: Part) => {
            parts(key).id should be(part.id)
            parts(key).version should be(part.version)
            parts(key).projectId should be(part.projectId)
            parts(key).name should be(part.name)
            parts(key).enabled should be(part.enabled)
            parts(key).position should be(part.position)
            parts(key).tasks should be(part.tasks)
            parts(key).createdAt.toString should be(part.createdAt.toString)
            parts(key).updatedAt.toString should be(part.updatedAt.toString)
          }
        }
      }
      "return empty Vector() if project doesn't exist" in {
        (cache.getCached(_: String)) when(*) returns(Future.successful(-\/(RepositoryError.NoResults)))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when(*, *, *) returns(Future.successful(\/-(())))

        val testProject = TestValues.testProjectD

        val result = partRepository.list(testProject)
        Await.result(result, Duration.Inf) should be (\/-(Vector()))
      }
      "list all Parts belonging to a given Component" in {
        val testComponent = TestValues.testTextComponentA

        val testPartList = TreeMap[Int, Part](
          0 -> TestValues.testPartA,
          1 -> TestValues.testPartB
        )

        val testTaskList = TreeMap(
          testPartList(0).id.toString -> Vector(
            TestValues.testLongAnswerTaskA,
            TestValues.testShortAnswerTaskB,
            TestValues.testMultipleChoiceTaskC
          ),
          testPartList(1).id.toString -> Vector(
            TestValues.testOrderingTaskD,
            TestValues.testMatchingTaskE
          )
        )

        // Put here tasks = Vector(), because after db query Project object is created without tasks.
        testPartList.foreach {
          case (key, part: Part) => {
            (taskRepository.list(_: Part)(_: Connection, _: ScalaCachePool)) when(part.copy(tasks = Vector()), *, *) returns(Future.successful(\/-(testTaskList(part.id.toString))))
          }
        }

        val result = partRepository.list(testComponent)
        val eitherParts = Await.result(result, Duration.Inf)
        val \/-(parts) = eitherParts

        parts.size should be(testPartList.size)

        testPartList.foreach {
          case (key, part: Part) => {
            parts(key).id should be(part.id)
            parts(key).version should be(part.version)
            parts(key).projectId should be(part.projectId)
            parts(key).name should be(part.name)
            parts(key).enabled should be(part.enabled)
            parts(key).position should be(part.position)
            parts(key).tasks should be(part.tasks)
            parts(key).createdAt.toString should be(part.createdAt.toString)
            parts(key).updatedAt.toString should be(part.updatedAt.toString)
          }
        }
      }
      "return empty Vector() if component doesn't exist" in {
        val testComponent = TestValues.testAudioComponentD

        val result = partRepository.list(testComponent)
        Await.result(result, Duration.Inf) should be (\/-(Vector()))
      }
    }
  }

  "PartRepository.find" should {
    inSequence {
      "find a single entry by ID" in {
        (cache.getCached(_: String)) when(*) returns(Future.successful(-\/(RepositoryError.NoResults)))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when(*, *, *) returns(Future.successful(\/-(())))

        val testPart = TestValues.testPartA

        val testTaskList = Vector(
          TestValues.testLongAnswerTaskA,
          TestValues.testShortAnswerTaskB,
          TestValues.testMultipleChoiceTaskC
        )

        (taskRepository.list(_: Part)(_: Connection, _: ScalaCachePool)) when(testPart.copy(tasks = Vector()), *, *) returns(Future.successful(\/-(testTaskList)))

        val result = partRepository.find(testPart.id)
        val eitherPart = Await.result(result, Duration.Inf)
        val \/-(part) = eitherPart

        part.id should be(testPart.id)
        part.version should be(testPart.version)
        part.projectId should be(testPart.projectId)
        part.name should be(testPart.name)
        part.enabled should be(testPart.enabled)
        part.position should be(testPart.position)
        part.tasks should be(testPart.tasks)
        part.createdAt.toString should be(testPart.createdAt.toString)
        part.updatedAt.toString should be(testPart.updatedAt.toString)
      }
      "reuturn RepositoryError.NoResults if part wasn't found by ID" in {
        (cache.getCached(_: String)) when(*) returns(Future.successful(-\/(RepositoryError.NoResults)))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when(*, *, *) returns(Future.successful(\/-(())))

        val id = UUID("f9aadc67-5e8b-48f3-b0a2-20a0d7d88477")

        val result = partRepository.find(id)
        Await.result(result, Duration.Inf) should be (-\/(RepositoryError.NoResults))
      }
      "find a single entry by its position within a project" in {
        (cache.getCached(_: String)) when(*) returns(Future.successful(-\/(RepositoryError.NoResults)))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when(*, *, *) returns(Future.successful(\/-(())))

        val testProject  = TestValues.testProjectA
        val testPart     = TestValues.testPartA
        val partPosition = testPart.position

        val testTaskList = Vector(
          TestValues.testLongAnswerTaskA,
          TestValues.testShortAnswerTaskB,
          TestValues.testMultipleChoiceTaskC
        )

        (taskRepository.list(_: Part)(_: Connection, _: ScalaCachePool)) when(testPart.copy(tasks = Vector()), *, *) returns(Future.successful(\/-(testTaskList)))

        val result = partRepository.find(testProject, partPosition)
        val eitherPart = Await.result(result, Duration.Inf)
        val \/-(part) = eitherPart

        part.id should be(testPart.id)
        part.version should be(testPart.version)
        part.projectId should be(testPart.projectId)
        part.name should be(testPart.name)
        part.enabled should be(testPart.enabled)
        part.position should be(testPart.position)
        part.tasks should be(testPart.tasks)
        part.createdAt.toString should be(testPart.createdAt.toString)
        part.updatedAt.toString should be(testPart.updatedAt.toString)
      }
      "return RepositoryError.NoResults if project doesn't exist" in {
        (cache.getCached(_: String)) when(*) returns(Future.successful(-\/(RepositoryError.NoResults)))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when(*, *, *) returns(Future.successful(\/-(())))

        val unexistingProject = TestValues.testProjectD
        val partPosition      = 10

        val result = partRepository.find(unexistingProject, partPosition)
        Await.result(result, Duration.Inf) should be (-\/(RepositoryError.NoResults))
      }
      "return RepositoryError.NoResults if position is wrong" in {
        (cache.getCached(_: String)) when(*) returns(Future.successful(-\/(RepositoryError.NoResults)))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when(*, *, *) returns(Future.successful(\/-(())))

        val testProject  = TestValues.testProjectA
        val testPart     = TestValues.testPartA
        val partPosition = 99

        val testTaskList = Vector(
          TestValues.testLongAnswerTaskA,
          TestValues.testShortAnswerTaskB,
          TestValues.testMultipleChoiceTaskC
        )

        (taskRepository.list(_: Part)(_: Connection, _: ScalaCachePool)) when(testPart.copy(tasks = Vector()), *, *) returns(Future.successful(\/-(testTaskList)))

        val result = partRepository.find(testProject, partPosition)
        Await.result(result, Duration.Inf) should be (-\/(RepositoryError.NoResults))
      }
    }
  }

  "PartRepository.insert" should {
    inSequence {
      "save a Part row" in {
        (cache.removeCached(_: String)) when(*) returns(Future.successful(\/-( () )))

        val testPart = TestValues.testPartD

        val result = partRepository.insert(testPart)
        val eitherPart = Await.result(result, Duration.Inf)
        val \/-(part) = eitherPart

        part.id should be(testPart.id)
        part.version should be(testPart.version)
        part.projectId should be(testPart.projectId)
        part.name should be(testPart.name)
        part.enabled should be(testPart.enabled)
        part.position should be(testPart.position)
        part.tasks should be(Vector())
      }
    }
    "return RepositoryError.ForeignKeyConflict if part contains unexisting project id" in {
      val testPart = TestValues.testPartD.copy(
        projectId = UUID("ad043c17-d552-4744-890a-6ab8a6778e4c")
      )

      val result = partRepository.insert(testPart)
      Await.result(result, Duration.Inf) should be(-\/(RepositoryError.ForeignKeyConflict("project_id", "parts_project_id_fkey")))
    }
    "return RepositoryError.PrimaryKeyConflict if part already exists" in {
      val testPart = TestValues.testPartA

      val result = partRepository.insert(testPart)
      Await.result(result, Duration.Inf) should be(-\/(RepositoryError.PrimaryKeyConflict))
    }
  }

  "PartRepository.update" should {
    inSequence {
      "update a part" in {
        (cache.removeCached(_: String)) when(*) returns(Future.successful(\/-( () )))

        val testPart = TestValues.testPartA
        val updatedPart = testPart.copy(
          projectId = TestValues.testProjectB.id,
          name = "updated test part",
          enabled = false,
          position = testPart.position + 1
        )

        val result = partRepository.update(updatedPart)
        val eitherPart = Await.result(result, Duration.Inf)
        val \/-(part) = eitherPart

        part.id should be(updatedPart.id)
        part.version should be(updatedPart.version + 1)
        part.projectId should be(updatedPart.projectId)
        part.name should be(updatedPart.name)
        part.enabled should be(updatedPart.enabled)
        part.position should be(updatedPart.position)
        part.tasks should be(updatedPart.tasks)
        part.createdAt.toString should be(updatedPart.createdAt.toString)
      }
      "return RepositoryError.NoResults when update an existing Part with wrong version" in {
        val testPart = TestValues.testPartA
        val updatedPart = testPart.copy(
          projectId = TestValues.testProjectB.id,
          name = "updated test part",
          enabled = false,
          position = testPart.position + 1,
          version = 99L
        )

        val result = partRepository.update(updatedPart)
        val eitherPart = Await.result(result, Duration.Inf)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
      }
      "return RepositoryError.NoResults when update an unexisting Part" in {
        val testPart = TestValues.testPartD
        val updatedPart = testPart.copy(
          projectId = TestValues.testProjectB.id,
          name = "updated test part",
          enabled = false,
          position = testPart.position + 1,
          version = 99L
        )

        val result = partRepository.update(updatedPart)
        val eitherPart = Await.result(result, Duration.Inf)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
      }
    }
  }

  "PartRepository.delete" should {
    inSequence {
      "delete a part if there is no dependency in the tasks.dependency_id" in {
        (cache.removeCached(_: String)) when(*) returns(Future.successful(\/-( () )))

        val testPart = TestValues.testPartB

        val result = partRepository.delete(testPart)
        val eitherPart = Await.result(result, Duration.Inf)
        val \/-(part) = eitherPart

        part.id should be(testPart.id)
        part.version should be(testPart.version)
        part.projectId should be(testPart.projectId)
        part.name should be(testPart.name)
        part.enabled should be(testPart.enabled)
        part.position should be(testPart.position)
        part.tasks should be(testPart.tasks)
        part.createdAt.toString should be(testPart.createdAt.toString)
        part.updatedAt.toString should be(testPart.updatedAt.toString)
      }
      "return RepositoryError.ForeignKeyConflict if there is a dependency in the tasks.dependency_id" in {
        val testPart = TestValues.testPartA

        val result = partRepository.delete(testPart)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.ForeignKeyConflict("dependency_id", "tasks_dependency_id_fkey")))
      }
      "return RepositoryError.NoResults if project doesn't exist" in {
        val testPart = TestValues.testPartD

        val result = partRepository.delete(testPart)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
      }
      "delete all parts in a project" in {
        (cache.removeCached(_: String)) when(*) returns(Future.successful(\/-( () )))

        val testProject = TestValues.testProjectC

        val testPartList = TreeMap[Int, Part](
          0 -> TestValues.testPartE,
          1 -> TestValues.testPartF,
          2 -> TestValues.testPartH
        )

        val testTaskList = TreeMap(
          testPartList(0).id.toString -> Vector(
            TestValues.testMatchingTaskK
          ),
          testPartList(1).id.toString -> Vector(),
          testPartList(2).id.toString -> Vector()
        )

        // Put here tasks = Vector(), because after db query Project object is created without tasks.
        testPartList.foreach {
          case (key, part: Part) => {
            (taskRepository.list(_: Part)(_: Connection, _: ScalaCachePool)) when(part.copy(tasks = Vector()), *, *) returns(Future.successful(\/-(testTaskList(part.id.toString))))
          }
        }

        val result = partRepository.delete(testProject)
        val eitherParts = Await.result(result, Duration.Inf)
        val \/-(parts) = eitherParts

        parts.size should be(testPartList.size)

        testPartList.foreach {
          case (key, part: Part) => {
            parts(key).id should be(part.id)
            parts(key).version should be(part.version)
            parts(key).projectId should be(part.projectId)
            parts(key).name should be(part.name)
            parts(key).enabled should be(part.enabled)
            parts(key).position should be(part.position)
            parts(key).tasks should be(part.tasks)
            parts(key).createdAt.toString should be(part.createdAt.toString)
            parts(key).updatedAt.toString should be(part.updatedAt.toString)
          }
        }
      }
      "return empty Vector() if project doesn't exist" in {
        (cache.removeCached(_: String)) when(*) returns(Future.successful(\/-( () )))

        val testProject = TestValues.testProjectD

        val result = partRepository.delete(testProject)
        Await.result(result, Duration.Inf) should be(\/-(Vector()))
      }
    }
  }
}