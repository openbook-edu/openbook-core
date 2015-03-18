//import java.awt.Color
//import java.io.File
//import ca.shiftfocus.krispii.core.models.Class
//import ca.shiftfocus.krispii.core.repositories.{UserRepositoryComponent, ClassRepositoryPostgresComponent}
//import ca.shiftfocus.krispii.core.services.datasource.PostgresDB
//import ca.shiftfocus.uuid.UUID
//import com.github.mauricio.async.db.RowData
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
//
//class ClassRepositorySpec
//  extends TestEnvironment
//{
//  "ClassRepository.list" should {
//    inSequence {
//      "list all classes" in {
//        val result = classRepository.list
//
//        val classes = Await.result(result, Duration.Inf)
//
//        classes.toString should be(Vector(TestValues.testClassA, TestValues.testClassB, TestValues.testClassD, TestValues.testClassF).toString)
//
//        Map[Int, Class](0 -> TestValues.testClassA, 1 -> TestValues.testClassB, 2 -> TestValues.testClassD, 3 -> TestValues.testClassF).foreach {
//          case (key, clas: Class) => {
//            classes(key).id should be(clas.id)
//            classes(key).teacherId should be(clas.teacherId)
//            classes(key).name should be(clas.name)
//            classes(key).color should be(clas.color)
//          }
//        }
//      }
//      "return class by its project" in {
//        val result = classRepository.list(TestValues.testProjectA)
//
//        val classes = Await.result(result, Duration.Inf)
//
//        classes.toString should be(Vector(TestValues.testClassA).toString)
//
//        Map[Int, Class](0 -> TestValues.testClassA).foreach {
//          case (key, clas: Class) => {
//            classes(key).id should be(clas.id)
//            classes(key).teacherId should be(clas.teacherId)
//            classes(key).name should be(clas.name)
//            classes(key).color should be(clas.color)
//          }
//        }
//      }
//      "return empty Vector() for unexisting project" in {
//        val result = classRepository.list(TestValues.testProjectD)
//        val classes = Await.result(result, Duration.Inf)
//
//        classes should be (Vector())
//      }
//      "select classes if user is not teacher for any class and has record in 'users_classes' table and (asTeacher = FALSE)" in {
//        val result = classRepository.list(TestValues.testUserE, false)
//
//        val classes = Await.result(result, Duration.Inf)
//
//        Map[Int, Class](0 -> TestValues.testClassB).foreach {
//          case (key, clas: Class) => {
//            classes(key).id should be(clas.id)
//            classes(key).teacherId should be(clas.teacherId)
//            classes(key).name should be(clas.name)
//            classes(key).color should be(clas.color)
//          }
//        }
//      }
//      "return empty Vector() if user is not teacher for any class and has record in 'users_classes' table and (asTeacher = TRUE)" in {
//        val result = classRepository.list(TestValues.testUserE, true)
//        val classes = Await.result(result, Duration.Inf)
//
//        classes should be(Vector())
//      }
//      "select classes if user is teacher for any class and has record in 'users_classes' table and (asTeacher = TRUE)" in {
//        val result = classRepository.list(TestValues.testUserA, true)
//
//        val classes = Await.result(result, Duration.Inf)
//
//        Map[Int, Class](0 -> TestValues.testClassA).foreach {
//          case (key, clas: Class) => {
//            classes(key).id should be(clas.id)
//            classes(key).teacherId should be(clas.teacherId)
//            classes(key).name should be(clas.name)
//            classes(key).color should be(clas.color)
//          }
//        }
//      }
//      "select classes if user is teacher for any class table and (asTeacher = TRUE)" in {
//        val result = classRepository.list(TestValues.testUserA, true)
//
//        val classes = Await.result(result, Duration.Inf)
//
//        Map[Int, Class](0 -> TestValues.testClassA).foreach {
//          case (key, clas: Class) => {
//            classes(key).id should be(clas.id)
//            classes(key).teacherId should be(clas.teacherId)
//            classes(key).name should be(clas.name)
//            classes(key).color should be(clas.color)
//          }
//        }
//      }
//      "return empty Vector() if user is teacher for any class and hasn't a record in 'users_classes' table and (asTeacher = FALSE)" in {
//        val result = classRepository.list(TestValues.testUserF, false)
//        val classes = Await.result(result, Duration.Inf)
//
//        classes should be(Vector())
//      }
//      "return empty Vector() if user unexists and (asTeacher = FALSE)" in {
//        val result = classRepository.list(TestValues.testUserD, false)
//        val classes = Await.result(result, Duration.Inf)
//
//        classes should be(Vector())
//      }
//      "return empty Vector() if user unexists and (asTeacher = TRUE)" in {
//        val result = classRepository.list(TestValues.testUserD, true)
//        val classes = Await.result(result, Duration.Inf)
//
//        classes should be(Vector())
//      }
//      "list the classes associated with a users" in {
//        val result = classRepository.list(Vector(TestValues.testUserA, TestValues.testUserB))
//        val classes = Await.result(result, Duration.Inf)
//
//        val classesUserA = classes(TestValues.testUserA.id)
//        val classesUserB = classes(TestValues.testUserB.id)
//
//        Map[Int, Class](0 -> TestValues.testClassA).foreach {
//          case (key, clas: Class) => {
//            classesUserA(key).id should be(clas.id)
//            classesUserA(key).teacherId should be(clas.teacherId)
//            classesUserA(key).name should be(clas.name)
//            classesUserA(key).color should be(clas.color)
//          }
//        }
//        Map[Int, Class](0 -> TestValues.testClassB).foreach {
//          case (key, clas: Class) => {
//            classesUserB(key).id should be(clas.id)
//            classesUserB(key).teacherId should be(clas.teacherId)
//            classesUserB(key).name should be(clas.name)
//            classesUserB(key).color should be(clas.color)
//          }
//        }
//      }
//      "return empty Vector() if user doesn't exist" in {
//        val result = classRepository.list(Vector(TestValues.testUserD))
//        val classes = Await.result(result, Duration.Inf)
//
//        val classesUserD = classes(TestValues.testUserD.id)
//
//        classesUserD should be (Vector())
//      }
//      "return empty Vector() if user is teacher and doesn't have references in users_classes table" in {
//        val result = classRepository.list(Vector(TestValues.testUserF))
//        val classes = Await.result(result, Duration.Inf)
//
//        val classesUserF = classes(TestValues.testUserF.id)
//
//        classesUserF should be (Vector())
//      }
//    }
//  }
//
////  "ClassRepository.find" should {
////    inSequence {
////      "find a single entry by ID" in {
////        val result = classRepository.find(TestValues.testClassA.id).map(_.get)
////
////        val clas = Await.result(result, Duration.Inf)
////        clas.id should be(TestValues.testClassA.id)
////        clas.version should be(TestValues.testClassA.version)
////        clas.name should be(TestValues.testClassA.name)
////        clas.createdAt.toString should be(TestValues.testClassA.createdAt.toString)
////        clas.updatedAt.toString should be(TestValues.testClassA.updatedAt.toString)
////      }
////      "be NONE if entry wasn't found by ID" in {
////        val result = classRepository.find(UUID("f9aadc67-5e8b-48f3-b0a2-20a0d7d88477"))
////
////        Await.result(result, Duration.Inf) should be (None)
////      }
////    }
////  }
////
////  "ClassRepository.findUserForTeacher" should {
////    inSequence {
////      "find student in teacher's class" in {
////        val result = classRepository.findUserForTeacher(TestValues.testUserE, TestValues.testUserB).map(_.get)
////
////        val user = Await.result(result, Duration.Inf)
////        user should be (TestValues.testUserE)
////      }
////      "return NONE if student isn't in the teacher's class" in {
////        val result = classRepository.findUserForTeacher(TestValues.testUserE, TestValues.testUserA)
////
////        Await.result(result, Duration.Inf) should be (None)
////      }
////    }
////  }
////
////  "ClassRepository.addUser" should {
////    inSequence {
////      "add user to a class" in {
////        val result = classRepository.addUser(TestValues.testUserC, TestValues.testClassA)
////        Await.result(result, Duration.Inf) should be(true)
////
////        // Check
////        val queryResult = db.pool.sendPreparedStatement(countUserInClass, Array[Any](TestValues.testUserC.id.bytes, TestValues.testClassA.id.bytes))
////        val rowsAffected = Await.result(queryResult, Duration.Inf).rowsAffected
////
////        rowsAffected should be (1)
////      }
////      "throw a GenericDatabaseException if user is already in the class" in {
////        val result = classRepository.addUser(TestValues.testUserE, TestValues.testClassB)
////
////        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
////      }
////      "throw a GenericDatabaseException if user unexists" in {
////        val result = classRepository.addUser(TestValues.testUserD, TestValues.testClassB)
////
////        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
////      }
////      "throw a GenericDatabaseException if class unexists" in {
////        val result = classRepository.addUser(TestValues.testUserE, TestValues.testClassC)
////
////        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
////      }
////    }
////  }
////
////  "ClassRepository.removeUser" should {
////    inSequence {
////      "remove a user from a class " in {
////        // Check if user present
////        val queryResultIs = db.pool.sendPreparedStatement(countUserInClass, Array[Any](TestValues.testUserA.id.bytes, TestValues.testClassA.id.bytes))
////        val rowsAffectedIs = Await.result(queryResultIs, Duration.Inf).rowsAffected
////
////        rowsAffectedIs should be (1)
////
////        val result = classRepository.removeUser(TestValues.testUserA, TestValues.testClassA)
////        Await.result(result, Duration.Inf) should be(true)
////
////        // Check
////        val queryResult = db.pool.sendPreparedStatement(countUserInClass, Array[Any](TestValues.testUserA.id.bytes, TestValues.testClassA.id.bytes))
////        val rowsAffected = Await.result(queryResult, Duration.Inf).rowsAffected
////
////        rowsAffected should be (0)
////      }
////      "be FALSE if user unexists" in {
////        val result = classRepository.removeUser(TestValues.testUserD, TestValues.testClassB)
////
////        Await.result(result, Duration.Inf) should be(false)
////      }
////      "be FALSE if class unexists" in {
////        val result = classRepository.removeUser(TestValues.testUserA, TestValues.testClassC)
////
////        Await.result(result, Duration.Inf) should be(false)
////      }
////    }
////  }
////
////  "ClassRepository.hasProject" should {
////    inSequence {
////      "verify if this user has access to this project through any of his classes" in {
////        val result = classRepository.hasProject(TestValues.testUserE, TestValues.testProjectB)
////
////        Await.result(result, Duration.Inf) should be(true)
////      }
////      "be FALSE if user is not in a project's class" in {
////        val result = classRepository.hasProject(TestValues.testUserE, TestValues.testProjectA)
////
////        Await.result(result, Duration.Inf) should be(false)
////      }
////      "be FALSE if user unexists" in {
////        val result = classRepository.hasProject(TestValues.testUserD, TestValues.testProjectA)
////
////        Await.result(result, Duration.Inf) should be(false)
////      }
////      "be FALSE if project unexists" in {
////        val result = classRepository.hasProject(TestValues.testUserD, TestValues.testProjectD)
////
////        Await.result(result, Duration.Inf) should be(false)
////      }
////    }
////  }
////
////  "ClassRepository.addUsers" should {
////    inSequence {
////      "add users to a `class`" in {
////        val result = classRepository.addUsers(TestValues.testClassD, Vector(TestValues.testUserA, TestValues.testUserB))
////
////        Await.result(result, Duration.Inf) should be(true)
////
////        // Check User A in class D
////        val queryResultUserA = db.pool.sendPreparedStatement(countUserInClass, Array[Any](TestValues.testUserA.id.bytes, TestValues.testClassD.id.bytes))
////        val rowsAffectedUserA = Await.result(queryResultUserA, Duration.Inf).rowsAffected
////
////        rowsAffectedUserA should be (1)
////
////        // Check User B in class D
////        val queryResultUserB = db.pool.sendPreparedStatement(countUserInClass, Array[Any](TestValues.testUserB.id.bytes, TestValues.testClassD.id.bytes))
////        val rowsAffectedUserB = Await.result(queryResultUserB, Duration.Inf).rowsAffected
////
////        rowsAffectedUserB should be (1)
////      }
////      "throw a GenericDatabaseException if user is already in the class" in {
////        val result = classRepository.addUsers(TestValues.testClassB, Vector(TestValues.testUserE))
////
////        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
////      }
////      "throw a GenericDatabaseException if user unexists" in {
////        val result = classRepository.addUsers(TestValues.testClassB, Vector(TestValues.testUserD))
////
////        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
////      }
////      "throw a GenericDatabaseException if class unexists" in {
////        val result = classRepository.addUsers(TestValues.testClassC, Vector(TestValues.testUserE))
////
////        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
////      }
////    }
////  }
////
////  "ClassRepository.removeUsers" should {
////    inSequence {
////      "Remove users from a `class`" in {
////        // Check User B in class B
////        val queryResultIsUserB = db.pool.sendPreparedStatement(countUserInClass, Array[Any](TestValues.testUserB.id.bytes, TestValues.testClassB.id.bytes))
////        val rowsAffectedIsUserB = Await.result(queryResultIsUserB, Duration.Inf).rowsAffected
////
////        rowsAffectedIsUserB should be (1)
////
////        // Check User E in class B
////        val queryResultIsUserE = db.pool.sendPreparedStatement(countUserInClass, Array[Any](TestValues.testUserE.id.bytes, TestValues.testClassB.id.bytes))
////        val rowsAffectedIsUserE = Await.result(queryResultIsUserE, Duration.Inf).rowsAffected
////
////        rowsAffectedIsUserE should be (1)
////
////        val result = classRepository.removeUsers(TestValues.testClassB, Vector(TestValues.testUserB, TestValues.testUserE))
////
////        Await.result(result, Duration.Inf) should be(true)
////
////        // Check User B in class B
////        val queryResultUserB = db.pool.sendPreparedStatement(countUserInClass, Array[Any](TestValues.testUserB.id.bytes, TestValues.testClassB.id.bytes))
////        val rowsAffectedUserB = Await.result(queryResultUserB, Duration.Inf).rowsAffected
////
////        rowsAffectedUserB should be (0)
////
////        // Check User E in class B
////        val queryResultUserE = db.pool.sendPreparedStatement(countUserInClass, Array[Any](TestValues.testUserE.id.bytes, TestValues.testClassB.id.bytes))
////        val rowsAffectedUserE = Await.result(queryResultUserE, Duration.Inf).rowsAffected
////
////        rowsAffectedUserE should be (0)
////      }
////      "be FALSE if user is not in the class" in {
////        val result = classRepository.removeUsers(TestValues.testClassA, Vector(TestValues.testUserE))
////
////        Await.result(result, Duration.Inf) should be(false)
////      }
////      "be FALSE if user unexists" in {
////        val result = classRepository.removeUsers(TestValues.testClassB, Vector(TestValues.testUserD))
////
////        Await.result(result, Duration.Inf) should be(false)
////      }
////      "be FALSE if class unexists" in {
////        val result = classRepository.removeUsers(TestValues.testClassC, Vector(TestValues.testUserE))
////
////        Await.result(result, Duration.Inf) should be(false)
////      }
////    }
////  }
////
////  "ClassRepository.removeAllUsers" should {
////    inSequence {
////      "remove all users from a `class`" in {
////        // Check User G in class B
////        val queryResultIsUserG = db.pool.sendPreparedStatement(countUserInClass, Array[Any](TestValues.testUserG.id.bytes, TestValues.testClassB.id.bytes))
////        val rowsAffectedIsUserG = Await.result(queryResultIsUserG, Duration.Inf).rowsAffected
////
////        rowsAffectedIsUserG should be (1)
////
////        // Check User H in class B
////        val queryResultIsUserH = db.pool.sendPreparedStatement(countUserInClass, Array[Any](TestValues.testUserH.id.bytes, TestValues.testClassB.id.bytes))
////        val rowsAffectedIsUserH = Await.result(queryResultIsUserH, Duration.Inf).rowsAffected
////
////        rowsAffectedIsUserH should be (1)
////
////
////        val result = classRepository.removeAllUsers(TestValues.testClassB)
////
////        Await.result(result, Duration.Inf) should be(true)
////
////        // Check User G in class B
////        val queryResultUserG = db.pool.sendPreparedStatement(countUserInClass, Array[Any](TestValues.testUserG.id.bytes, TestValues.testClassB.id.bytes))
////        val rowsAffectedUserG = Await.result(queryResultUserG, Duration.Inf).rowsAffected
////
////        rowsAffectedUserG should be (0)
////
////        // Check User H in class B
////        val queryResultUserH = db.pool.sendPreparedStatement(countUserInClass, Array[Any](TestValues.testUserH.id.bytes, TestValues.testClassB.id.bytes))
////        val rowsAffectedUserH = Await.result(queryResultUserH, Duration.Inf).rowsAffected
////
////        rowsAffectedUserH should be (0)
////      }
////      "be FALSE if class unexists" in {
////        val result = classRepository.removeAllUsers(TestValues.testClassC)
////
////        Await.result(result, Duration.Inf) should be(false)
////      }
////    }
////  }
////
////  "ClassRepository.insert" should {
////    inSequence {
////      "insert new class" in {
////        val result = classRepository.insert(TestValues.testClassE)
////
////        val newClass = Await.result(result, Duration.Inf)
////
////        newClass.id should be (TestValues.testClassE.id)
////        newClass.teacherId should be (TestValues.testClassE.teacherId)
////        newClass.version should be (1L)
////        newClass.name should be (TestValues.testClassE.name)
////        newClass.color should be (TestValues.testClassE.color)
////
////        // Check
////        val queryResult = db.pool.sendPreparedStatement(SelectOne, Array[Any](TestValues.testClassE.id.bytes)).map { queryResult =>
////          val classList = queryResult.rows.get.map {
////            item: RowData => Class(item)
////          }
////          classList
////        }
////
////        val classList = Await.result(queryResult, Duration.Inf)
////
////        classList(0).id should be(TestValues.testClassE.id)
////        classList(0).teacherId should be(TestValues.testClassE.teacherId)
////        classList(0).version should be(1L)
////        classList(0).name should be(TestValues.testClassE.name)
////        classList(0).color should be(TestValues.testClassE.color)
////      }
////      "throw a GenericDatabaseException if class has unexisting teacher id" in {
////        val result = classRepository.insert(TestValues.testClassC.copy(
////          teacherId = Option(UUID("9d8ed645-b055-4a69-ab7d-387791c1e064"))
////        ))
////
////        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
////      }
////      "throw a GenericDatabaseException if class already exists" in {
////        val result = classRepository.insert(TestValues.testClassA)
////
////        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
////      }
////    }
////  }
////
////  "ClassRepository.update" should {
////    inSequence {
////      "update existing class" in {
////        val result = classRepository.update(TestValues.testClassA.copy(
////          teacherId = Option(TestValues.testUserG.id),
////          name = "new test class A name",
////          color = new Color(78, 40, 23)
////        ))
////
////        val updatedClass = Await.result(result, Duration.Inf)
////
////        updatedClass.id should be (TestValues.testClassA.id)
////        updatedClass.teacherId should be (Some(TestValues.testUserG.id)) // TODO - check why SOME?
////        updatedClass.version should be (TestValues.testClassA.version + 1)
////        updatedClass.name should be ("new test class A name")
////        updatedClass.color should be (new Color(78, 40, 23))
////
////        // Check
////        val queryResult = db.pool.sendPreparedStatement(SelectOne, Array[Any](TestValues.testClassA.id.bytes)).map { queryResult =>
////          val classList = queryResult.rows.get.map {
////            item: RowData => Class(item)
////          }
////          classList
////        }
////
////        val classList = Await.result(queryResult, Duration.Inf)
////
////        classList(0).id should be(TestValues.testClassA.id)
////        classList(0).teacherId should be(Some(TestValues.testUserG.id))
////        classList(0).version should be(TestValues.testClassA.version + 1)
////        classList(0).name should be("new test class A name")
////        classList(0).color should be(new Color(78, 40, 23))
////      }
////      "throw a NoSuchElementException when update an existing Class with wrong version" in {
////        val result = classRepository.update(TestValues.testClassA.copy(
////          version = 99L,
////          name = "new test class A name"
////        ))
////
////        an[java.util.NoSuchElementException] should be thrownBy Await.result(result, Duration.Inf)
////      }
////      "throw a NoSuchElementException when update an unexisting Class" in {
////        val result = classRepository.update(Class(
////          teacherId = Option(TestValues.testUserG.id),
////          name = "new test class A name",
////          color = new Color(8, 4, 3)
////        ))
////
////        an[java.util.NoSuchElementException] should be thrownBy Await.result(result, Duration.Inf)
////      }
////    }
////  }
////
////  "ClassRepository.delete" should {
////    inSequence {
////      "delete class if class has no references" in {
////        val result = classRepository.delete(TestValues.testClassF)
////
////        Await.result(result, Duration.Inf) should be(true)
////
////        // Check
////        val queryResult = db.pool.sendPreparedStatement(SelectOne, Array[Any](TestValues.testClassF.id.bytes)).map { queryResult =>
////          val classList = queryResult.rows.get.map {
////            item: RowData => Class(item)
////          }
////          classList
////        }
////
////        Await.result(queryResult, Duration.Inf) should be(Vector())
////      }
////      "delete class if class has references in users_classes table" in {
////        val result = classRepository.delete(TestValues.testClassD)
////
////        Await.result(result, Duration.Inf) should be(true)
////
////        // Check
////        val queryResult = db.pool.sendPreparedStatement(SelectOne, Array[Any](TestValues.testClassD.id.bytes)).map { queryResult =>
////          val classList = queryResult.rows.get.map {
////            item: RowData => Class(item)
////          }
////          classList
////        }
////
////        Await.result(queryResult, Duration.Inf) should be(Vector())
////      }
////      "throw a GenericDatabaseException if class has references in projects table" in {
////        val result = classRepository.delete(TestValues.testClassB)
////
////        an [com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException] should be thrownBy Await.result(result, Duration.Inf)
////      }
////      "return FALSE if Class hasn't been found" in {
////        val result = classRepository.delete(Class(
////          teacherId = Option(TestValues.testUserG.id),
////          name = "new test class A name",
////          color = new Color(8, 4, 3)
////        ))
////
////        Await.result(result, Duration.Inf) should be(false)
////      }
////    }
////  }
////
////  // TODO - delete
////  "ClassRepository.enablePart"  + Console.RED + Console.BOLD + " (NOTE: Method is deprecated, talbe CLASSES_PROJECTS will be deleted) " + Console.RESET should {
////    inSequence {
////      "enable a particular project part for this Section's users" in {
////
////      }
////    }
////  }
////
////  // TODO - delete
////  "ClassRepository.disablePart" + Console.RED + Console.BOLD + " (NOTE: Method is deprecated, talbe CLASSES_PROJECTS will be deleted) " + Console.RESET should {
////    inSequence {
////      "disable a particular project part for this Section's users" in {
////
////      }
////    }
////  }
//}
