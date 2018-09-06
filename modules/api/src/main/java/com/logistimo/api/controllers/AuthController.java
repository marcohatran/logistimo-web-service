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

import com.logistimo.api.models.AuthLoginModel;
import com.logistimo.api.models.AuthModel;
import com.logistimo.api.models.ChangePasswordModel;
import com.logistimo.api.models.PasswordModel;
import com.logistimo.auth.SecurityConstants;
import com.logistimo.auth.SecurityMgr;
import com.logistimo.auth.SecurityUtil;
import com.logistimo.auth.service.AuthenticationService;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.auth.utils.SessionMgr;
import com.logistimo.communications.MessageHandlingException;
import com.logistimo.communications.service.MessageService;
import com.logistimo.constants.Constants;
import com.logistimo.dao.JDOUtils;
import com.logistimo.events.entity.IEvent;
import com.logistimo.events.processor.EventPublisher;
import com.logistimo.exception.BadRequestException;
import com.logistimo.exception.ValidationException;
import com.logistimo.logger.XLog;
import com.logistimo.security.BadCredentialsException;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.security.UserDisabledException;
import com.logistimo.services.ObjectNotFoundException;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;
import com.logistimo.services.cache.MemcacheService;
import com.logistimo.services.impl.PMF;
import com.logistimo.services.utils.ConfigUtil;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.users.entity.IUserToken;
import com.logistimo.users.entity.UserAccount;
import com.logistimo.users.service.UsersService;
import com.logistimo.utils.MsgUtil;
import com.logistimo.utils.RandomPasswordGenerator;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.jdo.PersistenceManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Created by Mohan Raja on 03/04/15
 */
@Controller
@RequestMapping("/auth")
public class AuthController {
  private static final XLog xLogger = XLog.getLog(AuthController.class);
  private static final Integer INVALID_USERNAME = 1;
  private static final Integer ALREADY_LOGGED_IN = 2;
  private static final Integer ACCOUNT_DISABLED = 3;
  private static final Integer ACCESS_DENIED = 4;
  private static final Integer SYSTEM_ERROR = 5;
  private static final Integer USER_NOTFOUND = 6;
  private static final Integer EMAIL_UNAVAILABLE = 7;
  private static final Integer OTP_EXPIRED = 8;
  private static final Integer PASSWORD_MISMATCH = 9;
  private static final Integer LINK_EXPIRED = 10;
  private static boolean isGAE = ConfigUtil.getBoolean("gae.deployment", true);
  private static final String WEB = "w";

  private AuthenticationService authenticationService;
  private UsersService usersService;
  private MemcacheService memcacheService;

  @Autowired
  public void setAuthenticationService(AuthenticationService authenticationService) {
    this.authenticationService = authenticationService;
  }

  @Autowired
  public void setMemcacheService(MemcacheService memcacheService) {
    this.memcacheService = memcacheService;
  }

  @Autowired
  public void setUsersService(UsersService usersService) {
    this.usersService = usersService;
  }


  private static void updateUserDetails(SecureUserDetails userDetails, String ipAddress,
                                        String userAgent) {
    PersistenceManager pm = null;
    try {
      pm = PMF.get().getPersistenceManager();
      IUserAccount u = JDOUtils.getObjectById(IUserAccount.class, userDetails.getUsername(), pm);
      u.setLastLogin(new Date());
      u.setIPAddress(ipAddress);
      u.setPreviousUserAgent(u.getUserAgent());
      u.setUserAgent(userAgent);
      u.setAppVersion("LogiWeb");
      Map<String, Object> params = new HashMap<>(1);
      params.put("ipaddress", u.getIPAddress());
      EventPublisher.generate(u.getDomainId(), IEvent.IP_ADDRESS_MATCHED, params,
          UserAccount.class.getName(), u.getKeyString(),
          null);
    } catch (Exception ignored) {
      // do nothing
    } finally {
      if (pm != null) {
        pm.close();
      }
    }
  }

