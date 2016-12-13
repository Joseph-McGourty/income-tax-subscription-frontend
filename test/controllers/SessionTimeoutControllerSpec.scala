/*
 * Copyright 2016 HM Revenue & Customs
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

package controllers

import akka.actor._
import akka.stream._
import assets.MessageLookup
import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import org.jsoup.Jsoup

class SessionTimeoutControllerSpec extends UnitSpec with WithFakeApplication {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  "Calling the timeout action of the SessionTimeoutController" should {

    lazy val result = SessionTimeoutController.timeout(FakeRequest())
    lazy val document = Jsoup.parse(bodyOf(result))

    "return 200" in {
      status(result) shouldBe Status.OK
    }

    "return HTML" in {
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }

    s"have the title '${MessageLookup.timeout.title}'" in {
      document.title() shouldBe MessageLookup.timeout.title
    }
  }
}
