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

package forms.matching

import assets.MessageLookup
import forms.matching.UserDetailsForm._
import forms.submapping.DateMapping._
import forms.validation.ErrorMessageFactory
import forms.validation.testutils._
import models.DateModel
import models.matching.UserDetailsModel
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.Messages.Implicits._
import utils.TestConstants

class UserDetailsFormSpec extends PlaySpec with GuiceOneAppPerSuite {

  val testFirstName = "Test first name"
  val testLastName = "Test last name"
  val testNino = TestConstants.testNino
  val dob = DateModel("01", "02", "1980")

  def setupTestData(fname: String = testFirstName,
                    lname: String = testLastName,
                    NINO: String = testNino,
                    dob: DateModel = dob
                   ): Map[String, String] = {
    Map(
      dateOfBirth * dateDay -> dob.day,
      dateOfBirth * dateMonth -> dob.month,
      dateOfBirth * dateYear -> dob.year,
      firstName -> fname,
      nino -> NINO,
      lastName -> lname
    )
  }

  "The userDetailsForm" should {

    "For valid data should transform the data to the case class" in {
      val testInput = setupTestData()
      val expected = UserDetailsModel(testFirstName, testLastName, testNino, dob)
      val actual = userDetailsForm.bind(testInput).value
      actual mustBe Some(expected)
    }

    "when testing the validation for the data" should {

      "when testing the first name" should {

        "error if no name is supplied" in {
          val errors = ErrorMessageFactory.error("error.user_details.first_name.empty")
          errors fieldErrorIs MessageLookup.Error.UserDetails.firstNameEmpty
          errors summaryErrorIs MessageLookup.Error.UserDetails.firstNameEmpty
          val testInput = setupTestData(fname = "")
          userDetailsForm.bind(testInput) assert firstName hasExpectedErrors errors
        }

        "error if an invalid name is supplied" in {
          val errors = ErrorMessageFactory.error("error.user_details.first_name.invalid")
          errors fieldErrorIs MessageLookup.Error.UserDetails.firstNameInvalid
          errors summaryErrorIs MessageLookup.Error.UserDetails.firstNameInvalid
          val testInput = setupTestData(fname = "␢")
          userDetailsForm.bind(testInput) assert firstName hasExpectedErrors errors
        }

        "error if a name which is too long is supplied" in {
          val errors = ErrorMessageFactory.error("error.user_details.first_name.maxLength")
          errors fieldErrorIs MessageLookup.Error.UserDetails.firstNameMaxLength
          errors summaryErrorIs MessageLookup.Error.UserDetails.firstNameMaxLength
          val testInput = setupTestData(fname = "abc" * 100)
          userDetailsForm.bind(testInput) assert firstName hasExpectedErrors errors
        }

      }

      "when testing the last name" should {

        "Error if no last name is supplied" in {
          val errors = ErrorMessageFactory.error("error.user_details.last_name.empty")
          errors fieldErrorIs MessageLookup.Error.UserDetails.lastNameEmpty
          errors summaryErrorIs MessageLookup.Error.UserDetails.lastNameEmpty
          val testInput = setupTestData(lname = "")
          userDetailsForm.bind(testInput) assert lastName hasExpectedErrors errors
        }

        "Error if an invalid last name is supplied" in {
          val errors = ErrorMessageFactory.error("error.user_details.last_name.invalid")
          errors fieldErrorIs MessageLookup.Error.UserDetails.lastNameInvalid
          errors summaryErrorIs MessageLookup.Error.UserDetails.lastNameInvalid
          val testInput = setupTestData(lname = "␢")
          userDetailsForm.bind(testInput) assert lastName hasExpectedErrors errors
        }

        "error if a name which is too long is supplied" in {
          val errors = ErrorMessageFactory.error("error.user_details.last_name.maxLength")
          errors fieldErrorIs MessageLookup.Error.UserDetails.lastNameMaxLength
          errors summaryErrorIs MessageLookup.Error.UserDetails.lastNameMaxLength
          val testInput = setupTestData(lname = "abc" * 100)
          userDetailsForm.bind(testInput) assert lastName hasExpectedErrors errors
        }

      }

      "when testing the NINO" should {

        "error if no NINO is supplied" in {
          val errors = ErrorMessageFactory.error("error.nino.empty")
          errors fieldErrorIs MessageLookup.Error.Nino.empty
          errors summaryErrorIs MessageLookup.Error.Nino.empty
          val testInput = setupTestData(NINO = "")
          userDetailsForm.bind(testInput) assert nino hasExpectedErrors errors
        }

        "error if an invalid NINO is supplied" in {
          val errors = ErrorMessageFactory.error("error.nino.invalid")
          errors fieldErrorIs MessageLookup.Error.Nino.invalid
          errors summaryErrorIs MessageLookup.Error.Nino.invalid
          val testInput = setupTestData(NINO = "3456677")
          userDetailsForm.bind(testInput) assert nino hasExpectedErrors errors
        }

      }

      "when testing the DoB" should {

        "error if no DoB is supplied" in {
          val errors = ErrorMessageFactory.error("error.date.empty")
          errors fieldErrorIs MessageLookup.Error.Date.empty
          errors summaryErrorIs MessageLookup.Error.Date.empty
          val testInput = setupTestData(dob = DateModel("", "", ""))
          userDetailsForm.bind(testInput) assert dateOfBirth hasExpectedErrors errors
        }

        "error if an invalid day is supplied" in {
          val errors = ErrorMessageFactory.error("error.date.invalid")
          errors fieldErrorIs MessageLookup.Error.Date.invalid
          errors summaryErrorIs MessageLookup.Error.Date.invalid
          val testInput = setupTestData(dob = DateModel("56", "10", "1990"))
          userDetailsForm.bind(testInput) assert dateOfBirth hasExpectedErrors errors
        }

        "error if an invalid month is supplied" in {
          val errors = ErrorMessageFactory.error("error.date.invalid")
          errors fieldErrorIs MessageLookup.Error.Date.invalid
          errors summaryErrorIs MessageLookup.Error.Date.invalid
          val testInput = setupTestData(dob = DateModel("01", "15", "1990"))
          userDetailsForm.bind(testInput) assert dateOfBirth hasExpectedErrors errors
        }

        "error if an invalid year is supplied" in {
          val errors = ErrorMessageFactory.error("error.date.invalid")
          errors fieldErrorIs MessageLookup.Error.Date.invalid
          errors summaryErrorIs MessageLookup.Error.Date.invalid
          val testInput = setupTestData(dob = DateModel("01", "15", "1234567899"))
          userDetailsForm.bind(testInput) assert dateOfBirth hasExpectedErrors errors
        }

      }
    }
  }
}
