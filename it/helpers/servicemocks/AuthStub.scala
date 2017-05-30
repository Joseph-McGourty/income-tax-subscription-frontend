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

package helpers.servicemocks

import helpers.{IntegrationTestConstants, WiremockHelper}
import org.joda.time.{DateTime, DateTimeZone}
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.frontend.auth.connectors.domain
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{Authority, ConfidenceLevel, CredentialStrength}
import IntegrationTestConstants._
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock._

object AuthStub {
  //Stub for the Auth microservice

  val idsLink = "/uri/to/ids"
  val authority = "/auth/authority"

  val GETAuthority: MappingBuilder = get(urlMatching(authority))

  //GET /auth/authority
  def stubGetAuthority(authority: Authority): Unit = {
    val authBody = Json.toJson(authority).toString

    WiremockHelper.stubGet(AuthStub.authority, Status.OK, authBody)
  }

  lazy val authorisedUserAccounts = domain.Accounts(paye = Some(domain.PayeAccount(link = "/paye/abc", nino = Nino(testNino))))
  val loggedInAt = Some(new DateTime(2015, 11, 22, 11, 33, 15, 234, DateTimeZone.UTC))
  val previouslyLoggedInAt = Some(new DateTime(2014, 8, 3, 9, 25, 44, 342, DateTimeZone.UTC))

  lazy val stubbedTestUser: Authority =
    Authority(uri = userId,
      accounts = authorisedUserAccounts,
      loggedInAt = loggedInAt,
      previouslyLoggedInAt = previouslyLoggedInAt,
      credentialStrength = CredentialStrength.Strong,
      confidenceLevel = ConfidenceLevel.L50,
      userDetailsLink = None,
      enrolments = None,
      ids = None,
      legacyOid = ""
    )
}
