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

import com.logistimo.api.builders.EntityBuilder;
import com.logistimo.api.builders.UserBuilder;
import com.logistimo.api.builders.UserMessageBuilder;
import com.logistimo.api.models.AddRemoveAccDomainsResponseObj;
import com.logistimo.api.models.ChangePasswordModel;
import com.logistimo.api.models.UserMessageModel;
import com.logistimo.api.models.UserModel;
import com.logistimo.api.models.configuration.AdminContactConfigModel;
import com.logistimo.api.request.AddAccDomainsRequestObj;
import com.logistimo.api.request.UserFilterRequestObj;
import com.logistimo.api.util.UserMessageUtil;
import com.logistimo.auth.GenericAuthoriser;
import com.logistimo.auth.SecurityConstants;
import com.logistimo.auth.SecurityUtil;
import com.logistimo.auth.service.AuthenticationService;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.communications.MessageHandlingException;
import com.logistimo.communications.service.MessageService;
import com.logistimo.config.entity.IConfig;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.config.service.ConfigurationMgmtService;
import com.logistimo.constants.Constants;
import com.logistimo.dao.JDOUtils;
import com.logistimo.entities.entity.IKiosk;
import com.logistimo.entities.models.UserEntitiesModel;
import com.logistimo.entities.service.EntitiesService;
import com.logistimo.entity.IMessageLog;
import com.logistimo.exception.BadRequestException;
import com.logistimo.exception.ForbiddenAccessException;
import com.logistimo.exception.InvalidDataException;
import com.logistimo.exception.InvalidServiceException;
import com.logistimo.exception.SystemException;
import com.logistimo.logger.XLog;
import com.logistimo.models.AuthRequest;
import com.logistimo.models.ICounter;
import com.logistimo.models.superdomains.DomainSuggestionModel;
import com.logistimo.models.users.UserLoginHistoryModel;
import com.logistimo.pagination.Navigator;
import com.logistimo.pagination.PageParams;
import com.logistimo.pagination.Results;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.ObjectNotFoundException;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;
import com.logistimo.services.impl.PMF;
import com.logistimo.users.UserUtils;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.users.entity.IUserLoginHistory;
import com.logistimo.users.service.UsersService;
import com.logistimo.utils.Counter;
import com.logistimo.utils.LocalDateUtil;
import com.logistimo.utils.MessageUtil;
import com.logistimo.utils.MsgUtil;
import com.logistimo.utils.RandomPasswordGenerator;
import com.logistimo.utils.StringUtil;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TimeZone;

import javax.jdo.PersistenceManager;
import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/users")
public class UsersController {
  private static final XLog xLogger = XLog.getLog(UsersController.class);
  private static final String ACTIVE_USERS = "au";
  private UserBuilder userBuilder;
  private UserMessageBuilder messageBuilder;
  private UsersService usersService;
  private AuthenticationService authenticationService;
  private EntitiesService entitiesService;
  private ConfigurationMgmtService configurationMgmtService;
  private EntityBuilder entityBuilder;

  @Autowired
  public void setUserBuilder(UserBuilder userBuilder) {
    this.userBuilder = userBuilder;
  }

  @Autowired
  public void setMessageBuilder(UserMessageBuilder messageBuilder) {
    this.messageBuilder = messageBuilder;
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
  public void setEntitiesService(EntitiesService entitiesService) {
    this.entitiesService = entitiesService;
  }

  @Autowired
  public void setConfigurationMgmtService(ConfigurationMgmtService configurationMgmtService) {
    this.configurationMgmtService = configurationMgmtService;
  }

  @Autowired
  public void setEntityBuilder(EntityBuilder entityBuilder) {
    this.entityBuilder = entityBuilder;
  }

  private Results getUsers(String q, HttpServletRequest request, int offset, int size,
                           boolean isSuggest, boolean activeUsersOnly, boolean includeSuperusers,
                           boolean includeChildDomainUsers) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    Long domainId = sUser.getCurrentDomainId();
    Results results;
    try {
      IUserAccount loggedInUser = usersService.getUserAccount(sUser.getUsername());
      Navigator
          navigator =
          new Navigator(request.getSession(), "UsersController.getUsers", offset, size, "dummy", 0);
      PageParams pageParams = new PageParams(navigator.getCursor(offset), offset, size);
      results =
          usersService.getUsers(domainId, loggedInUser, activeUsersOnly, includeSuperusers, q, pageParams,
              includeChildDomainUsers);
      navigator.setResultParams(results);
      if (SecurityUtil.compareRoles(sUser.getRole(), SecurityConstants.ROLE_DOMAINOWNER) >= 0
          && StringUtils.isBlank(q)) {
        ICounter counter = Counter.getUserCounter(domainId);
        results.setNumFound(counter.getCount());
      } else {
        results.setNumFound(-1);
      }
    } catch (ObjectNotFoundException e) {
      xLogger.warn("Unable to fetch logged in sUser details", e);
      throw new InvalidServiceException(backendMessages.getString("users.logged.fetch"));
    } catch (ServiceException e) {
      xLogger.severe("Unable to fetch logged in sUser details", e);
      throw new InvalidServiceException(backendMessages.getString("users.logged.fetch"));
    }
    results.setOffset(offset);
    return userBuilder.buildUsers(results, sUser, isSuggest);
  }


