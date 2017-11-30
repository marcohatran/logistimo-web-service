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

package com.logistimo.service.impl;

import com.logistimo.context.StaticApplicationContext;
import com.logistimo.entities.entity.Kiosk;
import com.logistimo.entities.service.EntitiesService;
import com.logistimo.inventory.entity.IInvntry;
import com.logistimo.inventory.entity.Invntry;
import com.logistimo.inventory.exceptions.InventoryAllocationException;
import com.logistimo.inventory.service.impl.InventoryManagementServiceImpl;
import com.logistimo.materials.entity.Material;
import com.logistimo.materials.service.MaterialCatalogService;
import com.logistimo.services.ServiceException;
import com.logistimo.utils.BigUtil;

import org.springframework.context.ApplicationContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.math.BigDecimal;

import javax.jdo.PersistenceManager;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Created by charan on 04/11/16.
 */
public class InvManagementUnitTest {

  @BeforeClass
  public void setup() {
    ApplicationContext mockApplicationContext = mock(ApplicationContext.class);
    EntitiesService entitiesService = mock(EntitiesService.class);
    try {
      doReturn(new Kiosk()).when(entitiesService).getKiosk(anyLong(), anyBoolean());
    } catch (ServiceException e) {
      //exception
    }
    when(mockApplicationContext.getBean(EntitiesService.class)).thenReturn(
        entitiesService);
    MaterialCatalogService materialCatalogService = mock(MaterialCatalogService.class);
    try {
      doReturn(new Material()).when(materialCatalogService).getMaterial(anyLong());
    } catch (ServiceException e) {
      //exception
    }
    when(mockApplicationContext.getBean(MaterialCatalogService.class)).thenReturn(
        materialCatalogService);
    StaticApplicationContext applicationContext = new StaticApplicationContext();
    applicationContext.setApplicationContext(mockApplicationContext);
  }

  @Test
  public void testIncrementInventoryAvailableQuantity() throws Exception {
    InventoryManagementServiceImpl ims = new InventoryManagementServiceImpl();
    PersistenceManager pm = mock(PersistenceManager.class);
    IInvntry inv = new Invntry();
    inv.setStock(BigDecimal.TEN);
    inv.setAllocatedStock(BigDecimal.ZERO);
    inv.setAvailableStock(BigDecimal.TEN);
    inv.setKioskName("Test kiosk");
    inv.setMaterialName("Test material");
    ims.incrementInventoryAvailableQuantity(1l, 1l, null, BigDecimal.ZERO.subtract(BigDecimal.TEN),
        pm, inv, null, false);
    assertTrue(BigUtil.equals(inv.getAllocatedStock(), BigDecimal.TEN), "Allocated stock mismatch"
        + inv.getAllocatedStock() + ": e:" + BigDecimal.ZERO);
    assertTrue(BigUtil.equals(inv.getAvailableStock(), BigDecimal.ZERO),
        "Available stock mismatch a:"
            + inv.getAvailableStock() + ": e:" + BigDecimal.ZERO);
    assertTrue(BigUtil.equals(inv.getStock(), BigDecimal.TEN), "Stock mismatch a:"
        + inv.getStock() + ": e:" + BigDecimal.TEN);
  }


  @Test
  public void testAllocationNotEnoughAvailable() throws Exception {
    InventoryManagementServiceImpl ims = new InventoryManagementServiceImpl();
    PersistenceManager pm = mock(PersistenceManager.class);
    IInvntry inv = new Invntry();
    inv.setStock(BigDecimal.TEN);
    inv.setAllocatedStock(BigDecimal.TEN);
    inv.setAvailableStock(BigDecimal.ZERO);
    inv.setKioskName("Test kiosk");
    inv.setMaterialName("Test material");
    try {
      ims.incrementInventoryAvailableQuantity(1l, 1l, null,
          BigDecimal.ZERO.subtract(BigDecimal.TEN),
          pm, inv, null, false);
      fail("Expected exception that inventory is not available to allocate");
    } catch (InventoryAllocationException e) {
      assertTrue(e.getMessage().startsWith("Unable to allocate stock for material"),
          "Message does not match");
    }
  }
}
