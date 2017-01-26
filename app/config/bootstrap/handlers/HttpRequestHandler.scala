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

package config.bootstrap.handlers


import javax.inject.Inject

import config.bootstrap.GlobalModule
import play.Logger
import play.api.http._
import play.api.mvc._
import play.api.routing.Router

class HttpRequestHandler @Inject()(globalModule: GlobalModule,
                                   router: Router,
                                   errorHandler: HttpErrorHandler,
                                   configuration: HttpConfiguration, filters: HttpFilters)
  extends DefaultHttpRequestHandler(router, errorHandler, configuration, filters.filters: _*) {

  override def routeRequest(request: RequestHeader): Option[Handler] = {
    lazy val routeRequestDefault = super.routeRequest(request).orElse {
      Some(request.path).filter(_.endsWith("/")).flatMap(p => super.routeRequest(request.copy(path = p.dropRight(1))))
    }

    globalModule.blockedPathPattern match {
      case Some(isBlockedPath) => request.path match {
        case isBlockedPath() =>
          Logger.debug(s"Blocked request for ${request.path} as it matches $isBlockedPath")
          None
        case isNotBlockedPath => routeRequestDefault
      }
      case None => routeRequestDefault
    }
  }

}
