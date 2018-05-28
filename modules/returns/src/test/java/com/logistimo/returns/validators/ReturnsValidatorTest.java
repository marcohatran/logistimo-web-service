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
import com.logistimo.inventory.entity.IInvntry;
import com.logistimo.inventory.entity.Invntry;
import com.logistimo.materials.model.HandlingUnitModel;
import com.logistimo.returns.Status;
import com.logistimo.returns.models.UpdateStatusModel;
import com.logistimo.returns.utility.ReturnsGsonMapper;
import com.logistimo.returns.utility.ReturnsTestConstant;
import com.logistimo.returns.utility.ReturnsTestUtility;
import com.logistimo.returns.vo.ReturnsItemVO;
import com.logistimo.returns.vo.ReturnsStatusVO;
import com.logistimo.returns.vo.ReturnsVO;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.ServiceException;
import com.logistimo.shipments.FulfilledQuantityModel;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

/**
 * Created by pratheeka on 20/04/18.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({SecurityUtils.class, EntityAuthoriser.class
})
public class ReturnsValidatorTest {
  private ReturnsValidator validator;

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
    validator.validateStatusChange(newStatus, oldStatus);
  }

  @Test(expected = InvalidDataException.class)
  public void testNullStatusChange() {
    Status oldStatus = Status.OPEN;
    validator.validateStatusChange(null, oldStatus);
  }

  @Test(expected = InvalidDataException.class)
  public void testInvalidCancelStatus() {
    Status oldStatus = Status.CANCELLED;
    Status newStatus = Status.RECEIVED;
    validator.validateStatusChange(newStatus, oldStatus);
  }

  @Test(expected = InvalidDataException.class)
  public void testForInvalidCancelStatusChange() {
    Status oldStatus = Status.RECEIVED;
    Status newStatus = Status.CANCELLED;
    validator.validateStatusChange(newStatus, oldStatus);
  }

  @Test(expected = InvalidDataException.class)
  public void testInvalidOpentatusChange() {
    Status oldStatus = Status.SHIPPED;
    Status newStatus = Status.OPEN;
    validator.validateStatusChange(newStatus, oldStatus);
  }


  @Test
  public void testStatusChangeAccess() throws ServiceException {
    UpdateStatusModel statusModel = new UpdateStatusModel();
    statusModel.setStatus(Status.SHIPPED);
    ReturnsVO returnsVO = new ReturnsVO();
    returnsVO.setCustomerId(2L);
    assertTrue(validator.checkAccessForStatusChange(statusModel, returnsVO));
    returnsVO.setCustomerId(1L);
    returnsVO.setStatus(new ReturnsStatusVO());
    assertFalse(validator.checkAccessForStatusChange(statusModel, returnsVO));
    statusModel.setStatus(Status.RECEIVED);
    returnsVO.setCustomerId(1L);
    returnsVO.setVendorId(2L);
    assertTrue(validator.checkAccessForStatusChange(statusModel, returnsVO));
    statusModel.setStatus(Status.RECEIVED);
    returnsVO.setCustomerId(1L);
    returnsVO.setVendorId(1L);
    assertFalse(validator.checkAccessForStatusChange(statusModel, returnsVO));
    statusModel.setStatus(Status.CANCELLED);
    returnsVO.setCustomerId(1L);
    returnsVO.setVendorId(1L);
    assertFalse(validator.checkAccessForStatusChange(statusModel, returnsVO));
    returnsVO.setVendorId(2L);
    assertTrue(validator.checkAccessForStatusChange(statusModel, returnsVO));
    returnsVO.setCustomerId(2L);
    returnsVO.setVendorId(1L);
    assertTrue(validator.checkAccessForStatusChange(statusModel, returnsVO));
    ReturnsStatusVO statusVO = new ReturnsStatusVO();
    statusVO.setStatus(Status.SHIPPED);
    returnsVO.setStatus(statusVO);
    statusModel.setStatus(Status.CANCELLED);
    returnsVO.setVendorId(2L);
    assertTrue(validator.checkAccessForStatusChange(statusModel, returnsVO));
  }

  @Test(expected = ValidationException.class)
  public void testReturnsPolicy() {
    ReturnsConfig returnsConfiguration = new ReturnsConfig();
    Long orderFullfillmentTime = 7L;
    validator.validateReturnsPolicy(returnsConfiguration, orderFullfillmentTime);
    returnsConfiguration.setIncomingDuration(0);
    validator.validateReturnsPolicy(returnsConfiguration, orderFullfillmentTime);
    returnsConfiguration.setIncomingDuration(3);
    validator.validateReturnsPolicy(returnsConfiguration, null);
    orderFullfillmentTime = 2L;
    validator.validateReturnsPolicy(returnsConfiguration, orderFullfillmentTime);
    orderFullfillmentTime = 7L;
    validator.validateReturnsPolicy(returnsConfiguration, orderFullfillmentTime);
  }

  @Test(expected = InvalidDataException.class)
  public void validateShipments() {
    validator.validateReturnedQuantity(null, null, null);
  }

  private List<FulfilledQuantityModel> getFulfilledQtyList() {
    List<FulfilledQuantityModel> fulfilledQuantityModelList = new ArrayList<>(2);
    FulfilledQuantityModel
        fulfilledQuantityModel =
        ReturnsGsonMapper
            .getTestObject(ReturnsTestConstant.FULFILLED_QTY_MODEL_1, FulfilledQuantityModel.class);
    fulfilledQuantityModelList.add(fulfilledQuantityModel);
    FulfilledQuantityModel
        fulfilledQuantityModel2 =
        ReturnsGsonMapper
            .getTestObject(ReturnsTestConstant.FULFILLED_QTY_MODEL_2, FulfilledQuantityModel.class);
    fulfilledQuantityModelList.add(fulfilledQuantityModel2);
    FulfilledQuantityModel
        fulfilledQuantityModelNonBatch =
        ReturnsGsonMapper
            .getTestObject(ReturnsTestConstant.FULFILLED_QTY_MODEL_3, FulfilledQuantityModel.class);
    fulfilledQuantityModelList.add(fulfilledQuantityModelNonBatch);
    return fulfilledQuantityModelList;
  }

  @Test(expected = InvalidDataException.class)
  public void testNullFulfilledQty() {
    List<ReturnsItemVO>
        returnItemList =
        getReturnsItemVOList(ReturnsTestConstant.RETURNS_ITEM_BATCH_2);
    validator.validateReturnedQuantity(returnItemList, null, null);
  }

  @Test(expected = ValidationException.class)
  public void testBatchReturnedQtyGreaterThanFulfilledQty() {
    List<ReturnsItemVO>
        returnItemList =
        getReturnsItemVOList(ReturnsTestConstant.RETURNS_ITEM_BATCH_2);
    List<FulfilledQuantityModel> fulfilledQuantityModelList = getFulfilledQtyList();
    validator.validateReturnedQuantity(returnItemList, fulfilledQuantityModelList, null);
  }

  @Test(expected = ValidationException.class)
  public void testNonBatchReturnedQtyGreaterThanFulfilledQty() {
    List<ReturnsItemVO>
        returnItemList =
        getReturnsItemVOList(ReturnsTestConstant.RETURNS_ITEM_NON_BATCH_2);
    List<FulfilledQuantityModel> fulfilledQuantityModelList = getFulfilledQtyList();
    validator.validateReturnedQuantity(returnItemList, fulfilledQuantityModelList, null);
  }

  @Test
  public void testReturnedQtyLesserThanFulfilledQty() {
    List<ReturnsItemVO>
        returnItemList =
        getReturnsItemVOList(ReturnsTestConstant.RETURNS_ITEM_NON_BATCH_1);
    returnItemList.addAll(getReturnsItemVOList(ReturnsTestConstant.RETURNS_ITEM_BATCH_1));
    List<FulfilledQuantityModel> fulfilledQuantityModelList = getFulfilledQtyList();
    assertTrue(
        validator.validateReturnedQuantity(returnItemList, fulfilledQuantityModelList, null));

  }


  @Test(expected = ValidationException.class)
  public void testReturnedQtyNotMultipleOfHandlingUnit() {
    List<ReturnsItemVO>
        returnItemList =
        getReturnsItemVOList(ReturnsTestConstant.RETURNS_ITEM_NON_BATCH_1);
    returnItemList.addAll(getReturnsItemVOList(ReturnsTestConstant.RETURNS_ITEM_BATCH_1));
    List<FulfilledQuantityModel> fulfilledQuantityModelList = getFulfilledQtyList();
    List<HandlingUnitModel> handlingUnitModelList = new ArrayList<>(1);
    HandlingUnitModel
        handlingUnitModel =
        ReturnsGsonMapper
            .getTestObject(ReturnsTestConstant.HANDLING_UNIT_1, HandlingUnitModel.class);
    handlingUnitModelList.add(handlingUnitModel);
    assertTrue(validator.validateReturnedQuantity(returnItemList, fulfilledQuantityModelList,
        handlingUnitModelList));

  }

  @Test(expected = ValidationException.class)
  public void testReturnedQtyGreaterThanShippedQty() {
    List<ReturnsItemVO>
        returnItemList =
        getReturnsItemVOList(ReturnsTestConstant.RETURNS_ITEM_BATCH_2);
    Invntry
        iInvntry =
        ReturnsGsonMapper.getTestObject(ReturnsTestConstant.INVENTORY_1, Invntry.class);
    List<IInvntry> invntryList = new ArrayList<>(1);
    invntryList.add(iInvntry);
    validator.validateShippedQuantity(returnItemList, invntryList);
  }

  public List<ReturnsItemVO> getReturnsItemVOList(String jsonString) {
    List<ReturnsItemVO> returnItemList = new ArrayList<>(1);
    ReturnsItemVO
        vo =
        ReturnsGsonMapper.getTestObject(jsonString, ReturnsItemVO.class);
    returnItemList.add(vo);
    return returnItemList;
  }


}
