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

package com.logistimo.utils;

import java.security.SecureRandom;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Created by pratheeka on 16/08/18.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RandomPasswordGenerator {

  private static final String UPPERCASE_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  private static final String LOWERCASE_CHARACTERS = "abcdefghijklmnopqrstuvwxyz";
  private static final String NUMERIC_CHARACTERS = "1234567890";
  private static final String SPECIAL_CHARACTERS = "!#$%&*@_";
  private static SecureRandom secureRandom = new SecureRandom();

  /**
   * Method creates a Random password with 3 lowercase characters,2 uppercase characters,
   * 1 special character and 2 numbers
   */
  public static String generate(boolean isAdmin) {
    StringBuilder passwordBuilder = new StringBuilder();
    appendCharacters(UPPERCASE_CHARACTERS, passwordBuilder, 1);
    appendCharacters(LOWERCASE_CHARACTERS, passwordBuilder, 4);
    appendCharacters(NUMERIC_CHARACTERS, passwordBuilder, 2);
    if (isAdmin) {
      appendCharacters(SPECIAL_CHARACTERS, passwordBuilder, 1);
    }
    return passwordBuilder.toString();
  }

  private static void appendCharacters(String characterSet,
                                       StringBuilder passwordBuilder,
                                       int numberOfTimes) {
    for (int i = 0; i < numberOfTimes; i++) {
      passwordBuilder.append(characterSet.charAt(secureRandom.nextInt(characterSet.length())));
    }
  }
}
