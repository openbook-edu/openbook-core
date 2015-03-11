import java.io.File

import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
import com.github.mauricio.async.db.{Connection, Configuration}
import com.github.mauricio.async.db.pool.PoolConfiguration
import com.typesafe.config.ConfigFactory
import grizzled.slf4j.Logger
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfter, Suite, MustMatchers, WordSpec}
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class TestEnvironment
  extends WordSpec
  with MustMatchers
  with MockFactory
  with Suite
  with BeforeAndAfter
{
  val logger = Logger[this.type]

  //--------------------
  //--START CONNECTION--
  //--------------------
  private val config = ConfigFactory.load()

  private val dbConfig = new Configuration(
    username = config.getString("db.postgresql.username"), //"accounts",
    host     = config.getString("db.postgresql.host"), //"localhost",
    password = Some(config.getString("db.postgresql.password")), //"the spice must flow"),
    database = Some(config.getString("db.postgresql.database")), //"accounts")
    port     = config.getInt("db.postgresql.port")
  )

  private val poolConfig = new PoolConfiguration(
    maxObjects = 12,
    maxIdle = 25,
    maxQueueSize = 1000,
    validationInterval = 50
  )

  val database: PostgresDB = new PostgresDB(dbConfig, poolConfig)
  val testConnection: Connection = database.pool

  implicit val conn: Connection = testConnection
  //------------------
  //--END CONNECTION--
  //------------------

  val project_path = new File(".").getAbsolutePath()
  val create_schema_path = s"${project_path}/src/test/resources/schemas/create_schema.sql"
  val drop_schema_path = s"${project_path}/src/test/resources/schemas/drop_schema.sql"
  val data_schema_path = s"${project_path}/src/test/resources/schemas/data_schema.sql"

  /**
   * Implements query from schema file
   * @param path Path to schema file
   */
  def load_schema(path: String, conn: Connection): Unit = {
    val sql_schema_file = scala.io.Source.fromFile(path)
    val query = sql_schema_file.getLines().mkString
    sql_schema_file.close()
    val result = conn.sendQuery(query)
    Await.result(result, Duration.Inf)
  }

  // Before test
  before {
    // DROP tables
    load_schema(drop_schema_path, conn)
    // CREATE tables
    load_schema(create_schema_path, conn)
    // Insert data into tables
    load_schema(data_schema_path, conn)
  }

  // After test
  after {
    // DROP tables
    load_schema(drop_schema_path, conn)
  }

  def selectOneById(from: String) =
    s"""
       SELECT *
       FROM ${from}
       WHERE id = ?
     """
}
