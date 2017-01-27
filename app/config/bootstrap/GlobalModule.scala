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

package config.bootstrap

import javax.inject.{Inject, Singleton}

import config.graphite.Graphite
import org.slf4j.MDC
import play.Logger
import play.api.{Application, Configuration}
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.filters.DeviceIdCookieFilter

import scala.util.matching.Regex


@Singleton
class GlobalModule @Inject()(val application: Application,
                             val graphite: Graphite,
                             val configuration: Configuration,
                             val auditConnector: AuditConnector
                            )
//  extends FrontendFilters
//  with GraphiteConfig
//  with RemovingOfTrailingSlashes
//  with Routing.BlockingOfPaths
//  with ErrorAuditingSettings
//  with ShowErrorPage
{
  lazy val appName = configuration.getString("appName").getOrElse("APP NAME NOT SET")
  lazy val enableSecurityHeaderFilter = configuration.getBoolean("security.headers.filter.enabled").getOrElse(true)
  lazy val deviceIdFilter = DeviceIdCookieFilter(appName, auditConnector)
  lazy val loggerDateFormat: Option[String] = configuration.getString("logger.json.dateformat")

  def blockedPathPattern: Option[Regex] = None

  Logger.info(s"Starting frontend : $appName : in mode : ${application.mode}")
  MDC.put("appName", appName)
  loggerDateFormat.foreach(str => MDC.put("logger.json.dateformat", str))
  ApplicationCrypto.verifyConfiguration()
  Logger.info(blockedPathPattern.fold
  (s"No requests will be blocked based on their path")
  (p => s"Any requests with paths that match $p will be blocked"))
}
