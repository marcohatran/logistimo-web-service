/*
 * Copyright © 2017 Logistimo.
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

package com.logistimo.api.util;

import org.apache.commons.lang.xwork.StringUtils;

import java.io.UnsupportedEncodingException;

/**
 * Created by chandrakant on 29/09/17.
 * Util class to encode and decode numeric values from decimal (base-10) to base-62.
 * For encoding, a subset of characters in UTF-8 are being used (0-9, A-Z and a-z) in the respective
 * order.
 */
public class SMSDecodeUtil {

  private static final int ASCII_START_INDEX_SET1 = 48;
  private static final int ASCII_END_INDEX_SET1 = 58; // exclusive
  private static final int SIZE_ASCII_SET1 = ASCII_END_INDEX_SET1 - ASCII_START_INDEX_SET1;
  private static final int ASCII_START_INDEX_SET2 = 65;
  private static final int ASCII_END_INDEX_SET2 = 91; // exclusive
  private static final int SIZE_ASCII_SET2 = ASCII_END_INDEX_SET2 - ASCII_START_INDEX_SET2;
  private static final int ASCII_START_INDEX_SET3 = 97;
  private static final int ASCII_END_INDEX_SET3 = 123; // exclusive
  private static final int SIZE_ASCII_SET3 = ASCII_END_INDEX_SET3 - ASCII_START_INDEX_SET3;

  private static final int TOTAL_CHARS =
      SIZE_ASCII_SET1 + SIZE_ASCII_SET2 + SIZE_ASCII_SET3;

  private SMSDecodeUtil() {
  }

  private static int getIndex(char c) {
    int i = (int) c;
    if (i >= ASCII_START_INDEX_SET1 && i < ASCII_END_INDEX_SET1) {
      return i - ASCII_START_INDEX_SET1;
    } else if (i >= ASCII_START_INDEX_SET2 && i < ASCII_END_INDEX_SET2) {
      return i - ASCII_START_INDEX_SET1 - (ASCII_START_INDEX_SET2 - ASCII_END_INDEX_SET1);
    } else if (i >= ASCII_START_INDEX_SET3 && i < ASCII_END_INDEX_SET3) {
      return i - ASCII_START_INDEX_SET1 - (ASCII_START_INDEX_SET2 - ASCII_END_INDEX_SET1) - (
          ASCII_START_INDEX_SET3 - ASCII_END_INDEX_SET2);
    }
    return -1;
  }

  private static char getChar(int i) {
    if (i < (ASCII_END_INDEX_SET1 - ASCII_START_INDEX_SET1)) {
      return (char) (i + ASCII_START_INDEX_SET1);
    } else if (i >= SIZE_ASCII_SET1 && i < (SIZE_ASCII_SET2 + SIZE_ASCII_SET1)) {
      return (char) (i + ASCII_START_INDEX_SET1 + (ASCII_START_INDEX_SET2 - ASCII_END_INDEX_SET1));
    } else if (i >= (SIZE_ASCII_SET2 + SIZE_ASCII_SET1) && i < TOTAL_CHARS) {
      return (char) (i + ASCII_START_INDEX_SET1 + (ASCII_START_INDEX_SET2 - ASCII_END_INDEX_SET1)
          + (
          ASCII_START_INDEX_SET3 - ASCII_END_INDEX_SET2));
    } else {
      return '!';
    }
  }


  public static String encode(long number) {
    StringBuilder builder = new StringBuilder();
    while (number != 0) {
      int rem = (int) (number % TOTAL_CHARS);
      builder.insert(0, getChar(rem));
      number /= TOTAL_CHARS;
    }
    return builder.length() > 0 ? builder.toString() : "0";
  }

  public static long decode(String str) throws UnsupportedEncodingException {
    long l = 0;
    if (StringUtils.isNotEmpty(str)) {
      str = str.trim();
      for (int i = 0; i < str.length(); i++) {
        int index = getIndex(str.charAt(str.length() - i - 1));
        if (index == -1) {
          throw new UnsupportedEncodingException(str);
        } else {
          l += index * Math.pow(TOTAL_CHARS, i);
        }
      }
    }
    return l;
  }

}