  @RequestMapping(value = "/login", method = RequestMethod.POST)
  public
  @ResponseBody
  AuthModel login(@RequestBody AuthLoginModel authLoginModel, HttpServletRequest request) {
    try {
      HttpSession session = request.getSession();
      if (SecurityMgr.isLoggedInAsAnotherUser(session, authLoginModel.getUserId())) {
        return constructAuthModel(ALREADY_LOGGED_IN, authLoginModel.getLanguage());
      }
      SecureUserDetails
          userDetails =
          SecurityMgr.authenticate(authLoginModel.getUserId(), authLoginModel.getPassword());
      //Recreates and initialize the session after successful login.
      SessionMgr.recreateSession(request, userDetails);
      Long domainId = SecurityUtils.getReqCookieDomain(request);
      if (domainId != null && usersService.hasAccessToDomain(userDetails.getUsername(), domainId)) {
          SessionMgr.setCurrentDomain(request.getSession(), domainId);
      }
      String ipAddress = isGAE ? request.getRemoteAddr() : request.getHeader("X-REAL-IP");
      updateUserDetails(userDetails, ipAddress, request.getHeader("User-Agent"));

      xLogger.info("ip: {0}, headers: {1}", ipAddress, request.getHeader("X-Forwarded-For"));
      if (SecurityConstants.ROLE_KIOSKOWNER.equals(userDetails.getRole())) {
        SessionMgr.cleanupSession(request.getSession());
        return constructAuthModel(ACCESS_DENIED, authLoginModel.getLanguage());
      }
      return constructAuthModel(0, authLoginModel.getLanguage());
    } catch (BadCredentialsException e) {
      xLogger.warn("Invalid user name or password: {0}", authLoginModel.getUserId(), e);
      return constructAuthModel(INVALID_USERNAME, authLoginModel.getLanguage());
    } catch (UserDisabledException e) {
      xLogger.warn("User disabled: {0}", authLoginModel.getUserId(), e);
      return constructAuthModel(ACCOUNT_DISABLED, authLoginModel.getLanguage());
    } catch (ObjectNotFoundException e) {
      xLogger.warn("User not found: {0}", authLoginModel.getUserId(), e);
      return constructAuthModel(INVALID_USERNAME, authLoginModel.getLanguage());
    } catch (Exception e) {
      xLogger.severe("{0} when authenticating user {1}: {2}", e.getClass().getName(),
          authLoginModel.getUserId(), e.getMessage(), e);
      return constructAuthModel(ACCESS_DENIED, authLoginModel.getLanguage());
    }
  }

  private AuthModel constructAuthModel(int status, String language) {
    AuthModel model = new AuthModel();
    Locale locale;
    if (language != null) {
      locale = new Locale(language);
    } else {
      locale = new Locale("en");
    }
    try {
      ResourceBundle backendMessages = Resources.get().getBundle("BackendMessages", locale);
      model.isError = true;
      model.ec = status;
      if (status == INVALID_USERNAME) {
        model.errorMsg = backendMessages.getString("user.invalidup");
      } else if (status == ALREADY_LOGGED_IN) {
        model.errorMsg = backendMessages.getString("user.already.logged");
      } else if (status == ACCOUNT_DISABLED) {
        model.errorMsg = backendMessages.getString("your.account.disabled");
      } else if (status == ACCESS_DENIED) {
        model.errorMsg = backendMessages.getString("user.access.denied");
      } else if (status == SYSTEM_ERROR) {
        model.errorMsg = backendMessages.getString("system.error");
      } else if (status == USER_NOTFOUND) {
        model.errorMsg = backendMessages.getString("user.none");
      } else if (status == EMAIL_UNAVAILABLE) {
        model.errorMsg = backendMessages.getString("password.email.unavailable");
      } else if (status == OTP_EXPIRED) {
        model.errorMsg = backendMessages.getString("password.otp.invalid");
      } else if (status == PASSWORD_MISMATCH) {
        model.errorMsg = backendMessages.getString("password.confirm.mismatch");
      } else if (status == LINK_EXPIRED) {
        model.errorMsg = backendMessages.getString("password.otp.expired");
      } else {
        model.isError = false;
      }
    } catch (Exception ignored) {
      // do nothing
    }
    return model;
  }

