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

package com.logistimo.api.servlets.mobile;

import com.logistimo.api.auth.AuthenticationUtil;
import com.logistimo.api.builders.UserDevicesBuilder;
import com.logistimo.api.servlets.JsonRestServlet;
import com.logistimo.api.servlets.mobile.json.JsonOutput;
import com.logistimo.api.util.RESTUtil;
import com.logistimo.auth.SecurityMgr;
import com.logistimo.auth.service.AuthenticationService;
import com.logistimo.auth.service.impl.AuthenticationServiceImpl;
import com.logistimo.communications.MessageHandlingException;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.constants.Constants;
import com.logistimo.constants.SourceConstants;
import com.logistimo.context.StaticApplicationContext;
import com.logistimo.exception.UnauthorizedException;
import com.logistimo.exception.ValidationException;
import com.logistimo.logger.XLog;
import com.logistimo.models.AuthRequest;
import com.logistimo.models.users.UserDetails;
import com.logistimo.pagination.PageParams;
import com.logistimo.proto.ProtocolException;
import com.logistimo.proto.RestConstantsZ;
import com.logistimo.security.BadCredentialsException;
import com.logistimo.security.UserDisabledException;
import com.logistimo.services.ObjectNotFoundException;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;
import com.logistimo.services.utils.ConfigUtil;
import com.logistimo.twofactorauthentication.service.TwoFactorAuthenticationService;
import com.logistimo.twofactorauthentication.vo.UserDevicesVO;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.users.service.UsersService;
import com.logistimo.users.service.impl.UsersServiceImpl;
import com.logistimo.utils.HttpUtil;
import com.logistimo.utils.LocalDateUtil;
import com.logistimo.utils.TwoFactorAuthenticationUtil;

import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Date;
import java.util.InputMismatchException;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.jdo.JDOObjectNotFoundException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Newer login servlet conforming to version 01 of REST protocol
 *
 * @author Arun
 */

@SuppressWarnings("serial")
public class LoginServlet extends JsonRestServlet {

  private static final String USER_AGENT = "User-Agent";
  private static final String DEVICE_DETAILS = "Device-Details";
  private static final String DEVICE_PROFILE = "Device-Profile";
  private static final String REFERER = "referer";
  private static final String START = "start";
  private static final String FORGOT_PASSWRD = "fp";
  private static final String DEFAULT_VERSION = "01";
  private static final String OTP = "otp";
  private static final String AU = "au";
  private static final String TYPE_MOBILE = "p";
  private static final String TYPE_EMAIL = "e";
  private static final String AU_VAL = "o";
  // Added a logger to help debug this servlet's behavior (arun, 1/11/09)
  private static final XLog xLogger = XLog.getLog(LoginServlet.class);
  private static boolean isGAE = ConfigUtil.getBoolean("gae.deployment", true);


  public void processGet(HttpServletRequest req, HttpServletResponse resp,
                         ResourceBundle messages)
      throws IOException, ServiceException {
    String action = req.getParameter(RestConstantsZ.ACTION);
    if (RestConstantsZ.ACTION_LOGIN.equalsIgnoreCase(action)) {
      authenticateUser(req, resp, messages, true);
    } else if(RestConstantsZ.ACTION_NEW_LOGIN.equalsIgnoreCase(action)) {
      authenticateUser(req, resp, messages, false);
    } else if (RestConstantsZ.ACTION_LOGOUT.equalsIgnoreCase(action)) {
      xLogger.info(
          "Logged out - does nothing, just there for backward compatibility, if at all needed");
    } else if (FORGOT_PASSWRD.equalsIgnoreCase(action)) {
      validateForgotPassword(req, resp, messages);
    } else {
      throw new ServiceException("Invalid action: " + action);
    }
  }

  public void processPost(HttpServletRequest req, HttpServletResponse resp,
                          ResourceBundle messages)
      throws IOException, ServiceException {
    processGet(req, resp, messages);
  }

