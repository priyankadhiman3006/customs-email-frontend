/*
 * Copyright 2022 HM Revenue & Customs
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

import org.joda.time.DateTime
import play.api.libs.json.Json
import uk.gov.hmrc.customs.emailfrontend.RandomUUIDGenerator

case class RequestCommon(regime: String, receiptDate: DateTime, acknowledgementReference: String)

object RequestCommon {

  import uk.gov.hmrc.customs.emailfrontend.DateTimeUtil._

  def apply(): RequestCommon =
    RequestCommon("CDS", receiptDate = dateTime, acknowledgementReference = RandomUUIDGenerator.generateUUIDAsString)

  implicit val formats = Json.format[RequestCommon]
}