  @RequestMapping(value = "/logout", method = RequestMethod.GET)
  public
  @ResponseBody
  AuthModel logout(HttpServletRequest request, HttpServletResponse response) {
    HttpSession session = request.getSession(false);
    if (session != null) {
      SessionMgr.cleanupSession(session);
    }
    //clear cookies
    SecurityUtils.clearTokenCookie(request,response);
    return constructAuthModel(0, null);
  }

  @RequestMapping(value = "/resetpassword/{token:.*}", method = RequestMethod.GET)
  public
  @ResponseBody
  AuthModel resetPassword(@PathVariable String token, @RequestParam String src,
                          HttpServletRequest request, HttpServletResponse response) {

    try {
      String successMsg = resetAndRedirect(token, src, response);
      return new AuthModel(false, successMsg);
    } catch (ObjectNotFoundException e) {
      xLogger.warn("Error updating password for {0}", e);
      return constructAuthModel(USER_NOTFOUND, null);
    } catch (Exception e) {
      xLogger.warn("Error updating password ", e);
      return constructAuthModel(SYSTEM_ERROR, null);
    }
  }

  @RequestMapping(value = "/resetpassword", method = RequestMethod.POST)
  public
  @ResponseBody
  AuthModel generatePassword(@RequestBody PasswordModel model, HttpServletRequest request)
      throws ServiceException,
      MessageHandlingException, IOException {
    if (model != null) {
      String successMsg = authenticationService.resetPassword(model.getUid(), model.getMode(),
          model.getOtp(), WEB,
          request.getParameter("au"));
        return new AuthModel(false, successMsg);
    }

    return null;
  }

  @RequestMapping(value = "/generateOtp", method = RequestMethod.POST)
  public
  @ResponseBody
  AuthModel generateOtp(@RequestBody PasswordModel model, HttpServletRequest request)
      throws ServiceException, IOException, MessageHandlingException {
    if (model != null) {
        //web client is not sending this variable
        if (StringUtils.isEmpty(model.getUdty())) {
          model.setUdty(WEB);
        }
      if (WEB.equals(model.getUdty())) {
        boolean
            captchaVerified = authenticationService
            .verifyCaptcha(model.getCaptcha());
        if (!captchaVerified) {
          throw new BadRequestException("G010", null);
        }
      }
      String successMsg = authenticationService.generateOTP(model.getUid(), model.getMode(), model.getUdty(),
            request.getHeader("host"));
        return new AuthModel(false, successMsg);
    }

    return null;
  }

  @RequestMapping(value = "/changePassword", method = RequestMethod.POST)
  public
  @ResponseBody
  AuthModel changePassword(@RequestBody ChangePasswordModel model) {
    if (model != null) {
      try {
        String successMsg = authenticationService.setNewPassword(model.getKey(), model.getNpd(), model.getCpd());
        return new AuthModel(false, successMsg);
      } catch (ObjectNotFoundException e) {
        xLogger.warn("Error updating password for {0}", model.getUid(), e);
        return constructAuthModel(USER_NOTFOUND, null);
      } catch (InputMismatchException e) {
        xLogger.warn("Mismatch in passwords entered for {0}", model.getUid(), e);
        return constructAuthModel(PASSWORD_MISMATCH, null);
      } catch (ValidationException e) {
        xLogger.warn("Exception: " + e);
        return constructAuthModel(LINK_EXPIRED, null);
      } catch (Exception e) {
        xLogger.severe("Exception: " + e);
        return constructAuthModel(SYSTEM_ERROR, null);
      }
    }

    return null;
  }

  @RequestMapping(value = "/request-access-key", method = RequestMethod.GET)
  public
  @ResponseBody
  String requestAccessKey() {
    return authenticationService.generateAccessKey();
  }

  @RequestMapping(value = "/authorise-access-key", method = RequestMethod.POST)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void authoriseAccessKey(@RequestBody String accessKey)
      throws ServiceException {
    authenticationService.authoriseAccessKey(accessKey);
  }

