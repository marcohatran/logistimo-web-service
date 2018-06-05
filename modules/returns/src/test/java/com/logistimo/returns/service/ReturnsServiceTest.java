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

import com.logistimo.AppFactory;
import com.logistimo.activity.entity.IActivity;
import com.logistimo.activity.service.impl.ActivitiesServiceImpl;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.config.models.ReturnsConfig;
import com.logistimo.constants.Constants;
import com.logistimo.conversations.service.impl.ConversationsServiceImpl;
import com.logistimo.exception.InvalidServiceException;
import com.logistimo.exception.ValidationException;
import com.logistimo.inventory.service.InventoryManagementService;
import com.logistimo.materials.service.IHandlingUnitService;
import com.logistimo.orders.entity.Order;
import com.logistimo.orders.service.OrderManagementService;
import com.logistimo.orders.service.impl.DemandService;
import com.logistimo.pagination.Results;
import com.logistimo.returns.Status;
import com.logistimo.returns.models.ReturnsFilters;
import com.logistimo.returns.models.UpdateStatusModel;
import com.logistimo.returns.transactions.ReturnsTransactionHandler;
import com.logistimo.returns.utility.ReturnsGsonMapper;
import com.logistimo.returns.utility.ReturnsTestConstant;
import com.logistimo.returns.utility.ReturnsTestUtility;
import com.logistimo.returns.validators.ReturnsValidator;
import com.logistimo.returns.vo.ReturnsItemVO;
import com.logistimo.returns.vo.ReturnsVO;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.DuplicationException;
import com.logistimo.services.ServiceException;
import com.logistimo.shipments.service.impl.ShipmentService;
import com.logistimo.utils.LockUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.logistimo.returns.utility.ReturnsTestUtility.getReturnsVO;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
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
  private ShipmentService shipmentService;

  @Mock
  private IHandlingUnitService handlingUnitService;

  @Mock
  private ReturnsValidator validator;

  @Mock
  private InventoryManagementService inventoryManagementService;

  @Mock
  private DemandService demandService;

  @Mock
  private ActivitiesServiceImpl activityService;


  @Before
  public void setup() {

    mockStatic(LockUtil.class);
    mockStatic(SecurityUtils.class);
    mockStatic(DomainConfig.class);

    try {
      SecureUserDetails userDetails = ReturnsTestUtility.getSecureUserDetails();
      PowerMockito.when(SecurityUtils.getUserDetails()).thenReturn(userDetails);
      PowerMockito.when(LockUtil.lock(Constants.TX_O + 1L))
          .thenReturn(LockUtil.LockStatus.NEW_LOCK);
      PowerMockito.when(LockUtil.isLocked(LockUtil.LockStatus.NEW_LOCK)).thenReturn(true);
      PowerMockito.when(LockUtil.lock(Constants.TX_O + 2L))
          .thenReturn(LockUtil.LockStatus.FAILED_TO_LOCK);
      PowerMockito.when(LockUtil.isLocked(LockUtil.LockStatus.FAILED_TO_LOCK)).thenReturn(false);

      when(conversationService
          .addMessageToConversation(eq(IActivity.TYPE.RETURNS.name()), eq(String.valueOf(1L)),
              eq(COMMENT), eq("TEST"), eq(Collections.singleton("RETURNS:1")),
              eq(1L), isNull())).thenReturn(COMMENT);

      when(shipmentService.getFulfilledQuantityByOrderId(any(), anyList())).thenReturn(null);
      when(handlingUnitService.getHandlingUnitDataByMaterialIds(anyList())).thenReturn(null);
      when(inventoryManagementService.getReturnsConfig(any()))
          .thenReturn(Optional.of(new ReturnsConfig()));
      when(validator.validateReturnedQuantity(anyList(), anyList(), anyList())).thenReturn(true);
      when(returnsRepository.getReturnsCount(any())).thenReturn(80L);
      when(returnsRepository.getReturnedItems(1L)).thenReturn(getReturnsItemVOList());
      when(returnsRepository.getReturns(any())).thenReturn(getReturnVOList());
      when(returnsRepository.getReturnsById(1L)).thenReturn(getReturnsVO());
      when(demandService.updateDemandReturns(any(), any(), eq(false))).thenReturn(null);
      when(activityService.saveActivity(any())).thenReturn(null);
      when(orderManagementService.getOrder(any())).thenReturn(getOrder());
      when(inventoryManagementService.getInventoryByKiosk(any(), isNull()))
          .thenReturn(new Results());
      doNothing().when(validator).validateShippedQuantity(anyList(), anyList());
      doNothing().when(validator).validateStatusChange(any(), any());
      when(validator.checkAccessForStatusChange(any(), any())).thenReturn(true);
      doNothing().when(returnsTransactionHandler).postTransactions(any(), any(), any());
      PowerMockito.when(DomainConfig.getInstance(getReturnsVO().getSourceDomain()))
          .thenReturn(getDomainConfig());
    } catch (Exception ignored) {
      ignored.printStackTrace();
    }
  }

  @Test(expected = InvalidServiceException.class)
  public void testErrorAcquiringLock() throws ServiceException {
    ReturnsVO returnsVO = getReturnsVO();
    returnsVO.setOrderId(2L);
    returnsService.createReturns(returnsVO);
  }

  @Test
  public void testCreateReturns() throws ServiceException {
    ReturnsVO returnsVO = getReturnsVO();
    returnsVO.setOrderId(1L);
    returnsVO.setItems(getReturnsItemVOList());
    returnsService.createReturns(returnsVO);
  }

  @Test(expected = ValidationException.class)
  public void testNoItems() throws ServiceException {
    ReturnsVO returnsVO = getReturnsVO();
    returnsVO.setOrderId(1L);
    assertEquals(returnsService.createReturns(returnsVO).getId(), Long.valueOf(1L));
  }

  @Test
  public void testReturnsCount() {
    ReturnsFilters returnsFilters = getReturnsFilters();
    assertEquals(returnsService.getReturnsCount(returnsFilters).longValue(), 80L);
  }

  private ReturnsFilters getReturnsFilters() {
    ReturnsFilters returnsFilters = ReturnsFilters.builder().build();
    returnsFilters.setUserId(SecurityUtils.getUserDetails().getUsername());
    return returnsFilters;
  }

  @Test(expected = ValidationException.class)
  public void testErrorGetReturnsItem() {
    returnsService.getReturnsItem(null);
  }

  @Test
  public void testGetReturnsItem() {
    assertEquals(returnsService.getReturnsItem(1L).size(), 1);
  }

  @Test
  public void testGetReturns() {
    assertEquals(returnsService.getReturns(getReturnsFilters()).size(), 1);
  }

  @Test
  public void testGetReturnById() {
    assertEquals(returnsService.getReturnsById(1L).getId(), getReturnsVO().getId());
  }

  @Test
  public void testAddComment() {
    assertEquals(returnsService.postComment(1L, COMMENT, "TEST", 1L), COMMENT);
    assertEquals(returnsService.postComment(1L, "", "TEST", 1L), null);

  }

  @Test
  public void testUpdateReturnsStatus() throws ServiceException, DuplicationException {
    UpdateStatusModel statusModel = new UpdateStatusModel();
    statusModel.setStatus(Status.SHIPPED);
    statusModel.setReturnId(1L);
    assertEquals(returnsService.updateReturnsStatus(statusModel).getId(), Long.valueOf(1L));
  }

  @Test
  public void testCancelReturns() throws ServiceException, DuplicationException {
    UpdateStatusModel statusModel = new UpdateStatusModel();
    statusModel.setStatus(Status.CANCELLED);
    statusModel.setReturnId(1L);
    assertEquals(returnsService.updateReturnsStatus(statusModel).getId(),Long.valueOf(1L));
  }

  private List<ReturnsVO> getReturnVOList() {
    ReturnsVO vo = getReturnsVO();
    List<ReturnsVO> list = new ArrayList<>(1);
    list.add(vo);
    return list;
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

  private List<ReturnsItemVO> getReturnsItemVOList() {
    ReturnsItemVO vo = ReturnsGsonMapper
        .getTestObject(ReturnsTestConstant.RETURNS_ITEM_BATCH_1, ReturnsItemVO.class);
    List<ReturnsItemVO> list = new ArrayList<>(1);
    list.add(vo);
    return list;
  }
}
