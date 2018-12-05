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

package com.logistimo.auth.provider;

import com.logistimo.auth.entity.UserAuth;
import com.logistimo.auth.repository.UserAuthRepository;
import com.logistimo.auth.service.AuthProvider;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.constants.Constants;
import com.logistimo.exception.UnauthorizedException;
import com.logistimo.exception.ValidationException;
import com.logistimo.logger.XLog;
import com.logistimo.models.users.UserDetails;
import com.logistimo.security.BadCredentialsException;
import com.logistimo.security.UserDisabledException;
import com.logistimo.services.ServiceException;
import com.logistimo.utils.PasswordEncoder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

import javax.transaction.Transactional;

/**
 * Created by kumargaurav on 13/08/18.
 */
@Component
public class DBAuthProvider implements AuthProvider {

  private static final XLog xLogger = XLog.getLog(DBAuthProvider.class);

  @Autowired
  public UserAuthRepository authRepository;

  @Override
  @Transactional
  public UserDetails authenticate(String userId, String password) {

    xLogger.fine("Entering authenticate");
    UserAuth user = authRepository.findOne(userId);
    if (user == null) {
      throw new BadCredentialsException("G008");
    }
    boolean isAuthenticated;
    if(user.hasSalt()){
      isAuthenticated = matchesOldOrNewPassword(password, user);
    } else {
      isAuthenticated = matchesOldPassword(password, user);
      if (isAuthenticated) {
        createNewPassword(password, user);
      }
    }
    if(isAuthenticated) {
      if(user.isEnabled()) {
        return mapUserAuthToUserDetails(user);
      } else {
        xLogger.warn("Authentication failed! User {0} is disabled", userId);
        throw new UserDisabledException("User is disabled");
      }
    } else {
      throw new BadCredentialsException("G008");
    }
  }

  private void createNewPassword(String password, UserAuth user) {
    String salt = SecurityUtils.salt();
    String encNewPwd = PasswordEncoder.bcrypt(PasswordEncoder.sha512(password, salt));
    user.setSalt(salt);
    user.setPassword(encNewPwd);
    authRepository.save(user);
  }

  private boolean matchesOldOrNewPassword(String password, UserAuth user) {
    return matchesNewPassword(password, user) || matchesOldPassword(password, user);
  }

  private boolean matchesNewPassword(String password, UserAuth user) {
    return PasswordEncoder.bcryptMatches(password, user.getPassword());
  }

  private boolean matchesOldPassword(String password, UserAuth userAuth) {
    return PasswordEncoder.MD5(password).equals(userAuth.getEncodedPassword());
  }


  private UserDetails mapUserAuthToUserDetails(UserAuth user) {
    UserDetails userDetails = new UserDetails();
    userDetails.setUserId(user.getUserId());
    userDetails.setEnabled(user.isEnabled());
    userDetails.setFirstName(user.getFirstName());
    userDetails.setLastName(user.getLastName());
    userDetails.setSalt(user.getSalt());
    userDetails.setEncodedPassword(user.getEncodedPassword());
    userDetails.setPassword(user.getPassword());
    userDetails.setPrevUsrAgnt(user.getUserAgent());
    userDetails.setRole(user.getRole());
    userDetails.setLastLogin(new Date());
    return userDetails;
  }

  @Override
  @Transactional
  public void changePassword(String userId, String oldPassword, String newPassword, boolean isEnhanced) {

    if (newPassword == null || newPassword.isEmpty()) {
      throw new ValidationException("G007",userId);
    }
    if (null != oldPassword) {
      try {
        authenticate(userId, oldPassword);
      } catch (Exception e) {
        xLogger.warn("Change password: authentication failed for user {0}", userId);
        throw new UnauthorizedException("G002", userId);
      }
    }
    //getting db instance
    UserAuth userAuth = authRepository.findOne(userId);
    if(isEnhanced){
      //this route is for new client with salt
      userAuth.setPassword(PasswordEncoder.bcrypt(newPassword));
      userAuth.setEncodedPassword(null);
      xLogger.info("Enhanced password flow: change password for user {0}",userId);
    } else {
      String salt = SecurityUtils.salt();
      String encNewPwd = PasswordEncoder.bcrypt(PasswordEncoder.sha512(newPassword,salt));
      userAuth.setSalt(salt);
      userAuth.setPassword(encNewPwd);
      userAuth.setEncodedPassword(PasswordEncoder.MD5(newPassword));
      xLogger.info("Old password flow: change password for user {0}",userId);
    }
    authRepository.save(userAuth);
  }

  @Override
  @Transactional
  public void enableAccount(String userId) throws ServiceException {
    UserAuth userAuth = authRepository.findOne(userId);
    if (userAuth == null) {
      xLogger.warn("Invalid user {0}",userId);
      throw new ServiceException("Invalid user");
    }
    userAuth.setEnabled(true);
    authRepository.save(userAuth);
  }

  @Override
  @Transactional
  public void disableAccount(String userId) throws ServiceException {
    UserAuth userAuth = authRepository.findOne(userId);
    if (userAuth == null) {
      xLogger.warn("Invalid user {0}",userId);
      throw new ServiceException("Invalid user");
    }
    userAuth.setEnabled(false);
    authRepository.save(userAuth);
  }

  @Override
  public String getUserSalt(String userId) {
    UserAuth userAuth =  authRepository.findOne(userId);
    if(userAuth == null) {
      return Constants.EMPTY;
    }
    return userAuth.getSalt();
  }
}