  public void authenticateUser(HttpServletRequest req, HttpServletResponse resp,
                               ResourceBundle backendMessages,
                               boolean skipTwoFactorAuthentication)
      throws IOException {
    xLogger.fine("Entering authenticateUser");
    UsersService usersService = null;
    IUserAccount user = null;
    AuthenticationService authenticationService = StaticApplicationContext.getBean(AuthenticationServiceImpl.class);
    boolean status = true;
    String message = null;
    Long domainId = null;
    boolean forceIntegerForStock = false;
    String jsonString = null;
    Date start = null;
    String locale;
    boolean onlyAuthenticate = false;
    // whether min. data is to be sent back - "1" = only kiosk info., in case of multiple kiosks; "2" = same as "1", but also do NOT send related kiosks info. (for each kiosk); null implies send back everything (kiosk info., materials and related kiosk info.)
    String minResponseCode = req.getParameter(RestConstantsZ.MIN_RESPONSE);
    // Getting authentication token for user
    String authToken = req.getHeader(Constants.TOKEN);
    int actionInitiator = getActionInitiator(req);

    // Get the size & offset, if available
    String sizeStr = req.getParameter(RestConstantsZ.SIZE);
    String offsetStr = req.getParameter(Constants.OFFSET);
    int offset = 0;
    if (StringUtils.isNotBlank(offsetStr)) {
      try {
        offset = Integer.parseInt(offsetStr);
      } catch (Exception e) {
        xLogger.warn("Invalid offset {0}: {1}", offsetStr, e.getMessage());
      }
    }
    boolean isTwoFAInitiated = false;

    PageParams pageParams = null;
    // Get page params, if any (allow NULL possibility to enable backward compatibility, where size/cursor is never sent)
    if (sizeStr != null && !sizeStr.isEmpty()) {
      try {
        int size = Integer.parseInt(sizeStr);
        pageParams = new PageParams(offset, size);
      } catch (Exception e) {
        xLogger.warn("Invalid size {0}: {1}", sizeStr, e.getMessage());
      }
    }

    boolean skipInventory =
        Boolean.parseBoolean(req.getParameter(Constants.SKIP_INVENTORY));

    if (authToken != null) {
      try {
        user = AuthenticationUtil.authenticateToken(authToken, actionInitiator);
        if (RESTUtil.checkIfLoginShouldNotBeAllowed(user, resp, req)) {
          xLogger.warn("Switching user {0} to new host...", user.getUserId());
          return;
        }
        domainId = user.getDomainId();
      } catch (ServiceException | ObjectNotFoundException | JDOObjectNotFoundException | UnauthorizedException e) {
        xLogger.warn("Invalid token: ", e);
        message = "Invalid token";
        status = false;
        try {
          jsonString =
              RESTUtil.getJsonOutputAuthenticate(status, user, message, null, null,
                  false, false, null, Optional.<Date>empty(), null, true);
          resp.setStatus(401);
          resp.setContentType(JSON_UTF8);
          PrintWriter pw = resp.getWriter();
          pw.write(jsonString);
          pw.close();
        } catch (Exception e1) {
          xLogger.warn("Protocol exception after data formatting error (during login): {0}",
              e.getMessage());
          resp.setStatus(500);
          return;
        }
      }
    } else {
      String appVersion = null;
      String onlyAuthenticateStr = req.getParameter(RestConstantsZ.ONLY_AUTHENTICATE);
      onlyAuthenticate = (onlyAuthenticateStr != null);
      locale = req.getParameter(RestConstantsZ.LOCALE);
      // Get locale
      if (locale == null || locale.isEmpty()) {
        locale = Constants.LANG_DEFAULT;
      }
      // Get the start date and time
      String startDateStr = req.getParameter(START);
      AuthRequest authRequest = buildAuthRequest(req);
      try {
        usersService = StaticApplicationContext.getBean(UsersServiceImpl.class);
        authenticationService = StaticApplicationContext.getBean(AuthenticationService.class);
        // Check if user ID and password is sent as Basic auth. header
        if (authRequest.getUserId() != null && authRequest.getPassword() != null) { // no problems with userId/password so far
          // Authenticate user
          UserDetails userDetails = authenticationService.authenticate(authRequest);
          // Get user details
          user = usersService.getUserAccount(authRequest.getUserId());
          if (userDetails != null) {
            //adding token and token expiry
            setResponseHeaders(resp,userDetails);
            // Switch the user to another host, if that is enabled
            if (status && RESTUtil.checkIfLoginShouldNotBeAllowed(user, resp, req)) {
              xLogger.warn("Switching user {0} to new host...", authRequest.getUserId());
              return;
            }
            String successMsg = null;
            if(!skipTwoFactorAuthentication) {
              successMsg = authenticateUserByCredentials(user, authenticationService, req);
            }
            if(StringUtils.isNotBlank(successMsg)) {
              xLogger.info(successMsg);
              status = false;
              message = successMsg;
              isTwoFAInitiated = true;
            } else {
            domainId = user.getDomainId();
            // Get the resource bundle according to the user's login
            try {
              backendMessages = Resources.getBundle(user.getLocale());
            } catch (MissingResourceException e) {
              xLogger
                  .severe("Unable to get resource bundles BackendMessages for locale {0}", locale);
            }
            if (startDateStr != null && !startDateStr.isEmpty()) {
              // Convert the start string to a Date format.
              try {
                start =
                    LocalDateUtil
                        .parseCustom(startDateStr, Constants.DATETIME_FORMAT, user.getTimezone());
              } catch (ParseException pe) {
                status = false;
                backendMessages = Resources.getBundle(user.getLocale());
                message = backendMessages.getString("error.invalidstartdate");
                xLogger.severe("Exception while parsing start date. Exception: {0}, Message: {1}",
                    pe.getClass().getName(), pe.getMessage());
                }
              }
            }
          } else {
            // Authentication failed, use the locale from the request, if provided
            status = false;
            try {
              backendMessages = Resources.getBundle(new Locale(locale));
              message = backendMessages.getString("error.invalidusername");
            } catch (MissingResourceException e) {
              xLogger
                  .severe("Unable to get resource bundles BackendMessages for locale {0}", locale);
            }
            xLogger.warn("Unable to authenticate user {0}: {1}", authRequest.getUserId(), message);
          }
          appVersion = authRequest.getSourceVersion();
          // FOR BACKWARD COMPATIBILITY: check whether stock has to be sent back as integer (it is float beyond mobile app. version 1.2.0)
          forceIntegerForStock = RESTUtil.forceIntegerForStock(appVersion);
        } else {
          status = false;
          message = backendMessages.getString("error.invalidusername");
        }
      } catch (BadCredentialsException | UserDisabledException e) {
        xLogger.warn("Issue with login for user: {0}", authRequest.getUserId(),e);
        status = false;
        message = backendMessages.getString("error.invalidusername");
      } catch (ServiceException e) {
        xLogger.severe("Service Exception during login: {0}", e.getMessage(),e);
        status = false;
        message = backendMessages.getString("error.systemerror");
      } catch (MessageHandlingException e) {
        xLogger.severe("Error while sending OTP to user: {0}", user.getUserId(), e);
        status = false;
        message = backendMessages.getString("otp.error.msg");
      } catch (NoSuchAlgorithmException e) {
        xLogger.severe("Error while generating authentication key for user: {0}", user.getDomainId(), e);
        status = false;
        message = "Error while generating authentication key for user: " + user.getDomainId();
      } catch (ValidationException e) {
        String msg = "Invalid one time password entered for user " + user.getUserId();
        xLogger.severe(msg);
        status = false;
        message = msg;
      }

      //Generate authentication token and update the UserTokens table with userId, Token and Expires

        try {
          if(!status){
            jsonString =
                RESTUtil.getJsonOutputAuthenticate(status, user, message, null, minResponseCode,
                    onlyAuthenticate, forceIntegerForStock, start, null, pageParams, true);
            if(isTwoFAInitiated) {
              sendJsonResponse(resp, HttpServletResponse.SC_OK, jsonString);
            } else {
              sendJsonResponse(resp, HttpServletResponse.SC_UNAUTHORIZED, jsonString);
            }
            return;
          }
        } catch (ServiceException|ProtocolException e) {
          xLogger.severe("Service Exception during login: {0}", e.getMessage(), e);
          status = false;
          message = backendMessages.getString("error.systemerror");
        }



        // FOR BACKWARD COMPATIBILITY: check whether stock has to be sent back as integer (it is float beyond mobile app. version 1.2.0)
        forceIntegerForStock = RESTUtil.forceIntegerForStock(appVersion);
        // Persist the user account object, to store the changes, esp. last reconnected time
        // NOTE: we are doing this post sending response, so that respone time is not impacted
      if (user != null) {
        try {
          Date loginTime = user.getLastLogin();
          if(!skipTwoFactorAuthentication) {
            if (StringUtils.isNotBlank(req.getParameter(RestConstantsZ.OTP))) {
              createUserDeviceInformation(SourceConstants.MOBILE, user.getUserId(), loginTime, resp);
            } else {
              String key = TwoFactorAuthenticationUtil.generateAuthKey(user.getUserId());
              resp.setHeader(key, req.getHeader(key));
            }
          }
        } catch (Exception e) {
          xLogger.severe("{0} when trying to store user account object {1} in domain {2}: {3}",
              e.getClass().getName(), user.getUserId(), domainId, e.getMessage());
        }
      }
      }
      Date lastModified = new Date();
      Optional<Date> modifiedSinceDate = Optional.empty();
      try {
        // Get the domain configuration from the data store
        DomainConfig dc = null;
        if (domainId != null) {
          dc = DomainConfig.getInstance(domainId);
        }
        modifiedSinceDate = HttpUtil.getModifiedDate(req);
        jsonString =
            RESTUtil.getJsonOutputAuthenticate(status, user, message, dc, minResponseCode,
                onlyAuthenticate, forceIntegerForStock, start, modifiedSinceDate, pageParams, skipInventory);
      } catch (Exception e2) {
        xLogger.warn("Protocol exception during login: {0}", e2);
        if (status) { // that is, login successful, but data formatting exception
          status = false;
          message = backendMessages.getString("error.nomaterials");
          try {
            jsonString =
                RESTUtil
                    .getJsonOutputAuthenticate(status, null, message, null, minResponseCode,
                        onlyAuthenticate, forceIntegerForStock, start, modifiedSinceDate, null, true);
          } catch (Exception e) {
            xLogger.severe("Protocol exception after data formatting error (during login): {0}", e);
            resp.setStatus(500);
            return;
          }
        }
      }
      if (jsonString != null) {
        HttpUtil.setLastModifiedHeader(resp, lastModified);
        sendJsonResponse(resp, HttpServletResponse.SC_OK, jsonString);
      }

  }

