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

package com.logistimo.returns.service;

import com.logistimo.activity.service.impl.ActivitiesServiceImpl;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.config.models.ReturnsConfig;
import com.logistimo.constants.Constants;
import com.logistimo.conversations.service.impl.ConversationsServiceImpl;
import com.logistimo.exception.InvalidServiceException;
import com.logistimo.inventory.service.InventoryManagementService;
import com.logistimo.materials.service.IHandlingUnitService;
import com.logistimo.orders.entity.Order;
import com.logistimo.orders.service.OrderManagementService;
import com.logistimo.orders.service.impl.DemandService;
import com.logistimo.pagination.Results;
import com.logistimo.returns.Status;
import com.logistimo.returns.actions.CreateReturnsAction;
import com.logistimo.returns.actions.GetReturnsAction;
import com.logistimo.returns.actions.UpdateReturnAction;
import com.logistimo.returns.actions.UpdateReturnsTrackingDetailAction;
import com.logistimo.returns.exception.ReturnsException;
import com.logistimo.returns.models.ReturnsFilters;
import com.logistimo.returns.models.ReturnsUpdateModel;
import com.logistimo.returns.transactions.ReturnsTransactionHandler;
import com.logistimo.returns.utility.ReturnsTestUtility;
import com.logistimo.returns.validators.ReturnsValidationHandler;
import com.logistimo.returns.vo.ReturnsQuantityDetailsVO;
import com.logistimo.returns.vo.ReturnsVO;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.DuplicationException;
import com.logistimo.services.ServiceException;
import com.logistimo.shipments.FulfilledQuantityModel;
import com.logistimo.shipments.service.impl.ShipmentService;
import com.logistimo.utils.LockUtil;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.logistimo.returns.utility.ReturnsTestUtility.getReturnsVO;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

