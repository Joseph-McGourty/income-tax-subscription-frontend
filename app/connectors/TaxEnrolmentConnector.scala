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

package connectors

import javax.inject.{Inject, Singleton}

import audit.Logging
import config.AppConfig
import connectors.models.Enrolment
import play.api.http.Status.OK
import play.api.libs.json.{Json, Reads}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpResponse}
import utils.Implicits._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class TaxEnrolmentConnector @Inject()(appConfig: AppConfig,
                                      val http: HttpGet,
                                      logging: Logging) extends RawResponseReads {

  def loadAuthority()(implicit hc: HeaderCarrier, r: Reads[AuthResponse]): Future[AuthResponse] = {
    http.GET[HttpResponse](s"${appConfig.authUrl}/auth/authority").map {
      response =>
        response.status match {
          case OK =>
            response.json.as[AuthResponse]
          case _ =>
            throw new RuntimeException(s"status=${response.status}\nbody=${response.body}")
        }
    }
  }

  def getTaxEnrolment()(implicit hc: HeaderCarrier, r: Reads[AuthResponse]): Future[Option[Seq[Enrolment]]] = {
    val mhc = hc.withExtraHeaders("True-Client-IP" -> "ITSA-AGENT")
    //    val credId = user.authContext.enrolmentsUri.get
    loadAuthority()(mhc, implicitly).flatMap {
      case AuthResponse(credId) =>
        val getUrl = s"${appConfig.taxEnrolmentsUrl}/users/$credId/enrolments"
        lazy val requestDetails: Map[String, String] = Map("credId" -> credId)
        logging.debug(s"TaxEnrolmentConnector.getTaxEnrolment Request:\n$requestDetails")
        http.GET[HttpResponse](getUrl)(implicitly, mhc).map {
          response =>
            response.status match {
              case OK =>
                logging.debug(s"TaxEnrolmentConnector.getTaxEnrolment Response:\n${response.body}")
                response.json.as[Seq[Enrolment]]
              case _ =>
                logging.warn(s"Get Tax enrolment responded with a unexpected error\n${response.body}")
                None
            }
        }
    }
  }

}

case class AuthResponse(credId: String)

object AuthResponse {
  implicit val format = Json.format[AuthResponse]
}
