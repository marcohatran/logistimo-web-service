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

package com.logistimo.api.migrators;

import com.logistimo.config.entity.IConfig;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.config.models.EventSummaryConfigModel;
import com.logistimo.config.service.ConfigurationMgmtService;
import com.logistimo.domains.entity.IDomain;
import com.logistimo.domains.service.DomainsService;
import com.logistimo.logger.XLog;
import com.logistimo.pagination.Results;
import com.logistimo.services.ServiceException;
import com.logistimo.services.cache.MemcacheService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by kumargaurav on 18/10/18.
 */
@Component
public class UpdateNotificationConfigAction {

  private static final XLog LOGGER = XLog.getLog(UpdateNotificationConfigAction.class);

  private DomainsService domainsService;

  private ConfigurationMgmtService mgmtService;

  private MemcacheService memcacheService;

  @Autowired
  public void setDomainsService(DomainsService domainsService) {
    this.domainsService = domainsService;
  }

  @Autowired
  public void setMgmtService(ConfigurationMgmtService mgmtService) {
    this.mgmtService = mgmtService;
  }

  @Autowired
  public void setMemcacheService(MemcacheService memcacheService) {
    this.memcacheService = memcacheService;
  }

  public void invoke() {
    try {
      Results results = domainsService.getAllDomains(null);
      List<IDomain> domains = results.getResults();
      if (domains != null && !domains.isEmpty()) {
        domains.forEach(this::trigger);
      }
    } catch (ServiceException e) {
      LOGGER.severe("Failed to trigger update notification config- {0}", e.getMessage(),e);
    }
  }

  private void trigger(IDomain domain) {

    long domainId = domain.getId();
    DomainConfig domainConfig = domainConfig(domainId);
    EventSummaryConfigModel eventsConfig = domainConfig.getEventSummaryConfig();
    if(eventsConfig == null || eventsConfig.getEvents() == null || eventsConfig.getEvents().isEmpty()) {
      return;
    } else {
      eventsConfig.getEvents().stream().forEach(events -> events.setNotification(Boolean.TRUE));
      domainConfig.setEventSummaryConfig(eventsConfig);
      updateConfig(domainConfig, domainId);
    }
  }

  protected DomainConfig domainConfig(Long domainId) {
    return DomainConfig.getInstance(domainId);
  }

  protected void updateConfig(DomainConfig domainConfig,Long domainId) {
    try {
      IConfig config = mgmtService.getConfiguration(IConfig.CONFIG_PREFIX + domainId);
      config.setConfig(domainConfig.toJSONSring());
      mgmtService.updateConfiguration(config);
    } catch (Exception e) {
      LOGGER.severe("Failed to update notification config for domain {0}", domainId, e);
    }
    memcacheService.put(DomainConfig.getCacheKey(domainId), domainConfig);
  }
}
