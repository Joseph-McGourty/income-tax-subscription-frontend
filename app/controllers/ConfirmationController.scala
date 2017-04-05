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

import java.time.LocalDate
import javax.inject.{Inject, Singleton}

import audit.Logging
import config.BaseControllerConfig
import models.DateModel.dateConvert
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import services.KeystoreService

import scala.concurrent.Future

@Singleton
class ConfirmationController @Inject()(val baseConfig: BaseControllerConfig,
                                       val messagesApi: MessagesApi,
                                       val keystoreService: KeystoreService,
                                       val logging: Logging
                                      ) extends BaseController {

  val showConfirmation: Action[AnyContent] = Authorised.asyncForEnrolled { implicit user =>
    implicit request =>
      keystoreService.fetchSubscriptionId.map {
        case Some(id) =>
          Ok(views.html.confirmation(
            subscriptionId = id,
            submissionDate = dateConvert(LocalDate.now()),
            controllers.routes.SignOutController.signOut()
          ))
        case _ =>
          logging.info("User attempted to view confirmation with no subscriptionId stored in Keystore")
          InternalServerError
      }
  }

}
