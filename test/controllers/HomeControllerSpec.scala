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

package controllers

import assets.MessageLookup.FrontPage
import audit.Logging
import auth.{MockConfig, authenticatedFakeRequest, authenticatedNoNinoFakeRequest}
import config.BaseControllerConfig
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Request, RequestHeader}
import play.api.test.Helpers._
import services.mocks.{MockKeystoreService, MockSubscriptionService, MockThrottlingService}
import utils.{TestConstants, TestModels}


class HomeControllerSpec extends ControllerBaseSpec
  with MockThrottlingService
  with MockSubscriptionService
  with MockKeystoreService {

  override val controllerName: String = "HomeControllerSpec"

  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "index" -> TestHomeController(enableThrottling = false, showGuidance = false, enableCheckSubscriptionCalls = true).index()
  )

  def mockBaseControllerConfig(isThrottled: Boolean, showStartPage: Boolean, enableCheckSubscriptionCalls: Boolean): BaseControllerConfig = {
    val mockConfig = new MockConfig {
      override val enableThrottling: Boolean = isThrottled
      override val showGuidance: Boolean = showStartPage
      override val enableCheckSubscription: Boolean = enableCheckSubscriptionCalls
    }
    mockBaseControllerConfig(mockConfig)
  }

  def TestHomeController(enableThrottling: Boolean, showGuidance: Boolean, enableCheckSubscriptionCalls: Boolean) = new HomeController(
    mockBaseControllerConfig(enableThrottling, showGuidance, enableCheckSubscriptionCalls),
    messagesApi,
    TestThrottlingService,
    TestSubscriptionService,
    MockKeystoreService,
    app.injector.instanceOf[Logging]
  )

  "Calling the home action of the Home controller with an authorised user" should {

    "If the start page (showGuidance) is enabled" should {

      lazy val result = TestHomeController(enableThrottling = false, showGuidance = true, enableCheckSubscriptionCalls = true).home()(authenticatedFakeRequest())

      "Return status OK (200)" in {
        status(result) must be(Status.OK)
      }

      "Should have the page title" in {
        Jsoup.parse(contentAsString(result)).title mustBe FrontPage.title
      }
    }

    "If the start page (showGuidance) is disabled" should {
      lazy val result = TestHomeController(enableThrottling = false, showGuidance = false, enableCheckSubscriptionCalls = true).home()(authenticatedFakeRequest())

      "Return status SEE_OTHER (303) redirect" in {
        status(result) must be(Status.SEE_OTHER)
      }

      "Redirect to the 'Index' page" in {
        redirectLocation(result).get mustBe controllers.routes.HomeController.index().url
      }
    }
  }

  "Calling the index action of the HomeController with an authorised user" should {
    def call() = TestHomeController(enableThrottling = true, showGuidance = false, enableCheckSubscriptionCalls = true).index()(authenticatedFakeRequest())

    "redirect them to already subscribed page if they already has a subscription" in {
      setupGetSubscription(auth.nino)(subscribeSuccess)
      // this is mocked to check we don't call throttle as well
      setupMockCheckAccess(auth.nino)(OK)

      val result = call()
      status(result) must be(Status.SEE_OTHER)
      redirectLocation(result).get mustBe controllers.routes.AlreadyEnrolledController.enrolled().url

      verifyGetSubscription(auth.nino)(1)
      verifyMockCheckAccess(auth.nino)(0)
    }

    "display the error page if there was an error checking the subscription" in {
      setupGetSubscription(auth.nino)(subscribeBadRequest)
      // this is mocked to check we don't call throttle as well
      setupMockCheckAccess(auth.nino)(OK)

      status(call()) must be(Status.INTERNAL_SERVER_ERROR)

      verifyGetSubscription(auth.nino)(1)
      verifyMockCheckAccess(auth.nino)(0)
    }

    // N.B. the subscribeNone case is covered below
  }

  "Calling the index action of the Home controller with an authorised user who has no nino in auth" when {
    def call(request: Request[AnyContent]) = TestHomeController(enableThrottling = true, showGuidance = false, enableCheckSubscriptionCalls = true).index()(request)

    "there is no nino hash in session" should {
      s"redirect them to ${controllers.matching.routes.UserDetailsController.show().url}" in {
        setupGetSubscription(auth.nino)(subscribeNone)
        // this is mocked to check we don't call throttle as well
        setupMockCheckAccess(auth.nino)(OK)

        val result = call(authenticatedNoNinoFakeRequest)
        status(result) must be(Status.SEE_OTHER)
        redirectLocation(result).get mustBe controllers.matching.routes.UserDetailsController.show().url

        verifyGetSubscription(auth.nino)(0)
        verifyMockCheckAccess(auth.nino)(0)
      }
    }

    "the nino hash in session matches the nino stored in keystore" should {
      s"pass through the controller normally and proceed to ${controllers.preferences.routes.PreferencesController.checkPreferences().url}" in {
        setupGetSubscription(auth.nino)(subscribeNone)
        // this is mocked to check we don't call throttle as well
        setupMockCheckAccess(auth.nino)(OK)
        val userDetails = TestModels.testUserDetails.copy(nino = TestConstants.testNino)
        setupMockKeystore(fetchUserDetails = userDetails)

        val result = call(authenticatedNoNinoFakeRequest.withSession(ITSASessionKey.NINO -> userDetails.ninoHash))
        status(result) must be(Status.SEE_OTHER)

        redirectLocation(result).get mustBe controllers.preferences.routes.PreferencesController.checkPreferences().url

        verifyGetSubscription(auth.nino)(1)
        verifyMockCheckAccess(auth.nino)(1)
      }
    }

    "the nino hash in session does not match the nino stored in keystore" should {
      s"redirects to ${controllers.matching.routes.UserDetailsController.show().url}" in {
        setupGetSubscription(auth.nino)(subscribeNone)
        // this is mocked to check we don't call throttle as well
        setupMockCheckAccess(auth.nino)(OK)
        val userDetails = TestModels.testUserDetails.copy(nino = TestConstants.testNino)
        setupMockKeystore(fetchUserDetails = userDetails)

        val request = authenticatedNoNinoFakeRequest.withSession(ITSASessionKey.NINO -> "notNinoHash")
        implicit val requestHeader: RequestHeader = request.copy()

        val result = call(request)
        status(result) must be(Status.SEE_OTHER)

        redirectLocation(result).get mustBe controllers.matching.routes.UserDetailsController.show().url

        // the nino hash must be removed from session
        await(result).session.get(ITSASessionKey.NINO) mustBe None

        verifyGetSubscription(auth.nino)(0)
        verifyMockCheckAccess(auth.nino)(0)
      }
    }
  }

  for (enableCheckSubscription <- Seq(true, false)) {

    s"enableCheckSubscription is set to $enableCheckSubscription" when {

      val expectedGetSubscriptionCalls = enableCheckSubscription match {
        case true => 1
        case false => 0
      }

      "Calling the index action of the HomeController with an authorised user who does not already have a subscription" should {

        "If throttling is enabled when calling the index" should {
          lazy val result = TestHomeController(enableThrottling = true, showGuidance = false, enableCheckSubscriptionCalls = enableCheckSubscription).index()(authenticatedFakeRequest())

          "trigger a call to the throttling service" in {
            setupGetSubscription(auth.nino)(subscribeNone)
            setupMockCheckAccess(auth.nino)(OK)

            status(result) must be(Status.SEE_OTHER)

            redirectLocation(result).get mustBe controllers.preferences.routes.PreferencesController.checkPreferences().url

            verifyGetSubscription(auth.nino)(expectedGetSubscriptionCalls)
            verifyMockCheckAccess(auth.nino)(1)
          }
        }

        "If throttling is disabled when calling the index" should {
          lazy val result = TestHomeController(enableThrottling = false, showGuidance = false, enableCheckSubscriptionCalls = enableCheckSubscription).index()(authenticatedFakeRequest())

          "not trigger a call to the throttling service" in {
            setupGetSubscription(auth.nino)(subscribeNone)
            setupMockCheckAccess(auth.nino)(OK)

            status(result) must be(Status.SEE_OTHER)

            redirectLocation(result).get mustBe controllers.preferences.routes.PreferencesController.checkPreferences().url

            verifyGetSubscription(auth.nino)(expectedGetSubscriptionCalls)
            verifyMockCheckAccess(auth.nino)(0)
          }
        }
      }

    }
  }

  authorisationTests()

}
