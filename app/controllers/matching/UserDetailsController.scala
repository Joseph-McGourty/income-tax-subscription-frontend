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

package controllers.matching

import javax.inject.{Inject, Singleton}

import config.BaseControllerConfig
import controllers.BaseController
import forms.matching.UserDetailsForm
import models.matching.UserDetailsModel
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request}
import play.twirl.api.Html
import services.KeystoreService

import scala.concurrent.Future

@Singleton
class UserDetailsController @Inject()(val baseConfig: BaseControllerConfig,
                                      val messagesApi: MessagesApi,
                                      val keystoreService: KeystoreService
                                       ) extends BaseController {

  def view(clientDetailsForm: Form[UserDetailsModel], isEditMode: Boolean)(implicit request: Request[_]): Html =
    views.html.matching.user_details(
      clientDetailsForm,
      controllers.matching.routes.UserDetailsController.submit(editMode = isEditMode),
      isEditMode
    )

  def show(isEditMode: Boolean): Action[AnyContent] = Authorised.async { implicit user =>
    implicit request =>
      keystoreService.fetchUserDetails() map {
        clientDetails => Ok(view(UserDetailsForm.userDetailsForm.form.fill(clientDetails), isEditMode = isEditMode))
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authorised.async { implicit user =>
    implicit request =>
      UserDetailsForm.userDetailsForm.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, isEditMode = isEditMode))),
        clientDetails =>
          keystoreService.saveUserDetails(clientDetails).map(_ => Redirect(routes.ConfirmDetailsController.show()))
      )
  }

}
