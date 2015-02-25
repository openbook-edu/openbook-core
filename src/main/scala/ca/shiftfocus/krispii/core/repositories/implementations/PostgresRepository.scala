package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.fail.{NoResults, Fail}
import ca.shiftfocus.krispii.core.models.ComponentScratchpad
import com.github.mauricio.async.db.{RowData, ResultSet}

import scalaz.{-\/, \/-, \/}

trait PostgresRepository {

  /**
   * Generic method to build an entity from postgresql database results, since
   * the postgresql database may not return results.
   *
   * @param maybeResultSet an optional [[ResultSet]] returned from the database
   * @param build a function that can build entities of type A from a [[RowData]] object
   * @tparam A the type of entity to be built
   * @return a disjunction containing either a Fail, or an object of type A
   */
  protected def buildEntity[A](maybeResultSet: Option[ResultSet], build: RowData => A): \/[Fail, A] = {
    maybeResultSet match {
      case Some(resultSet) => resultSet.headOption match {
        case Some(firstRow) => \/-(build(firstRow))
        case None => -\/(NoResults("The query was successful but ResultSet was empty."))
      }
      case None => -\/(NoResults("The query was successful but no ResultSet was returned."))
    }
  }

  /**
   * Generic method to build a list of entities from postgresql database results, since
   * the postgresql database may not return results.
   *
   * @param maybeResultSet an optional [[ResultSet]] returned from the database
   * @param build a function that can build entities of type A from a [[RowData]] object
   * @tparam A the type of entity to be built
   * @return a disjunction containing either a Fail, or an object of type A
   */
  protected def buildEntityList[A](maybeResultSet: Option[ResultSet], build: RowData => A): \/[Fail, IndexedSeq[A]] = {
    maybeResultSet match {
      case Some(resultSet) => \/-(resultSet.map(build))
      case None => -\/(NoResults("The query was successful but no ResultSet was returned."))
    }
  }

}