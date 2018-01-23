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

package com.logistimo.api.servlets.mobile.builders;

import com.logistimo.logger.XLog;
import com.logistimo.proto.MobileUserModel;
import com.logistimo.services.ObjectNotFoundException;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.users.service.UsersService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by vani on 28/06/17.
 */
@Component
public class MobileUserBuilder {

  private UsersService usersService;

  @Autowired
  public void setUsersService(UsersService usersService) {
    this.usersService = usersService;
  }

  private static final XLog LOGGER = XLog.getLog(MobileUserBuilder.class);
  /**
   * Builds a list of user models as required by the mobile from a list of user account objects
   * @param users
   */
  public List<MobileUserModel> buildMobileUserModels(List<IUserAccount> users) {
    if (users == null || users.isEmpty()) {
      return null;
    }
    return (users.stream()
        .map(this::buildMobileUserModel)
        .collect(Collectors.toList()));
  }

  private MobileUserModel buildMobileUserModel(IUserAccount user) {
    MobileUserModel mobileUserModel = new MobileUserModel();
    mobileUserModel.uid = user.getUserId();
    mobileUserModel.mob = user.getMobilePhoneNumber();
    mobileUserModel.eml = user.getEmail();
    mobileUserModel.n = user.getFullName();
    return mobileUserModel;
  }

  /**
   * Constructs a list of UserAccount objects from a list of user ids
   * @param userIds
   */
  public List<IUserAccount> constructUserAccount(List<String> userIds) {
    if (userIds == null || userIds.isEmpty()) {
      return null;
    }
    return (userIds.stream()
        .map(userId -> getUserAccount(usersService, userId))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList()));
  }

  /**
   * Returns an Optional user account object for a specified user id or an empty object if the user account does not exist.
   */
  private Optional<IUserAccount> getUserAccount(UsersService usersService, String userId) {
    try {
      return Optional.of(usersService.getUserAccount(userId));
    } catch (ObjectNotFoundException e) {
      LOGGER.warn("Exception while getting user account for user {0}", userId, e);
    }
    return Optional.empty();
  }
}
