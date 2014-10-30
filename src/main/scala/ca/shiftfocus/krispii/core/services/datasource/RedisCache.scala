package ca.shiftfocus.krispii.core.services.datasource

import com.redis._
import scala.concurrent.duration._

trait RedisCache {

  val redis = new RedisClient("localhost", 6379)

}
