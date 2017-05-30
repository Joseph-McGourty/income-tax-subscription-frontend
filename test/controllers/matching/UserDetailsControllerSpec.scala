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

import assets.MessageLookup.{UserDetails => messages}
import auth._
import controllers.{ControllerBaseSpec, ITSASessionKey}
import forms.matching.UserDetailsForm
import models.DateModel
import models.matching.UserDetailsModel
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, contentAsString, contentType, _}
import services.mocks.MockKeystoreService
import utils.{TestConstants, TestModels}

import scala.concurrent.Future


class UserDetailsControllerSpec extends ControllerBaseSpec
  with MockKeystoreService {

  override val controllerName: String = "UserDetailsController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestUserDetailsController.show(isEditMode = false),
    "submit" -> TestUserDetailsController.submit(isEditMode = false)
  )

  object TestUserDetailsController extends UserDetailsController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService
  )

  lazy val testNino: String = TestConstants.testNino

  def userDetail(nino: String): UserDetailsModel = TestModels.testUserDetails.copy(nino = nino)

  def ninoHash(nino: String): String = userDetail(nino = nino).ninoHash

  def newRequest(ninoInSession: Option[String] = None): FakeRequest[AnyContentAsEmpty.type] =
    ninoInSession match {
      case Some(oldNino) => authenticatedNoNinoFakeRequest.withSession(ITSASessionKey.NINO -> ninoHash(oldNino))
      case _ => authenticatedNoNinoFakeRequest
    }

  "Calling the show action of the UserDetailsController with an authorised user" should {

    lazy val result = TestUserDetailsController.show(isEditMode = false)(authenticatedNoNinoFakeRequest)

    "return ok (200)" in {
      setupMockKeystore(fetchUserDetails = None)

      status(result) must be(Status.OK)

      await(result)
      verifyKeystore(fetchUserDetails = 1, saveUserDetails = 0)
    }

    "return HTML" in {
      contentType(result) must be(Some("text/html"))
      charset(result) must be(Some("utf-8"))
    }

    "render the 'Not subscribed to Agent Services page'" in {
      val document = Jsoup.parse(contentAsString(result))
      document.title mustBe messages.title
    }
  }

  "If the user has a nino" when {
    for (editMode <- Seq(true, false)) {

      s"editMode=$editMode" when {

        def callShow(request: Request[AnyContent], isEditMode: Boolean): Future[Result] =
          TestUserDetailsController.show(isEditMode = isEditMode)(request)

        "The nino is in the auth" should {
          s"bounce the user back to ${controllers.routes.HomeController.index().url}" in {
            val result = callShow(authenticatedFakeRequest(), editMode)
            await(result)
            status(result) mustBe SEE_OTHER
            redirectLocation(result).get mustBe controllers.routes.HomeController.index().url
          }
        }

        "The nino is in the session" should {
          s"bounce the user back to ${controllers.routes.HomeController.index().url}" in {
            val result = callShow(authenticatedNoNinoFakeRequest.withSession(ITSASessionKey.NINO -> "anyValue"), editMode)
            await(result)
            status(result) mustBe SEE_OTHER
            redirectLocation(result).get mustBe controllers.routes.HomeController.index().url
          }
        }
      }
    }
  }

  for (editMode <- Seq(true, false)) {

    s"editMode=$editMode" when {

      "Calling the submit action of the UserDetailsController with an authorised user and valid submission" should {

        def callSubmit(isEditMode: Boolean) =
          TestUserDetailsController.submit(isEditMode = isEditMode)(
            authenticatedNoNinoFakeRequest
              .post(UserDetailsForm.userDetailsForm.form, UserDetailsModel(
                firstName = "Abc",
                lastName = "Abc",
                nino = testNino,
                dateOfBirth = DateModel("01", "01", "1980")))
          )

        "return a redirect status (SEE_OTHER - 303)" in {
          setupMockKeystoreSaveFunctions()

          val goodResult = callSubmit(isEditMode = editMode)

          status(goodResult) must be(Status.SEE_OTHER)

          await(goodResult)
          verifyKeystore(fetchUserDetails = 0, saveUserDetails = 1)
        }

        s"redirect to '${controllers.matching.routes.ConfirmDetailsController.show().url}'" in {
          setupMockKeystoreSaveFunctions()

          val goodResult = callSubmit(isEditMode = editMode)

          redirectLocation(goodResult) mustBe Some(controllers.matching.routes.ConfirmDetailsController.show().url)

          await(goodResult)
          verifyKeystore(fetchUserDetails = 0, saveUserDetails = 1)
        }
      }

      "Calling the submit action of the UserDetailsController with an authorised user and invalid submission" should {

        def callSubmit(isEditMode: Boolean) =
          TestUserDetailsController.submit(isEditMode = isEditMode)(
            authenticatedNoNinoFakeRequest
              .post(UserDetailsForm.userDetailsForm.form, UserDetailsModel(
                firstName = "Abc",
                lastName = "Abc",
                nino = testNino,
                dateOfBirth = DateModel("00", "01", "1980")))
          )

        "return a redirect status (BAD_REQUEST - 400)" in {
          setupMockKeystoreSaveFunctions()

          val badResult = callSubmit(isEditMode = editMode)

          status(badResult) must be(Status.BAD_REQUEST)

          await(badResult)
          verifyKeystore(fetchUserDetails = 0, saveUserDetails = 0)
        }

        "return HTML" in {
          val badResult = callSubmit(isEditMode = editMode)

          contentType(badResult) must be(Some("text/html"))
          charset(badResult) must be(Some("utf-8"))
        }

        s"render the tite '${messages.title}'" in {
          val badResult = callSubmit(isEditMode = editMode)
          val document = Jsoup.parse(contentAsString(badResult))
          document.title mustBe messages.title
        }

      }
    }

  }

  authorisationTests()
}
