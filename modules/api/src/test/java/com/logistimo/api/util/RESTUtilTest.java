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

package com.logistimo.api.util;

import com.logistimo.config.entity.Config;
import com.logistimo.config.entity.IConfig;
import com.logistimo.config.models.InventoryConfig;
import com.logistimo.config.models.MatStatusConfig;
import com.logistimo.config.service.ConfigurationMgmtService;
import com.logistimo.proto.JsonTagsZ;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by yuvaraj on 12/04/17.
 */
public class RESTUtilTest {

  private static final String CONFIG_KEY = "config.2";
  @Test
  public void testGetMaterialStatus() throws Exception {

    InventoryConfig ic = new InventoryConfig();
    MatStatusConfig mc = new MatStatusConfig();
    mc.setDf("hello,hello,,");
    ic.setMatStatusConfigByType("i",mc);
    Hashtable<String, Hashtable<String, String>>
        nestedHashTable =
        RESTUtil.getMaterialStatus(ic);
    Hashtable<String, String> values = nestedHashTable.get("i");
    String uniqueMStatVal = values.get(JsonTagsZ.ALL);
    assertNotNull(uniqueMStatVal);
    assertEquals("Status does not match", "hello",uniqueMStatVal);

  }

  @Test
  public void testIsConfigModifiedModifiedSinceDateGreaterThanLastUpdatedTime() throws Exception {
    ConfigurationMgmtService configManagementService = mock(ConfigurationMgmtService.class);
    IConfig mockConfig = new Config();
    mockConfig.setLastUpdated(getDate(-1));
    when(configManagementService.getConfiguration(CONFIG_KEY))
        .thenReturn(mockConfig);
    Method isConfigModified = RESTUtil.class.getDeclaredMethod("isConfigModified", Optional.class, Long.class);
    isConfigModified.setAccessible(true);
    Object o = isConfigModified.invoke(null, Optional.of(getDate(0)), 2l);
    assertEquals(o, false);
  }

  @Test
  public void testIsConfigModifiedModifiedSinceDateLesserThanLastUpdatedTime() throws Exception {
    ConfigurationMgmtService configManagementService = mock(ConfigurationMgmtService.class);
    IConfig mockConfig = new Config();
    mockConfig.setLastUpdated(getDate(0));
    when(configManagementService.getConfiguration(CONFIG_KEY))
        .thenReturn(mockConfig);
    Method isConfigModified = RESTUtil.class.getDeclaredMethod("isConfigModified", Optional.class, Long.class);
    isConfigModified.setAccessible(true);
    Object o = isConfigModified.invoke(null, Optional.of(getDate(-1)), 2l);
    assertEquals(o, true);
  }

  @Test
  public void testIsConfigModifiedModifiedSinceDateSameAsLastUpdatedTime() throws Exception {
    ConfigurationMgmtService configManagementService = mock(ConfigurationMgmtService.class);
    IConfig mockConfig = new Config();
    mockConfig.setLastUpdated(getDate(0));
    when(configManagementService.getConfiguration(CONFIG_KEY))
        .thenReturn(mockConfig);
    Method isConfigModified = RESTUtil.class.getDeclaredMethod("isConfigModified", Optional.class, Long.class);
    isConfigModified.setAccessible(true);
    Object o = isConfigModified.invoke(null, Optional.of(getDate(0)), 2l);
    assertEquals(o, false);
  }

  @Test
  public void testIsConfigModifiedWithoutModifiedSinceDate() throws Exception {
    ConfigurationMgmtService configManagementService = mock(ConfigurationMgmtService.class);
    IConfig mockConfig = new Config();
    mockConfig.setLastUpdated(getDate(0));
    when(configManagementService.getConfiguration(CONFIG_KEY))
        .thenReturn(mockConfig);
    Method isConfigModified = RESTUtil.class.getDeclaredMethod("isConfigModified", Optional.class, Long.class);
    isConfigModified.setAccessible(true);
    Object o = isConfigModified.invoke(null, Optional.empty(), 2l);
    assertEquals(o, true);
  }

  private Date getDate(int daysOffset) {
    final Calendar cal = Calendar.getInstance();
    cal.add(Calendar.DATE, daysOffset);
    return cal.getTime();
  }
}