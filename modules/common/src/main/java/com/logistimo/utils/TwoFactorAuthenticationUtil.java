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

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;

import com.logistimo.constants.CharacterConstants;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * @author Smriti
 */
public class TwoFactorAuthenticationUtil {

  public static String generateUserDeviceCacheKey(String userId, long lastLoginTime)
      throws UnsupportedEncodingException, NoSuchAlgorithmException {
    String key = generateAuthKey(userId) + lastLoginTime + (new Random().longs(100000,999999));
    return Hashing.murmur3_128().hashString(key, Charsets.UTF_16LE).toString();
  }

  public static String generateAuthKey(String userId)
      throws UnsupportedEncodingException, NoSuchAlgorithmException {
    return CharacterConstants.UNDERSCORE + CommonUtils.AUTHENTICATION_KEY + CommonUtils
        .getMD5(userId);
  }

  public static String generateUserDeviceKey(String userId, String deviceKey) {
    return userId.concat(CharacterConstants.UNDERSCORE).concat(deviceKey);
  }
}
