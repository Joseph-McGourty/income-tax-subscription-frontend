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

    //test vectors are obtained from: http://www.di-mgt.com.au/sha_testvectors.html
    // and https://github.com/blooddy/blooddy_crypto/issues/17
    ".ninoHash should return a SHA-256 hash of the nino" in {
      val testVectors = Map[String, String](
        "" -> "e3b0c442 98fc1c14 9afbf4c8 996fb924 27ae41e4 649b934c a495991b 7852b855".replace(" ", ""),
        // the hash should be the version of nino whereby all of the characters have been converted to upper case and
        // all spaces should have been removed
        // therefore aaa should return the hash for AAA
        "aaa" -> "cb1ad2119d8fafb69566510ee712661f9f14b83385006ef92aec47f523a38358",
        "AAA" -> "cb1ad2119d8fafb69566510ee712661f9f14b83385006ef92aec47f523a38358",
        "ABC" -> "b5d4045c3f466fa91fe2cc6abe79232a1a57cdf104f7a26e716e0a1e2789df78",
        "123" -> "a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3"
      )
      for ((text, digest) <- testVectors) {
        withClue(s"test failed for text=$text") {
          val hash = userDetails.copy(nino = text).ninoHash
          hash.toLowerCase() mustBe digest
        }
      }
    }
  }
}
