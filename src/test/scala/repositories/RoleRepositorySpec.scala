import ca.shiftfocus.krispii.core.error.RepositoryError
import com.github.mauricio.async.db.RowData
import scala.collection._
import scala.collection.immutable.TreeMap
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.repositories.{UserRepositoryPostgres, RoleRepositoryPostgres}
import ca.shiftfocus.uuid.UUID
import org.scalatest._
import Matchers._
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scalaz.{-\/, \/-}


//TODO check fields in users_roles if user already has this role, file PostgresRepository method queryOne println(val fields)
class RoleRepositorySpec
  extends TestEnvironment
{
  val userRepository = new UserRepositoryPostgres
  val roleRepository = new RoleRepositoryPostgres(userRepository)

  "RoleRepository.list" should {
    inSequence {
      "list all roles" in {
        val testRolesList = TreeMap[Int, Role](
          0 -> TestValues.testRoleA,
          1 -> TestValues.testRoleB,
          2 -> TestValues.testRoleC,
          3 -> TestValues.testRoleF,
          4 -> TestValues.testRoleG,
          5 -> TestValues.testRoleH
        )

        val result = roleRepository.list
        val eitherRoles = Await.result(result, Duration.Inf)
        val \/-(roles) = eitherRoles

        eitherRoles.toString should be(\/- (testRolesList.map(_._2.toString)(breakOut)).toString)

        testRolesList.foreach {
          case (key, role: Role) => {
            roles(key).id should be(role.id)
            roles(key).version should be(role.version)
            roles(key).name should be(role.name)
            roles(key).createdAt.toString should be(role.createdAt.toString)
            roles(key).updatedAt.toString should be(role.updatedAt.toString)
          }
        }
      }
      "list the roles associated with a user" in {
        val testRolesList = TreeMap[Int, Role](
          0 -> TestValues.testRoleA,
          1 -> TestValues.testRoleB,
          2 -> TestValues.testRoleF,
          3 -> TestValues.testRoleG
        )

        val result = roleRepository.list(TestValues.testUserA)
        val eitherRoles = Await.result(result, Duration.Inf)
        val \/-(roles) = eitherRoles

        eitherRoles.toString should be(\/- (testRolesList.map(_._2.toString)(breakOut)).toString)

        testRolesList.foreach {
          case (key, role: Role) => {
            roles(key).id should be(role.id)
            roles(key).version should be(role.version)
            roles(key).name should be(role.name)
            roles(key).createdAt.toString should be(role.createdAt.toString)
            roles(key).updatedAt.toString should be(role.updatedAt.toString)
          }
        }
      }
      "return empty Vector() if user doesn't exist" in {
        val unexistingUser = User(
          email     = "unexisting_email@example.com",
          username  = "unexisting_username",
          givenname = "unexisting_givenname",
          surname   = "unexisting_surname"
        )

        val result = roleRepository.list(unexistingUser)

        Await.result(result, Duration.Inf) should be (\/- (Vector()))
      }
      "list the roles associated with a users" in {
        val testUsersList = TreeMap[Int, User](
          0 -> TestValues.testUserA,
          1 -> TestValues.testUserB
        )

        val testRoleList = Map[UUID, Vector[Role]](
          testUsersList(0).id -> Vector(
            TestValues.testRoleA,
            TestValues.testRoleB,
            TestValues.testRoleF,
            TestValues.testRoleG
          ),
          testUsersList(1).id -> Vector(
            TestValues.testRoleA,
            TestValues.testRoleB,
            TestValues.testRoleF
          )
        )

        val result = roleRepository.list(Vector(testUsersList(0), testUsersList(1)))
        val eitherRoles = Await.result(result, Duration.Inf)
        val \/-(roles) = eitherRoles

        eitherRoles.toString should be(\/-(testRoleList).toString)

        testRoleList.foreach {
          case (userId: UUID, rolesList: Vector[Role]) => {
            var key = 0
            for (role: Role <- rolesList) {
              roles(userId)(key).id should be(role.id)
              roles(userId)(key).version should be(role.version)
              roles(userId)(key).name should be(role.name)
              roles(userId)(key).createdAt.toString should be(role.createdAt.toString)
              roles(userId)(key).updatedAt.toString should be(role.updatedAt.toString)
              key = key + 1
            }
          }
        }
      }
      "return empty Vector() if one of the users doesn't exist" in {
        val testUsersList = TreeMap[Int, User](
          0 -> TestValues.testUserA,
          1 -> TestValues.testUserD
        )

        val testRoleList = Map[UUID, Vector[Role]](
          testUsersList(0).id -> Vector(
            TestValues.testRoleA,
            TestValues.testRoleB,
            TestValues.testRoleF,
            TestValues.testRoleG
          ),
          testUsersList(1).id -> Vector()
        )

        val result = roleRepository.list(Vector(testUsersList(0), testUsersList(1)))
        val eitherRoles = Await.result(result, Duration.Inf)
        val \/-(roles) = eitherRoles

        eitherRoles.toString should be(\/-(testRoleList).toString)
      }
    }
  }

  "RoleRepository.find" should {
    inSequence {
      "find a single entry by ID" in {
        val testRole = TestValues.testRoleA

        val result = roleRepository.find(testRole.id)
        val eitherRole = Await.result(result, Duration.Inf)
        val \/-(role) = eitherRole

        role.id should be(testRole.id)
        role.version should be(testRole.version)
        role.name should be(testRole.name)
        role.createdAt.toString should be(testRole.createdAt.toString)
        role.updatedAt.toString should be(testRole.updatedAt.toString)
      }
      "return RepositoryError.NoResults if entry wasn't found by ID" in {
        val result = roleRepository.find(UUID("f9aadc67-5e8b-48f3-b0a2-20a0d7d88477"))

        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
      }
      "find a single entry by name" in {
        val testRole = TestValues.testRoleA

        val result = roleRepository.find(testRole.name)
        val eitherRole = Await.result(result, Duration.Inf)
        val \/-(role) = eitherRole

        role.id should be(testRole.id)
        role.version should be(testRole.version)
        role.name should be(testRole.name)
        role.createdAt.toString should be(testRole.createdAt.toString)
        role.updatedAt.toString should be(testRole.updatedAt.toString)
      }
      "return RepositoryError.NoResults if entry wasn't found by name" in {
        val result = roleRepository.find("unexisting_role_name")

        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
      }
    }
  }


  // TODO - find out the way to test -\/(RepositoryError.DatabaseError("Role couldn't be added to all users."))
  "RoleRepository.addUsers" should {
    inSequence {
      "add role to users" in {
        val testRole = TestValues.testRoleC
        val testUsersList = Vector(
          TestValues.testUserA,
          TestValues.testUserB
        )

        val result = roleRepository.addUsers(testRole, testUsersList)
        Await.result(result, Duration.Inf) should be(Unit)
      }
      "return RepositoryError.PrimaryKeyConflict if we add a role to the user that already has this role" in {
        val testRole = TestValues.testRoleB
        val testUsersList = Vector(
          TestValues.testUserC,
          TestValues.testUserA
        )

        val result = roleRepository.addUsers(testRole, testUsersList)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.PrimaryKeyConflict))
      }
      "return RepositoryError.ForeignKeyConflict if we add a role to unexisting user" in {
        val testRole = TestValues.testRoleC
        val testUsersList = Vector(
          TestValues.testUserA,
          TestValues.testUserD
        )

        val result = roleRepository.addUsers(testRole, testUsersList)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.ForeignKeyConflict("user_id", "users_roles_user_id_fkey")))
      }
      "return RepositoryError.ForeignKeyConflict if we add an unexisting role to user" in {
        val testRole = TestValues.testRoleD
        val testUsersList = Vector(
          TestValues.testUserA
        )

        val result = roleRepository.addUsers(testRole, testUsersList)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.ForeignKeyConflict("role_id", "users_roles_role_id_fkey")))
      }
    }
  }


  "RoleRepository.removeUsers" should {
    inSequence {
      "remove role from users" in {
        val testRole = TestValues.testRoleB
        val testUsersList = Vector(
          TestValues.testUserA,
          TestValues.testUserB
        )

        val result = roleRepository.removeUsers(testRole, testUsersList)
        Await.result(result, Duration.Inf) should be(Unit)
      }
      "return RepositoryError.DatabaseError if the user doesn't have this role" in {
        val testRole = TestValues.testRoleA
        val testUsersList = Vector(
          TestValues.testUserA,
          TestValues.testUserC
        )

        val result = roleRepository.removeUsers(testRole, testUsersList)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.DatabaseError("Role couldn't be removed from all users." ,None)))
      }
      "return RepositoryError.DatabaseError if role doesn't exist" in {
        val testRole = TestValues.testRoleD
        val testUsersList = Vector(
          TestValues.testUserA
        )

        val result = roleRepository.removeUsers(testRole, testUsersList)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.DatabaseError("Role couldn't be removed from all users." ,None)))
      }
      "return RepositoryError.DatabaseError if user doesn't exist" in {
        val testRole = TestValues.testRoleA
        val testUsersList = Vector(
          TestValues.testUserD
        )

        val result = roleRepository.removeUsers(testRole, testUsersList)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.DatabaseError("Role couldn't be removed from all users." ,None)))
      }
    }
  }


  "RoleRepository.insert" should {
    inSequence {
      "save a Role row" in {
        val testRole = TestValues.testRoleD

        val result = roleRepository.insert(testRole)
        val eitherRole = Await.result(result, Duration.Inf)
        val \/-(role) = eitherRole

        role.id should be(testRole.id)
        role.version should be(testRole.version)
        role.name should be(testRole.name)
      }
      "return RepositoryError.PrimaryKeyConflict if role already exists" in {
        val result = roleRepository.insert(TestValues.testRoleA)

        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.PrimaryKeyConflict))
      }
    }
  }

  "RoleRepository.update" should {
    inSequence {
      "update an existing Role" in {
        val testRole = TestValues.testRoleC
        val updatedRole = testRole.copy(
          name = "updated test role name"
        )

        val result = roleRepository.update(updatedRole)
        val eitherRole = Await.result(result, Duration.Inf)
        val \/-(role) = eitherRole

        role.id should be(updatedRole.id)
        role.version should be(updatedRole.version + 1)
        role.name should be(updatedRole.name)
        role.createdAt.toString should be(updatedRole.createdAt.toString)
      }
      "reutrn RepositoryError.NoResults when update an existing Role with wrong version" in {
        val testRole = TestValues.testRoleC
        val updatedRole = testRole.copy(
          name    = "updated test role name",
          version = 99L
        )

        val result = roleRepository.update(updatedRole)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
      }
      "reutrn RepositoryError.NoResults when update an unexisting Role" in {
        val testRole = TestValues.testRoleD
        val updatedRole = testRole.copy(
          name    = "updated test role name"
        )

        val result = roleRepository.update(updatedRole)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
      }
    }
  }


  "RoleRepository.delete" should {
    inSequence{
      "delete role if role doesn't have any references in other tables" in {
        val testRole = TestValues.testRoleB

        val result = roleRepository.delete(testRole)
        Await.result(result, Duration.Inf) should be(\/-(testRole))
      }
      "delete role if role has a references in other tables" in {
        val testRole = TestValues.testRoleA

        val result = roleRepository.delete(testRole)
        Await.result(result, Duration.Inf) should be(\/-(testRole))
      }
      "return RepositoryError.NoResults if Role hasn't been found" in {
        val testRole = Role(
          name = "unexisting role"
        )

        val result = roleRepository.delete(testRole)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
      }
    }
  }
