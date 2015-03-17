package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.lib.concurrent.Lifting

trait Repository extends Lifting[RepositoryError.Fail]