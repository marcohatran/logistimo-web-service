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

import com.logistimo.config.models.DomainConfig;
import com.logistimo.domains.entity.Domain;
import com.logistimo.domains.entity.IDomain;
import com.logistimo.domains.service.DomainsService;
import com.logistimo.exception.TaskSchedulingException;
import com.logistimo.pagination.Results;
import com.logistimo.services.ServiceException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by kumargaurav on 01/10/18.
 */
@RunWith(MockitoJUnitRunner.class)
public class ScheduleEventUpdateActionTest {

  @Mock
  DomainsService domainsService;

  ScheduleEventUpdateAction scheduleEventUpdateAction;

  @Before
  public void setUp() {
    scheduleEventUpdateAction = new ScheduleEventUpdateAction(){
      @Override
      protected DomainConfig domainConfig(Long domainId) {
        DomainConfig config = new DomainConfig();
        TimeZone timeZone = TimeZone.getTimeZone("Asia/Calcutta");
        config.setTimezone(timeZone.getDisplayName());
        return config ;
      }
      @Override
      protected void schedule(IDomain domain, Long eta) {
        //do nothing
      }
    };
    scheduleEventUpdateAction.setDomainsService(domainsService);
  }

  @Test
  public void testInvoke() throws ServiceException,TaskSchedulingException {

    Mockito.when(domainsService.getAllDomains(null)).thenReturn(results());
    scheduleEventUpdateAction.invoke();
  }

  private Results results () {
    IDomain domain = new Domain();
    domain.setId(1l);
    domain.setName("Test");
    List<IDomain> list = new ArrayList<>();
    list.add(domain);
    return new Results(list,null);
  }

}