/**
 * Created by pratheeka on 21/05/18.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({SecurityUtils.class, LockUtil.class, DomainConfig.class})
public class ReturnsServiceTest {

  private static final String COMMENT = "Added comment";

  @Mock
  private ConversationsServiceImpl conversationService;

  @Mock
  private OrderManagementService orderManagementService;

  @Mock
  private ReturnsRepository returnsRepository;

  @Mock
  private ReturnsTransactionHandler returnsTransactionHandler;


  @InjectMocks
  private ReturnsService returnsService;

  @Mock
  private ReturnsStatusHistoryService returnsStatusHistoryService;

  @Mock
  private CreateReturnsAction createReturnsAction;

  @Mock
  private UpdateReturnAction updateReturnAction;

  @Mock
  private ReturnsCommentService returnsCommentService;

  @Mock
  private UpdateReturnsTrackingDetailAction updateReturnsTrackingDetailAction;

  @Mock
  private ShipmentService shipmentService;

  @Mock
  private IHandlingUnitService handlingUnitService;

  @Mock
  private ReturnsValidationHandler validationHandler;

  @Mock
  private InventoryManagementService inventoryManagementService;

  @Mock
  private DemandService demandService;

  @Mock
  private ActivitiesServiceImpl activityService;

  @Mock
  private GetReturnsAction getReturnsAction;


  @Before
  public void setup() {

    mockStatic(LockUtil.class);
    mockStatic(SecurityUtils.class);
    mockStatic(DomainConfig.class);

    try {

      when(returnsService.addComment(any(), any(), any(), any())).thenReturn(COMMENT);
      doNothing().when(returnsStatusHistoryService).addStatusHistory(any(), any(), any(), any());
      when(shipmentService.getFulfilledQuantityByOrderId(any(), anyList())).thenReturn(null);
      when(handlingUnitService.getHandlingUnitDataByMaterialIds(anyList())).thenReturn(null);
      when(inventoryManagementService.getReturnsConfig(any()))
          .thenReturn(Optional.of(new ReturnsConfig()));
      doNothing().when(validationHandler).validateHandlingUnit(anyList());
      SecureUserDetails userDetails = ReturnsTestUtility.getSecureUserDetails();
      PowerMockito.when(SecurityUtils.getUserDetails()).thenReturn(userDetails);
      when(orderManagementService.getOrder(any())).thenReturn(getOrder());
      when(validationHandler.checkAccessForStatusChange(any(), any())).thenReturn(true);
      when(inventoryManagementService.getInventoryByKiosk(any(), isNull()))
          .thenReturn(new Results());
      doNothing().when(validationHandler).validateShippingQtyAgainstAvailableQty(any());
      PowerMockito.when(DomainConfig.getInstance(getReturnsVO().getSourceDomain()))
          .thenReturn(getDomainConfig());
    } catch (Exception ignored) {
      ignored.printStackTrace();
    }
  }

  @Test(expected = InvalidServiceException.class)
  public void testErrorAcquiringLock() throws ServiceException, IOException {
    PowerMockito.when(LockUtil.lock(Constants.TX_O + 2L))
        .thenReturn(LockUtil.LockStatus.FAILED_TO_LOCK);

    ReturnsVO returnsVO = getReturnsVO();
    returnsVO.setOrderId(2L);
    returnsService.createReturns(returnsVO);
  }

  @Test
  public void testCreateReturns() throws ServiceException, IOException {
    ReturnsVO returnsVO = ReturnsTestUtility.getReturnsVO();
    returnsVO.setOrderId(1L);
    PowerMockito.when(LockUtil.lock(Constants.TX_O + 1L)).thenReturn(LockUtil.LockStatus.NEW_LOCK);
    when(createReturnsAction.invoke(any())).thenReturn(returnsVO);
    PowerMockito.when(LockUtil.isLocked(LockUtil.LockStatus.NEW_LOCK)).thenReturn(true);
    ReturnsVO savedReturns = returnsService.createReturns(returnsVO);
    Assert.assertEquals(savedReturns.getId(), returnsVO.getId());
    Assert.assertEquals(returnsVO.getItems().size(),savedReturns.getItems().size());
  }

  @Test
  public void testGetReturn() throws IOException {
    ReturnsVO returnsVO = ReturnsTestUtility.getReturnsVO();
    when(getReturnsAction.invoke(returnsVO.getId())).thenReturn(returnsVO);
    ReturnsVO resultVO = returnsService.getReturn(returnsVO.getId());
    Assert.assertEquals(resultVO.getId(), returnsVO.getId());
  }

  @Test
  public void testReturnsCount() {
    ReturnsFilters returnsFilters = ReturnsTestUtility.getReturnsFilters();
    when(returnsRepository.getReturnsCount(returnsFilters)).thenReturn(80L);
    assertEquals(returnsService.getReturnsCount(returnsFilters).longValue(), 80L);
  }


  @Test
  public void testGetReturns() throws IOException {
    when(returnsRepository.getReturns(ReturnsTestUtility.getReturnsFilters(), false))
        .thenReturn(ReturnsTestUtility.getReturnVOList());
    assertEquals(returnsService.getReturns(ReturnsTestUtility.getReturnsFilters(), false).size(),
        1);
  }

  @Test
  public void testGetReturnById() throws IOException {
    when(getReturnsAction.invoke(any())).thenReturn(ReturnsTestUtility.getReturnsVO());
    assertEquals(returnsService.getReturn(1L).getId(), getReturnsVO().getId());
  }

  @Test
  public void testUpdateReturnsStatusAsShipped()
      throws ServiceException, DuplicationException, IOException, ReturnsException {
    PowerMockito.when(LockUtil.lock(Constants.TX_O + 1L)).thenReturn(LockUtil.LockStatus.NEW_LOCK);
    PowerMockito.when(LockUtil.isLocked(LockUtil.LockStatus.NEW_LOCK)).thenReturn(true);

    ReturnsVO updatedReturnsVO = ReturnsTestUtility.getUpdatedReturnsVO();
    updatedReturnsVO.setStatusValue(Status.SHIPPED);
    ReturnsVO oldReturnVO = ReturnsTestUtility.getReturnsVO();
    oldReturnVO.setOrderId(1L);
    oldReturnVO.setStatusValue(Status.OPEN);
    when(getReturnsAction.invoke(updatedReturnsVO.getId())).thenReturn(oldReturnVO);
    when(updateReturnAction.invoke(any())).thenReturn(updatedReturnsVO);
    assertEquals(returnsService.updateReturnsStatus(updatedReturnsVO, 1L).getId(),
        Long.valueOf(1L));
  }

  @Test
  public void testUpdateReturnsStatusAsReceived()
      throws ServiceException, DuplicationException, IOException, ReturnsException {
    PowerMockito.when(LockUtil.lock(Constants.TX_O + 1L)).thenReturn(LockUtil.LockStatus.NEW_LOCK);
    PowerMockito.when(LockUtil.isLocked(LockUtil.LockStatus.NEW_LOCK)).thenReturn(true);
    ReturnsVO updatedReturnsVO = ReturnsTestUtility.getUpdatedReturnsVO();
    updatedReturnsVO.setStatusValue(Status.RECEIVED);
    ReturnsVO oldReturnVO = ReturnsTestUtility.getReturnsVO();
    oldReturnVO.setStatusValue(Status.OPEN);
    when(getReturnsAction.invoke(updatedReturnsVO.getId())).thenReturn(oldReturnVO);
    oldReturnVO.setOrderId(1L);
    when(updateReturnAction.invoke(any())).thenReturn(updatedReturnsVO);
    assertEquals(returnsService.updateReturnsStatus(updatedReturnsVO, 1L).getId(),
        Long.valueOf(1L));
  }

  @Test
  public void testCancelReturns()
      throws ServiceException, DuplicationException, IOException, ReturnsException {
    ReturnsVO returnsVO = getReturnsVO();
    returnsVO.setOrderId(1L);
    PowerMockito.when(LockUtil.lock(Constants.TX_O + 1L)).thenReturn(LockUtil.LockStatus.NEW_LOCK);
    ReturnsUpdateModel statusModel = new ReturnsUpdateModel();
    statusModel.setStatus(Status.CANCELLED);
    statusModel.setReturnId(1L);
    ReturnsVO updatedReturnVO = ReturnsTestUtility.getUpdatedReturnsVO();
    when(returnsRepository.getReturnsById(updatedReturnVO.getId())).thenReturn(returnsVO);
    statusModel.setItems(updatedReturnVO.getItems());
    when(getReturnsAction.invoke(any())).thenReturn(returnsVO);
    PowerMockito.when(LockUtil.isLocked(LockUtil.LockStatus.NEW_LOCK)).thenReturn(true);
    when(updateReturnAction.invoke(any())).thenReturn(returnsVO);
    assertEquals(returnsService.updateReturnsStatus(returnsVO, 1L).getId(), Long.valueOf(1L));
  }


  private DomainConfig getDomainConfig() {
    DomainConfig config = new DomainConfig();
    config.setAutoGI(true);
    return config;
  }

  private Order getOrder() {
    Order order = new Order();
    order.setCreatedOn(new Date());
    return order;
  }


  @Test
  public void testGetReturnItemDetails() throws ServiceException {

    List<FulfilledQuantityModel> shipmentList = ReturnsTestUtility.getFulfilledQuantityModelList();
    List<ReturnsQuantityDetailsVO>
        totalReturnsQtyList = ReturnsTestUtility.getTotalReturnedQty();
    when(shipmentService.getFulfilledQuantityByOrderId(any(), any())).thenReturn(shipmentList);
    when(returnsRepository.getAllReturnsQuantityByOrderId(any())).thenReturn(totalReturnsQtyList);
    returnsService.getReturnsQuantityDetails(1L);

  }

}
