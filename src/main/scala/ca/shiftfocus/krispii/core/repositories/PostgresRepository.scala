package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error._
import ca.shiftfocus.lib.exceptions.ExceptionWriter
import ca.shiftfocus.krispii.core.models.ComponentScratchpad
import com.github.mauricio.async.db.exceptions.ConnectionStillRunningQueryException
import com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException
import com.github.mauricio.async.db.{ Connection, RowData, ResultSet }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.{ -\/, \/-, \/ }

trait PostgresRepository[A] {

  val entityName: String

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
    }
    else {
      conn.sendQuery(queryText)
    }

    fRes.map {
      res => buildEntity(res.rows, constructor)
    }.recover {
      case exception: ConnectionStillRunningQueryException =>
        -\/(RepositoryError.DatabaseError("Attempted to send concurrent queries in the same transaction.", Some(exception)))

      case exception: GenericDatabaseException =>
        val fields = exception.errorMessage.fields
        (fields.get('t'), fields.get('n')) match {
          case (Some(table), Some(nField)) if nField endsWith "_pkey" =>
            \/.left(RepositoryError.PrimaryKeyConflict)

          case (Some(table), Some(nField)) if nField endsWith "_key" =>
            \/.left(RepositoryError.UniqueKeyConflict(fields.getOrElse('c', nField.toCharArray.slice(table.length + 1, nField.length - 4).mkString), nField))

          case (Some(table), Some(nField)) if nField endsWith "_fkey" =>
            \/.left(RepositoryError.ForeignKeyConflict(fields.getOrElse('c', nField.toCharArray.slice(table.length + 1, nField.length - 5).mkString), nField))

          case _ => \/.left(RepositoryError.DatabaseError("Unhandled GenericDataabaseException", Some(exception)))
        }

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
  protected def queryList(queryText: String, parameters: Seq[Any] = Seq.empty[Any]) // format: OFF
                         (implicit conn: Connection): Future[\/[RepositoryError.Fail, IndexedSeq[A]]] = { // format: ON
    val fRes = if (parameters.nonEmpty) {
      conn.sendPreparedStatement(queryText, parameters)
    }
    else {
      conn.sendQuery(queryText)
    }

    fRes.map {
      res =>
        {
          println(Console.GREEN + res.rows + Console.RESET)
          buildEntityList(res.rows, constructor)
        }
    }.recover {
      case exception: ConnectionStillRunningQueryException =>
        -\/(RepositoryError.DatabaseError("Attempted to send concurrent queries in the same transaction.", Some(exception)))

      case exception: GenericDatabaseException =>
        val fields = exception.errorMessage.fields
        (fields.get('t'), fields.get('n')) match {
          case (Some(table), Some(nField)) if nField endsWith "_pkey" =>
            \/.left(RepositoryError.PrimaryKeyConflict)

          case (Some(table), Some(nField)) if nField endsWith "_key" =>
            \/.left(RepositoryError.UniqueKeyConflict(fields.getOrElse('c', nField.toCharArray.slice(table.length + 1, nField.length - 4).mkString), nField))

          case (Some(table), Some(nField)) if nField endsWith "_fkey" =>
            \/.left(RepositoryError.ForeignKeyConflict(fields.getOrElse('c', nField.toCharArray.slice(table.length + 1, nField.length - 5).mkString), nField))

          case _ => \/.left(RepositoryError.DatabaseError("Unhandled GenericDatabaseException", Some(exception)))
        }

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
  protected def queryNumRows(queryText: String, parameters: Seq[Any] = Seq.empty[Any]) // format: OFF
                            (compare: Long => Boolean)(implicit conn: Connection): Future[\/[RepositoryError.Fail, Boolean]] = { // format: ON
    val fRes = if (parameters.nonEmpty) {
      conn.sendPreparedStatement(queryText, parameters)
    }
    else {
      conn.sendQuery(queryText)
    }

    fRes.map {
      res => \/-(compare(res.rowsAffected))
    }.recover {
      case exception: ConnectionStillRunningQueryException =>
        -\/(RepositoryError.DatabaseError("Attempted to send concurrent queries in the same transaction.", Some(exception)))

      case exception: GenericDatabaseException =>
        val fields = exception.errorMessage.fields
        (fields.get('t'), fields.get('n')) match {
          case (Some(table), Some(nField)) if nField endsWith "_pkey" =>
            \/.left(RepositoryError.PrimaryKeyConflict)

          case (Some(table), Some(nField)) if nField endsWith "_key" =>
            \/.left(RepositoryError.UniqueKeyConflict(fields.getOrElse('c', nField.toCharArray.slice(table.length + 1, nField.length - 4).mkString), nField))

          case (Some(table), Some(nField)) if nField endsWith "_fkey" =>
            \/.left(RepositoryError.ForeignKeyConflict(fields.getOrElse('c', nField.toCharArray.slice(table.length + 1, nField.length - 5).mkString), nField))

          case _ => \/.left(RepositoryError.DatabaseError("Unhandled GenericDatabaseException", Some(exception)))
        }

      case exception => throw exception
    }
  }

  /**
   * Generic method to build an entity from postgresql database results, since
   * the postgresql database may not return results.
   *
   * @param maybeResultSet an optional ResultSet returned from the database
   * @param build a function that can build entities of type A from a RowData object
   * @tparam B the type of entity to be built
   * @return a disjunction containing either a RepositoryError.Fail, or an object of type A
   */
  protected def buildEntity[B](maybeResultSet: Option[ResultSet], build: RowData => B): \/[RepositoryError.Fail, B] = {
    maybeResultSet match {
      case Some(resultSet) => resultSet.headOption match {
        case Some(firstRow) => \/-(build(firstRow))
        case None => -\/(RepositoryError.NoResults(s"ResultSet returned no rows. Could not build entity of type $entityName"))
      }
      case None => -\/(RepositoryError.NoResults(s"No ResultSet was returned. Could not build entity of type $entityName"))
    }
  }

  /**
   * Generic method to build a list of entities from postgresql database results, since
   * the postgresql database may not return results.
   *
   * TODO: check in which case the database driver will return "None" instead of Some(ResultSet)
   *
   * @param maybeResultSet an optional ResultSet returned from the database
   * @param build a function that can build entities of type A from a RowData object
   * @tparam B the type of entity to be built
   * @return a disjunction containing either a RepositoryError.Fail, or an object of type A
   */
  protected def buildEntityList[B](maybeResultSet: Option[ResultSet], build: RowData => B): \/[RepositoryError.Fail, IndexedSeq[B]] = {
    maybeResultSet match {
      case Some(resultSet) => \/-(resultSet.map(build))
      case None => -\/(RepositoryError.NoResults(s"Could not list entity of type $entityName"))
    }
  }

}
