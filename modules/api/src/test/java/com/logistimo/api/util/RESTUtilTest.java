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
import com.logistimo.config.models.DomainConfig;
import com.logistimo.config.models.InventoryConfig;
import com.logistimo.config.models.MatStatusConfig;
import com.logistimo.config.models.OrdersConfig;
import com.logistimo.config.service.ConfigurationMgmtService;
import com.logistimo.context.StaticApplicationContext;
import com.logistimo.materials.entity.IMaterialManufacturers;
import com.logistimo.materials.entity.MaterialManufacturers;
import com.logistimo.proto.JsonTagsZ;
import com.logistimo.services.ServiceException;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by yuvaraj on 12/04/17.
 */
public class RESTUtilTest {

  private static final String CONFIG_KEY = "config.2";
  ConfigurationMgmtService configurationMgmtService;
  @Before
  public void setup() throws ServiceException {
    ApplicationContext mockApplicationContext = mock(ApplicationContext.class);
    configurationMgmtService = mock(ConfigurationMgmtService.class);
    when(mockApplicationContext.getBean(ConfigurationMgmtService.class)).thenReturn(
        configurationMgmtService);
    StaticApplicationContext applicationContext = new StaticApplicationContext();
    applicationContext.setApplicationContext(mockApplicationContext);
  }
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
    Config config = new Config();
    config.setLastUpdated(getDate(-1));
    try {
      doReturn(config).when(configurationMgmtService).getConfiguration(CONFIG_KEY);
    } catch (ServiceException e) {
      //exception
    }
    assertEquals(false, RESTUtil.isConfigModified(Optional.of(getDate(0)), 2l));
  }

  @Test
  public void testIsConfigModifiedModifiedSinceDateLesserThanLastUpdatedTime() throws Exception {
    IConfig config = new Config();
    config.setLastUpdated(getDate(0));
    try {
      doReturn(config).when(configurationMgmtService).getConfiguration(CONFIG_KEY);
    } catch (ServiceException e) {
      //exception
    }
    assertEquals(true, RESTUtil.isConfigModified(Optional.of(getDate(-1)), 2l));
  }

  @Test
  public void testIsConfigModifiedModifiedSinceDateSameAsLastUpdatedTime() throws Exception {
    IConfig config = new Config();
    Date date = getDate(0);
    config.setLastUpdated(date);
    try {
      doReturn(config).when(configurationMgmtService).getConfiguration(CONFIG_KEY);
    } catch (ServiceException e) {
      //exception
    }
    assertEquals(true, RESTUtil.isConfigModified(Optional.of(date), 2l));
  }

  @Test
  public void testIsConfigModifiedWithoutModifiedSinceDate() throws Exception {
    IConfig config = new Config();
    config.setLastUpdated(getDate(0));
    try {
      doReturn(config).when(configurationMgmtService).getConfiguration(CONFIG_KEY);
    } catch (ServiceException e) {
      //exception
    }
    assertEquals(true, RESTUtil.isConfigModified(Optional.empty(), 2l));
  }

  private Date getDate(int daysOffset) {
    final Calendar cal = Calendar.getInstance();
    cal.add(Calendar.DATE, daysOffset);
    return cal.getTime();
  }

  @Test
  public void getManufacturerListTest() {

    List<Map<String, Object>>
        manufacturerList =
        RESTUtil.getManufacturerList(
            setMaterialManufacturers(1l, 1111l, "Serum Institute Of India", 1218l,
                new BigDecimal(20)));
    assertNotNull(manufacturerList);
    assertEquals(manufacturerList.size(), 1);
    assertEquals(manufacturerList.get(0).get(JsonTagsZ.MANUFACTURER_NAME),
        "Serum Institute Of India");
    assertEquals(manufacturerList.get(0).get(JsonTagsZ.QUANTITY), new BigDecimal(20));
    assertNull(manufacturerList.get(0).get("material_id"));
  }

  @Test
  public void testEmptyManufacturerList() {
    List<Map<String, Object>> manufacturerList = RESTUtil.getManufacturerList(null);
    assertEquals(manufacturerList.size(), 0);
  }

  @Test
  public void testGetMandatoryOrderFields() {
    DomainConfig dc = new DomainConfig();
    OrdersConfig oc = new OrdersConfig();
    oc.setReferenceIdMandatory(true);
    oc.setExpectedArrivalDateMandatory(false);
    dc.setOrdersConfig(oc);
    HashMap<String, Object> data = RESTUtil.getMandatoryOrderFields(oc);
    HashMap<String, Object> salesOrderConfig =
        (HashMap<String, Object>) data.get(JsonTagsZ.SALES_ORDERS);
    HashMap<String, Object> shippingConfig =
        (HashMap<String, Object>) salesOrderConfig.get(JsonTagsZ.SHIPPING);
    assertEquals(data.entrySet().size(), 1);
    assertEquals(salesOrderConfig.entrySet().size(), 1);
    assertEquals(shippingConfig.entrySet().size(), 2);
    assertEquals(shippingConfig.get(JsonTagsZ.REFERENCE_ID), true);
    assertEquals(shippingConfig.get(JsonTagsZ.EXPECTED_TIME_OF_ARRIVAL), false);
  }

  private List<IMaterialManufacturers> setMaterialManufacturers(Long key, Long code, String name,
                                                                Long materialCode, BigDecimal qty) {
    List<IMaterialManufacturers> manufacturers = new ArrayList<>();
    MaterialManufacturers mfrs = new MaterialManufacturers();
    mfrs.setKey(key);
    mfrs.setManufacturerCode(code);
    mfrs.setManufacturerName(name);
    mfrs.setMaterialCode(materialCode);
    mfrs.setQuantity(qty);
    manufacturers.add(mfrs);
    return manufacturers;
  }
}