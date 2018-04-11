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

package com.logistimo.stockrebalancing.actions;

import com.logistimo.auth.SecurityConstants;
import com.logistimo.communications.MessageHandlingException;
import com.logistimo.communications.service.MessageService;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.constants.Constants;
import com.logistimo.context.StaticApplicationContext;
import com.logistimo.domains.service.DomainsService;
import com.logistimo.logger.XLog;
import com.logistimo.pagination.Results;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;
import com.logistimo.stockrebalancing.entity.StockRebalancingEvent;
import com.logistimo.stockrebalancing.models.ExecutionMetadataModel;
import com.logistimo.stockrebalancing.models.StockRebalancingFilters;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.users.service.UsersService;

import org.apache.camel.Handler;
import org.apache.commons.lang.text.StrSubstitutor;
import org.springframework.data.domain.Page;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.transaction.Transactional;

/**
 * Created by charan on 03/04/18.
 */
public class StockRebalancingRunNotificationAction {

  private static final XLog xLogger = XLog.getLog(StockRebalancingRunNotificationAction.class);
  private static final String INVENTORY_COUNT = "inventoryCount";
  private static final String DOMAIN_NAME = "domainName";
  private static final String MESSAGES = "Messages";
  private static final String STOCKREBALANCING_ADMIN_NOTIFICATION = "stockrebalancing.admin.notification";

  @Handler
  @Transactional
  public void execute(ExecutionMetadataModel executionMetadataModel) {
    xLogger.info("Stock rebalancing run completed for - {0}", executionMetadataModel);
    GetStockRebalancingEventsAction getStockRebalancingEventsAction =
        StaticApplicationContext.getBean(GetStockRebalancingEventsAction.class);
    Page<StockRebalancingEvent> events = getStockRebalancingEventsAction.invoke(
        StockRebalancingFilters.builder().executionId(executionMetadataModel.getExecutionId())
            .build(), 0, 1);
    if (events.getSize() > 0 && events.getTotalElements() > 0) {
      DomainConfig domainConfig = DomainConfig.getInstance(executionMetadataModel.getDomainId());
      UsersService userService = StaticApplicationContext.getBean(UsersService.class);
      try {
        Results<IUserAccount> users = userService.getUsers(executionMetadataModel.getDomainId(),
            SecurityConstants.ROLE_DOMAINOWNER, true, null, null);
        if (users != null && users.getNumFound() > 0) {
          String message = getMessage(executionMetadataModel, events, domainConfig);
          users.getResults().stream().forEach(user -> {
            try {
              MessageService messageService = MessageService.getInstance(MessageService.SMS,
                  user.getCountry(), true, executionMetadataModel.getDomainId(),
                  Constants.SYSTEM_USER_ID, null);
              messageService.send(user, message,
                  MessageService.getMessageType(message), null, null, null);
            } catch (MessageHandlingException | IOException e) {
              xLogger.severe("Unable to notify user {0}({1}) for run {2}", user.getFullName(),
                  user.getUserId(), executionMetadataModel, e);
            }
          });

        } else {
          xLogger.warn("No admin users to notify for run {0}", executionMetadataModel);
        }
      } catch (ServiceException e) {
        xLogger.severe("Error while notifying for run {0}", executionMetadataModel);
      }
    } else {
      xLogger.info("No events were triggered for run {0}", executionMetadataModel);
    }

  }

  private String getMessage(ExecutionMetadataModel executionMetadataModel,
      Page<StockRebalancingEvent> events, DomainConfig domainConfig) {
    ResourceBundle messages =
        Resources.get().getBundle(MESSAGES, new Locale(domainConfig.getLangPreference()));
    String message = messages.getString(STOCKREBALANCING_ADMIN_NOTIFICATION);
    Map<String, String> values = new HashMap<>();
    values.put(INVENTORY_COUNT, String.valueOf(events.getTotalElements()));
    values.put(DOMAIN_NAME, getDomainName(executionMetadataModel.getDomainId()));
    StrSubstitutor sub = new StrSubstitutor(values);
    return sub.replace(message);
  }

  private String getDomainName(Long domainId) {
    DomainsService domainService = StaticApplicationContext.getBean(DomainsService.class);
    try {
      return domainService.getDomain(domainId).getName();
    } catch (ServiceException e) {
      xLogger.warn("Unable to get domain name for domain {0}", domainId, e);
    }
    return Constants.EMPTY;
  }
}
