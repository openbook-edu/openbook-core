package ca.shiftfocus.krispii.core.services

import ca.shiftfocus.krispii.core.error.ErrorUnion
import ca.shiftfocus.krispii.core.models.JournalEntry._
import ca.shiftfocus.krispii.core.models.{User, JournalEntry}
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.krispii.core.services.datasource.DB
import ca.shiftfocus.uuid.UUID
import com.github.mauricio.async.db.Connection
import org.joda.time.DateTime

import scala.concurrent.Future
import scalaz._

class JournalServiceDefault (val config: Boolean,
                             val db: DB,
                             val journalRepository: JournalRepository,
                             val userRepository: UserRepository,
                             val projectRepository: ProjectRepository)
  extends JournalService {

  implicit def conn: Connection = db.pool
  
  def list(entryType: String)(implicit conn: Connection): Future[\/[ErrorUnion#Fail, IndexedSeq[JournalEntry]]] = {
    journalRepository.list(entryType)
  }
  
  def list(userId: UUID)(implicit conn: Connection): Future[\/[ErrorUnion#Fail, IndexedSeq[JournalEntry]]] = {
    journalRepository.list(userId)
  }
  
  def list(startDate: Option[DateTime], endDate: Option[DateTime])(implicit conn: Connection): Future[\/[ErrorUnion#Fail, IndexedSeq[JournalEntry]]] = {
    journalRepository.list(startDate, endDate)
  }

  def find(id: UUID)(implicit conn: Connection): Future[\/[ErrorUnion#Fail, JournalEntry]] = {
    journalRepository.find(id)
  }

  /**
   * Create a Journal Entry
   *
   * @param userId User that made an action
   * @param projectId Project where action was made
   * @param action Action that was made. Is declared in JournalEntry object.
   * @param item What object was affected. ex: a task, a button, some video etc.
   * @param conn
   * @return
   */
  private def create(userId: UUID, projectId: UUID, action: Action, item: String)(implicit conn: Connection): Future[\/[ErrorUnion#Fail, JournalEntry]] = {
    journalRepository.insert(JournalEntry(
      userId    = userId,
      projectId = projectId,
      entryType = action.entryType,
      item      = item
    ))
  }

  def delete(journalEntry: JournalEntry)(implicit conn: Connection): Future[\/[ErrorUnion#Fail, JournalEntry]] = {
    journalRepository.delete(journalEntry)
  }
  
  def delete(entryType: String)(implicit conn: Connection): Future[\/[ErrorUnion#Fail, IndexedSeq[JournalEntry]]] = {
    journalRepository.delete(entryType)
  }
  
  def delete(user: User)(implicit conn: Connection): Future[\/[ErrorUnion#Fail, IndexedSeq[JournalEntry]]] = {
    journalRepository.delete(user)
  }

  // --- LOG METHODS --------------------------------------------------------------------------------------------------

  /**
   * Log View action
   *
   * @param userId User that made an action
   * @param projectId Project where action was made
   * @param item What object was affected. ex: a task, a button, some video etc.
   * @param conn
   * @return
   */
  def logView(userId: UUID, projectId: UUID, item: String)(implicit conn: Connection): Future[\/[ErrorUnion#Fail, Unit]] = {
    if (config) lift(create(userId, projectId, JournalEntryView, item)).map { journalEntry => () }
    else Future.successful(\/.right( () ))
  }

  /**
   * Log Click action
   *
   * @param userId User that made an action
   * @param projectId Project where action was made
   * @param item What object was affected. ex: a task, a button, some video etc.
   * @param conn
   * @return
   */
  def logClick(userId: UUID, projectId: UUID, item: String)(implicit conn: Connection): Future[\/[ErrorUnion#Fail, Unit]] = {
    if (config) lift(create(userId, projectId, JournalEntryClick, item)).map { journalEntry => () }
    else Future.successful(\/.right( () ))
  }

  /**
   * Log Watch action
   *
   * @param userId User that made an action
   * @param projectId Project where action was made
   * @param item What object was affected. ex: a task, a button, some video etc.
   * @param conn
   * @return
   */
  def logWatch(userId: UUID, projectId: UUID, item: String)(implicit conn: Connection): Future[\/[ErrorUnion#Fail, Unit]] = {
    if (config) lift(create(userId, projectId, JournalEntryWatch, item)).map { journalEntry => () }
    else Future.successful(\/.right( () ))
  }

  /**
   * Log Listen action
   *
   * @param userId User that made an action
   * @param projectId Project where action was made
   * @param item What object was affected. ex: a task, a button, some video etc.
   * @param conn
   * @return
   */
  def logListen(userId: UUID, projectId: UUID, item: String)(implicit conn: Connection): Future[\/[ErrorUnion#Fail, Unit]] = {
    if (config) lift(create(userId, projectId, JournalEntryListen, item)).map { journalEntry => () }
    else Future.successful(\/.right( () ))
  }

  /**
   * Log Input action
   *
   * @param userId User that made an action
   * @param projectId Project where action was made
   * @param item What object was affected. ex: a task, a button, some video etc.
   * @param conn
   * @return
   */
  def logInput(userId: UUID, projectId: UUID, item: String)(implicit conn: Connection): Future[\/[ErrorUnion#Fail, Unit]] = {
    if (config) lift(create(userId, projectId, JournalEntryWrite, item)).map { journalEntry => () }
    else Future.successful(\/.right( () ))
  }

  /**
   * Log Update action
   *
   * @param userId User that made an action
   * @param projectId Project where action was made
   * @param item What object was affected. ex: a task, a button, some video etc.
   * @param conn
   * @return
   */
  def logUpdate(userId: UUID, projectId: UUID, item: String)(implicit conn: Connection): Future[\/[ErrorUnion#Fail, Unit]] = {
    if (config) lift(create(userId, projectId, JournalEntryUpdate, item)).map { journalEntry => () }
    else Future.successful(\/.right( () ))
  }

  /**
   * Log Create action
   *
   * @param userId User that made an action
   * @param projectId Project where action was made
   * @param item What object was affected. ex: a task, a button, some video etc.
   * @param conn
   * @return
   */
  def logCreate(userId: UUID, projectId: UUID, item: String)(implicit conn: Connection): Future[\/[ErrorUnion#Fail, Unit]] = {
    if (config) lift(create(userId, projectId, JournalEntryCreate, item)).map { journalEntry => () }
    else Future.successful(\/.right( () ))
  }

  /**
   * Log Delete action
   *
   * @param userId User that made an action
   * @param projectId Project where action was made
   * @param item What object was affected. ex: a task, a button, some video etc.
   * @param conn
   * @return
   */
  def logDelete(userId: UUID, projectId: UUID, item: String)(implicit conn: Connection): Future[\/[ErrorUnion#Fail, Unit]] = {
    if (config) lift(create(userId, projectId, JournalEntryDelete, item)).map { journalEntry => () }
    else Future.successful(\/.right( () ))
  }
}