  @RequestMapping(value = "/check-access-key", method = RequestMethod.POST)
  public
  @ResponseBody
  Map<String, String> checkAccessKeyStatus(@RequestBody String accessKey)
      throws ServiceException {
    Optional<IUserToken>
        optionalToken =
        authenticationService.checkAccessKeyStatus(accessKey);
    Map<String, String> tokenDetails = new HashMap<>(2);
    if (optionalToken.isPresent()) {
      tokenDetails.put(Constants.TOKEN, optionalToken.get().getRawToken());
      tokenDetails.put(Constants.EXPIRES,
          String.valueOf(optionalToken.get().getExpires().getTime()));
    }
    return tokenDetails;

  }

  public String resetAndRedirect(String inputToken, String src,
                                 HttpServletResponse response) throws IOException,
      ServiceException, MessageHandlingException {
    if (StringUtils.isNotEmpty(inputToken)) {
      // last two character defines user role, being used by UI for validation
      String role = inputToken.substring(inputToken.length() - 2);
      String token = inputToken.substring(0, inputToken.length() - 2);
      String[] tokens = authenticationService.decryptJWT(token);
      if (tokens.length > 0) {
        String userId = tokens[0];
        Boolean isWithinTimeLimit = timeDiff(tokens[1]);
        String resetKey = userId + "&&" + tokens[1];
        if (isWithinTimeLimit && resetKey.equals(memcacheService.get("RESET_" + userId))) {
          IUserAccount account = usersService.getUserAccount(userId);
          Locale locale = account.getLocale();
          ResourceBundle backendMessages = Resources.get().getBundle("BackendMessages", locale);
          if (WEB.equalsIgnoreCase(src)) {
            response.sendRedirect(
                "/v2/password-request.html?key=" + token + role + "&user=" + userId);
          } else {
            sendPasswordInEmail(response, userId, account, backendMessages);
          }
          xLogger.info("AUDITLOG\t{0}\t{1}\tUSER\t " +
                  "FORGOT PASSWORD\t{2}\t{3}", account.getDomainId(), account.getFullName(),
              userId,
              account.getFullName());
          return backendMessages.getString("password.forgot.success") + " " + account
              .getFirstName() + "'s " + backendMessages.getString("password.mid") + ". " +
              MsgUtil.bold(backendMessages.getString("note") + ":") + " " + backendMessages
              .getString("password.login") + ".";
        } else {
          response.sendRedirect("/v2/password-reset-error.html#/");
        }
      }
    }
    return null;
  }

  private void sendPasswordInEmail(HttpServletResponse response, String userId,
                                     IUserAccount account, ResourceBundle backendMessages)
      throws ServiceException, MessageHandlingException, IOException {
    String newPassword;
    String msg;
    String logMsg;
    String sendType;
    newPassword = RandomPasswordGenerator.generate(
        SecurityUtil.isUserAdmin(account.getRole()));
    msg = backendMessages.getString("password.reset.success") + ": " + newPassword;
    logMsg = backendMessages.getString("password.reset.success.log");
    sendType = "email";
    authenticationService.changePassword(userId, null, null, newPassword,false);
    MessageService
        ms =
        MessageService
            .getInstance(sendType, account.getCountry(), true, account.getDomainId(),
                account.getFirstName(), null);
    ms.send(account, msg, MessageService.NORMAL,
        backendMessages.getString("password.reset.success"), null, logMsg);
    response.sendRedirect("/v2/mobile-pwd-reset-success.html#/");
    memcacheService.delete("RESET_" + userId);
  }

  /**
   * Calculate the difference b/w received token's milliseconds and current system time's milliseconds
   */
  private Boolean timeDiff(String tokenTime) {
    if (tokenTime != null) {
      Long tmillis = Long.valueOf(tokenTime);
      Long cmilli = System.currentTimeMillis();
      long diff = cmilli - tmillis;
      if (diff < 86400000) {
        return true;
      }
    }
    return false;
  }

}
