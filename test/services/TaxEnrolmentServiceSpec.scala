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

import play.api.test.Helpers._
import services.mocks.MockTaxEnrolmentService
import utils.TestConstants

class TaxEnrolmentServiceSpec extends MockTaxEnrolmentService {

  "EnrolmentService.setuphasNinoInCid" should {
    def result = await(TestTaxEnrolmentService.hasNinoInCid)

    "when tax enrolment returned an enrolment with NINO then return true" in {
      setuphasNinoInCid(enrolmentsWithNino(TestConstants.testNino))
      await(TestTaxEnrolmentConnector.getTaxEnrolment()) mustBe Some(enrolmentsWithNino(TestConstants.testNino))
      result mustBe true
    }

    "when tax enrolment does not return an enrolment with NINO then return false" in {
      setuphasNinoInCid(enrolmentsWithoutNino)
      result mustBe false
    }
  }

}
