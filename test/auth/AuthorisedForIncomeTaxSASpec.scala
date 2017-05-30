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

import controllers.ITSASessionKey
import play.api.http.Status
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.AuthenticationProviderIds
import org.scalatest.Matchers._
import play.api.i18n.MessagesApi
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

class AuthorisedForIncomeTaxSASpec extends MockAuthTestController {

  "Calling authenticated async action with no logged in session" should {
    "result in a redirect to login" in {
      val result = AuthTestController.authorisedAsyncAction(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
    }
  }

  "Calling authenticated async action with a logged in user with Confidence Level 500" should {
    "result in an OK status" in {
      val result = AuthTestController.authorisedAsyncAction(authenticatedFakeRequest(AuthenticationProviderIds.GovernmentGatewayId, mockAuthorisedUserIdCL500))
      status(result) shouldBe Status.OK
    }
  }

  "Calling authenticated async action with a logged in user with Confidence Level 200 and a PAYE account" should {
    "result in an OK status" in {
      val result = AuthTestController.authorisedAsyncAction(authenticatedFakeRequest(AuthenticationProviderIds.GovernmentGatewayId, mockAuthorisedUserIdCL200))
      status(result) shouldBe Status.OK
    }
  }

  "Calling authenticated async action with a logged in user with Confidence Level 200 and NO PAYE account" should {

    lazy val result = AuthTestController.authorisedAsyncAction(authenticatedFakeRequest(AuthenticationProviderIds.GovernmentGatewayId, mockUpliftUserIdCL200NoAccounts))

    "result in a status SEE_OTHER (303) redirect" in {
      status(result) shouldBe Status.SEE_OTHER
    }
  }

  "Calling authenticated async action with a logged in user with Confidence Level 100" should {

    lazy val result = AuthTestController.authorisedAsyncAction(authenticatedFakeRequest(AuthenticationProviderIds.GovernmentGatewayId, mockUpliftUserIdCL100))

    "result in a status OK (200)" in {
      status(result) shouldBe Status.OK
    }
  }

  "Calling authenticated async action with a logged in user with Confidence Level 50" should {

    lazy val result = AuthTestController.authorisedAsyncAction(authenticatedFakeRequest(AuthenticationProviderIds.GovernmentGatewayId, mockUpliftUserIdCL50))

    "result in a status OK (200)" in {
      status(result) shouldBe Status.OK
    }
  }

  "Calling authenticated action with a logged in user with weak credentials" should {

    lazy val result = AuthTestController.authorisedAsyncAction(authenticatedFakeRequest(AuthenticationProviderIds.GovernmentGatewayId, mockWeakUserId))

    "result in a OK status (200)" in {
      status(result) shouldBe Status.OK
    }
  }

  "Calling authenticated action with a timed out user session" should {

    lazy val result = AuthTestController.authorisedAsyncAction(timeoutFakeRequest())

    "result in a redirect status" in {
      status(result) shouldBe Status.SEE_OTHER
    }

    "redirect to the Session Timeout Page" in {
      redirectLocation(result) shouldBe Some(controllers.routes.SessionTimeoutController.timeout().url)
    }
  }

  "Calling authenticated action with an enrolled user" should {
    lazy val result = AuthTestController.authorisedAsyncAction(authenticatedFakeRequest(AuthenticationProviderIds.GovernmentGatewayId, mockEnrolled))

    "result in a redirect status" in {
      status(result) shouldBe Status.SEE_OTHER
    }

    "redirect to the Already Enrolled Page" in {
      redirectLocation(result) shouldBe Some(mockConfig.alreadyEnrolledUrl)
    }
  }

  "A controller without passing through the home controller" should {
    lazy val result = AuthTestController.authorisedAsyncAction(authenticatedFakeRequest(beenHome = false))
    "be redirected to the home controller" in {
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(controllers.routes.HomeController.index().url)
    }
  }

  "A controller passed through the home controller" should {
    lazy val result = AuthTestController.authorisedAsyncAction(authenticatedFakeRequest())
    "be allowed to proceed normally" in {
      status(result) shouldBe Status.OK // the OK is defined by authorisedAsyncAction
    }
  }

  "When a user who logged in without a nino in auth hits a controller which implements AuthorisedForIncomeTaxSA" when {

    def authTestController(setEnableUserDetails: Boolean, setCheckNino: Boolean) = new FrontendController with AuthorisedForIncomeTaxSA {
      override val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
      override lazy val applicationConfig =
        new MockConfig {
          override lazy val enableUserDetails: Boolean = setEnableUserDetails
        }
      override lazy val authConnector = TestAuthConnector
      override lazy val enrolmentService = TestEnrolmentService
      override lazy val postSignInRedirectUrl = "www.redirected.com"
      override val checkNino: Boolean = setCheckNino

      val authorisedAsyncAction = Authorised.async {
        implicit user => implicit request => Future.successful(Ok)
      }

    }

    "manual nino entry journey is enabled (enableUserDetails=true)" when {
      "it is not a manual nino provider page (checkNino = true)" should {
        s"bounce the user to ${controllers.matching.routes.UserDetailsController.show().url}" in {
          lazy val result = authTestController(setEnableUserDetails = true, setCheckNino = true)
            .authorisedAsyncAction(authenticatedNoNinoFakeRequest)
          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result).get shouldBe controllers.matching.routes.UserDetailsController.show().url
        }
      }

      "it is not a manual nino provider page (checkNino = true) checkNino = true and there is a nino hash in session" should {
        s"proceed with the specified action" in {
          lazy val result = authTestController(setEnableUserDetails = true, setCheckNino = true)
            .authorisedAsyncAction(authenticatedNoNinoFakeRequest.withSession(ITSASessionKey.NINO -> "anyValue"))
          status(result) shouldBe Status.OK
        }
      }

      "it is a manual nino provider page (checkNino = false)" should {
        s"proceed with the specified action" in {
          lazy val result = authTestController(setEnableUserDetails = true, setCheckNino = false)
            .authorisedAsyncAction(authenticatedNoNinoFakeRequest)
          status(result) shouldBe Status.OK
        }
      }

      "it is a manual nino provider page (checkNino = false) and there is a nino hash in session" should {
        s"bounce the user to ${controllers.routes.HomeController.index().url}" in {
          lazy val result = authTestController(setEnableUserDetails = true, setCheckNino = false)
            .authorisedAsyncAction(authenticatedNoNinoFakeRequest.withSession(ITSASessionKey.NINO -> "anyValue"))
          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result).get shouldBe controllers.routes.HomeController.index().url
        }
      }

    }

    "manual nino entry journey is disabled (enableUserDetails=false)" when {

      "checkNino = true" should {
        s"bounce take the user to ${controllers.routes.NoNinoController.showNoNino().url}" in {
          lazy val result = authTestController(setEnableUserDetails = false, setCheckNino = true)
            .authorisedAsyncAction(authenticatedNoNinoFakeRequest)
          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result).get shouldBe controllers.routes.NoNinoController.showNoNino().url
        }
      }

      "checkNino = false" should {
        s"bounce take the user to ${controllers.routes.NoNinoController.showNoNino().url}" in {
          lazy val result = authTestController(setEnableUserDetails = false, setCheckNino = false)
            .authorisedAsyncAction(authenticatedNoNinoFakeRequest)
          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result).get shouldBe controllers.routes.NoNinoController.showNoNino().url
        }
      }

    }
  }

}
