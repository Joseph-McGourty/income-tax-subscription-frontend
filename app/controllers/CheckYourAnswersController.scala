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

import audit.Logging
import config.BaseControllerConfig
import connectors.models.subscription.FESuccessResponse
import play.api.i18n.MessagesApi
import services.{KeystoreService, SubscriptionService}
import uk.gov.hmrc.play.http.InternalServerException

import scala.concurrent.Future

@Singleton
class CheckYourAnswersController @Inject()(val baseConfig: BaseControllerConfig,
                                           val messagesApi: MessagesApi,
                                           val keystoreService: KeystoreService,
                                           val middleService: SubscriptionService,
                                           logging: Logging
                                          ) extends BaseController {

  import services.CacheUtil._

  val show = Authorised.async { implicit user =>
    implicit request =>
      keystoreService.fetchAll() map {
        case Some(cache) =>
          Ok(views.html.check_your_answers(cache.getSummary,
            controllers.routes.CheckYourAnswersController.submit(),
            backUrl = backUrl
          ))
        case _ =>
          logging.info("User attempted to view 'Check Your Answers' without any keystore cached data")
          InternalServerError
      }
  }

  val submit = Authorised.async { implicit user =>
    implicit request =>
      keystoreService.fetchAll() flatMap {
        case Some(source) =>
          for {
            nino <- keystoreService.getNino()
              .collect { case Some(nino) => nino}
              .recoverWith { case _ => error("Cannot find the nino for the user") }
            mtditid <- middleService.submitSubscription(nino, source.getSummary())
              .collect { case Some(FESuccessResponse(Some(id))) => id }
              .recoverWith { case _ => error("Successful response not received from submission") }
            cacheMap <- keystoreService.saveSubscriptionId(mtditid)
              .recoverWith { case _ => error("Failed to save to keystore") }
          } yield Redirect(controllers.routes.ConfirmationController.showConfirmation())
        case _ =>
          error("User attempted to submit 'Check Your Answers' without any keystore cached data")
      }
  }

  def error(message: String): Future[Nothing] = {
    logging.warn(message)
    Future.failed(new InternalServerException(message))
  }

  lazy val backUrl: String = controllers.routes.TermsController.showTerms().url

}
