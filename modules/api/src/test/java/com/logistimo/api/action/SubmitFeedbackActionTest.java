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

package com.logistimo.api.action;

import com.logistimo.api.models.FeedbackModel;
import com.logistimo.domains.entity.Domain;
import com.logistimo.domains.entity.IDomain;
import com.logistimo.domains.service.DomainsService;
import com.logistimo.entities.entity.IKiosk;
import com.logistimo.entities.entity.Kiosk;
import com.logistimo.entities.service.EntitiesService;
import com.logistimo.jpa.Repository;
import com.logistimo.pagination.PageParams;
import com.logistimo.pagination.Results;
import com.logistimo.services.ServiceException;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.users.entity.UserAccount;
import com.logistimo.users.service.UsersService;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;

/**
 * Created by kumargaurav on 23/07/18.
 */
@RunWith(MockitoJUnitRunner.class)
public class SubmitFeedbackActionTest {

  SubmitFeedbackAction action;

  @Mock
  UsersService usersService;

  @Mock
  EntitiesService entitiesService;

  @Mock
  DomainsService domainsService;

  @Mock
  Repository repository;

  @Before
  public void setUp() {

    action = new SubmitFeedbackAction() {
      protected String getFeedbackAddress() {
        return "test@email.com";
      }
    };
    action.setDomainsService(domainsService);
    action.setEntitiesService(entitiesService);
    action.setUsersService(usersService);
    action.setRepository(repository);
  }

  @Test
  public void testInvoke() throws Exception {
    //request
    FeedbackModel model = new FeedbackModel();
    model.setApp("app");
    model.setAppVersion("1.0");
    model.setUserId("kumarg");
    model.setText("text");
    //mock user account service call
    IUserAccount account = new UserAccount();
    account.setUserId("kumarg");
    account.setDomainId(1l);
    account.setEmail("kumar@logistimo.com");
    account.setMobilePhoneNumber("8750986526");
    when(usersService.getUserAccount("kumarg")).thenReturn(account);
    //mock domain service call
    IDomain domain = new Domain();
    domain.setId(1l);
    domain.setName("Default");
    when(domainsService.getDomain(1l)).thenReturn(domain);
    //mock entity service call
    List<IKiosk> list =new ArrayList<>();
    IKiosk userKiosk = new Kiosk();
    userKiosk.setName("test");
    userKiosk.setDistrict("test");
    userKiosk.setCity("test");
    userKiosk.setState("test");
    list.add(userKiosk);
    Results results = new Results(list,1,1);
    when(entitiesService.getKiosksForUser(Mockito.<IUserAccount>any(),Mockito.<String>any(),Mockito.<PageParams>any())).thenReturn(results);
    action.invoke(model);
  }

  @Test(expected = ServiceException.class)
  public void testInvokeWithException() throws Exception {
    //request
    FeedbackModel model = new FeedbackModel();
    model.setApp("app");
    model.setAppVersion("1.0");
    model.setUserId("kumarg");
    model.setText("text");
    //mock user account service call
    IUserAccount account = new UserAccount();
    account.setUserId("kumarg");
    account.setDomainId(1l);
    account.setEmail("kumar@logistimo.com");
    account.setMobilePhoneNumber("8750986526");
    when(usersService.getUserAccount("kumarg")).thenReturn(account);
    //mock domain service call
    IDomain domain = new Domain();
    domain.setId(1l);
    domain.setName("Default");
    when(domainsService.getDomain(1l)).thenReturn(domain);
    //mock entity service call
    when(entitiesService.getKiosksForUser(Mockito.<IUserAccount>any(),Mockito.<String>any(),Mockito.<PageParams>any())).thenThrow(new ServiceException("runtime excepttion"));
    action.invoke(model);
  }

}
