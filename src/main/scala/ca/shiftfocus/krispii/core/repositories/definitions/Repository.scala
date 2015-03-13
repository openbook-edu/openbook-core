package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.lib.concurrent.{Serialized, Lifting, FutureMonad}
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.mauricio.async.db.Connection

trait Repository extends Lifting[RepositoryError.Fail] with Serialized with FutureMonad {

}