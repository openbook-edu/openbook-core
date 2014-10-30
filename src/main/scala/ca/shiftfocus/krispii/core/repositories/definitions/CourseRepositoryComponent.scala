package ca.shiftfocus.krispii.core.repositories

import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import ca.shiftfocus.krispii.core.lib._
import ca.shiftfocus.krispii.core.models._
import scala.concurrent.Future

trait CourseRepositoryComponent {
  val courseRepository: CourseRepository

  trait CourseRepository {
    def list: Future[IndexedSeq[Course]]
    def find(id: UUID): Future[Option[Course]]
    def insert(task: Course)(implicit conn: Connection): Future[Course]
    def update(task: Course)(implicit conn: Connection): Future[Course]
    def delete(task: Course)(implicit conn: Connection): Future[Boolean]
  }
}