  private AuthRequest buildAuthRequest (HttpServletRequest req) {

    // Get request parameters
    String userId = req.getParameter(RestConstantsZ.USER_ID);
    String password = req.getParameter(RestConstantsZ.PASSWRD);
    String version = req.getParameter(RestConstantsZ.VERSION);
    // Get the user-agent and device details from header, if available
    String userAgentStr = req.getHeader(USER_AGENT);
    String deviceDetails = req.getHeader(DEVICE_DETAILS);
    String deviceProfile = req.getHeader(DEVICE_PROFILE);
    String referer = req.getHeader(REFERER);
    if (userAgentStr == null) {
      userAgentStr = "";
    }
    // Add device details to user-agent, if sent
    if (deviceDetails != null && !deviceDetails.isEmpty()) {
      userAgentStr += " [Device-details: " + deviceDetails + "]";
    }
    if (deviceProfile != null && !deviceProfile.isEmpty()) {
      userAgentStr += " [Device-Profile: " + deviceProfile + "]";
    }
    // Get the user's IP address, if available
    String ipAddress = isGAE ? req.getRemoteAddr() : req.getHeader("X-REAL-IP");
    xLogger.fine("ip: {0}, headers: {1}", ipAddress, req.getHeader("X-Forwarded-For"));
    // Init. flags
    // Check if user ID and password is sent as Basic auth. header
    SecurityMgr.Credentials creds = SecurityMgr.getUserCredentials(req);
    if (creds != null) {
      userId = creds.userId;
      password = creds.password;
    }

    AuthRequest authRequest = AuthRequest.builder()
        .userId(userId)
        .password(password)
        .ipAddress(ipAddress)
        .loginSource(SourceConstants.MOBILE)
        .referer(referer)
        .sourceVersion(version)
        .userAgent(userAgentStr).build();
    return authRequest;
  }

