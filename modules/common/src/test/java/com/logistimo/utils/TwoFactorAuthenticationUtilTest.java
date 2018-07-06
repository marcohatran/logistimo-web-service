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

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author smriti
 */
public class TwoFactorAuthenticationUtilTest {
  HttpServletRequest request = mock(HttpServletRequest.class);
  static final String testUserId = "testUser";

  @Test
  public void testGenerateUserDeviceKey() throws Exception {
    String deviceKey = TwoFactorAuthenticationUtil.generateUserDeviceCacheKey(testUserId, 1529041742l);
    assertEquals(true, StringUtils.isNotBlank(deviceKey));
  }

  @Test
  public void testGenerateAuthKey() throws Exception {
    String authKey = TwoFactorAuthenticationUtil.generateAuthKey(testUserId);
    assertEquals(true, StringUtils.isNotBlank(authKey));
    assertNotNull(authKey);
    assertEquals(true, authKey.startsWith("_di"));
  }

  @Test
  public void testGetCookieByName() throws UnsupportedEncodingException, NoSuchAlgorithmException {

    Cookie[] cookies = new Cookie[1];
    String key = TwoFactorAuthenticationUtil.generateAuthKey(testUserId);
    cookies[0] = new Cookie(key, "01dc28819svbd82991bdn");
    when(request.getCookies()).thenReturn(cookies);
    String cookieValue = CommonUtils.getCookieByName(request, key);
    assertNotNull(cookieValue);
    assertEquals("01dc28819svbd82991bdn", cookieValue);
  }

  @Test
  public void testGetCookieByNameForNullValues()
      throws UnsupportedEncodingException, NoSuchAlgorithmException {
    String key = TwoFactorAuthenticationUtil.generateAuthKey(testUserId);
    when(request.getCookies()).thenReturn(null);
    String cookieValue = CommonUtils.getCookieByName(request, key);
    assertNull(cookieValue);
  }

}