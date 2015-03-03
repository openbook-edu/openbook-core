package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.lib.FutureMonad
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.mauricio.async.db.Connection

trait Repository extends FutureMonad[RepositoryError.Fail] {

}