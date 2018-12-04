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

package com.logistimo.api.auth;

import com.logistimo.auth.SecurityMgr;
import com.logistimo.auth.service.AuthenticationService;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.constants.Constants;
import com.logistimo.constants.SourceConstants;
import com.logistimo.context.StaticApplicationContext;
import com.logistimo.exception.UnauthorizedException;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.ObjectNotFoundException;
import com.logistimo.services.ServiceException;
import com.logistimo.services.utils.ConfigUtil;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.users.entity.IUserToken;
import com.logistimo.users.service.UsersService;

import org.springframework.util.StringUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by charan on 09/03/17.
 */
public class AuthenticationUtil {

  public static IUserAccount authenticateToken(String authtoken, Integer actionInitiator)
      throws ServiceException, ObjectNotFoundException {
    AuthenticationService aus = StaticApplicationContext.getBean(AuthenticationService.class);
    IUserToken token = aus.authenticateToken(authtoken, actionInitiator);
    UsersService usersService = StaticApplicationContext.getBean(UsersService.class);
    return usersService.getUserAccount(token.getUserId());
  }

  public static void authenticateTokenAndSetSession(HttpServletRequest req,
                                                    HttpServletResponse response)
      throws ServiceException {
    String authToken = getToken(req);
    AuthenticationService aus = StaticApplicationContext.getBean(AuthenticationService.class);
    try {
      IUserToken token = aus.authenticateToken(authToken, getSource(req));

      UsersService usersService = StaticApplicationContext.getBean(UsersService.class);
      SecurityMgr.setSessionDetails(usersService.getUserAccount(token.getUserId()));
      SecureUserDetails userDetails = SecurityUtils.getUserDetails();
      if (!token.hasAccessKey()) {
        Long requestDomainId = getRequestDomain(req);
        if (requestDomainId != null && usersService
            .hasAccessToDomain(userDetails.getUsername(), requestDomainId)) {
          userDetails.setCurrentDomainId(requestDomainId);
        } else {
          userDetails.setCurrentDomainId(token.getDomainId());
        }
      } else {
        userDetails.setCurrentDomainId(token.getDomainId());
      }
      SecurityUtils.setUserDetails(userDetails);
    } catch (UnauthorizedException e) {
      SecurityUtils.clearTokenCookie(req, response);
      throw e;
    }
  }

  private static int getSource(HttpServletRequest req) {
    String sourceParam = req.getHeader(Constants.ACCESS_INITIATOR);
    return StringUtils.isEmpty(sourceParam) ? SourceConstants.WEB : Integer.parseInt(sourceParam);
  }

  public static boolean hasAccessToken(HttpServletRequest req) {
    return StringUtils.hasText(getToken(req));
  }

  private static String getToken(HttpServletRequest req) {
    return StringUtils.hasText(req.getHeader(Constants.TOKEN)) ? req.getHeader(Constants.TOKEN)
        : getTokenCookie(req);
  }

  private static String getTokenCookie(HttpServletRequest req) {
    Cookie[] cookies = req.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (Constants.TOKEN.equals(cookie.getName())) {
          return cookie.getValue();
        }
      }
    }
    return null;
  }

  private static Long getRequestDomain(HttpServletRequest req) {
    return StringUtils.hasText(req.getHeader(Constants.DOMAIN)) ? Long
        .valueOf(req.getHeader(Constants.DOMAIN))
        : getRequestDomainFromCookie(req);
  }

  private static Long getRequestDomainFromCookie(HttpServletRequest req) {
    Cookie[] cookies = req.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (Constants.DOMAIN.equals(cookie.getName())) {
          return Long.valueOf(cookie.getValue());
        }
      }
    }
    return null;
  }

  public static boolean isCaptchaEnabled() {
    return ConfigUtil.getBoolean("captcha.enable", true);
  }
}
