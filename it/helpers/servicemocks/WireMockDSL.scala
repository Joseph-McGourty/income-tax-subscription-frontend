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

import com.github.tomakehurst.wiremock.client.{MappingBuilder, ResponseDefinitionBuilder}
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import helpers.servicemocks.WireMockDSL.HTTPVerbMapping.HTTPVerbStub
import play.api.http.Status
import play.api.libs.json.Writes



object WireMockDSL {
  val stub = WireMockStub

  object WireMockStub {
    def when(verbStub: HTTPVerbStub): WireMockStub = new WireMockStub(verbStub.mapping)
  }

  class WireMockStub(mapping: => MappingBuilder) {
    def thenReturn[T](value: T)(implicit writes: Writes[T]): StubMapping =
      stubFor(
        mapping
        .willReturn(
          aResponse().
            withStatus(Status.OK).withBody(writes.writes(value).toString())
        )
      )

    def thenReturn(status: Int): StubMapping =
      stubFor(
        mapping
          .willReturn(
            aResponse().
              withStatus(status))
          )
  }

  object HTTPVerbMapping {
    sealed trait HTTPVerbStub {
      val uri: String

      val mapping: MappingBuilder
    }

    case class Get(uri: String) extends HTTPVerbStub {
      override val mapping = get(urlMatching(uri))
    }

    case class Put(uri: String) extends HTTPVerbStub {
      override val mapping = put(urlMatching(uri))
    }
  }
}

