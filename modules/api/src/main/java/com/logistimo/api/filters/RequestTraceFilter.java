/*
 * Copyright © 2019 Logistimo.
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

import com.logistimo.utils.ThreadLocalUtil;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

public class RequestTraceFilter implements Filter {

  public void destroy() {
    //nothing to destroy
  }

  public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
      throws ServletException, IOException {
    try {
      HttpServletRequest request = (HttpServletRequest) req;
      String requestId = request.getHeader("X-Request-Id");
      if(requestId == null) {
        requestId = generateRequestId();
      }
      ThreadLocalUtil.get().setTraceId(requestId);
      chain.doFilter(req, resp);
    } finally {
      ThreadLocalUtil.get().removeTraceId();
    }
  }

  private String generateRequestId() {
    return UUID.randomUUID().toString();
  }

  public void init(FilterConfig config) throws ServletException {
    //nothing to initialise
  }

}
