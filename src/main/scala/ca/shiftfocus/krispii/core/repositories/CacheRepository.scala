package ca.shiftfocus.krispii.core.repositories

import java.io.{ByteArrayInputStream, ObjectInputStream, Serializable}
import java.util.UUID

import ca.shiftfocus.krispii.core.lib.{ScalaCacheConfig, ScalaCachePool}
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.group.{Course, Exam, Team}
import ca.shiftfocus.krispii.core.models.tasks.Task
import ca.shiftfocus.krispii.core.models.user.{Scorer, User}
import ca.shiftfocus.krispii.core.models.work.{Score, Test}
import scalacache.serialization.Codec
import scalacache.serialization.Codec._
import scalacache.serialization.binary.{JavaSerializationAnyRefCodec, _}

import scala.reflect.ClassTag

case class CacheRepository(
    val scalaCacheConfig: ScalaCacheConfig
) extends SerializationSeqBinary {
  // we don't actually cache pure strings nor integers
  val cacheString: ScalaCachePool[String] = new ScalaCachePool[String](scalaCacheConfig)
  val cacheSeqString: ScalaCachePool[IndexedSeq[String]] = new ScalaCachePool[IndexedSeq[String]](scalaCacheConfig)

  val cacheInt: ScalaCachePool[Int] = new ScalaCachePool[Int](scalaCacheConfig)
  val cacheSeqInt: ScalaCachePool[IndexedSeq[Int]] = new ScalaCachePool[IndexedSeq[Int]](scalaCacheConfig)

  val cacheUUID: ScalaCachePool[UUID] = new ScalaCachePool[UUID](scalaCacheConfig)
  // we don't actually reference sequences of records by their UUIDs
  val cacheSeqUUID: ScalaCachePool[IndexedSeq[UUID]] = new ScalaCachePool[IndexedSeq[UUID]](scalaCacheConfig)

  val cacheCourse: ScalaCachePool[Course] = new ScalaCachePool[Course](scalaCacheConfig)
  val cacheSeqCourse: ScalaCachePool[IndexedSeq[Course]] = new ScalaCachePool[IndexedSeq[Course]](scalaCacheConfig)

  val cacheAccount: ScalaCachePool[Account] = new ScalaCachePool[Account](scalaCacheConfig)
  val cacheSeqAccount: ScalaCachePool[IndexedSeq[Account]] = new ScalaCachePool[IndexedSeq[Account]](scalaCacheConfig)

  val cacheUser: ScalaCachePool[User] = new ScalaCachePool[User](scalaCacheConfig)
  val cacheSeqUser: ScalaCachePool[IndexedSeq[User]] = new ScalaCachePool[IndexedSeq[User]](scalaCacheConfig)

  val cacheGroupSchedule: ScalaCachePool[GroupSchedule] = new ScalaCachePool[GroupSchedule](scalaCacheConfig)
  val cacheSeqGroupSchedule: ScalaCachePool[IndexedSeq[GroupSchedule]] = new ScalaCachePool[IndexedSeq[GroupSchedule]](scalaCacheConfig)

  val cacheGroupScheduleException: ScalaCachePool[GroupScheduleException] = new ScalaCachePool[GroupScheduleException](scalaCacheConfig)
  val cacheSeqGroupScheduleException: ScalaCachePool[IndexedSeq[GroupScheduleException]] = new ScalaCachePool[IndexedSeq[GroupScheduleException]](scalaCacheConfig)

  val cacheExam: ScalaCachePool[Exam] = new ScalaCachePool[Exam](scalaCacheConfig)
  val cacheSeqExam: ScalaCachePool[IndexedSeq[Exam]] = new ScalaCachePool[IndexedSeq[Exam]](scalaCacheConfig)

  val cachePart: ScalaCachePool[Part] = new ScalaCachePool[Part](scalaCacheConfig)
  val cacheSeqPart: ScalaCachePool[IndexedSeq[Part]] = new ScalaCachePool[IndexedSeq[Part]](scalaCacheConfig)

  val cacheProject: ScalaCachePool[Project] = new ScalaCachePool[Project](scalaCacheConfig)
  val cacheSeqProject: ScalaCachePool[IndexedSeq[Project]] = new ScalaCachePool[IndexedSeq[Project]](scalaCacheConfig)

  val cacheRole: ScalaCachePool[Role] = new ScalaCachePool[Role](scalaCacheConfig)
  val cacheSeqRole: ScalaCachePool[IndexedSeq[Role]] = new ScalaCachePool[IndexedSeq[Role]](scalaCacheConfig)

  val cacheScore: ScalaCachePool[Score] = new ScalaCachePool[Score](scalaCacheConfig)
  val cacheSeqScore: ScalaCachePool[IndexedSeq[Score]] = new ScalaCachePool[IndexedSeq[Score]](scalaCacheConfig)

  val cacheScorer: ScalaCachePool[Scorer] = new ScalaCachePool[Scorer](scalaCacheConfig)
  val cacheSeqScorer: ScalaCachePool[IndexedSeq[Scorer]] = new ScalaCachePool[IndexedSeq[Scorer]](scalaCacheConfig)

  val cacheSession: ScalaCachePool[Session] = new ScalaCachePool[Session](scalaCacheConfig)
  val cacheSeqSession: ScalaCachePool[IndexedSeq[Session]] = new ScalaCachePool[IndexedSeq[Session]](scalaCacheConfig)

  val cacheTag: ScalaCachePool[Tag] = new ScalaCachePool[Tag](scalaCacheConfig)
  val cacheSeqTag: ScalaCachePool[IndexedSeq[Tag]] = new ScalaCachePool[IndexedSeq[Tag]](scalaCacheConfig)

  val cacheTask: ScalaCachePool[Task] = new ScalaCachePool[Task](scalaCacheConfig)
  val cacheSeqTask: ScalaCachePool[IndexedSeq[Task]] = new ScalaCachePool[IndexedSeq[Task]](scalaCacheConfig)

  val cacheTeam: ScalaCachePool[Team] = new ScalaCachePool[Team](scalaCacheConfig)
  val cacheSeqTeam: ScalaCachePool[IndexedSeq[Team]] = new ScalaCachePool[IndexedSeq[Team]](scalaCacheConfig)

  val cacheTest: ScalaCachePool[Test] = new ScalaCachePool[Test](scalaCacheConfig)
  val cacheSeqTest: ScalaCachePool[IndexedSeq[Test]] = new ScalaCachePool[IndexedSeq[Test]](scalaCacheConfig)
}

