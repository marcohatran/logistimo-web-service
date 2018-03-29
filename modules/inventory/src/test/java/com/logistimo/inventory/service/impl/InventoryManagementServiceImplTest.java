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

package com.logistimo.inventory.service.impl;

import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.dao.JDOUtils;
import com.logistimo.exception.LogiException;
import com.logistimo.inventory.dao.impl.TransDao;
import com.logistimo.inventory.entity.IInvntry;
import com.logistimo.inventory.entity.IInvntryBatch;
import com.logistimo.inventory.entity.ITransaction;
import com.logistimo.inventory.entity.Invntry;
import com.logistimo.inventory.entity.Transaction;
import com.logistimo.tags.dao.TagDao;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.jdo.PersistenceManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

/**
 * Created by charan on 15/12/17.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(SecurityUtils.class)
public class InventoryManagementServiceImplTest {
  InventoryManagementServiceImpl ims;
  PersistenceManager pm;

  @Before
  public void setup() {
    ims = new InventoryManagementServiceImpl();
    pm = mock(PersistenceManager.class);
    TagDao tagDao = new TagDao();
    ims.setTagDao(tagDao);
    mockStatic(SecurityUtils.class);
    PowerMockito.when(SecurityUtils.getUsername()).thenReturn("user");

  }

  @Test
  public void testIsATDValid() throws ParseException {
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    assertFalse("ATD should be considered valid, but not",
        ims.isAtdNotValid("Asia/Kolkata", df.parse("2017-12-16T01:30:00.000+0000"),
            df.parse("2017-12-15T19:30:00.000+0000")));
  }

  @Test
  public void testIsATDNotValid() throws ParseException {
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    assertTrue("ATD should be considered in future, but not",
        ims.isAtdNotValid("Asia/Kolkata", df.parse("2017-12-16T01:30:00.000+0000"),
            df.parse("2017-12-15T17:30:00.000+0000")));
  }

  @Test
  public void testSetInventoryFieldsTax()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    IInvntry
        invntry =
        getInvntry(1l, 10l, 100l, BigDecimal.ZERO, new BigDecimal(30), BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO, null, null, null);
    IInvntry inventory = new Invntry();
    Method
        method =
        InventoryManagementServiceImpl.class
            .getDeclaredMethod("setInventoryFields", IInvntry.class, IInvntry.class, String.class);
    method.setAccessible(true);
    method.invoke(ims, inventory, invntry, "testuser");

    assertEquals(new BigDecimal(30), inventory.getTax());
    assertNotNull(inventory.getUpdatedOn());
    assertNull(inventory.getTimestamp());
  }

  @Test
  public void testSetInventoryFieldsConsumptionRateManual()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    IInvntry
        invntry =
        getInvntry(1l, 10l, 100l, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal(20),
            BigDecimal.ZERO, BigDecimal.ZERO, null, null, null);
    IInvntry inventory = new Invntry();
    Method
        method =
        InventoryManagementServiceImpl.class
            .getDeclaredMethod("setInventoryFields", IInvntry.class, IInvntry.class, String.class);
    method.setAccessible(true);
    method.invoke(ims, inventory, invntry, "testuser");

    assertEquals(new BigDecimal(20), inventory.getConsumptionRateManual());
    assertNotNull(inventory.getMnlConsumptionRateUpdatedTime());
    assertNull(inventory.getTimestamp());
  }

  @Test
  public void testSetInventoryFieldsRetailerPrice()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    IInvntry
        invntry =
        getInvntry(1l, 10l, 100l, new BigDecimal(100), BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO, null, null, null);
    IInvntry inventory = new Invntry();
    Method
        method =
        InventoryManagementServiceImpl.class
            .getDeclaredMethod("setInventoryFields", IInvntry.class, IInvntry.class, String.class);
    method.setAccessible(true);
    method.invoke(ims, inventory, invntry, "testuser");

    assertEquals(new BigDecimal(100), inventory.getRetailerPrice());
    assertNotNull(inventory.getRetailerPriceUpdatedTime());
    assertNull(inventory.getTimestamp());
  }

  @Test
  public void testSetInventoryFieldsMinMax()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    IInvntry
        invntry =
        getInvntry(1l, 10l, 100l, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE,
            BigDecimal.TEN, null, null, null);
    IInvntry inventory = new Invntry();
    Method
        method =
        InventoryManagementServiceImpl.class
            .getDeclaredMethod("setInventoryFields", IInvntry.class, IInvntry.class, String.class);
    method.setAccessible(true);
    method.invoke(ims, inventory, invntry, "testuser");

    assertEquals(BigDecimal.ONE, inventory.getReorderLevel());
    assertEquals(BigDecimal.TEN, inventory.getMaxStock());
    assertNotNull(inventory.getReorderLevelUpdatedTime());
    assertNotNull(inventory.getMaxUpdatedTime());
    assertNull(inventory.getTimestamp());
  }

  @Test
  public void testSetInventoryFieldsPSTimestamp()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Date psTimeStamp = new Date();
    IInvntry
        invntry =
        getInvntry(1l, 10l, 100l, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO, null, psTimeStamp, null);
    IInvntry inventory = new Invntry();
    Method
        method =
        InventoryManagementServiceImpl.class
            .getDeclaredMethod("setInventoryFields", IInvntry.class, IInvntry.class, String.class);
    method.setAccessible(true);
    method.invoke(ims, inventory, invntry, "testuser");
    assertEquals(psTimeStamp, inventory.getPSTimestamp());
    assertNull(inventory.getTimestamp());
  }

  @Test
  public void testSetInventoryFieldsDQTimeStamp()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Date dqTimeStamp = new Date();
    IInvntry
        invntry =
        getInvntry(1l, 10l, 100l, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO, null, null, dqTimeStamp);
    IInvntry inventory = new Invntry();
    Method
        method =
        InventoryManagementServiceImpl.class
            .getDeclaredMethod("setInventoryFields", IInvntry.class, IInvntry.class, String.class);
    method.setAccessible(true);
    method.invoke(ims, inventory, invntry, "testuser");
    assertEquals(dqTimeStamp, inventory.getDQTimestamp());
    assertNull(inventory.getTimestamp());
  }

  @Test
  public void testUpdateStock()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    IInvntry
        invntry =
        getInvntry(1l, 10l, 100l, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO, null, null, null);
    Date timestamp = new Date();
    invntry.setStock(new BigDecimal(30));
    invntry.setAvailableStock(new BigDecimal(20));
    invntry.setAllocatedStock(BigDecimal.TEN);

    ITransaction transaction = JDOUtils.createInstance(ITransaction.class);
    transaction.setKioskId(1l);
    transaction.setMaterialId(10l);
    transaction.setDomainId(100l);
    transaction.setQuantity(BigDecimal.TEN);
    transaction.setType(ITransaction.TYPE_ISSUE);

    Method
        method =
        InventoryManagementServiceImpl.class
            .getDeclaredMethod("updateStock", IInvntry.class, IInvntryBatch.class,
                ITransaction.class, Date.class, PersistenceManager.class);
    method.setAccessible(true);
    method.invoke(ims, invntry, null, transaction, timestamp, null);

    assertEquals( new BigDecimal(30), transaction.getOpeningStock());
    assertEquals(new BigDecimal(20), invntry.getStock());
    assertEquals(BigDecimal.TEN, invntry.getAvailableStock());
    assertEquals(timestamp, invntry.getTimestamp());
  }

  @Test
  public void testIncrementInventoryAvailableQuantity()
      throws Exception {
    IInvntry inv = getInvntry(1l, 2l, 1343724l, null, null, null, null, null, new Date(), null, null);
    inv.setAvailableStock(BigDecimal.TEN);
    inv.setStock(BigDecimal.TEN);
    inv.setAllocatedStock(BigDecimal.ZERO);
    ims.incrementInventoryAvailableQuantity(1l, 2l, null, new BigDecimal(-10), pm, inv, null, true);
    assertEquals(BigDecimal.ZERO, inv.getAvailableStock());
    assertEquals(BigDecimal.TEN, inv.getAllocatedStock());
  }

  private IInvntry getInvntry(Long kId, Long mId, Long sdId, BigDecimal price, BigDecimal tax,
                              BigDecimal crMnl, BigDecimal min, BigDecimal max, Date timeStamp,
                              Date psTimeStamp, Date dqTimeStamp) {
    IInvntry invntry = new Invntry();
    invntry.setKioskId(kId);
    invntry.setDomainId(sdId);
    invntry.setMaterialId(mId);
    invntry.setRetailerPrice(price);
    invntry.setTax(tax);
    invntry.setConsumptionRateManual(crMnl);
    invntry.setReorderLevel(min);
    invntry.setMaxStock(max);
    if (timeStamp != null) {
      invntry.setTimestamp(timeStamp);
    }
    invntry.setPSTimestamp(psTimeStamp);
    invntry.setDQTimestamp(dqTimeStamp);
    return invntry;
  }

  @Test
  public void testCheckReturnsErrors() {
    TransDao mockTransDao = mock(TransDao.class);
    PersistenceManager mockPm = mock(PersistenceManager.class);
    ITransaction linkedTransaction = new Transaction();
    linkedTransaction.setType(ITransaction.TYPE_ISSUE);
    linkedTransaction.setQuantity(BigDecimal.ONE);
    when(mockTransDao.getById("1",mockPm))
        .thenReturn(linkedTransaction);
    ITransaction transaction = new Transaction();
    transaction.setQuantity(BigDecimal.TEN);
    transaction.setTrackingId("1");
    InventoryManagementServiceImpl inventoryManagementService =
        new InventoryManagementServiceImpl();
    inventoryManagementService.setTransDao(mockTransDao);
    try {
      inventoryManagementService.checkReturnsErrors(transaction, mockPm);
    } catch(LogiException exception) {
      assertTrue(exception.getCode().equals("M017"));
    }
    transaction.setTrackingObjectType("invalid");
    try {
      inventoryManagementService.checkReturnsErrors(transaction, mockPm);
    } catch(LogiException exception) {
      assertTrue(exception.getCode().equals("M017"));
    }
    transaction.setTrackingObjectType("iss_trn");
    transaction.setReason("Customer did not like the product");
    try {
      inventoryManagementService.checkReturnsErrors(transaction, mockPm);
    } catch(LogiException exception) {
      assertTrue(exception.getCode().equals("M019"));
    }
  }

}