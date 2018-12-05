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

package com.logistimo.communications.actions;

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
 * Created by kumargaurav on 05/09/18.
 */
@Component
public class ScheduleEventUpdateAction {

  private static final XLog LOGGER = XLog.getLog(ScheduleEventUpdateAction.class);

  private static final String NOTIFY_EVENT_UPDATE_URL = "/s2/api/notifications/execute-eventupdate";


  private DomainsService domainsService;

  @Autowired
  public void setDomainsService(DomainsService domainsService) {
    this.domainsService = domainsService;
  }

  public void invoke() {
    try {
      Results results = domainsService.getAllDomains(null);
      List<IDomain> domains = results.getResults();
      if (domains != null && !domains.isEmpty()) {
        domains.forEach(this::trigger);
      }
    } catch (ServiceException e) {
      LOGGER.severe("Failed to trigger event notification- {0}", e.getMessage(),e);
    }
  }

  private void trigger(IDomain domain) {
    try {
      DomainConfig domainConfig = domainConfig(domain.getId());
      long etaMillis = LocalDateUtil.getNextRunTime(domainConfig.getTimezone(),
          ConfigUtil.getInt("eventnotification.schedule.hour", 7),
          ConfigUtil.getInt("eventnotification.schedule.minute", 30));
      //schedule
      schedule(domain,etaMillis);
    } catch (TaskSchedulingException e) {
      LOGGER.warn("Failed to schedule event notificationtask for domain {0}:{1}", domain.getId(),
          domain.getName(), e);
    }
  }

  protected DomainConfig domainConfig(Long domainId) {
    return DomainConfig.getInstance(domainId);
  }

  protected void schedule (IDomain domain, Long etaMillis) throws TaskSchedulingException {
    AppFactory.get().getTaskService().schedule(ITaskService.QUEUE_OPTIMZER,
        NOTIFY_EVENT_UPDATE_URL + "?domain_id=" + domain.getId(), null, null, null,
        ITaskService.METHOD_GET, etaMillis);
    LOGGER.info("Scheduled sevent notification for domain {0}:{1}", domain.getId(),
        domain.getName());
    ;
  }
}


