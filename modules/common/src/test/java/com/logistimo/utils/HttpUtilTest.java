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

package com.logistimo.utils;

import com.logistimo.constants.Constants;

import junit.framework.TestCase;

import org.junit.Test;

import java.util.Date;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by vani on 23/10/17.
 */
public class HttpUtilTest extends TestCase {
  private static final String MODIFIED_SINCE_DATE = "Tue, 23 Aug 2017 07:28:00";
  private static final String TIMEZONE = "Asia/Kolkata";

  @Test
  public void testGetModifiedDateWithValidDateNoTimezone() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader(Constants.IF_MODIFIED_SINCE_HEADER)).thenReturn(MODIFIED_SINCE_DATE);
    HttpUtil httpUtil = new HttpUtil();
    assertNotSame(httpUtil.getModifiedDate(request, null), Optional.<Date>empty());
  }

  @Test
  public void testGetModifiedDateWithValidDateValidTimezone() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader(Constants.IF_MODIFIED_SINCE_HEADER)).thenReturn(MODIFIED_SINCE_DATE);
    HttpUtil httpUtil = new HttpUtil();
    assertNotSame(httpUtil.getModifiedDate(request, TIMEZONE), Optional.<Date>empty());
  }

  @Test
  public void testGetModifiedDateWithEmptyDateNoTimezone() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader(Constants.IF_MODIFIED_SINCE_HEADER)).thenReturn("");
    HttpUtil httpUtil = new HttpUtil();
    assertSame(httpUtil.getModifiedDate(request, null), Optional.<Date>empty());
  }

  @Test
  public void testGetModifiedDateWithUnparsableDateNoTimezone() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader(Constants.IF_MODIFIED_SINCE_HEADER)).thenReturn("56843754");
    HttpUtil httpUtil = new HttpUtil();
    assertSame(httpUtil.getModifiedDate(request, null), Optional.<Date>empty());
  }

  @Test
  public void testGetModifiedDateWithUnparsableDateValidTimezone() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader(Constants.IF_MODIFIED_SINCE_HEADER)).thenReturn("56843754");
    HttpUtil httpUtil = new HttpUtil();
    assertSame(httpUtil.getModifiedDate(request, TIMEZONE), Optional.<Date>empty());
  }

  @Test
  public void testSetLastModifiedDate() {
    HttpServletResponse response = mock(HttpServletResponse.class);
    HttpUtil httpUtil = new HttpUtil();
    httpUtil.setLastModifiedHeader(response, MODIFIED_SINCE_DATE);
    verify(response).addHeader(Constants.LAST_MODIFIED_HEADER, MODIFIED_SINCE_DATE);
  }
}