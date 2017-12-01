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

package com.logistimo.social.provider;

import com.google.gson.GsonBuilder;

import com.logistimo.collaboration.core.events.LikeRegisteredEvent;
import com.logistimo.config.models.AdminContactConfig;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.config.models.EventSummaryConfigModel;
import com.logistimo.services.ServiceException;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.users.service.impl.UsersServiceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Created by kumargaurav on 27/11/17.
 */
@Component
public class SubscriberProvider {

  UsersServiceImpl usersService;

  @Autowired
  public void setUsersService(UsersServiceImpl usersService) {
    this.usersService = usersService;
  }

  public List<IUserAccount> getSubscriber(LikeRegisteredEvent event) throws ServiceException {
    if ("domain".equalsIgnoreCase(event.getObjectType())) {
      //check for domain config to determine domain level (i.e. country , state or district)
      return getDomainContacts(event.getObjectId());
    } else if ("store".equalsIgnoreCase(event.getObjectType())) {
      return getStoreContacts(event.getObjectId(),
          getStoreUserTagForEvent(event.getContextAttributes()));
    }
    return Collections.emptyList();
  }

  private List<String> getStoreUserTagForEvent(String contextAttribute) {
    EventSummaryConfigModel.Threshold
        threshold =
        new GsonBuilder().create()
            .fromJson(contextAttribute, EventSummaryConfigModel.Threshold.class);
    Optional<EventSummaryConfigModel.Condition> cond = threshold.getConditions().stream().
        filter(condition -> condition.getName().equalsIgnoreCase("user_tags_responsible"))
        .findFirst();
    if (cond.isPresent()) {
      return cond.get().getValues();
    }
    return Collections.emptyList();
  }

  private List<IUserAccount> getDomainContacts(String domainId) throws ServiceException {

    long dId = Long.valueOf(domainId);
    DomainConfig dc = DomainConfig.getInstance(dId);
    AdminContactConfig acc = dc.getAdminContactConfig();
    List<String> adminUserdIds = new ArrayList<>();
    if (acc != null) {
      if (!StringUtils.isEmpty(acc.getPrimaryAdminContact())) {
        adminUserdIds.add(acc.getPrimaryAdminContact());
      }
      if (!StringUtils.isEmpty(acc.getSecondaryAdminContact())) {
        adminUserdIds.add(acc.getSecondaryAdminContact());
      }
    }
    if (adminUserdIds.isEmpty()) {
      return Collections.emptyList();
    } else {
      return usersService
          .getUsersByIds(adminUserdIds);
    }
  }

  private List<IUserAccount> getStoreContacts(String storeId, List<String> userTags)
      throws ServiceException {
    return usersService.getUsersByTag(Long.valueOf(storeId), "store", userTags);
  }

}
