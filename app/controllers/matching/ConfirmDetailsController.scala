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
import controllers.{BaseController, ITSASessionKey}
import models.matching.UserDetailsModel
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request}
import play.twirl.api.Html
import services.{KeystoreService, UserMatchingService}
import utils.Implicits._

@Singleton
class ConfirmDetailsController @Inject()(val baseConfig: BaseControllerConfig,
                                         val messagesApi: MessagesApi,
                                         val keystoreService: KeystoreService,
                                         val clientMatchingService: UserMatchingService
                                        ) extends BaseController {

  override val checkNino: Boolean = false

  def view(clientDetailsModel: UserDetailsModel)(implicit request: Request[_]): Html =
    views.html.matching.check_user_details(
      clientDetailsModel,
      routes.ConfirmDetailsController.submit(),
      backUrl
    )

  def show(): Action[AnyContent] = Authorised.async { implicit user =>
    implicit request =>
      keystoreService.fetchUserDetails() map {
        case Some(clientDetails) => Ok(view(clientDetails))
        case _ => Redirect(routes.UserDetailsController.show())
      }
  }

  def submit(): Action[AnyContent] = Authorised.async { implicit user =>
    implicit request =>
      keystoreService.fetchUserDetails() flatMap {
        case Some(clientDetails) => {
          for {
            matchFound <- clientMatchingService.matchClient(clientDetails)
          } yield matchFound
        }.flatMap {
          // TODO pending the result of the spike SAR-681, make calls to update auth?
          // the hash of the nino is put in the session so that controller predicates would not require to call keystore on every controller
          case true => Redirect(controllers.routes.HomeController.index()).addingToSession(ITSASessionKey.NINO -> clientDetails.ninoHash)
          // if for whatever reason the user managed to change the nino to an invalid entry then the session nino hash is removed
          case false => Redirect(controllers.matching.routes.UserDetailsErrorController.show()).removingFromSession(ITSASessionKey.NINO)
        }
        // if there are no client details redirect them back to client details
        case _ => Redirect(routes.UserDetailsController.show())
      }
  }

  lazy val backUrl: String = routes.UserDetailsController.show().url

}
