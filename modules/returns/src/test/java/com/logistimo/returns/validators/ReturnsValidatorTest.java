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

package com.logistimo.returns.validators;

import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.config.models.ReturnsConfig;
import com.logistimo.entities.auth.EntityAuthoriser;
import com.logistimo.exception.InvalidDataException;
import com.logistimo.exception.ValidationException;
import com.logistimo.inventory.entity.jpa.Inventory;
import com.logistimo.inventory.entity.jpa.InventoryBatch;
import com.logistimo.inventory.service.InventoryManagementService;
import com.logistimo.inventory.service.jpa.InventoryService;
import com.logistimo.materials.model.HandlingUnitModel;
import com.logistimo.materials.service.IHandlingUnitService;
import com.logistimo.orders.entity.Order;
import com.logistimo.orders.service.OrderManagementService;
import com.logistimo.returns.Status;
import com.logistimo.returns.exception.ReturnsException;
import com.logistimo.returns.models.ReturnsUpdateModel;
import com.logistimo.returns.service.ReturnsRepository;
import com.logistimo.returns.utility.ReturnsGsonMapper;
import com.logistimo.returns.utility.ReturnsTestConstant;
import com.logistimo.returns.utility.ReturnsTestUtility;
import com.logistimo.returns.vo.ReturnsItemVO;
import com.logistimo.returns.vo.ReturnsStatusVO;
import com.logistimo.returns.vo.ReturnsVO;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.ServiceException;
import com.logistimo.shipments.service.impl.ShipmentService;
import com.logistimo.utils.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.logistimo.returns.utility.ReturnsTestUtility.getFulfilledQuantityModelList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

