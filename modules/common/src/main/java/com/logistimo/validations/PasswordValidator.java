/*
 * Copyright © 2018 Logistimo.
 *
 * This file is part of Logistimo.
 *
 * Logistimo software is a mobile & web platform for supply chain management and remote temperature monitoring in
 * low-resource settings, made available under the terms of the GNU Affero General Public License (AGPL).
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * You can be released from the requirements of the license by purchasing a commercial license. To know more about
 * the commercial license, please contact us at opensource@logistimo.com
 */

package com.logistimo.validations;

import com.logistimo.auth.SecurityUtil;
import com.logistimo.exception.ValidationException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

/**
 * Created by pratheeka on 14/08/18.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PasswordValidator {

  private static final String PATTERN_UPPERCASE = "(?=.*[A-Z])";

  private static final String PATTERN_LOWERCASE = "(?=.*[a-z])";

  private static final String PATTERN_SPECIAL_CHARACTERS =
      "(?=.*[ !\"#$%&'()*+,-./:;<=>?@[\\\\]^_`{|}~])";

  private static final String PATTERN_NUMBERS = "(?=.*\\d)";

  private static final String PATTERN_CHARACTERS = "(?=.*[A-Za-z])";

  /**
   * Validate the password based on the user role
   *
   * @param username
   * @param role
   * @param newPassword
   */
  public static void validate(String username, String role, String newPassword) {
    if (newPassword.equalsIgnoreCase(username)) {
      throw new ValidationException("G012",(Object[]) null);
    }
    PatternBuilderParams patternBuilderParams;
    boolean isAdmin=SecurityUtil.isUserAdmin(role);
    if (isAdmin) {
      patternBuilderParams = PatternBuilderParams.builder()
          .checkLowercaseCharacter(true)
          .checkUppercaseCharacter(true)
          .checkNumber(true)
          .checkSpecialCharacter(true)
          .minLength(8)
          .build();
    } else {
      patternBuilderParams = PatternBuilderParams.builder()
          .checkNumber(true)
          .checkCharacter(true)
          .minLength(7)
          .build();
    }
    Pattern pattern = Pattern.compile(buildPattern(patternBuilderParams));
    Matcher m = pattern.matcher(newPassword);
    if (!m.matches()) {
      String errorCode = isAdmin ? "G013" : "G014";
      throw new ValidationException(errorCode, (Object[]) null);
    }

  }

  /**
   * Based on the params, build pattern
   * @param params
   * @return
   */
  private static String buildPattern(PatternBuilderParams params) {
    StringBuilder patternBuilder = new StringBuilder("(");
    if (params.checkUppercaseCharacter) {
      patternBuilder.append(PATTERN_UPPERCASE);
    }
    if ((params.checkLowercaseCharacter)) {
      patternBuilder.append(PATTERN_LOWERCASE);
    }
    if ((params.checkSpecialCharacter)) {
      patternBuilder.append(PATTERN_SPECIAL_CHARACTERS);
    }
    if (params.checkNumber) {
      patternBuilder.append(PATTERN_NUMBERS);
    }
    if ((params.checkCharacter)) {
      patternBuilder.append(PATTERN_CHARACTERS);
    }
    patternBuilder.append(".{").append(params.minLength).append(",})");
    return patternBuilder.toString();
  }
}

@Builder
class PatternBuilderParams {
  boolean checkSpecialCharacter;
  boolean checkUppercaseCharacter;
  boolean checkLowercaseCharacter;
  boolean checkCharacter;
  boolean checkNumber;
  int minLength;
}