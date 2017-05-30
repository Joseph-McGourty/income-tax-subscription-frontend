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

import audit.Logging
import auth._
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.mocks.{MockKeystoreService, MockSubscriptionService}
import uk.gov.hmrc.play.http.InternalServerException
import utils.{TestConstants, TestModels}

class CheckYourAnswersControllerSpec extends ControllerBaseSpec
  with MockKeystoreService
  with MockSubscriptionService {

  override val controllerName: String = "CheckYourAnswersController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestCheckYourAnswersController.show,
    "submit" -> TestCheckYourAnswersController.submit
  )

  object TestCheckYourAnswersController extends CheckYourAnswersController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService,
    middleService = TestSubscriptionService,
    app.injector.instanceOf[Logging]
  )

  "Calling the show action of the CheckYourAnswersController with an authorised user" should {

    lazy val result = TestCheckYourAnswersController.show(authenticatedFakeRequest())

    "return ok (200)" in {
      setupMockKeystore(fetchAll = TestModels.testCacheMap)

      status(result) must be(Status.OK)
    }
  }

  "Calling the submit action of the CheckYourAnswersController with an authorised user" should {

    def call = TestCheckYourAnswersController.submit(authenticatedFakeRequest())

    "When the submission is successful" should {
      lazy val result = call

      "return a redirect status (SEE_OTHER - 303)" in {
        setupMockKeystore(fetchAll = TestModels.testCacheMap)
        setupSubscribe()(subscribeSuccess)
        status(result) must be(Status.SEE_OTHER)
        await(result)
        verifyKeystore(fetchAll = 1, saveSubscriptionId = 1)
      }

      s"redirect to '${controllers.routes.ConfirmationController.showConfirmation().url}'" in {
        redirectLocation(result) mustBe Some(controllers.routes.ConfirmationController.showConfirmation().url)
      }
    }

    "When the submission is unsuccessful" should {
      lazy val result = call

      "return a failure if subscription fails" in {
        setupMockKeystore(fetchAll = TestModels.testCacheMap)
        setupSubscribe()(subscribeBadRequest)
        val ex = intercept[InternalServerException](await(call))
        ex.message mustBe "Successful response not received from submission"
        verifyKeystore(fetchAll = 1, saveSubscriptionId = 0)
      }

    }
  }

  "Calling the submit action of the CheckYourAnswersController with an authorised user who does not have nino in their auth profile" should {

    def call(request: FakeRequest[AnyContentAsEmpty.type]) = TestCheckYourAnswersController.submit(request
      .withSession(ITSASessionKey.NINO -> TestModels.testUserDetails.copy(nino = TestConstants.testNino).ninoHash))

    "if the nino hash is in session and it matches the nino in keystore" should {

      "proceed normally" in {
        setupMockKeystore(fetchAll = TestModels.testCacheMap)
        val userDetails = TestModels.testUserDetails.copy(nino = TestConstants.testNino)
        setupMockKeystore(fetchUserDetails = userDetails)
        setupSubscribe()(subscribeSuccess)

        val result = call(authenticatedNoNinoFakeRequest)

        status(result) must be(Status.SEE_OTHER)
        redirectLocation(result) mustBe Some(controllers.routes.ConfirmationController.showConfirmation().url)

        await(result)
        verifyKeystore(fetchAll = 1, saveSubscriptionId = 1)
      }

    }

    "if the nino hash is in session but it does not matches the nino in keystore" should {
      "show internal server error" in {
        setupMockKeystore(fetchAll = TestModels.testCacheMap)
        setupSubscribe()(subscribeSuccess)
        val userDetails = TestModels.testUserDetails.copy(nino = "differentNino")
        setupMockKeystore(fetchUserDetails = userDetails)

        val ex = intercept[InternalServerException](await(call(authenticatedNoNinoFakeRequest)))
        ex.message mustBe "Cannot find the nino for the user"

        verifyKeystore(fetchAll = 1, saveSubscriptionId = 0)
      }

    }
  }

  "The back url" should {
    s"point to ${controllers.routes.TermsController.showTerms().url}" in {
      TestCheckYourAnswersController.backUrl mustBe controllers.routes.TermsController.showTerms().url
    }
  }

  authorisationTests()

}
