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

package controllers

import javax.inject.Inject

import config.{AppConfig, BaseControllerConfig}
import forms.{IncomeSourceForm, NotEligibleForm, PropertyIncomeForm}
import models.NotEligibleModel
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.twirl.api.Html
import services.KeystoreService
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.InternalServerException

import scala.concurrent.Future


class NoNinoController @Inject()(implicit val applicationConfig: AppConfig,
                                 val messagesApi: MessagesApi
                                ) extends FrontendController with I18nSupport {

  val showNoNino: Action[AnyContent] = Action.async {
    implicit request => Future.successful(Ok(views.html.no_nino(postAction = controllers.routes.NoNinoController.submitNoNino())))
  }

  //TODO Need to update for signout functionality
  val submitNoNino: Action[AnyContent] = TODO

}