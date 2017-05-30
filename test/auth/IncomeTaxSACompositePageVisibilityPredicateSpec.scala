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

package auth

import play.api.http.Status
import play.api.test.Helpers._
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

class IncomeTaxSACompositePageVisibilityPredicateSpec extends UnitSpec with WithFakeApplication {

  val testPredicate = new IncomeTaxSACompositePageVisibilityPredicate(checkNino = true, enableUserDetails = true)

  "Calling IncomeTaxSACompositePageVisibilityPredicate with an auth context that has strong credentials and CL50 confidence" should {
    "result in page is visible" in {
      val predicate = testPredicate
      val authContext = ggUser.userCL50Context
      val result = predicate(authContext, fakeRequest)
      val pageVisibility = await(result)
      pageVisibility.isVisible shouldBe true
    }
  }

  "Calling IncomeTaxSACompositePageVisibilityPredicate with an auth context that has weak credentials and CL50 confidence" should {
    "result in page is visible" in {
      val predicate = testPredicate
      val authContext = ggUser.weakStrengthUserContext
      val result = predicate(authContext, fakeRequest)
      val pageVisibility = await(result)
      pageVisibility.isVisible shouldBe true
    }
  }

  "Calling IncomeTaxSACompositePageVisibilityPredicate with an auth context that has no NINO on the Auth Profile" when {

    "user details is enabled" should {
      s"checkNino is true and result in page is not visible and redirect to ${controllers.matching.routes.UserDetailsController.show().url}" in {
        val predicate = new IncomeTaxSACompositePageVisibilityPredicate(enableUserDetails = true, checkNino = true)
        val authContext = ggUser.userCL200NoAccountsContext
        val result = predicate(authContext, fakeRequest)
        val pageVisibility = await(result)
        pageVisibility.isVisible shouldBe false
        val r = await(pageVisibility.nonVisibleResult)
        status(r) shouldBe Status.SEE_OTHER
        redirectLocation(r).get shouldBe controllers.matching.routes.UserDetailsController.show().url
      }

      s"checkNino is false and result in page is visible" in {
        val predicate = new IncomeTaxSACompositePageVisibilityPredicate(enableUserDetails = true, checkNino = false)
        val authContext = ggUser.userCL200NoAccountsContext
        val result = predicate(authContext, fakeRequest)
        val pageVisibility = await(result)
        pageVisibility.isVisible shouldBe true
      }
    }

    "user details is disabled" should {
      s"checkNino is true and result in page is not visible and redirect to ${controllers.routes.NoNinoController.showNoNino()}" in {
        val predicate = new IncomeTaxSACompositePageVisibilityPredicate(enableUserDetails = false, checkNino = true)
        val authContext = ggUser.userCL200NoAccountsContext
        val result = predicate(authContext, fakeRequest)
        val pageVisibility = await(result)
        pageVisibility.isVisible shouldBe false
        val r = await(pageVisibility.nonVisibleResult)
        status(r) shouldBe Status.SEE_OTHER
        redirectLocation(r).get shouldBe controllers.routes.NoNinoController.showNoNino().url
      }

      s"checkNino is false and result in page is not visible and redirect to ${controllers.routes.NoNinoController.showNoNino()}" in {
        val predicate = new IncomeTaxSACompositePageVisibilityPredicate(enableUserDetails = false, checkNino = false)
        val authContext = ggUser.userCL200NoAccountsContext
        val result = predicate(authContext, fakeRequest)
        val pageVisibility = await(result)
        pageVisibility.isVisible shouldBe false
        val r = await(pageVisibility.nonVisibleResult)
        status(r) shouldBe Status.SEE_OTHER
        redirectLocation(r).get shouldBe controllers.routes.NoNinoController.showNoNino().url
      }
    }
  }


}
