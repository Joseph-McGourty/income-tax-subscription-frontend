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

package services.mocks

import models.BusinessNameModel
import org.mockito.Matchers
import org.mockito.Mockito._
import services.KeystoreService
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import util.MockTrait

import scala.concurrent.Future


trait MockKeystoreService extends MockTrait {

  import services.CacheConstants._

  val returnedCacheMap: CacheMap = CacheMap("", Map())

  object MockKeystoreService extends KeystoreService {
    override val session: SessionCache = mock[SessionCache]
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(MockKeystoreService.session)
    setupMockKeystoreSaveFunctions()
  }

  private final def mockFetchFromKeyStore[T](key: String, config: MFO[T]): Unit =
    config ifConfiguredThen (dataToReturn => when(MockKeystoreService.session.fetchAndGetEntry[T](Matchers.eq(key))(Matchers.any(), Matchers.any())).thenReturn(dataToReturn))

  private final def verifyKeystoreFetch[T](key: String, someCount: Option[Int]): Unit =
    someCount ifDefinedThen (count => verify(MockKeystoreService.session, times(count)).fetchAndGetEntry[T](Matchers.eq(key))(Matchers.any(), Matchers.any()))

  private final def verifyKeystoreSave[T](key: String, someCount: Option[Int]): Unit =
    someCount ifDefinedThen (count => verify(MockKeystoreService.session, times(count)).cache[T](Matchers.eq(key), Matchers.any())(Matchers.any(), Matchers.any()))


  protected final def setupMockKeystoreSaveFunctions(): Unit =
    when(MockKeystoreService.session.cache(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(returnedCacheMap))

  protected final def setupMockKeystore(
                                         fetchBusinessName: MFO[BusinessNameModel] = DoNotConfigure
                                       ): Unit = {
    mockFetchFromKeyStore[BusinessNameModel](BusinessName, fetchBusinessName)
  }

  protected final def verifyKeystore(
                                      fetchBusinessName: Option[Int] = None,
                                      saveBusinessName: Option[Int] = None
                                    ): Unit = {
    verifyKeystoreFetch(BusinessName, fetchBusinessName)
    verifyKeystoreSave(BusinessName, fetchBusinessName)
  }

}
