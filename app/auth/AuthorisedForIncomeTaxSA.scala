/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package auth

import config.AppConfig
import connectors.models.Enrolment.{Enrolled, NotEnrolled}
import controllers.{ErrorPageRenderer, ITSASessionKey}
import controllers.ITSASessionKey._
import play.api.mvc.{Action, AnyContent, Request, Result}
import services.EnrolmentService
import uk.gov.hmrc.play.frontend.auth._
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Accounts
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.Implicits._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait AuthorisedForIncomeTaxSA extends Actions with ErrorPageRenderer {

  val enrolmentService: EnrolmentService
  val applicationConfig: AppConfig
  val postSignInRedirectUrl: String
  lazy val alreadyEnrolledUrl: String = applicationConfig.alreadyEnrolledUrl

  private type PlayRequest = Request[AnyContent] => Result
  private type UserRequest = IncomeTaxSAUser => PlayRequest
  private type AsyncPlayRequest = Request[AnyContent] => Future[Result]
  private type AsyncUserRequest = IncomeTaxSAUser => AsyncPlayRequest

  // $COVERAGE-OFF$
  implicit private def hc(implicit request: Request[_]): HeaderCarrier = HeaderCarrier.fromHeadersAndSession(request.headers, Some(request.session))

  // $COVERAGE-ON$

  // this boolean only comes into play if applicationConfig.enableUserDetails = true
  // it is designed so the pages designed to enable user entry of nino do not have the no nino predicate
  val checkNino: Boolean = true

  lazy val visibilityPredicate = new IncomeTaxSACompositePageVisibilityPredicate(applicationConfig.enableUserDetails, checkNino)

  // if the user has a nino defined in auth, or manually defined
  private def hasNino(implicit authContext: AuthContext, request: Request[_]): Boolean =
    authContext.principal.accounts.paye.fold(false)(_ => true) ||
      request.session.get(ITSASessionKey.NINO).fold(false)(_ => true)

  class AuthorisedBy(regime: TaxRegime) {
    lazy val authedBy: AuthenticatedBy = AuthorisedFor(regime, visibilityPredicate)

    def asyncCore(action: AsyncUserRequest): Action[AnyContent] =
      authedBy.async { implicit authContext: AuthContext =>
        implicit request =>
          // check to see if the user has passed through the home controller successfully
          // the GoHome token will only be added to session if the user has a valid nino be it from auth or manually entered
          request.session.get(GoHome) match {
            // if (applicationConfig.enableUserDetails && !checkNino) then we are on a page for users to manually provide the nino
            case Some(_) =>
              // if they have already supplied a nino then we should not allow access to the nino entry pages
              (applicationConfig.enableUserDetails, checkNino, hasNino) match {
                case (true, false, true) => Redirect(controllers.routes.HomeController.index())
                case _ => action(IncomeTaxSAUser(authContext))(request)
              }
            case None =>
              (applicationConfig.enableUserDetails, checkNino, hasNino) match {
                // if the session key for the nino hash exists then the user must have provided a nino
                case (true, false, false) => action(IncomeTaxSAUser(authContext))(request)
                case _ => Redirect(controllers.routes.HomeController.index())
              }
          }
      }

    def async(action: AsyncUserRequest): Action[AnyContent] =
      asyncCore {
        authContext: IncomeTaxSAUser =>
          implicit request =>
            enrolmentService.checkEnrolment {
              case NotEnrolled => action(authContext)(request)
              case _ => Future.successful(Redirect(alreadyEnrolledUrl))
            }
      }

    def asyncForEnrolled(action: AsyncUserRequest): Action[AnyContent] =
      asyncCore {
        authContext: IncomeTaxSAUser =>
          implicit request =>
            enrolmentService.checkEnrolment {
              case Enrolled => action(authContext)(request)
              case _ => Future.successful(showNotFound)
            }
      }

    def asyncForHomeController(action: AsyncUserRequest): Action[AnyContent] =
      authedBy.async {
        authContext: AuthContext =>
          implicit request =>
            enrolmentService.checkEnrolment {
              case NotEnrolled => action(IncomeTaxSAUser(authContext))(request)
              case _ => Future.successful(Redirect(alreadyEnrolledUrl))
            }.flatMap { x => x.withSession(x.session.+(GoHome -> "et")) }
      }
  }

  trait IncomeTaxSARegime extends TaxRegime {
    override def isAuthorised(accounts: Accounts): Boolean = true

    override def authenticationType: AuthenticationProvider = new GovernmentGatewayProvider(postSignInRedirectUrl, applicationConfig.ggSignInUrl)
  }

  object IncomeTaxSARegime extends IncomeTaxSARegime

  object Authorised extends AuthorisedBy(IncomeTaxSARegime)

}
