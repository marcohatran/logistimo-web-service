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

package com.logistimo.entities.auth;

import com.logistimo.auth.GenericAuthoriser;
import com.logistimo.auth.SecurityConstants;
import com.logistimo.auth.SecurityUtil;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.context.StaticApplicationContext;
import com.logistimo.entities.entity.IKioskLink;
import com.logistimo.entities.service.EntitiesService;
import com.logistimo.entities.service.EntitiesServiceImpl;
import com.logistimo.exception.UnauthorizedException;
import com.logistimo.pagination.Results;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.ObjectNotFoundException;
import com.logistimo.services.ServiceException;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.users.service.UsersService;
import com.logistimo.users.service.impl.UsersServiceImpl;

import java.util.List;

/**
 * Created by Mohan Raja on 10/03/15
 */

public class EntityAuthoriser {

  private EntityAuthoriser() {
  }

  public static boolean authoriseEntity(Long entityId)
      throws ServiceException {
    return authoriseEntityPerm(entityId) > 0;
  }

  public static Integer authoriseEntityPerm(Long entityId)
      throws ServiceException {
    SecureUserDetails secureUserDetails = SecurityUtils.getUserDetails();
    String role = secureUserDetails.getRole();
    if (SecurityConstants.ROLE_SUPERUSER.equals(role)) {
      return GenericAuthoriser.MANAGE_MASTER_DATA;
    }
    String userId = secureUserDetails.getUsername();
    UsersService as = StaticApplicationContext.getBean(UsersServiceImpl.class);
    try {
      IUserAccount account = as.getUserAccount(userId);
      List<Long> domainIds = account.getAccessibleDomainIds();
      if (domainIds != null && domainIds.size() > 0) {
        for (Long dId : domainIds) {
          Integer permission = authoriseEntityPerm(entityId, role, userId, dId);
          if (permission > GenericAuthoriser.NO_ACCESS) {
            return permission;
          }
        }
      }
    } catch (ObjectNotFoundException e) {
      throw new ServiceException("User not found: " + userId);
    }

    return GenericAuthoriser.NO_ACCESS;
  }

  public static boolean authoriseEntity(Long entityId, String role, String userId,
                                        Long domainId) throws ServiceException {
    return authoriseEntityPerm(entityId, role, userId, domainId) > 0;
  }

  public static Integer authoriseEntityPerm(Long entityId, String role,
                                            String userId, Long domainId) throws ServiceException {
    Integer permission = GenericAuthoriser.NO_ACCESS;
    if (SecurityConstants.ROLE_SUPERUSER.equals(role)) {
      return GenericAuthoriser.MANAGE_MASTER_DATA;
    }
    EntitiesService as = StaticApplicationContext.getBean(EntitiesServiceImpl.class);
    boolean hasPermission = as.hasAccessToKiosk(userId, entityId, domainId, role);
    if (hasPermission) {
      permission = GenericAuthoriser.MANAGE_MASTER_DATA;
    }
    // Todo: Need to authorize entity for manager by some other way like using query (If possible validate inside hasAccessTokiosk method itself)
    if (!hasPermission && (SecurityConstants.ROLE_SERVICEMANAGER.equals(role)
        || SecurityConstants.ROLE_KIOSKOWNER.equals(role))) {
      try {
        permission = as.hasAccessToKiosk(userId, entityId);

      } catch (Exception ignored) { // Proceed to return false
        // do nothing
      }
    }
    return permission;
  }

  public static boolean authoriseEntityDomain(SecureUserDetails secureUserDetails, Long entityId, Long domainId)
      throws ServiceException {
    String role = secureUserDetails.getRole();
    EntitiesService as = StaticApplicationContext.getBean(EntitiesServiceImpl.class);
    return role.equals(SecurityConstants.ROLE_SUPERUSER) || as.getKiosk(entityId).getDomainIds()
        .contains(domainId);
  }


  public static boolean authoriseInventoryAccess(SecureUserDetails secureUserDetails, Long entityId)
      throws ServiceException {
    EntitiesService as = StaticApplicationContext.getBean(EntitiesServiceImpl.class);
    if (SecurityUtil.compareRoles(secureUserDetails.getRole(), SecurityConstants.ROLE_DOMAINOWNER) < 0 && !as
        .hasKiosk(secureUserDetails.getUsername(), entityId)) {
      Results results = as.getKioskIdsForUser(secureUserDetails.getUsername(), null, null);
      if (results != null && results.getSize() > 0) {
        List<Long> userEntities = results.getResults();
        for (Long userEntityId : userEntities) {
          if (as.hasKioskLink(userEntityId, IKioskLink.TYPE_VENDOR, entityId) || as
              .hasKioskLink(userEntityId, IKioskLink.TYPE_CUSTOMER, entityId)) {
            return true;
          }
        }
      }
      return false;
    }
    return true;
  }


  public static boolean check(Long entityId) {
    try {
      return authoriseEntity(entityId);
    } catch (ServiceException e) {
      throw new UnauthorizedException("G002");
    }
  }
}
