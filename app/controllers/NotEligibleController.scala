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

import javax.inject.{Inject, Singleton}

import config.BaseControllerConfig
import forms.{IncomeSourceForm, NotEligibleForm}
import models.NotEligibleModel
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.twirl.api.Html
import services.KeystoreService
import uk.gov.hmrc.play.http.InternalServerException

import scala.concurrent.Future

@Singleton
class NotEligibleController @Inject()(val baseConfig: BaseControllerConfig,
                                      val messagesApi: MessagesApi,
                                      val keystoreService: KeystoreService
                                     ) extends BaseController {

  def view(notEligibleForm: Form[NotEligibleModel], backUrl: String)(implicit request: Request[_]): Html =
    views.html.not_eligible(
      notEligibleForm = notEligibleForm,
      postAction = controllers.routes.NotEligibleController.submitNotEligible(),
      backUrl = backUrl
    )

  val showNotEligible: Action[AnyContent] = Authorised.async { implicit user =>
    implicit request =>
      for {
        choice <- keystoreService.fetchNotEligible
        backUrl <- backUrl
      } yield Ok(view(NotEligibleForm.notEligibleForm.fill(choice), backUrl))
  }

  val submitNotEligible: Action[AnyContent] = Authorised.async { implicit user =>
    implicit request =>
      NotEligibleForm.notEligibleForm.bindFromRequest.fold(
        formWithErrors => backUrl.map(backUrl => BadRequest(view(notEligibleForm = formWithErrors, backUrl = backUrl))),
        choice => {
          keystoreService.saveNotEligible(choice).flatMap { _ =>
            choice.choice match {
              case NotEligibleForm.option_signup => signUp
              case NotEligibleForm.option_signout => signOut
            }
          }
        }
      )
  }

  def signUp(implicit request: Request[_]): Future[Result] =
    keystoreService.fetchIncomeSource() map {
      case Some(incomeSource) =>
        incomeSource.source match {
          case IncomeSourceForm.option_business | IncomeSourceForm.option_both =>
            Redirect(controllers.business.routes.BusinessAccountingPeriodDateController.showAccountingPeriod())
          case IncomeSourceForm.option_property =>
            Redirect(controllers.preferences.routes.PreferencesController.checkPreferences())
        }
      case _ => throw new InternalServerException("NotEligibleController: fetchIncomeSource failed")
    }


  def signOut(implicit request: Request[_]): Future[Result] = Future.successful(NotImplemented)

  def backUrl(implicit request: Request[_]): Future[String] = Future.successful(controllers.routes.IncomeSourceController.showIncomeSource().url)

}
