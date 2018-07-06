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

package com.logistimo.api.controllers;

import com.logistimo.api.auth.AuthenticationUtil;
import com.logistimo.api.builders.UserBuilder;
import com.logistimo.api.builders.UserDevicesBuilder;
import com.logistimo.api.models.AuthLoginModel;
import com.logistimo.api.models.AuthModel;
import com.logistimo.api.models.ChangePasswordModel;
import com.logistimo.api.models.PasswordModel;
import com.logistimo.api.models.UserDetailModel;
import com.logistimo.api.models.mobile.ValidateOtpModel;
import com.logistimo.auth.SecurityConstants;
import com.logistimo.auth.service.AuthenticationService;
import com.logistimo.communications.MessageHandlingException;
import com.logistimo.config.models.ConfigurationException;
import com.logistimo.constants.Constants;
import com.logistimo.constants.SourceConstants;
import com.logistimo.exception.BadRequestException;
import com.logistimo.exception.UnauthorizedException;
import com.logistimo.security.BadCredentialsException;
import com.logistimo.security.UserDisabledException;
import com.logistimo.services.ObjectNotFoundException;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;
import com.logistimo.twofactorauthentication.service.TwoFactorAuthenticationService;
import com.logistimo.twofactorauthentication.vo.UserDevicesVO;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.users.entity.IUserToken;
import com.logistimo.users.service.UsersService;
import com.logistimo.utils.CommonUtils;
import com.logistimo.utils.TwoFactorAuthenticationUtil;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@Controller
@RequestMapping("/mauth")
public class AuthControllerMV1 {

  private static final String USER_AGENT = "User-Agent";
  private static final String DEVICE_DETAILS = "Device-Details";
  private static final String DEVICE_PROFILE = "Device-Profile";

  private UserBuilder userBuilder;
  private AuthenticationService authenticationService;
  private UsersService usersService;
  private TwoFactorAuthenticationService twoFactorAuthenticationService;
  private UserDevicesBuilder userDevicesBuilder;

  @Autowired
  public void setUserBuilder(UserBuilder userBuilder) {
    this.userBuilder = userBuilder;
  }

  @Autowired
  public void setAuthenticationService(AuthenticationService authenticationService) {
    this.authenticationService = authenticationService;
  }

  @Autowired
  public void setUsersService(UsersService usersService) {
    this.usersService = usersService;
  }

  @Autowired
  public void setTwoFactorAuthenticationService(TwoFactorAuthenticationService twoFactorAuthenticationService) {
   this.twoFactorAuthenticationService = twoFactorAuthenticationService;
  }

  @Autowired
  public void setUserDevicesBuilder(UserDevicesBuilder userDevicesBuilder) { this.userDevicesBuilder = userDevicesBuilder; }

  /**
   * This method is used for user's login
   *
   * @param loginModel userid and password
   * @param req        http request
   * @deprecated
   * @return UserDetail object on successful login
   */
  @Deprecated
  @RequestMapping(value = "/login", method = RequestMethod.POST)
  public
  @ResponseBody
  UserDetailModel login(@RequestBody AuthLoginModel loginModel,
                        HttpServletRequest req, HttpServletResponse res)
      throws Exception {
    loginModel.setSkipTwoFactorAuthentication(true);
    return processLoginRequest(loginModel, req, res);
  }

  /**
   * This method is used for user's login along with two factor authentication
   *
   * @param loginModel userid and password
   * @param req        http request
   * @return UserDetail object on successful login
   */
  @RequestMapping(value = "/login/v1", method = RequestMethod.POST)
  public
  @ResponseBody
  UserDetailModel login2FA(@RequestBody AuthLoginModel loginModel,
                        HttpServletRequest req, HttpServletResponse res)
      throws Exception {
    loginModel.setSkipTwoFactorAuthentication(false);
    return processLoginRequest(loginModel, req, res);
  }

  /**
   * This method will validate user's otp
   *
   * @param otpModel consists of user id, otp
   */
  @RequestMapping(value = "/validate-otp", method = RequestMethod.POST)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void validateOtp(@RequestBody ValidateOtpModel otpModel) {
    authenticationService.validateOtpMMode(otpModel.uid, otpModel.otp);
  }

