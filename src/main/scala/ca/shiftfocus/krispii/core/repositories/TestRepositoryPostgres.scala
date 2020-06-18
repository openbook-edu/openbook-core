package ca.shiftfocus.krispii.core.repositories

import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.Team
import ca.shiftfocus.krispii.core.models.work.{MediaAnswer, Test}
import com.github.mauricio.async.db.{Connection, RowData}
import org.joda.time.DateTime
import scalaz.{\/, \/-}

import scala.concurrent.Future

class TestRepositoryPostgres extends TestRepository with PostgresRepository[Test] {

  override val entityName: String = "test"

  override def constructor(row: RowData): Test =
    Test(
      row("id").asInstanceOf[UUID],
      row("examId").asInstanceOf[UUID],
      row("teamId").asInstanceOf[UUID],
      row("name").asInstanceOf[String],
      row("version").asInstanceOf[Long],
      row("grade").asInstanceOf[String],
      row("orig_response").asInstanceOf[MediaAnswer],
      None, // scorers
      None, // tests
      row("created_at").asInstanceOf[DateTime],
      row("updated_at").asInstanceOf[DateTime]
    )

  override def list(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Test]]] =
    Future successful \/-(IndexedSeq.empty[Test])
  override def list(team: Team)(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[Test]]] =
    Future successful \/-(IndexedSeq.empty[Test])

}