  private void setResponseHeaders(HttpServletResponse response, UserDetails userDetails) {
    if (userDetails.getToken() != null) {
      response.addHeader(Constants.TOKEN, userDetails.getToken());
      if (userDetails.getTokenExpiry() != null) {
        response.addHeader(Constants.EXPIRES, String.valueOf(userDetails.getTokenExpiry()));
      }
    }
    if (userDetails.getToken() != null) {
      response.addHeader(Constants.REQ_ID, userDetails.getRequestId());
    }
  }

  public void generateNewPassword(HttpServletRequest req, HttpServletResponse resp,
                                  ResourceBundle backendMessages) {
    xLogger.fine("Entering forgot password");
    String message = null;
    boolean status = false;
    String userId = req.getParameter(RestConstantsZ.USER_ID);
    String sendType = req.getParameter(RestConstantsZ.TYPE);
    String otp = req.getParameter(OTP);
    if (userId != null) {
      try {
        AuthenticationService as = StaticApplicationContext.getBean(AuthenticationServiceImpl.class);
        String successMsg;
        String au = req.getParameter("au");
        if (TYPE_EMAIL.equalsIgnoreCase(sendType)) {
          successMsg = as.resetPassword(userId, 1, otp, "m", au);
        } else {
          successMsg = as.resetPassword(userId, 0, otp, "m", au);
        }
        if (StringUtils.isNotEmpty(successMsg)) {
          status = true;
        }
      } catch (ServiceException | IOException e) {
        xLogger
            .severe("Error while processing forgot password request: {0}, user: {1} and type: {2}",
                e.getMessage(), userId, sendType, e);
        message = backendMessages.getString("error.systemerror");
      } catch (ObjectNotFoundException e) {
        xLogger.warn("Error while processing forgot password request: No user found with ID: {0}",
            userId);
        message = backendMessages.getString("user.none") + ": " + userId;
      } catch (MessageHandlingException e) {
        xLogger.warn("Error while processing forgot password request: {0}, user: {1} and type: {2}",
            e.getMessage(), userId, sendType, e);
        message = backendMessages.getString("error.systemerror");
      } catch (InputMismatchException | ValidationException e) {
        xLogger.warn("Error while processing forgot password request: {0}, user: {1} and type: {2}",
            e.getMessage(), userId, sendType, e);
        message = backendMessages.getString("password.otp.invalid");
      }
    } else {
      xLogger.warn("No user name to generate password of type: {1}", sendType);
      message = backendMessages.getString("error.invalidusername");
    }
    try {
      sendJsonResponse(resp, HttpServletResponse.SC_OK,
          new JsonOutput(DEFAULT_VERSION, status, status ? null : message).toJSONString());
    } catch (IOException e) {
      resp.setStatus(500);
    }
    xLogger.fine("Exiting forgot password");
  }

