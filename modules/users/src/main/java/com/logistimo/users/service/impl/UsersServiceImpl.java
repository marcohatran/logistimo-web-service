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

package com.logistimo.users.service.impl;

import com.logistimo.AppFactory;
import com.logistimo.api.models.UserDeviceModel;
import com.logistimo.auth.SecurityConstants;
import com.logistimo.auth.SecurityUtil;
import com.logistimo.auth.service.AuthenticationService;
import com.logistimo.auth.service.AuthorizationService;
import com.logistimo.constants.CharacterConstants;
import com.logistimo.constants.Constants;
import com.logistimo.constants.QueryConstants;
import com.logistimo.constants.SourceConstants;
import com.logistimo.dao.JDOUtils;
import com.logistimo.domains.entity.IDomainLink;
import com.logistimo.domains.service.DomainsService;
import com.logistimo.domains.utils.DomainsUtil;
import com.logistimo.entities.entity.IUserToKiosk;
import com.logistimo.entity.comparator.LocationComparator;
import com.logistimo.events.entity.IEvent;
import com.logistimo.events.exceptions.EventGenerationException;
import com.logistimo.events.processor.EventPublisher;
import com.logistimo.exception.ForbiddenAccessException;
import com.logistimo.exception.HttpBadRequestException;
import com.logistimo.exception.InvalidServiceException;
import com.logistimo.exception.SystemException;
import com.logistimo.exception.UnauthorizedException;
import com.logistimo.locations.client.LocationClient;
import com.logistimo.locations.model.LocationResponseModel;
import com.logistimo.logger.XLog;
import com.logistimo.pagination.PageParams;
import com.logistimo.pagination.QueryParams;
import com.logistimo.pagination.Results;
import com.logistimo.services.ObjectNotFoundException;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;
import com.logistimo.services.cache.MemcacheService;
import com.logistimo.services.impl.PMF;
import com.logistimo.tags.dao.ITagDao;
import com.logistimo.tags.entity.ITag;
import com.logistimo.users.builders.UserDeviceBuilder;
import com.logistimo.users.dao.IUserDao;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.users.entity.IUserDevice;
import com.logistimo.users.entity.UserAccount;
import com.logistimo.users.models.ExtUserAccount;
import com.logistimo.users.service.UsersService;
import com.logistimo.utils.Counter;
import com.logistimo.utils.MessageUtil;
import com.logistimo.utils.PasswordEncoder;
import com.logistimo.utils.QueryUtil;
import com.logistimo.utils.StringUtil;
import com.logistimo.utils.ThreadLocalUtil;
import com.netflix.hystrix.exception.HystrixBadRequestException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;

/**
 * Created by charan on 04/03/17.
 */
@Service
public class UsersServiceImpl implements UsersService {

  private static final XLog xLogger = XLog.getLog(UsersServiceImpl.class);
  private static final String SALT_HASH_SEPARATOR = "####";
  private static final String MMA = "MMA";
  private static final String SUCCESS = "SUCCESS";

  private ITagDao tagDao;
  private IUserDao userDao;
  private DomainsService domainsService;
  private AuthenticationService authenticationService;
  private LocationClient locationClient;
  private AuthorizationService authorizationService;

  private MemcacheService memcacheService;

  public MemcacheService getMemcacheService() {
    if (memcacheService == null) {
      memcacheService = AppFactory.get().getMemcacheService();
    }
    return memcacheService;
  }

  @Autowired
  public void setTagDao(ITagDao tagDao) {
    this.tagDao = tagDao;
  }

  @Autowired
  public void setUserDao(IUserDao userDao) {
    this.userDao = userDao;
  }

  @Autowired
  public void setDomainsService(DomainsService domainsService) {
    this.domainsService = domainsService;
  }

  @Autowired
  public void setAuthenticationService(AuthenticationService authenticationService) {
    this.authenticationService = authenticationService;
  }

  @Autowired
  public void setLocationClient(LocationClient locationClient) {
    this.locationClient = locationClient;
  }

