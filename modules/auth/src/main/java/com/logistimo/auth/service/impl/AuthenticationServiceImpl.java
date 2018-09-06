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

package com.logistimo.auth.service.impl;

import com.logistimo.AppFactory;
import com.logistimo.auth.SecurityUtil;
import com.logistimo.auth.action.AuditAction;
import com.logistimo.auth.command.VerifyCaptchaCommand;
import com.logistimo.auth.service.AuthProvider;
import com.logistimo.auth.service.AuthenticationService;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.communications.MessageHandlingException;
import com.logistimo.communications.service.EmailService;
import com.logistimo.communications.service.MessageService;
import com.logistimo.config.models.BBoardConfig;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.constants.CharacterConstants;
import com.logistimo.constants.Constants;
import com.logistimo.constants.PropertyConstants;
import com.logistimo.constants.SourceConstants;
import com.logistimo.dao.JDOUtils;
import com.logistimo.events.entity.IEvent;
import com.logistimo.events.processor.EventPublisher;
import com.logistimo.exception.InvalidDataException;
import com.logistimo.exception.SystemException;
import com.logistimo.exception.TaskSchedulingException;
import com.logistimo.exception.UnauthorizedException;
import com.logistimo.exception.ValidationException;
import com.logistimo.logger.XLog;
import com.logistimo.models.AuthRequest;
import com.logistimo.models.users.LoginStatus;
import com.logistimo.models.users.UserDetails;
import com.logistimo.security.BadCredentialsException;
import com.logistimo.security.UserDisabledException;
import com.logistimo.services.ObjectNotFoundException;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;
import com.logistimo.services.cache.MemcacheService;
import com.logistimo.services.impl.PMF;
import com.logistimo.services.taskqueue.ITaskService;
import com.logistimo.services.utils.ConfigUtil;
import com.logistimo.twofactorauthentication.entity.UserDevices;
import com.logistimo.twofactorauthentication.service.TwoFactorAuthenticationService;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.users.entity.IUserToken;
import com.logistimo.users.entity.UserAccount;
import com.logistimo.users.service.UsersService;
import com.logistimo.utils.PasswordEncoder;
import com.logistimo.utils.RandomPasswordGenerator;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwe.KeyManagementAlgorithmIdentifiers;
import org.jose4j.keys.AesKey;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.security.Key;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.UUID;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

  private static final XLog xLogger = XLog.getLog(AuthenticationServiceImpl.class);
  private static final String UPDATE_LAST_ACCESSED_TASK = "/s2/api/users/update/mobileaccessed";
  private static final String ACCESS_KEY_PREFIX = "ACCESSKEY_";
  private static final String DOMAIN_KEY_SEPARATOR = "_";
  private static final String TOKEN_ACCESS_PREFIX = "at_";
  private static final String BACKEND_MESSAGES = "BackendMessages";
  private static final String RESET_PREFIX = "RESET_";
  private static final String NEW_LINE_HTML = "<br><br>";
  private static ITaskService taskService = AppFactory.get().getTaskService();
  private static final int
      WEB_TOKEN_INACTIVITY_MILLIS =
      ConfigUtil.getInt(PropertyConstants.TOKEN_EXPIRE_WEB, 30) * 60_000;
  private static final String TWO_FACTOR_AUTHENTICATION_OTP = "Auth_OTP";
  private static final String OTP = "OTP";
  private static final String JWTKEY = "jwt.key";

  private MemcacheService memcacheService;

  private UsersService usersService;

  @Autowired
  public AuthProvider authProvider;

  @Autowired
  public AuditAction auditAction;

  private TwoFactorAuthenticationService twoFactorAuthenticationService;

  @Autowired
  public void setCacheService(MemcacheService memcacheService) {
    this.memcacheService = memcacheService;
  }

  @Autowired
  public void setUsersService(UsersService usersService) {
    this.usersService = usersService;
  }

  @Autowired
  @Qualifier("authenticationRestTemplate")
  private RestTemplate restTemplate;

  @Autowired
  public void setTwoFactorAuthenticationService(TwoFactorAuthenticationService twoFactorAuthenticationService) {
    this.twoFactorAuthenticationService = twoFactorAuthenticationService;
  }

  public IUserToken generateUserToken(String userId, Integer source) throws ServiceException {
    return generateUserToken(userId, null, null, source);
  }

  public IUserToken generateUserToken(String userId, String accessKey, Long domainId,
                                      Integer source)
      throws ServiceException {
    xLogger.fine("Entering generateUserToken");

    if (StringUtils.isEmpty(userId)) {
      throw new ServiceException("User id is null or empty.");
    }
    IUserAccount account = usersService.getUserAccount(userId);
    IUserToken iUserToken;
    String token;
    PersistenceManager pm = PMF.get().getPersistenceManager();
    try {
      if (StringUtils.isEmpty(accessKey)) {
        //Clear previous user sessions on mobile and web.
        updateUserSession(userId, null);
      }
      token = generateUuid();
      iUserToken = JDOUtils.createInstance(IUserToken.class);

      int validityInMinutes = getTokenValidityInMinutes(account, accessKey, source);
      if (validityInMinutes > 0) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MINUTE, validityInMinutes);
        iUserToken.setExpires(c.getTime());
      } else {
        updateWebAccessTime(token);
      }
      iUserToken.setUserId(userId);
      iUserToken.setToken(PasswordEncoder.MD5(token));
      iUserToken.setRawToken(token);
      iUserToken.setDomainId(domainId != null ? domainId : account.getDomainId());
      iUserToken.setAccessKey(accessKey);
      pm.makePersistent(iUserToken);
      xLogger.fine("generateUserToken: userId is {0}, token is {1}, expires is {2}", userId,
          iUserToken.getToken(), iUserToken.getExpires());
      return iUserToken;
    } catch (Exception e) {
      xLogger.severe("Failed to encode password for user : {0}", userId, e);
      throw new ServiceException("G001", new Object[]{});
    } finally {
      pm.close();
    }
  }

  private int getTokenValidityInMinutes(IUserAccount account, String isAccessKey, Integer source) {
    int validityTimeInMinutes = ConfigUtil.getInt(PropertyConstants.TOKEN_EXPIRY, 30 * 1440);

    if (StringUtils.isEmpty(isAccessKey)) {
      // Web app limit token access time
      if (SourceConstants.WEB.equals(source)) {
        validityTimeInMinutes = -1;
      } else {
        //Mobile apps pick configuration
        if (account.getAuthenticationTokenExpiry() != 0) {
          validityTimeInMinutes = account.getAuthenticationTokenExpiry() * 1440;
        } else {
          DomainConfig domainConfig = DomainConfig.getInstance(account.getDomainId());
          if (domainConfig.getAuthenticationTokenExpiry() != 0) {
            validityTimeInMinutes = domainConfig.getAuthenticationTokenExpiry() * 1440;
          }
        }
      }
    } else {
      //Bulletin board, use domain default
      DomainConfig domainConfig = DomainConfig.getInstance(account.getDomainId());
      BBoardConfig bBoardConfig = domainConfig.getBBoardConfig();
      validityTimeInMinutes = bBoardConfig.getExpiry() * 1440;
    }

    return validityTimeInMinutes;
  }

  public IUserToken authenticateToken(String token, Integer accessInitiator) throws UnauthorizedException {
    if (StringUtils.isEmpty(token)) {
      throw new UnauthorizedException("Token is empty or null");
    }
    IUserToken iUserToken;
    PersistenceManager pm = PMF.get().getPersistenceManager();
    try {
      iUserToken = JDOUtils.getObjectById(IUserToken.class, PasswordEncoder.MD5(token), pm);
    } catch (JDOObjectNotFoundException e) {
      throw new UnauthorizedException("Invalid token");
    } finally {
      pm.close();
    }

    if (iUserToken != null) {
      if(SourceConstants.BULLETIN_BOARD != accessInitiator.intValue() && iUserToken.hasAccessKey()) {
        throw new UnauthorizedException("G002", new Object[]{});
      }
      if (iUserToken.getExpires() == null) {
        checkWebTokenExpiry(token);
        updateWebAccessTime(token);
      } else {
        checkTokenExpiry(iUserToken);
        updateLastAccessTime(accessInitiator, iUserToken);
      }
      return iUserToken;
    } else {
      throw new UnauthorizedException("Invalid Token " + token);
    }
  }

  private void updateWebAccessTime(String token) {
    memcacheService.put(TOKEN_ACCESS_PREFIX + token, System.currentTimeMillis());
  }

  private void updateLastAccessTime(Integer accessInitiator, IUserToken iUserToken) {
    if (!Objects.equals(accessInitiator, Constants.LAST_ACCESSED_BY_SYSTEM)) {
      String userId = iUserToken.getUserId();
      Map<String, String> params = new HashMap<>(2);
      params.put("userId", userId);
      params.put("aTime", String.valueOf(System.currentTimeMillis()));
      try {
        taskService.schedule(ITaskService.QUEUE_DEFAULT, UPDATE_LAST_ACCESSED_TASK, params,
            ITaskService.METHOD_POST);
      } catch (TaskSchedulingException e) {
        xLogger
            .warn("Failed to update the last mobile accessed time for user : {0} ", userId, e);
      }
    }
  }

  private void checkTokenExpiry(IUserToken iUserToken) {
    Date expires = iUserToken.getExpires();
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(expires);
    if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
      throw new UnauthorizedException("Token expired");
    }
  }

  private void checkWebTokenExpiry(String token) {
    Long lastAccessTime = (Long) memcacheService.get(TOKEN_ACCESS_PREFIX + token);
    if (lastAccessTime == null
        || System.currentTimeMillis() > lastAccessTime + WEB_TOKEN_INACTIVITY_MILLIS) {
      throw new UnauthorizedException("Token expired");
    }
  }

  public String getUserIdByToken(String token) {
    IUserToken iUserToken;
    PersistenceManager pm = PMF.get().getPersistenceManager();
    try {
      iUserToken = JDOUtils.getObjectById(IUserToken.class, PasswordEncoder.MD5(token), pm);
      return iUserToken.getUserId();
    } catch (Exception e) {
      xLogger.warn("Exception while getting User ID using token", e);
    } finally {
      pm.close();
    }
    return null;
  }

  public Boolean clearUserTokens(String userId, boolean removeAccessKeyTokens) {
    PersistenceManager pm = null;
    Query q = null;
    if (StringUtils.isNotEmpty(userId)) {
      String
          queryStr =
          "SELECT FROM " + JDOUtils.getImplClass(IUserToken.class).getName()
              + " WHERE userId == uIdParam "
              + (removeAccessKeyTokens ? "" : " && accessKey == null")
              + " PARAMETERS String uIdParam";
      try {
        pm = PMF.get().getPersistenceManager();
        q = pm.newQuery(queryStr);
        List<IUserToken> tokensList = (List<IUserToken>) q.execute(userId);
        if (CollectionUtils.isNotEmpty(tokensList)) {
          pm.deletePersistentAll(tokensList);
          return true;
        }
      } catch (JDOObjectNotFoundException ignored) {
        //do nothing
      } finally {
        if (q != null) {
          try {
            q.closeAll();
          } catch (Exception ignored) {
            xLogger.warn("Exception while closing query", ignored);
          }
        }
        if (pm != null) {
          pm.close();
        }
      }
    }
    return false;
  }

  @Override
  public String getUserToken(String userId) {
    if (StringUtils.isNotEmpty(userId)) {
      PersistenceManager pm = null;
      Query q = null;
      try {
        pm = PMF.get().getPersistenceManager();
        q = pm.newQuery(JDOUtils.getImplClass(IUserToken.class));
        q.setResult("token");
        q.setFilter("userId == ?");
        q.setUnique(true);
        return (String) q.execute(userId);
      } catch (JDOObjectNotFoundException ignored) {
        //do nothing
      } finally {
        if (q != null) {
          try {
            q.closeAll();
          } catch (Exception ignored) {
            xLogger.warn("Exception while closing query", ignored);
          }
        }
        if (pm != null) {
          pm.close();
        }
      }
    }
    return null;
  }

  @Override
  public void updateUserSession(String userId, String sessionId) {
    clearUserTokens(userId, false);
    MemcacheService cacheService = AppFactory.get().getMemcacheService();
    String currentSession = (String) cacheService.get(Constants.USER_SESS_PREFIX + userId);
    if (currentSession != null) {
      cacheService.delete(currentSession);
    }
    if (sessionId != null) {
      cacheService.put(Constants.USER_SESS_PREFIX + userId, sessionId);
    } else {
      cacheService.delete(Constants.USER_SESS_PREFIX + userId);
    }
    xLogger.info("New user session: {1} created for user {0}, old session:{2}", userId, sessionId,
        currentSession);
  }

  /**
   * @param userId
   * @param mode
   * @return
   * @throws MessageHandlingException
   * @throws IOException
   * @throws ServiceException
   * @throws ObjectNotFoundException
   * @throws InvalidDataException
   */
  @Override
  public String generateOTP(String userId, int mode, String src, String hostUri)
      throws MessageHandlingException, IOException, ServiceException {
    IUserAccount account = usersService.getUserAccount(userId);
    if (!account.isEnabled()) {
      throw new ObjectNotFoundException("USR001", userId);
    }
    Locale locale = account.getLocale();
    ResourceBundle backendMessages = Resources.get().getBundle(BACKEND_MESSAGES, locale);
    MemcacheService cache = AppFactory.get().getMemcacheService();
    if (mode == 1) {
      String email = account.getEmail();
      if (StringUtils.isNotEmpty(email)) {
        List<String> mails = new ArrayList<>(1);
        String resetUrl = null;
        mails.add(email);
        Long currentMilli = System.currentTimeMillis();
        String otp = createJWT(userId, currentMilli);
        String resetKey = userId + "&&" + currentMilli;
        trackResetKey(userId, resetKey);
        if (src.equalsIgnoreCase(Constants.WEB)) {
          resetUrl =
              hostUri.concat("/s2/api/auth/resetpassword/") + otp + account.getRole().substring(5)
                  .concat("?src=").concat(Constants.WEB);
        } else if (src.equalsIgnoreCase("m")) {
          resetUrl =
              hostUri.concat("/s2/api/auth/resetpassword/") + otp + account.getRole().substring(5)
                  .concat("?src=m");
        }

        String
            msg =
            backendMessages.getString("password.reset.info.user.name") + " " + account.getFullName()
                + "," + NEW_LINE_HTML +
                backendMessages.getString("password.reset.info.req.msg") + NEW_LINE_HTML +
                backendMessages.getString("password.reset.info.user.id") + ": " + account
                .getFirstName() + NEW_LINE_HTML +
                "<a href=http://" + resetUrl + ">" + backendMessages.getString("password.reset.now")
                + "</a>" + NEW_LINE_HTML +
                backendMessages.getString("password.reset.expiry") + NEW_LINE_HTML +
                backendMessages.getString("password.reset.retain") + NEW_LINE_HTML +
                backendMessages.getString("password.reset.confidentiality.notice");
        EmailService svc = EmailService.getInstance();
        svc.sendHTML(account.getDomainId(), mails, backendMessages.getString("password.reset"), msg,
            null);
        return backendMessages.getString("password.link.success1") + " " + account.getFirstName()
            + backendMessages.getString("password.link.success2");
      } else {
        xLogger.warn("The user " + userId + " has not registered the selected send type " + mode);
        throw new InvalidDataException("No email Id available.");
      }
    } else {
      generateResetPasswordOTP(userId, cache);
      return backendMessages.getString("password.otp.success1") + " " + account.getFirstName()
          + backendMessages.getString("password.otp.success2");
    }
  }

  @Override
  public String generate2FAOTP(String userId)
      throws MessageHandlingException, IOException {
    String msg, logMsg, otp;
    MemcacheService cache = AppFactory.get().getMemcacheService();

    IUserAccount userAccount = usersService.getUserAccount(userId);
    ResourceBundle
        backendMessages =
        Resources.get().getBundle(BACKEND_MESSAGES, userAccount.getLocale());

    String cacheKey = TWO_FACTOR_AUTHENTICATION_OTP.concat(CharacterConstants.UNDERSCORE).concat(userId);
    if(cache != null && cache.get(cacheKey) != null) {
      otp = (String) cache.get(cacheKey);
    } else {
      otp = generateMobileOTP();
      if (cache != null) {
        cache
            .put(cacheKey, otp, 15*60);
      }
    }
    msg = otp.concat(CharacterConstants.SPACE).concat(backendMessages.getString("otp.generation.msg"));
    logMsg =
        backendMessages.getString("otp.generation.success").concat(CharacterConstants.SPACE)
            .concat(userId);
    sendOTP(logMsg, msg, userAccount, backendMessages);
    return backendMessages.getString("password.otp.success1") + " " + userAccount.getFirstName()
        + backendMessages.getString("password.otp.success2");
  }

  private void generateResetPasswordOTP(String userId, MemcacheService cache)
      throws MessageHandlingException, IOException {
    if (cache == null) {
      cache = AppFactory.get().getMemcacheService();
    }
    IUserAccount userAccount = usersService.getUserAccount(userId);
    ResourceBundle
        backendMessages =
        Resources.get().getBundle(BACKEND_MESSAGES, userAccount.getLocale());

    String otp = generateMobileOTP();
    if (cache != null) {
      cache.put(OTP.concat(CharacterConstants.UNDERSCORE).concat(userId), otp, 86400);
    }
    String msg =
        backendMessages.getString("password.otp.info") + " " + otp + " " + backendMessages
            .getString("password.otp.validity");
    String logMsg =
        backendMessages.getString("password.otp.success1") + " " + userAccount.getFirstName()
            + backendMessages.getString("password.otp.success2");
    sendOTP(logMsg, msg, userAccount, backendMessages);
  }

  private String sendOTP(String logMsg, String msg, IUserAccount userAccount,
                         ResourceBundle backendMessages)
      throws MessageHandlingException, IOException {
    MessageService
        ms =
        MessageService
            .getInstance("sms", userAccount.getCountry(), true, userAccount.getDomainId(),
                userAccount.getFirstName(),
                null);
    ms.send(userAccount, msg, MessageService.NORMAL, backendMessages.getString("password.updated"),
        null, logMsg);
    return msg;
  }

  private String generateMobileOTP() {
    long randomPIN = new Random().longs(100000, 999999).findAny().getAsLong();
    return String.valueOf(randomPIN);
  }


  /**
   * Reset password validating the OTP recieved
   */
  @Override
  public String resetPassword(String userId, int mode, String otp, String src,
                              String au)
      throws ServiceException, MessageHandlingException, IOException{
    IUserAccount account = usersService.getUserAccount(userId);
    Locale locale = account.getLocale();
    ResourceBundle backendMessages = Resources.get().getBundle(BACKEND_MESSAGES, locale);
    String sendType = null;
    String sendMode = null;
    if (mode == 0) {
      boolean checkOtp = true;
      if ("m".equalsIgnoreCase(src) && StringUtils.isEmpty(au)) {
          checkOtp = false;
          sendType = "sms";
      }
      if (checkOtp) {
        validateOtpMMode(userId, otp);
        sendType = "sms";
        sendMode = backendMessages.getString("password.ph");
      }

    } else {
      String email = account.getEmail();
      if (StringUtils.isNotEmpty(email)) {
        sendType = "email";
        sendMode = backendMessages.getString("password.mid");
      } else {
        xLogger.warn("The user " + userId + " has not registered the selected send type " + userId);
        throw new InvalidDataException("No email Id available.");
      }
    }
    String newPassword = RandomPasswordGenerator.generate(
        SecurityUtil.isUserAdmin(account.getRole()));
    changePassword(userId, null,null, newPassword,false);
    xLogger.info("AUDITLOG\t{0}\t{1}\tUSER\t " +
            "FORGOT PASSWORD\t{2}\t{3}", account.getDomainId(), account.getFullName(), userId,
        account.getFullName());
    MessageService
        ms =
        MessageService.getInstance(sendType, account.getCountry(), true, account.getDomainId(),
            account.getFirstName(), null);
    ms.send(account, backendMessages.getString("password.reset.success") + " " + newPassword,
        MessageService.NORMAL, backendMessages.getString("password.reseted"), null,
        backendMessages.getString("password.reset.success.log"));
    return backendMessages.getString("password.forgot.success") + " " + account.getFirstName()
        + "'s " + sendMode + ". " + backendMessages.getString("password.login");

  }

  /**
   * Validate otp if chosen mode is mobile
   */

  public void validateOtpMMode(String userId, String otp) {
    validateOtpMMode(userId, otp, false);
  }


  /**
   * Validate otp if chosen mode is mobile for two factor authentication otp
   */
  @Override
  public void validateOtpMMode(String userId, String otp, boolean isTwoFactorAuthenticationOTP) {

    MemcacheService cache = AppFactory.get().getMemcacheService();
    String cacheOTP;
    String cacheKey;
    if (isTwoFactorAuthenticationOTP) {
      cacheKey = TWO_FACTOR_AUTHENTICATION_OTP.concat(CharacterConstants.UNDERSCORE).concat(userId);
      cacheOTP = String.valueOf(cache.get(cacheKey));
    } else {
      cacheKey = OTP.concat(CharacterConstants.UNDERSCORE).concat(userId);
      cacheOTP = String.valueOf(cache.get(cacheKey));
    }

    if (cacheOTP == null) {
      xLogger.warn("OTP expired or already used to generate new otp for  " + userId);
      throw new InputMismatchException("OTP not valid");
    }

    if (otp.equals(cacheOTP)) {
      cache.delete(cacheKey);
    } else {
      xLogger.warn("Wrong OTP entered for  " + userId);
      throw new ValidationException("UA002", userId);
    }
  }

  @Override
  public String generateAccessKey() {
    String accessKey;
    try {
      accessKey = UUID.randomUUID().toString().substring(0, 8);
      memcacheService.put(ACCESS_KEY_PREFIX + accessKey, "",
          ConfigUtil.getInt("access.key.request.expiry.minutes", 30) * 60);
    } catch (Exception e) {
      throw new SystemException(e, "G001");
    }
    return accessKey;
  }

  @Override
  public Optional<IUserToken> checkAccessKeyStatus(String accessKey) throws ServiceException {
    String value = (String) memcacheService.get(ACCESS_KEY_PREFIX + accessKey);
    if (value == null) {
      throw new ValidationException("A001", accessKey);
    } else if (StringUtils.isNotEmpty(value)) {
      int domainKeySeparatorIndex = value.indexOf(DOMAIN_KEY_SEPARATOR);
      return Optional.of(generateUserToken(
          value.substring(domainKeySeparatorIndex + 1, value.length()), accessKey,
          Long.valueOf(value.substring(0, domainKeySeparatorIndex)),
          SourceConstants.BULLETIN_BOARD));
    } else {
      return Optional.empty();
    }
  }

  @Override
  public void authoriseAccessKey(String accessKey) throws ServiceException {
    if (SecurityUtils.isAdmin()) {
      String date = (String) memcacheService.get(ACCESS_KEY_PREFIX + accessKey);
      if (date == null) {
        throw new ValidationException("A001", accessKey);
      }
      memcacheService.put(ACCESS_KEY_PREFIX + accessKey,
          SecurityUtils.getCurrentDomainId() + DOMAIN_KEY_SEPARATOR
              + SecurityUtils.getUsername(),
          ConfigUtil.getInt("access.key.authorisation.validity.minutes", 30) * 60);
      return;
    }
    throw new UnauthorizedException("G003", HttpStatus.FORBIDDEN, new Object[]{});
  }

  private String generateUuid() {
    return UUID.randomUUID().toString().replaceAll("-", "");
  }

  /**
   * Encrypt the otp to be send via mail
   */
  @Override
  public String createJWT(String userid, long ttlMillis) {
    Key key = new AesKey(ConfigUtil.get(JWTKEY).getBytes());
    JsonWebEncryption jwe = new JsonWebEncryption();
    jwe.setKey(key);
    jwe.setAlgorithmHeaderValue(KeyManagementAlgorithmIdentifiers.A128KW);
    jwe.setEncryptionMethodHeaderParameter(
        ContentEncryptionAlgorithmIdentifiers.AES_128_CBC_HMAC_SHA_256);
    jwe.setPayload(userid + "&&" + ttlMillis);
    try {
      return jwe.getCompactSerialization();
    } catch (JoseException e) {
      xLogger.warn("Unable to get the jwt service: {0}", e.getMessage());
    }
    return null;
  }

  /**
   * Decrypt the otp received via mail
   */
  @Override
  public String[] decryptJWT(String token) {
    String[] tokens = new String[0];

    JsonWebEncryption jwe = new JsonWebEncryption();
    Key key = new AesKey(ConfigUtil.get(JWTKEY).getBytes());
    jwe.setKey(key);
    jwe.setAlgorithmHeaderValue(KeyManagementAlgorithmIdentifiers.A128KW);
    jwe.setEncryptionMethodHeaderParameter(
        ContentEncryptionAlgorithmIdentifiers.AES_128_CBC_HMAC_SHA_256);
    try {
      jwe.setCompactSerialization(token);
      String decryptedToken = jwe.getPayload();
      if (decryptedToken != null) {
        tokens = decryptedToken.split("&&");
      }
      return tokens;
    } catch (JoseException e) {
      xLogger.warn("Unable to get the jwt service: {0}", e.getMessage());
    }
    return tokens;
  }


  @Override
  public String setNewPassword(String token, String newPassword, String confirmPassword)
      throws ServiceException {
    if (StringUtils.isNotEmpty(token)) {
      String[] tokens = decryptJWT(token);
      if (tokens.length > 0) {
        String userId = tokens[0];
        String resetKey = userId + "&&" + tokens[1];
        if (resetKeyMatches(userId, resetKey)) {
          return validateAndChangePassword(newPassword, confirmPassword, userId);
        } else {
          throw new ValidationException("G015");
        }
      }
    }
    return null;
  }

  private String validateAndChangePassword(String newPassword, String confirmPassword,
                                           String userId) throws ServiceException {
    if (StringUtils.isNotEmpty(newPassword) && StringUtils.isNotEmpty(confirmPassword)) {
      if (newPassword.equals(confirmPassword)) {
        changePassword(userId, null, null, newPassword, true);
        clearResetKey(userId);
        ResourceBundle backendMessages = Resources.get().getBundle(BACKEND_MESSAGES,
            Locale.ENGLISH);
        return backendMessages.getString("pwd.forgot.success");
      } else {
        throw new InputMismatchException("Password mismatch");
      }
    }
    return null;
  }


  @Override
  public String generatePassword(String id) {
    if (id == null || id.isEmpty()) {
      return null;
    }
    String password;
    // Take the first 4 letters of id
    if (id.length() <= 4) {
      password = id;
    } else {
      password = id.substring(0, 4);
    }
    // Generate a 3 digit random number
    Random r = new Random();
    r.setSeed(System.currentTimeMillis());
    float f = r.nextFloat();
    password += String.valueOf((int) ((f * 1000.0f) % 1000));
    return password;
  }

  @Override
  public UserDetails authenticate(AuthRequest authRequest) throws ServiceException {
    String userId = authRequest.getUserId();
    String password = authRequest.getPassword();
    Integer lgSrc = authRequest.getLoginSource();
    xLogger.info("Entering authentication for user {0}", userId);
    try {
      UserDetails details = authProvider.authenticate(userId, password);
      IUserToken token = generateUserToken(userId, lgSrc);
      if (token != null) {
        details.setToken(token.getRawToken());
        if (token.getExpires() != null) {
          details.setTokenExpiry(String.valueOf(token.getExpires().getTime()));
        }
      }
      //audit login request only when it is from external source
      if (StringUtils.isNotBlank(authRequest.getIpAddress())) {
        authRequest.setPrevUserAgent(details.getPrevUsrAgnt());
        authRequest.setLoginTime(details.getLastLogin());
        auditAction.invoke(authRequest, LoginStatus.SUCCESS);
        generateSystemEvent(usersService.getUserAccount(userId));
      }
      removeUserFromCache(userId);
      return details;
    } catch (BadCredentialsException e) {
      auditAction.invoke(authRequest, LoginStatus.INVALID_CREDENTIALS);
      throw e;
    } catch (UserDisabledException e) {
      auditAction.invoke(authRequest, LoginStatus.DISABLED);
      throw e;
    }
  }

  private void generateSystemEvent(IUserAccount user) {
    Map<String, Object> params = new HashMap<>(1);
    params.put("ipaddress", user.getIPAddress());
    try {
      EventPublisher.generate(user.getDomainId(), IEvent.IP_ADDRESS_MATCHED, params,
          UserAccount.class.getName(), user.getKeyString(),
          null);
    } catch (Exception e) {
      xLogger.warn("Exception encountered during system generation for login for user {0}",user.getUserId());
    }
  }

  @Override
  public void changePassword(String userId, String otp, String oldPassword, String newPassword, boolean isEnhanced) throws ServiceException {
    xLogger.info("Entering change password for user {0}",userId);
    if (StringUtils.isNotEmpty(otp)) {
      validateOtpMMode(userId, otp);
    }
    authProvider.changePassword(userId,oldPassword,newPassword,isEnhanced);
  }

  @Override
  public void enableAccount(String userId) throws ServiceException {
    authProvider.enableAccount(userId);
    removeUserFromCache(userId);
  }

  @Override
  public void disableAccount(String userId) throws ServiceException {
    authProvider.disableAccount(userId);
    removeUserFromCache(userId);
  }

  @Override
  public void removeUserFromCache(String userId) {
    memcacheService.delete(Constants.USER_KEY + CharacterConstants.HASH + userId);
  }

  @Override
  public String getUserSalt(String userId) {
    return authProvider.getUserSalt(userId);
  }

  @Override
  public String generateRandomSalt() {
    return SecurityUtils.salt();
  }

  @Override
  public void logForbiddenAccess(AuthRequest authRequest) {
    auditAction.invoke(authRequest, LoginStatus.FORBIDDEN);
  }

  public boolean verifyCaptcha(String captchaResponse) {
    if(StringUtils.isNotEmpty(captchaResponse)) {
      return new VerifyCaptchaCommand(restTemplate, captchaResponse).execute();
    }
    return false;
  }

  private void trackResetKey(String userId, String resetKey) {
    memcacheService.put(RESET_PREFIX + userId, resetKey, 86400);
  }

  private void clearResetKey(String userId) {
    memcacheService.delete(RESET_PREFIX + userId);
  }

  private boolean resetKeyMatches(String userId, String resetKey) {
    return resetKey.equals(memcacheService.get(RESET_PREFIX + userId));
  }

  @Override
  public boolean isUserDeviceAuthenticated(String deviceKey, String userId,
                                           Long domainId)
      throws IOException {
    DomainConfig domainConfig = DomainConfig.getInstance(domainId);

    if(!domainConfig.isTwoFactorAuthenticationEnabled()) {
      return true;
    } else if(StringUtils.isNotBlank(deviceKey)) {
      UserDevices
          userDevices =
          twoFactorAuthenticationService.getDeviceInformation(deviceKey, userId);
      if(userDevices != null && userDevices.getExpiresOn().getTime() > System.currentTimeMillis()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean authenticateUserByCredentials(String userId, String deviceKey, Integer loginSource,
                                               String otp, boolean isTwoFactorAuthenticationOTP)
      throws IOException, MessageHandlingException {
    IUserAccount user = usersService.getUserAccount(userId);
    if (StringUtils.isEmpty(otp)) {
      boolean isDeviceRegistered = isUserDeviceAuthenticated(deviceKey, userId, user.getDomainId());
      if (!isDeviceRegistered) {
        generate2FAOTP(user.getUserId());
        return false;
      }
    } else {
      validateOtpMMode(userId, otp, isTwoFactorAuthenticationOTP);
    }
    return true;
  }

}
