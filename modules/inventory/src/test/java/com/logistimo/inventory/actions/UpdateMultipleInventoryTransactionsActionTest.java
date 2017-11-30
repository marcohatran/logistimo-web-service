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
import com.logistimo.inventory.policies.AllowAllTransactionsPolicy;
import com.logistimo.inventory.service.InventoryManagementService;
import com.logistimo.services.ServiceException;
import com.logistimo.utils.LockUtil;
import com.logistimo.utils.ThreadLocalUtil;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

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

}