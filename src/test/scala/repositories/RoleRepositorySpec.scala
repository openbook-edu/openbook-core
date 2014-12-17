//import java.awt.Color
//import java.io.File
//
//import ca.shiftfocus.krispii.core.models.{Role, Class, User}
//import ca.shiftfocus.krispii.core.repositories.RoleRepositoryPostgresComponent
//import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
//import ca.shiftfocus.uuid.UUID
//import grizzled.slf4j.Logger
//import org.scalamock.scalatest.MockFactory
//import org.scalatest.{MustMatchers, WordSpec, BeforeAndAfterAll, Suite}
//
//import scala.concurrent.Await
//import scala.concurrent.duration.Duration
//
//trait RoleRepoTestEnvironment
//  extends RoleRepositoryPostgresComponent
//  with Suite
//  with BeforeAndAfterAll
//  with PostgresDB {
//  val logger = Logger[this.type]
//
//  implicit val connection = db.pool
//
//  val project_path = new File(".").getAbsolutePath()
//  val create_schema_path = s"${project_path}/src/test/resources/schemas/create_schema.sql"
//  val drop_schema_path = s"${project_path}/src/test/resources/schemas/drop_schema.sql"
//  val data_schema_path = s"${project_path}/src/test/resources/schemas/roles/data_schema.sql"
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
//
//  val testRoleA = Role(
//
//  )
//
//}
//
//class RoleRepositorySpec extends WordSpec
//with MustMatchers
//with MockFactory
//with RoleRepoTestEnvironment {
//  "RoleRepository.list" should {
//    "list all roles" in {
//      val result = roleRepository.list
//
//      val roles = Await.result(result, Duration.Inf)
//
//      roles should be(Vector(testRoleA, testRoleB, testRoleC))
//      Map[Int, User](0 -> testRoleA, 1 -> testRoleB, 2 -> testRoleC).foreach {
//        case (key, role: Role) => {
//          roles(key).id should be(role.id)
//          roles(key).version should be(role.version)
//          roles(key).email should be(role.email)
//          roles(key).username should be(role.username)
//          roles(key).givenname should be(role.givenname)
//          roles(key).surname should be(role.surname)
//        }
//      }
//    }
//  }
//}
