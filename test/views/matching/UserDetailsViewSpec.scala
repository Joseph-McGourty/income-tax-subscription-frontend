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

package views.matching

import assets.MessageLookup.{Base => common, UserDetails => messages}
import forms.matching.UserDetailsForm
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import views.ViewSpecTrait

class UserDetailsViewSpec extends ViewSpecTrait {

  val action = ViewSpecTrait.testCall

  def page(isEditMode: Boolean) = views.html.matching.user_details(
    userDetailsForm = UserDetailsForm.userDetailsForm.form,
    postAction = action,
    isEditMode = isEditMode
  )(FakeRequest(), applicationMessages, appConfig)

  def documentCore(isEditMode: Boolean) = TestView(
    name = "User Details View",
    title = messages.title,
    heading = messages.heading,
    page = page(isEditMode = isEditMode)
  )

  "The User Details view" should {

    val testPage = documentCore(isEditMode = false)

    testPage.mustHavePara(messages.line1)

    val form = testPage.getForm("User Details form")(actionCall = action)

    form.mustHaveTextField(
      name = UserDetailsForm.firstName,
      label = messages.field1)

    form.mustHaveTextField(
      name = UserDetailsForm.lastName,
      label = messages.field2)

    form.mustHaveTextField(
      name = UserDetailsForm.nino,
      label = messages.field3,
      hint = messages.formhint1_line1 + " " + messages.formhint1_line2)

    form.mustHaveDateField(
      id = UserDetailsForm.dateOfBirth,
      legend = common.dateOfBirth,
      exampleDate = messages.formhint2)

    form.mustHaveContinueButton()

  }

  "The User Details view in edit mode" should {
    val editModePage = documentCore(isEditMode = true)

    editModePage.mustHaveUpdateButton()
  }

}
