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

package services

import javax.inject.{Inject, Singleton}

import connectors.TaxEnrolmentConnector
import connectors.models.{Enrolment, Identifier}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class TaxEnrolmentService @Inject()(taxEnrolmentConnector: TaxEnrolmentConnector) {

  @tailrec
  private[services] final def hasNinoIdentifier(identifiers: Seq[Identifier]): Boolean = identifiers match {
    case h :: t =>
      h match {
        case Identifier(Enrolment.NINO_IDENTIFIER, _) => true
        case _ => hasNinoIdentifier(t)
      }
    case _ => false
  }

  @tailrec
  private[services] final def hasNinoEnrolment(enrolments: Seq[Enrolment]): Boolean = enrolments match {
    case h :: t =>
      h match {
        case Enrolment(Enrolment.HMRC_NI_ENROLMENT, identifiers: Seq[Identifier], Enrolment.ACTIVATED) =>
          hasNinoIdentifier(identifiers)
        case _ => hasNinoEnrolment(t)
      }
    case _ => false
  }

  def hasNinoInCid(implicit hc: HeaderCarrier): Future[Boolean] =
    taxEnrolmentConnector.getTaxEnrolment().map {
      case Some(seq) => hasNinoEnrolment(seq)
      case _ => false
    }

}
