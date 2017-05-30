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

import helpers.ComponentSpecBase
import helpers.servicemocks.AuthStub._
import helpers.servicemocks.EnrolmentsStub.Enrolments
import helpers.servicemocks.KeystoreStub._
import helpers.servicemocks.WireMockDSL.HTTPVerbMapping._
import helpers.servicemocks.WireMockDSL._
import play.api.http.Status._
import play.api.i18n.Messages
import helpers.IntegrationTestConstants._
import services.CacheConstants._

class UserDetailsControllerISpec extends ComponentSpecBase {
  import IncomeTaxSubscriptionFrontend._

  "GET /user-details" when {
    "authenticated without a NINO" should {
      "return the get user details page" in {
        stub when Get(authority) thenReturn stubbedTestUser
        stub when Get(Enrolments of stubbedTestUser) thenReturn Nil
        stub when Get(keystore) thenReturn emptyKeystoreData

        getUserDetails() should have (
          httpStatus(OK),
          pageTitle(Messages("user-details.title"))
        )
      }

      "return a populated page when keystore has user details data" in {
        stub when Get(authority) thenReturn stubbedTestUser
        stub when Get(Enrolments of stubbedTestUser) thenReturn Nil
        stub when Get(keystore) thenReturn keystoreUserDetails(testUserDetails)

        getUserDetails() should have (
          httpStatus(OK),
          pageTitle(Messages("user-details.title")),
          elementByID("firstName")(testUserDetails.firstName),
          elementByID("lastName")(testUserDetails.lastName),
          elementByID("nino")(testUserDetails.nino),
          elementByID("dateOfBirth.dateDay")(testUserDetails.dateOfBirth.day),
          elementByID("dateOfBirth.dateMonth")(testUserDetails.dateOfBirth.month),
          elementByID("dateOfBirth.dateYear")(testUserDetails.dateOfBirth.year)
        )
      }

      "return a redirect to the login page when unauthorized" in {
        stub when Get(authority) thenReturn UNAUTHORIZED

        getUserDetails() should have (
          httpStatus(SEE_OTHER)
        )
      }

      "return a failure when enrolments cannot be accessed" in {
        stub when Get(authority) thenReturn stubbedTestUser
        stub when Get(Enrolments of stubbedTestUser) thenReturn INTERNAL_SERVER_ERROR

        getUserDetails() should have (
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }

      "return a failure when keystore cannot be accessed" in {
        stub when Get(authority) thenReturn stubbedTestUser
        stub when Get(Enrolments of stubbedTestUser) thenReturn Nil
        stub when Get(keystore) thenReturn INTERNAL_SERVER_ERROR

        getUserDetails() should have (
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }

  "POST /user-details" should {
    "redirect to the user details confirmation page" in {
      stub when Get(authority) thenReturn stubbedTestUser
      stub when Get(Enrolments of stubbedTestUser) thenReturn Nil
      stub when Put(keystore(UserDetails)) thenReturn keystoreUserDetails(testUserDetails)

      postUserDetails(testUserDetails) should have (
        httpStatus(SEE_OTHER),
        redirectURI(s"/$baseURI/confirm-details")
      )
    }
  }
}
