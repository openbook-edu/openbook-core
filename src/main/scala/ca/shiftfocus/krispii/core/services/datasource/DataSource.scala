package ca.shiftfocus.krispii.core.services.datasource

import ca.shiftfocus.krispii.core.error.ErrorUnion
import ca.shiftfocus.krispii.core.lib.FutureMonad
import com.github.mauricio.async.db.Configuration
import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.postgresql.pool.PostgreSQLConnectionFactory
import com.github.mauricio.async.db.pool.{PoolConfiguration, ConnectionPool}
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext

import com.typesafe.config._
import ca.shiftfocus.krispii.core.fail._
import scala.concurrent.Future
import scalaz.{\/, -\/, \/-}

case class OutOfDateException(msg: String) extends Exception

trait DB {
  /**
   * Takes a function that returns a future, and runs it inside a database
   * transaction.
   */
  def transactional[A](f : Connection => Future[A]) = {
    pool.inTransaction(f).recover {
      case exception => throw exception
    }
  }

  val dbconfig: Configuration
  def pool: Connection
}

/**
 * The PostgreSQL implementation of the DB trait.
 *
 * All concrete repositories that depend on the postgresql database should
 * mixin this implementation trait.
 */
class PostgresDB(val dbconfig: Configuration) extends DB {
  val config = ConfigFactory.load()
  //val cacheExpiry = Option(config.getInt("app.cache.expires")).getOrElse(5)
  lazy val factory = new PostgreSQLConnectionFactory(dbconfig)
  override def pool = connectionPool

  private val connectionPool = new ConnectionPool(factory, new PoolConfiguration(
    maxObjects = 30,
    maxIdle = 25,
    maxQueueSize = 1000,
    validationInterval = 50)
  )
}
