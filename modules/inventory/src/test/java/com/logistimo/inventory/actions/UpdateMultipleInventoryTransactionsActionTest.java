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

package com.logistimo.inventory.actions;

import com.logistimo.constants.Constants;
import com.logistimo.inventory.entity.ITransaction;
import com.logistimo.inventory.entity.Transaction;
import com.logistimo.inventory.models.ErrorDetailModel;
import com.logistimo.inventory.models.ResponseDetailModel;
import com.logistimo.inventory.models.SuccessDetailModel;
import com.logistimo.inventory.policies.AllowAllTransactionsPolicy;
import com.logistimo.inventory.policies.InventoryUpdatePolicy;
import com.logistimo.inventory.service.InventoryManagementService;
import com.logistimo.services.DuplicationException;
import com.logistimo.services.ServiceException;
import com.logistimo.utils.LockUtil;
import com.logistimo.utils.ThreadLocalUtil;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jdo.PersistenceManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Created by charan on 14/11/17.
 */
public class UpdateMultipleInventoryTransactionsActionTest {

  @Test
  public void testApplyPolicy() throws ServiceException {
    InventoryManagementService ims = mock(InventoryManagementService.class);
    UpdateMultipleInventoryTransactionsAction
        action =
        new UpdateMultipleInventoryTransactionsAction();
    action.setInventoryManagementService(ims);
    action = spy(action);
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        return null;
      }
    }).when(action).filterInvalidTransaction(any(), any(), any());
    doReturn(null).when(action).getLastWebTransaction(anyLong(),anyLong(),any());

    AllowAllTransactionsPolicy inventoryUpdatePolicy = new AllowAllTransactionsPolicy();
    
    HashMap<Long, List<ITransaction>> materialTransactionsMap = new HashMap<>();
    materialTransactionsMap.put(2l, buildTransactionlist());
    ThreadLocalUtil.get().locks.add(Constants.TX+1l);
    HashMap<Long, LockUtil.LockStatus> locks = new HashMap<>();
    locks.put(1l, LockUtil.LockStatus.ALREADY_LOCKED);

    HashMap<Long, List<ErrorDetailModel>> materialErrorDetailModelsMap = new HashMap<>();
    HashMap<Long, Integer> midCountMap = new HashMap<>();
    HashMap<Long, Integer> midFailedFromPositionMap = new HashMap<>();

    List<ITransaction> transactions = action
        .validateAndApplyPolicy(materialTransactionsMap, locks, materialErrorDetailModelsMap,
            midCountMap,
            midFailedFromPositionMap, inventoryUpdatePolicy);

    ThreadLocalUtil.get().locks.remove(Constants.TX+1l);

    assertTrue(transactions.size() == 1);
  }

  private List<ITransaction> buildTransactionlist() {
    ITransaction trans = new Transaction();
    trans.setKioskId(1l);
    trans.setOpeningStock(new BigDecimal(700));
    trans.setMaterialId(2l);
    trans.setType(ITransaction.TYPE_RECEIPT);
    trans.setQuantity(new BigDecimal(100));
    trans.setEntryTime(new Date());
    ArrayList<ITransaction> list = new ArrayList<>();
    list.add(trans);
    return list;
  }

  @Test
  public void testGetMaterialResponseDetailModel() {
    Map<Long,List<SuccessDetailModel>> successDetailModelsMap = buildMidSuccessDetailModelsMap();
    Map<Long,List<ErrorDetailModel>> errorDetailModelsMap = buildMidErrorDetailModelsMap();
    UpdateMultipleInventoryTransactionsAction
        action =
        new UpdateMultipleInventoryTransactionsAction();
    Map<Long,ResponseDetailModel> midResponseDetailModelMap = action.getMaterialResponseDetailModel(buildMidSuccessDetailModelsMap(),buildMidErrorDetailModelsMap());
    assertNotNull(midResponseDetailModelMap);
    assertTrue(!midResponseDetailModelMap.isEmpty());
    assertEquals(successDetailModelsMap.get(1l),midResponseDetailModelMap.get(1l).successDetailModels);
    assertEquals(successDetailModelsMap.get(2l), midResponseDetailModelMap.get(2l).successDetailModels);
    assertEquals(errorDetailModelsMap.get(1l), midResponseDetailModelMap.get(1l).errorDetailModels);
    assertEquals(errorDetailModelsMap.get(2l), midResponseDetailModelMap.get(2l).errorDetailModels);
  }

  private HashMap<Long,List<SuccessDetailModel>> buildMidSuccessDetailModelsMap() {
    HashMap<Long,List<SuccessDetailModel>> midSuccessDetailModelsMap = new HashMap<>(2,1);
    List<SuccessDetailModel> successDetailModels1 = new ArrayList<>(1);
    successDetailModels1.add(new SuccessDetailModel("M015", 1, Arrays.asList("1111","2222")));
    midSuccessDetailModelsMap.put(1l,successDetailModels1);
    List<SuccessDetailModel> successDetailModels2 = new ArrayList<>(1);
    successDetailModels2.add(new SuccessDetailModel("M015", 1, Arrays.asList("1111","2222")));
    midSuccessDetailModelsMap.put(2l,successDetailModels2);
    return midSuccessDetailModelsMap;
  }

  private HashMap<Long,List<ErrorDetailModel>> buildMidErrorDetailModelsMap() {
    HashMap<Long,List<ErrorDetailModel>> midErrorDetailModelsMap = new HashMap<>(2,1);
    List<ErrorDetailModel> errorDetailModels1 = new ArrayList<>(1);
    errorDetailModels1.add(new ErrorDetailModel("M015", 1, "message1"));
    midErrorDetailModelsMap.put(1l,errorDetailModels1);

    List<ErrorDetailModel> errorDetailModels2 = new ArrayList<>(1);
    errorDetailModels2.add(new ErrorDetailModel("M015", 1, "message1"));
    midErrorDetailModelsMap.put(2l,errorDetailModels2);
    return midErrorDetailModelsMap;
  }

  @Test
  public void testGetMaterialResponseDetailModelWithEmptyInputs() {
    Map<Long,List<SuccessDetailModel>> successDetailModelsMap = new HashMap<>(1,1);
    Map<Long,List<ErrorDetailModel>> errorDetailModelsMap = new HashMap<>(1,1);
    UpdateMultipleInventoryTransactionsAction
        action =
        new UpdateMultipleInventoryTransactionsAction();
    Map<Long,ResponseDetailModel> midResponseDetailModelMap = action.getMaterialResponseDetailModel(successDetailModelsMap, errorDetailModelsMap);
    assertNotNull(midResponseDetailModelMap);
    assertTrue(midResponseDetailModelMap.isEmpty());
  }

  @Test
  public void testGetMaterialResponseDetailModelWithSuccessDetailModels() {
    Map<Long,List<SuccessDetailModel>> successDetailModelsMap = buildMidSuccessDetailModelsMap();
    Map<Long,List<ErrorDetailModel>> errorDetailModelsMap = new HashMap<>(1,1);
    UpdateMultipleInventoryTransactionsAction
        action =
        new UpdateMultipleInventoryTransactionsAction();
    Map<Long,ResponseDetailModel> midResponseDetailModelMap = action.getMaterialResponseDetailModel(successDetailModelsMap, errorDetailModelsMap);
    assertNotNull(midResponseDetailModelMap);
    assertTrue(!midResponseDetailModelMap.isEmpty());
    assertTrue(midResponseDetailModelMap.size() == 2);
    assertEquals(successDetailModelsMap.get(1l), midResponseDetailModelMap.get(1l).successDetailModels);
    assertEquals(successDetailModelsMap.get(2l), midResponseDetailModelMap.get(2l).successDetailModels);
    assertNotNull(midResponseDetailModelMap.get(1l).errorDetailModels);
    assertTrue(midResponseDetailModelMap.get(1l).errorDetailModels.isEmpty());
  }

  @Test
  public void testGetMaterialResponseDetailModelWithErrorDetailModels() {
    Map<Long,List<ErrorDetailModel>> errorDetailModelsMap = buildMidErrorDetailModelsMap();
    Map<Long,List<SuccessDetailModel>> successDetailModelsMap = new HashMap<>(1,1);
    UpdateMultipleInventoryTransactionsAction
        action =
        new UpdateMultipleInventoryTransactionsAction();
    Map<Long,ResponseDetailModel> midResponseDetailModelMap = action.getMaterialResponseDetailModel(successDetailModelsMap, errorDetailModelsMap);
    assertNotNull(midResponseDetailModelMap);
    assertTrue(!midResponseDetailModelMap.isEmpty());
    assertTrue(midResponseDetailModelMap.size() == 2);
    assertNotNull(midResponseDetailModelMap.get(1l).successDetailModels);
    assertTrue(midResponseDetailModelMap.get(1l).successDetailModels.isEmpty());
    assertEquals(errorDetailModelsMap.get(1l), midResponseDetailModelMap.get(1l).errorDetailModels);
    assertEquals(errorDetailModelsMap.get(2l), midResponseDetailModelMap.get(2l).errorDetailModels);
  }

  @Test
  public void testGetMaterialResponseDetailModelWithUniqueMids() {
    Map<Long,List<SuccessDetailModel>> successDetailModelsMap = buildMidSuccessDetailModelsMap();
    Map<Long,List<ErrorDetailModel>> errorDetailModelsMap = buildNewMidErrorDetailModelsMap();
    UpdateMultipleInventoryTransactionsAction
        action =
        new UpdateMultipleInventoryTransactionsAction();
    Map<Long,ResponseDetailModel> midResponseDetailModelMap = action.getMaterialResponseDetailModel(successDetailModelsMap,errorDetailModelsMap);
    assertNotNull(midResponseDetailModelMap);
    assertTrue(!midResponseDetailModelMap.isEmpty());
    assertTrue(midResponseDetailModelMap.size() == 4);
    assertEquals(successDetailModelsMap.get(1l),midResponseDetailModelMap.get(1l).successDetailModels);
    assertEquals(successDetailModelsMap.get(2l), midResponseDetailModelMap.get(2l).successDetailModels);
    assertTrue(midResponseDetailModelMap.get(1l).errorDetailModels.isEmpty());
    assertTrue(midResponseDetailModelMap.get(2l).errorDetailModels.isEmpty());
    assertTrue(midResponseDetailModelMap.get(3l).successDetailModels.isEmpty());
    assertTrue(midResponseDetailModelMap.get(4l).successDetailModels.isEmpty());
    assertEquals(errorDetailModelsMap.get(3l),midResponseDetailModelMap.get(3l).errorDetailModels);
    assertEquals(errorDetailModelsMap.get(4l), midResponseDetailModelMap.get(4l).errorDetailModels);

  }

  private HashMap<Long,List<ErrorDetailModel>> buildNewMidErrorDetailModelsMap() {
    HashMap<Long,List<ErrorDetailModel>> midErrorDetailModelsMap = new HashMap<>(2,1);
    List<ErrorDetailModel> errorDetailModels1 = new ArrayList<>(1);
    errorDetailModels1.add(new ErrorDetailModel("M015", 1, "message1"));
    midErrorDetailModelsMap.put(3l,errorDetailModels1);

    List<ErrorDetailModel> errorDetailModels2 = new ArrayList<>(1);
    errorDetailModels2.add(new ErrorDetailModel("M015", 1, "message1"));
    midErrorDetailModelsMap.put(4l,errorDetailModels2);
    return midErrorDetailModelsMap;
  }

  @Test
  public void testGetReturnTransactionByLocalTrackingID() {
    UpdateMultipleInventoryTransactionsAction action = new UpdateMultipleInventoryTransactionsAction();
    List<ITransaction> transactions = new ArrayList<>(4);
    ITransaction transaction1 = new Transaction();
    transaction1.setLocalTrackingID("ltid_1");
    transaction1.setType(ITransaction.TYPE_ISSUE);
    ITransaction transaction2 = new Transaction();
    transaction2.setLocalTrackingID("ltid_1");
    transaction2.setType(ITransaction.TYPE_RETURNS_INCOMING);
    ITransaction transaction3 = new Transaction();
    transaction3.setLocalTrackingID("ltid_2");
    transaction3.setType(ITransaction.TYPE_RECEIPT);
    ITransaction transaction4 = new Transaction();
    transaction4.setLocalTrackingID("ltid_2");
    transaction4.setType(ITransaction.TYPE_RETURNS_OUTGOING);
    ITransaction transaction5 = new Transaction();
    transaction4.setType(ITransaction.TYPE_RETURNS_OUTGOING);

    transactions.add(transaction1);
    transactions.add(transaction2);
    transactions.add(transaction3);
    transactions.add(transaction4);
    transactions.add(transaction5);

    Map<String,ITransaction> returnTransByLocalTrackingID = action.getReturnTransactionByLocalTrackingID(transactions);
    assertNotNull(returnTransByLocalTrackingID);
    assertTrue(returnTransByLocalTrackingID.size() == 2);
    returnTransByLocalTrackingID = action.getReturnTransactionByLocalTrackingID(null);
    assertNotNull(returnTransByLocalTrackingID);
    assertTrue(returnTransByLocalTrackingID.isEmpty());
  }

  @Test
  public void testExecuteTransactionsWithLocalTrackingID() throws DuplicationException, ServiceException {
    UpdateMultipleInventoryTransactionsAction action = new UpdateMultipleInventoryTransactionsAction();
    InventoryManagementService ims = mock(InventoryManagementService.class);
    action.setInventoryManagementService(ims);
    Long domainID = 1l;
    PersistenceManager pm = mock(PersistenceManager.class);
    Map<Long,List<ErrorDetailModel>> errorDetailModelByMaterialMap = new HashMap<>(1);
    Map<Long,List<SuccessDetailModel>> successDetailModelByMaterialMap = new HashMap<>(1);
    Map<Long,Integer> countByMaterialMap = new HashMap<>(1);
    List<ITransaction> validTransactions = new ArrayList<>(4);
    ITransaction transaction1 = new Transaction();
    transaction1.setMaterialId(1l);
    transaction1.setType(ITransaction.TYPE_ISSUE);
    transaction1.setLocalTrackingID("ltid_1");
    ITransaction transaction2 = new Transaction();
    transaction2.setMaterialId(1l);
    transaction2.setType(ITransaction.TYPE_RETURNS_INCOMING);
    transaction2.setLocalTrackingID("ltid_1");
    ITransaction transaction3 = new Transaction();
    transaction3.setMaterialId(2l);
    transaction3.setType(ITransaction.TYPE_ISSUE);
    transaction3.setLocalTrackingID("ltid_2");
    ITransaction transaction4 = new Transaction();
    transaction4.setMaterialId(2l);
    transaction4.setType(ITransaction.TYPE_RETURNS_OUTGOING);
    transaction3.setLocalTrackingID("ltid_2");
    validTransactions.add(transaction1);
    validTransactions.add(transaction2);
    validTransactions.add(transaction3);
    validTransactions.add(transaction4);

    Transaction createdTransaction1 = ((Transaction) (transaction1.clone()));
    createdTransaction1.setKey(1l);
    Transaction createdTransaction2 = ((Transaction) (transaction1.clone()));
    createdTransaction2.setKey(2l);
    Transaction createdTransaction3 = ((Transaction) (transaction2.clone()));
    createdTransaction3.setKey(null);
    createdTransaction3.setMsgCode("M017");
    createdTransaction3.setMessage(null);
    Transaction createdTransaction4 = ((Transaction) (transaction3.clone()));
    createdTransaction4.setKey(null);

    Map<Long,Integer> failedFromPositionByMaterialMap = new HashMap<>(1);
    InventoryUpdatePolicy inventoryUpdatePolicy = new AllowAllTransactionsPolicy();
    when(ims.updateInventoryTransaction(1l, validTransactions.get(0), false,
        !inventoryUpdatePolicy.shouldDeduplicate(), pm)).thenReturn(createdTransaction1);
    when(ims.updateInventoryTransaction(1l, validTransactions.get(1),false,!inventoryUpdatePolicy.shouldDeduplicate(),pm)).thenReturn(createdTransaction2);
    when(ims.updateInventoryTransaction(1l, validTransactions.get(2),false,!inventoryUpdatePolicy.shouldDeduplicate(),pm)).thenReturn(createdTransaction3);
    when(ims.updateInventoryTransaction(1l, validTransactions.get(3),false,!inventoryUpdatePolicy.shouldDeduplicate(),pm)).thenReturn(createdTransaction4);
    action.executeTransactions(domainID,pm,errorDetailModelByMaterialMap,successDetailModelByMaterialMap,countByMaterialMap,validTransactions,failedFromPositionByMaterialMap,inventoryUpdatePolicy);
    assertTrue(errorDetailModelByMaterialMap.size() == 1);
    assertNotNull(errorDetailModelByMaterialMap.get(2l));
    assertTrue(errorDetailModelByMaterialMap.get(2l).size() == 1);
    ErrorDetailModel expectedErrorDetailModel = new ErrorDetailModel("M017",0,null);
    assertEquals(expectedErrorDetailModel, errorDetailModelByMaterialMap.get(2l).get(0));
    assertTrue(successDetailModelByMaterialMap.size() == 1);
    assertNotNull(successDetailModelByMaterialMap.get(1l));
    assertTrue(successDetailModelByMaterialMap.get(1l).size() == 1);
    SuccessDetailModel expectedSuccessDetailModel = new SuccessDetailModel("M016",1,Arrays.asList("1","2"));
    assertEquals(expectedSuccessDetailModel, successDetailModelByMaterialMap.get(1l).get(0));
  }

  @Test
  public void testExecuteTransactionsWithoutLocalTrackingID() throws DuplicationException, ServiceException {
    UpdateMultipleInventoryTransactionsAction action = new UpdateMultipleInventoryTransactionsAction();
    InventoryManagementService ims = mock(InventoryManagementService.class);
    action.setInventoryManagementService(ims);
    Long domainID = 1l;
    PersistenceManager pm = mock(PersistenceManager.class);
    Map<Long,List<ErrorDetailModel>> errorDetailModelByMaterialMap = new HashMap<>(1);
    Map<Long,List<SuccessDetailModel>> successDetailModelByMaterialMap = new HashMap<>(1);
    Map<Long,Integer> countByMaterialMap = new HashMap<>(1);
    List<ITransaction> validTransactions = new ArrayList<>(4);
    ITransaction transaction1 = new Transaction();
    transaction1.setMaterialId(1l);
    transaction1.setType(ITransaction.TYPE_ISSUE);
    ITransaction transaction2 = new Transaction();
    transaction2.setMaterialId(1l);
    transaction2.setType(ITransaction.TYPE_RETURNS_INCOMING);
    transaction2.setTrackingId("1");
    ITransaction transaction3 = new Transaction();
    transaction3.setMaterialId(2l);
    transaction3.setType(ITransaction.TYPE_ISSUE);
    ITransaction transaction4 = new Transaction();
    transaction4.setMaterialId(2l);
    transaction4.setType(ITransaction.TYPE_RETURNS_OUTGOING);
    transaction3.setTrackingId("3");
    validTransactions.add(transaction1);
    validTransactions.add(transaction2);
    validTransactions.add(transaction3);
    validTransactions.add(transaction4);

    Transaction createdTransaction1 = ((Transaction) (transaction1.clone()));
    createdTransaction1.setKey(1l);
    Transaction createdTransaction2 = ((Transaction) (transaction1.clone()));
    createdTransaction2.setKey(2l);
    Transaction createdTransaction3 = ((Transaction) (transaction2.clone()));
    createdTransaction3.setKey(3l);
    Transaction createdTransaction4 = ((Transaction) (transaction3.clone()));
    createdTransaction4.setKey(4l);

    Map<Long,Integer> failedFromPositionByMaterialMap = new HashMap<>(1);
    InventoryUpdatePolicy inventoryUpdatePolicy = new AllowAllTransactionsPolicy();
    when(ims.updateInventoryTransaction(1l, validTransactions.get(0), false,
        !inventoryUpdatePolicy.shouldDeduplicate(), pm)).thenReturn(createdTransaction1);
    when(ims.updateInventoryTransaction(1l, validTransactions.get(1),false,!inventoryUpdatePolicy.shouldDeduplicate(),pm)).thenReturn(createdTransaction2);
    when(ims.updateInventoryTransaction(1l, validTransactions.get(2),false,!inventoryUpdatePolicy.shouldDeduplicate(),pm)).thenReturn(createdTransaction3);
    when(ims.updateInventoryTransaction(1l, validTransactions.get(3),false,!inventoryUpdatePolicy.shouldDeduplicate(),pm)).thenReturn(createdTransaction4);
    action.executeTransactions(domainID,pm,errorDetailModelByMaterialMap,successDetailModelByMaterialMap,countByMaterialMap,validTransactions,failedFromPositionByMaterialMap,inventoryUpdatePolicy);
    assertTrue(errorDetailModelByMaterialMap.isEmpty());
    assertTrue(successDetailModelByMaterialMap.size() == 2);
    assertNotNull(successDetailModelByMaterialMap.get(1l));
    assertTrue(successDetailModelByMaterialMap.get(1l).size() == 1);
    SuccessDetailModel expectedSuccessDetailModel = new SuccessDetailModel("M016",1,Arrays.asList("1","2"));
    assertEquals(expectedSuccessDetailModel, successDetailModelByMaterialMap.get(1l).get(0));

    assertNotNull(successDetailModelByMaterialMap.get(2l));
    assertTrue(successDetailModelByMaterialMap.get(2l).size() == 1);
    expectedSuccessDetailModel = new SuccessDetailModel("M016",1,Arrays.asList("3","4"));
    assertEquals(expectedSuccessDetailModel, successDetailModelByMaterialMap.get(2l).get(0));
  }


  @Test
  public void testExecuteTransactionsWithServiceException() throws DuplicationException, ServiceException {
    UpdateMultipleInventoryTransactionsAction action = new UpdateMultipleInventoryTransactionsAction();
    InventoryManagementService ims = mock(InventoryManagementService.class);
    action.setInventoryManagementService(ims);
    Long domainID = 1l;
    PersistenceManager pm = mock(PersistenceManager.class);
    Map<Long,List<ErrorDetailModel>> errorDetailModelByMaterialMap = new HashMap<>(1);
    Map<Long,List<SuccessDetailModel>> successDetailModelByMaterialMap = new HashMap<>(1);
    Map<Long,Integer> countByMaterialMap = new HashMap<>(1);
    List<ITransaction> validTransactions = new ArrayList<>(4);
    ITransaction transaction1 = new Transaction();
    transaction1.setMaterialId(1l);
    transaction1.setType(ITransaction.TYPE_ISSUE);
    ITransaction transaction2 = new Transaction();
    transaction2.setMaterialId(1l);
    transaction2.setType(ITransaction.TYPE_RETURNS_INCOMING);
    ITransaction transaction3 = new Transaction();
    transaction3.setMaterialId(2l);
    transaction3.setType(ITransaction.TYPE_ISSUE);
    ITransaction transaction4 = new Transaction();
    transaction4.setMaterialId(2l);
    transaction4.setType(ITransaction.TYPE_RETURNS_OUTGOING);
    validTransactions.add(transaction1);
    validTransactions.add(transaction2);
    validTransactions.add(transaction3);
    validTransactions.add(transaction4);

    Transaction createdTransaction1 = ((Transaction) (transaction1.clone()));
    createdTransaction1.setKey(1l);
    Transaction createdTransaction2 = ((Transaction) (transaction1.clone()));
    createdTransaction2.setKey(2l);

    Map<Long,Integer> failedFromPositionByMaterialMap = new HashMap<>(1);
    InventoryUpdatePolicy inventoryUpdatePolicy = new AllowAllTransactionsPolicy();
    when(ims.updateInventoryTransaction(1l, validTransactions.get(0), false,
        !inventoryUpdatePolicy.shouldDeduplicate(), pm)).thenReturn(createdTransaction1);
    when(ims.updateInventoryTransaction(1l, validTransactions.get(1),false,!inventoryUpdatePolicy.shouldDeduplicate(),pm)).thenReturn(createdTransaction2);
    when(ims.updateInventoryTransaction(1l, validTransactions.get(2),false,!inventoryUpdatePolicy.shouldDeduplicate(),pm)).thenThrow(ServiceException.class);

    action.executeTransactions(domainID,pm,errorDetailModelByMaterialMap,successDetailModelByMaterialMap,countByMaterialMap,validTransactions,failedFromPositionByMaterialMap,inventoryUpdatePolicy);
    assertTrue(errorDetailModelByMaterialMap.size() == 1);
    assertNotNull(errorDetailModelByMaterialMap.get(2l));
    assertTrue(errorDetailModelByMaterialMap.get(2l).size() == 1);
    ErrorDetailModel expectedErrorDetailModel = new ErrorDetailModel("M012",0,null);
    assertEquals(expectedErrorDetailModel, errorDetailModelByMaterialMap.get(2l).get(0));

    assertTrue(successDetailModelByMaterialMap.size() == 1);
    assertNotNull(successDetailModelByMaterialMap.get(1l));
    assertTrue(successDetailModelByMaterialMap.get(1l).size() == 1);
    SuccessDetailModel expectedSuccessDetailModel = new SuccessDetailModel("M016", 1, Arrays.asList("1","2"));
    assertTrue(successDetailModelByMaterialMap.get(1l).get(0).index == 1);
    assertEquals(expectedSuccessDetailModel, successDetailModelByMaterialMap.get(1l).get(0));
  }
}