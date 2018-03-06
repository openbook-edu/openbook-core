import java.io.File

import ca.shiftfocus.krispii.core.lib.ScalaCacheConfig
import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
import com.github.mauricio.async.db.pool.PoolConfiguration
import com.github.mauricio.async.db.{Configuration, Connection}
import com.typesafe.config.ConfigFactory
import grizzled.slf4j.Logger
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, MustMatchers, Suite, WordSpec}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
 * Test Environment
 *
 * @param writeToDb Populate test DB with tables and values
 */
@RunWith(classOf[JUnitRunner])
abstract class TestEnvironment(writeToDb: Boolean = true)
    extends WordSpec
    with MustMatchers
    with MockFactory
    with Suite
    with BeforeAndAfter {
  val logger = Logger[this.type]

  //--------------------
  //--START CONNECTION--
  //--------------------
  private val config = ConfigFactory.load()

  private val dbConfig = new Configuration(
    username = config.getString("db.postgresql.username"), //"test_user",
    host = config.getString("db.postgresql.host"), //"localhost",
    password = Some(config.getString("db.postgresql.password")), //"test_user"),
    database = Some(config.getString("db.postgresql.database")), //"testdb")
    port = config.get[Option[Int]]("db.postgresql.port")
  )

  private val poolConfig = new PoolConfiguration(
    maxObjects = 12,
    maxIdle = 1000,
    maxQueueSize = 1000,
    validationInterval = 100
  )

  val database: PostgresDB = new PostgresDB(dbConfig, poolConfig)
  val testConnection: Connection = database.pool

  implicit val conn: Connection = testConnection
  //------------------
  //--END CONNECTION--
  //------------------

  //--------------------
  //--START CACHE--
  //--------------------
  val masterConfig: (String, Int) = ("localhost", 6379) // scalastyle:ignore
  val slaves = Seq.empty[play.api.Configuration]
  val slaveConfigs: Seq[(String, Int)] = slaves.map { slaveConfig =>
    (slaveConfig.get[String]("host"), slaveConfig.get[Int]("port"))
  }
  lazy val scalaCacheConfig: ScalaCacheConfig = ScalaCacheConfig(masterConfig, slaveConfigs)
  //------------------
  //--END CACHE--
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

  // CREATE Journal table in database with current month
  val journalTable = "journal"
  val currentDate = new DateTime
  val lastDayDate = currentDate.dayOfMonth().withMaximumValue()

  val formatSuffix = DateTimeFormat.forPattern("YYYYMM")
  val formatDate = DateTimeFormat.forPattern("YYYY-MM-")
  val formatLastDay = DateTimeFormat.forPattern("dd")

  val suffix = formatSuffix.print(currentDate)
  val checkDate = formatDate.print(currentDate)
  val checkDateDay = formatLastDay.print(lastDayDate)

  val createJournalQuery =
    s"""
      |CREATE TABLE ${journalTable}_${suffix}  (
      |  PRIMARY KEY(id),
      |  check (created_at BETWEEN '${checkDate}01' AND '${checkDate}${checkDateDay}T23:59:59')
      |) INHERITS (${journalTable})
    """.stripMargin

  if (writeToDb) {
    // Before test
    before {
      // DROP tables
      load_schema(drop_schema_path, conn)
      // CREATE tables
      load_schema(create_schema_path, conn)
      // Insert data into tables
      load_schema(data_schema_path, conn)
      // Create Journal table
      val resultJournal = conn.sendQuery(createJournalQuery)
      Await.result(resultJournal, Duration.Inf)
    }

    // After test
    after {
      // DROP tables
      load_schema(drop_schema_path, conn)
    }
  }

  /**
   * Print colored input in console (if supported)
   * println(Console.RED + Console.BOLD + " (TEXT) " + Console.RESET)
   *
   * @param print
   */
  def console_log(print: Any): Unit = {
    val debug = Console.GREEN + Console.BOLD + "[DEBUG] " + Console.RESET
    val value = Console.RED + Console.BOLD + print + Console.RESET
    println(debug + value)
  }
}
