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

package uk.gov.hmrc.customs.emailfrontend.controllers

import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.{eq => meq}
import org.scalatest.BeforeAndAfterEach
import play.api.test.FakeRequest
import play.api.test.Helpers.{redirectLocation, _}
import play.api.{Application, inject}
import uk.gov.hmrc.customs.emailfrontend.config.ErrorHandler
import uk.gov.hmrc.customs.emailfrontend.connectors.SubscriptionDisplayConnector
import uk.gov.hmrc.customs.emailfrontend.model._
import uk.gov.hmrc.customs.emailfrontend.services.{EmailVerificationService, Save4LaterService}
import uk.gov.hmrc.customs.emailfrontend.utils.{FakeIdentifierAgentAction, SpecBase}
import uk.gov.hmrc.http.{HeaderCarrier, HttpException}

import scala.concurrent.Future

class WhatIsYourEmailControllerSpec extends SpecBase with BeforeAndAfterEach {

  trait Setup {

    protected val mockSave4LaterService: Save4LaterService = mock[Save4LaterService]
    protected val mockSubscriptionDisplayConnector: SubscriptionDisplayConnector = mock[SubscriptionDisplayConnector]
    protected val mockEmailVerificationService: EmailVerificationService = mock[EmailVerificationService]
    protected val mockErrorHandler: ErrorHandler = mock[ErrorHandler]

    protected val app: Application = applicationBuilder[FakeIdentifierAgentAction]()
      .overrides(inject.bind[Save4LaterService].toInstance(mockSave4LaterService),
        inject.bind[SubscriptionDisplayConnector].toInstance(mockSubscriptionDisplayConnector),
        inject.bind[EmailVerificationService].toInstance(mockEmailVerificationService)
      ).build()
  }

  private val emailVerificationTimeStamp = "2016-3-17T9:30:47.114"
  private val someSubscriptionDisplayResponse = SubscriptionDisplayResponse(Some("test@test.com"), Some(emailVerificationTimeStamp), None, None)
  private val someSubscriptionDisplayResponseWithNoEmailVerificationTimeStamp = SubscriptionDisplayResponse(Some("test@test.com"), None, None, None)
  private val someSubscriptionDisplayResponseWithStatus = SubscriptionDisplayResponse(None, None, Some("statusText"), Some("FAIL"))
  private val noneSubscriptionDisplayResponse = SubscriptionDisplayResponse(None, None, None, None)

  "WhatIsYourEmailController" should {

    "have a status of SEE_OTHER for show method when email found in cache and email status is AmendmentCompleted" in new Setup {
      when(mockSave4LaterService.fetchEmail(any)(any))
        .thenReturn(Future.successful(Some(EmailDetails(None, "test@email", Some(DateTime.now().minusDays(2))))))

      when(mockSave4LaterService.remove(any)(any))
        .thenReturn(Future.successful(Right(())))

      running(app) {

        val request = FakeRequest(GET, routes.WhatIsYourEmailController.show.url)

        val result = route(app, request).value
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get shouldBe routes.WhatIsYourEmailController.create.url
      }
    }

    "have a status of SEE_OTHER for show method when email is not found in cache and email status is AmendmentNotDetermined" in new Setup  {
      when(mockSave4LaterService.fetchEmail(any)(any))
        .thenReturn(Future.successful(None))

      running(app) {

        val request = FakeRequest(GET, routes.WhatIsYourEmailController.show.url)

        val result = route(app, request).value
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get shouldBe routes.WhatIsYourEmailController.create.url
      }

    }

    "have a status of SEE_OTHER for show method when email found in cache with no timestamp and email is verified" in new Setup  {
      when(mockSave4LaterService.fetchEmail(any)(any))
        .thenReturn(Future.successful(Some(EmailDetails(None, "test@email", None))))

      when(mockEmailVerificationService.isEmailVerified(meq("test@email"))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(true)))

      running(app) {

        val request = FakeRequest(GET, routes.WhatIsYourEmailController.show.url)

        val result = route(app, request).value
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get shouldBe routes.EmailConfirmedController.show.url
      }
    }

