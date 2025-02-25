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

package uk.gov.hmrc.customs.emailfrontend.utils

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import org.mockito.scalatest.MockitoSugar
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.inject.bind
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.CSRFTokenHelper._
import play.api.test.FakeRequest
import uk.gov.hmrc.customs.emailfrontend.controllers.actions.IdentifierAction

import scala.annotation.implicitNotFound
import scala.reflect.ClassTag

trait SpecBase extends AnyWordSpecLike with Matchers with MockitoSugar with OptionValues with ScalaFutures {

  def fakeRequest(method: String, path: String): FakeRequest[AnyContentAsEmpty.type] = {
    FakeRequest(method, path)
      .withHeaders("X-Session-ID" -> "someSessionId")
  }

  def fakeRequestWithCsrf(method: String, path: String): FakeRequest[AnyContentAsEmpty.type] = {
    fakeRequest(method, path)
      .withCSRFToken
      .asInstanceOf[FakeRequest[AnyContentAsEmpty.type]]
  }

  @implicitNotFound("Pass a type for the identifier action")
  def applicationBuilder[IA <: IdentifierAction](disableAuth: Boolean = false)(implicit c: ClassTag[IA]) : GuiceApplicationBuilder = {

    val overrides: List[GuiceableModule] = List(bind[Metrics].toInstance(new FakeMetrics))
    val optionalOverrides: List[GuiceableModule] = if (disableAuth) {
      Nil
    } else {
      List(bind[IdentifierAction].to[IA])
    }

    new GuiceApplicationBuilder()
      .overrides(overrides ::: optionalOverrides: _*)
      .configure("play.filters.csp.nonce.enabled" -> false,
        "auditing.enabled" -> "false",
        "microservice.metrics.graphite.enabled" -> "false",
        "metrics.enabled" -> "false")
  }
}

class FakeMetrics extends Metrics {
  override val defaultRegistry: MetricRegistry = new MetricRegistry
  override val toJson: String = "{}"
}

object TestImplicits {
  implicit class RemoveCsrf(s: String) {
    def removeCsrf(): String = {
      val regEx = "<[/]?input type[^>]*>"
      s.replaceAll(regEx, "")
    }
  }
}