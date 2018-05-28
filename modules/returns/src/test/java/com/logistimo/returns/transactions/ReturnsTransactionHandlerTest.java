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

package com.logistimo.returns.transactions;

import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.exception.SystemException;
import com.logistimo.inventory.entity.ITransaction;
import com.logistimo.inventory.entity.Transaction;
import com.logistimo.inventory.models.CreateTransactionsReturnModel;
import com.logistimo.inventory.service.InventoryManagementService;
import com.logistimo.returns.models.UpdateStatusModel;
import com.logistimo.returns.utility.ReturnsGsonMapper;
import com.logistimo.returns.utility.ReturnsTestConstant;
import com.logistimo.returns.utility.ReturnsTestUtility;
import com.logistimo.returns.vo.ReturnsItemVO;
import com.logistimo.returns.vo.ReturnsVO;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.DuplicationException;
import com.logistimo.services.ServiceException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

import javax.jdo.PersistenceManager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

/**
 * Created by pratheeka on 23/05/18.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({SecurityUtils.class})
public class ReturnsTransactionHandlerTest {
  @Mock
  InventoryManagementService inventoryManagementService;

  @Spy
  @InjectMocks
  ReturnsTransactionHandler handler = new ReturnsTransactionHandler();

  @Mock
  PersistenceManager pm;

  @Mock
  javax.jdo.Transaction tx;

  @Before
  public void setup() throws ServiceException {
    try {
      mockStatic(SecurityUtils.class);
      SecureUserDetails userDetails = ReturnsTestUtility.getSecureUserDetails();
      PowerMockito.when(SecurityUtils.getUserDetails()).thenReturn(userDetails);
      doReturn(pm).when(handler).getPersistenceManager();
      doReturn(tx).when(pm).currentTransaction();
      when(inventoryManagementService
          .updateInventoryTransactions(eq(1L), anyList(), isNull(), eq(true), eq(false),
              any(PersistenceManager.class)))
          .thenReturn(getErrorResponse());
      when(inventoryManagementService
          .updateInventoryTransactions(eq(2L), anyList(), isNull(), eq(true), eq(false),
              any(PersistenceManager.class)))
          .thenReturn(getResponse());

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test(expected = SystemException.class)
  public void testErrorPostingTransactions() throws ServiceException, DuplicationException {
    ReturnsVO returnsVO = getReturnsVO();
    UpdateStatusModel statusModel = getUpdateStatusModel();
    handler.postTransactions(statusModel, returnsVO, 1L);
  }

  @Test
  public void testPostingTransactions() throws ServiceException, DuplicationException {
    ReturnsVO returnsVO = getReturnsVO();
    UpdateStatusModel statusModel = getUpdateStatusModel();
    handler.postTransactions(statusModel, returnsVO, 2L);
    returnsVO = getBatchReturnsVO();
    handler.postTransactions(statusModel, returnsVO, 2L);
  }

  private ReturnsVO getReturnsVO() {
    ReturnsVO
        returnsVO =
        ReturnsTestUtility.getReturnsVO();
    List<ReturnsItemVO> returnItemList = new ArrayList<>(1);
    ReturnsItemVO vo = getReturnsItemVO();
    returnItemList.add(vo);
    returnsVO.setItems(returnItemList);
    return returnsVO;
  }

  private ReturnsVO getBatchReturnsVO() {
    ReturnsVO
        returnsVO =
        ReturnsTestUtility.getReturnsVO();
    List<ReturnsItemVO> returnItemList = new ArrayList<>(1);
    ReturnsItemVO vo = getBatchReturnsItemVO();
    returnItemList.add(vo);
    returnsVO.setItems(returnItemList);
    return returnsVO;
  }

  private ReturnsItemVO getReturnsItemVO() {
    return ReturnsGsonMapper
        .getTestObject(ReturnsTestConstant.RETURNS_ITEM_NON_BATCH_1, ReturnsItemVO.class);
  }

  private ReturnsItemVO getBatchReturnsItemVO() {
    return ReturnsGsonMapper
        .getTestObject(ReturnsTestConstant.RETURNS_ITEM_BATCH_1, ReturnsItemVO.class);
  }

  private UpdateStatusModel getUpdateStatusModel() {
    return ReturnsGsonMapper
        .getTestObject(ReturnsTestConstant.STATUS_MODEL, UpdateStatusModel.class);
  }

  private List<ITransaction> getTransactionList() {
    List<ITransaction> iTransactions = new ArrayList<>(1);
    Transaction
        transaction =
        ReturnsGsonMapper.getTestObject(ReturnsTestConstant.TRANSACTION, Transaction.class);
    iTransactions.add(transaction);
    return iTransactions;
  }

  private CreateTransactionsReturnModel getErrorResponse() {
    return new CreateTransactionsReturnModel(null, getTransactionList());
  }

  private CreateTransactionsReturnModel getResponse() {
    return new CreateTransactionsReturnModel(getTransactionList(), null);
  }

}
