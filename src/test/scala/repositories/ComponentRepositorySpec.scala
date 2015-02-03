//import java.io.File
//
//import ca.shiftfocus.krispii.core.repositories.ComponentRepositoryPostgresComponent
//import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
//import grizzled.slf4j.Logger
//import org.scalamock.scalatest.MockFactory
//import org.scalatest.{MustMatchers, WordSpec, BeforeAndAfterAll, Suite}
//import org.scalatest._
//import Matchers._
//import scala.concurrent.ExecutionContext.Implicits.global
//
//import scala.concurrent.Await
//import scala.concurrent.duration.Duration
//
//trait ComponentRepoTestEnvironment
//  extends ComponentRepositoryPostgresComponent
//  with Suite
//  with BeforeAndAfterAll
//  with PostgresDB {
//
//  val logger = Logger[this.type]
//
//  implicit val connection = db.pool
//
//  val project_path = new File(".").getAbsolutePath()
//  val create_schema_path = s"${project_path}/src/test/resources/schemas/create_schema.sql"
//  val drop_schema_path = s"${project_path}/src/test/resources/schemas/drop_schema.sql"
//  val data_schema_path = s"${project_path}/src/test/resources/schemas/data_schema.sql"
//
//  /**
//   * Implements query from schema file
//   * @param path Path to schema file
//   */
//  def load_schema(path: String): Unit = {
//    val sql_schema_file = scala.io.Source.fromFile(path)
//    val query = sql_schema_file.getLines().mkString
//    sql_schema_file.close()
//    val result = db.pool.sendQuery(query)
//    Await.result(result, Duration.Inf)
//  }
//
//  // Before test
//  override def beforeAll(): Unit = {
//    // DROP tables
//    load_schema(drop_schema_path)
//    // CREATE tables
//    load_schema(create_schema_path)
//    // Insert data into tables
//    load_schema(data_schema_path)
//  }
//
//  // After test
//  override def afterAll(): Unit = {
//    // DROP tables
//    load_schema(drop_schema_path)
//  }
//}
//
//class ComponentRepositorySpec
//  extends WordSpec
//  with MustMatchers
//  with MockFactory
//  with ComponentRepoTestEnvironment {
//
//    "bla" should {
//      inSequence {
//        "do" in {
//          val a = 1
//
//          a should be (1)
//        }
//      }
//    }
//  }