    "have a status of SEE_OTHER for show method when email found in cache with no timestamp and email is not verified" in new Setup  {
      when(mockSave4LaterService.fetchEmail(any)(any))
        .thenReturn(Future.successful(Some(EmailDetails(None, "test@email", None))))

      when(mockEmailVerificationService.isEmailVerified(meq("test@email"))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(false)))

      running(app) {

        val request = FakeRequest(GET, routes.WhatIsYourEmailController.show.url)

        val result = route(app, request).value
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get shouldBe routes.CheckYourEmailController.show.url
      }
    }

    "have a status of SEE_OTHER for show method when email found in cache with timestamp for AmendmentInProgress" in new Setup  {
      when(mockSave4LaterService.fetchEmail(any)(any))
        .thenReturn(Future.successful(Some(EmailDetails(None, "test@email", Some(DateTime.now())))))

      running(app) {

        val request = FakeRequest(GET, routes.WhatIsYourEmailController.show.url)

        val result = route(app, request).value
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get shouldBe routes.AmendmentInProgressController.show.url
      }
    }

    "have a status of SEE_OTHER for show method email is not found in cache " in new Setup  {
      when(mockSave4LaterService.fetchEmail(any)(any))
        .thenReturn(Future.successful(None))

      running(app) {

        val request = FakeRequest(GET, routes.WhatIsYourEmailController.show.url)

        val result = route(app, request).value
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get shouldBe routes.WhatIsYourEmailController.create.url
      }
    }

    "have a status of OK for create method when verified email found in subscription display response" in new Setup  {
      when(mockSave4LaterService.fetchEmail(any)(any))
        .thenReturn(Future.successful(None))

      when(mockSubscriptionDisplayConnector.subscriptionDisplay(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(someSubscriptionDisplayResponse))

      running(app) {

        val request = FakeRequest(GET, routes.WhatIsYourEmailController.create.url)

        val result = route(app, request).value
        status(result) shouldBe OK
        contentAsString(result) should include("test@test.com")
      }
    }

    "have a status of OK for create method when unverified email found in subscription display response" in new Setup  {
      when(mockSave4LaterService.fetchEmail(any)(any))
        .thenReturn(Future.successful(None))

      when(mockSubscriptionDisplayConnector.subscriptionDisplay(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(someSubscriptionDisplayResponseWithNoEmailVerificationTimeStamp))

      running(app) {

        val request = FakeRequest(GET, routes.WhatIsYourEmailController.create.url)

        val result = route(app, request).value
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get shouldBe routes.WhatIsYourEmailController.verify.url
      }
    }

    "have a status of SEE_OTHER for create method when no email found in subscription display response but returned OK" in new Setup  {
      when(mockSave4LaterService.fetchEmail(any)(any))
        .thenReturn(Future.successful(None))

      when(mockSubscriptionDisplayConnector.subscriptionDisplay(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(noneSubscriptionDisplayResponse))

      running(app) {

        val request = FakeRequest(GET, routes.WhatIsYourEmailController.create.url)

        val result = route(app, request).value
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get shouldBe routes.WhatIsYourEmailController.verify.url
      }
    }

    "have a status of OK for create method when email found in cache with no timestamp" in new Setup  {
      when(mockSave4LaterService.fetchEmail(any)(any))
        .thenReturn(Future.successful(Some(EmailDetails(Some("old@email"), "test@email", None))))

      when(mockSubscriptionDisplayConnector.subscriptionDisplay(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(someSubscriptionDisplayResponse))

      running(app) {

        val request = FakeRequest(GET, routes.WhatIsYourEmailController.create.url)

        val result = route(app, request).value
        status(result) shouldBe OK
      }
    }

    "have a status of SEE_OTHER for create method when current email not found in cache with no timestamp" in new Setup  {
      when(mockSave4LaterService.fetchEmail(any)(any))
        .thenReturn(Future.successful(Some(EmailDetails(None, "test@email", None))))

      when(mockSubscriptionDisplayConnector.subscriptionDisplay(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(someSubscriptionDisplayResponse))

      running(app) {

        val request = FakeRequest(GET, routes.WhatIsYourEmailController.create.url)

        val result = route(app, request).value
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe routes.WhatIsYourEmailController.problemWithService.url
      }
    }

    "have a status of SEE_OTHER for create method when the bookmark url is used and user already completed success amend email journey" in new Setup  {
      when(mockSave4LaterService.fetchEmail(any)(any))
        .thenReturn(Future.successful(Some(EmailDetails(None, "test@email", Some(DateTime.now())))))

      when(mockSubscriptionDisplayConnector.subscriptionDisplay(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(someSubscriptionDisplayResponse))

      running(app) {

        val request = FakeRequest(GET, routes.WhatIsYourEmailController.create.url)

        val result = route(app, request).value
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe routes.AmendmentInProgressController.show.url
      }
    }

    "have a status of SEE_OTHER for create method when unverified email found in subscription display response" in new Setup {
      when(mockSave4LaterService.fetchEmail(any)(any))
        .thenReturn(Future.successful(None))

      when(mockSubscriptionDisplayConnector.subscriptionDisplay(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(someSubscriptionDisplayResponseWithNoEmailVerificationTimeStamp))

      running(app) {

        val request = FakeRequest(GET, routes.WhatIsYourEmailController.create.url)

        val result = route(app, request).value
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe routes.WhatIsYourEmailController.verify.url
      }
    }

    "show 'there is a problem with the service' page when subscription display is failed" in new Setup  {
      when(mockSave4LaterService.fetchEmail(any)(any))
        .thenReturn(Future.successful(None))

      when(mockSubscriptionDisplayConnector.subscriptionDisplay(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new HttpException("Failed", BAD_REQUEST)))

      running(app) {

        val request = FakeRequest(GET, routes.WhatIsYourEmailController.create.url)

        val result = route(app, request).value
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe routes.WhatIsYourEmailController.problemWithService.url
      }
    }

    "show 'there is a problem with the service' page when subscription display response has paramValue 'FAIL' with no email" in new Setup  {
      when(mockSave4LaterService.fetchEmail(any)(any))
        .thenReturn(Future.successful(None))

      when(mockSubscriptionDisplayConnector.subscriptionDisplay(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(someSubscriptionDisplayResponseWithStatus))

      running(app) {

        val request = FakeRequest(GET, routes.WhatIsYourEmailController.create.url)

        val result = route(app, request).value
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe routes.WhatIsYourEmailController.problemWithService.url
      }
    }

    "have a status of OK for verify method" in new Setup  {
      when(mockSave4LaterService.fetchEmail(any)(any))
        .thenReturn(Future.successful(None))

      running(app) {

        val request = FakeRequest(GET, routes.WhatIsYourEmailController.verify.url)

        val result = route(app, request).value
        status(result) shouldBe OK
      }
    }

    "have a status of OK for verify method when email found in cache with no timestamp" in new Setup  {
      when(mockSave4LaterService.fetchEmail(any)(any))
        .thenReturn(Future.successful(Some(EmailDetails(None, "test@email", None))))

      running(app) {

        val request = FakeRequest(GET, routes.WhatIsYourEmailController.verify.url)

        val result = route(app, request).value
        status(result) shouldBe OK
      }
    }

    "have a status of SEE_OTHER for verify method when the bookmark url is used and user already complete success amend email journey " in new Setup  {
      when(mockSave4LaterService.fetchEmail(any)(any))
        .thenReturn(Future.successful(Some(EmailDetails(None, "test@email", Some(DateTime.now())))))

      running(app) {

        val request = FakeRequest(GET, routes.WhatIsYourEmailController.verify.url)

        val result = route(app, request).value
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe routes.AmendmentInProgressController.show.url
      }
    }
    "have a status of Bad Request when no email is provided in the form" in new Setup {
      when(mockSubscriptionDisplayConnector.subscriptionDisplay(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(someSubscriptionDisplayResponse))

      running(app) {

        val requestWithForm = fakeRequestWithCsrf(POST, routes.WhatIsYourEmailController.submit.url)
          .withFormUrlEncodedBody(("email", ""))
        val result = route(app, requestWithForm).value
        status(result) shouldBe BAD_REQUEST
      }
    }

    "show 'there is a problem with the service' page when subscription display return response with no email or status for submit" in new Setup  {
      when(mockSubscriptionDisplayConnector.subscriptionDisplay(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(noneSubscriptionDisplayResponse))

      running(app) {

        val request = FakeRequest(POST, routes.WhatIsYourEmailController.submit.url)

        val result = route(app, request).value
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe routes.WhatIsYourEmailController.problemWithService.url
      }
    }

    "have a status of Bad Request when the email is invalid" in new Setup {
      when(mockSubscriptionDisplayConnector.subscriptionDisplay(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(someSubscriptionDisplayResponse))

      running(app) {

        val request = fakeRequestWithCsrf(POST, routes.WhatIsYourEmailController.submit.url)
          .withFormUrlEncodedBody("email" -> "invalidEmail")

        val result = route(app, request).value
        status(result) shouldBe BAD_REQUEST
      }
    }

    "have a status of SEE_OTHER when the email is valid" in new Setup  {
      when(mockSubscriptionDisplayConnector.subscriptionDisplay(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(someSubscriptionDisplayResponse))

      when(mockSave4LaterService.saveEmail(any, meq(EmailDetails(Some("test@test.com"), "valid@email.com", None)))(any))
        .thenReturn(Future.successful(Right(())))

      running(app) {

        val request = FakeRequest(POST, routes.WhatIsYourEmailController.submit.url)
          .withFormUrlEncodedBody("email" -> "valid@email.com")

        val result = route(app, request).value
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe routes.CheckYourEmailController.show.url
      }
    }

    "show 'there is a problem with the service' page when subscription display is failed for submit" in new Setup  {
      when(mockSubscriptionDisplayConnector.subscriptionDisplay(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new HttpException("Failed", BAD_REQUEST)))

      running(app) {

        val request = FakeRequest(POST, routes.WhatIsYourEmailController.submit.url)
          .withFormUrlEncodedBody("email" -> "")

        val result = route(app, request).value
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe routes.WhatIsYourEmailController.problemWithService.url
      }
    }

    "show 'there is a problem with the service' page when subscription display response has paramValue 'FAIL' with no email for submit" in new Setup  {
      when(mockSubscriptionDisplayConnector.subscriptionDisplay(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(someSubscriptionDisplayResponseWithStatus))

      running(app) {

        val request = FakeRequest(POST, routes.WhatIsYourEmailController.submit.url)
          .withFormUrlEncodedBody("email" -> "valid@email.com")

        val result = route(app, request).value
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe routes.WhatIsYourEmailController.problemWithService.url
      }
    }

    "show 'there is a problem with the service' page when subscription display response has paramValue 'FAIL' with no email for bad request form" in new Setup  {
      when(mockSubscriptionDisplayConnector.subscriptionDisplay(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(someSubscriptionDisplayResponseWithStatus))

      running(app) {

        val request = FakeRequest(POST, routes.WhatIsYourEmailController.submit.url)
          .withFormUrlEncodedBody("email" -> "invalidEmail")

        val result = route(app, request).value
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe routes.WhatIsYourEmailController.problemWithService.url
      }
    }

    "have a status of BAD_REQUEST for verifySubmit method when no email is provided in the form" in new Setup {
      running(app) {

        val request = fakeRequestWithCsrf(POST, routes.WhatIsYourEmailController.verifySubmit.url)

        val result = route(app, request).value
        status(result) shouldBe BAD_REQUEST
      }
    }

    "have a status of SEE_OTHER for verifyEmail method when the email is valid" in new Setup  {
      when(mockSave4LaterService.saveEmail(any, any)(any))
        .thenReturn(Future.successful(Right(())))

      running(app) {

        val request = FakeRequest(POST, routes.WhatIsYourEmailController.verifySubmit.url)
          .withFormUrlEncodedBody("email" -> "valid@email.com")

        val result = route(app, request).value
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe routes.CheckYourEmailController.show.url
      }
    }

    "redirect to 'there is a problem with the service' page" in new Setup  {

      running(app) {
        val errorHandler = app.injector.instanceOf[ErrorHandler]
        val request = FakeRequest(GET, routes.WhatIsYourEmailController.problemWithService.url).withFormUrlEncodedBody("email" -> "")

        val result = route(app, request).value
        status(result) shouldBe BAD_REQUEST
        contentAsString(result) shouldBe errorHandler.problemWithService()(request).toString()
      }
    }
  }
}
