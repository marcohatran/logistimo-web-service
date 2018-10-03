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

import com.logistimo.communications.models.EventSummary;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.config.models.EventSummaryConfigModel;
import com.logistimo.users.models.ExtUserAccount;
import com.logistimo.users.service.UsersService;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Locale;

/**
 * Created by kumargaurav on 01/10/18.
 */
@RunWith(MockitoJUnitRunner.class)
public class NotifyEventUpdateActionTest {

  @Mock
  private RestTemplate restTemplate;

  @Mock
  private UsersService usersService;

  NotifyEventUpdateAction action;

  @Before
  public void setUp() {
    action = new NotifyEventUpdateAction() {
      @Override
      protected DomainConfig domainConfig(Long domainId) {
        DomainConfig config = new DomainConfig();
        config.setEventSummaryConfig(eventSummaryConfigModel());
        config.setLangPreference("en");
        return config ;
      }
      @Override
      protected void enqueueNotification(List<EventSummary> summaries, List<ExtUserAccount> users,
                                         Locale locale) {
        // do nothing
      }
    };
    action.setRestTemplate(restTemplate);
    action.setUsersService(usersService);
  }

  @Test
  public void testInvoke() {
    action.invoke(1l);
  }

  private EventSummaryConfigModel eventSummaryConfigModel () {
    return new EventSummaryConfigModel();
  }

  @Test
  public void testIsNewEvent () {
    String timestamp = "2018-10-03T00:00:00.000+01:00";
    boolean res = action.isNewEvent(action.eventDate(timestamp),action.todayDate());
    Assert.assertNotNull(res);
  }
}