  public void generateOtpLink(HttpServletRequest request, HttpServletResponse response,
                              ResourceBundle backendMessages) {
    xLogger.fine("Entering generate OTP");
    String message = null;
    boolean status = false;
    String userId = request.getParameter(RestConstantsZ.USER_ID);
    String sendType = request.getParameter(RestConstantsZ.TYPE);
    if (userId != null) {
      try {
        AuthenticationService as = StaticApplicationContext.getBean(AuthenticationServiceImpl.class);
        String successMsg = null;
        String host = request.getHeader("host");
        if (TYPE_EMAIL.equalsIgnoreCase(sendType)) {
          successMsg = as.generateOTP(userId, 1, "m", host);
        } else if (TYPE_MOBILE.equalsIgnoreCase(sendType)) {
          successMsg = as.generateOTP(userId, 0, "m", host);
        }
        if (StringUtils.isNotEmpty(successMsg)) {
          status = true;
        }
      } catch (ServiceException | IOException e) {
        xLogger
            .severe("Error while processing forgot password request: {0}, user: {1} and type: {2}",
                e.getMessage(), userId, sendType, e);
        message = backendMessages.getString("error.systemerror");
      } catch (ObjectNotFoundException e) {
        xLogger.warn("Error while processing forgot password request: No user found with ID: {0}",
            userId);
        message = backendMessages.getString("user.none") + ": " + userId;
      } catch (MessageHandlingException e) {
        xLogger.warn("Error while processing forgot password request: {0}, user: {1} and type: {2}",
            e.getMessage(), userId, sendType, e);
        message = backendMessages.getString("error.systemerror");
      }
    } else {
      xLogger.warn("No user name to generate OTP of type: {0}", sendType);
      message = backendMessages.getString("error.invalidusername");
    }
    try {
      sendJsonResponse(response, HttpServletResponse.SC_OK,
          new JsonOutput(DEFAULT_VERSION, status, status ? null : message).toJSONString());
    } catch (IOException e) {
      response.setStatus(500);
    }
    xLogger.fine("Exiting generate OTP");
  }

