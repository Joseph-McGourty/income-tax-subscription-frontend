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

package controllers.matching

import auth._
import controllers.ControllerBaseSpec
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers.{await, _}
import services.mocks.{MockUserMatchingService, MockKeystoreService}
import utils.{TestConstants, TestModels}

class ConfirmDetailsControllerSpec extends ControllerBaseSpec
  with MockKeystoreService
  with MockUserMatchingService {

  override val controllerName: String = "ConfirmDetailsController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestConfirmDetailsController.show(),
    "submit" -> TestConfirmDetailsController.submit()
  )

  object TestConfirmDetailsController extends ConfirmDetailsController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService,
    TestUserMatchingService
  )

  "Calling the show action of the ConfirmDetailsController with an authorised user" should {

    def call = TestConfirmDetailsController.show()(authenticatedFakeRequest())

    "when there are no client details store redirect them to client details" in {
      setupMockKeystore(fetchUserDetails = None)

      val result = call

      status(result) must be(Status.SEE_OTHER)

      await(result)
      verifyKeystore(fetchUserDetails = 1, saveUserDetails = 0)

    }

    "if there is are client details return ok (200)" in {
      setupMockKeystore(fetchUserDetails = TestModels.testUserDetails)
      val result = call

      status(result) must be(Status.OK)

      await(result)
      verifyKeystore(fetchUserDetails = 1, saveUserDetails = 0)
    }
  }

  "Calling the submit action of the ConfirmDetailsController with an authorised user and valid submission" should {

    val testNino = TestConstants.testNino

    def callSubmit() = TestConfirmDetailsController.submit()(authenticatedFakeRequest())

    "When a match has been found" should {
      "return a redirect status (SEE_OTHER)" in {
        setupMatchUser(matchUserMatched)
        setupMockKeystore(fetchUserDetails = TestModels.testUserDetails)

        val goodRequest = callSubmit()

        status(goodRequest) must be(Status.SEE_OTHER)

        await(goodRequest)
        verifyKeystore(fetchUserDetails = 1, saveUserDetails = 0)
      }

      s"redirect to '${controllers.routes.IncomeSourceController.showIncomeSource().url}" in {
        setupMatchUser(matchUserMatched)
        setupMockKeystore(fetchUserDetails = TestModels.testUserDetails)

        val goodRequest = callSubmit()

        redirectLocation(goodRequest) mustBe Some(controllers.routes.IncomeSourceController.showIncomeSource().url)

        await(goodRequest)
        verifyKeystore(fetchUserDetails = 1, saveUserDetails = 0)
      }
    }

    "When no match was been found" should {
      "return a redirect status (SEE_OTHER)" in {
        setupMatchUser(matchUserNoMatch)
        setupMockKeystore(fetchUserDetails = TestModels.testUserDetails)

        val goodRequest = callSubmit()

        status(goodRequest) must be(Status.SEE_OTHER)

        await(goodRequest)
        verifyKeystore(fetchUserDetails = 1, saveUserDetails = 0)
      }

      s"redirect to '${controllers.matching.routes.UserDetailsErrorController.show().url}" in {
        setupMatchUser(matchUserNoMatch)
        setupMockKeystore(fetchUserDetails = TestModels.testUserDetails)

        val goodRequest = callSubmit()

        redirectLocation(goodRequest) mustBe Some(controllers.matching.routes.UserDetailsErrorController.show().url)

        await(goodRequest)
        verifyKeystore(fetchUserDetails = 1, saveUserDetails = 0)
      }
    }
  }

  "The back url" should {
    s"point to ${controllers.matching.routes.UserDetailsController.show().url}" in {
      TestConfirmDetailsController.backUrl mustBe controllers.matching.routes.UserDetailsController.show().url
    }
  }

  authorisationTests()
}
