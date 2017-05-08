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

package testonly.controllers

import javax.inject.{Inject, Singleton}

import common.Constants
import config.BaseControllerConfig
import connectors.models.Enrolment
import controllers.BaseController
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.Request
import play.twirl.api.Html
import services.EnrolmentService
import testonly.connectors.{MatchingStubConnector, UserData, Value}
import testonly.forms.ClientToStubForm
import testonly.models.ClientToStubModel
import utils.Implicits._

//$COVERAGE-OFF$Disabling scoverage on this class as it is only intended to be used by the test only controller

/*
* This controller is used to create a user stub entry to simulate data in CID
* It will create the record in the dynamic-test-data mongo db
*/
@Singleton
class MatchingStubController @Inject()(override val baseConfig: BaseControllerConfig,
                                       override val messagesApi: MessagesApi,
                                       matchingStubConnector: MatchingStubConnector,
                                       enrolmentService: EnrolmentService
                                      ) extends BaseController {

  def view(clientToStubForm: Form[ClientToStubModel])(implicit request: Request[_]): Html =
    testonly.views.html.stub_user(
      clientToStubForm,
      routes.MatchingStubController.stubUser()
    )

  def show = Authorised.async { implicit user =>
    implicit request =>
      lazy val noUTR = Ok(view(ClientToStubForm.clientToStubForm.form.fill(UserData().toClientToStubModel)))
      enrolmentService.getAllEnrolment.flatMap { enrolments =>
        enrolments.map(_.find(_.key == Constants.irSaServiceName)) match {
          case Some(Some(irsa)) =>
            irsa.identifiers.find(_.key == Enrolment.UTR_IDENTIFIER).fold(noUTR)(
              id => Ok(view(ClientToStubForm.clientToStubForm.form.fill(UserData(sautr = Value(id.value)).toClientToStubModel)))
            )
          case _ => noUTR
        }
      }
  }

  def stubUser = Authorised.async { implicit user =>
    implicit request =>
      ClientToStubForm.clientToStubForm.bindFromRequest.fold(
        formWithErrors => BadRequest(view(formWithErrors)),
        userDetails =>
          matchingStubConnector.newUser(userDetails) map {
            _ => Ok(testonly.views.html.show_stubbed_details(userDetails))
          }
      )
  }

}

// $COVERAGE-ON$
