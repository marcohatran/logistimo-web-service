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

package com.logistimo.auth;

import com.logistimo.auth.service.AuthenticationService;
import com.logistimo.auth.service.impl.AuthenticationServiceImpl;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.constants.CharacterConstants;
import com.logistimo.context.StaticApplicationContext;
import com.logistimo.exception.UnauthorizedException;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.ServiceException;
import com.logistimo.users.service.UsersService;
import com.logistimo.users.service.impl.UsersServiceImpl;
import com.logistimo.utils.PatternConstants;

import org.springframework.util.StringUtils;

/**
 * Created by charan on 22/06/17.
 */
public class GenericAuthoriser {

  public static final Integer NO_ACCESS = 0;
  public static final Integer MANAGE_MASTER_DATA = 2;

  private GenericAuthoriser() {
  }

  public static boolean authoriseUser(String userId)
      throws ServiceException {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    String rUserId = sUser.getUsername();
    String role = sUser.getRole();
    Long domainId = SecurityUtils.getDomainId();
    UsersService as = StaticApplicationContext.getBean(UsersServiceImpl.class);
    return role.equals(SecurityConstants.ROLE_SUPERUSER) || as
        .hasAccessToUser(userId, rUserId, domainId, role);
  }

  public static boolean authoriseAdmin() {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    String role = sUser.getRole();
    return SecurityUtil.compareRoles(role, SecurityConstants.ROLE_DOMAINOWNER) >= 0;
  }

  public static boolean authoriseSMS(String mobileNumber, String userMobileNumber, String userId,
                                     String tokenSuffix) throws ServiceException {
    boolean isAuthorised;

    //If token is present validate token else validate mobile number
    AuthenticationService as = StaticApplicationContext.getBean(AuthenticationServiceImpl.class);
    String token = as.getUserToken(userId);
    if( StringUtils.isEmpty(token) || StringUtils.isEmpty(tokenSuffix)
        || !token.endsWith(tokenSuffix)) {
      throw new UnauthorizedException("Invalid token");
    }

    String tmpUserMobileNumber = userMobileNumber.replaceAll(PatternConstants.PLUS_AND_SPACES,
        CharacterConstants.EMPTY);
    isAuthorised = tmpUserMobileNumber.equals(
            mobileNumber.replaceAll(PatternConstants.PLUS_AND_SPACES, CharacterConstants.EMPTY));

    return isAuthorised;
  }

}
