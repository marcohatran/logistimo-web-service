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

import com.logistimo.AppFactory;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.domains.entity.IDomain;
import com.logistimo.domains.service.DomainsService;
import com.logistimo.exception.TaskSchedulingException;
import com.logistimo.logger.XLog;
import com.logistimo.pagination.Results;
import com.logistimo.services.ServiceException;
import com.logistimo.services.taskqueue.ITaskService;
import com.logistimo.services.utils.ConfigUtil;
import com.logistimo.utils.LocalDateUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by charan on 01/08/17.
 */
@Component
public class ScheduleStockRebalancingAction {

  private static final XLog LOGGER = XLog.getLog(ScheduleStockRebalancingAction.class);
  private static final String
      STOCK_REBALANCING_AUTOMATION_URL =
      "/s2/api/stock-rebalancing/automate";


  private DomainsService domainsService;

  @Autowired
  public ScheduleStockRebalancingAction(DomainsService domainsService) {
    this.domainsService = domainsService;
  }

  public void invoke() {
    try {
      Results results = domainsService.getAllDomains(null);
      List<IDomain> domains = results.getResults();
      if (domains != null && !domains.isEmpty()) {
        domains.forEach(this::schedule);
      }
    } catch (ServiceException e) {
      LOGGER.severe("Failed to schedule order automation", e);
    }
  }

  private void schedule(IDomain domain) {
    try {
      DomainConfig domainConfig = DomainConfig.getInstance(domain.getId());
      long
          etaMillis =
          LocalDateUtil.getNextRunTime(domainConfig.getTimezone(),
              ConfigUtil.getInt("stock-rebalancing.schedule.hour", 6),
              ConfigUtil.getInt("stock-rebalancing.schedule.minute", 30));

      if (domainConfig.isCapabilityDisabled(DomainConfig.CAPABILITY_ORDERS)
          && domainConfig.getStockRebalancingConfig().isEnableStockRebalancing()) {
        AppFactory.get().getTaskService()
            .schedule(ITaskService.QUEUE_OPTIMZER,
                STOCK_REBALANCING_AUTOMATION_URL + "?domain_id=" + domain.getId(), null, null, null,
                ITaskService.METHOD_GET, etaMillis);
        LOGGER.info("Scheduled stock rebalancing for domain {0}:{1}", domain.getId(),
            domain.getName());
      }
    } catch (TaskSchedulingException e) {
      LOGGER
          .warn("Failed to schedule stock rebalancing task for domain {0}:{1}", domain.getId(),
              domain.getName(),
              e);
    }
  }

}
