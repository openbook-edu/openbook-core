package ca.shiftfocus.krispii.core.services.datasource

import com.github.mauricio.async.db.Configuration
import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.postgresql.pool.PostgreSQLConnectionFactory
import com.github.mauricio.async.db.pool.{PoolConfiguration, ConnectionPool}
import com.typesafe.config._
import scalaz.{\/, -\/, \/-}

trait DB {
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