  /**
   * This method will validate access token for an user.
   *
   * @return userId
   */
  @RequestMapping(value = "/validate-token", method = RequestMethod.POST)
  public
  @ResponseBody
  String validateToken(@RequestBody String token, HttpServletRequest req)
      throws ServiceException {
    String initiatorStr = req.getHeader(Constants.ACCESS_INITIATOR);
    int
        accessInitiator =
        StringUtils.isNotBlank(initiatorStr) ? Integer.parseInt(initiatorStr) : -1;
    return authenticationService.authenticateToken(token, accessInitiator).getUserId();
  }

  /**
   * This will reset user's password
   *
   * @param pwdModel consists of user id ,otp and new password
   */
  @RequestMapping(value = "/change-password", method = RequestMethod.POST)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void resetPassword(@RequestBody ChangePasswordModel pwdModel)
      throws ServiceException {
    authenticationService.validateOtpMMode(pwdModel.uid, pwdModel.otp);
    usersService.changePassword(pwdModel.uid, null, pwdModel.npd);
  }

  @RequestMapping(value = "generate-authentication-otp", method = RequestMethod.POST)
  public
  @ResponseBody
  AuthModel generate2FAOTP(@RequestBody PasswordModel model)
      throws BadCredentialsException, MessageHandlingException, IOException {
    if(StringUtils.isBlank(model.getUid())) {
      throw new BadCredentialsException("User ID is empty.");
    }
    if(StringUtils.isBlank(model.getUdty())) {
      model.setUdty(Constants.WEB);
    }
    String successMsg = authenticationService.generate2FAOTP(model.getUid());
    return new AuthModel(false, successMsg);
  }


  private UserDetailModel processLoginRequest(@RequestBody AuthLoginModel loginModel,
                                              HttpServletRequest req, HttpServletResponse res)
      throws ServiceException, BadCredentialsException, UserDisabledException,
      MessageHandlingException, IOException, ConfigurationException, NoSuchAlgorithmException {
    IUserAccount user;
    Map<String, String> headers = new HashMap<>();
    String authToken = req.getHeader(Constants.TOKEN);
    String initiator = req.getHeader(Constants.ACCESS_INITIATOR);
    Integer actionInitiator = StringUtils.isEmpty(initiator) ? -1 : Integer.parseInt(initiator);
    Integer loginSource = getAppName(actionInitiator, req);
    headers.put(Constants.REQ_ID, req.getHeader(Constants.REQ_ID));
    if (authToken != null) {
      user = AuthenticationUtil.authenticateToken(authToken, actionInitiator);
    } else {
      user = validateUser(loginModel, actionInitiator, loginSource);
      String mobileNumber = authenticateUser(loginModel, user, loginSource, req);

      if(StringUtils.isNotBlank(mobileNumber)) {
        UserDetailModel model = new UserDetailModel();
        model.mobileNo = mobileNumber;
        return model;
      }
    }
    Date loginTime = user.getLastLogin();
    updateUserMetaData(user, actionInitiator, headers, req);
    if(!loginModel.isSkipTwoFactorAuthentication()) {
      if (StringUtils.isNotEmpty(loginModel.getOtp())) {
        createUserDeviceInformation(headers, loginSource, user.getUserId(), loginTime);
      } else {
        String key = TwoFactorAuthenticationUtil.generateAuthKey(loginModel.getUserId());
        updateHeaderForUserDevice(headers, CommonUtils.getCookieByName(req, key), user.getUserId());
      }
    }
    //setting response headers
    setResponseHeaders(res, headers);
    return userBuilder.buildMobileAuthResponseModel(user);
  }

  private int getAppName(Integer actionInitiator, HttpServletRequest req) {
    String appName = req.getHeader(Constants.APP_NAME);

    int src = SourceConstants.MOBILE;
    if (!StringUtils.isEmpty(appName) && appName.equals(Constants.MMA_NAME)) {
      src = SourceConstants.MMA;
    } else if (actionInitiator == 1) {
      src = actionInitiator;
    }
    return src;
  }

  private IUserAccount validateUser(AuthLoginModel loginModel, Integer actionInitiator, Integer loginSource)
      throws BadCredentialsException, UserDisabledException, ServiceException {
    if (StringUtils.isBlank(loginModel.getUserId()) && StringUtils.isBlank(loginModel.getPassword())) {
      throw new BadRequestException("Login credentials are empty");
    }
    IUserAccount user;
    try {
      user = usersService.authenticateUser(loginModel.getUserId(), loginModel.getPassword(), loginSource);
    } catch (ObjectNotFoundException oe) {
      throw new BadCredentialsException("Invalid user name or password");
    }
    if (user == null) {
      throw new BadCredentialsException("Invalid user name or password");
    }
    if (!user.isEnabled()) {
      throw new UserDisabledException("Your account is disabled");
    }
    if ((SourceConstants.WEB.equals(actionInitiator) || SourceConstants.BULLETIN_BOARD
        .equals(actionInitiator)) && SecurityConstants.ROLE_KIOSKOWNER.equals(user.getRole())) {
      ResourceBundle backendMessages = Resources.get().getBundle("BackendMessages",
          Locale.getDefault());
      throw new UnauthorizedException(backendMessages.getString("user.access.denied"),HttpStatus.FORBIDDEN);
    }
    return user;
  }