//
//  /*
//    After implementation
//    In db: RoleC, RoleD, RoleF, RoleG, RoleH
//    testUserA -> RoleF, RoleG, RoleC
//    testUserB -> RoleF, RoleG, RoleC
//    testUserF -> RoleC, RoleH
//  */
//  "RoleRepository.addToUser" should {
//    inSequence {
//      "associate a role (by object) to a user" in {
//        val query_result = roleRepository.addToUser(TestValues.testUserC, TestValues.testRoleC)
//
//        Await.result(query_result, Duration.Inf) should be (true)
//
//        // Find roles for TestValues.testUserC
//        val result = db.pool.sendPreparedStatement(find_roles_query, Array[Any](TestValues.testUserC.id.bytes)).map { queryResult =>
//          val roleList = queryResult.rows.get.map {
//            item: RowData => Role(item)
//          }
//          roleList
//        }
//
//        val roleList = Await.result(result, Duration.Inf)
//        roleList contains TestValues.testRoleC should be (true)
//      }
//
//      "associate a role (by name) to a user" in {
//        val query_result = roleRepository.addToUser(TestValues.testUserC, TestValues.testRoleH.name)
//
//        Await.result(query_result, Duration.Inf) should be (true)
//
//        // Find roles for TestValues.testUserC
//        val result = db.pool.sendPreparedStatement(find_roles_query, Array[Any](TestValues.testUserC.id.bytes)).map { queryResult =>
//          val roleList = queryResult.rows.get.map {
//            item: RowData => Role(item)
//          }
//          roleList
//        }
//
//        val roleList = Await.result(result, Duration.Inf)
//        roleList contains TestValues.testRoleH should be (true)
//      }
//      "throw a GenericDatabaseException exception if user doesn't exist" in {
//        val query_result = roleRepository.addToUser(TestValues.testUserD, TestValues.testRoleB)
//
//        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(query_result, Duration.Inf)
//      }
//      "throw a GenericDatabaseException exception if role (object) doesn't exist" in {
//        val query_result = roleRepository.addToUser(TestValues.testUserA, TestValues.testRoleE)
//
//        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(query_result, Duration.Inf)
//      }
//      "return FALSE if role (name) doesn't exist" in {
//        val query_result = roleRepository.addToUser(TestValues.testUserA, TestValues.testRoleE.name)
//
//        Await.result(query_result, Duration.Inf) should be (false)
//      }
//      "throw a GenericDatabaseException exception if user has already this role (object)" in {
//        val query_result = roleRepository.addToUser(TestValues.testUserA, TestValues.testRoleF)
//
//        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(query_result, Duration.Inf)
//      }
//      "throw a GenericDatabaseException exception if user has already this role (name)" in {
//        val query_result = roleRepository.addToUser(TestValues.testUserA, TestValues.testRoleF.name)
//
//        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(query_result, Duration.Inf)
//      }
//    }
//  }
//
//  /*
//    After implementation
//    In db: RoleC, RoleD, RoleF, RoleG, RoleH
//    testUserA -> RoleF, RoleG, RoleC
//    testUserB -> RoleF, RoleG, RoleC
//  */
//  "RoleRepository.removeFromUser" should {
//    inSequence {
//      "remove role from user when role is object" in {
//        val query_result = roleRepository.removeFromUser(TestValues.testUserF, TestValues.testRoleC)
//
//        Await.result(query_result, Duration.Inf) should be (true)
//
//        // Find roles for TestValues.testUserA
//        val result = db.pool.sendPreparedStatement(find_roles_query, Array[Any](TestValues.testUserF.id.bytes)).map { queryResult =>
//          val roleList = queryResult.rows.get.map {
//            item: RowData => Role(item)
//          }
//          roleList
//        }
//
//        val roleList = Await.result(result, Duration.Inf)
//        roleList contains TestValues.testRoleC should be (false)
//      }
//      "remove role from user by role name" in {
//        val query_result = roleRepository.removeFromUser(TestValues.testUserF, TestValues.testRoleH.name)
//
//        Await.result(query_result, Duration.Inf) should be (true)
//
//        // Find roles for TestValues.testUserB
//        val result = db.pool.sendPreparedStatement(find_roles_query, Array[Any](TestValues.testUserF.id.bytes)).map { queryResult =>
//          val roleList = queryResult.rows.get.map {
//            item: RowData => Role(item)
//          }
//          roleList
//        }
//
//        val roleList = Await.result(result, Duration.Inf)
//        roleList contains TestValues.testRoleH should be (false)
//      }
//      "return FALSE if role (object) doesn't exist" in {
//        val query_result = roleRepository.removeFromUser(TestValues.testUserA, TestValues.testRoleE)
//
//        Await.result(query_result, Duration.Inf) should be (false)
//      }
//      "return FALSE if role (name) doesn't exist" in {
//        val query_result = roleRepository.removeFromUser(TestValues.testUserA, TestValues.testRoleE.name)
//
//        Await.result(query_result, Duration.Inf) should be (false)
//      }
//      "return FALSE if user doesn't exist" in {
//        val query_result = roleRepository.removeFromUser(TestValues.testUserD, TestValues.testRoleA)
//
//        Await.result(query_result, Duration.Inf) should be (false)
//      }
//      "return FALSE if user doesn't have this role (object)" in {
//        val query_result = roleRepository.removeFromUser(TestValues.testUserG, TestValues.testRoleG)
//
//        Await.result(query_result, Duration.Inf) should be (false)
//      }
//      "return FALSE if user doesn't have this role (name)" in {
//        val query_result = roleRepository.removeFromUser(TestValues.testUserG, TestValues.testRoleG)
//
//        Await.result(query_result, Duration.Inf) should be (false)
//      }
//    }
//  }
//
//  /*
//    After implementation
//    In db: RoleC, RoleD, RoleF, RoleG, RoleH
//    testUserA -> RoleC
//    testUserB -> RoleC
//  */
//  "RoleRepository.removeFromAllUsers" should {
//    inSequence {
//      "remove role from all users when role is object"  in {
//        val query_result = roleRepository.removeFromAllUsers(TestValues.testRoleF)
//
//        Await.result(query_result, Duration.Inf) should be (true)
//
//        // Find roles for TestValues.testUserA
//        val resultForUserA = db.pool.sendPreparedStatement(find_roles_query, Array[Any](TestValues.testUserA.id.bytes)).map { queryResult =>
//          val roleList = queryResult.rows.get.map {
//            item: RowData => Role(item)
//          }
//          roleList
//        }
//
//        val roleListUserA = Await.result(resultForUserA, Duration.Inf)
//        roleListUserA contains TestValues.testRoleF should be (false)
//
//        // Find roles for TestValues.testUserB
//        val resultForUserB = db.pool.sendPreparedStatement(find_roles_query, Array[Any](TestValues.testUserB.id.bytes)).map { queryResult =>
//          val roleList = queryResult.rows.get.map {
//            item: RowData => Role(item)
//          }
//          roleList
//        }
//
//        val roleListUserB = Await.result(resultForUserB, Duration.Inf)
//        roleListUserB contains TestValues.testRoleF should be (false)
//      }
//      "remove role from all users by role name" in {
//        val query_result = roleRepository.removeFromAllUsers(TestValues.testRoleG.name)
//
//        Await.result(query_result, Duration.Inf) should be (true)
//
//        // Find roles for TestValues.testUserA
//        val resultForUserA = db.pool.sendPreparedStatement(find_roles_query, Array[Any](TestValues.testUserA.id.bytes)).map { queryResult =>
//          val roleList = queryResult.rows.get.map {
//            item: RowData => Role(item)
//          }
//          roleList
//        }
//
//        val roleListUserA = Await.result(resultForUserA, Duration.Inf)
//        roleListUserA contains TestValues.testRoleG should be (false)
//
//        // Find roles for TestValues.testUserB
//        val resultForUserB = db.pool.sendPreparedStatement(find_roles_query, Array[Any](TestValues.testUserB.id.bytes)).map { queryResult =>
//          val roleList = queryResult.rows.get.map {
//            item: RowData => Role(item)
//          }
//          roleList
//        }
//
//        val roleListUserB = Await.result(resultForUserB, Duration.Inf)
//        roleListUserB contains TestValues.testRoleG should be (false)
//      }
//      "return FALSE if role (object) doesn't exist" in {
//        val query_result = roleRepository.removeFromAllUsers(TestValues.testRoleE)
//
//        Await.result(query_result, Duration.Inf) should be (false)
//      }
//      "return FALSE if role (name) doesn't exist" in {
//        val query_result = roleRepository.removeFromAllUsers(TestValues.testRoleE.name)
//
//        Await.result(query_result, Duration.Inf) should be (false)
//      }
//      "return FALSE if no one from users doesn't have this role (object)" in {
//        val query_result = roleRepository.removeFromAllUsers(TestValues.testRoleG)
//
//        Await.result(query_result, Duration.Inf) should be (false)
//      }
//      "return FALSE if no one from users doesn't have this role (name)" in {
//        val query_result = roleRepository.removeFromAllUsers(TestValues.testRoleG.name)
//
//        Await.result(query_result, Duration.Inf) should be (false)
//      }
//    }
//  }
}
