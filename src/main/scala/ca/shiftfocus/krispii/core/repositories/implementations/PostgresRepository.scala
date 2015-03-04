package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.krispii.core.lib.ExceptionWriter
import ca.shiftfocus.krispii.core.models.ComponentScratchpad
import com.github.mauricio.async.db.exceptions.ConnectionStillRunningQueryException
import com.github.mauricio.async.db.{Connection, RowData, ResultSet}
import play.api.Logger
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.{-\/, \/-, \/}

trait PostgresRepository[A] {

  def constructor(row: RowData): A

  /**
   * Send query to the database and retrieve a single entity.
   *
   * @param queryText
   * @param parameters
   * @param conn
   * @return
   */
  protected def queryOne(queryText: String, parameters: Seq[Any] = Seq.empty[Any])(implicit conn: Connection): Future[\/[RepositoryError.Fail, A]] = {
    val fRes = if (parameters.nonEmpty) {
      conn.sendPreparedStatement(queryText, parameters)
    } else {
      conn.sendQuery(queryText)
    }

    fRes.map {
      res => buildEntity(res.rows, constructor)
    }.recover {
      case exception: ConnectionStillRunningQueryException =>
        -\/(RepositoryError.DatabaseError("Attempted to send concurrent queries in the same transaction.", Some(exception)))
      case exception => throw exception
    }
  }

  /**
   * Send a query to the database and retrieve a list of entities.
   *
   * @param queryText
   * @param parameters
   * @param conn
   * @return
   */
  protected def queryList(queryText: String, parameters: Seq[Any] = Seq.empty[Any])(implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[A]]] = {
    val fRes = if (parameters.nonEmpty) {
      conn.sendPreparedStatement(queryText, parameters)
    } else {
      conn.sendQuery(queryText)
    }

    fRes.map {
      res => buildEntityList(res.rows, constructor)
    }.recover {
      case exception: ConnectionStillRunningQueryException =>
        -\/(RepositoryError.DatabaseError("Attempted to send concurrent queries in the same transaction.", Some(exception)))
      case exception => throw exception
    }
  }

  /**
   * Send query to the database and compare the number
   * of rows affected.
   *
   * @param queryText
   * @param parameters
   * @param conn
   * @return
   */
  protected def queryNumRows(queryText: String, parameters: Seq[Any] = Seq.empty[Any])
                            (compare: Long => Boolean)
                            (implicit conn: Connection): Future[\/[RepositoryError.Fail, Boolean]] =
  {
    val fRes = if (parameters.nonEmpty) {
      conn.sendPreparedStatement(queryText, parameters)
    } else {
      conn.sendQuery(queryText)
    }

    fRes.map {
      res => \/-(compare(res.rowsAffected))
    }.recover {
      case exception: ConnectionStillRunningQueryException =>
        -\/(RepositoryError.DatabaseError("Attempted to send concurrent queries in the same transaction.", Some(exception)))
      case exception => throw exception
    }
  }

  /**
   * Generic method to build an entity from postgresql database results, since
   * the postgresql database may not return results.
   *
   * @param maybeResultSet an optional [[ResultSet]] returned from the database
   * @param build a function that can build entities of type A from a [[RowData]] object
   * @tparam A the type of entity to be built
   * @return a disjunction containing either a RepositoryError.Fail, or an object of type A
   */
  protected def buildEntity[B](maybeResultSet: Option[ResultSet], build: RowData => B): \/[RepositoryError.Fail, B] = {
    maybeResultSet match {
      case Some(resultSet) => resultSet.headOption match {
        case Some(firstRow) => \/-(build(firstRow))
        case None => -\/(RepositoryError.NoResults("The query was successful but ResultSet was empty."))
      }
      case None => -\/(RepositoryError.NoResults("The query was successful but no ResultSet was returned."))
    }
  }

  /**
   * Generic method to build a list of entities from postgresql database results, since
   * the postgresql database may not return results.
   *
   * @param maybeResultSet an optional [[ResultSet]] returned from the database
   * @param build a function that can build entities of type A from a [[RowData]] object
   * @tparam A the type of entity to be built
   * @return a disjunction containing either a RepositoryError.Fail, or an object of type A
   */
  protected def buildEntityList[B](maybeResultSet: Option[ResultSet], build: RowData => B): \/[RepositoryError.Fail, IndexedSeq[B]] = {
    maybeResultSet match {
      case Some(resultSet) => \/-(resultSet.map(build))
      case None => -\/(RepositoryError.NoResults("The query was successful but no ResultSet was returned."))
    }
  }

}