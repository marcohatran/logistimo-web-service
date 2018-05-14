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
import com.logistimo.returns.Status;
import com.logistimo.returns.models.UpdateStatusModel;
import com.logistimo.returns.vo.ReturnsItemBatchVO;
import com.logistimo.returns.vo.ReturnsItemVO;
import com.logistimo.returns.vo.ReturnsStatusVO;
import com.logistimo.returns.vo.ReturnsVO;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.ServiceException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

/**
 * Created by pratheeka on 20/04/18.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({SecurityUtils.class, EntityAuthoriser.class})
public class ReturnsValidatorTest {
  private ReturnsValidator validator;
  public static final String TEST_USER = "test";

  @Before
  public void setup() throws ServiceException {
    validator = new ReturnsValidator();
    mockStatic(SecurityUtils.class);
    mockStatic(EntityAuthoriser.class);
    SecureUserDetails userDetails = getSecureUserDetails();
    PowerMockito.when(SecurityUtils.getUserDetails()).thenReturn(userDetails);
    PowerMockito.when(EntityAuthoriser.authoriseEntityPerm(1l)).thenReturn(1);
    PowerMockito.when(EntityAuthoriser.authoriseEntityPerm(2l)).thenReturn(2);
  }

  @Test(expected = InvalidDataException.class)
  public void testInvalidStatusChange() {
    Status oldStatus = Status.OPEN;
    Status newStatus = Status.RECEIVED;
    validator.validateStatusChange(newStatus, oldStatus);
  }

  @Test(expected = InvalidDataException.class)
  public void testForNullStatus() {
    Status oldStatus = Status.OPEN;
    validator.validateStatusChange(null, oldStatus);
  }

  @Test(expected = InvalidDataException.class)
  public void testForInvalidCancelStatus() {
    Status oldStatus = Status.CANCELLED;
    Status newStatus = Status.RECEIVED;
    validator.validateStatusChange(newStatus, oldStatus);
  }

  @Test(expected = InvalidDataException.class)
  public void testForInvalidReceivedStatus() {
    Status oldStatus = Status.RECEIVED;
    Status newStatus = Status.CANCELLED;
    validator.validateStatusChange(newStatus, oldStatus);
  }

  @Test(expected = InvalidDataException.class)
  public void testForInvalidShippedStatusChange() {
    Status oldStatus = Status.SHIPPED;
    Status newStatus = Status.OPEN;
    validator.validateStatusChange(newStatus, oldStatus);
  }

  @Test
  public void testAccess() throws ServiceException {
    UpdateStatusModel statusModel = new UpdateStatusModel();
    statusModel.setStatus(Status.SHIPPED);
    ReturnsVO returnsVO = new ReturnsVO();
    returnsVO.setCustomerId(2l);
    assertTrue(validator.checkAccessForStatusChange(statusModel, returnsVO));
    returnsVO.setCustomerId(1l);
    returnsVO.setStatus(new ReturnsStatusVO());
    assertFalse(validator.checkAccessForStatusChange(statusModel, returnsVO));
    statusModel.setStatus(Status.RECEIVED);
    returnsVO.setCustomerId(1l);
    returnsVO.setVendorId(2l);
    assertTrue(validator.checkAccessForStatusChange(statusModel, returnsVO));
    statusModel.setStatus(Status.RECEIVED);
    returnsVO.setCustomerId(1l);
    returnsVO.setVendorId(1l);
    assertFalse(validator.checkAccessForStatusChange(statusModel, returnsVO));
    statusModel.setStatus(Status.CANCELLED);
    returnsVO.setCustomerId(1l);
    returnsVO.setVendorId(1l);
    assertFalse(validator.checkAccessForStatusChange(statusModel, returnsVO));
    returnsVO.setVendorId(2l);
    assertTrue(validator.checkAccessForStatusChange(statusModel, returnsVO));
    returnsVO.setCustomerId(2l);
    returnsVO.setVendorId(1l);
    assertTrue(validator.checkAccessForStatusChange(statusModel, returnsVO));
    ReturnsStatusVO statusVO = new ReturnsStatusVO();
    statusVO.setStatus(Status.SHIPPED);
    returnsVO.setStatus(statusVO);
    statusModel.setStatus(Status.CANCELLED);
    returnsVO.setVendorId(2l);
    assertTrue(validator.checkAccessForStatusChange(statusModel, returnsVO));
  }

  @Test(expected = ValidationException.class)
  public void policyTest() {
    ReturnsConfig returnsConfiguration = new ReturnsConfig();
    Long orderFullfillmentTime = 7l;
    validator.validateReturnsPolicy(returnsConfiguration, orderFullfillmentTime);
    returnsConfiguration.setIncomingDuration(0);
    validator.validateReturnsPolicy(returnsConfiguration, orderFullfillmentTime);
    returnsConfiguration.setIncomingDuration(3);
//     orderFullfillmentTime=null;
//    validator.validateReturnsPolicy(returnsConfiguration,orderFullfillmentTime);
    orderFullfillmentTime = 2l;
    validator.validateReturnsPolicy(returnsConfiguration, orderFullfillmentTime);
    orderFullfillmentTime = 7l;
    validator.validateReturnsPolicy(returnsConfiguration, orderFullfillmentTime);
  }

  @Test(expected = ValidationException.class)
  public void testReturnedQtyGreaterThanAvailableQty() {
    List<IInvntry> inventoryList = getInventoryList();
    List<ReturnsItemVO> returnsItemVOList = new ArrayList<>(2);
    ReturnsItemVO returnsItemVO = new ReturnsItemVO();
    returnsItemVO.setQuantity(new BigDecimal(20));
    returnsItemVO.setMaterialId(1l);
    returnsItemVOList.add(returnsItemVO);
    returnsItemVO = new ReturnsItemVO();
    returnsItemVO.setQuantity(BigDecimal.TEN);
    returnsItemVO.setMaterialId(2l);
    returnsItemVOList.add(returnsItemVO);
    validator.validateShippedQuantity(returnsItemVOList, inventoryList);
  }

  @Test(expected = ValidationException.class)
  public void testBatchReturnedQtyGreaterThanAvailableQty() {
    List<IInvntry> inventoryList = getInventoryList();
    List<ReturnsItemVO> returnsItemVOList = new ArrayList<>(2);
    ReturnsItemVO returnsItemVO = new ReturnsItemVO();
    returnsItemVO.setQuantity(BigDecimal.TEN);
    returnsItemVO.setMaterialId(1l);
    returnsItemVO.setReturnItemBatches(getBatchList(BigDecimal.TEN));
    returnsItemVOList.add(returnsItemVO);
    returnsItemVO = new ReturnsItemVO();
    returnsItemVO.setQuantity(BigDecimal.TEN);
    returnsItemVO.setMaterialId(2l);
    returnsItemVO.setReturnItemBatches(getBatchList(BigDecimal.TEN));
    returnsItemVOList.add(returnsItemVO);
    validator.validateShippedQuantity(returnsItemVOList, inventoryList);
  }

  @Test
  public void batchReturnedQuantityLessThanAvailableQty() {
    List<IInvntry> inventoryList = getInventoryList();
    List<ReturnsItemVO> returnsItemVOList = new ArrayList<>(2);
    ReturnsItemVO returnsItemVO = new ReturnsItemVO();
    returnsItemVO.setQuantity(BigDecimal.TEN);
    returnsItemVO.setMaterialId(1l);
    returnsItemVO.setReturnItemBatches(getBatchList(new BigDecimal(2)));
    returnsItemVOList.add(returnsItemVO);
    returnsItemVO = new ReturnsItemVO();
    returnsItemVO.setQuantity(BigDecimal.TEN);
    returnsItemVO.setMaterialId(2l);
    returnsItemVO.setReturnItemBatches(getBatchList(new BigDecimal(2)));
    returnsItemVOList.add(returnsItemVO);
    validator.validateShippedQuantity(returnsItemVOList, inventoryList);
  }

  @Test
  public void testReturnedQtyLessThanAvailableQty() {
    List<IInvntry> inventoryList = getInventoryList();
    List<ReturnsItemVO> returnsItemVOList = new ArrayList<>(2);
    ReturnsItemVO returnsItemVO = new ReturnsItemVO();
    returnsItemVO.setQuantity(new BigDecimal(2));
    returnsItemVO.setMaterialId(1l);
    returnsItemVOList.add(returnsItemVO);
    returnsItemVO = new ReturnsItemVO();
    returnsItemVO.setQuantity(new BigDecimal(2));
    returnsItemVO.setMaterialId(2l);
    returnsItemVOList.add(returnsItemVO);
    validator.validateShippedQuantity(returnsItemVOList, inventoryList);
  }


  @Test
  public void returnPolicyTest() {
    ReturnsConfig returnsConfig = new ReturnsConfig();
    returnsConfig.setIncomingDuration(5);
    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.DAY_OF_MONTH, -2);
    Long orderFulfillmentTime = cal.getTimeInMillis();
    validator.validateReturnsPolicy(returnsConfig, orderFulfillmentTime);
  }

  @Test(expected = ValidationException.class)
  public void returnDurationPolicyTest() {
    ReturnsConfig returnsConfig = new ReturnsConfig();
    returnsConfig.setIncomingDuration(5);
    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.DAY_OF_MONTH, -10);
    Long orderFulfillmentTime = cal.getTimeInMillis();
    validator.validateReturnsPolicy(returnsConfig, orderFulfillmentTime);
  }

  private SecureUserDetails getSecureUserDetails() {
    SecureUserDetails userDetails = new SecureUserDetails();
    userDetails.setCurrentDomainId(123456L);
    userDetails.setUsername(TEST_USER);
    userDetails.setLocale(Locale.ENGLISH);
    return userDetails;
  }

  private List<ReturnsItemBatchVO> getBatchList(BigDecimal quantity) {
    List<ReturnsItemBatchVO> returnsItemBatchVOList = new ArrayList<>(2);
    ReturnsItemBatchVO returnsItemBatchVO = new ReturnsItemBatchVO();
    returnsItemBatchVO.setQuantity(quantity);
    returnsItemBatchVOList.add(returnsItemBatchVO);
    ReturnsItemBatchVO returnsItemBatchVO1 = new ReturnsItemBatchVO();
    returnsItemBatchVO1.setQuantity(quantity);
    returnsItemBatchVOList.add(returnsItemBatchVO1);
    return returnsItemBatchVOList;
  }

  private List<ReturnsItemVO> getReturnsItemList() {
    List<ReturnsItemVO> returnsItemVOList = new ArrayList<>(2);
    ReturnsItemVO returnsItemVO = new ReturnsItemVO();
    returnsItemVO.setQuantity(BigDecimal.TEN);
    returnsItemVO.setMaterialId(1l);
    returnsItemVO.setReturnItemBatches(getBatchList(BigDecimal.TEN));
    returnsItemVOList.add(returnsItemVO);

    returnsItemVO = new ReturnsItemVO();
    returnsItemVO.setQuantity(BigDecimal.TEN);
    returnsItemVO.setMaterialId(2l);
    returnsItemVO.setReturnItemBatches(getBatchList(BigDecimal.TEN));

    returnsItemVOList.add(returnsItemVO);
    return returnsItemVOList;
  }

  private List<IInvntry> getInventoryList() {
    List<IInvntry> inventoryList = new ArrayList<>(2);
    IInvntry iInvntry = new Invntry();
    iInvntry.setMaterialId(1l);
    iInvntry.setAvailableStock(BigDecimal.TEN);
    inventoryList.add(iInvntry);

    iInvntry = new Invntry();
    iInvntry.setMaterialId(2l);
    iInvntry.setAvailableStock(BigDecimal.TEN);
    inventoryList.add(iInvntry);
    return inventoryList;
  }

}
