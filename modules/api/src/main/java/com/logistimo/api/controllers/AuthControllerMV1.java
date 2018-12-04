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
import com.logistimo.api.models.auth.SaltModel;
import com.logistimo.api.models.mobile.ValidateOtpModel;
import com.logistimo.auth.SecurityConstants;
import com.logistimo.auth.service.AuthenticationService;
import com.logistimo.communications.MessageHandlingException;
import com.logistimo.constants.Constants;
import com.logistimo.constants.SourceConstants;
import com.logistimo.exception.BadRequestException;
import com.logistimo.exception.ForbiddenAccessException;
import com.logistimo.exception.ValidationException;
import com.logistimo.logger.XLog;
import com.logistimo.models.AuthRequest;
import com.logistimo.models.users.UserDetails;
import com.logistimo.security.BadCredentialsException;
import com.logistimo.services.ObjectNotFoundException;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;
import com.logistimo.twofactorauthentication.service.TwoFactorAuthenticationService;
import com.logistimo.twofactorauthentication.vo.UserDevicesVO;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.users.service.UsersService;
import com.logistimo.utils.CommonUtils;
import com.logistimo.utils.TwoFactorAuthenticationUtil;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@Controller
@RequestMapping("/mauth")
public class AuthControllerMV1 {

  private static final XLog xLogger = XLog.getLog(AuthControllerMV1.class);
  private static final String USER_AGENT = "User-Agent";
  private static final String DEVICE_DETAILS = "Device-Details";
  private static final String DEVICE_PROFILE = "Device-Profile";
  private static final String REFERER = "referer";

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


