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

package com.logistimo.api.builders;


import com.logistimo.config.entity.Config;
import com.logistimo.config.models.ConfigurationException;
import com.logistimo.config.service.ConfigurationMgmtService;
import com.logistimo.constants.SourceConstants;
import com.logistimo.services.ServiceException;
import com.logistimo.twofactorauthentication.vo.UserDevicesVO;

import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.Assert.*;

/**
 * @author smriti
 */
public class UserDevicesBuilderTest {


  ConfigurationMgmtService configurationMgmtService;
  UserDevicesBuilder builder = new UserDevicesBuilder();
  Config config = new Config();


  @Before
  public void setUp() throws ServiceException, ConfigurationException {
    configurationMgmtService = mock(ConfigurationMgmtService.class);
    builder.setConfigurationMgmtService(configurationMgmtService);
    config.setKey("domain.132453");
    config.setDomainId(132453l);
    config.setLastUpdated(new Date());
    when(configurationMgmtService.getConfiguration(anyString())).thenReturn(config);
  }

  @Test
  public void testBuildUserDevices() throws Exception {
    Date date = new Date();

    UserDevicesVO
        userDevicesVO =
        builder.buildUserDevicesVO("01bc2719vdb1882bdf83029wn82", "testUser",
            SourceConstants.WEB, date);
    assertEquals("testUser", userDevicesVO.getUserId());
    assertEquals("testUser_01bc2719vdb1882bdf83029wn82", userDevicesVO.getId());
    assertEquals(SourceConstants.WEB, userDevicesVO.getApplicationName());
    assertEquals(userDevicesVO.getExpiresOn(), date);
  }
}