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
import com.logistimo.config.models.ActualTransConfig;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.config.models.InventoryConfig;
import com.logistimo.config.models.MatStatusConfig;
import com.logistimo.config.models.OrdersConfig;
import com.logistimo.config.service.ConfigurationMgmtService;
import com.logistimo.context.StaticApplicationContext;
import com.logistimo.inventory.entity.ITransaction;
import com.logistimo.materials.entity.IMaterialManufacturers;
import com.logistimo.materials.entity.MaterialManufacturers;
import com.logistimo.proto.JsonTagsZ;
import com.logistimo.services.ServiceException;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
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

  /*
  @Test
  public void testGetReasonsByTag() {
    InventoryConfig inventoryConfig = new InventoryConfig();
    inventoryConfig.setImtransreasons(getMaterialTagsReasonsMap());
    inventoryConfig.setRmtransreasons(getMaterialTagsReasonsMap());
    inventoryConfig.setSmtransreasons(getMaterialTagsReasonsMap());
    inventoryConfig.setTmtransreasons(getMaterialTagsReasonsMap());
    inventoryConfig.setDmtransreasons(getMaterialTagsReasonsMap());
    inventoryConfig.setMtagRetIncRsns(getMaterialTagsReasonsMap());
    inventoryConfig.setMtagRetOutRsns(getMaterialTagsReasonsMap());
    Map<String,Map<String,String>> matTagRsnsMap = RESTUtil.getReasonsByTag(inventoryConfig);
    assertNotNull(matTagRsnsMap);
    assertTrue(!matTagRsnsMap.isEmpty());
    assertTrue(matTagRsnsMap.size() == 7);
    assertEquals(getMaterialTagsReasonsMap(), matTagRsnsMap.get(JsonTagsZ.ISSUES));
    assertEquals(getMaterialTagsReasonsMap(), matTagRsnsMap.get(JsonTagsZ.RECEIPTS));
    assertEquals(getMaterialTagsReasonsMap(), matTagRsnsMap.get(JsonTagsZ.PHYSICAL_STOCK));
    assertEquals(getMaterialTagsReasonsMap(), matTagRsnsMap.get(JsonTagsZ.TRANSFER));
    assertEquals(getMaterialTagsReasonsMap(), matTagRsnsMap.get(JsonTagsZ.DISCARDS));
    assertEquals(getMaterialTagsReasonsMap(), matTagRsnsMap.get(JsonTagsZ.RETURNS_INCOMING));
    assertEquals(getMaterialTagsReasonsMap(), matTagRsnsMap.get(JsonTagsZ.RETURNS_OUTGOING));
  }*/

  private Map<String,String> getMaterialTagsReasonsMap() {
    Map<String,String> matTagReasonsMap = new HashMap<>(3,1);
    matTagReasonsMap.put("mtag1", "reason1");
    matTagReasonsMap.put("mtag2","reason2,reason3,reason4");
    matTagReasonsMap.put("mtag3","reason5");
    return matTagReasonsMap;
  }

  @Test
  public void testGetReasonsByTagWithNullValues() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    InventoryConfig inventoryConfig = new InventoryConfig();
    inventoryConfig.setImtransreasons(null);
    inventoryConfig.setRmtransreasons(null);
    inventoryConfig.setSmtransreasons(null);
    inventoryConfig.setTmtransreasons(null);
    inventoryConfig.setDmtransreasons(null);
    inventoryConfig.setMtagRetIncRsns(null);
    inventoryConfig.setMtagRetOutRsns(null);

    Map<String,Map<String,String>> matTagRsnsMap = RESTUtil.getReasonsByTag(inventoryConfig);
    assertNotNull(matTagRsnsMap);
    assertTrue(matTagRsnsMap.isEmpty());
  }

  @Test
  public void testGetMaterialStatusByType() {
    InventoryConfig inventoryConfig = new InventoryConfig();
    inventoryConfig.setMatStatusConfigByType(ITransaction.TYPE_ISSUE, getMaterialStatusConfig());
    inventoryConfig.setMatStatusConfigByType(ITransaction.TYPE_RECEIPT, getMaterialStatusConfig());
    inventoryConfig.setMatStatusConfigByType(ITransaction.TYPE_PHYSICALCOUNT, getMaterialStatusConfig());
    inventoryConfig.setMatStatusConfigByType(ITransaction.TYPE_TRANSFER, getMaterialStatusConfig());
    inventoryConfig.setMatStatusConfigByType(ITransaction.TYPE_WASTAGE, getMaterialStatusConfig());
    inventoryConfig.setMatStatusConfigByType(ITransaction.TYPE_RETURNS_INCOMING, getMaterialStatusConfig());
    inventoryConfig.setMatStatusConfigByType(ITransaction.TYPE_RETURNS_OUTGOING, getMaterialStatusConfig());

    Map<String,Map<String,String>> matStatusByType = RESTUtil.getMaterialStatusByType(
        inventoryConfig);
    assertNotNull(matStatusByType);
    assertTrue(!matStatusByType.isEmpty());
    assertTrue(matStatusByType.size() == 7);
    assertEquals(getExpectedMatStatusMap(), matStatusByType.get(JsonTagsZ.ISSUES));
    assertEquals(getExpectedMatStatusMap(), matStatusByType.get(JsonTagsZ.RECEIPTS));
    assertEquals(getExpectedMatStatusMap(), matStatusByType.get(JsonTagsZ.PHYSICAL_STOCK));
    assertEquals(getExpectedMatStatusMap(), matStatusByType.get(JsonTagsZ.TRANSFER));
    assertEquals(getExpectedMatStatusMap(), matStatusByType.get(JsonTagsZ.DISCARDS));
    assertEquals(getExpectedMatStatusMap(), matStatusByType.get(JsonTagsZ.RETURNS_INCOMING));
    assertEquals(getExpectedMatStatusMap(), matStatusByType.get(JsonTagsZ.RETURNS_OUTGOING));
  }

  private MatStatusConfig getMaterialStatusConfig() {
    MatStatusConfig matStatusConfig = new MatStatusConfig();
    matStatusConfig.setDf("status1,status2");
    matStatusConfig.setEtsm("tempstatus1,tempstatus2");
    matStatusConfig.setStatusMandatory(true);
    return matStatusConfig;
  }

  private Map<String,String> getExpectedMatStatusMap() {
    Map<String,String> expectedMatStatusMap = new HashMap<>(3,1);
    expectedMatStatusMap.put(JsonTagsZ.ALL, "status1,status2");
    expectedMatStatusMap.put(JsonTagsZ.TEMP_SENSITVE_MATERIALS, "tempstatus1,tempstatus2");
    expectedMatStatusMap.put(JsonTagsZ.MANDATORY, String.valueOf(true));
    return expectedMatStatusMap;
  }

  @Test
  public void testGetMaterialStatusByTypeWithNullValues() {
    InventoryConfig inventoryConfig = new InventoryConfig();
    inventoryConfig.setMatStatusConfigByType(ITransaction.TYPE_ISSUE, null);
    inventoryConfig.setMatStatusConfigByType(ITransaction.TYPE_RECEIPT, null);
    inventoryConfig.setMatStatusConfigByType(ITransaction.TYPE_PHYSICALCOUNT, null);
    inventoryConfig.setMatStatusConfigByType(ITransaction.TYPE_TRANSFER, null);
    inventoryConfig.setMatStatusConfigByType(ITransaction.TYPE_WASTAGE, null);
    inventoryConfig.setMatStatusConfigByType(ITransaction.TYPE_RETURNS_INCOMING, null);
    inventoryConfig.setMatStatusConfigByType(ITransaction.TYPE_RETURNS_OUTGOING, null);

    Map<String,Map<String,String>> matStatusByType = RESTUtil.getMaterialStatusByType(
        inventoryConfig);
    assertNotNull(matStatusByType);
    assertTrue(matStatusByType.isEmpty());
  }

  @Test
  public void testGetActualTransDateConfigByType() {
    InventoryConfig inventoryConfig = new InventoryConfig();
    inventoryConfig.setActualTransDateByType(ITransaction.TYPE_ISSUE, getActualTransactionConfig());
    inventoryConfig.setActualTransDateByType(ITransaction.TYPE_RECEIPT,
        getActualTransactionConfig());
    inventoryConfig.setActualTransDateByType(ITransaction.TYPE_PHYSICALCOUNT,
        getActualTransactionConfig());
    inventoryConfig.setActualTransDateByType(ITransaction.TYPE_TRANSFER,
        getActualTransactionConfig());
    inventoryConfig.setActualTransDateByType(ITransaction.TYPE_WASTAGE,
        getActualTransactionConfig());
    inventoryConfig.setActualTransDateByType(ITransaction.TYPE_RETURNS_INCOMING,
        getActualTransactionConfig());
    inventoryConfig.setActualTransDateByType(ITransaction.TYPE_RETURNS_OUTGOING,
        getActualTransactionConfig());

    Map<String,Map<String,String>> actualTransDateByType = RESTUtil.getActualTransDateConfigByType(
        inventoryConfig);
    assertNotNull(actualTransDateByType);
    assertTrue(!actualTransDateByType.isEmpty());
    assertTrue(actualTransDateByType.size() == 7);
    assertEquals(getExpectedActualTransMap(), actualTransDateByType.get(JsonTagsZ.ISSUES));
    assertEquals(getExpectedActualTransMap(), actualTransDateByType.get(JsonTagsZ.RECEIPTS));
    assertEquals(getExpectedActualTransMap(), actualTransDateByType.get(JsonTagsZ.PHYSICAL_STOCK));
    assertEquals(getExpectedActualTransMap(), actualTransDateByType.get(JsonTagsZ.TRANSFER));
    assertEquals(getExpectedActualTransMap(), actualTransDateByType.get(JsonTagsZ.DISCARDS));
    assertEquals(getExpectedActualTransMap(), actualTransDateByType.get(JsonTagsZ.RETURNS_INCOMING));
    assertEquals(getExpectedActualTransMap(), actualTransDateByType.get(JsonTagsZ.RETURNS_OUTGOING));
  }

  private ActualTransConfig getActualTransactionConfig() {
    ActualTransConfig actualTransConfig = new ActualTransConfig();
    actualTransConfig.setTy("1");
    return actualTransConfig;
  }

  private Map<String,String> getExpectedActualTransMap() {
    Map<String,String> expectedActualTransDateMap = new HashMap<>(1,1);
    expectedActualTransDateMap.put(JsonTagsZ.TYPE,"1");
    return expectedActualTransDateMap;
  }

  @Test
  public void testGetActualTransDateConfigByTypeWithNullValues() {
    InventoryConfig inventoryConfig = new InventoryConfig();
    inventoryConfig.setActualTransDateByType(ITransaction.TYPE_ISSUE, null);
    inventoryConfig.setActualTransDateByType(ITransaction.TYPE_RECEIPT,
        null);
    inventoryConfig.setActualTransDateByType(ITransaction.TYPE_PHYSICALCOUNT,
        null);
    inventoryConfig.setActualTransDateByType(ITransaction.TYPE_TRANSFER,
        null);
    inventoryConfig.setActualTransDateByType(ITransaction.TYPE_WASTAGE,
        null);
    inventoryConfig.setActualTransDateByType(ITransaction.TYPE_RETURNS_INCOMING,
        null);
    inventoryConfig.setActualTransDateByType(ITransaction.TYPE_RETURNS_OUTGOING,
        null);

    Map<String,Map<String,String>> actualTransDateByType = RESTUtil.getActualTransDateConfigByType(
        inventoryConfig);
    assertNotNull(actualTransDateByType);
    assertTrue(actualTransDateByType.isEmpty());
  }
}