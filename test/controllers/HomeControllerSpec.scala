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
import auth.{MockConfig, authenticatedFakeRequest}
import config.BaseControllerConfig
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import services.mocks.{MockKeystoreService, MockSubscriptionService, MockThrottlingService}


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
