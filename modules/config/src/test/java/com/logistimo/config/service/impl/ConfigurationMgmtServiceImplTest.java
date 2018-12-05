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

package com.logistimo.config.service.impl;


import com.logistimo.config.entity.IConfig;
import com.logistimo.config.models.ConfigurationException;
import com.logistimo.services.ServiceException;

import org.junit.Before;
import org.junit.Test;

import javax.jdo.PersistenceManager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;


/**
 * Created by smriti on 25/05/18.
 */
public class ConfigurationMgmtServiceImplTest {

  ConfigurationMgmtServiceImpl configurationMgmtService = spy(ConfigurationMgmtServiceImpl.class);
  PersistenceManager pm = mock(PersistenceManager.class);

  @Before
  public void setUp() throws ServiceException {
    doNothing().when(configurationMgmtService).addConfiguration(any(), any(IConfig.class));
  }


  @Test
  public void testAddDefaultDomainConfig() throws ServiceException, ConfigurationException {
    configurationMgmtService.addDefaultDomainConfig(1l, "India", "Assam", null, "Asia/Kolkata", "testuser123", pm);
  }
}