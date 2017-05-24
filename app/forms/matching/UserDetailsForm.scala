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

import forms.prevalidation.PreprocessedForm
import forms.submapping.DateMapping.dateMapping
import forms.validation.Constraints._
import forms.validation.utils.ConstraintUtil._
import forms.validation.utils.MappingUtil._
import models.matching.UserDetailsModel
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraint

object UserDetailsForm {

  val firstName = "firstName"
  val lastName = "lastName"
  val nino = "nino"
  val dateOfBirth = "dateOfBirth"

  val nameMaxLength = 105

  val firstNameNonEmpty: Constraint[String] = nonEmpty("error.user_details.first_name.empty")
  val lastNameNonEmpty: Constraint[String] = nonEmpty("error.user_details.last_name.empty")

  val firstNameInvalid: Constraint[String] = invalidFormat("error.user_details.first_name.invalid")
  val lastNameInvalid: Constraint[String] = invalidFormat("error.user_details.last_name.invalid")

  val firstNameMaxLength: Constraint[String] = maxLength(nameMaxLength, "error.user_details.first_name.maxLength")
  val lastNameMaxLength: Constraint[String] = maxLength(nameMaxLength, "error.user_details.last_name.maxLength")

  val userDetailsValidationForm = Form(
    mapping(
      firstName -> oText.toText.verifying(firstNameNonEmpty andThen firstNameMaxLength andThen firstNameInvalid),
      lastName -> oText.toText.verifying(lastNameNonEmpty andThen lastNameMaxLength andThen lastNameInvalid),
      nino -> oText.toText.verifying(emptyNino andThen validateNino),
      dateOfBirth -> dateMapping.verifying(dateEmpty andThen dateValidation)
    )(UserDetailsModel.apply)(UserDetailsModel.unapply)
  )

  import forms.prevalidation.CaseOption._
  import forms.prevalidation.TrimOption._

  val userDetailsForm = PreprocessedForm(
    validation = userDetailsValidationForm,
    trimRules = Map(nino -> bothAndCompress),
    caseRules = Map(nino -> upper)
  )

}
