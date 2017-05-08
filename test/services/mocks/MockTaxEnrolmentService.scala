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

package services.mocks

import connectors.mocks.MockTaxEnrolmentConnector
import connectors.models.{Enrolment, Identifier}
import play.api.http.Status._
import services.TaxEnrolmentService
import utils.JsonUtils._
import utils.{TestConstants, UnitTestTrait}

trait MockTaxEnrolmentService extends UnitTestTrait
  with MockTaxEnrolmentConnector {

  object TestTaxEnrolmentService extends TaxEnrolmentService(TestTaxEnrolmentConnector)

  def setuphasNinoInCid(taxEnrolmentResponse: Seq[Enrolment]): Unit = {
    setupLoadAuthority(loadAuthoritySuccess)
    setupMockGetTaxEnrolment(TestConstants.testCredId)(OK, taxEnrolmentResponse)
  }

  val enrolmentsWithoutNino: Seq[Enrolment] =
    Seq(
      Enrolment(Enrolment.UTR_IDENTIFIER, Seq(Identifier(Enrolment.UTR_IDENTIFIER, TestConstants.testUTR)), Enrolment.ACTIVATED)
    )

  val enrolmentsWithNino: String => Seq[Enrolment] = nino =>
    enrolmentsWithoutNino :+
      Enrolment(Enrolment.HMRC_NI_ENROLMENT, Seq(Identifier(Enrolment.NINO_IDENTIFIER, nino)), Enrolment.ACTIVATED)

}
