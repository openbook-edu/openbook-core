import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.work._
import ca.shiftfocus.krispii.core.models.document._
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.Connection
import ca.shiftfocus.krispii.core.models.tasks.MatchingTask.Match

import org.scalatest._
import Matchers._
import scala.collection.immutable.TreeMap
import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration
import scalaz._

class WorkRepositorySpec
  extends TestEnvironment {
  val documentRepository = stub[DocumentRepository]
  val revisionRepository = stub[RevisionRepository]
  val workRepository = new WorkRepositoryPostgres(documentRepository, revisionRepository)

  "WorkRepository.list" should {
    inSequence {
      /* --- list(testUser, testProject) --- */
      "list the latest revision of work for each task in a project for a user (MultipleChoiceWork, LongAnswerWork, ShortAnswerWork, OrderingWork)" in {
        val testUser = TestValues.testUserC
        val testProject = TestValues.testProjectA

        val testWorkList = TreeMap[Int, Work](
          0 -> TestValues.testLongAnswerWorkA,
          1 -> TestValues.testShortAnswerWorkG,
          2 -> TestValues.testMultipleChoiceWorkC,
          3 -> TestValues.testOrderingWorkD
        )

        val testDocumentList = TreeMap[Int, Document](
          0 -> TestValues.testDocumentA,
          1 -> TestValues.testDocumentD
        )

        testDocumentList.foreach {
          case (key, document: Document) => {
            (documentRepository.find(_: UUID, _: Long)(_: Connection)) when(document.id, *, *) returns(Future.successful(\/-(document)))
          }
        }

        val result = workRepository.list(testUser, testProject)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works) = eitherWorks

        works.size should be(testWorkList.size)

        testWorkList.foreach {
          case (key, work: Work) => {
            works(key).id should be(work.id)
            works(key).studentId should be(work.studentId)
            works(key).taskId should be(work.taskId)
            works(key).version should be(work.version)
            works(key).isComplete should be(work.isComplete)
            works(key).createdAt.toString should be(work.createdAt.toString)
            works(key).updatedAt.toString should be(work.updatedAt.toString)

            //Specific
            works(key) match {
              case documentWork: DocumentWork => {
                work match {
                  case work: DocumentWork => {
                    documentWork.response should be(work.response)
                  }
                }
              }
              case intListWork: IntListWork => {
                work match {
                  case work: IntListWork => {
                    intListWork.response should be(work.response)
                  }
                }
              }
              case matchListWork: MatchListWork => {
                work match {
                  case work: MatchListWork => {
                    matchListWork.response should be(work.response)
                  }
                }
              }
            }
          }
        }
      }
      "list the latest revision of work for each task in a project for a user (MatchingWork)" in {
        val testUser = TestValues.testUserC
        val testProject = TestValues.testProjectB

        val testWorkList = TreeMap[Int, Work](
          0 -> TestValues.testMatchingWorkE
        )

        val result = workRepository.list(testUser, testProject)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works) = eitherWorks
        works.size should be(testWorkList.size)

        testWorkList.foreach {
          case (key, work: Work) => {
            works(key).id should be(work.id)
            works(key).studentId should be(work.studentId)
            works(key).taskId should be(work.taskId)
            works(key).version should be(work.version)
            works(key).isComplete should be(work.isComplete)
            works(key).createdAt.toString should be(work.createdAt.toString)
            works(key).updatedAt.toString should be(work.updatedAt.toString)

            //Specific
            works(key) match {
              case matchListWork: MatchListWork => {
                work match {
                  case work: MatchListWork => {
                    matchListWork.response should be(work.response)
                  }
                }
              }
            }
          }
        }
      }
      "return empty Vector() if user doesn't exist" in {
        val testUser = TestValues.testUserD
        val testProject = TestValues.testProjectB

        val result = workRepository.list(testUser, testProject)
        Await.result(result, Duration.Inf) should be(\/-(Vector()))
      }
      "return empty Vector() if project doesn't exist" in {
        val testUser = TestValues.testUserC
        val testProject = TestValues.testProjectD

        val result = workRepository.list(testUser, testProject)
        Await.result(result, Duration.Inf) should be(\/-(Vector()))
      }
      "return empty Vector() if user doesn't have this project" in {
        val testUser = TestValues.testUserH
        val testProject = TestValues.testProjectB

        val result = workRepository.list(testUser, testProject)
        Await.result(result, Duration.Inf) should be(\/-(Vector()))
      }

      /* --- list(testUser, testTask) --- */
      "list all revisions of a specific work for a user (LongAnswerWork)" in {
        val testUser = TestValues.testUserC
        val testTask = TestValues.testLongAnswerTaskA
        val testWork = TestValues.testLongAnswerWorkA

        val testDocumentList = TreeMap[Int, Document](
          0 -> TestValues.testDocumentA
        )

        val testRevisionList = Map[UUID, IndexedSeq[Revision]](
          testDocumentList(0).id -> IndexedSeq(
            TestValues.testCurrentRevisionA,
            TestValues.testPreviousRevisionA
          )
        )

        val testWorkResult = testWork.copy(
          response = Some(testWork.response.get.copy(
            revisions = testRevisionList(testDocumentList(0).id)
          ))
        )

        testDocumentList.foreach {
          case (key, document: Document) => {
            (documentRepository.find(_: UUID, _: Long)(_: Connection)) when(document.id, *, *) returns(Future.successful(\/-(document)))
            (revisionRepository.list(_: Document, _: Long, _: Long)(_: Connection)) when(document, *, document.version, *) returns(Future.successful(\/-(testRevisionList(testDocumentList(key).id))))
          }
        }

        val result = workRepository.list(testUser, testTask)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works) = eitherWorks
        val Left(documentWork: DocumentWork) = works

        documentWork.id should be(testWorkResult.id)
        documentWork.studentId should be(testWorkResult.studentId)
        documentWork.taskId should be(testWorkResult.taskId)
        documentWork.version should be(testWorkResult.version)
        documentWork.response should be(testWorkResult.response)
        documentWork.isComplete should be(testWorkResult.isComplete)
        documentWork.createdAt.toString should be(testWorkResult.createdAt.toString)
        documentWork.updatedAt.toString should be(testWorkResult.updatedAt.toString)
      }
      "list all revisions of a specific work for a user (ShortAnswerWork)" in {
        val testUser = TestValues.testUserE
        val testTask = TestValues.testShortAnswerTaskB
        val testWork = TestValues.testShortAnswerWorkB


        val testDocumentList = TreeMap[Int, Document](
          0 -> TestValues.testDocumentC
        )

        val testRevisionList = Map[UUID, IndexedSeq[Revision]](
          testDocumentList(0).id -> IndexedSeq(
            TestValues.testCurrentRevisionC,
            TestValues.testPreviousRevisionC
          )
        )

        val testWorkResult = testWork.copy(
          response = Some(testWork.response.get.copy(
            revisions = testRevisionList(testDocumentList(0).id)
          ))
        )

        testDocumentList.foreach {
          case (key, document: Document) => {
            (documentRepository.find(_: UUID, _: Long)(_: Connection)) when(document.id, *, *) returns(Future.successful(\/-(document)))
            (revisionRepository.list(_: Document, _: Long, _: Long)(_: Connection)) when(document, *, document.version, *) returns(Future.successful(\/-(testRevisionList(testDocumentList(key).id))))
          }
        }

        val result = workRepository.list(testUser, testTask)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works) = eitherWorks
        val Left(documentWork: DocumentWork) = works

        documentWork.id should be(testWorkResult.id)
        documentWork.studentId should be(testWorkResult.studentId)
        documentWork.taskId should be(testWorkResult.taskId)
        documentWork.version should be(testWorkResult.version)
        documentWork.response should be(testWorkResult.response)
        documentWork.isComplete should be(testWorkResult.isComplete)
        documentWork.createdAt.toString should be(testWorkResult.createdAt.toString)
        documentWork.updatedAt.toString should be(testWorkResult.updatedAt.toString)
      }
      "list all revisions of a specific work for a user (MultipleChoiceWork)" in {
        val testUser = TestValues.testUserE
        val testTask = TestValues.testMultipleChoiceTaskC

        val testWorkList = TreeMap[Int, Work](
          0 -> TestValues.testMultipleChoiceWorkH,
          1 -> TestValues.testMultipleChoiceWorkRevisionH
        )

        val result = workRepository.list(testUser, testTask)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works) = eitherWorks
        val Right(listWork) = works

        listWork.size should be(testWorkList.size)

        testWorkList.foreach {
          case (key, work: IntListWork) => {
            listWork(key).id should be(work.id)
            listWork(key).studentId should be(work.studentId)
            listWork(key).taskId should be(work.taskId)
            listWork(key).version should be(work.version)
            listWork(key).response should be(work.response)
            listWork(key).isComplete should be(work.isComplete)
            listWork(key).createdAt.toString should be(work.createdAt.toString)
            listWork(key).updatedAt.toString should be(work.updatedAt.toString)
          }
        }
      }
      "list all revisions of a specific work for a user (OrderingWork)" in {
        val testUser = TestValues.testUserE
        val testTask = TestValues.testOrderingTaskN

        val testWorkList = TreeMap[Int, Work](
          0 -> TestValues.testOrderingWorkI,
          1 -> TestValues.testOrderingWorkRevisionI
        )

        val result = workRepository.list(testUser, testTask)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works) = eitherWorks
        val Right(listWork) = works

        listWork.size should be(testWorkList.size)

        testWorkList.foreach {
          case (key, work: IntListWork) => {
            listWork(key).id should be(work.id)
            listWork(key).studentId should be(work.studentId)
            listWork(key).taskId should be(work.taskId)
            listWork(key).version should be(work.version)
            listWork(key).response should be(work.response)
            listWork(key).isComplete should be(work.isComplete)
            listWork(key).createdAt.toString should be(work.createdAt.toString)
            listWork(key).updatedAt.toString should be(work.updatedAt.toString)
          }
        }
      }
      "list all revisions of a specific work for a user (MatchingWork)" in {
        val testUser = TestValues.testUserE
        val testTask = TestValues.testMatchingTaskE

        val testWorkList = TreeMap[Int, Work](
          0 -> TestValues.testMatchingWorkJ,
          1 -> TestValues.testMatchingWorkRevisionJ
        )

        val result = workRepository.list(testUser, testTask)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works) = eitherWorks
        val Right(listWork) = works

        listWork.size should be(testWorkList.size)

        testWorkList.foreach {
          case (key, work: MatchListWork) => {
            listWork(key).id should be(work.id)
            listWork(key).studentId should be(work.studentId)
            listWork(key).taskId should be(work.taskId)
            listWork(key).version should be(work.version)
            listWork(key).response should be(work.response)
            listWork(key).isComplete should be(work.isComplete)
            listWork(key).createdAt.toString should be(work.createdAt.toString)
            listWork(key).updatedAt.toString should be(work.updatedAt.toString)
          }
        }
      }
      "return empty Vector() for task if user doesn't exist" in {
        val testUser = TestValues.testUserD
        val testTask = TestValues.testMatchingTaskE

        val result = workRepository.list(testUser, testTask)
        Await.result(result, Duration.Inf) should be(\/-(Right(Vector())))
      }
      "return empty Vector() if the task doesn't exist for a user" in {
        val testUser = TestValues.testUserE
        val testTask = TestValues.testMatchingTaskJ

        val result = workRepository.list(testUser, testTask)
        Await.result(result, Duration.Inf) should be(\/-(Right(Vector())))
      }
      "return empty Vector() if the user is not connected with this task" in {
        val testUser = TestValues.testUserG
        val testTask = TestValues.testMatchingTaskE

        val result = workRepository.list(testUser, testTask)
        Await.result(result, Duration.Inf) should be(\/-(Right(Vector())))
      }
      "return empty Vector() if the user doesn't have any work within this task" in {
        val testUser = TestValues.testUserC
        val testTask = TestValues.testOrderingTaskD

        val result = workRepository.list(testUser, testTask)
        Await.result(result, Duration.Inf) should be(\/-(Right(Vector())))
      }

      /* --- list(testTask) --- */
      "list latest revisions of a specific work for all users (LongAnswerWork)" in {
        val testTask = TestValues.testLongAnswerTaskA

        val testWorkList = TreeMap[Int, Work](
          0 -> TestValues.testLongAnswerWorkA,
          1 -> TestValues.testLongAnswerWorkF
        )

        val testDocumentList = TreeMap[Int, Document](
          0 -> TestValues.testDocumentA,
          1 -> TestValues.testDocumentB
        )

        testDocumentList.foreach {
          case (key, document: Document) => {
            (documentRepository.find(_: UUID, _: Long)(_: Connection)) when(document.id, *, *) returns(Future.successful(\/-(document)))
          }
        }

        val result = workRepository.list(testTask)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works) = eitherWorks

        works.size should be(testWorkList.size)

        testWorkList.foreach {
          case (key, work: Work) => {
            works(key).id should be(work.id)
            works(key).studentId should be(work.studentId)
            works(key).taskId should be(work.taskId)
            works(key).version should be(work.version)
            works(key).isComplete should be(work.isComplete)
            works(key).createdAt.toString should be(work.createdAt.toString)
            works(key).updatedAt.toString should be(work.updatedAt.toString)

            //Specific
            works(key) match {
              case documentWork: DocumentWork => {
                work match {
                  case work: DocumentWork => {
                    documentWork.response should be(work.response)
                  }
                }
              }
            }
          }
        }
      }
      "list latest revisions of a specific work for all users (ShortAnswerWork)" in {
        val testTask = TestValues.testShortAnswerTaskB

        val testWorkList = TreeMap[Int, Work](
          0 -> TestValues.testShortAnswerWorkB,
          1 -> TestValues.testShortAnswerWorkG
        )

        val testDocumentList = TreeMap[Int, Document](
          0 -> TestValues.testDocumentC,
          1 -> TestValues.testDocumentD
        )

        testDocumentList.foreach {
          case (key, document: Document) => {
            (documentRepository.find(_: UUID, _: Long)(_: Connection)) when(document.id, *, *) returns(Future.successful(\/-(document)))
          }
        }

        val result = workRepository.list(testTask)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works) = eitherWorks

        works.size should be(testWorkList.size)

        testWorkList.foreach {
          case (key, work: Work) => {
            works(key).id should be(work.id)
            works(key).studentId should be(work.studentId)
            works(key).taskId should be(work.taskId)
            works(key).version should be(work.version)
            works(key).isComplete should be(work.isComplete)
            works(key).createdAt.toString should be(work.createdAt.toString)
            works(key).updatedAt.toString should be(work.updatedAt.toString)

            //Specific
            works(key) match {
              case documentWork: DocumentWork => {
                work match {
                  case work: DocumentWork => {
                    documentWork.response should be(work.response)
                  }
                }
              }
            }
          }
        }
      }
      "list latest revisions of a specific work for all users (MultipleChoiceWork)" in {
        val testTask = TestValues.testMultipleChoiceTaskC

        val testWorkList = TreeMap[Int, Work](
          0 -> TestValues.testMultipleChoiceWorkC,
          1 -> TestValues.testMultipleChoiceWorkH
        )

        val result = workRepository.list(testTask)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works) = eitherWorks

        works.size should be(testWorkList.size)

        testWorkList.foreach {
          case (key, work: Work) => {
            works(key).id should be(work.id)
            works(key).studentId should be(work.studentId)
            works(key).taskId should be(work.taskId)
            works(key).version should be(work.version)
            works(key).isComplete should be(work.isComplete)
            works(key).createdAt.toString should be(work.createdAt.toString)
            works(key).updatedAt.toString should be(work.updatedAt.toString)

            //Specific
            works(key) match {
              case intListWork: IntListWork => {
                work match {
                  case work: IntListWork => {
                    intListWork.response should be(work.response)
                  }
                }
              }
            }
          }
        }
      }
      "list latest revisions of a specific work for all users (OrderingWork)" in {
        val testTask = TestValues.testOrderingTaskN

        val testWorkList = TreeMap[Int, Work](
          0 -> TestValues.testOrderingWorkI,
          1 -> TestValues.testOrderingWorkD
        )

        val result = workRepository.list(testTask)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works) = eitherWorks

        works.size should be(testWorkList.size)

        testWorkList.foreach {
          case (key, work: Work) => {
            works(key).id should be(work.id)
            works(key).studentId should be(work.studentId)
            works(key).taskId should be(work.taskId)
            works(key).version should be(work.version)
            works(key).isComplete should be(work.isComplete)
            works(key).createdAt.toString should be(work.createdAt.toString)
            works(key).updatedAt.toString should be(work.updatedAt.toString)

            //Specific
            works(key) match {
              case intListWork: IntListWork => {
                work match {
                  case work: IntListWork => {
                    intListWork.response should be(work.response)
                  }
                }
              }
            }
          }
        }
      }
      "list latest revisions of a specific work for all users (MatchingWork)" in {
        val testTask = TestValues.testMatchingTaskE

        val testWorkList = TreeMap[Int, Work](
          0 -> TestValues.testMatchingWorkJ,
          1 -> TestValues.testMatchingWorkE
        )

        val result = workRepository.list(testTask)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works) = eitherWorks

        works.size should be(testWorkList.size)

        testWorkList.foreach {
          case (key, work: Work) => {
            works(key).id should be(work.id)
            works(key).studentId should be(work.studentId)
            works(key).taskId should be(work.taskId)
            works(key).version should be(work.version)
            works(key).isComplete should be(work.isComplete)
            works(key).createdAt.toString should be(work.createdAt.toString)
            works(key).updatedAt.toString should be(work.updatedAt.toString)

            //Specific
            works(key) match {
              case matchListWork: MatchListWork => {
                work match {
                  case work: MatchListWork => {
                    matchListWork.response should be(work.response)
                  }
                }
              }
            }
          }
        }
      }
      "return empty Vector() if the task doesn't exist" in {
        val testTask = TestValues.testMatchingTaskJ

        val result = workRepository.list(testTask)
        Await.result(result, Duration.Inf) should be(\/-(Vector()))
      }
      "return empty Vector() if the task doesn't have any work" in {
        val testTask = TestValues.testMatchingTaskM

        val result = workRepository.list(testTask)
        Await.result(result, Duration.Inf) should be(\/-(Vector()))
      }
    }
  }

  "WorkRepository.find" should {
    inSequence {
      /* --- find(workId) --- */
      "find the latest revision of a single work (LongAnswerWork)" in {
        val testWork     = TestValues.testLongAnswerWorkA
        val testDocument = TestValues.testDocumentA

        (documentRepository.find(_: UUID, _: Long)(_: Connection)) when(testDocument.id, *, *) returns(Future.successful(\/-(testDocument)))

        val result = workRepository.find(testWork.id)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: DocumentWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "find the latest revision of a single work (ShortAnswerWork)" in {
        val testWork     = TestValues.testShortAnswerWorkB
        val testDocument = TestValues.testDocumentC

        (documentRepository.find(_: UUID, _: Long)(_: Connection)) when(testDocument.id, *, *) returns(Future.successful(\/-(testDocument)))

        val result = workRepository.find(testWork.id)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: DocumentWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "find the latest revision of a single work (MultipleChoiceWork)" in {
        val testWork = TestValues.testMultipleChoiceWorkC

        val result = workRepository.find(testWork.id)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: MultipleChoiceWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "find the latest revision of a single work (OrderingWork)" in {
        val testWork = TestValues.testOrderingWorkD

        val result = workRepository.find(testWork.id)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: OrderingWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "find the latest revision of a single work (MatchingWork)" in {
        val testWork = TestValues.testMatchingWorkJ

        val result = workRepository.find(testWork.id)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: MatchingWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "return RepositoryError.NoResults if work doesn't exist" in {
        val testWork = TestValues.testMatchingWorkO

        val result = workRepository.find(testWork.id)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
      }

      /* --- find(workId, version) --- */
      "find a specific revision of a single work (LongAnswerWork)" in {
        val testWork     = TestValues.testLongAnswerWorkRevisionA
        val testDocument = TestValues.testDocumentRevisionA

        (documentRepository.find(_: UUID, _: Long)(_: Connection)) when(testDocument.id, testDocument.version, *) returns(Future.successful(\/-(testDocument)))

        val result = workRepository.find(testWork.id, testDocument.version)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: DocumentWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "find a specific revision of a single work (ShortAnswerWork)" in {
        val testWork     = TestValues.testShortAnswerWorkRevisionB
        val testDocument = TestValues.testDocumentRevisionC

        (documentRepository.find(_: UUID, _: Long)(_: Connection)) when(testDocument.id, testDocument.version, *) returns(Future.successful(\/-(testDocument)))

        val result = workRepository.find(testWork.id, testDocument.version)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: DocumentWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "find a specifict revision of a single work (MultipleChoiceWork)" in {
        val testWork = TestValues.testMultipleChoiceWorkRevisionC

        val result = workRepository.find(testWork.id, testWork.version)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: MultipleChoiceWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "find a specific revision of a single work (OrderingWork)" in {
        val testWork = TestValues.testOrderingWorkRevisionD

        val result = workRepository.find(testWork.id, testWork.version)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: OrderingWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "find a specific revision of a single work (MatchingWork)" in {
        val testWork = TestValues.testMatchingWorkRevisionJ

        val result = workRepository.find(testWork.id, testWork.version)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: MatchingWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "return RepositoryError.NoResults if work doesn't exist with version" in {
        val testWork = TestValues.testMatchingWorkO

        val result = workRepository.find(testWork.id, testWork.version)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
      }

      /* --- find(user, task) --- */
      "find the latest revision of a single work for a user within a Task (LongAnswerWork)" in {
        val testUser     = TestValues.testUserE
        val testTask     = TestValues.testLongAnswerTaskA
        val testWork     = TestValues.testLongAnswerWorkF
        val testDocument = TestValues.testDocumentB

        (documentRepository.find(_: UUID, _: Long)(_: Connection)) when(testDocument.id, *, *) returns(Future.successful(\/-(testDocument)))

        val result = workRepository.find(testUser, testTask)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: DocumentWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "find the latest revision of a single work for a user within a Task (ShortAnswerWork)" in {
        val testUser     = TestValues.testUserE
        val testTask     = TestValues.testShortAnswerTaskB
        val testWork     = TestValues.testShortAnswerWorkB
        val testDocument = TestValues.testDocumentC

        (documentRepository.find(_: UUID, _: Long)(_: Connection)) when(testDocument.id, *, *) returns(Future.successful(\/-(testDocument)))

        val result = workRepository.find(testUser, testTask)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: DocumentWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "find the latest revision of a single work for a user within a Task (MultipleChoice)" in {
        val testUser     = TestValues.testUserE
        val testTask     = TestValues.testMultipleChoiceTaskC
        val testWork     = TestValues.testMultipleChoiceWorkH

        val result = workRepository.find(testUser, testTask)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: MultipleChoiceWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "find the latest revision of a single work for a user within a Task (OrderingWork)" in {
        val testUser     = TestValues.testUserE
        val testTask     = TestValues.testOrderingTaskN
        val testWork     = TestValues.testOrderingWorkI

        val result = workRepository.find(testUser, testTask)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: OrderingWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "find the latest revision of a single work for a user within a Task (MatchingWork)" in {
        val testUser     = TestValues.testUserE
        val testTask     = TestValues.testMatchingTaskE
        val testWork     = TestValues.testMatchingWorkJ

        val result = workRepository.find(testUser, testTask)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: MatchingWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "return RepositoryError.NoResults if user doesn't exist" in {
        val testUser     = TestValues.testUserD
        val testTask     = TestValues.testMatchingTaskE

        val result = workRepository.find(testUser, testTask)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
      }
      "return RepositoryError.NoResults if task doesn't exist" in {
        val testUser     = TestValues.testUserE
        val testTask     = TestValues.testMatchingTaskJ

        val result = workRepository.find(testUser, testTask)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
      }
      "return RepositoryError.NoResults if user is not connected with a task" in {
        val testUser     = TestValues.testUserG
        val testTask     = TestValues.testMatchingTaskE

        val result = workRepository.find(testUser, testTask)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
      }
      "return RepositoryError.NoResults if user doesn't have work within a task" in {
        val testUser     = TestValues.testUserE
        val testTask     = TestValues.testMatchingTaskM

        val result = workRepository.find(testUser, testTask)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
      }

      /* --- find(user, task, version) --- */
      "find a specific revision for a single work for a user within a Task (LongAnswerWork)" in {
        val testUser     = TestValues.testUserC
        val testTask     = TestValues.testLongAnswerTaskA
        val testWork     = TestValues.testLongAnswerWorkRevisionA
        val testDocument = TestValues.testDocumentRevisionA

        (documentRepository.find(_: UUID, _: Long)(_: Connection)) when(testDocument.id, *, *) returns(Future.successful(\/-(testDocument)))

        val result = workRepository.find(testUser, testTask, testWork.version)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: DocumentWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "find a specific revision for a single work for a user within a Task (ShortAnswerWork)" in {
        val testUser     = TestValues.testUserC
        val testTask     = TestValues.testShortAnswerTaskB
        val testWork     = TestValues.testShortAnswerWorkRevisionG
        val testDocument = TestValues.testDocumentRevisionD

        (documentRepository.find(_: UUID, _: Long)(_: Connection)) when(testDocument.id, *, *) returns(Future.successful(\/-(testDocument)))

        val result = workRepository.find(testUser, testTask, testWork.version)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: DocumentWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "find a specific revision for a single work for a user within a Task (MultipleChoiceWork)" in {
        val testUser     = TestValues.testUserC
        val testTask     = TestValues.testMultipleChoiceTaskC
        val testWork     = TestValues.testMultipleChoiceWorkRevisionC

        val result = workRepository.find(testUser, testTask, testWork.version)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: MultipleChoiceWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "find a specific revision for a single work for a user within a Task (OrderingWork)" in {
        val testUser     = TestValues.testUserC
        val testTask     = TestValues.testOrderingTaskN
        val testWork     = TestValues.testOrderingWorkRevisionD

        val result = workRepository.find(testUser, testTask, testWork.version)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: OrderingWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "find a specific revision for a single work for a user within a Task (MatchingWork)" in {
        val testUser     = TestValues.testUserC
        val testTask     = TestValues.testMatchingTaskE
        val testWork     = TestValues.testMatchingWorkRevisionE

        val result = workRepository.find(testUser, testTask, testWork.version)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: MatchingWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "return RepositoryError.NoResults if user doesn't exist (with version)" in {
        val testUser     = TestValues.testUserD
        val testTask     = TestValues.testMatchingTaskE
        val testWork     = TestValues.testMatchingWorkRevisionE

        val result = workRepository.find(testUser, testTask, testWork.version)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
      }
      "return RepositoryError.NoResults if task doesn't exist (with version)" in {
        val testUser     = TestValues.testUserC
        val testTask     = TestValues.testMatchingTaskJ
        val testWork     = TestValues.testMatchingWorkRevisionE

        val result = workRepository.find(testUser, testTask, testWork.version)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
      }
      "return RepositoryError.NoResults if user is not connected with a task (with version)" in {
        val testUser     = TestValues.testUserG
        val testTask     = TestValues.testMatchingTaskE
        val testWork     = TestValues.testMatchingWorkRevisionE

        val result = workRepository.find(testUser, testTask, testWork.version)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
      }
      "return RepositoryError.NoResults if user doesn't have work within a task (with version)" in {
        val testUser     = TestValues.testUserE
        val testTask     = TestValues.testMatchingTaskM
        val testWork     = TestValues.testMatchingWorkRevisionE

        val result = workRepository.find(testUser, testTask, testWork.version)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
      }
      "return RepositoryError.NoResults if version is wrong" in {
        val testUser     = TestValues.testUserC
        val testTask     = TestValues.testMatchingTaskE
        val testWork     = TestValues.testMatchingWorkRevisionE

        val result = workRepository.find(testUser, testTask, testWork.version + 99)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
      }
    }
  }

  "WorkRepository.insert" should {
    inSequence {
      "insert new LongAnswerWork" in {
        val testWork = TestValues.testLongAnswerWorkK

        val result = workRepository.insert(testWork)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: LongAnswerWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(1L)
        work.response should be(None)
        work.isComplete should be(testWork.isComplete)
      }
      "insert new ShortAnswerWork" in {
        val testWork = TestValues.testShortAnswerWorkL

        val result = workRepository.insert(testWork)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: ShortAnswerWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(1L)
        work.response should be(None)
        work.isComplete should be(testWork.isComplete)
      }
      "insert new MultipleChoiceWork" in {
        val testWork = TestValues.testMultipleChoiceWorkM.copy(
          response = IndexedSeq.empty[Int]
        )

        val result = workRepository.insert(testWork)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: MultipleChoiceWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(1L)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
      }
      "insert new OrderingWork" in {
        val testWork = TestValues.testOrderingWorkN.copy(
          response = IndexedSeq.empty[Int]
        )

        val result = workRepository.insert(testWork)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: OrderingWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(1L)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
      }
      "insert new MatchingWork" in {
        val testWork = TestValues.testMatchingWorkO.copy(
          response = IndexedSeq.empty[Match]
        )

        val result = workRepository.insert(testWork)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: MatchingWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(1L)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
      }
      "return RepositoryError.PrimaryKeyConflict if this work allready exists (LongAnswerWork)" in {
        val testWork = TestValues.testLongAnswerWorkA.copy(
          studentId = TestValues.testUserG.id
        )

        val result = workRepository.insert(testWork)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.PrimaryKeyConflict))
      }
    }
  }

  "WorkRepository.update" should {
    inSequence {
      /* Only thing we should be able to update is isComplete */
      "update LongAnswerWork (newRevision = FALSE)" in {
        val testWork    = TestValues.testLongAnswerWorkF
        // Work for new values
        val oppositeWork  = TestValues.testLongAnswerWorkA
        val testDocument = testWork.response.get
        val updatedWork = testWork.copy(
          // Shouldn't be updated
          studentId  = oppositeWork.studentId,
          taskId     = oppositeWork.taskId,
          documentId = oppositeWork.documentId,
          response   = oppositeWork.response,
          createdAt  = oppositeWork.createdAt,

          // Should be updated
          isComplete = !testWork.isComplete
        )

        (documentRepository.find(_: UUID, _: Long)(_: Connection)) when(testDocument.id, *, *) returns(Future.successful(\/-(testDocument)))

        val result = workRepository.update(updatedWork)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: LongAnswerWork) = eitherWork

        // This values should remain unchanged
        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.response should be(testWork.response)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.version should be(testWork.version)

        // This should be updated
        work.isComplete should be(updatedWork.isComplete)
        work.updatedAt.toString should not be(testWork.updatedAt.toString)
      }
      "update ShortAnswerWork (newRevision = FALSE)" in {
        val testWork    = TestValues.testShortAnswerWorkB
        // Work for new values
        val oppositeWork  = TestValues.testLongAnswerWorkA
        val testDocument = testWork.response.get
        val updatedWork = testWork.copy(
          // Shouldn't be updated
          studentId  = oppositeWork.studentId,
          taskId     = oppositeWork.taskId,
          documentId = oppositeWork.documentId,
          response   = oppositeWork.response,
          createdAt  = oppositeWork.createdAt,

          // Should be updated
          isComplete = !testWork.isComplete
        )

        (documentRepository.find(_: UUID, _: Long)(_: Connection)) when(testDocument.id, *, *) returns(Future.successful(\/-(testDocument)))

        val result = workRepository.update(updatedWork)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: ShortAnswerWork) = eitherWork

        // This values should remain unchanged
        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.response should be(testWork.response)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.version should be(testWork.version)

        // This should be updated
        work.isComplete should be(updatedWork.isComplete)
        work.updatedAt.toString should not be(testWork.updatedAt.toString)
      }
      "update MultipleChoiceWork (newRevision = FALSE)" in {
        val testWork    = TestValues.testMultipleChoiceWorkC
        // Work for new values
        val oppositeWork  = TestValues.testMultipleChoiceWorkH
        val updatedWork = testWork.copy(
          // Shouldn't be updated
          studentId  = oppositeWork.studentId,
          taskId     = oppositeWork.taskId,
          response   = oppositeWork.response,
          createdAt  = oppositeWork.createdAt,

          // Should be updated
          isComplete = !testWork.isComplete
        )

        val result = workRepository.update(updatedWork)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: MultipleChoiceWork) = eitherWork

        // This values should remain unchanged
        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.response should be(testWork.response)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.version should be(testWork.version)

        // This should be updated
        work.isComplete should be(updatedWork.isComplete)
        work.updatedAt.toString should not be(testWork.updatedAt.toString)
      }
      "update OrderingWork (newRevision = FALSE)" in {
        val testWork    = TestValues.testOrderingWorkI
        // Work for new values
        val oppositeWork  = TestValues.testOrderingWorkD
        val updatedWork = testWork.copy(
          // Shouldn't be updated
          studentId  = oppositeWork.studentId,
          taskId     = oppositeWork.taskId,
          response   = oppositeWork.response,
          createdAt  = oppositeWork.createdAt,

          // Should be updated
          isComplete = !testWork.isComplete
        )

        val result = workRepository.update(updatedWork)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: OrderingWork) = eitherWork

        // This values should remain unchanged
        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.response should be(testWork.response)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.version should be(testWork.version)

        // This should be updated
        work.isComplete should be(updatedWork.isComplete)
        work.updatedAt.toString should not be(testWork.updatedAt.toString)
      }
      "update MatchingWork (newRevision = FALSE)" in {
        val testWork = TestValues.testMatchingWorkJ
        // Work for new values
        val oppositeWork = TestValues.testMatchingWorkE
        val updatedWork = testWork.copy(
          // Shouldn't be updated
          studentId  = oppositeWork.studentId,
          taskId     = oppositeWork.taskId,
          response   = oppositeWork.response,
          createdAt  = oppositeWork.createdAt,

          // Should be updated
          isComplete = !testWork.isComplete
        )

        val result = workRepository.update(updatedWork)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: MatchingWork) = eitherWork

        // This values should remain unchanged
        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.response should be(testWork.response)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.version should be(testWork.version)

        // This should be updated
        work.isComplete should be(updatedWork.isComplete)
        work.updatedAt.toString should not be(testWork.updatedAt.toString)
      }
      "return RepositoryError.NoResults if version is wrong (newRevision = FALSE)" in {
        val testWork = TestValues.testMatchingWorkJ
        // Work for new values
        val oppositeWork = TestValues.testMatchingWorkE
        val updatedWork = testWork.copy(
          version = 99L,

          // Shouldn't be updated
          studentId  = oppositeWork.studentId,
          taskId     = oppositeWork.taskId,
          response   = oppositeWork.response,
          createdAt  = oppositeWork.createdAt,

          // Should be updated
          isComplete = !testWork.isComplete
        )

        val result = workRepository.update(updatedWork)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
      }
      "return RepositoryError.NoResults if work doesn't exist (newRevision = FALSE)" in {
        val testWork = TestValues.testMatchingWorkO
        // Work for new values
        val oppositeWork = TestValues.testMatchingWorkE
        val updatedWork = testWork.copy(
          // Shouldn't be updated
          studentId  = oppositeWork.studentId,
          taskId     = oppositeWork.taskId,
          response   = oppositeWork.response,
          createdAt  = oppositeWork.createdAt,

          // Should be updated
          isComplete = !testWork.isComplete
        )

        val result = workRepository.update(updatedWork)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
      }
      "return RepositoryError.NoResults if try to update work previous revision (newRevision = FALSE)" in {
        val testWork = TestValues.testMatchingWorkRevisionJ
        // Work for new values
        val oppositeWork = TestValues.testMatchingWorkE
        val updatedWork = testWork.copy(
          // Shouldn't be updated
          studentId  = oppositeWork.studentId,
          taskId     = oppositeWork.taskId,
          response   = oppositeWork.response,
          createdAt  = oppositeWork.createdAt,

          // Should be updated
          isComplete = !testWork.isComplete
        )

        val result = workRepository.update(updatedWork)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
      }

      /* Update aka create new revision */
      "return RepositoryError.BadParam when try to update LongAnswerWork with (newRevision = TRUE)" in {
        val testWork    = TestValues.testLongAnswerWorkF

        val result = workRepository.update(testWork, true)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.BadParam("Adding new Revisions to a DocumentWork should be done in the Document Repository")))
      }

      "return RepositoryError.BadParam when try to update ShortAnswerWork with (newRevision = TRUE)" in {
        val testWork    = TestValues.testShortAnswerWorkB

        val result = workRepository.update(testWork, true)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.BadParam("Adding new Revisions to a DocumentWork should be done in the Document Repository")))
      }
      "update MultipleChoiceWork (newRevision = TRUE)" in {
        val testWork    = TestValues.testMultipleChoiceWorkC
        // Work for new values
        val oppositeWork  = TestValues.testMultipleChoiceWorkH
        val updatedWork = testWork.copy(
          // Shouldn't be updated
          studentId  = oppositeWork.studentId,
          taskId     = oppositeWork.taskId,
          response   = oppositeWork.response,
          createdAt  = oppositeWork.createdAt,

          // Should be updated
          isComplete = !testWork.isComplete
        )

        val result = workRepository.update(updatedWork, true)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: MultipleChoiceWork) = eitherWork

        // This values should remain unchanged
        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.createdAt.toString should be(testWork.createdAt.toString)

        // This should be updated
        work.response should be(updatedWork.response)
        work.version should be(updatedWork.version + 1)
        work.isComplete should be(updatedWork.isComplete)
        work.updatedAt.toString should not be(testWork.updatedAt.toString)
      }
      "update OrderingWork (newRevision = TRUE)" in {
        val testWork    = TestValues.testOrderingWorkI
        // Work for new values
        val oppositeWork  = TestValues.testOrderingWorkD
        val updatedWork = testWork.copy(
          // Shouldn't be updated
          studentId  = oppositeWork.studentId,
          taskId     = oppositeWork.taskId,
          response   = oppositeWork.response,
          createdAt  = oppositeWork.createdAt,

          // Should be updated
          isComplete = !testWork.isComplete
        )

        val result = workRepository.update(updatedWork, true)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: OrderingWork) = eitherWork

        // This values should remain unchanged
        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.createdAt.toString should be(testWork.createdAt.toString)

        // This should be updated
        work.response should be(updatedWork.response)
        work.version should be(updatedWork.version + 1)
        work.isComplete should be(updatedWork.isComplete)
        work.updatedAt.toString should not be(testWork.updatedAt.toString)
      }
      "update MatchingWork (newRevision = TRUE)" in {
        val testWork = TestValues.testMatchingWorkJ
        // Work for new values
        val oppositeWork = TestValues.testMatchingWorkE
        val updatedWork = testWork.copy(
          // Shouldn't be updated
          studentId  = oppositeWork.studentId,
          taskId     = oppositeWork.taskId,
          response   = oppositeWork.response,
          createdAt  = oppositeWork.createdAt,

          // Should be updated
          isComplete = !testWork.isComplete
        )

        val result = workRepository.update(updatedWork, true)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: MatchingWork) = eitherWork

        // This values should remain unchanged
        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.createdAt.toString should be(testWork.createdAt.toString)

        // This should be updated
        work.response should be(updatedWork.response)
        work.version should be(updatedWork.version + 1)
        work.isComplete should be(updatedWork.isComplete)
        work.updatedAt.toString should not be(testWork.updatedAt.toString)
      }
      "return RepositoryError.NoResults if version is wrong (newRevision = TRUE)" in {
        val testWork = TestValues.testMatchingWorkJ
        // Work for new values
        val oppositeWork = TestValues.testMatchingWorkE
        val updatedWork = testWork.copy(
          version = 99L,

          // Shouldn't be updated
          studentId  = oppositeWork.studentId,
          taskId     = oppositeWork.taskId,
          response   = oppositeWork.response,
          createdAt  = oppositeWork.createdAt,

          // Should be updated
          isComplete = !testWork.isComplete
        )

        val result = workRepository.update(updatedWork, true)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
      }
      "return RepositoryError.NoResults if work doesn't exist (newRevision = TRUE)" in {
        val testWork = TestValues.testMatchingWorkO
        // Work for new values
        val oppositeWork = TestValues.testMatchingWorkE
        val updatedWork = testWork.copy(
          // Shouldn't be updated
          studentId  = oppositeWork.studentId,
          taskId     = oppositeWork.taskId,
          response   = oppositeWork.response,
          createdAt  = oppositeWork.createdAt,

          // Should be updated
          isComplete = !testWork.isComplete
        )

        val result = workRepository.update(updatedWork, true)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
      }
      "return RepositoryError.NoResults if try to update work previous revision (newRevision = TRUE)" in {
        val testWork = TestValues.testMatchingWorkRevisionJ
        // Work for new values
        val oppositeWork = TestValues.testMatchingWorkE
        val updatedWork = testWork.copy(
          // Shouldn't be updated
          studentId  = oppositeWork.studentId,
          taskId     = oppositeWork.taskId,
          response   = oppositeWork.response,
          createdAt  = oppositeWork.createdAt,

          // Should be updated
          isComplete = !testWork.isComplete
        )

        val result = workRepository.update(updatedWork, true)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
      }
    }
  }

  "WorkRepository.delete" should {
    inSequence {
      /* --- delete(Work) --- */
      "delete all revisions of a work (LongAnswerWork)" in {
        val testWork = TestValues.testLongAnswerWorkF
        val testDocument = TestValues.testDocumentB

        (documentRepository.find(_: UUID, _: Long)(_: Connection)) when(testDocument.id, *, *) returns(Future.successful(\/-(testDocument)))

        val result = workRepository.delete(testWork)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: LongAnswerWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "delete all revisions of a work (ShortAnswerWork)" in {
        val testWork = TestValues.testShortAnswerWorkG
        val testDocument = TestValues.testDocumentD

        (documentRepository.find(_: UUID, _: Long)(_: Connection)) when(testDocument.id, *, *) returns(Future.successful(\/-(testDocument)))

        val result = workRepository.delete(testWork)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: ShortAnswerWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "delete all revisions of a work (MultipleChoiceWork)" in {
        val testWork = TestValues.testMultipleChoiceWorkC

        val result = workRepository.delete(testWork)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: MultipleChoiceWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "delete all revisions of a work (OrderingWork)" in {
        val testWork = TestValues.testOrderingWorkD

        val result = workRepository.delete(testWork)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: OrderingWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "delete all revisions of a work (MatchingWork)" in {
        val testWork = TestValues.testMatchingWorkJ

        val result = workRepository.delete(testWork)
        val eitherWork = Await.result(result, Duration.Inf)
        val \/-(work: MatchingWork) = eitherWork

        work.id should be(testWork.id)
        work.studentId should be(testWork.studentId)
        work.taskId should be(testWork.taskId)
        work.version should be(testWork.version)
        work.response should be(testWork.response)
        work.isComplete should be(testWork.isComplete)
        work.createdAt.toString should be(testWork.createdAt.toString)
        work.updatedAt.toString should be(testWork.updatedAt.toString)
      }
      "return RepositoryError.NoResults if work doesn't exist" in {
        val testWork = TestValues.testMatchingWorkO

        val result = workRepository.delete(testWork)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
      }

      /* --- delete(Task) --- */
      "delete all work for a given task (LongAnswerWork)" in {
        val testTask = TestValues.testLongAnswerTaskA

        val testWorkList = TreeMap[Int, Work](
          0 -> TestValues.testLongAnswerWorkA,
          1 -> TestValues.testLongAnswerWorkF
        )

        val testDocumentList = TreeMap[Int, Document](
          0 -> TestValues.testDocumentA,
          1 -> TestValues.testDocumentB
        )

        testDocumentList.foreach {
          case (key, document: Document) => {
            (documentRepository.find(_: UUID, _: Long)(_: Connection)) when(document.id, *, *) returns(Future.successful(\/-(document)))
          }
        }

        val result = workRepository.delete(testTask)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works: IndexedSeq[DocumentWork]) = eitherWorks

        works.size should be(testWorkList.size)

        testWorkList.foreach {
          case (key, work: DocumentWork) => {
            works(key).id should be(work.id)
            works(key).studentId should be(work.studentId)
            works(key).taskId should be(work.taskId)
            works(key).version should be(work.version)
            works(key).response should be(work.response)
            works(key).isComplete should be(work.isComplete)
            works(key).createdAt.toString should be(work.createdAt.toString)
            works(key).updatedAt.toString should be(work.updatedAt.toString)
          }
        }
      }
      "delete all work for a given task (ShortAnswerWork)" in {
        val testTask = TestValues.testShortAnswerTaskB

        val testWorkList = TreeMap[Int, Work](
          0 -> TestValues.testShortAnswerWorkB,
          1 -> TestValues.testShortAnswerWorkG
        )

        val testDocumentList = TreeMap[Int, Document](
          0 -> TestValues.testDocumentD,
          1 -> TestValues.testDocumentC
        )

        testDocumentList.foreach {
          case (key, document: Document) => {
            (documentRepository.find(_: UUID, _: Long)(_: Connection)) when(document.id, *, *) returns(Future.successful(\/-(document)))
          }
        }

        val result = workRepository.delete(testTask)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works: IndexedSeq[DocumentWork]) = eitherWorks

        works.size should be(testWorkList.size)

        testWorkList.foreach {
          case (key, work: DocumentWork) => {
            works(key).id should be(work.id)
            works(key).studentId should be(work.studentId)
            works(key).taskId should be(work.taskId)
            works(key).version should be(work.version)
            works(key).response should be(work.response)
            works(key).isComplete should be(work.isComplete)
            works(key).createdAt.toString should be(work.createdAt.toString)
            works(key).updatedAt.toString should be(work.updatedAt.toString)
          }
        }
      }
      "delete all work for a given task (MultipleChoiceWork)" in {
        val testTask = TestValues.testMultipleChoiceTaskC

        val testWorkList = TreeMap[Int, Work](
          0 -> TestValues.testMultipleChoiceWorkC,
          1 -> TestValues.testMultipleChoiceWorkH
        )

        val result = workRepository.delete(testTask)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works: IndexedSeq[MultipleChoiceWork]) = eitherWorks

        works.size should be(testWorkList.size)

        testWorkList.foreach {
          case (key, work: MultipleChoiceWork) => {
            works(key).id should be(work.id)
            works(key).studentId should be(work.studentId)
            works(key).taskId should be(work.taskId)
            works(key).version should be(work.version)
            works(key).response should be(work.response)
            works(key).isComplete should be(work.isComplete)
            works(key).createdAt.toString should be(work.createdAt.toString)
            works(key).updatedAt.toString should be(work.updatedAt.toString)
          }
        }
      }
      "delete all work for a given task (OrderingWork)" in {
        val testTask = TestValues.testOrderingTaskN

        val testWorkList = TreeMap[Int, Work](
          0 -> TestValues.testOrderingWorkI,
          1 -> TestValues.testOrderingWorkD
        )

        val result = workRepository.delete(testTask)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works: IndexedSeq[OrderingWork]) = eitherWorks

        works.size should be(testWorkList.size)

        testWorkList.foreach {
          case (key, work: OrderingWork) => {
            works(key).id should be(work.id)
            works(key).studentId should be(work.studentId)
            works(key).taskId should be(work.taskId)
            works(key).version should be(work.version)
            works(key).response should be(work.response)
            works(key).isComplete should be(work.isComplete)
            works(key).createdAt.toString should be(work.createdAt.toString)
            works(key).updatedAt.toString should be(work.updatedAt.toString)
          }
        }
      }
      "delete all work for a given task (MatchingWork)" in {
        val testTask = TestValues.testMatchingTaskE

        val testWorkList = TreeMap[Int, Work](
          0 -> TestValues.testMatchingWorkJ,
          1 -> TestValues.testMatchingWorkE
        )

        val result = workRepository.delete(testTask)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works: IndexedSeq[MatchingWork]) = eitherWorks

        works.size should be(testWorkList.size)

        testWorkList.foreach {
          case (key, work: MatchingWork) => {
            works(key).id should be(work.id)
            works(key).studentId should be(work.studentId)
            works(key).taskId should be(work.taskId)
            works(key).version should be(work.version)
            works(key).response should be(work.response)
            works(key).isComplete should be(work.isComplete)
            works(key).createdAt.toString should be(work.createdAt.toString)
            works(key).updatedAt.toString should be(work.updatedAt.toString)
          }
        }
      }
      "return empty Vector() if task doesn't exist" in {
        val testTask = TestValues.testMatchingTaskJ

        val result = workRepository.delete(testTask)
        Await.result(result, Duration.Inf) should be(\/-(Vector()))
      }
    }
  }
}