  private String authenticateUser(AuthLoginModel loginModel, IUserAccount user,
                                  Integer loginSource,
                                  HttpServletRequest request)
      throws MessageHandlingException, IOException,
      NoSuchAlgorithmException {

    if (!loginModel.isSkipTwoFactorAuthentication()) {
      String otp = loginModel.getOtp();
      String
          deviceKey =
          CommonUtils.getCookieByName(request, TwoFactorAuthenticationUtil
              .generateAuthKey(user.getUserId()));

      boolean
          isUserAuthenticated =
          authenticationService
              .authenticateUserByCredentials(user.getUserId(), deviceKey, loginSource, otp, true);
      if (!isUserAuthenticated) {
        return user.getMobilePhoneNumber();
      }
    }
    return null;
  }


  private void updateUserMetaData(IUserAccount user, Integer actionInitiator, Map<String, String> headers, HttpServletRequest req)
      throws ServiceException {
    String userAgentStr = req.getHeader(USER_AGENT);
    String deviceDetails = req.getHeader(DEVICE_DETAILS);
    String deviceProfile = req.getHeader(DEVICE_PROFILE);
    String ipaddr = req.getHeader("X-REAL-IP");
    String appVer = req.getHeader(Constants.APP_VER);
    if (userAgentStr == null) {
      userAgentStr = "";
    }
    if (deviceDetails != null && !deviceDetails.isEmpty()) {
      userAgentStr += " [Device-details: " + deviceDetails + "]";
    }
    if (deviceProfile != null && !deviceProfile.isEmpty()) {
      userAgentStr += " [Device-Profile: " + deviceProfile + "]";
    }
    generateUserToken(headers, user.getUserId(), actionInitiator);
    Integer loginSource = getAppName(actionInitiator, req);
    user.setIPAddress(ipaddr);
    user.setLoginSource(loginSource);
    user.setPreviousUserAgent(user.getUserAgent());
    user.setUserAgent(userAgentStr);
    user.setAppVersion(appVer);
    //to store the history of user login's
    usersService.updateUserLoginHistory(user.getUserId(), loginSource, userAgentStr,
        ipaddr, new Date(), appVer);
    //update user account
    usersService.updateMobileLoginFields(user);
  }

  private void setResponseHeaders(HttpServletResponse response, Map<String, String> headers) {
    Set<Map.Entry<String, String>> entrySet = headers.entrySet();
    for (Map.Entry<String, String> entry : entrySet) {
      response.addHeader(entry.getKey(), entry.getValue());
    }
  }

  private void generateUserToken(Map<String, String> headers,
                                 String userid, int src)
      throws ServiceException {
    IUserToken token;
    token = authenticationService.generateUserToken(userid, src);
    if (token != null) {
      headers.put(Constants.TOKEN, token.getRawToken());
      if (token.getExpires() != null) {
        headers.put(Constants.EXPIRES, String.valueOf(token.getExpires().getTime()));
      }
    }
  }

  private void createUserDeviceInformation(Map<String, String> headers, Integer src, String userId,
                                           Date lastLogin)
      throws ServiceException, ConfigurationException, UnsupportedEncodingException,
      NoSuchAlgorithmException {
    String key = TwoFactorAuthenticationUtil.generateUserDeviceCacheKey(userId, lastLogin.getTime());
    UserDevicesVO
        userDevicesVO =
        userDevicesBuilder.buildUserDevicesVO(key, userId, src, lastLogin);
    twoFactorAuthenticationService.createUserDevices(userDevicesVO);
    updateHeaderForUserDevice(headers, key, userDevicesVO.getUserId());
  }

  private void updateHeaderForUserDevice(Map<String, String> headers, String key, String userId)
      throws UnsupportedEncodingException, NoSuchAlgorithmException {
    headers.put(TwoFactorAuthenticationUtil.generateAuthKey(userId), key);
  }
}
