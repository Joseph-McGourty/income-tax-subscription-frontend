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

package services

import auth.IncomeTaxSAUser
import config.SessionCache
import controllers.ITSASessionKey
import models.matching.UserDetailsModel
import models.{BusinessNameModel, DateModel}
import org.scalatest.Matchers._
import play.api.test.Helpers._
import services.mocks.MockKeystoreService
import uk.gov.hmrc.play.http.HttpResponse
import utils.{TestConstants, TestModels, UnitTestTrait}

class KeystoreServiceSpec extends UnitTestTrait
  with MockKeystoreService {

  "Keystore service" should {
    "be DIed with the correct session cache object" in {
      val cache = app.injector.instanceOf[SessionCache]
      cache.defaultSource shouldBe cache.getConfString("session-cache.income-tax-subscription-frontend.cache", "income-tax-subscription-frontend")
      cache.baseUri shouldBe cache.baseUrl("session-cache")
      cache.domain shouldBe cache.getConfString("session-cache.domain", throw new Exception(s"Could not find config 'session-cache.domain'"))
    }
  }

  object TestKeystore {
    val keystoreService: KeystoreService = MockKeystoreService
  }

  "mock keystore service" should {

    "configure and verify fetch and save business name as specified" in {
      val testBusinessName = BusinessNameModel("my business name")
      setupMockKeystore(fetchBusinessName = testBusinessName)
      for {
        businessName <- TestKeystore.keystoreService.fetchBusinessName()
        _ <- TestKeystore.keystoreService.saveBusinessName(testBusinessName)
      } yield {
        businessName shouldBe testBusinessName

        verifyKeystore(
          fetchBusinessName = 1,
          saveBusinessName = 1
        )
      }
    }

    "configure and verify fetch all as specified" in {
      val testFetchAll = TestModels.emptyCacheMap
      setupMockKeystore(fetchAll = testFetchAll)
      for {
        fetched <- TestKeystore.keystoreService.fetchAll()
      } yield {
        fetched shouldBe testFetchAll

        verifyKeystore(fetchAll = 1)
      }
    }

    "configure and verify remove all as specified" in {
      val testDeleteAll = HttpResponse(200)
      setupMockKeystore(deleteAll = testDeleteAll)
      for {
        response <- TestKeystore.keystoreService.deleteAll()
      } yield {
        response shouldBe testDeleteAll

        verifyKeystore(fetchAll = 1)
      }
    }

  }

  "KeyStoreServiceUtil.fetchUserEnteredNino" should {
    val testNino = TestConstants.testNino
    val testUserDetails = UserDetailsModel(
      firstName = "",
      lastName = "",
      nino = testNino,
      dateOfBirth = DateModel("01", "01", "1980")
    )

    lazy val requestNoNinoInSession = auth.authenticatedNoNinoFakeRequest
    lazy val requestNinoInSession = auth.authenticatedNoNinoFakeRequest.withSession(ITSASessionKey.NINO -> testUserDetails.ninoHash)

    "return None if there is no User Detail in keystore" in {
      implicit val req = requestNoNinoInSession
      setupMockKeystore(fetchUserDetails = None)
      val r = TestKeystore.keystoreService.fetchUserEnteredNino()
      await(r) shouldBe None
    }

    "return None if there is User Detail in keystore but there is no NINO hash in session" in {
      implicit val req = requestNoNinoInSession
      setupMockKeystore(fetchUserDetails = testUserDetails)
      val r = TestKeystore.keystoreService.fetchUserEnteredNino()
      await(r) shouldBe None
    }

    "return None if there is User Detail in keystore but it does not match the NINO hash in session" in {
      implicit val req = requestNinoInSession.withSession(ITSASessionKey.NINO -> (testUserDetails.ninoHash + "1"))
      setupMockKeystore(fetchUserDetails = testUserDetails)
      val r = TestKeystore.keystoreService.fetchUserEnteredNino()
      await(r) shouldBe None
    }

    "return the NINO if there is User Detail in keystore and it matches the NINO hash in session" in {
      implicit val req = requestNinoInSession
      setupMockKeystore(fetchUserDetails = testUserDetails)
      val r = TestKeystore.keystoreService.fetchUserEnteredNino()
      await(r) shouldBe Some(testNino)
    }
  }

  "KeyStoreServiceUtil.getNino" should {

    val testNino = TestConstants.testNino
    val testUserDetails = UserDetailsModel(
      firstName = "",
      lastName = "",
      nino = testNino,
      dateOfBirth = DateModel("01", "01", "1980")
    )

    lazy val requestNoNinoInSession = auth.authenticatedNoNinoFakeRequest
    lazy val requestNinoInSession = auth.authenticatedNoNinoFakeRequest.withSession(ITSASessionKey.NINO -> testUserDetails.ninoHash)
    lazy val requestWrongNinoInSession = auth.authenticatedNoNinoFakeRequest.withSession(ITSASessionKey.NINO -> "differentHash")


    "if the nino is in auth then return that and do not call keystore" in {
      implicit val user = IncomeTaxSAUser(auth.ggUser.userCL200Context)
      implicit val req = auth.authenticatedFakeRequest()
      setupMockKeystore(fetchUserDetails = None)
      val r = TestKeystore.keystoreService.getNino()
      await(r) shouldBe Some(testNino)

      verifyKeystore(fetchUserDetails = 0)
    }

    "if the nino is not in auth and nino hash is in session then call keystore" in {
      implicit val user = IncomeTaxSAUser(auth.ggUser.userCL200NoAccountsContext)
      implicit val req = requestNinoInSession
      setupMockKeystore(fetchUserDetails = testUserDetails)
      val r = TestKeystore.keystoreService.getNino()
      await(r) shouldBe Some(testNino)

      verifyKeystore(fetchUserDetails = 1)
    }

    "if the nino is not in auth and nino hash is in session then call keystore, but if nino hash does not match then return None" in {
      implicit val user = IncomeTaxSAUser(auth.ggUser.userCL200NoAccountsContext)
      implicit val req = requestWrongNinoInSession
      setupMockKeystore(fetchUserDetails = testUserDetails)
      val r = TestKeystore.keystoreService.getNino()
      await(r) shouldBe None

      verifyKeystore(fetchUserDetails = 1)
    }

    "if the nino is not in auth and nino hash is not in session then do not call keystore and return None" in {
      implicit val user = IncomeTaxSAUser(auth.ggUser.userCL200NoAccountsContext)
      implicit val req = requestNoNinoInSession
      setupMockKeystore(fetchUserDetails = testUserDetails)
      val r = TestKeystore.keystoreService.getNino()
      await(r) shouldBe None

      verifyKeystore(fetchUserDetails = 0)
    }
  }

}