/**
 * Created by pratheeka on 20/04/18.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({SecurityUtils.class, EntityAuthoriser.class
})
public class ReturnsValidatorTest {

  private ReturnsValidator validator;

  @InjectMocks
  private ReturnsValidationHandler returnsValidationHandler;

  @Mock
  private OrderManagementService orderManagementService;

  @Mock
  private InventoryManagementService inventoryManagementService;

  @Mock
  private InventoryService inventoryService;

  @Mock
  private IHandlingUnitService handlingUnitService;

  @Mock
  private ShipmentService shipmentService;

  @Mock
  private ReturnsRepository returnsRepository;

  @Before
  public void setup() throws ServiceException {
    validator = new ReturnsValidator();
    mockStatic(SecurityUtils.class);
    mockStatic(EntityAuthoriser.class);
    SecureUserDetails userDetails = ReturnsTestUtility.getSecureUserDetails();
    PowerMockito.when(SecurityUtils.getUserDetails()).thenReturn(userDetails);
    PowerMockito.when(EntityAuthoriser.authoriseEntityPerm(1L)).thenReturn(1);
    PowerMockito.when(EntityAuthoriser.authoriseEntityPerm(2L)).thenReturn(2);
  }

  @Test(expected = InvalidDataException.class)
  public void testInvalidReceiveStatusChange() {
    Status oldStatus = Status.OPEN;
    Status newStatus = Status.RECEIVED;
    returnsValidationHandler.validateStatusChange(newStatus, oldStatus);
  }

  @Test(expected = InvalidDataException.class)
  public void testNullStatusChange() {
    Status oldStatus = Status.OPEN;
    returnsValidationHandler.validateStatusChange(null, oldStatus);
  }

  @Test(expected = InvalidDataException.class)
  public void testInvalidCancelStatus() {
    Status oldStatus = Status.CANCELLED;
    Status newStatus = Status.RECEIVED;
    returnsValidationHandler.validateStatusChange(newStatus, oldStatus);
  }

  @Test(expected = InvalidDataException.class)
  public void testForInvalidCancelStatusChange() {
    Status oldStatus = Status.RECEIVED;
    Status newStatus = Status.CANCELLED;
    returnsValidationHandler.validateStatusChange(newStatus, oldStatus);
  }

  @Test(expected = InvalidDataException.class)
  public void testInvalidOpenStatusChange() {
    Status oldStatus = Status.SHIPPED;
    Status newStatus = Status.OPEN;
    returnsValidationHandler.validateStatusChange(newStatus, oldStatus);
  }


  @Test
  public void testStatusChangeAccess() throws ServiceException {
    ReturnsUpdateModel statusModel = new ReturnsUpdateModel();
    statusModel.setStatus(Status.SHIPPED);
    ReturnsVO returnsVO = new ReturnsVO();
    returnsVO.setCustomerId(2L);
    assertTrue(validator.checkAccessForStatusChange(Status.SHIPPED, returnsVO));

    returnsVO.setCustomerId(1L);
    returnsVO.setStatus(new ReturnsStatusVO());
    assertFalse(validator.checkAccessForStatusChange(Status.SHIPPED, returnsVO));
    statusModel.setStatus(Status.RECEIVED);
    returnsVO.setCustomerId(1L);
    returnsVO.setVendorId(2L);
    assertTrue(validator.checkAccessForStatusChange(Status.RECEIVED, returnsVO));
    statusModel.setStatus(Status.RECEIVED);
    returnsVO.setCustomerId(1L);
    returnsVO.setVendorId(1L);
    assertFalse(validator.checkAccessForStatusChange(Status.RECEIVED, returnsVO));
    statusModel.setStatus(Status.CANCELLED);
    returnsVO.setCustomerId(1L);
    returnsVO.setVendorId(1L);
    returnsVO.setCustomerId(2L);
    returnsVO.setVendorId(1L);
    ReturnsStatusVO statusVO = new ReturnsStatusVO();
    statusVO.setStatus(Status.SHIPPED);
    returnsVO.setStatus(statusVO);
    statusModel.setStatus(Status.CANCELLED);
    returnsVO.setVendorId(2L);
    assertTrue(returnsValidationHandler.checkAccessForStatusChange(Status.CANCELLED, returnsVO));
    returnsVO.setVendorId(2L);
    returnsVO.setCustomerId(1L);
    statusVO.setStatus(Status.OPEN);
    returnsVO.setStatus(statusVO);
    assertTrue(returnsValidationHandler.checkAccessForStatusChange(Status.CANCELLED, returnsVO));
  }

  @Test(expected = ValidationException.class)
  public void testReturnsPolicy() throws ServiceException {
    when(inventoryManagementService.getReturnsConfig(any()))
        .thenReturn(Optional.of(new ReturnsConfig()));
    when(orderManagementService.getOrder(any())).thenReturn(getOrder());

    ReturnsConfig returnsConfiguration = new ReturnsConfig();
    Long orderFullfillmentTime = 7L;
    returnsValidationHandler.validateReturnsPolicy(1L, orderFullfillmentTime);
    returnsConfiguration.setIncomingDuration(0);
    validator.validateReturnsPolicy(returnsConfiguration, orderFullfillmentTime);
    returnsConfiguration.setIncomingDuration(3);
    validator.validateReturnsPolicy(returnsConfiguration, null);
    orderFullfillmentTime = 2L;
    validator.validateReturnsPolicy(returnsConfiguration, orderFullfillmentTime);
    orderFullfillmentTime = 7L;
    validator.validateReturnsPolicy(returnsConfiguration, orderFullfillmentTime);
  }

  @Test
  public void testReturnedQtyLesserThanFulfilledQty() {
    List<ReturnsItemVO>
        returnItemList =
        getReturnsItemVOList(ReturnsTestConstant.RETURNS_ITEM_NON_BATCH_1);
    returnItemList.addAll(getReturnsItemVOList(ReturnsTestConstant.RETURNS_ITEM_BATCH_1));
    returnsValidationHandler.validateHandlingUnit(returnItemList);

  }


  @Test(expected = ValidationException.class)
  public void testReturnedQtyNotMultipleOfHandlingUnit() {
    List<ReturnsItemVO> returnItemList =
        getReturnsItemVOList(ReturnsTestConstant.RETURNS_ITEM_NON_BATCH_1);
    returnItemList.addAll(getReturnsItemVOList(ReturnsTestConstant.RETURNS_ITEM_BATCH_1));
    List<HandlingUnitModel> handlingUnitModelList = new ArrayList<>(1);
    HandlingUnitModel handlingUnitModel =
        ReturnsGsonMapper
            .getTestObject(ReturnsTestConstant.HANDLING_UNIT_1, HandlingUnitModel.class);
    handlingUnitModelList.add(handlingUnitModel);
    when(handlingUnitService.getHandlingUnitDataByMaterialIds(anyList()))
        .thenReturn(handlingUnitModelList);
    returnsValidationHandler.validateHandlingUnit(returnItemList);

  }

  @Test(expected = Exception.class)
  public void testReturnedQtyGreaterThanShippedQty() throws ReturnsException {
    List<ReturnsItemVO> returnItemList =
        getReturnsItemVOList(ReturnsTestConstant.RETURNS_ITEM_BATCH_2);
    ReturnsVO returnsVO = new ReturnsVO();
    returnsVO.setItems(returnItemList);
    Inventory inventory =
        ReturnsGsonMapper.getTestObject(ReturnsTestConstant.INVENTORY_1, Inventory.class);
    List<Inventory> inventories = new ArrayList<>(1);
    inventories.add(inventory);

    InventoryBatch inventoryBatch =
        ReturnsGsonMapper
            .getTestObject(ReturnsTestConstant.INVENTORY_BATCH_1, InventoryBatch.class);
    List<InventoryBatch> inventoryBatches = new ArrayList<>(1);
    inventoryBatches.add(inventoryBatch);
    inventoryBatches.add(ReturnsGsonMapper
        .getTestObject(ReturnsTestConstant.INVENTORY_BATCH_2, InventoryBatch.class));
    when(inventoryService.getInventoryBatchesByKioskId(any(), anyList()))
        .thenReturn(inventoryBatches);
    when(inventoryService.getInventoriesByKioskId(any(), anyList())).thenReturn(inventories);
    returnsValidationHandler.validateShippingQtyAgainstAvailableQty(returnsVO);
  }

  public List<ReturnsItemVO> getReturnsItemVOList(String jsonString) {
    List<ReturnsItemVO> returnItemList = new ArrayList<>(1);
    ReturnsItemVO
        vo =
        ReturnsGsonMapper.getTestObject(jsonString, ReturnsItemVO.class);
    returnItemList.add(vo);
    return returnItemList;
  }

  @Test
  public void testValidateTotalReturnedQuantity() throws IOException, ServiceException {
    ReturnsVO returnsVO = ReturnsTestUtility.getUpdatedReturnsVO();

    when(shipmentService.getFulfilledQuantityByOrderId(returnsVO.getOrderId(),
        Stream.toList(returnsVO.getItems(), ReturnsItemVO::getMaterialId))).thenReturn(
        getFulfilledQuantityModelList());
    when(returnsRepository
        .getReturnsQuantityDetailsByOrderId(returnsVO.getOrderId(), returnsVO.getId()))
        .thenReturn(ReturnsTestUtility.getTotalReturnedQty());
    returnsValidationHandler
        .validateQuantity(returnsVO.getOrderId(), returnsVO.getId(), returnsVO.getItems());
  }

  private Order getOrder() {
    Order order = new Order();
    order.setCreatedOn(new Date());
    return order;
  }

}
