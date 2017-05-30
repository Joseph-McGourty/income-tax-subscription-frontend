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
import controllers.{ControllerBaseSpec, ITSASessionKey}
import play.api.http.Status
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, _}
import services.mocks.{MockKeystoreService, MockUserMatchingService}
import utils.{TestConstants, TestModels}

import scala.concurrent.Future

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

    def callShow(request: Request[AnyContent]) = TestConfirmDetailsController.show()(request)

    "when there are no client details store redirect them to client details" in {
      setupMockKeystore(fetchUserDetails = None)

      val result = callShow(authenticatedNoNinoFakeRequest)

      status(result) must be(Status.SEE_OTHER)

      await(result)
      verifyKeystore(fetchUserDetails = 1, saveUserDetails = 0)

    }

    "if there is are client details return ok (200)" in {
      setupMockKeystore(fetchUserDetails = TestModels.testUserDetails)
      val result = callShow(authenticatedNoNinoFakeRequest)

      status(result) must be(Status.OK)

      await(result)
      verifyKeystore(fetchUserDetails = 1, saveUserDetails = 0)
    }

    "If the user has a nino" when {
      "The nino is in the auth" should {
        s"bounce the user back to ${controllers.routes.HomeController.index().url}" in {
          val result = callShow(authenticatedFakeRequest())
          await(result)
          status(result) mustBe SEE_OTHER
          redirectLocation(result).get mustBe controllers.routes.HomeController.index().url
        }
      }

      "The nino is in the session" should {
        s"bounce the user back to ${controllers.routes.HomeController.index().url}" in {
          val result = callShow(authenticatedNoNinoFakeRequest.withSession(ITSASessionKey.NINO -> "anyValue"))
          await(result)
          status(result) mustBe SEE_OTHER
          redirectLocation(result).get mustBe controllers.routes.HomeController.index().url
        }
      }
    }
  }

  "Calling the submit action of the ConfirmDetailsController with an authorised user and valid submission" when {

    val testNino = TestConstants.testNino

    def userDetail(nino: String) = TestModels.testUserDetails.copy(nino = nino)

    def ninoHash(nino: String) = userDetail(nino = nino).ninoHash

    def newRequest(ninoInSession: Option[String] = None): FakeRequest[AnyContentAsEmpty.type] =
      ninoInSession match {
        case Some(oldNino) => authenticatedNoNinoFakeRequest.withSession(ITSASessionKey.NINO -> ninoHash(oldNino))
        case _ => authenticatedNoNinoFakeRequest
      }


    def callSubmit(request: Request[AnyContent]) = TestConfirmDetailsController.submit()(request)

    "a match has been found" should {

      lazy val testRequest = newRequest()
      lazy val testUserDetail = userDetail(testNino)

      "return a redirect status (SEE_OTHER)" in {
        setupMatchUser(matchUserMatched)
        setupMockKeystore(fetchUserDetails = testUserDetail)

        val goodRequest = callSubmit(testRequest)

        status(goodRequest) must be(Status.SEE_OTHER)

        val result = await(goodRequest)
        verifyKeystore(fetchUserDetails = 1, saveUserDetails = 0)
      }

      s"redirect to '${controllers.routes.HomeController.index().url}" in {
        setupMatchUser(matchUserMatched)
        setupMockKeystore(fetchUserDetails = testUserDetail)

        val goodRequest = callSubmit(testRequest)

        redirectLocation(goodRequest) mustBe Some(controllers.routes.HomeController.index().url)

        await(goodRequest)
        verifyKeystore(fetchUserDetails = 1, saveUserDetails = 0)
      }

      "The session is updated with the nino hash" in {
        setupMatchUser(matchUserMatched)
        setupMockKeystore(fetchUserDetails = testUserDetail)

        val goodRequest = callSubmit(testRequest)

        val result = await(goodRequest)
        result.session(testRequest).get(ITSASessionKey.NINO) mustBe Some(ninoHash(testUserDetail.nino))
      }
    }

    "no match was been found" should {

      lazy val testRequest = newRequest()
      lazy val testUserDetail = userDetail(testNino)

      "return a redirect status (SEE_OTHER)" in {
        setupMatchUser(matchUserNoMatch)
        setupMockKeystore(fetchUserDetails = testUserDetail)

        val goodRequest = callSubmit(testRequest)

        status(goodRequest) must be(Status.SEE_OTHER)

        await(goodRequest)
        verifyKeystore(fetchUserDetails = 1, saveUserDetails = 0)
      }

      s"redirect to '${controllers.matching.routes.UserDetailsErrorController.show().url}" in {
        setupMatchUser(matchUserNoMatch)
        setupMockKeystore(fetchUserDetails = testUserDetail)

        val goodRequest = callSubmit(testRequest)

        redirectLocation(goodRequest) mustBe Some(controllers.matching.routes.UserDetailsErrorController.show().url)

        await(goodRequest)
        verifyKeystore(fetchUserDetails = 1, saveUserDetails = 0)
      }

      "There should not be a nino hash in the session" in {
        setupMatchUser(matchUserNoMatch)
        setupMockKeystore(fetchUserDetails = testUserDetail)

        val goodRequest = callSubmit(testRequest)

        val result = await(goodRequest)
        result.session(testRequest).get(ITSASessionKey.NINO) mustBe None
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
