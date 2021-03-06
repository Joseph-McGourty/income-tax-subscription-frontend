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

package views.preferences

import assets.MessageLookup
import assets.MessageLookup.{PreferencesCallBack => messages}
import org.jsoup.Jsoup
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import views.ViewSpecTrait

class ContinueRegistrationViewSpec extends ViewSpecTrait {

  val action = ViewSpecTrait.testCall

  lazy val page = views.html.preferences.continue_registration(
    postAction = action
  )(FakeRequest(), applicationMessages, appConfig)

  "The Continue Registration view" should {
    val testPage = TestView(
      name = "Continue Registration View",
      title = messages.title,
      heading = messages.heading,
      page = page
    )

    val form = testPage.getForm("Continue Registration form")(actionCall = action)

    form.mustHaveGoBackButton()

    testPage.mustHaveALink(id = "sign-out", href = controllers.routes.SignOutController.signOut().url, text = messages.signOut)

  }
}
