import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.krispii.core.models._

import org.scalatest._
import Matchers._
import scala.collection.immutable.TreeMap
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scalaz._

class ComponentRepositorySpec
  extends TestEnvironment
{
  val partRepository = stub[PartRepository]
  val userRepository = stub[UserRepository]
  val componentRepository = new ComponentRepositoryPostgres(userRepository, partRepository)

  "ComponentRepository.list" should {
    inSequence {
      "find all components" in {
        val testComponentList = TreeMap[Int, Component](
          0 -> TestValues.testAudioComponentC,
          1 -> TestValues.testTextComponentA,
          2 -> TestValues.testVideoComponentB
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
      "find all components in a specific project" in {
        val testProject = TestValues.testProjectA

        val testComponentList = TreeMap[Int, Component](
          0 -> TestValues.testTextComponentA,
          1 -> TestValues.testVideoComponentB
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
            }
          }
        }
      }
      "find all components enabled for a specific user, in a specific project." in {
        val testUser = TestValues.testUserC
        val testProject = TestValues.testProjectA

        val testComponentList = TreeMap[Int, Component](
          0 -> TestValues.testTextComponentA
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
            }
          }
        }
      }
    }
  }

  "ComponentRepository.find" should {
    inSequence {
      "find a single entry by ID" in {
        val testComponent = TestValues.testAudioComponentD

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
    }
  }

  "ComponentRepository.addToPart" should {
    inSequence {
      "add a component to a Part" in {

      }
    }
  }
}