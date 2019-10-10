/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.customs.emailfrontend.model

import play.api.libs.json.{JsResult, JsValue, Reads}

case class SubscriptionDisplayResponse(email: Option[String], paramValue: Option[String])

object SubscriptionDisplayResponse {
  implicit val etmpReads: Reads[SubscriptionDisplayResponse] = new Reads[SubscriptionDisplayResponse] {
    def reads(json: JsValue): JsResult[SubscriptionDisplayResponse] = {
      for {
        email <- (json \ "subscriptionDisplayResponse" \ "responseDetail" \ "contactInformation" \ "emailAddress").validateOpt[String]
        paramValue <- (json \ "subscriptionDisplayResponse" \ "responseCommon" \ "returnParameters" \ 0 \ "paramValue").validateOpt[String]
      } yield {
        SubscriptionDisplayResponse(email, paramValue)
      }
    }
  }
}