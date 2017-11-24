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

package com.logistimo.api.filters;

import com.logistimo.constants.Constants;
import com.logistimo.security.SecureUserDetails;

import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by charan on 19/11/17.
 */
public class APISecurityFilterTest {

  @Test
  public void testDoFilter() throws Exception {
    APISecurityFilter securityFilter = new APISecurityFilter();
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getServletPath()).thenReturn("/s2");
    when(request.getPathInfo()).thenReturn("/api/auth");
    HttpServletResponse response = mock(HttpServletResponse.class);
    HttpSession session = mock(HttpSession.class);
    SecureUserDetails secureUserDetails = new SecureUserDetails();
    secureUserDetails.setUsername("charan");
    when(session.getAttribute(Constants.PARAM_USER)).thenReturn(secureUserDetails);
    when(request.getSession()).thenReturn(session);
    FilterChain filterChain = mock(FilterChain.class);
    Mockito.doThrow(new RuntimeException()).when(filterChain).doFilter(request, response);
    try {
      securityFilter.doFilter(request, response, filterChain);
      fail("Filter chain not invoked");
    } catch (RuntimeException re) {
      //Runtime exception should be thrown
    }

  }
}