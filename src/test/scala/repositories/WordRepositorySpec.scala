import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.work.Work
import ca.shiftfocus.krispii.core.repositories._
import com.github.mauricio.async.db.Connection
import org.scalatest.Matchers._
import org.scalatest._

import scala.collection.immutable.TreeMap
import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }
import scala.util.Right
import scalaz._
import ca.shiftfocus.krispii.core.models._

/**
 * Created by vzaytseva on 23/02/16.
 */
class WordRepositorySpec extends TestEnvironment {
  val wordRepository = new WordRepositoryPostgres
  "WordRepository.get" should {
    inSequence {
      "Return random string by language" in {
        val testWorkList = IndexedSeq[LinkWord](
          TestValues.testWordB,
          TestValues.testWordE
        )

        val result = wordRepository.get("fr")
        val eitherWord = Await.result(result, Duration.Inf)
        val \/-(wordRandom) = eitherWord
        val contain = testWorkList.contains(wordRandom)

        contain should be(true)
      }
    }
  }
}
