package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.error.ErrorUnion
import ca.shiftfocus.krispii.core.models.JournalEntry._
import ca.shiftfocus.krispii.core.models.JournalEntry
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.krispii.core.services.datasource.DB
import java.util.UUID

import ca.shiftfocus.krispii.core.models.user.User
import com.github.mauricio.async.db.Connection
import org.joda.time.DateTime

import scala.concurrent.Future
import scalaz._

class JournalServiceDefault(
  val config: Boolean,
  val db: DB,
  val authService: AuthService,
  val journalRepository: JournalRepository,
  val userRepository: UserRepository,
  val projectRepository: ProjectRepository
)
    extends JournalService {

  implicit def conn: Connection = db.pool

  def list(entryType: String): Future[\/[ErrorUnion#Fail, IndexedSeq[JournalEntry]]] = {
    journalRepository.list(entryType)
  }

  def list(userId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[JournalEntry]]] = {
    val fUser = authService.find(userId)

    for {
      user <- lift(fUser)
      journalEntryList <- lift(journalRepository.list(user))
    } yield journalEntryList
  }

  def list(startDate: Option[DateTime], endDate: Option[DateTime]): Future[\/[ErrorUnion#Fail, IndexedSeq[JournalEntry]]] = {
    journalRepository.list(startDate, endDate)
  }

  def find(id: UUID): Future[\/[ErrorUnion#Fail, JournalEntry]] = {
    journalRepository.find(id)
  }

  /**
   * Create a Journal Entry
   *
   * @param userId User that made an action
   * @param projectId Project where action was made
   * @param action Action that was made. Is declared in JournalEntry object.
   * @param item What object was affected. ex: a task, a button, some video etc.
   * @return
   */
  private def create(userId: UUID, projectId: UUID, action: Action, item: String): Future[\/[ErrorUnion#Fail, JournalEntry]] = {
    transactional { implicit conn =>
      journalRepository.insert(JournalEntry(
        userId = userId,
        projectId = projectId,
        entryType = action.entryType,
        item = item
      ))
    }
  }

  def delete(journalEntry: JournalEntry): Future[\/[ErrorUnion#Fail, JournalEntry]] = {
    transactional { implicit conn =>
      journalRepository.delete(journalEntry)
    }
  }

  def delete(entryType: String): Future[\/[ErrorUnion#Fail, IndexedSeq[JournalEntry]]] = {
    transactional { implicit conn =>
      journalRepository.delete(entryType)
    }
  }

  def delete(user: User): Future[\/[ErrorUnion#Fail, IndexedSeq[JournalEntry]]] = {
    transactional { implicit conn =>
      journalRepository.delete(user)
    }
  }

  // --- LOG METHODS --------------------------------------------------------------------------------------------------

  /**
   * Log user Connected to a project action
   *
   * @param userId User that made an action
   * @param projectId Project where action was made
   * @param item What object was affected. ex: a task, a button, some video etc.
   * @return
   */
  def logConnect(userId: UUID, projectId: UUID, item: String): Future[\/[ErrorUnion#Fail, Unit]] = {
    if (config) lift(create(userId, projectId, JournalEntryConnect, item)).map { journalEntry => () }
    else Future.successful(\/.right(()))
  }

  /**
   * Log user Disconnected from a project action
   *
   * @param userId User that made an action
   * @param projectId Project where action was made
   * @param item What object was affected. ex: a task, a button, some video etc.
   * @return
   */
  def logDisconnect(userId: UUID, projectId: UUID, item: String): Future[\/[ErrorUnion#Fail, Unit]] = {
    if (config) lift(create(userId, projectId, JournalEntryDisconnect, item)).map { journalEntry => () }
    else Future.successful(\/.right(()))
  }

  /**
   * Log View action
   *
   * @param userId User that made an action
   * @param projectId Project where action was made
   * @param item What object was affected. ex: a task, a button, some video etc.
   * @return
   */
  def logView(userId: UUID, projectId: UUID, item: String): Future[\/[ErrorUnion#Fail, Unit]] = {
    if (config) lift(create(userId, projectId, JournalEntryView, item)).map { journalEntry => () }
    else Future.successful(\/.right(()))
  }

  /**
   * Log Click action
   *
   * @param userId User that made an action
   * @param projectId Project where action was made
   * @param item What object was affected. ex: a task, a button, some video etc.
   * @return
   */
  def logClick(userId: UUID, projectId: UUID, item: String): Future[\/[ErrorUnion#Fail, Unit]] = {
    if (config) lift(create(userId, projectId, JournalEntryClick, item)).map { journalEntry => () }
    else Future.successful(\/.right(()))
  }

  /**
   * Log Watch action
   *
   * @param userId User that made an action
   * @param projectId Project where action was made
   * @param item What object was affected. ex: a task, a button, some video etc.
   * @return
   */
  def logWatch(userId: UUID, projectId: UUID, item: String): Future[\/[ErrorUnion#Fail, Unit]] = {
    if (config) lift(create(userId, projectId, JournalEntryWatch, item)).map { journalEntry => () }
    else Future.successful(\/.right(()))
  }

  /**
   * Log Listen action
   *
   * @param userId User that made an action
   * @param projectId Project where action was made
   * @param item What object was affected. ex: a task, a button, some video etc.
   * @return
   */
  def logListen(userId: UUID, projectId: UUID, item: String): Future[\/[ErrorUnion#Fail, Unit]] = {
    if (config) lift(create(userId, projectId, JournalEntryListen, item)).map { journalEntry => () }
    else Future.successful(\/.right(()))
  }

  /**
   * Log Input action
   *
   * @param userId User that made an action
   * @param projectId Project where action was made
   * @param item What object was affected. ex: a task, a button, some video etc.
   * @return
   */
  def logInput(userId: UUID, projectId: UUID, item: String): Future[\/[ErrorUnion#Fail, Unit]] = {
    if (config) lift(create(userId, projectId, JournalEntryWrite, item)).map { journalEntry => () }
    else Future.successful(\/.right(()))
  }

  /**
   * Log Update action
   *
   * @param userId User that made an action
   * @param projectId Project where action was made
   * @param item What object was affected. ex: a task, a button, some video etc.
   * @return
   */
  def logUpdate(userId: UUID, projectId: UUID, item: String): Future[\/[ErrorUnion#Fail, Unit]] = {
    if (config) lift(create(userId, projectId, JournalEntryUpdate, item)).map { journalEntry => () }
    else Future.successful(\/.right(()))
  }

  /**
   * Log Create action
   *
   * @param userId User that made an action
   * @param projectId Project where action was made
   * @param item What object was affected. ex: a task, a button, some video etc.
   * @return
   */
  def logCreate(userId: UUID, projectId: UUID, item: String): Future[\/[ErrorUnion#Fail, Unit]] = {
    if (config) lift(create(userId, projectId, JournalEntryCreate, item)).map { journalEntry => () }
    else Future.successful(\/.right(()))
  }

  /**
   * Log Delete action
   *
   * @param userId User that made an action
   * @param projectId Project where action was made
   * @param item What object was affected. ex: a task, a button, some video etc.
   * @return
   */
  def logDelete(userId: UUID, projectId: UUID, item: String): Future[\/[ErrorUnion#Fail, Unit]] = {
    if (config) lift(create(userId, projectId, JournalEntryDelete, item)).map { journalEntry => () }
    else Future.successful(\/.right(()))
  }
}
