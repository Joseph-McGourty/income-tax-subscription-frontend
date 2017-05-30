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

import helpers.ImplicitConversions._
import helpers.IntegrationTestConstants._
import helpers.WiremockHelper
import models.matching.UserDetailsModel
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import services.CacheConstants

object KeystoreStub {
  val keystore = s"/keystore/income-tax-subscription-frontend/$SessionId"
  def keystore(key: String): String = s"$keystore/data/$key"

  def stubFetchUserDetails(userDetailsModel: UserDetailsModel): Unit = {
    val model = Json.toJson(userDetailsModel).toString

    val body =
      s"""{
          |"id": "$SessionId",
          |"data": {${CacheConstants.UserDetails} : {$model}}
          |}""".stripMargin

    WiremockHelper.stubGet(keystore, Status.OK, body)
  }

  case class KeystoreData(id: String, data: Map[String, JsValue])

  implicit val keystoreFormat = Json.format[KeystoreData]

  val emptyKeystoreData = KeystoreData(SessionId, Map.empty)
  def keystoreUserDetails(data: UserDetailsModel): KeystoreData = KeystoreData(SessionId, Map(CacheConstants.UserDetails -> data))
}

