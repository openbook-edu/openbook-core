package ca.shiftfocus.krispii.core.services.datasource

import com.github.mauricio.async.db.Configuration
import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.postgresql.pool.PostgreSQLConnectionFactory
import com.github.mauricio.async.db.pool.{PoolConfiguration, ConnectionPool}
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext

import com.typesafe.config._

import scala.concurrent.Future

case class OutOfDateException(msg: String) extends Exception

trait DB {
  /**
   * Takes a function that returns a future, and runs it inside a database
   * transaction.
   */
  def transactional[A](f : Connection => Future[A]) = {
    db.pool.inTransaction(f).recover {
      case exception => throw exception
    }
  }

  /**
   * Creates futures over a collection in sequence.
   *
   * Normally, if you map a function over a collection that creates futures,
   * those futures will immediately begin running in parallel. Since all
   * database calls return futures, mapping a database call over a collection
   * will cause those calls to run in parallel which is problematic when they
   * are running inside of a transaction.
   *
   * Only one query may be run in a transaction at a time. This function will
   * take the call you want to map, and ensure that it is run over your
   * collection *sequentially*, one at a time, using a fold. Use this when
   * you need to map a function, such as taskRepository.update, over a list
   * of tasks, inside a transaction.
   *
   * Example usage: db.serialized(taskList)(taskRepository.update)
   *
   * In this example, taskRepository.update will be called for each member of
   * taskList, but each item in the list will only be mapped when the previous
   * one has returned a result.
   *
   * @TODO: Only works with IndexedSeq right now! Rewrite this to work generically
   *         with any type of collection.
   * @param collection the collection of elements to map the function over
   * @param fn the function--that returns a future--to run on each element of
   *            the collection.
   * @return the new collection of results
   */
  def serialized[E, R, L[E] <: IndexedSeq[E]](collection: L[E])(fn: E => Future[R]): Future[IndexedSeq[R]] = {
    collection.foldLeft(Future(IndexedSeq.empty[R])) { (fAccumulated, nextItem) =>
      for {
        accumulated <- fAccumulated
        nextResult <- fn(nextItem)
      }
      yield accumulated :+ nextResult
    }
  }

  /**
   * Components extending the DB object expect to have a "db" value giving them
   * access to certain database functionality.
   */
  val db: DBSettings

  /**
   * This trait defines what values we expect "db" to provide, essentially:
   * - databaseConfiguration: a Configuration object with information such as
   *                          the hostname, username, password, etc.
   * - pool: a Connection object either providing a single connection, or a
   *         connection pool
   * - cacheExpiry: an Int giving the length of time in seconds that objects
   *                cached by Redis will live for.
   */
  trait DBSettings {
    val databaseConfiguration: Configuration
    val pool: Connection
    val cacheExpiry: Int
  }
}

/**
 * The PostgreSQL implementation of the DB trait.
 *
 * All concrete repositories that depend on the postgresql database should
 * mixin this implementation trait.
 */
trait PostgresDB extends DB {
  override val db: DBSettings = new PostgresDBSettings

  private class PostgresDBSettings extends DBSettings {
    val config = ConfigFactory.load()

    override val cacheExpiry = Option(config.getInt("app.cache.expires")).getOrElse(5)

    override val databaseConfiguration = {
      new Configuration(
        username = config.getString("db.postgresql.username"), //"accounts",
        host = config.getString("db.postgresql.host"), //"localhost",
        password = Some(config.getString("db.postgresql.password")), //"the spice must flow"),
        database = Some(config.getString("db.postgresql.database")), //"accounts")
        port = config.getInt("db.postgresql.port")
      )
    }

    lazy val factory = new PostgreSQLConnectionFactory(databaseConfiguration)
    override val pool = new ConnectionPool(factory, new PoolConfiguration(
      maxObjects = 60,
      maxIdle = 50,
      maxQueueSize = 1000,
      validationInterval = 50)
    )
  }
}
