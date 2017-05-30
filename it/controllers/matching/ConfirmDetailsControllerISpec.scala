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

class ConfirmDetailsControllerISpec extends ComponentSpecBase {
  import IncomeTaxSubscriptionFrontend._

  "GET /confirm-details" should {
    "show the inputted details from keystore" in {
      stub when Get(authority) thenReturn stubbedTestUser
      stub when Get(Enrolments of stubbedTestUser) thenReturn Nil
      stub when Get(keystore) thenReturn keystoreUserDetails(testUserDetails)

      getConfirmDetails() should have (
        httpStatus(OK),
        pageTitle(Messages("user-details.summary.title")),
        elementTextByID("first-name-answer")(testUserDetails.firstName),
        elementTextByID("last-name-answer")(testUserDetails.lastName)
        //NINO and DOB are formatted differently from the model
      )
    }

    "redirect when there is no data in the keystore" in {
      stub when Get(authority) thenReturn stubbedTestUser
      stub when Get(Enrolments of stubbedTestUser) thenReturn Nil
      stub when Get(keystore) thenReturn emptyKeystoreData

      getConfirmDetails() should have (
        httpStatus(SEE_OTHER),
        redirectURI(baseURI + userDetailsURI)
      )
    }
  }
}
