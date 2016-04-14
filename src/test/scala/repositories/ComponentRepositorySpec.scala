import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.repositories._
import org.scalatest.Matchers._
import org.scalatest._

import scala.collection.immutable.TreeMap
import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }
import scalaz._

class ComponentRepositorySpec
    extends TestEnvironment {
  val componentRepository = new ComponentRepositoryPostgres()

  "ComponentRepository.list" should {
    inSequence {
      "find all components" in {
        val testComponentList = TreeMap[Int, Component](
          0 -> TestValues.testAudioComponentE,
          1 -> TestValues.testTextComponentA,
          2 -> TestValues.testVideoComponentB,
          3 -> TestValues.testAudioComponentC
        )

        val result = componentRepository.list
        val eitherComponents = Await.result(result, Duration.Inf)
        val \/-(components) = eitherComponents

        components.size should be(testComponentList.size)

        testComponentList.foreach {
          case (key, component: Component) => {
            //Common
            components(key).id should be(component.id)
            components(key).version should be(component.version)
            components(key).ownerId should be(component.ownerId)
            components(key).title should be(component.title)
            components(key).questions should be(component.questions)
            components(key).thingsToThinkAbout should be(component.thingsToThinkAbout)
            components(key).createdAt.toString should be(component.createdAt.toString)
            components(key).updatedAt.toString should be(component.updatedAt.toString)

            //Specific
            components(key) match {
              case textComponent: TextComponent => {
                component match {
                  case component: TextComponent => {
                    textComponent.content should be(component.content)
                  }
                }
              }
              case videoComponent: VideoComponent => {
                component match {
                  case component: VideoComponent => {
                    videoComponent.vimeoId should be(component.vimeoId)
                    videoComponent.width should be(component.width)
                    videoComponent.height should be(component.height)
                  }
                }
              }
              case audioComponent: AudioComponent => {
                component match {
                  case component: AudioComponent => {
                    audioComponent.soundcloudId should be(component.soundcloudId)
                  }
                }
              }
            }
          }
        }
      }
      "find all components belonging to a specific part" in {
        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))

        val testPart = TestValues.testPartB

        val testComponentList = TreeMap[Int, Component](
          0 -> TestValues.testTextComponentA,
          1 -> TestValues.testVideoComponentB
        )

        val result = componentRepository.list(testPart)
        val eitherComponents = Await.result(result, Duration.Inf)
        val \/-(components) = eitherComponents

        components.size should be(testComponentList.size)

        testComponentList.foreach {
          case (key, component: Component) => {
            //Common
            components(key).id should be(component.id)
            components(key).version should be(component.version)
            components(key).ownerId should be(component.ownerId)
            components(key).title should be(component.title)
            components(key).questions should be(component.questions)
            components(key).thingsToThinkAbout should be(component.thingsToThinkAbout)
            components(key).createdAt.toString should be(component.createdAt.toString)
            components(key).updatedAt.toString should be(component.updatedAt.toString)

            //Specific
            components(key) match {
              case textComponent: TextComponent => {
                component match {
                  case component: TextComponent => {
                    textComponent.content should be(component.content)
                  }
                }
              }
              case videoComponent: VideoComponent => {
                component match {
                  case component: VideoComponent => {
                    videoComponent.vimeoId should be(component.vimeoId)
                    videoComponent.width should be(component.width)
                    videoComponent.height should be(component.height)
                  }
                }
              }
            }
          }
        }
      }
      "return empty Vector() if Part doesn't exist" in {
        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))

        val testPart = TestValues.testPartD

        val result = componentRepository.list(testPart)
        Await.result(result, Duration.Inf) should be(\/-(Vector()))
      }
      "find all components in a specific project" in {
        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))

        val testProject = TestValues.testProjectA

        val testComponentList = TreeMap[Int, Component](
          0 -> TestValues.testTextComponentA,
          1 -> TestValues.testVideoComponentB,
          2 -> TestValues.testAudioComponentC
        )

        val result = componentRepository.list(testProject)
        val eitherComponents = Await.result(result, Duration.Inf)
        val \/-(components) = eitherComponents

        components.size should be(testComponentList.size)

        testComponentList.foreach {
          case (key, component: Component) => {
            //Common
            components(key).id should be(component.id)
            components(key).version should be(component.version)
            components(key).ownerId should be(component.ownerId)
            components(key).title should be(component.title)
            components(key).questions should be(component.questions)
            components(key).thingsToThinkAbout should be(component.thingsToThinkAbout)
            components(key).createdAt.toString should be(component.createdAt.toString)
            components(key).updatedAt.toString should be(component.updatedAt.toString)

            //Specific
            components(key) match {
              case textComponent: TextComponent => {
                component match {
                  case component: TextComponent => {
                    textComponent.content should be(component.content)
                  }
                }
              }
              case videoComponent: VideoComponent => {
                component match {
                  case component: VideoComponent => {
                    videoComponent.vimeoId should be(component.vimeoId)
                    videoComponent.width should be(component.width)
                    videoComponent.height should be(component.height)
                  }
                }
              }
              case audioComponent: AudioComponent => {
                component match {
                  case component: AudioComponent => {
                    audioComponent.soundcloudId should be(component.soundcloudId)
                  }
                }
              }
            }
          }
        }
      }
      "return empty Vector() if Project doesn't exist" in {
        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))

        val testProject = TestValues.testProjectD

        val result = componentRepository.list(testProject)
        Await.result(result, Duration.Inf) should be(\/-(Vector()))
      }
      "find all components enabled for a specific user, in a specific project." in {
        val testUser = TestValues.testUserC
        val testProject = TestValues.testProjectA

        val testComponentList = TreeMap[Int, Component](
          0 -> TestValues.testTextComponentA,
          1 -> TestValues.testAudioComponentC
        )

        val result = componentRepository.list(testProject, testUser)
        val eitherComponents = Await.result(result, Duration.Inf)
        val \/-(components) = eitherComponents

        components.size should be(testComponentList.size)

        testComponentList.foreach {
          case (key, component: Component) => {
            //Common
            components(key).id should be(component.id)
            components(key).version should be(component.version)
            components(key).ownerId should be(component.ownerId)
            components(key).title should be(component.title)
            components(key).questions should be(component.questions)
            components(key).thingsToThinkAbout should be(component.thingsToThinkAbout)
            components(key).createdAt.toString should be(component.createdAt.toString)
            components(key).updatedAt.toString should be(component.updatedAt.toString)

            //Specific
            components(key) match {
              case textComponent: TextComponent => {
                component match {
                  case component: TextComponent => {
                    textComponent.content should be(component.content)
                  }
                }
              }
              case audioComponent: AudioComponent => {
                component match {
                  case component: AudioComponent => {
                    audioComponent.soundcloudId should be(component.soundcloudId)
                  }
                }
              }
            }
          }
        }
      }
      "return empty Vector() for a specific user if Project doesn't exist" in {
        val testUser = TestValues.testUserC
        val testProject = TestValues.testProjectD

        val result = componentRepository.list(testProject, testUser)
        Await.result(result, Duration.Inf) should be(\/-(Vector()))
      }
      "return empty Vector() if User doesn't exist" in {
        val testUser = TestValues.testUserD
        val testProject = TestValues.testProjectA

        val result = componentRepository.list(testProject, testUser)
        Await.result(result, Duration.Inf) should be(\/-(Vector()))
      }
    }
  }

  "ComponentRepository.find" should {
    inSequence {
      "find a single entry by ID" in {
        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))

        val testComponent = TestValues.testAudioComponentC

        val result = componentRepository.find(testComponent.id)
        val eitherComponent = Await.result(result, Duration.Inf)
        val \/-(component: AudioComponent) = eitherComponent

        //Common
        component.id should be(testComponent.id)
        component.version should be(testComponent.version)
        component.ownerId should be(testComponent.ownerId)
        component.title should be(testComponent.title)
        component.questions should be(testComponent.questions)
        component.thingsToThinkAbout should be(testComponent.thingsToThinkAbout)
        component.createdAt.toString should be(testComponent.createdAt.toString)
        component.updatedAt.toString should be(testComponent.updatedAt.toString)

        //Specific
        component.soundcloudId should be(testComponent.soundcloudId)
      }
      "return RepositoryError.NoResults if id is wrong" in {
        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))

        val id = UUID.fromString("024e4bde-282c-4947-a623-81ec11d2d85c")

        val result = componentRepository.find(id)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Component")))
      }
    }
  }

  "ComponentRepository.addToPart" should {
    inSequence {
      "add a component to a Part" in {
        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val testComponent = TestValues.testAudioComponentC
        val testPart = TestValues.testPartC

        val result = componentRepository.addToPart(testComponent, testPart)
        Await.result(result, Duration.Inf) should be(\/-(()))
      }
      "return RepositoryError.PrimaryKeyConflict if part already has the component" in {
        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val testComponent = TestValues.testTextComponentA
        val testPart = TestValues.testPartA

        val result = componentRepository.addToPart(testComponent, testPart)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.PrimaryKeyConflict))
      }
      "return RepositoryError.ForeignKeyConflict if Part doesn't exist" in {
        val testComponent = TestValues.testAudioComponentC
        val testPart = TestValues.testPartD

        val result = componentRepository.addToPart(testComponent, testPart)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.ForeignKeyConflict("part_id", "parts_components_part_id_fkey")))
      }
      "return RepositoryError.ForeignKeyConflict if Component doesn't exist" in {
        val testComponent = TestValues.testAudioComponentD
        val testPart = TestValues.testPartC

        val result = componentRepository.addToPart(testComponent, testPart)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.ForeignKeyConflict("component_id", "parts_components_component_id_fkey")))
      }
    }
  }

  "ComponentRepository.removeFromPart" should {
    inSequence {
      "remove a component from a Part" in {
        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val testComponent = TestValues.testTextComponentA
        val testPart = TestValues.testPartA

        val result = componentRepository.removeFromPart(testComponent, testPart)
        Await.result(result, Duration.Inf) should be(\/-(()))
      }
      "return RepositoryError.NoResults if Part doesn't exist" in {
        val testComponent = TestValues.testTextComponentA
        val testPart = TestValues.testPartD

        val result = componentRepository.removeFromPart(testComponent, testPart)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults(s"Could not remove component ${testComponent.id} from part ${testPart.id}")))
      }
      "return RepositoryError.NoResults if Component doesn't exist" in {
        val testComponent = TestValues.testAudioComponentD
        val testPart = TestValues.testPartA

        val result = componentRepository.removeFromPart(testComponent, testPart)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults(s"Could not remove component ${testComponent.id} from part ${testPart.id}")))
      }
      "remove all Components from a Part" in {
        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))
        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val testPart = TestValues.testPartB

        val testComponentList = TreeMap[Int, Component](
          0 -> TestValues.testTextComponentA,
          1 -> TestValues.testVideoComponentB
        )

        val result = componentRepository.removeFromPart(testPart)
        val eitherComponents = Await.result(result, Duration.Inf)
        val \/-(components) = eitherComponents

        components.size should be(testComponentList.size)

        testComponentList.foreach {
          case (key, component: Component) => {
            //Common
            components(key).id should be(component.id)
            components(key).version should be(component.version)
            components(key).ownerId should be(component.ownerId)
            components(key).title should be(component.title)
            components(key).questions should be(component.questions)
            components(key).thingsToThinkAbout should be(component.thingsToThinkAbout)
            components(key).createdAt.toString should be(component.createdAt.toString)
            components(key).updatedAt.toString should be(component.updatedAt.toString)

            //Specific
            components(key) match {
              case textComponent: TextComponent => {
                component match {
                  case component: TextComponent => {
                    textComponent.content should be(component.content)
                  }
                }
              }
              case videoComponent: VideoComponent => {
                component match {
                  case component: VideoComponent => {
                    videoComponent.vimeoId should be(component.vimeoId)
                    videoComponent.width should be(component.width)
                    videoComponent.height should be(component.height)
                  }
                }
              }
            }
          }
        }
      }
      "return empty Vector() when try to remove all Components if Part doesn't exist" in {
        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))
        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val testPart = TestValues.testPartD

        val result = componentRepository.removeFromPart(testPart)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults(s"Either the part ${testPart.id} doesn't have components or the part doesn't exist")))
      }
    }
  }

  "ComponentRepository.insert" should {
    inSequence {
      "insert TextComponent" in {
        val testComponent = TestValues.testTextComponentG

        val result = componentRepository.insert(testComponent)
        val eitherComponent = Await.result(result, Duration.Inf)
        val \/-(component: TextComponent) = eitherComponent

        //Common
        component.id should be(testComponent.id)
        component.version should be(testComponent.version)
        component.ownerId should be(testComponent.ownerId)
        component.title should be(testComponent.title)
        component.questions should be(testComponent.questions)
        component.thingsToThinkAbout should be(testComponent.thingsToThinkAbout)

        //Specific
        component.content should be(testComponent.content)
      }
      "return RepositoryError.PrimaryKeyConflict if TextComponent already exists" in {
        val testComponent = TestValues.testTextComponentA

        val result = componentRepository.insert(testComponent)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.PrimaryKeyConflict))
      }
      "insert VideoComponent" in {
        val testComponent = TestValues.testVideoComponentF

        val result = componentRepository.insert(testComponent)
        val eitherComponent = Await.result(result, Duration.Inf)
        val \/-(component: VideoComponent) = eitherComponent

        //Common
        component.id should be(testComponent.id)
        component.version should be(testComponent.version)
        component.ownerId should be(testComponent.ownerId)
        component.title should be(testComponent.title)
        component.questions should be(testComponent.questions)
        component.thingsToThinkAbout should be(testComponent.thingsToThinkAbout)

        //Specific
        component.vimeoId should be(testComponent.vimeoId)
        component.width should be(testComponent.width)
        component.height should be(testComponent.height)
      }
      "return RepositoryError.PrimaryKeyConflict if VideoComponent already exists" in {
        val testComponent = TestValues.testVideoComponentB

        val result = componentRepository.insert(testComponent)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.PrimaryKeyConflict))
      }
      "insert AudioComponent" in {
        val testComponent = TestValues.testAudioComponentD

        val result = componentRepository.insert(testComponent)
        val eitherComponent = Await.result(result, Duration.Inf)
        val \/-(component: AudioComponent) = eitherComponent

        //Common
        component.id should be(testComponent.id)
        component.version should be(testComponent.version)
        component.ownerId should be(testComponent.ownerId)
        component.title should be(testComponent.title)
        component.questions should be(testComponent.questions)
        component.thingsToThinkAbout should be(testComponent.thingsToThinkAbout)

        //Specific
        component.soundcloudId should be(testComponent.soundcloudId)
      }
      "return RepositoryError.PrimaryKeyConflict if AudioComponent already exists" in {
        val testComponent = TestValues.testAudioComponentC

        val result = componentRepository.insert(testComponent)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.PrimaryKeyConflict))
      }
    }
  }

  "ComponentRepository.update" should {
    inSequence {
      "update TextComponent" in {
        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val testComponent = TestValues.testTextComponentA
        val updatedComponent = testComponent.copy(
          ownerId = TestValues.testUserF.id,
          title = "updated title",
          questions = "updated questions",
          thingsToThinkAbout = "updated thingsToThinkAbout",
          content = "updated content"
        )

        val result = componentRepository.update(updatedComponent, None)
        val eitherComponent = Await.result(result, Duration.Inf)
        val \/-(component: TextComponent) = eitherComponent

        //Common
        component.id should be(updatedComponent.id)
        component.version should be(updatedComponent.version + 1)
        component.ownerId should be(updatedComponent.ownerId)
        component.title should be(updatedComponent.title)
        component.questions should be(updatedComponent.questions)
        component.thingsToThinkAbout should be(updatedComponent.thingsToThinkAbout)
        component.createdAt.toString should be(updatedComponent.createdAt.toString)
        component.updatedAt.toString should not be (updatedComponent.updatedAt.toString)

        //Specific
        component.content should be(updatedComponent.content)
      }
      "return RepositoryError.NoResults if TextComponent doesn't exist" in {
        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val testComponent = TestValues.testTextComponentG
        val updatedComponent = testComponent.copy(
          ownerId = TestValues.testUserF.id,
          title = "updated title",
          questions = "updated questions",
          thingsToThinkAbout = "updated thingsToThinkAbout",
          content = "updated content"
        )

        val result = componentRepository.update(updatedComponent, None)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Component")))
      }
      "update VideoComponent" in {
        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val testComponent = TestValues.testVideoComponentB
        val updatedComponent = testComponent.copy(
          ownerId = TestValues.testUserF.id,
          title = "updated title",
          questions = "updated questions",
          thingsToThinkAbout = "updated thingsToThinkAbout",
          vimeoId = "bla bla",
          width = 128,
          height = 128
        )

        val result = componentRepository.update(updatedComponent, None)
        val eitherComponent = Await.result(result, Duration.Inf)
        val \/-(component: VideoComponent) = eitherComponent

        //Common
        component.id should be(updatedComponent.id)
        component.version should be(updatedComponent.version + 1)
        component.ownerId should be(updatedComponent.ownerId)
        component.title should be(updatedComponent.title)
        component.questions should be(updatedComponent.questions)
        component.thingsToThinkAbout should be(updatedComponent.thingsToThinkAbout)
        component.createdAt.toString should be(updatedComponent.createdAt.toString)
        component.updatedAt.toString should not be (updatedComponent.updatedAt.toString)

        //Specific
        component.vimeoId should be(updatedComponent.vimeoId)
        component.width should be(updatedComponent.width)
        component.height should be(updatedComponent.height)
      }
      "return RepositoryError.NoResults if VideoComponent doesn't exist" in {
        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val testComponent = TestValues.testVideoComponentF
        val updatedComponent = testComponent.copy(
          ownerId = TestValues.testUserF.id,
          title = "updated title",
          questions = "updated questions",
          thingsToThinkAbout = "updated thingsToThinkAbout",
          vimeoId = "bla bla",
          width = 128,
          height = 128
        )

        val result = componentRepository.update(updatedComponent, None)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Component")))
      }
      "update AudioComponent" in {
        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val testComponent = TestValues.testAudioComponentC
        val updatedComponent = testComponent.copy(
          ownerId = TestValues.testUserF.id,
          title = "updated title",
          questions = "updated questions",
          thingsToThinkAbout = "updated thingsToThinkAbout",
          soundcloudId = "bla bla bla"
        )

        val result = componentRepository.update(updatedComponent, None)
        val eitherComponent = Await.result(result, Duration.Inf)
        val \/-(component: AudioComponent) = eitherComponent

        //Common
        component.id should be(updatedComponent.id)
        component.version should be(updatedComponent.version + 1)
        component.ownerId should be(updatedComponent.ownerId)
        component.title should be(updatedComponent.title)
        component.questions should be(updatedComponent.questions)
        component.thingsToThinkAbout should be(updatedComponent.thingsToThinkAbout)
        component.createdAt.toString should be(updatedComponent.createdAt.toString)
        component.updatedAt.toString should not be (updatedComponent.updatedAt.toString)

        //Specific
        component.soundcloudId should be(updatedComponent.soundcloudId)
      }
      "return RepositoryError.NoResults if AudioComponent doesn't exist" in {
        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val testComponent = TestValues.testAudioComponentD
        val updatedComponent = testComponent.copy(
          ownerId = TestValues.testUserF.id,
          title = "updated title",
          questions = "updated questions",
          thingsToThinkAbout = "updated thingsToThinkAbout",
          soundcloudId = "bla bla bla"
        )

        val result = componentRepository.update(updatedComponent, None)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Component")))
      }
    }
  }

  "ComponentRepository.delete" should {
    inSequence {
      "delete TextComponent" in {
        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val testComponent = TestValues.testTextComponentA

        val result = componentRepository.delete(testComponent)
        val eitherComponent = Await.result(result, Duration.Inf)
        val \/-(component: TextComponent) = eitherComponent

        //Common
        component.id should be(testComponent.id)
        component.version should be(testComponent.version)
        component.ownerId should be(testComponent.ownerId)
        component.title should be(testComponent.title)
        component.questions should be(testComponent.questions)
        component.thingsToThinkAbout should be(testComponent.thingsToThinkAbout)
        component.createdAt.toString should be(testComponent.createdAt.toString)
        component.updatedAt.toString should be(testComponent.updatedAt.toString)

        //Specific
        component.content should be(testComponent.content)
      }
      "return RepositoryError.NoResults if TextComponent doesn't exist" in {
        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val testComponent = TestValues.testTextComponentG

        val result = componentRepository.delete(testComponent)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Component")))
      }
      "return RepositoryError.NoResults if TextComponent version is wrong" in {
        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val testComponent = TestValues.testTextComponentA.copy(
          version = 99L
        )

        val result = componentRepository.delete(testComponent)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Component")))
      }
      "delete VideoComponent" in {
        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val testComponent = TestValues.testVideoComponentB

        val result = componentRepository.delete(testComponent)
        val eitherComponent = Await.result(result, Duration.Inf)
        val \/-(component: VideoComponent) = eitherComponent

        //Common
        component.id should be(testComponent.id)
        component.version should be(testComponent.version)
        component.ownerId should be(testComponent.ownerId)
        component.title should be(testComponent.title)
        component.questions should be(testComponent.questions)
        component.thingsToThinkAbout should be(testComponent.thingsToThinkAbout)

        //Specific
        component.vimeoId should be(testComponent.vimeoId)
        component.width should be(testComponent.width)
        component.height should be(testComponent.height)
      }
      "return RepositoryError.NoResults if VideoComponent doesn't exist" in {
        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val testComponent = TestValues.testVideoComponentF

        val result = componentRepository.delete(testComponent)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Component")))
      }
      "return RepositoryError.NoResults if VideoComponent version is wrong" in {
        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val testComponent = TestValues.testVideoComponentB.copy(
          version = 99L
        )

        val result = componentRepository.delete(testComponent)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Component")))
      }
      "delete AudioComponent" in {
        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val testComponent = TestValues.testAudioComponentC

        val result = componentRepository.delete(testComponent)
        val eitherComponent = Await.result(result, Duration.Inf)
        val \/-(component: AudioComponent) = eitherComponent

        //Common
        component.id should be(testComponent.id)
        component.version should be(testComponent.version)
        component.ownerId should be(testComponent.ownerId)
        component.title should be(testComponent.title)
        component.questions should be(testComponent.questions)
        component.thingsToThinkAbout should be(testComponent.thingsToThinkAbout)

        //Specific
        component.soundcloudId should be(testComponent.soundcloudId)
      }
      "return RepositoryError.NoResults if AudioComponent doesn't exist" in {
        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val testComponent = TestValues.testAudioComponentD

        val result = componentRepository.delete(testComponent)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Component")))
      }
      "return RepositoryError.NoResults if AudioComponent version is wrong" in {
        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val testComponent = TestValues.testAudioComponentC.copy(
          version = 99L
        )

        val result = componentRepository.delete(testComponent)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Component")))
      }
    }
  }
}