  @RequestMapping(value = "/role/{role}", method = RequestMethod.GET)
  public
  @ResponseBody
  Results getUserByRole(
      @RequestParam(defaultValue = PageParams.DEFAULT_OFFSET_STR) int offset,
      @RequestParam(defaultValue = PageParams.DEFAULT_SIZE_STR) int size,
      @RequestParam(required = false) String q,
      @PathVariable String role, HttpServletRequest request) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    Long domainId = sUser.getCurrentDomainId();
    Results results;
    try {
      Navigator navigator =
          new Navigator(request.getSession(), "UsersController.getUserByRole", offset, size,
              "dummy", 0);
      PageParams pageParams = new PageParams(navigator.getCursor(offset), offset, size);
      if (SecurityConstants.ROLE_SUPERUSER.equals(role) && !sUser.getRole()
          .equals(SecurityConstants.ROLE_SUPERUSER)) {
        throw new ForbiddenAccessException(backendMessages.getString("user.unauthorized"));
      }
      if (SecurityConstants.ROLE_SUPERUSER.equals(role)) {
        results = new Results<>(usersService.getSuperusers(), null);
      } else {
        results = usersService.getUsers(domainId, role, true, q, pageParams);
      }
      navigator.setResultParams(results);
      results.setNumFound(-1);
    } catch (ServiceException e) {
      xLogger.severe("Error in getting users by role", e);
      throw new InvalidServiceException(backendMessages.getString("user.by.role.error"));
    }
    return userBuilder.buildUsers(results, sUser, false);
  }

  @RequestMapping(value = "/roles", method = RequestMethod.GET)
  public
  @ResponseBody
  Map<String, String> getRoles(@RequestParam boolean edit, @RequestParam String euid) {
    SecureUserDetails user = SecurityUtils.getUserDetails();
    String role = user.getRole();
    String uid = user.getUsername();
    Map<String, String> roles = new LinkedHashMap<>();
    roles.put("ROLE_ko", "kioskowner");
    if (edit && SecurityConstants.ROLE_SERVICEMANAGER.equals(role) && StringUtils.isNotBlank(euid)
        && euid.equals(uid)) {
      roles.put("ROLE_sm", "servicemanager");
    }
    if (SecurityUtil.compareRoles(role, SecurityConstants.ROLE_DOMAINOWNER) >= 0) {
      roles.put("ROLE_sm", "servicemanager");
      roles.put("ROLE_do", "domainowner");
    }
    if (SecurityUtil.compareRoles(role, SecurityConstants.ROLE_SUPERUSER) == 0) {
      roles.put("ROLE_su", "superuser");
    }
    return roles;
  }

  @RequestMapping(value = "/", method = RequestMethod.GET)
  public
  @ResponseBody
  Results getDomainUsers(
      @RequestParam(defaultValue = PageParams.DEFAULT_OFFSET_STR) int offset,
      @RequestParam(defaultValue = PageParams.DEFAULT_SIZE_STR) int size,
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String utype,
      @RequestParam(required = false) boolean includeSuperusers,
      @RequestParam(required = false) boolean includeChildDomainUsers,
      HttpServletRequest request) {
    return getUsers(q, request, offset, size, false, ACTIVE_USERS.equals(utype), includeSuperusers,
        includeChildDomainUsers);

  }

  @RequestMapping(value = "/elements", method = RequestMethod.GET)
  public
  @ResponseBody
  Set<String> getElementsByUserFilter(
      @RequestParam String paramName,
      @RequestParam String paramValue,
      HttpServletRequest request) {

    if (StringUtils.isBlank(paramName) && StringUtils.isBlank(paramValue)) {
      throw new IllegalArgumentException("paramName and ParamValue is not supplied");
    }

    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    Long domainId = sUser.getCurrentDomainId();
    Set<String> elementSet;
    try {
      Navigator navigator =
          new Navigator(request.getSession(), "UsersController.getUserByRole", 0, 10, "dummy", 0);
      PageParams pageParams = new PageParams(navigator.getCursor(0), 0, 10);
      IUserAccount loggedInUser = usersService.getUserAccount(sUser.getUsername());
      elementSet =
          usersService.getElementSetByUserFilter(domainId, loggedInUser, paramName, paramValue, pageParams);
    } catch (ObjectNotFoundException e) {
      xLogger.warn("Unable to fetch logged in user details", e);
      throw new InvalidServiceException(backendMessages.getString("users.logged.fetch"));
    } catch (ServiceException e) {
      xLogger.severe("Unable to fetch logged in user details", e);
      throw new InvalidServiceException(backendMessages.getString("users.logged.fetch"));
    }
    return elementSet;
  }

  @RequestMapping(value = "/domain/users", method = RequestMethod.POST)
  public
  @ResponseBody
  Results getFilteredDomainUsers(
      @RequestBody UserFilterRequestObj filters,
      HttpServletRequest request) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    Long domainId = sUser.getCurrentDomainId();
    DomainConfig dc = DomainConfig.getInstance(domainId);
    Results results;
    try {
      IUserAccount loggedInUser = usersService.getUserAccount(sUser.getUsername());
      Navigator navigator =
          new Navigator(request.getSession(), "UsersController.getFilteredDomainUsers",
              filters.offset, filters.size, "dummy", 0);
      PageParams
          pageParams =
          new PageParams(navigator.getCursor(filters.offset), filters.offset, filters.size);
      Map<String, Object> fMap = new HashMap<>();

      if (filters.role != null && !filters.role.isEmpty()) {
        fMap.put("role", filters.role);
      }
      if (filters.nName != null && !filters.nName.isEmpty()) {
        fMap.put("nName", filters.nName);
      }
      if (filters.mobilePhoneNumber != null && !filters.mobilePhoneNumber.isEmpty()) {
        if (!filters.mobilePhoneNumber.startsWith("+")) {
          filters.mobilePhoneNumber = "+" + filters.mobilePhoneNumber;
        }
        fMap.put("mobilePhoneNumber", filters.mobilePhoneNumber);
      }
      if (filters.lastLoginFrom != null && !filters.lastLoginFrom.isEmpty()) {
        try {
          Date
              fromDate =
              LocalDateUtil
                  .parseCustom(filters.lastLoginFrom, Constants.DATETIME_FORMAT, dc.getTimezone());
          fMap.put("from", fromDate);
        } catch (Exception e) {
          xLogger.warn("Exception when parsing from date " + filters.lastLoginFrom, e);
        }
      }
      if (filters.lastLoginTo != null && !filters.lastLoginTo.isEmpty()) {
        try {
          Date
              toDate =
              LocalDateUtil.getOffsetDate(LocalDateUtil
                      .parseCustom(filters.lastLoginTo, Constants.DATETIME_FORMAT,
                          dc.getTimezone()),
                  1);
          fMap.put("to", toDate);
        } catch (Exception e) {
          xLogger.warn("Exception when parsing to date " + filters.lastLoginTo, e);
        }
      }
      if (StringUtils.isNotEmpty(filters.v)) {
        fMap.put("v", filters.v);
      }
      if (filters.isEnabled != null && !filters.isEnabled.isEmpty()) {
        fMap.put("isEnabled", Boolean.parseBoolean(filters.isEnabled));
      }
      if (filters.neverLogged) {
        fMap.put("neverLogged", true);
      }
      if (filters.tgs != null && !filters.tgs.isEmpty()) {
        fMap.put("utag", filters.tgs);
      }
      results = usersService.getUsersByFilter(domainId, loggedInUser, fMap, pageParams);
      navigator.setResultParams(results);
    } catch (ObjectNotFoundException e) {
      xLogger.warn("Unable to fetch logged in user details", e);
      throw new InvalidServiceException(backendMessages.getString("users.logged.fetch"));
    } catch (ServiceException e) {
      xLogger.severe("Unable to fetch logged in user details", e);
      throw new InvalidServiceException(backendMessages.getString("users.logged.fetch"));
    }
    return userBuilder.buildUsers(results, sUser, false);
  }

  @RequestMapping("/check/")
  public
  @ResponseBody
  boolean checkUserExist(@RequestParam String userid) {
    if (StringUtils.isNotEmpty(userid)) {
      SecureUserDetails user = SecurityUtils.getUserDetails();
      Locale locale = user.getLocale();
      ResourceBundle backendMessages = Resources.getBundle(locale);
      try {
        if (usersService.userExists(userid)) {
          return true;
        }
      } catch (ServiceException e) {
        xLogger.severe("Exception when checking user ID existence for userID {0}", userid);
        throw new InvalidServiceException(
            backendMessages.getString("user.id.existence") + " " + userid);
      }
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  @RequestMapping(value = "/check/custom", method = RequestMethod.GET)
  public
  @ResponseBody
  boolean checkCustomIDExists(@RequestParam String customId,
                              @RequestParam(required = false) String userId) {
    try {
      return usersService.customIdExists(SecurityUtils.getCurrentDomainId(), customId, userId);
    } catch (ServiceException e) {
      xLogger.warn("Error while adding or updating user Account : Custom ID {0} already exists.",
          customId);
    }
    return false;
  }

  @RequestMapping(value = "/", method = RequestMethod.POST)
  public
  @ResponseBody
  String create(@RequestBody UserModel userModel) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    IUserAccount ua = userBuilder.buildUserAccount(userModel);
    ua.setRegisteredBy(sUser.getUsername());
    ua.setUpdatedBy(sUser.getUsername());
    try {
      if (ua.getUserId() != null) {
        long domainId = SecurityUtils.getCurrentDomainId();
        ua = usersService.addAccount(domainId, ua);
        xLogger.info("AUDITLOG \t {0} \t {1} \t USER \t " +
                "CREATE \t {2} \t {3}", domainId, sUser.getUsername(), ua.getUserId(),
            ua.getFullName());
      } else {
        throw new InvalidDataException(backendMessages.getString("user.id.none"));
      }
    } catch (ServiceException e) {
      xLogger.warn("Error creating User for " + ua.getDomainId(), e);
      throw new InvalidServiceException(
          backendMessages.getString("user.create.error") + " " + ua.getDomainId());
    }
    return backendMessages.getString("user.uppercase") + " " + MsgUtil.bold(ua.getFullName()) + " "
        + backendMessages.getString("created.success");
  }

  @RequestMapping(value = "/delete/", method = RequestMethod.POST)
  public
  @ResponseBody
  String delete(@RequestBody String userIds) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Long domainId = sUser.getCurrentDomainId();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    int usrCnt;
    int errCnt = 0;
    StringBuilder errMsg = new StringBuilder();
    List<String> errUsr = new ArrayList<>();
    try {
      String[] usersArray = userIds.split(",");
      ArrayList<String> users = new ArrayList<>(usersArray.length);
      for (String userId : usersArray) {
        if (GenericAuthoriser.authoriseUser(userId.trim())) {
          try {
            Results results = entitiesService.getKioskIdsForUser(userId, null, null);
            if (results.getResults() != null && !results.getResults().isEmpty()) {
              IUserAccount u = usersService.getUserAccount(userId);
              errMsg.append(" - ").append(u.getFullName())
                  .append(" ").append(backendMessages.getString("user.cannotdelete")).append(" ")
                  .append(results.getResults().size())
                  .append(" ").append(backendMessages.getString("one.or.more.kiosk"))
                  .append(MsgUtil.newLine());
            } else {
              users.add(userId.trim());
            }
          } catch (ObjectNotFoundException e) {
            xLogger.warn("Delete: Error fetching kiosks for user " + userIds, e);
          }
        } else {
          errCnt++;
          errUsr.add(userId.trim());
        }
      }
      usersService.deleteAccounts(domainId, users, sUser.getUsername());
      usrCnt = users.size();
    } catch (ServiceException e) {
      xLogger.warn("Error deleting User details of " + userIds, e);
      throw new InvalidServiceException(
          backendMessages.getString("user.delete.error") + " " + userIds);
    }
    if (errCnt > 0) {
      StringBuilder sb = new StringBuilder();
      for (String usr : errUsr) {
        sb.append("\n").append(usr);
      }
      if (errMsg.length() > 0) {
        errMsg.append(MsgUtil.newLine()).append(backendMessages.getString("user.auth.denied"))
            .append(": ").append(sb);
      }
    }
    if (errMsg.length() > 0) {
      return MsgUtil.bold(String.valueOf(usrCnt)) + " " + backendMessages
          .getString("users.delete.success") + "." +
          MsgUtil.newLine() + MsgUtil.newLine() + backendMessages.getString("errors.oneormore")
          + " " +
          MsgUtil.newLine() + errMsg;
    }
    return MsgUtil.bold(String.valueOf(usrCnt)) + " " + backendMessages
        .getString("users.delete.success");
  }

  @RequestMapping(value = "/user/{userId:.+}", method = RequestMethod.GET)
  public
  @ResponseBody
  UserModel getUserById(@PathVariable String userId,
                        @RequestParam(required = false) boolean isDetail) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    String timezone = sUser.getTimezone();
    IUserAccount rb = null;
    IUserAccount lu = null;
    UserModel model;
    try {
      if (!GenericAuthoriser.authoriseUser(userId)) {
        throw new ForbiddenAccessException(backendMessages.getString("permission.denied"));
      }
      UserEntitiesModel userEntitiesModel = entitiesService.getUserWithKiosks(userId);
      IUserAccount ua = userEntitiesModel.getUserAccount();
      if (ua.getRegisteredBy() != null) {
        try {
          rb = usersService.getUserAccount(ua.getRegisteredBy());
        } catch (Exception ignored) {
        }
      }
      if (ua.getUpdatedBy() != null) {
        try {
          lu = usersService.getUserAccount(ua.getUpdatedBy());
        } catch (Exception ignored) {
        }
      }
      model = userBuilder.buildUserModel(ua, rb, lu, locale, sUser.getTimezone(), false);
      if (userEntitiesModel.getKiosks() != null) {
        model.entities = entityBuilder.buildUserEntities(userEntitiesModel.getKiosks());
      }
      if (isDetail) {
        setDisplayNames(locale, timezone, model);
      }
    } catch (ServiceException | ObjectNotFoundException se) {
      xLogger.warn("Error fetching User details for " + userId, se);
      throw new InvalidServiceException(
          backendMessages.getString("user.details.fetch.error") + " " + userId);
    }
    return model;
  }

  @RequestMapping(value = "/user/meta/{userId:.+}", method = RequestMethod.GET)
  public
  @ResponseBody
  UserModel getUserMetaById(@PathVariable String userId) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    UserModel model;
    try {
      IUserAccount ua = usersService.getUserAccount(userId);
      model = userBuilder.buildUserModel(ua, locale, sUser.getTimezone(), true, null);
    } catch (ObjectNotFoundException se) {
      xLogger.warn("Error fetching User details for " + userId, se);
      throw new InvalidServiceException(
          backendMessages.getString("user.details.fetch.error") + " " + userId);
    }
    return model;
  }

  @RequestMapping(value = "/users/", method = RequestMethod.GET)
  public
  @ResponseBody
  List<UserModel> getUsersByIds(@RequestParam String userIds,
                                @RequestParam(required = false) boolean isMessage) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    String[] ids;
    if (!"null".equalsIgnoreCase(userIds)) {
      ids = userIds.split(",");
      List<UserModel> models = new ArrayList<>(ids.length);
      try {
        for (String id : ids) {
          try {
            if (GenericAuthoriser.authoriseUser(id)) {
              IUserAccount ua = usersService.getUserAccount(id);
              List<IKiosk> kiosks = null;
              if (!isMessage) {
                kiosks = entitiesService.getKiosksForUser(ua, null, null).getResults();
              }
              UserModel model = userBuilder.buildUserModel(ua, locale, sUser.getTimezone(), isMessage,
                  kiosks);
              models.add(model);
            }
          } catch (ObjectNotFoundException oe) {
            xLogger.warn("Error fetching User details for messaging ", oe);
          }
        }
      } catch (ServiceException se) {
        xLogger.warn("Error fetching User details for messaging ", se);
        throw new InvalidServiceException(
            backendMessages.getString("user.details.fetch.id.error") + " " + userIds);
      }
      return models;
    }
    return new ArrayList<>();
  }

  private void setDisplayNames(Locale locale, String timezone, UserModel model) {
    model.lngn = new Locale(model.lng).getDisplayLanguage();
    if (null != model.getGen()) {
      model.setGenderLabel(UserUtils.getGenderDisplay(model.getGen(), locale));
    }
    if (null != model.ro) {
      model.ron = UserUtils.getRoleDisplay(model.ro, locale);
    }
    if (null != model.ms) {
      model.msn = LocalDateUtil.format(model.ms, locale, timezone);
    }
    if (null != model.ll) {
      model.lln = LocalDateUtil.format(model.ll, locale, timezone);
    }
    if (null != model.lr) {
      model.lrn = LocalDateUtil.format(model.lr, locale, timezone);
    }
    if (null != model.tz) {
      model.tzn = TimeZone.getTimeZone(model.tz).getDisplayName();
    }
    if (!IUserAccount.PERMISSION_DEFAULT.equals(model.per)) {
      model.pd = UserUtils.getPermissionDisplay(model.per, locale);
    }
  }

  @RequestMapping(value = "/user/{userId:.+}", method = RequestMethod.POST)
  public
  @ResponseBody
  String updateUser(@RequestBody UserModel userModel, @PathVariable String userId) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Long domainId = sUser.getCurrentDomainId();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    try {
      if (!GenericAuthoriser.authoriseUser(userModel.id)) {
        throw new ForbiddenAccessException(backendMessages.getString("permission.denied"));
      }
      IUserAccount user = usersService.getUserAccount(userModel.id);
      user.setUpdatedBy(sUser.getUsername());
      int exp = user.getAuthenticationTokenExpiry();
      userModel.pw = user.getEncodedPassword();
      IUserAccount ua = userBuilder.buildUserAccount(userModel, user);
      if (ua.getUserId() != null) {
        if (exp != userModel.atexp) {
          authenticationService.clearUserTokens(userId, false);
        }
        usersService.updateAccount(ua, sUser.getUsername());
      } else {
        throw new BadRequestException(backendMessages.getString("user.id.none"));
      }
      xLogger.info("AUDITLOG\t{0}\t{1}\t USER \t " +
          "UPDATE \t {2} \t {3}", domainId, sUser.getUsername(), ua.getUserId(), ua.getFullName());
    } catch (ServiceException | ObjectNotFoundException e) {
      xLogger.warn("Error Updating User details for " + userId, e);
      throw new InvalidServiceException(
          backendMessages.getString("user.details.update.error") + " " + userId);
    }
    return MsgUtil.cleanup(backendMessages.getString("user.uppercase") + " " + MsgUtil
        .bold(MsgUtil.trimConcat(userModel.fnm, userModel.lnm, " ")) + " " + backendMessages
        .getString("update.success"));
  }

  @RequestMapping(value = "/updatepassword", method = RequestMethod.POST)
  public
  @ResponseBody
  String updateUserPassword(@RequestBody ChangePasswordModel model) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Long domainId = sUser.getCurrentDomainId();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    String userId = model.getUid();
    try {
      IUserAccount user = usersService.getUserAccount(userId);
      if (!GenericAuthoriser.authoriseUser(userId)) {
        throw new ForbiddenAccessException(backendMessages.getString("permission.denied"));
      }
      // Authenticate user
      AuthRequest authRequest = AuthRequest.builder()
          .userId(userId)
          .password(model.getOldPassword()).build();
      if (authenticationService.authenticate(authRequest) == null) {
        return backendMessages.getString("invalid.lowercase");
      }
      authenticationService
          .changePassword(userId, null, model.getOldPassword(), model.getNpd(),
              model.isEnhanced());
      authenticationService.clearUserTokens(userId, false);
      xLogger.info("AUDITLOG \t {0} \t {1} \t USER \t " +
              "UPDATE PASSWORD \t {2} \t {3}", domainId, sUser.getUsername(), userId,
          user.getFullName());
    } catch (ServiceException se) {
      xLogger.warn("Error Updating User password for " + userId, se);
      throw new InvalidServiceException(
          backendMessages.getString("user.password.update.error") + " " + userId);
    } catch (ObjectNotFoundException e) {
      xLogger.severe("Object Not found exception in authenticate: {0}", e.getMessage());
      throw new InvalidServiceException(
          backendMessages.getString("user.password.update.error") + " " + userId);
    }
    return backendMessages.getString("password.change.success");
  }

  @RequestMapping(value = "/resetpassword/", method = RequestMethod.GET)
  public
  @ResponseBody
  String resetUserPassword(@RequestParam String userId, @RequestParam String sendType) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    String loggedInUserId = sUser.getUsername();
    Long domainId = sUser.getCurrentDomainId();
    try {
      IUserAccount ua = usersService.getUserAccount(userId);
      String newPassword = RandomPasswordGenerator.generate(SecurityUtil.isUserAdmin(ua.getRole()));
      String msg = backendMessages.getString("password.reset.success") + ": " + newPassword;
      String logMsg = backendMessages.getString("password.reset.success.log");
      authenticationService.changePassword(userId, null, null, newPassword,false);
      authenticationService.updateUserSession(userId, null);
      MessageService
          ms =
          MessageService
              .getInstance(sendType, ua.getCountry(), true, domainId, loggedInUserId, null);
      ms.send(ua, msg, MessageService.NORMAL, backendMessages.getString("password.updated"), null,
          logMsg);
      xLogger.info("AUDITLOG \t {0} \t {1} \t USER \t " +
          "RESET PASSWORD \t {2} \t {3}", domainId, sUser.getUsername(), userId, ua.getFullName());
      return backendMessages.getString("password.reset.info") + ". <br>" +
          MsgUtil.bold(backendMessages.getString("note") + ":") + " " + backendMessages
          .getString("user.password.check") + ".";
    } catch (Exception se) {
      xLogger.severe("{0} Exception while updating password:", se.getMessage(), se);
      throw new InvalidServiceException(
          backendMessages.getString("password.update.error") + " " + userId);
    }
  }

  @RequestMapping(value = "/userstate/", method = RequestMethod.GET)
  public
  @ResponseBody
  String enableDisableUser(@RequestParam String userId, @RequestParam String action) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Long domainId = sUser.getCurrentDomainId();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    try {
      IUserAccount ua = usersService.getUserAccount(userId);
      if (GenericAuthoriser.authoriseUser(userId)) {
        if ("e".equals(action)) {
          authenticationService.enableAccount(userId);
          xLogger.info("AUDITLOG \t {0} \t {1} \t USER \t " +
              "ENABLE \t {2} \t {3}", domainId, sUser.getUsername(), userId, ua.getFullName());
          return backendMessages.getString("user.account.enabled") + " " + MsgUtil.bold(userId);
        } else {
          authenticationService.disableAccount(userId);
          authenticationService.updateUserSession(userId, null);
          xLogger.info("AUDITLOG \t {0} \t {1} \t USER \t " +
              "DISABLE \t {2} \t {3}", domainId, sUser.getUsername(), userId, ua.getFullName());
          return backendMessages.getString("user.account.disabled") + " " + MsgUtil.bold(userId);
        }
      } else {
        throw new ForbiddenAccessException(backendMessages.getString("permission.denied"));
      }
    } catch (ServiceException | ObjectNotFoundException se) {
      xLogger.warn("Error Updating User password for " + userId, se);
      throw new InvalidServiceException(
          backendMessages.getString("user.password.update.error") + " " + userId);
    }
  }

  @RequestMapping(value = "/sendmessage/", method = RequestMethod.POST)
  public
  @ResponseBody
  String sendMessage(@RequestBody UserMessageModel model) {
    if (model.type == null || model.type.isEmpty()) {
      model.type = "sms";
    }
    if (!"wappush".equalsIgnoreCase(model.template)) {
      model.pushURL = null;
    }
    boolean allUsers = StringUtils.isEmpty(model.userIds);
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    String senderUserId = null;
    Long domainId = null;
    Locale locale = null;
    if (sUser != null) {
      senderUserId = sUser.getUsername();
      domainId = sUser.getCurrentDomainId();
      locale = sUser.getLocale();
    }
    ResourceBundle backendMessages = Resources.getBundle(locale);
    if (allUsers) {
      UserMessageUtil.scheduleSendingToAllUsers(model, domainId, null, null, senderUserId);
    } else {
      try {
        UserMessageUtil.sendToSelectedUsers(model, senderUserId, domainId);
      } catch (ServiceException e) {
        xLogger.warn("Error sending message to users", e);
        throw new InvalidServiceException(
            backendMessages.getString("message.send.error") + " " + model.userIds);
      }
    }
    return backendMessages.getString("message.sent.success");
  }

  @RequestMapping(value = "/msgstatus/", method = RequestMethod.GET)
  public
  @ResponseBody
  Results getUsersMessageStatus(
      @RequestParam(defaultValue = PageParams.DEFAULT_OFFSET_STR) int offset,
      @RequestParam(defaultValue = PageParams.DEFAULT_SIZE_STR) int size,
      HttpServletRequest request) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    String userId = sUser.getUsername();
    Long domainId = sUser.getCurrentDomainId();
    String role = sUser.getRole();
    Navigator
        navigator =
        new Navigator(request.getSession(), "UsersController.getUsersMessageStatus", offset, size,
            "dummy", 0);
    PageParams pageParams = new PageParams(navigator.getCursor(offset), offset, size);
    Results results;
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    try {
      if (SecurityUtil.compareRoles(role, SecurityConstants.ROLE_DOMAINOWNER) >= 0) {
        results = MessageUtil.getLogs(domainId, pageParams);
      } else {
        results = MessageUtil.getLogs(userId, pageParams);
      }
      navigator.setResultParams(results);
    } catch (MessageHandlingException e) {
      xLogger.warn("Error in building message status", e);
      throw new InvalidServiceException(backendMessages.getString("message.status.build.error"));
    }

    String timezone = sUser.getTimezone();
    int no = offset;
    List<UserMessageModel> userMessageStatus = new ArrayList<>();
    for (Object res : results.getResults()) {
      IMessageLog ml = (IMessageLog) res;
      try {
        userMessageStatus
            .add(messageBuilder.buildUserMessageModel(ml, locale, userId, ++no, timezone));
      } catch (Exception e) {
        xLogger.warn("Error in building message status", e);
      }
    }
    return new Results<>(userMessageStatus, results.getCursor(), -1, offset);
  }

  @RequestMapping(value = "/switch", method = RequestMethod.GET)
  public
  @ResponseBody
  boolean switchConsole() {
    SecureUserDetails user = SecurityUtils.getUserDetails();
    Locale locale = user.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    try {
      IUserAccount ua = usersService.getUserAccount(user.getUsername());
      ua.setUiPref(false);
      usersService.updateAccount(ua, user.getUsername());
      return true;
    } catch (ObjectNotFoundException e) {
      xLogger.warn("Unable to fetch logged in user details", e);
      throw new InvalidServiceException(backendMessages.getString("users.logged.fetch"));
    } catch (ServiceException e) {
      xLogger.severe("Unable to fetch logged in user details", e);
      throw new InvalidServiceException(backendMessages.getString("users.logged.fetch"));
    }


  }

  @RequestMapping(value = "/generalconfig", method = RequestMethod.GET)
  public
  @ResponseBody
  String getGeneralConfig() {
    try {
      return getObject(IConfig.GENERALCONFIG);
    } catch (ServiceException e) {
      xLogger.severe("Error in getting General Configuration details");
      throw new InvalidServiceException("Error in getting General Config details");
    } catch (ObjectNotFoundException e) {
      xLogger.severe("Error in getting General Config details");
      throw new InvalidServiceException("Error in getting General Config details");
    }
  }

  private String getObject(String config)
      throws ServiceException, ObjectNotFoundException {
    IConfig c = configurationMgmtService.getConfiguration(config);
    String jsonObject = null;
    if (c != null && c.getConfig() != null) {
      jsonObject = c.getConfig();
    }
    return jsonObject;
  }

  @RequestMapping(value = "/addaccessibledomains", method = RequestMethod.POST)
  public
  @ResponseBody
  AddRemoveAccDomainsResponseObj addAccessibleDomains(
      @RequestBody AddAccDomainsRequestObj addAccDomainsReqObj) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Long domainId = sUser.getCurrentDomainId();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);

    if (addAccDomainsReqObj.accDids == null || addAccDomainsReqObj.accDids.isEmpty()) {
      xLogger.warn("Error while adding accessible domains for user {0}, accDids is null or empty",
          addAccDomainsReqObj.userId);
      throw new InvalidServiceException(
          backendMessages.getString("user.addaccessibledomain.error1") + " \'"
              + addAccDomainsReqObj.userId + "\'");
    }

    try {
      List<Long> finalUserAccDomainIds = usersService.addAccessibleDomains(addAccDomainsReqObj.userId,
          addAccDomainsReqObj.accDids);

      AddRemoveAccDomainsResponseObj resp = new AddRemoveAccDomainsResponseObj();
      resp.accDsm = userBuilder.buildAccDmnSuggestionModelList(finalUserAccDomainIds);
      resp.respMsg = backendMessages.getString("user") + " \'" + addAccDomainsReqObj.userId + "\' "
          + backendMessages.getString("user.addaccessibledomain.success");
      xLogger.info("AUDITLOG \t {0} \t {1} \t USER \t " +
              "ADD ACCESSIBLE DOMAINS \t {2}", domainId, sUser.getUsername(),
          addAccDomainsReqObj.userId);
      return resp;
    } catch (Exception e) {
      xLogger.warn("Error while adding accessible domains for user {0}", addAccDomainsReqObj.userId,
          e);
      throw new InvalidServiceException(
          backendMessages.getString("user.addaccessibledomain.error1") + " "
              + addAccDomainsReqObj.userId);
    }
  }

  @RequestMapping(value = "/removeaccessibledomain", method = RequestMethod.GET)
  public
  @ResponseBody
  AddRemoveAccDomainsResponseObj removeAccessibleDomains(@RequestParam String userId,
                                                         @RequestParam Long domainId) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    String returnMsg;
    List<DomainSuggestionModel> respAccDsm;
    try {
      if (domainId == null) {
        xLogger.warn("Error while removing accessible domain for user {0}", userId);
        throw new InvalidServiceException(
            backendMessages.getString("user.removeaccessibledomains.error1") + " \'" + userId
                + "\'");
      }
      IUserAccount ua = usersService.getUserAccount(userId);
      List<Long> uAccDids = ua.getAccessibleDomainIds();
      if (uAccDids != null && !uAccDids.isEmpty()) {
        uAccDids.remove(domainId);
        if (uAccDids.isEmpty()) {
          uAccDids.add(ua.getDomainId());
        }
      }

      usersService.updateAccount(ua, sUser.getUsername());
      returnMsg =
          backendMessages.getString("user") + " \'" + userId + "\' " + backendMessages
              .getString("user.removeaccessibledomain.success");
      respAccDsm = userBuilder.buildAccDmnSuggestionModelList(ua.getAccessibleDomainIds());
      AddRemoveAccDomainsResponseObj resp = new AddRemoveAccDomainsResponseObj();
      resp.accDsm = respAccDsm;
      resp.respMsg = returnMsg;
      xLogger.info("AUDITLOG \t {0} \t {1} \t USER \t " +
              "REMOVE ACCESSIBLE DOMAINS \t {2} \t {3}", domainId, sUser.getUsername(), userId,
          ua.getFullName());
      return resp;
    } catch (Exception e) {
      xLogger.warn("Error while removing accessible domains for user " + userId, e);
      throw new InvalidServiceException(
          backendMessages.getString("user.removeaccessibledomain.error1") + " \'" + userId + "\'");
    }

  }

  @RequestMapping(value = "/forcelogoutonmobile", method = RequestMethod.GET)
  public
  @ResponseBody
  String forceLogoutOnMobile(@RequestParam String userId)
      throws ServiceException {
    Boolean success;
    String msg;
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    if (userId != null) {
      try {
        IUserAccount userAccount = usersService.getUserAccount(userId);
        success = authenticationService.clearUserTokens(userId, false);
        if (success) {
          return backendMessages.getString("user.force.logout") + " " + MsgUtil
              .bold(userAccount.getFullName());
        } else {
          msg =
              backendMessages.getString("user.uppercase") + " " + MsgUtil
                  .bold(userAccount.getFullName()) + " " + backendMessages
                  .getString("user.login.warn");
        }
      } catch (ObjectNotFoundException e) {
        xLogger.warn("UserId not found" + userId, e);
        msg =
            backendMessages.getString("user.id") + " " + MsgUtil.bold(userId) + " "
                + backendMessages.getString("user.not.found") + ".";
      } catch (SystemException e) {
        msg = backendMessages.getString("user.logout.system.error") + " " + userId;
      }
    } else {
      xLogger.warn("UserId not found" + userId);
      msg =
          backendMessages.getString("user.id") + " " + MsgUtil.bold(userId) + " " + backendMessages
              .getString("user.not.found") + ".";
    }
    if (StringUtils.isNotEmpty(msg)) {
      throw new InvalidServiceException(msg);
    }
    return null;
  }

  @RequestMapping(value = "/update/mobileaccessed", method = RequestMethod.POST)
  public
  @ResponseBody
  String updateMobileUserLastAccessTime(@RequestParam String userId, @RequestParam Long aTime) {
    if (userId != null && aTime != null) {
      try {
        usersService.updateLastMobileAccessTime(userId, aTime);
      } catch (Exception e) {
        xLogger.warn(" {0} while updating last transacted time for the user, {1}", e.getMessage(),
            userId, e);
      }
    }
    return "";
  }

  @RequestMapping(value = "/update/loginhistory", method = RequestMethod.POST)
  public
  @ResponseBody
  void updateUserLoginHistory(@RequestBody UserLoginHistoryModel userLoginHistory) {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    try {
      IUserLoginHistory user = JDOUtils.createInstance(IUserLoginHistory.class);
      user.setUserId(userLoginHistory.getUserId());
      user.setLoginTime(userLoginHistory.getLoginTime());
      user.setLgSrc(userLoginHistory.getLgSrc());
      user.setIpAddr(userLoginHistory.getIpAddr());
      user.setUsrAgnt(userLoginHistory.getUsrAgnt());
      user.setVersion(userLoginHistory.getVersion());
      user.setReferer(userLoginHistory.getReferer());
      user.setStatus(userLoginHistory.getStatus());
      pm.makePersistent(user);
    } catch (Exception e) {
      xLogger.warn(" {0} while updating the user login history, {1}", e.getMessage(),
          userLoginHistory.getUserId(), e);
    } finally {
      if (pm != null) {
        pm.close();
      }
    }
  }

  @RequestMapping(value = "/user-contact-by-tag", method = RequestMethod.GET)
  public
  @ResponseBody
  List<AdminContactConfigModel> getUsersContactByTag(@RequestParam(name = "object_id") Long objectId,
                                                     @RequestParam(name = "object_type", required = false) String objectType,
                                                     @RequestParam(name = "user_tags") String userTags)
      throws ServiceException {
    if (objectId == null) {
      xLogger.warn("Object id is mandatory");
      throw new ServiceException("Object id is mandatory");
    }
    List<IUserAccount>
        results =
        usersService.getUsersByTag(objectId, objectType, StringUtil.getList(userTags));
    return userBuilder.buildAdminContactModel(results);
  }
}
