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

package uk.gov.hmrc.customs.emailfrontend.services

import org.mockito.ArgumentMatchers
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status.BAD_REQUEST
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.customs.emailfrontend.connectors.EmailVerificationConnector
import uk.gov.hmrc.customs.emailfrontend.connectors.httpparsers.EmailVerificationRequestHttpParser.{EmailAlreadyVerified, EmailVerificationRequestFailure, EmailVerificationRequestResponse, EmailVerificationRequestSent, EmailVerificationRequestSuccess}
import uk.gov.hmrc.customs.emailfrontend.connectors.httpparsers.EmailVerificationStateHttpParser.{EmailNotVerified, EmailVerificationStateErrorResponse, EmailVerificationStateResponse, EmailVerified}
import uk.gov.hmrc.customs.emailfrontend.model.EmailDetails
import uk.gov.hmrc.customs.emailfrontend.utils.SpecBase
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EmailVerificationServiceSpec extends SpecBase with BeforeAndAfterEach {
  private val mockConnector = mock[EmailVerificationConnector]

  implicit val hc: HeaderCarrier = mock[HeaderCarrier]
  implicit val rq: Request[AnyContent] = mock[Request[AnyContent]]

  val service = new EmailVerificationService(mockConnector)

  private val email = "test@test.com"
  private val emailDetails = EmailDetails(None, "test@test.com", None)
  private val continueUrl = "/customs/test-continue-url"
  private val eoriNumber = "EORINumber"

  override protected def beforeEach(): Unit =
    reset(mockConnector)

  def mockGetEmailVerificationState(emailAddress: String)(
      response: Future[EmailVerificationStateResponse]): Unit =
    when(
      mockConnector.getEmailVerificationState(
        ArgumentMatchers.eq(emailAddress))(ArgumentMatchers.any[HeaderCarrier])
    ) thenReturn response

  def mockCreateEmailVerificationRequest(details: EmailDetails,
                                         continueUrl: String,
                                         eoriNumber: String)(
      response: Future[EmailVerificationRequestResponse]
  ): Unit =
    when(
      mockConnector.createEmailVerificationRequest(
        ArgumentMatchers.eq(details),
        ArgumentMatchers.eq(continueUrl),
        ArgumentMatchers.eq(eoriNumber)
      )(ArgumentMatchers.any[HeaderCarrier])
    ) thenReturn response

  "Checking email verification status" when {

    "the email is verified" should {

      "return Some(true)" in {

        mockGetEmailVerificationState(email)(
          Future.successful(Right(EmailVerified)))
        val res: Option[Boolean] = {
          service.isEmailVerified(email).futureValue
        }
        res shouldBe Some(true)
      }

      "the email is not verified" should {

        "return Some(false)" in {

          mockGetEmailVerificationState(email)(
            Future.successful(Right(EmailNotVerified)))
          val res: Option[Boolean] = {
            service.isEmailVerified(email).futureValue
          }
          res shouldBe Some(false)
        }
      }

      "the email is check failed" should {

        "return None" in {

          mockGetEmailVerificationState(email)(
            Future.successful(
              Left(EmailVerificationStateErrorResponse(BAD_REQUEST, "")))
          )
          val res: Option[Boolean] = {
            service.isEmailVerified(email).futureValue
          }
          res shouldBe None
        }
      }
    }
  }

  "Creating an email verification request" when {

    "the email verification request is sent successfully" should {

      "return Some(EmailVerificationRequestSent)" in {
        mockCreateEmailVerificationRequest(emailDetails,
                                           continueUrl,
                                           eoriNumber)(
          Future.successful(Right(EmailVerificationRequestSent))
        )
        val res: Option[EmailVerificationRequestSuccess] = {
          service
            .createEmailVerificationRequest(emailDetails,
                                            continueUrl,
                                            eoriNumber)
            .futureValue
        }
        res shouldBe Some(EmailVerificationRequestSent)
      }
    }

    "the email address has already been verified" should {

      "return Some(EmailAlreadyVerified)" in {

        mockCreateEmailVerificationRequest(emailDetails,
                                           continueUrl,
                                           eoriNumber)(
          Future.successful(Right(EmailAlreadyVerified))
        )
        val res: Option[EmailVerificationRequestSuccess] = {
          service
            .createEmailVerificationRequest(emailDetails,
                                            continueUrl,
                                            eoriNumber)
            .futureValue
        }
        res shouldBe Some(EmailAlreadyVerified)
      }
    }

    "the email address verification request failed" should {

      "return None" in {
        mockCreateEmailVerificationRequest(emailDetails,
                                           continueUrl,
                                           eoriNumber)(
          Future.successful(Left(EmailVerificationRequestFailure(BAD_REQUEST, "")))
        )
        val res: Option[EmailVerificationRequestSuccess] = {
          service
            .createEmailVerificationRequest(emailDetails,
                                            continueUrl,
                                            eoriNumber)
            .futureValue
        }
        res shouldBe None
      }
    }
  }
}
