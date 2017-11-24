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

import com.logistimo.api.auth.AuthenticationUtil;
import com.logistimo.auth.SecurityMgr;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.constants.Constants;
import com.logistimo.exception.UnauthorizedException;
import com.logistimo.logger.XLog;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.ObjectNotFoundException;
import com.logistimo.users.entity.IUserAccount;

import org.apache.commons.lang.StringUtils;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by Mohan Raja on 02/04/15
 */
public class MediaSecurityFilter implements Filter {


  public static final String MEDIA_ENDPOINT_URL = "/_ah/api/mediaendpoint";

  private static final XLog xLogger = XLog.getLog(APISecurityFilter.class);
  private static final String X_ACCESS_USER = "x-access-user";

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
      throws IOException, ServletException {

    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse resp = (HttpServletResponse) response;
    String servletPath = req.getServletPath() + req.getPathInfo();
    xLogger.fine("Servlet path: ", servletPath);
    if (req.getCharacterEncoding() == null) {
      request.setCharacterEncoding(Constants.UTF8);
    }

    if (!(StringUtils.isNotBlank(servletPath) && servletPath.startsWith(MEDIA_ENDPOINT_URL))) {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    } else if (StringUtils.isNotBlank(req.getHeader(X_ACCESS_USER))) {
      try {
        SecurityMgr.setSessionDetails(req.getHeader(X_ACCESS_USER));
      } catch (UnauthorizedException | ObjectNotFoundException e) {
        xLogger.warn("Issue with api client authentication", e);
        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
        return;
      } catch (Exception e) {
        xLogger.severe("Issue with api client authentication", e);
        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        return;
      }
    } else if (StringUtils.isNotBlank(req.getHeader(Constants.TOKEN))) {
      try {
        IUserAccount user = AuthenticationUtil.authenticateToken(req.getHeader(Constants.TOKEN), -1);
        SecurityMgr.setSessionDetails(user.getUserId());
      } catch (UnauthorizedException | ObjectNotFoundException e) {
        xLogger.warn("Issue with api client authentication", e);
        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
        return;
      } catch (Exception e) {
          xLogger.severe("Issue with api client authentication", e);
          resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
          return;
      }
    }
    try {
      SecureUserDetails
          userDetails = SecurityMgr
          .getSessionDetails(req.getSession());
      if (userDetails == null) {
        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication Required.");
        return;
      }
      SecurityUtils.setUserDetails(userDetails);
      if (filterChain != null) {
        filterChain.doFilter(request, response);
      }
    } finally {
      SecurityUtils.setUserDetails(null);
    }
  }


  @Override
  public void init(FilterConfig filterConfig) throws ServletException {

  }

  @Override
  public void destroy() {

  }
}
