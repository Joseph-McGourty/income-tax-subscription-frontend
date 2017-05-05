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

package connectors.mocks

import audit.Logging
import connectors.TaxEnrolmentConnector
import play.api.http.Status._
import play.api.libs.json.JsValue
import utils.JsonUtils._
import utils.{TestConstants, UnitTestTrait}


trait MockTaxEnrolmentConnector extends UnitTestTrait with MockHttp {

  object TestEnrolmentConnector extends TaxEnrolmentConnector(appConfig, mockHttpGet, app.injector.instanceOf[Logging])

  def setupMockLoadAuthority(status: Int, response: JsValue): Unit =
    setupMockHttpGet(s"${appConfig.authUrl}/auth/authority")(status, response)

  def setupLoadAuthority = (setupMockLoadAuthority _).tupled

  // n.b. this call returns fields other than cred id as well but we do not care what they are
  val loadAuthoritySuccess = (OK,
    s"""
       |{
       |  "anotherfield" : "ignored",
       |  "credId": "${TestConstants.testCredId}"
       |}
    """.stripMargin: JsValue)

  val loadAuthorityUnauthroised = (UNAUTHORIZED, """{}""": JsValue)


  def setupMockGetTaxEnrolment(credId: String)(status: Int, response: JsValue): Unit =
    setupMockHttpGet(s"${appConfig.taxEnrolmentsUrl}/users/$credId/enrolments")(status, response)

  def setupGetTaxEnrolment(credId: String) = (setupMockGetTaxEnrolment(credId) _).tupled

  val getTaxEnrolmentSuccess = (OK,
    s"""
      |[
      |    {
      |      "key": "IR-SA",
      |      "state": "ACTIVATED",
      |      "identifiers": [
      |       {
      |         "key": "UTR",
      |         "value": "${TestConstants.testUTR}"
      |       },
      |       {
      |        "key": "UTR",
      |        "value": "${TestConstants.testUTR}"
      |       }
      |
      |      ]
      |    },
      |    {
      |      "key": "HMRC-NI",
      |      "state": "ACTIVATED",
      |      "identifiers": [
      |       {
      |         "key": "NINO",
      |         "value": "${TestConstants.testNino}"
      |       }
      |      ]
      |    }
      |]
    """.stripMargin: JsValue)

  val getTaxEnrolmentMissingUsers = (BAD_REQUEST, """{}""": JsValue)

}
