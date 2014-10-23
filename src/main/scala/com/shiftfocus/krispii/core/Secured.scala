/*
 * Copyright (c) 2014 - ShiftFocus Media Inc.
 * This source code file is part of the Krispii software package
 * and is protected by copyright.
 * All Rights Reserved.
 */

package controllers.front

import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import com.shiftfocus.krispii.core.models._
import com.shiftfocus.krispii.core.services.{AuthServiceComponent, SchoolServiceComponent}
import com.shiftfocus.krispii.core.lib.{ExceptionWriter, UUID}
import play.api._
import play.api.mvc._
import play.api.mvc.Results.InternalServerError
import play.api.libs.json.{JsObject, Json}
import play.api.data._
import play.api.data.Forms._
import play.api.Logger
import play.api.mvc.BodyParsers._
import scala.Some
import play.api.i18n.Messages
import net.sf.uadetector.service.UADetectorServiceFactory
import net.sf.uadetector.UserAgent
import net.sf.uadetector.UserAgentStringParser
import scala.concurrent.Future
import webcrank.password._

/**
 * Provide security features
 */
trait Secured
  extends AuthServiceComponent
  with SchoolServiceComponent {

  def param(field: String)(implicit request: RequestHeader): Option[String] =
    request.queryString.get(field).flatMap(_.headOption)

  /**
   * To ensure consistent naming (and help prevent errors caused by typos), the possible user
   * roles should be stored as constants here. That is until we have to produce a system
   * complex enough that necessitates a dynamic, database-driven role system.
   */
  object Roles {
    val Administrator = "administrator"
    val Authenticated = "authenticated"
    val Student       = "student"
    val Teacher       = "teacher"
  }

  /**
   * Shorthand type for the function signature that our controller action bodies have,
   * to make the below methods easier to read.
   */
  type AuthActionBody = Context => Request[AnyContent] => Future[SimpleResult]
  type MaybeAuthActionBody = Option[User] => Request[AnyContent] => Future[SimpleResult]


  /**
   * Retrieve the connected user ID.
   */
  private def userId(request: RequestHeader) = request.session.get("user_id")


  /**
   * Redirect to login if the user in not authorized.
   */
  private def onUnauthorized(request: RequestHeader) = Results.Redirect("/login")

  // --

  /**
   * A copy of Security.Authenticated that, on authentication failure, rather than
   * calling onUnauthorized, still calls the action but passing either Some(user) or None.
   *
   * Use this when you need to check for an authenticated user, but you still need
   * anonymous users to pass through.
   *
   * @param userinfo
   * @param action
   * @tparam A
   * @return
   */
  def CheckIfAuthenticated[A](userinfo: RequestHeader => Option[A])(action: Option[A] => EssentialAction): EssentialAction = {
    EssentialAction { request =>
      userinfo(request).map { user =>
        action(Some(user))(request)
      }.getOrElse {
        action(None)(request)
      }
    }
  }

  def IsAuthenticatedApi[A](requiredRoles: IndexedSeq[String], bodyParser: BodyParser[A])(f: => Context => Request[A] => Future[SimpleResult]): EssentialAction = {
    Security.Authenticated(userId, onUnauthorized) { userIdString =>
      val userId = UUID(userIdString)
      Action.async(bodyParser) { request =>
        val result = for {
          maybeUser <- authService.find(userId)
          result <- maybeUser match {
            case Some(userInfo) => {
              val someSessionId = request.session.get("session_id")
              val someOtherSessions = request.session.get("other_sessions")
              val studentSingleLogin = Play.current.configuration.getBoolean("sessions.students.singleLogin").getOrElse(false)

              if (!studentSingleLogin ||
                (!someOtherSessions.isDefined ||
                  (request.path == "/kickSessions" ||
                   request.path == "/logout") ||
                  userInfo.roles.map(_.name).intersect(IndexedSeq(Roles.Administrator, Roles.Teacher)).nonEmpty)
              ) {
                if (userInfo.roles.map(_.name.toLowerCase).intersect(requiredRoles).nonEmpty &&
                  someSessionId.isDefined &&
                  UUID.isValid(someSessionId.get)
                ) {
                  val sessionId = UUID(someSessionId.get)
                  val session = authService.findSession(sessionId).get
                  val uaParser = UADetectorServiceFactory.getResourceModuleParser()
                  val agent = uaParser.parse(request.headers.get("User-Agent").getOrElse(""))
                  val browser = agent.getProducer() + " " + agent.getFamily().getName() + " " + agent.getVersionNumber().toVersionString()
                  val updatedSession = authService.updateSession(session.sessionId, request.remoteAddress, userAgent = browser)
                  val context = Context(userInfo.user, updatedSession, userInfo.roles.map(_.name), userInfo.sections)
                  //Logbook.log("Authenticated HTTP request")(request, context)
                  f(context)(request)
                }
                else {
                  Future.successful(Results.Redirect("/login"))
                }
              }
              else {
                Logger.debug("[Secured.IsAuthenticated] - Already logged in, redirecting to kickSessions")
                Future.successful(Results.Redirect("/kickSessions"))
              }
            }
            case None => {
              Logger.debug("[Secured.IsAuthenticated] - User not found.")
              Future.successful(Results.Redirect("/login"))
            }
          }
        } yield result

        // For API requests, catch all exceptions and write them to the error log.
        // Clients just need to receive an HTTP 500 response.
        result.recover {
          case exception => {
            Logger.error(ExceptionWriter.print(exception))
            InternalServerError("The little hamster who was powering our server passed away while serving your API request. Our S.F.A. team has been notified.")
          }
        }
      }
    }
  }

  /*
   * !!! FYI !!!
   *
   * The following "IsAuthenticated" methods are simply aliases to the above
   * method. The goal is to provide a flexible API so that both user roles
   * and body parsers can (optionally) be specified. The valid combinations are:
   *
   *   IsAuthenticated
   *   IsAuthenticated(role: String)
   *   IsAuthenticated(bodyParser: BodyParser[A])
   *   IsAuthenticated(role: String, bodyParser: BodyParser[A])
   *
   *   which are all aliases to the general case...
   *
   *   IsAuthenticated(roles: Seq[String], bodyParser: BodyParser[A])
   */

  /**
   * No roles or parser specified. Defaults to the authenticated role and the
   * AnyContent parser.
   *
   * Usage:
   *   def index = IsAuthenticated { implicit user => implicit request => implicit ctx =>
   *     Future.successful(Ok("Response"))
   *   }
   */
  def IsAuthenticatedApi(f: => Context => Request[AnyContent] => Future[SimpleResult]): EssentialAction = {
    IsAuthenticatedApi(IndexedSeq(Roles.Authenticated), parse.anyContent)(f)
  }

  /**
   * Roles specified, but no parser. Defaults to the AnyContent parser.
   *
   * Usage:
   *   def index = IsAuthenticated(Roles.Administrator) { implicit user => implicit request => implicit ctx =>
   *     Future.successful(Ok("Response"))
   *   }
   */
  def IsAuthenticatedApi(role: String)(f: => Context => Request[AnyContent] => Future[SimpleResult]): EssentialAction = {
    IsAuthenticatedApi(IndexedSeq(role), parse.anyContent)(f)
  }

  /**
   * Roles unspecified, but parser given. Defaults to the Authenticated user.
   *
   * Usage:
   *   def index = IsAuthenticated(Roles.Administrator) { implicit user => implicit request => implicit ctx =>
   *     Future.successful(Ok("Response"))
   *   }
   */
  def IsAuthenticatedApi[A](bodyParser: BodyParser[A])(f: => Context => Request[A] => Future[SimpleResult]): EssentialAction = {
    IsAuthenticatedApi(IndexedSeq(Roles.Authenticated), bodyParser)(f)
  }

  /**
   * Roles specified, but no parser. Defaults to the AnyContent parser.
   *
   * Usage:
   *   def index = IsAuthenticated(Roles.Administrator, parse.json) { implicit user => implicit request => implicit ctx =>
   *     Future.successful(Ok("Response"))
   *   }
   */
  def IsAuthenticatedApi[A](role: String, bodyParser: BodyParser[A])(f: => Context => Request[A] => Future[SimpleResult]): EssentialAction = {
    IsAuthenticatedApi(IndexedSeq(role), bodyParser)(f)
  }

}