  public void validateForgotPassword(HttpServletRequest req, HttpServletResponse resp,
                                     ResourceBundle backendMessages) {
    String otp = req.getParameter(OTP);
    String au = req.getParameter(AU);
    String type = req.getParameter(RestConstantsZ.TYPE);
    String message = null;
    if (StringUtils.isNotEmpty(type) && (TYPE_MOBILE.equalsIgnoreCase(type) || TYPE_EMAIL
        .equalsIgnoreCase(type))) {
      if (StringUtils.isNotEmpty(au)) {
        if (AU_VAL.equalsIgnoreCase(au)) {
          if (StringUtils.isNotEmpty(otp)) {
            if (TYPE_MOBILE.equalsIgnoreCase(type)) {
              generateNewPassword(req, resp, backendMessages);
            } else {
              message = "Invalid reqeust";
            }
          } else {
            generateOtpLink(req, resp, backendMessages);
          }
        } else {
          message = "Invalid AU parameter au: " + au;
        }
      } else {
        generateNewPassword(req, resp, backendMessages);
      }
    } else {
      message = "Invalid type parameter ty: " + type;
    }

    if (StringUtils.isNotEmpty(message)) {
      try {
        sendJsonResponse(resp, HttpServletResponse.SC_OK,
            new JsonOutput(DEFAULT_VERSION, false, message).toJSONString());
      } catch (IOException e) {
        resp.setStatus(500);
      }
    }
  }

  private int getActionInitiator(HttpServletRequest req) {
    String sourceInitiatorStr = req.getHeader(Constants.ACCESS_INITIATOR);
    int actionInitiator = -1;
    if (sourceInitiatorStr != null) {
      try {
        actionInitiator = Integer.parseInt(sourceInitiatorStr);
      } catch (NumberFormatException e) {
        // ignore
      }
    }
    return actionInitiator;
  }

  private String authenticateUserByCredentials(IUserAccount user,
                                               AuthenticationService authenticationService,
                                               HttpServletRequest request)
      throws IOException, MessageHandlingException,
      NoSuchAlgorithmException {
    String otp = request.getParameter(RestConstantsZ.OTP);
    String deviceKey = request.getHeader(
        TwoFactorAuthenticationUtil.generateAuthKey(user.getUserId()));
    boolean
        isUserAuthenticated =
        authenticationService
            .authenticateUserByCredentials(user.getUserId(), deviceKey, SourceConstants.MOBILE, otp,
                true);
    if (!isUserAuthenticated) {
      return user.getMobilePhoneNumber();
    }
    return null;
  }

  private void createUserDeviceInformation(Integer src, String userId,
                                           Date lastLogin, HttpServletResponse response)
      throws Exception {
    String
        key =
        TwoFactorAuthenticationUtil.generateUserDeviceCacheKey(userId, lastLogin.getTime());
    UserDevicesBuilder
        userDevicesBuilder =
        StaticApplicationContext.getBean(UserDevicesBuilder.class);
    TwoFactorAuthenticationService
        twoFactorAuthenticationService =
        StaticApplicationContext.getBean(TwoFactorAuthenticationService.class);
    UserDevicesVO
        userDevicesVO =
        userDevicesBuilder.buildUserDevicesVO(key, userId, src, lastLogin);
    twoFactorAuthenticationService.createUserDevices(userDevicesVO);
    updateHeaderForUserDevice(key, userDevicesVO.getUserId(), response);
  }

  private void updateHeaderForUserDevice(String key, String userId,
                                         HttpServletResponse response)
      throws UnsupportedEncodingException, NoSuchAlgorithmException {
    response.setHeader(TwoFactorAuthenticationUtil.generateAuthKey(userId), key);
  }


}
