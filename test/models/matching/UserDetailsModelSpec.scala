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

package models.matching

import forms.validation.Constraints
import models.DateModel
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import utils.TestModels

class UserDetailsModelSpec extends PlaySpec with GuiceOneServerPerSuite {

  val testNino = TestModels.newNino

  // to lower case then add a space between each character
  val testNinoTransformed = testNino.toLowerCase.toCharArray.map(_.toString).reduce(_ + " " + _)

  val userDetails = UserDetailsModel(
    "",
    "",
    testNinoTransformed,
    DateModel("", "", "")
  )


  "the User Details Model" should {

    ".ninoInBackendFormat should return an upper cased nino with no spaces" in {
      val nino = userDetails.ninoInBackendFormat
      nino mustBe testNino
      nino.matches(Constraints.ninoRegex)
    }

    ".ninoInDisplayFormat should return an upper cased nino with spaces between each two characters" in {
      val nino = userDetails.ninoInDisplayFormat
      nino must not be testNino
      nino.replace(" ", "") mustBe testNino
      nino.matches(Constraints.ninoRegex)
    }

  }
}