  private UserDetailModel processLoginRequest(AuthLoginModel loginModel,
                                              HttpServletRequest req, HttpServletResponse res)
      throws Exception {
    IUserAccount user;
    String authToken = req.getHeader(Constants.TOKEN);
    String initiator = req.getHeader(Constants.ACCESS_INITIATOR);
    Integer actionInitiator = StringUtils.isEmpty(initiator) ? -1 : Integer.parseInt(initiator);
    Integer loginSource = getAppName(actionInitiator, req);
    if (authToken != null) {
      user = AuthenticationUtil.authenticateToken(authToken, actionInitiator);
    } else {
      if (StringUtils.isEmpty(loginModel.getUserId()) && StringUtils.isEmpty(loginModel.getPassword())) {
        throw new BadRequestException("G011", null);
      }

      AuthRequest authRequest = buildAuthRequest(loginModel,req);
      if (StringUtils.isEmpty(loginModel.getOtp())
          && Objects.equals(SourceConstants.WEB, authRequest.getLoginSource())
          && AuthenticationUtil.isCaptchaEnabled()) {
        boolean isCaptchaVerified = authenticationService.verifyCaptcha(loginModel.getCaptcha());
        if (!isCaptchaVerified) {
          xLogger.warn("Captcha verification failed for user {0}", authRequest.getUserId());
          throw new BadRequestException("G010", null);
        }
      }

      UserDetails userDetails = authenticationService.authenticate(authRequest);
      user = usersService.getUserAccount(authRequest.getUserId());

      //authorize login req from source like Bulletin Board
      authorizeLoginBySource(user, actionInitiator, authRequest);

      String mobileNumber = authoriseDevice(loginModel, user, loginSource, req);
      if(StringUtils.isNotBlank(mobileNumber)) {
        UserDetailModel model = new UserDetailModel();
        model.mobileNo = mobileNumber;
        return model;
      }

      if(!loginModel.isSkipTwoFactorAuthentication()) {
        if (StringUtils.isNotEmpty(loginModel.getOtp())) {
          createUserDeviceInformation(res, loginSource, user.getUserId());
        } else {
          String key = TwoFactorAuthenticationUtil.generateAuthKey(loginModel.getUserId());
          updateHeaderForUserDevice(res, CommonUtils.getCookieByName(req, key), user.getUserId());
        }
      }
      //setting response headers
      userDetails.setRequestId(req.getHeader(Constants.REQ_ID));
      setResponseHeaders(res, userDetails);//
    }
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


  private AuthRequest buildAuthRequest (AuthLoginModel loginModel, HttpServletRequest req) {

    String initiator = req.getHeader(Constants.ACCESS_INITIATOR);
    Integer actionInitiator = StringUtils.isEmpty(initiator) ? -1 : Integer.parseInt(initiator);
    String appName = req.getHeader(Constants.APP_NAME);
    String appVer = req.getHeader(Constants.APP_VER);
    int src = SourceConstants.MOBILE;
    if (!StringUtils.isEmpty(appName) && appName.equals(Constants.MMA_NAME)) {
      src = SourceConstants.MMA;
    } else if (actionInitiator == 1) {
      src = actionInitiator;
    }
    String userAgentStr = req.getHeader(USER_AGENT);
    String deviceDetails = req.getHeader(DEVICE_DETAILS);
    String deviceProfile = req.getHeader(DEVICE_PROFILE);
    String ipaddr = req.getHeader("X-REAL-IP");
    String referrer = req.getHeader(REFERER);
    if (userAgentStr == null) {
      userAgentStr = "";
    }
    if (deviceDetails != null && !deviceDetails.isEmpty()) {
      userAgentStr += " [Device-details: " + deviceDetails + "]";
    }
    if (deviceProfile != null && !deviceProfile.isEmpty()) {
      userAgentStr += " [Device-Profile: " + deviceProfile + "]";
    }

    return AuthRequest.builder()
        .userId(loginModel.getUserId())
        .password(loginModel.getPassword())
        .ipAddress(ipaddr)
        .loginSource(src)
        .referer(referrer)
        .sourceVersion(appVer)
        .userAgent(userAgentStr).build();

  }

  private void authorizeLoginBySource(IUserAccount user, Integer actionInitiator,
                                      AuthRequest authRequest) {
    if ((SourceConstants.WEB.equals(actionInitiator) || SourceConstants.BULLETIN_BOARD
        .equals(actionInitiator)) && SecurityConstants.ROLE_KIOSKOWNER.equals(user.getRole())) {
      ResourceBundle backendMessages = Resources.get().getBundle("BackendMessages",
          Locale.getDefault());
      authenticationService.logForbiddenAccess(authRequest);
      throw new ForbiddenAccessException(backendMessages.getString("user.access.denied"));
    }
  }

  private void setResponseHeaders(HttpServletResponse response, UserDetails userDetails) {
    if (userDetails.getToken() != null) {
      response.addHeader(Constants.TOKEN, userDetails.getToken());
      response.addHeader("Set-Cookie",Constants.TOKEN+"="+userDetails.getToken() + ";Path=/; HttpOnly");
      if (userDetails.getTokenExpiry() != null) {
        response.addHeader(Constants.EXPIRES, String.valueOf(userDetails.getTokenExpiry()));
      }
    }
    if (userDetails.getToken() != null) {
      response.addHeader(Constants.REQ_ID, userDetails.getRequestId());
    }
  }

  /**
   * This method will validate user's otp
   *
   * @param otpModel consists of user id, otp
   */
  @RequestMapping(value = "/validate-otp", method = RequestMethod.POST)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void validateOtp(@RequestBody ValidateOtpModel otpModel) throws ValidationException {
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
      throws ServiceException, ObjectNotFoundException {
    String initiatorStr = req.getHeader(Constants.ACCESS_INITIATOR);
    int
        accessInitiator =
        StringUtils.isNotBlank(initiatorStr) ? Integer.parseInt(initiatorStr) : -1;
    return authenticationService.authenticateToken(token, accessInitiator).getUserId();
  }

  private String authoriseDevice(AuthLoginModel loginModel, IUserAccount user,
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

  /**
   * This will reset user's password
   *
   * @param pwdModel consists of user id ,otp and new password
   */
  @RequestMapping(value = "/change-password", method = RequestMethod.POST)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void resetPassword(@RequestBody ChangePasswordModel pwdModel) throws ServiceException {
    if(StringUtils.isEmpty(pwdModel.getOtp())){
      throw new ValidationException("UA002");
    }
    authenticationService.changePassword(pwdModel.getUid(), pwdModel.getOtp(), null, pwdModel.getNpd(),pwdModel.isEnhanced());
  }

  /**
   * This gives user's salt
   *
   * @param userId
   * @return salt stored against queried user
   */
  @RequestMapping(value = "/get-salt/{userId}" ,method = RequestMethod.GET)
  public @ResponseBody SaltModel getSalt (@PathVariable String userId) {
    return new SaltModel(authenticationService.getUserSalt(userId));
  }

  /**
   * This gives user's salt
   *
   * @return salt stored against queried user
   */
  @RequestMapping(value = "/random-salt/" ,method = RequestMethod.GET)
  public @ResponseBody
  SaltModel randomSalt () {
    return new SaltModel(authenticationService.generateRandomSalt());
  }

  protected void createUserDeviceInformation(HttpServletResponse response, Integer src, String userId)
      throws Exception {
    Date twoFactorTokenGenerationTime = new Date();
    String cookieValue = TwoFactorAuthenticationUtil.generateUserDeviceCacheKey(userId, twoFactorTokenGenerationTime.getTime());
    UserDevicesVO
        userDevicesVO =
        userDevicesBuilder.buildUserDevicesVO(cookieValue, userId, src, twoFactorTokenGenerationTime);
    twoFactorAuthenticationService.createUserDevices(userDevicesVO);
    updateHeaderForUserDevice(response, cookieValue, userDevicesVO.getUserId());
  }

  protected void updateHeaderForUserDevice(HttpServletResponse response, String cookieValue, String userId)
      throws UnsupportedEncodingException, NoSuchAlgorithmException {
    response.addHeader("Set-Cookie",
        TwoFactorAuthenticationUtil.generateAuthKey(userId) + "=" + cookieValue + ";Path=/; HttpOnly");
  }

}