  @Autowired
  public void setAuthorizationService(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  /**
   * Check to see if a user id exists in the system or not.
   *
   * @param userId The id of the user to be checked
   * @return true, if the user exists (even if inactive); false, if this id does not exist
   */
  public boolean userExists(String userId) throws ServiceException {
    xLogger.fine("Entering userExists");

    if (userId == null || userId.isEmpty()) {
      throw new ServiceException("Invalid user Id: " + userId);
    }

    boolean userExists = false;
    PersistenceManager pm = PMF.get().getPersistenceManager();
    try {
      JDOUtils.getObjectById(IUserAccount.class, userId, pm);
      userExists = true; // if we come here, then the user with this id must exist
    } catch (JDOObjectNotFoundException e) {
      // user with this id does NOT exist; do nothing
    } finally {
      pm.close();
    }
    xLogger.fine("Exiting userExists");
    return userExists;
  }

  /**
   * Add a new user account
   */
  @SuppressWarnings("finally")
  public IUserAccount addAccount(Long domainId, IUserAccount account) throws ServiceException {
    xLogger.fine("Entering addAccount");

    if (domainId == null || account == null) {
      throw new ServiceException("Invalid input parameters");
    }

    boolean userExists = false;
    String errMsg = null;
    Exception exception = null;
    updateUserLocationIds(account);
    Date now = new Date();
    account.setMemberSince(now);
    account.setUpdatedOn(now);
    account.setEnabled(true);
    account.setDomainId(domainId);
    account.setFirstName(StringUtil.getTrimmedName(account.getFirstName()));
    account.setLastName(StringUtil.getTrimmedName(account.getLastName()));

    List<Long> accDids = new ArrayList<>();
    accDids.add(domainId);
    account.setAccessibleDomainIds(accDids);
    String accountId = account.getUserId();
    xLogger.info("addAccount: userId is {0}", accountId);
    PersistenceManager pm = PMF.get().getPersistenceManager();
    Transaction tx = pm.currentTransaction();
    final Locale locale = ThreadLocalUtil.get().getSecureUserDetails().getLocale();
    ResourceBundle messages = Resources.get().getBundle(Constants.MESSAGES, locale);
    ResourceBundle backendMessages = Resources.get().getBundle(Constants.BACKEND_MESSAGES, locale);
    try {
      try {
        if (!authorizationService.authoriseUpdateKiosk(
            account.getRegisteredBy(), domainId)) {
          throw new ForbiddenAccessException(backendMessages.getString("permission.denied"));
        }
        tx.begin();
        IUserAccount registeringUser = getUserAccount(account.getRegisteredBy());
        if(SecurityUtil.compareRoles(registeringUser.getRole(),account.getRole()) < 0){
          throw new ForbiddenAccessException(backendMessages.getString("permission.denied"));
        }
        @SuppressWarnings("unused")
        IUserAccount user = JDOUtils.getObjectById(IUserAccount.class, accountId, pm);
        xLogger.warn("addAccount: User {0} already exists", accountId);
        userExists = true;
      } catch (JDOObjectNotFoundException e) {
        xLogger.fine("addAccount: User {0} does not exist. Adding user to database", accountId);
        // Check if custom ID is specified for the user account. If yes, check if the specified custom ID already exists.
        boolean customIdExists = false;
        if (account.getCustomId() != null && !account.getCustomId().isEmpty()) {
          customIdExists = checkIfCustomIdExists(account);
        }
        if (customIdExists) {
          xLogger.warn("addAccount: FAILED!! Cannot add account {0}. Custom ID {1} already exists",
              account.getUserId(), account.getCustomId());
          throw new ServiceException(
              backendMessages.getString("error.cannotadd") + " '" + account.getUserId() + "'. "
                  + messages.getString("customid") + " " + account.getCustomId() + " "
                  + backendMessages.getString("error.alreadyexists") + ".");
        }
        setNewUserPassword(account);
        if (account.getTags() != null) {
          account.setTgs(tagDao.getTagsByNames(account.getTags(), ITag.USER_TAG));
        }
        account = (IUserAccount) DomainsUtil.addToDomain(account, domainId, pm);
        xLogger.info("addAccount: adding user {0}", account.getUserId(),
            account.getUserId());
        account = pm.detachCopy(account);
        tx.commit();
      }
      try {
        EventPublisher.generate(domainId, IEvent.CREATED, null,
            JDOUtils.getImplClass(IUserAccount.class).getName(), account.getKeyString(), null);
      } catch (EventGenerationException e) {
        exception = e;
        errMsg = e.getMessage();
        xLogger.warn(
            "Exception when generating event for user creation for user {0} in domain {1}: {2}",
            account.getUserId(), domainId, e.getMessage());
      }
    } catch (Exception e) {
      xLogger.warn("Error while adding user", e);
      errMsg = e.getMessage();
      exception = e;
    } finally {
      if (tx.isActive()) {
        tx.rollback();
      }
      pm.close();
    }
    if (userExists) {
      errMsg =
          messages.getString("user") + " '" + account.getUserId() + "' " + backendMessages
              .getString("user.exists");
    }
    xLogger.fine("Exiting addAccount");

    if (errMsg != null) {
      throw new ServiceException(errMsg, exception);
    }

    return account;
  }

  /**
   * TODO This method should be in authentication service
   * @param account
   */
  private void setNewUserPassword(IUserAccount account) {
    String password = account.getEncodedPassword();
    if(password.contains(SALT_HASH_SEPARATOR)) {
      String[] saltHash = password.split(SALT_HASH_SEPARATOR);
      account.setEncodedPassword(PasswordEncoder.MD5(saltHash[1]));
      account.setSalt(saltHash[0]);
      account.setPassword(PasswordEncoder.bcrypt(password));
    }else {
      account.setEncodedPassword(PasswordEncoder.MD5(password));
    }
  }

  @SuppressWarnings("unchecked")
  public IUserAccount getUserAccount(String userId)
      throws ObjectNotFoundException {
    xLogger.fine("Entering getUserAccount");
    if (userId == null) {
      throw new IllegalArgumentException("Invalid user ID");
    }

    IUserAccount
        cacheUser =
        (IUserAccount) getMemcacheService()
            .get(Constants.USER_KEY + CharacterConstants.HASH + userId);
    if (cacheUser != null) {
      return cacheUser;
    }
    PersistenceManager pm = PMF.get().getPersistenceManager();
    IUserAccount user;
    Exception exception = null;
    boolean notFound = false;
    try {
      //Get the user object from the database
      user = JDOUtils.getObjectById(IUserAccount.class, userId, pm);
      user = pm.detachCopy(user);
      getMemcacheService().put(Constants.USER_KEY + CharacterConstants.HASH + userId, user);
      return user;
    } catch (JDOObjectNotFoundException e) {
      xLogger.warn("getUserAccount: User {0} does not exist in the database", userId);
      notFound = true;
      exception = e;
    } catch (Exception e) {
      xLogger.warn("getUserAccount: Exception : {0}", e.getMessage(), e);
      exception = e;
    } finally {
      pm.close();
    }
    if (notFound) {
      throw new ObjectNotFoundException("USR001", userId);
    }

    throw new SystemException(exception);
  }

  public Results getUsers(Long domainId, IUserAccount user, boolean activeUsersOnly,
                          String nameStartsWith,
                          PageParams pageParams, boolean includeChildDomainUsers)
      throws ServiceException {
    return getUsers(domainId, user, activeUsersOnly, false, nameStartsWith, pageParams,
        includeChildDomainUsers);
  }

  // Get users visible to a given user
  @SuppressWarnings("unchecked")
  public Results getUsers(Long domainId, IUserAccount user, boolean activeUsersOnly,
                          String nameStartsWith, PageParams pageParams) throws ServiceException {
    return getUsers(domainId, user, activeUsersOnly, false, nameStartsWith, pageParams, false);
  }

  // Get all users of a given role
  @SuppressWarnings("unchecked")
  public Results<IUserAccount> getUsers(Long domainId, String role, boolean activeUsersOnly,
                          String nameStartsWith, PageParams pageParams) throws ServiceException {
    xLogger.fine("Entering getUsers");
    if (domainId == null || role == null) {
      throw new ServiceException("Invalid input parameters");
    }
    List<IUserAccount> filtered = null;
    List<IUserAccount> users = null;
    String cursor = null;
    // Get the users of the given role
    Results
        results =
        findAccountsByDomain(domainId, IUserAccount.ROLE, role, nameStartsWith,
            pageParams, false); // users of a given role
    if (results != null) {
      users = results.getResults();
      cursor = results.getCursor();
    }
    if (activeUsersOnly && users != null && !users.isEmpty()) {
      filtered = new ArrayList<>();
      for (IUserAccount u : users) {
        if (u.isEnabled()) {
          filtered.add(u);
        }
      }
    }
    xLogger.fine("Exit getUsers");
    if (filtered != null) {
      return new Results(filtered, cursor);
    } else {
      return new Results(users, cursor);
    }
  }

  /**
   * Get users visible to a given user along with all super users(across domains)
   */
  public Results getUsers(Long domainId, IUserAccount user, boolean activeUsersOnly,
                          boolean includeSuperusers, String nameStartsWith, PageParams pageParams,
                          boolean includeChildDomainUsers)
      throws ServiceException {
    xLogger.fine("Entered getUsers, includeSuperusers: {0}", includeSuperusers);
    List<IUserAccount> filteredUsers = new ArrayList<>();
    String role = user.getRole();
    List<IUserAccount> users = null;
    Results results;
    boolean isDomainOwner = SecurityConstants.ROLE_DOMAINOWNER.equals(role);
    if (SecurityConstants.ROLE_SUPERUSER.equals(role) || isDomainOwner) {
      results = findAllAccountsByDomain(domainId, nameStartsWith, pageParams,
          includeChildDomainUsers, user);
      if (results.getResults() != null) {
        users = (List<IUserAccount>) results.getResults();
      }
      if (includeSuperusers && SecurityConstants.ROLE_SUPERUSER.equals(role)) {
        List<IUserAccount> superusers = getSuperusers();
        if (superusers != null && !superusers.isEmpty()) {
          for (IUserAccount superuser : superusers) {
            String nsuperuserName = superuser.getFullName().toLowerCase();
            if (users != null && !users.contains(superuser) && (nameStartsWith == null
                || nameStartsWith.isEmpty()
                || nsuperuserName.startsWith(nameStartsWith))) {
              users.add(superuser);
            }
          }
        }
      }
      if (!activeUsersOnly && SecurityConstants.ROLE_SUPERUSER.equals(role)) {
        return new Results<>(users, results.getCursor());
      }
    } else {
      results =
          findAccountsByDomain(domainId, IUserAccount.REGISTERED_BY, user.getUserId(),
              nameStartsWith,
              pageParams,
              includeChildDomainUsers);
      List<IUserAccount> regUsers = (List<IUserAccount>) results.getResults();
      if (!user.getUserId().equals(
          user.getRegisteredBy())) {
        users = new ArrayList<>();
        users.add(user);
        users.addAll(regUsers);
        if (users.size() > 1) {
          sortUsers(users);
        }
      } else {
        users = regUsers;
      }
    }
    for (IUserAccount u : users) {
      if ((!activeUsersOnly || u.isEnabled()) &&
          (u.getUserId().equals(user.getUserId())
              || SecurityUtil.compareRoles(role, u.getRole()) >= 0
              || SecurityConstants.ROLE_SUPERUSER
              .equals(role))) {
        filteredUsers.add(u);
      }
    }
    xLogger.fine("Exiting getUsers");
    return new Results<>(filteredUsers, QueryUtil.getCursor(results.getResults()));
  }

  public Results getUsersByFilter(Long domainId, IUserAccount user, Map<String, Object> filters,
                                  PageParams pageParams) throws ServiceException {
    xLogger.fine("Entered getUsersByFilter ");
    List<IUserAccount> users;
    String cursor = null;
    Results results;
    String role = user.getRole();
    boolean isDomainOwner = SecurityConstants.ROLE_DOMAINOWNER.equals(role);
    if (SecurityConstants.ROLE_SUPERUSER.equals(role)) {
      results = findAccountsByFilter(domainId, filters, pageParams, false);
      if (results == null) {
        return new Results();
      }
      users = results.getResults();
      cursor = results.getCursor();
    } else if (isDomainOwner) {
      results = findAccountsByFilter(domainId, filters, pageParams, true);
      if (results == null) {
        return new Results();
      }
      users = results.getResults();
      cursor = results.getCursor();

    } else {
      results =
          findAccountsByDomain(domainId, IUserAccount.REGISTERED_BY, user.getUserId(), "",
              pageParams, false);
      List<IUserAccount> regUsers = (List<IUserAccount>) results.getResults();
      if (!user.getUserId().equals(
          user.getRegisteredBy())) {
        users = new ArrayList<>();
        users.add(user);
        users.addAll(regUsers);
        if (users.size() > 1) {
          sortUsers(users);
        }
      } else {
        users = regUsers;
      }
    }
    xLogger.fine("Exit getUsersByFilter");
    return new Results<>(users, cursor, results.getNumFound(), 0);
  }

  /**
   * Get all the super users (across domains)
   */
  @SuppressWarnings("unchecked")
  public List<IUserAccount> getSuperusers() throws ServiceException {
    xLogger.fine("Entering getSuperusers");
    List<IUserAccount> superUsers = null;
    PersistenceManager pm = PMF.get().getPersistenceManager();
    Query q = pm.newQuery(JDOUtils.getImplClass(IUserAccount.class));
    String roleParam = SecurityConstants.ROLE_SUPERUSER;
    String declaration = " String roleParam";
    q.setFilter("role == roleParam");
    q.declareParameters(declaration);
    try {
      superUsers = (List<IUserAccount>) q.execute(roleParam);
      if (superUsers != null) {
        superUsers.size(); // This is done so that pm can be closed without throwing an exception.
        superUsers = (List<IUserAccount>) pm.detachCopyAll(superUsers);
      }
    } finally {
      try {
        q.closeAll();
      } catch (Exception ignored) {
        xLogger.warn("Exception while closing query", ignored);
      }
      pm.close();
    }
    xLogger.fine("Exiting getSuperusers");
    return superUsers;
  }

  /**
   * Update a given user account
   *
   * @param account   - user account with updated values
   * @param updatedBy - Id of user who is updating the user account
   */
  public void updateAccount(IUserAccount account, String updatedBy) throws ServiceException {
    xLogger.fine("Entering updateAccount");
    Exception exception = null;
    Date now = new Date();
    PersistenceManager pm = PMF.get().getPersistenceManager();
    Transaction tx = pm.currentTransaction();
    final Locale locale = ThreadLocalUtil.get().getSecureUserDetails().getLocale();
    ResourceBundle messages = Resources.get().getBundle(Constants.MESSAGES, locale);
    ResourceBundle backendMessages = Resources.get().getBundle(Constants.BACKEND_MESSAGES, locale);
    try {
      if (!authorizationService.authoriseUpdateKiosk(updatedBy, account.getDomainId())) {
        throw new ForbiddenAccessException(backendMessages.getString("permission.denied"));
      }
      tx.begin();
      //First check if the user already exists in the database
      IUserAccount user = JDOUtils.getObjectById(IUserAccount.class, account.getUserId(), pm);
      IUserAccount registeringUser = getUserAccount(updatedBy);
      if(SecurityUtil.compareRoles(registeringUser.getRole(),user.getRole()) < 0){
        throw new ForbiddenAccessException(backendMessages.getString("permission.denied"));
      }
      //location check
      int locindex = new LocationComparator().compare(user, account);
      //If we get here, it means the user exists
      user.setRole(account.getRole());
      user.setFirstName(StringUtil.getTrimmedName(account.getFirstName()));
      user.setLastName(StringUtil.getTrimmedName(account.getLastName()));
      user.setMobilePhoneNumber(account.getMobilePhoneNumber());
      user.setLandPhoneNumber(account.getLandPhoneNumber());
      user.setCity(account.getCity());
      user.setStreet(account.getStreet());
      user.setTaluk(account.getTaluk());
      user.setDistrict(account.getDistrict());
      user.setState(account.getState());
      user.setCountry(account.getCountry());
      user.setPinCode(account.getPinCode());
      user.setGender(account.getGender());
      user.setAuthenticationTokenExpiry(account.getAuthenticationTokenExpiry());
      user.setBirthdate(account.getBirthdate());
      user.setLastLogin(account.getLastLogin());
      user.setEmail(account.getEmail());
      user.setLanguage(account.getLanguage());
      user.setPhoneBrand(account.getPhoneBrand());
      user.setPhoneModelNumber(account.getPhoneModelNumber());
      user.setImei(account.getImei());
      user.setPhoneServiceProvider(account.getPhoneServiceProvider());
      user.setTimezone(account.getTimezone());
      user.setSimId(account.getSimId());
      user.setPermission(account.getPermission());
      user.setUserAgent(account.getUserAgent());
      user.setPreviousUserAgent(account.getPreviousUserAgent());
      user.setIPAddress(account.getIPAddress());
      user.setAppVersion(account.getAppVersion());
      user.setLastMobileAccessed(account.getLastMobileAccessed());
      user.setPrimaryKiosk(account.getPrimaryKiosk());
      user.setUiPref(account.getUiPref());
      user.setUpdatedOn(now);
      user.setUpdatedBy(account.getUpdatedBy());
      user.setAccessibleDomainIds(account.getAccessibleDomainIds());
      user.setLoginReconnect(account.getLoginReconnect());
      user.setStoreAppTheme(account.getStoreAppTheme());
      if (locindex != 0) {
        updateUserLocationIds(user);
      }
      boolean customIdExists = false;
      if (account.getCustomId() != null && !account.getCustomId().isEmpty() && !account
          .getCustomId().equals(user.getCustomId())) {
        customIdExists = checkIfCustomIdExists(account);
      }
      if (customIdExists) {
        xLogger.warn(
            "updateUserAccount: FAILED!! Cannot update account {0}. Custom ID {1} already exists",
            account.getUserId(), account.getCustomId());
        throw new ServiceException(
            backendMessages.getString("error.cannotupdate") + " '" + account.getUserId() + "'. "
                + messages.getString("customid") + " " + account.getCustomId() + " "
                + backendMessages.getString("error.alreadyexists") + ".");
      }
      user.setCustomId(account.getCustomId());
      user.setTgs(tagDao.getTagsByNames(account.getTags(), ITag.USER_TAG));
      tx.commit();
      removeUserFromCache(user.getUserId());
      try {
        EventPublisher.generate(account.getDomainId(), IEvent.MODIFIED, null,
            JDOUtils.getImplClass(IUserAccount.class).getName(), account.getKeyString(), null);
      } catch (EventGenerationException e) {
        xLogger.warn(
            "Exception when generating event for user-updation for user {0} in domain {1}:",
            account.getUserId(), account.getDomainId(), e);
        exception = e;
      }
    } catch (JDOObjectNotFoundException e) {
      xLogger.warn("updateAccount: FAILED!! User {0} does not exist in the database",
          account.getUserId());
      exception = e;
    } catch(UnauthorizedException | ForbiddenAccessException e){
      throw e;
    }catch (Exception e) {
      exception = e;
    } finally {
      if (tx.isActive()) {
        tx.rollback();
      }
      pm.close();
    }
    if (exception != null) {
      if (exception instanceof HystrixBadRequestException) {
        HttpBadRequestException ex = (HttpBadRequestException) exception.getCause();
        throw new ServiceException(ex.getCode(),
            ThreadLocalUtil.get().getSecureUserDetails().getLocale(), (String) null);
      } else {
        throw new ServiceException(exception.getMessage());
      }
    }
    xLogger.fine("Exiting updateAccount");
  }

  @Override
  public List<Long> moveAccessibleDomains(String userId, Long srcDomainId, Long destDomainId)
      throws ServiceException {
    return addAccessibleDomains(userId, Collections.singletonList(destDomainId), srcDomainId);
  }

  private List<Long> addAccessibleDomains(String userId, List<Long> accDomainIds,
                                          Long removeDomainId)
      throws ServiceException {
    PersistenceManager pm = null;
    try {
      IUserAccount ua = getUserAccount(userId);
      List<Long> uAccDids = ua.getAccessibleDomainIds();
      if (uAccDids == null || uAccDids.isEmpty()) {
        final Locale locale = ThreadLocalUtil.get().getSecureUserDetails().getLocale();
        ResourceBundle
            backendMessages =
            Resources.get().getBundle(Constants.BACKEND_MESSAGES, locale);
        xLogger
            .warn("Error while adding accessible domains for user {0}, uAccDids is null ", userId);
        throw new InvalidServiceException(
            backendMessages.getString("user.addaccessibledomain.error1") + " \'" + userId + "\'");
      }

      accDomainIds.stream().filter(accDid -> !uAccDids.contains(accDid)).forEach(uAccDids::add);

      if (removeDomainId != null) {
        uAccDids.remove(removeDomainId);
      }

      Set<Long> allChildrenIds = new HashSet<>();
      for (Long uAccDid : uAccDids) {
        List<IDomainLink>
            chldLnks =
            domainsService.getDomainLinks(uAccDid, IDomainLink.TYPE_CHILD, -1);
        if (chldLnks != null && !chldLnks.isEmpty()) {
          for (IDomainLink chldLnk : chldLnks) {
            allChildrenIds.add(chldLnk.getLinkedDomainId());
          }
        }
      }
      uAccDids.removeAll(allChildrenIds);
      pm = PMF.get().getPersistenceManager();
      pm.makePersistent(ua);
      //remove from cache
      removeUserFromCache(ua.getUserId());
      return uAccDids;
    } catch (Exception e) {
      throw new ServiceException(e);
    } finally {
      if (pm != null) {
        pm.close();
      }
    }
  }

  public List<Long> addAccessibleDomains(String userId, List<Long> accDomainIds)
      throws ServiceException {
    return addAccessibleDomains(userId, accDomainIds, null);
  }

  /**
   * Delete user accounts, given list of fully qualified user Ids (i.e. domainId.userId)
   */
  @SuppressWarnings("unchecked")
  public void deleteAccounts(Long domainId, List<String> accountIds, String sUser)
      throws ServiceException {
    xLogger.fine("Entering deleteAccounts");
    IUserAccount account;
    Exception exception = null;
    String userName;
    sUser = (sUser != null ? sUser : " ");
    PersistenceManager pm = PMF.get().getPersistenceManager();
    try {
      if (!authorizationService.authoriseUpdateKiosk(sUser, domainId)) {
        final Locale locale = ThreadLocalUtil.get().getSecureUserDetails().getLocale();
        ResourceBundle
            backendMessages =
            Resources.get().getBundle(Constants.BACKEND_MESSAGES, locale);
        throw new ForbiddenAccessException(backendMessages.getString("permission.denied"));
      }
      for (String accountId : accountIds) {
        try {
          xLogger.info("deleteAccounts: Deleting user {0}", accountId);
          account = JDOUtils.getObjectById(IUserAccount.class, accountId, pm);
          userName = account.getFullName();
          Query query = pm.newQuery(JDOUtils.getImplClass(IUserToKiosk.class));
          query.setFilter("userId == userIdParam");
          query.declareParameters("String userIdParam");
          try {
            List<IUserToKiosk> results = (List<IUserToKiosk>) query.execute(accountId);
            if (!results.isEmpty()) {
              xLogger.warn("deleteAccounts: Failed to delete user {0}. User is a kiosk owner.",
                  accountId);
              throw new ServiceException("USR002", accountId);
            } else {
              try {
                EventPublisher
                    .generate(domainId, IEvent.DELETED, null, UserAccount.class.getName(),
                        account.getKeyString(),
                        null, account);
              } catch (EventGenerationException e) {
                xLogger.warn(
                    "Exception when generating event for user-deletion for user {0} in domain {1}: {2}",
                    accountId, domainId, e.getMessage());
              }
              xLogger.fine("deleteAccounts: deleting user {0} from the database", accountId);
              pm.deletePersistent(account);
              removeUserFromCache(accountId);
              authenticationService.clearUserTokens(accountId, true);
              xLogger.info("AUDITLOG\t{0}\t{1}\tUSER\t " +
                  "DELETE\t{2}\t{3}", domainId, sUser, accountId, userName);
            }
          } finally {
            query.closeAll();
          }
        } catch (JDOObjectNotFoundException e) {
          xLogger.warn("deleteAccounts: FAILED to delete user {0}!! User does not exist", accountId);
          exception = e;
        }
      }
    } catch (Exception e) {
      exception = e;
      xLogger.warn(e.getMessage(), e);
    } finally {
      xLogger.fine("Exiting deleteAccounts");
      pm.close();
    }
    if (exception != null) {
      throw new ServiceException(exception);
    }
  }

  /**
   * Find user accounts that meet the given criteria. paramName can be one of the following:
   * JsonTags.ID (either regular or fully qualified user Ids)
   * JsonTags.MOBILE_PH_NO
   * JsonTags.COUNTRY
   * JsonTags.STATE
   * JsonTags.DISTRICT
   * JsonTags.TALUK
   * JsonTags.CITY
   */
  @SuppressWarnings("unchecked")
  private Results findAccountsByDomain(Long domainId, String paramName, String paramValue,
                                       String nameStartsWith, PageParams pageParams,
                                       boolean includeChildDomainUsers)
      throws ServiceException {
    xLogger.fine("Entering findAccountsByDomain");
    if (domainId == null) {
      throw new ServiceException("Invalid domain ID");
    }

    Results results = null;
    PersistenceManager pm = PMF.get().getPersistenceManager();
    Query q = pm.newQuery(JDOUtils.getImplClass(IUserAccount.class));
    String paramNameAlias = paramName + "Param";
    String filter = CharacterConstants.EMPTY;
    if (includeChildDomainUsers) {
      filter += "dId.contains(domainIdParam)";
    } else {
      filter += "sdId == domainIdParam";
    }
    filter += " && " + paramName + " == " + paramNameAlias;
    String declaration = "Long domainIdParam, String " + paramNameAlias;
    Map<String, Object> params = new HashMap<>(1);
    params.put("domainIdParam", domainId);
    params.put(paramNameAlias, paramValue);
    boolean hasNameStartsWith = nameStartsWith != null && !nameStartsWith.trim().isEmpty();
    params.put(paramNameAlias, paramValue);
    if (hasNameStartsWith) {
      filter += " && nName >= nNameParam1 && nName < nNameParam2";
      declaration += ", String nNameParam1, String nNameParam2";
      String lNameStartsWith = nameStartsWith.trim().toLowerCase();
      params.put("nNameParam1", lNameStartsWith);
      params.put("nNameParam2", (lNameStartsWith + Constants.UNICODE_REPLACEANY));
    }
    q.setFilter(filter);
    q.declareParameters(declaration);
    q.setOrdering("nName asc");
    if (pageParams != null) {
      QueryUtil.setPageParams(q, pageParams);
    }
    try {
      List<IUserAccount> users = (List<IUserAccount>) q.executeWithMap(params);
      int numFound = Counter.getCountByMap(q, params);
      String cursor = null;
      if (users != null) {
        users.size();
        cursor = QueryUtil.getCursor(users);
        users = (List<IUserAccount>) pm.detachCopyAll(users);
      }
      results = new Results(users, cursor, numFound, 0);
    } finally {
      try {
        q.closeAll();
      } catch (Exception ignored) {
        xLogger.warn("Exception while closing query", ignored);
      }
      pm.close();

    }
    xLogger.fine("Exiting findAccountsByDomain");
    return results;
  }

  /**
   * Find all user accounts with pagination, in a given domain
   * TODO Implement pagination
   */
  @SuppressWarnings("unchecked")
  private Results findAllAccountsByDomain(Long domainId, String nameStartsWith,
                                          PageParams pageParams, boolean includeChildDomainUsers,
                                          IUserAccount user)
      throws ServiceException {
    xLogger.fine("Entering findAllAccountsByDomain");

    if (domainId == null) {
      throw new ServiceException("Invalid domain Id");
    }

    boolean isSuperUsers = user.getRole().equals("ROLE_su");

    PersistenceManager pm = PMF.get().getPersistenceManager();
    Results results = null;
    Exception exception = null;
    Query query = pm.newQuery(JDOUtils.getImplClass(IUserAccount.class));
    String declaration = "Long domainIdParam";
    String filter = CharacterConstants.EMPTY;
    if (includeChildDomainUsers) {
      filter += "dId.contains(domainIdParam)";
    } else {
      filter += "sdId == domainIdParam";
    }

    boolean hasNameStartsWith = nameStartsWith != null && !nameStartsWith.trim().isEmpty();
    String lNameStartsWith = "";
    if (hasNameStartsWith) {
      filter += "&& nName >= nNameParam1 && nName < nNameParam2";
      declaration += ", String nNameParam1, String nNameParam2";
      lNameStartsWith = nameStartsWith.trim().toLowerCase();
    }
    if (!isSuperUsers) {
      filter = filter.concat(QueryConstants.AND).concat("role != 'ROLE_su'");
    }
    query.setFilter(filter);
    query.declareParameters(declaration);
    query.setOrdering("nName asc");
    if (pageParams != null) {
      QueryUtil.setPageParams(query, pageParams);
    }
    try {
      List<IUserAccount> users;
      if (hasNameStartsWith) {
        users = (List<IUserAccount>) query.execute(domainId, lNameStartsWith,
            (lNameStartsWith + Constants.UNICODE_REPLACEANY));
      } else {
        users = (List<IUserAccount>) query.execute(domainId);
      }
      String cursor = null;
      if (users != null) {
        users
            .size(); // TODO - temp. fix for retrieving all obejcts and avoid "object manager closed" exception
        cursor = QueryUtil.getCursor(users);
        users = (List<IUserAccount>) pm.detachCopyAll(users); //detach data
      }
      // Form the results along with cursor of the last result entry (for future queries, if present)
      results = new Results(users, cursor);
    } catch (Exception e) {
      exception = e;
      xLogger.warn("exception in findAllAccounts()", e);
    } finally {
      try {
        query.closeAll();
      } catch (Exception ignored) {
        xLogger.warn("Exception while closing query", ignored);
      }
      pm.close();
    }
    if (exception != null) {
      throw new ServiceException(exception);
    }

    xLogger.fine("Exiting findAllAccountsByDomain");
    return results;
  }

  @Override
  public boolean hasAccessToDomain(String username, Long domainId)
      throws ServiceException, ObjectNotFoundException {
    IUserAccount userAccount = getUserAccount(username);
    List<Long> accDomains = userAccount.getAccessibleDomainIds();
    Set<Long> allDomains = new HashSet<>();
    allDomains.add(userAccount.getDomainId());
    if(accDomains != null){
      allDomains.addAll(accDomains);
    }
    return SecurityUtil.compareRoles(SecurityConstants.ROLE_SUPERUSER, userAccount.getRole()) == 0
        || allDomains.contains(domainId) || domainsService.hasAncestor(domainId, allDomains);
  }


  public Set<String> getElementSetByUserFilter(Long domainId, IUserAccount user, String paramName,
                                               String paramValue, PageParams pageParams)
      throws ServiceException {
    xLogger.fine("Entering getElementSetByUserFilter");
    if (domainId == null) {
      throw new ServiceException("Invalid domain ID");
    }

    boolean isSuperUsers = user.getRole().equals("ROLE_su");
    paramName = paramName.trim();
    PersistenceManager pm = PMF.get().getPersistenceManager();
    Query q = pm.newQuery(" SELECT " + paramName + " FROM " + JDOUtils.getImplClass(IUserAccount.class).getName());
    Set<String> elementSet = new HashSet<>();
    StringBuilder filter = new StringBuilder();
    StringBuilder declaration = new StringBuilder();
    String order = "nName asc";
    filter.append("dId.contains(dIdParam) ");
    declaration.append("Long dIdParam");
    Map<String, Object> params;
    params = new HashMap<>();
    params.put("dIdParam", domainId);
    String paramNameAlias = paramName + "Param";
    paramValue = paramValue.trim();
    String lNameStartsWith = (paramName.equals("v") ? paramValue : paramValue.toLowerCase());
    filter.append(QueryConstants.AND)
        .append(paramName).append(QueryConstants.GR_EQUAL).append(paramNameAlias).append("1")
        .append(QueryConstants.AND).append(paramName).append(QueryConstants.LESS_THAN)
        .append(paramNameAlias).append("2");
    declaration.append(CharacterConstants.COMMA).append("String ").append(paramNameAlias)
        .append("1")
        .append(CharacterConstants.COMMA).append("String ").append(paramNameAlias).append("2");
    params.put(paramNameAlias + "1", lNameStartsWith);
    params.put(paramNameAlias + "2", lNameStartsWith + Constants.UNICODE_REPLACEANY);
    if (!paramName.equals("nName")) {
      order = paramName + " asc, " + order;
    }
    if (!isSuperUsers) {
      filter.append(QueryConstants.AND).append("role != 'ROLE_su'");
    }
    q.setFilter(filter.toString());
    q.declareParameters(declaration.toString());
    q.setOrdering(order);
    if (pageParams != null) {
      QueryUtil.setPageParams(q, pageParams);
    }
    try {
      List elements = (List) q.executeWithMap(params);
      if (elements != null) {
        for (Object o : elements) {
          elementSet.add((String) o);
        }
      }
    } finally {
      try {
        q.closeAll();
      } catch (Exception ignored) {
        xLogger.warn("Exception while trying to close query", ignored);
      }
      pm.close();
    }
    xLogger.fine("Exiting getElementSetByUserFilter");
    return elementSet;
  }

  /**
   * update the lastmobileAccess time
   *
   * @param userId user id
   * @param aTime  accessed time
   */
  @Override
  public boolean updateLastMobileAccessTime(String userId, long aTime) {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    IUserAccount userAccount;
    Date now = new Date(aTime);
    try {
      userAccount = JDOUtils.getObjectById(IUserAccount.class, userId, pm);
      if (userAccount.getLastMobileAccessed() == null
          || userAccount.getLastMobileAccessed().compareTo(now) < 0) {
        userAccount.setLastMobileAccessed(now);
      }
    } catch (Exception e) {
      xLogger
          .warn("{0} while updating last transacted time for the user, {1}", e.getMessage(), userId,
              e);
      return false;
    } finally {
      pm.close();
    }
    return true;
  }

  /**
   * Get the list of enabled userIds from the given userIds
   */
  public List<String> getEnabledUserIds(List<String> uIds) {
    if (!CollectionUtils.isEmpty(uIds)) {
      PersistenceManager pm = PMF.get().getPersistenceManager();
      try {
        List<String> eUids = new ArrayList<>(1);
        for (String uid : uIds) {
          try {
            IUserAccount u = JDOUtils.getObjectById(IUserAccount.class, uid, pm);
            if (u.isEnabled()) {
              eUids.add(uid);
            }
          } catch (Exception e) {
            xLogger.warn("Error while getting enabled user {0}", uid, e);
          }
        }
        return eUids;
      } finally {
        pm.close();
      }
    }
    return null;
  }

  /**
   * Get the list of enabled userIds from the given tagNames
   */
  public List<String> getEnabledUserIdsWithTags(List<String> tagNames, Long domainId) {
    List<String> uIds = null;
    if (!CollectionUtils.isEmpty(tagNames)) {
      PersistenceManager pm = PMF.get().getPersistenceManager();
      List<Object> parameters=new ArrayList<>();
      StringBuilder query = new StringBuilder("SELECT UA.USERID FROM USERACCOUNT UA,USER_TAGS UT WHERE "
          + "UT.ID IN (SELECT ID FROM TAG WHERE NAME IN (");
      for (String name : tagNames) {
        query.append(CharacterConstants.QUESTION).append(CharacterConstants.COMMA);
        parameters.add(name);
      }
      query.setLength(query.length() - 1);
      query.append(") AND TYPE=4) AND UT.USERID = UA.USERID AND UA.ISENABLED = 1 AND UA.SDID = ?");
      parameters.add(domainId);
      Query q = pm.newQuery(Constants.JAVAX_JDO_QUERY_SQL, query.toString());
      try {
        List l = (List) q.executeWithArray(parameters.toArray());
        uIds = new ArrayList<>(l.size());
        for (Object o : l) {
          uIds.add((String) o);
        }
      } catch (Exception e) {
        xLogger.warn("Error while getting enabled user by tags {0}", tagNames.toString(), e);
      } finally {
        try {
          q.closeAll();
        } catch (Exception ignored) {
          xLogger.warn("Exception while closing query", ignored);
        }
        pm.close();
      }
    }
    return uIds;
  }

  private Results findAccountsByFilter(Long domainId, Map<String, Object> filters,
                                       PageParams pageParams, boolean excluderSuUser)
      throws ServiceException {
    xLogger.fine("Entering findAccountsByFilter");
    if (domainId == null) {
      throw new ServiceException("Invalid domain ID");
    }
    Results results = null;
    QueryParams qp;
    if (excluderSuUser) {
      qp = userDao.getQueryParams(domainId, filters, true, true);
    } else {
      qp = userDao.getQueryParams(domainId, filters, true);
    }
    if (qp == null) {
      throw new ServiceException("Error while creating query parameters");
    }
    PersistenceManager pm = PMF.get().getPersistenceManager();
    Query q = pm.newQuery(qp.query);
    if (pageParams != null) {
      QueryUtil.setPageParams(q, pageParams);
    }
    try {
      List<IUserAccount> users;
      if(MapUtils.isNotEmpty(qp.params)) {
        users = (List<IUserAccount>) q.executeWithMap(qp.params);
      } else {
        users = (List<IUserAccount>) q.executeWithArray(qp.listParams);
      }
      String cursor = null;
      if (users != null) {
        users.size();
        cursor = QueryUtil.getCursor(users);
        users = (List<IUserAccount>) pm.detachCopyAll(users);
      }
      // Get the results along with cursor, if present
      results = new Results<>(users, cursor, Counter.getCountByMap(q, qp.params), 0);
    } finally {
      try {
        q.closeAll();
      } catch (Exception ignored) {
        xLogger.warn("Exception while trying to close query", ignored);
      }
      pm.close();
    }
    xLogger.fine("Exiting findAccountsByFilter");
    return results;
  }

  /**
   * Check if a custom ID is available in a domain or it's child domains
   */
  public boolean customIdExists(Long domainId, String customId, String userId)
      throws ServiceException {
    if (domainId == null || customId == null || customId.isEmpty() || userId == null || userId
        .isEmpty()) {
      throw new ServiceException("Invalid or null Domain ID: {0}, custom ID : {1}, user ID: {2}",
          domainId, customId, userId);
    }
    PersistenceManager pm = PMF.get().getPersistenceManager();
    Query query = pm.newQuery(JDOUtils.getImplClass(IUserAccount.class));
    query.setFilter("dId.contains(domainIdParam) && cId == cidParam");
    query.declareParameters("Long domainIdParam, String cidParam");
    try {
      List<IUserAccount> results = (List<IUserAccount>) query.execute(domainId, customId);
      if (results != null && results.size() == 1) {
        IUserAccount res = results.get(0);
        return !userId.equals(res.getUserId());
      }
    } finally {
      try {
        query.closeAll();
      } catch (Exception ignored) {
        xLogger.warn("Exception while closing query", ignored);
      }
      pm.close();
    }
    return false;
  }

  /**
   * This method will update applicable location ids for an user
   */
  private void updateUserLocationIds(IUserAccount user) {
    LocationResponseModel response = locationClient.getLocationIds(user);
    user.setCountryId(response.getCountryId());
    user.setStateId(response.getStateId());
    user.setDistrictId(response.getDistrictId());
    user.setTalukId(response.getTalukId());
    user.setCityId(response.getCityId());
  }

  /**
   * Set the UI preference for a user. If true, then it means his preference is New UI. Otherwise, preference is Old UI.
   */
  public void setUiPreferenceForUser(String userId, boolean uiPref) throws ServiceException {
    // Get the user account for userId
    // Set the uiPref
    xLogger.fine("Entering setUiPreferenceForUser");
    if (userId == null) {
      throw new ServiceException("Invalid user Id");
    }

    boolean userExists = true;
    String errMsg = null;
    Exception exception = null;
    PersistenceManager pm = PMF.get().getPersistenceManager();

    try {
      try {
        //First check if the user exists in the database.
        IUserAccount user = JDOUtils.getObjectById(IUserAccount.class, userId, pm);
        // Only if the user exists, set the Ui preference.
        user.setUiPref(uiPref);
        //remove from cache
        removeUserFromCache(userId);
      } catch (JDOObjectNotFoundException e) {
        xLogger.warn("setUiPreferenceForUser: FAILED!! User {0} does not exist in the database",
            userId);
        userExists = false;
        exception = e;
      }
    } catch (Exception e) {
      exception = e;
      errMsg = e.getMessage();
    } finally {
      pm.close();
    }
    if (!userExists) {
      final Locale locale = ThreadLocalUtil.get().getSecureUserDetails().getLocale();
      ResourceBundle messages = Resources.get().getBundle(Constants.MESSAGES, locale);
      ResourceBundle
          backendMessages =
          Resources.get().getBundle(Constants.BACKEND_MESSAGES, locale);
      errMsg = messages.getString("user") + " '" + userId + "' " + backendMessages
          .getString("error.notfound");
    }
    if (errMsg != null) {
      throw new ServiceException(errMsg, exception);
    }
  }

  @Override
  public void addEditUserDevice(UserDeviceModel ud) throws ServiceException {
    UserDeviceBuilder builder = new UserDeviceBuilder();
    IUserDevice userDevice = getUserDevice(ud.getUserId(), ud.getAppName());
    userDevice = builder.buildUserDevice(userDevice, ud);
    PersistenceManager pm = PMF.get().getPersistenceManager();
    try {
      pm.makePersistent(userDevice);
    } catch (Exception e) {
      xLogger.warn("Issue with adding user firebase token {0}", e.getMessage(),e);
      throw new ServiceException("UD002", ud.getUserId());
    } finally {
      pm.close();
    }
  }

  @Override
  public IUserDevice getUserDevice(String userid, String appname) throws ServiceException {

    PersistenceManager pm = null;
    try {
      pm = PMF.get().getPersistenceManager();
      Query query = pm.newQuery(JDOUtils.getImplClass(IUserDevice.class));
      query.setFilter("userId == userIdParam && appname == appnameParam");
      query.declareParameters("String userIdParam, String appnameParam");
      query.setUnique(true);
      IUserDevice userDevice = (IUserDevice) query.execute(userid, appname);
      return pm.detachCopy(userDevice);
    } catch (Exception e) {
      xLogger.severe("Issue while getting firebase token for user {0}", userid, e);
      throw new ServiceException("UD001",userid);
    } finally {
      if (pm != null) {
        pm.close();
      }
    }
  }

  private boolean checkIfCustomIdExists(IUserAccount userAccount) {
    xLogger.fine("Entering checkIfCustomIdExists");
    boolean customIdExists = false;
    xLogger.fine("object is an instance of UserAccount");
    Long domainId = userAccount.getDomainId();
    PersistenceManager pm = PMF.get().getPersistenceManager();
    // Check if another user by the same custom ID exists in the database
    Query query = pm.newQuery(JDOUtils.getImplClass(IUserAccount.class));
    query.setFilter("dId.contains(domainIdParam) && cId == cidParam");
    query.declareParameters("Long domainIdParam, String cidParam");
    try {
      @SuppressWarnings("unchecked")
      List<IUserAccount>
          results =
          (List<IUserAccount>) query.execute(domainId, userAccount.getCustomId());
      if (results != null && results.size() == 1) {
        // UserAccount with this custom id already exists in the database!
        xLogger.warn(
            "Error while adding or updating user Account {0}: Custom ID {1} already exists.",
            userAccount.getUserId(), userAccount.getCustomId());
        customIdExists = true;
      }
    } finally {
      try {
        query.closeAll();
      } catch (Exception ignored) {
        xLogger.warn("Exception while closing query", ignored);
      }
      pm.close();
    }
    return customIdExists;
  }

  // Sort user data
  private static void sortUsers(List<IUserAccount> users) {
    Collections.sort(users, (o1, o2) -> o1.getFirstName().compareTo(o2.getFirstName()));
  }

  @Override
  public boolean hasAccessToUser(String userId, String rUserId, Long domainId, String role) {
    Map<String, Object> params = new HashMap<>(2);
    params.put("domainIdParam", domainId);
    params.put("userIdParam", userId);
    String queryStr;
    if (userId != null && userId.equals(rUserId)) {
      return true;
    } else if (SecurityUtil.compareRoles(role, SecurityConstants.ROLE_DOMAINOWNER) >= 0) {
      //Todo: Need to check user domainId and all its children domains
      queryStr =
          "SELECT userId FROM " + JDOUtils.getImplClass(IUserAccount.class).getName()
              + " WHERE userId == userIdParam && dId.contains(domainIdParam)" +
              " PARAMETERS String userIdParam, Long domainIdParam";
    } else {
      queryStr =
          "SELECT userId FROM " + JDOUtils.getImplClass(IUserAccount.class).getName()
              + " WHERE userId == userIdParam && registeredBy == registeredByParam && sdId == domainIdParam"
              +
              " PARAMETERS String userIdParam, String registeredByParam, Long domainIdParam";
      params.put("registeredByParam", rUserId);
    }
    return getAccess(params, queryStr);
  }

  private boolean getAccess(Map<String, Object> params, String queryStr) {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    boolean hasAccess = false;
    List<Long> results;
    Query q = null;
    try {
      q = pm.newQuery(queryStr);
      results = (List<Long>) q.executeWithMap(params);
      if (results != null && results.size() == 1) {
        hasAccess = true;
      }
    } finally {
      if (q != null) {
        try {
          q.closeAll();
        } catch (Exception ignored) {
          xLogger.warn("Exception while closing query", ignored);
        }
      }
      pm.close();
    }
    return hasAccess;
  }

  /**
   * Method to fetch the user account details for the given userIds
   *
   * @param userIds User Id list
   * @return List<IUserAccount>
   */
  public List<IUserAccount> getUsersByIds(List<String> userIds) {
    if (userIds == null || userIds.isEmpty()) {
      return null;
    }
    List<IUserAccount> results = null;
    PersistenceManager pm = PMF.get().getPersistenceManager();
    Query query = null;
    try {
      List<String> parameters = new ArrayList<>();
      StringBuilder queryBuilder = new StringBuilder("SELECT * FROM `USERACCOUNT` ");
      queryBuilder.append("WHERE USERID IN (");
      for (String userId : userIds) {
        queryBuilder.append(CharacterConstants.QUESTION).append(CharacterConstants.COMMA);
        parameters.add(userId);

      }
      queryBuilder.setLength(queryBuilder.length() - 1);
      queryBuilder.append(" )");
      query = pm.newQuery(Constants.JAVAX_JDO_QUERY_SQL, queryBuilder.toString());
      query.setClass(JDOUtils.getImplClass(IUserAccount.class));
      results = (List<IUserAccount>) query.executeWithArray(parameters.toArray());
      results = (List<IUserAccount>) pm.detachCopyAll(results);
    } catch (Exception e) {
      xLogger.warn("Exception while fetching approval status", e);
    } finally {
      if (query != null) {
        try {
          query.closeAll();
        } catch (Exception ignored) {
          xLogger.warn("Exception while closing query", ignored);
        }
      }
      pm.close();
    }
    return results;
  }

  public List<IUserAccount> getUsersByTag(Long objectId, String objectType, List<String> userTags)
      throws ServiceException {
    String tagQueryParam = StringUtil.getSingleQuotesCSV(userTags);
    List<String> parameters = new ArrayList<>();
    StringBuilder queryString = new StringBuilder();
    if (org.springframework.util.StringUtils.isEmpty(objectType)
        || "store".equalsIgnoreCase(objectType)) {
      queryString.append(
          "SELECT * FROM USERACCOUNT WHERE USERID IN (SELECT USERID FROM USERTOKIOSK WHERE KIOSKID = ?");
      parameters.add(String.valueOf(objectId));
    } else if ("domain".equalsIgnoreCase(objectType)) {
      queryString.append("SELECT * FROM USERACCOUNT WHERE SDID = ? ");
      parameters.add(String.valueOf(objectId));
    }
    if (StringUtils.isNotEmpty(tagQueryParam)) {
      queryString.append(
          " AND USERID IN (SELECT USERID FROM USER_TAGS WHERE ID IN (SELECT ID FROM TAG WHERE TYPE = ?")
          .append(
              " AND NAME IN(").append(tagQueryParam).append(")))");
      parameters.add(String.valueOf(ITag.USER_TAG));
    }
    if (org.springframework.util.StringUtils.isEmpty(objectType)
        || "store".equalsIgnoreCase(objectType)) {
      queryString.append(CharacterConstants.C_BRACKET);
    }
    PersistenceManager pm = getPM();
    Query query = pm.newQuery(Constants.JAVAX_JDO_QUERY_SQL, queryString.toString());
    query.setClass(JDOUtils.getImplClass(IUserAccount.class));
    try {
      List<IUserAccount>
          results =
          (List<IUserAccount>) query.executeWithArray(parameters.toArray());
      results = (List<IUserAccount>) pm.detachCopyAll(results);
      return results;
    } catch (Exception e) {
      xLogger
          .warn("Error while fetching users for kiosk: {0} for user tag: {1}", objectId, userTags,
              e);
      throw e;
    } finally {
      query.closeAll();
      pm.close();
    }
  }

  public List<ExtUserAccount> eligibleUsersForEventNotification(Long domainId) {

    List<String> parameters = new ArrayList<>();
    List<ExtUserAccount> users = new ArrayList<>();
    String querystr = "SELECT U.USERID, U.SDID, U.MOBILEPHONENUMBER, U.EMAIL, UD.TOKEN FROM USERACCOUNT U,"
        + " USERDEVICE UD WHERE U.USERID = UD.USERID AND U.SDID = ? AND UD.APPNAME = ? AND U.USERID IN "
        + " (SELECT USERID FROM USERLOGINHISTORY WHERE LGSRC = ? AND STATUS = ?)";
    parameters.add(String.valueOf(domainId));
    parameters.add(MMA);
    parameters.add(String.valueOf(SourceConstants.MMA));
    parameters.add(SUCCESS);
    PersistenceManager pm = getPM();
    Query query = pm.newQuery(Constants.JAVAX_JDO_QUERY_SQL, querystr);
    try {
      List list = (List) query.executeWithArray(parameters.toArray());
      for (Object st : list) {
        ExtUserAccount model = new ExtUserAccount();
        model.setUserId((String) ((Object[]) st)[0]);
        model.setSdId((Long)((Object[]) st)[1]);
        model.setMobilePhoneNumber((String)((Object[]) st)[2]);
        model.setEmail((String) ((Object[]) st)[3]);
        model.setToken((String) ((Object[]) st)[4]);
        users.add(model);
      }
      return users;
    } catch (Exception e) {
      xLogger
          .warn("Error while fetching eligible users for event notification for domain: {0}", domainId, e);
      throw e;
    } finally {
      query.closeAll();
      pm.close();
    }
  }

  protected PersistenceManager getPM() {
    return PMF.get().getPersistenceManager();
  }

  private void removeUserFromCache(String userId) {
    getMemcacheService().delete(Constants.USER_KEY + CharacterConstants.HASH + userId);
  }
}
