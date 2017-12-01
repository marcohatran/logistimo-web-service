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

import com.logistimo.config.models.EventSummaryConfigModel;
import com.logistimo.domains.service.impl.DomainsServiceImpl;
import com.logistimo.entities.service.EntitiesServiceImpl;
import com.logistimo.exception.SystemException;
import com.logistimo.logger.XLog;
import com.logistimo.services.ServiceException;
import com.logistimo.collaboration.core.models.ContextModel;
import com.logistimo.social.model.ContentQuerySpecs;
import com.logistimo.social.util.CollaborationDomainUtil;
import com.logistimo.social.util.CollaborationMessageUtil;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.users.service.impl.UsersServiceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Optional;

/**
 * Created by kumargaurav on 27/11/17.
 */
@Component
public class ContentProvider {

  private static final XLog log = XLog.getLog(ContentProvider.class);

  UsersServiceImpl usersService;

  DomainsServiceImpl domainsService;

  EntitiesServiceImpl entitiesService;

  @Autowired
  public void setUsersService(UsersServiceImpl usersService) {
    this.usersService = usersService;
  }

  @Autowired
  public void setDomainsService(DomainsServiceImpl domainsService) {
    this.domainsService = domainsService;
  }

  @Autowired
  public void setEntitiesService(EntitiesServiceImpl entitiesService) {
    this.entitiesService = entitiesService;
  }

  public String generateContent(ContentQuerySpecs query) {

    if(!"event".equals(query.getContextType())) {
      return "";
    }
    String user = query.getUser();
    IUserAccount userAccount = usersService.getUserAccount(user);
    Long dId = userAccount.getDomainId();
    ContextModel eventContext = CollaborationDomainUtil.getEventContext(query.getContextId(), dId);
    String
        mainContent =
        CollaborationMessageUtil.constructMessage(
            eventContext.getCategory() + "." + eventContext.getEventType() + ".text"
            , userAccount.getLocale()
            , new Object[]{userAccount.getFullName(),
                getObjectText(query.getObjectId(), query.getObjectType()),getDuration(eventContext,userAccount.getLocale())});
    return mainContent;
  }

  private String getObjectText(String objectId, String objectType) {
    StringBuilder sb = new StringBuilder();
    String name = null;
    try {
      if ("domain".equalsIgnoreCase(objectType)) {
        name = domainsService.getDomain(Long.valueOf(objectId)).getNormalizedName();
      } else if ("store".equalsIgnoreCase(objectType)) {
        name = entitiesService.getKiosk(Long.valueOf(objectId)).getName();
      }
    } catch (ServiceException se) {
      log.severe("Error with getting name for collaboration object with type {0} and id {1}",objectType,objectId,se);
       throw new SystemException(se,"CL004",objectType,objectId);
    }
    sb.append(name).append("\'s");
    return sb.toString();
  }

  private String getDuration(ContextModel eventContext,Locale locale) {
    EventSummaryConfigModel.Threshold
        threshold =
        new GsonBuilder().create()
            .fromJson(eventContext.getAttribute(), EventSummaryConfigModel.Threshold.class);
    String event = eventContext.getEventType();
    String category = eventContext.getCategory();

    if ("inventory".equals(category) && "inventory_performance_by_entity".equals(event)){
      return getConditionValueWithUnit(threshold,"duration",locale);
    } else if ("assets".equals(category) && "asset_performance_by_entity".equals(event)) {
      return getConditionValueWithUnit(threshold,"duration",locale);
    } else if ("activity".equals(category) && "data_entry_performance_by_entity".equals(event)) {
      return getConditionValueWithUnit(threshold,"period",locale);
    } else if ("supply".equals(category) && "supply_performance".equals(event)) {
      return getConditionValueWithUnit(threshold,"period",locale);
    }
    return "";
  }


  private String getConditionValueWithUnit (EventSummaryConfigModel.Threshold threshold, String name, Locale locale) {
    Optional<EventSummaryConfigModel.Condition> condition = threshold.getConditions().stream()
        .filter(cond -> cond.getName().equalsIgnoreCase(name))
        .findFirst();
    String ret = null;
    if (condition.isPresent()) {
      EventSummaryConfigModel.Condition conditionObj = condition.get();
      if (conditionObj.getValue() != null && !StringUtils.isEmpty(conditionObj.getValue())) {
        ret =  conditionObj.getValue();
      }
    }
    if (!StringUtils.isEmpty(ret)) {
      try {
        Integer count = Integer.parseInt(ret);
        if (count <= 1) {
          return CollaborationMessageUtil.constructMessage("months.singular.text",locale, new Object[]{count});
        } else {
          return CollaborationMessageUtil.constructMessage("months.plural.text",locale,new Object[]{count});
        }
      } catch (NumberFormatException ex) {
        return "";
      }
    }
    return "";
  }

}