trait SerializationSeqBinary {
  def codecSeq[IndexedSeq[Any] <: java.io.Serializable](implicit ev: ClassTag[IndexedSeq[Any]]): Codec[IndexedSeq[Any]] = {
    new ScalaSerializationAnyRefCodec[IndexedSeq[Any]](ev)
  }

  def codec[A <: java.io.Serializable](implicit ev: ClassTag[A]): Codec[A] = {
    new ScalaSerializationAnyRefCodec[A](ev)
  }

  // needs to be written here because TaskRepository extends an additional trait
  implicit val codecTask: Codec[Task] = codec.asInstanceOf[Codec[Task]]

  implicit val codecSeqString: Codec[IndexedSeq[String]] = codecSeq.asInstanceOf[Codec[IndexedSeq[String]]]
  implicit val codecSeqInt: Codec[IndexedSeq[Int]] = codecSeq.asInstanceOf[Codec[IndexedSeq[Int]]]
  implicit val codecSeqUUID: Codec[IndexedSeq[UUID]] = codecSeq.asInstanceOf[Codec[IndexedSeq[UUID]]]
  implicit val codecSeqCourse: Codec[IndexedSeq[Course]] = codecSeq.asInstanceOf[Codec[IndexedSeq[Course]]]
  implicit val codecSeqExam: Codec[IndexedSeq[Exam]] = codecSeq.asInstanceOf[Codec[IndexedSeq[Exam]]]
  implicit val codecSeqAccount: Codec[IndexedSeq[Account]] = codecSeq.asInstanceOf[Codec[IndexedSeq[Account]]]
  implicit val codecSeqUser: Codec[IndexedSeq[User]] = codecSeq.asInstanceOf[Codec[IndexedSeq[User]]]
  implicit val codecSeqGroupSchedule: Codec[IndexedSeq[GroupSchedule]] = codecSeq.asInstanceOf[Codec[IndexedSeq[GroupSchedule]]]
  implicit val codecSeqGroupScheduleException: Codec[IndexedSeq[GroupScheduleException]] = codecSeq.asInstanceOf[Codec[IndexedSeq[GroupScheduleException]]]
  implicit val codecSeqPart: Codec[IndexedSeq[Part]] = codecSeq.asInstanceOf[Codec[IndexedSeq[Part]]]
  implicit val codecSeqProject: Codec[IndexedSeq[Project]] = codecSeq.asInstanceOf[Codec[IndexedSeq[Project]]]
  implicit val codecSeqRole: Codec[IndexedSeq[Role]] = codecSeq.asInstanceOf[Codec[IndexedSeq[Role]]]
  implicit val codecSeqScore: Codec[IndexedSeq[Score]] = codecSeq.asInstanceOf[Codec[IndexedSeq[Score]]]
  implicit val codecSeqScorer: Codec[IndexedSeq[Scorer]] = codecSeq.asInstanceOf[Codec[IndexedSeq[Scorer]]]
  implicit val codecSeqSession: Codec[IndexedSeq[Session]] = codecSeq.asInstanceOf[Codec[IndexedSeq[Session]]]
  implicit val codecSeqTag: Codec[IndexedSeq[Tag]] = codecSeq.asInstanceOf[Codec[IndexedSeq[Tag]]]
  implicit val codecSeqTeam: Codec[IndexedSeq[Team]] = codecSeq.asInstanceOf[Codec[IndexedSeq[Team]]]
  implicit val codecSeqTest: Codec[IndexedSeq[Test]] = codecSeq.asInstanceOf[Codec[IndexedSeq[Test]]]
  implicit val codecSeqTask: Codec[IndexedSeq[Task]] = codecSeq.asInstanceOf[Codec[IndexedSeq[Task]]]
}

class ScalaSerializationAnyRefCodec[S <: Serializable](classTag: ClassTag[S]) extends JavaSerializationAnyRefCodec(classTag) {
  override def decode(data: Array[Byte]): DecodingResult[S] = {
    Codec.tryDecode {
      using(new ByteArrayInputStream(data)) { buf =>
        val in = new ObjectInputStream(new ByteArrayInputStream(data))
        using(in) { inp =>
          inp.readObject().asInstanceOf[S]
        }
      }
    }
  }
}
