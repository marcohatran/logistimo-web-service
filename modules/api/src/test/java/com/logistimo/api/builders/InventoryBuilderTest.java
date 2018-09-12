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

package com.logistimo.api.builders;

import com.logistimo.api.models.InventoryDetailModel;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.inventory.entity.IInvntryBatch;
import com.logistimo.inventory.entity.Invntry;
import com.logistimo.inventory.entity.InvntryBatch;
import com.logistimo.inventory.service.InventoryManagementService;
import com.logistimo.materials.entity.IHandlingUnit;
import com.logistimo.materials.service.IHandlingUnitService;
import com.logistimo.pagination.Results;
import com.logistimo.services.ServiceException;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

/**
 * @author smriti
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({DomainConfig.class})
public class InventoryBuilderTest {
  InventoryDetailModel inventoryModel;
  Invntry invntry;
  DomainConfig domainConfig;

  @Mock
  InventoryManagementService inventoryManagementService;

  @Mock
  IHandlingUnitService handlingUnitService;

  @Spy
  @InjectMocks
  InventoryBuilder inventoryBuilder = new InventoryBuilder();

  @Before
  public void setUp() {
    inventoryModel = new InventoryDetailModel();
    invntry = new Invntry();
    domainConfig = new DomainConfig();
    domainConfig.setTimezone("Asia/Kolkata");
    mockStatic(DomainConfig.class);
    when(DomainConfig.getInstance(any())).thenReturn(domainConfig);
  }

  @Test
  public void testSetBatchModelForEmptyData() throws Exception {
    when(inventoryManagementService.getBatches(any(), any(), any())).thenReturn(new Results<>());
    inventoryBuilder.setBatchModel(invntry, inventoryModel);
    assertEquals(null, inventoryModel.getExpiredBatches());
    assertEquals(null, inventoryModel.getBatches());

  }

  @Test
  public void testSetBatchModel() throws ServiceException {
    List<IInvntryBatch> invntryBatches = new ArrayList<>();
    invntryBatches.add(setInvntryBatch("B001","Serum Institute",new BigDecimal(20),new Date(), new Date(1506623400000l)));
    invntryBatches.add(setInvntryBatch("B002", "Serum Institute of Medicines", new BigDecimal(100), new Date(), new Date(1599806451000l)));

    when(inventoryManagementService.getBatches(any(), any(), any())).thenReturn(new Results<>(invntryBatches, null));
    inventoryBuilder.setBatchModel(invntry, inventoryModel);

    //Assert for expired batches
    assertNotNull(inventoryModel.getExpiredBatches());
    assertEquals(invntryBatches.get(0).getBatchId(), inventoryModel.getExpiredBatches().get(0).bid);
    assertEquals(invntryBatches.get(0).getBatchManufacturer(), inventoryModel.getExpiredBatches().get(0).bmfnm);
    assertEquals(invntryBatches.get(0).getQuantity(), inventoryModel.getExpiredBatches().get(0).q);
    assertEquals(invntryBatches.get(0).getTimestamp(), inventoryModel.getExpiredBatches().get(0).t);
    assertEquals(invntryBatches.get(0).getBatchExpiry(), inventoryModel.getExpiredBatches().get(0).bexp);

    //Assert for valid batches
    assertNotNull(inventoryModel.getBatches());
    assertEquals(invntryBatches.get(1).getBatchId(), inventoryModel.getBatches().get(0).bid);
    assertEquals(invntryBatches.get(1).getBatchManufacturer(), inventoryModel.getBatches().get(0).bmfnm);
    assertEquals(invntryBatches.get(1).getQuantity(), inventoryModel.getBatches().get(0).q);
    assertEquals(invntryBatches.get(1).getTimestamp(), inventoryModel.getBatches().get(0).t);
    assertEquals(invntryBatches.get(1).getBatchExpiry(), inventoryModel.getBatches().get(0).bexp);
  }

  @Test
  public void testSetHandlingUnitsForEmptyData() throws Exception {
    when(handlingUnitService.getHandlingUnitDataByMaterialId(any())).thenReturn(null);
    inventoryBuilder.setHandlingUnits(invntry, inventoryModel);
    assertEquals(false, inventoryModel.isEnforceHandlingUnit());
    assertEquals(null, inventoryModel.getHandlingUnitModel());
  }

  @Test
  public void testSetHandlingUnits() throws Exception {
    Map<String, String> handlingUnit = setHandlingUnit("101", "H1", new BigDecimal(5));
    when(handlingUnitService.getHandlingUnitDataByMaterialId(any())).thenReturn(handlingUnit);
    inventoryBuilder.setHandlingUnits(invntry, inventoryModel);
    assertEquals(true, inventoryModel.isEnforceHandlingUnit());
    assertEquals("101", inventoryModel.getHandlingUnitModel().getHandlingUnitId());
    assertEquals("H1", inventoryModel.getHandlingUnitModel().getName());
    assertEquals(new BigDecimal(5), inventoryModel.getHandlingUnitModel().getQuantity());
  }


  private IInvntryBatch setInvntryBatch(String batchId, String manufacturer, BigDecimal quantity, Date lastUpdatedTimestamp, Date expiryDate) {
    IInvntryBatch invntryBatch = new InvntryBatch();
    invntryBatch.setBatchId(batchId);
    invntryBatch.setBatchManufacturer(manufacturer);
    invntryBatch.setQuantity(quantity);
    invntryBatch.setTimestamp(lastUpdatedTimestamp);
    invntryBatch.setBatchExpiry(expiryDate);
    return invntryBatch;
  }

  private Map<String, String> setHandlingUnit(String id, String name, BigDecimal quantity) {
    Map<String, String> handlingUnits = new HashMap<>();
    handlingUnits.put(IHandlingUnit.HUID, id);
    handlingUnits.put(IHandlingUnit.NAME, name);
    handlingUnits.put(IHandlingUnit.QUANTITY, String.valueOf(quantity));
    return handlingUnits;
  }
}