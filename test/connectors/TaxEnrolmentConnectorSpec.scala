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

package connectors

import connectors.mocks.MockTaxEnrolmentConnector
import connectors.models.{Enrolment, Identifier}
import org.scalatest.Matchers._
import play.api.test.Helpers._
import uk.gov.hmrc.play.http.InternalServerException
import utils.{TestConstants, UnitTestTrait}

class TaxEnrolmentConnectorSpec extends UnitTestTrait with MockTaxEnrolmentConnector {


  "TaxEnrolmentConnector.loadAuthority" should {

    def call = await(TestTaxEnrolmentConnector.loadAuthority())

    "return the cred it from a success response" in {
      setupLoadAuthority(loadAuthoritySuccess)
      val result = call
      result mustBe AuthResponse(TestConstants.testCredId)
    }

    "return a failed future in an unsuccessful response" in {
      setupLoadAuthority(loadAuthorityUnauthroised)
      val thrown = intercept[Exception] {
        call
      }
      thrown.isInstanceOf[InternalServerException] shouldBe true
    }

  }

  "TaxEnrolmentConnector.getTaxEnrolment" should {

    def loadAuth = await(TestTaxEnrolmentConnector.loadAuthority())

    def call = await(TestTaxEnrolmentConnector.getTaxEnrolment())

    "return the enrolments from a success response" in {
      setupLoadAuthority(loadAuthoritySuccess)
      setupGetTaxEnrolment(loadAuth.credId)(getTaxEnrolmentSuccess)

      val result = call
      result mustBe Some(Seq[Enrolment](
        Enrolment("IR-SA", Seq[Identifier](Identifier("UTR", TestConstants.testUTR), Identifier("UTR", TestConstants.testUTR)), "ACTIVATED"),
        Enrolment("HMRC-NI", Seq[Identifier](Identifier("NINO", TestConstants.testNino)), "ACTIVATED")
      ))
    }

    "return None from a failure response" in {
      setupLoadAuthority(loadAuthoritySuccess)
      setupGetTaxEnrolment(loadAuth.credId)(getTaxEnrolmentMissingUsers)

      val result = call
      result mustBe None
    }

  }

}
