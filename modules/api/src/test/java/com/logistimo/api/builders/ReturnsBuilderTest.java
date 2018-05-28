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

package com.logistimo.api.builders;

import com.google.gson.Gson;

import com.logistimo.api.servlets.mobile.builders.MobileOrderBuilder;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.domains.entity.Domain;
import com.logistimo.domains.entity.IDomain;
import com.logistimo.domains.service.DomainsService;
import com.logistimo.entities.auth.EntityAuthoriser;
import com.logistimo.entities.entity.Kiosk;
import com.logistimo.entities.service.EntitiesService;
import com.logistimo.inventory.entity.InvntryBatch;
import com.logistimo.inventory.service.InventoryManagementService;
import com.logistimo.materials.entity.Material;
import com.logistimo.materials.service.MaterialCatalogService;
import com.logistimo.orders.entity.Order;
import com.logistimo.orders.service.OrderManagementService;
import com.logistimo.proto.MobileOrderModel;
import com.logistimo.returns.Status;
import com.logistimo.returns.models.ReturnsItemBatchModel;
import com.logistimo.returns.models.ReturnsItemModel;
import com.logistimo.returns.models.ReturnsModel;
import com.logistimo.returns.models.ReturnsRequestModel;
import com.logistimo.returns.models.ReturnsUpdateStatusModel;
import com.logistimo.returns.models.ReturnsUpdateStatusRequestModel;
import com.logistimo.returns.models.UpdateStatusModel;
import com.logistimo.returns.vo.BatchVO;
import com.logistimo.returns.vo.ReturnsItemBatchVO;
import com.logistimo.returns.vo.ReturnsItemVO;
import com.logistimo.returns.vo.ReturnsReceivedVO;
import com.logistimo.returns.vo.ReturnsStatusVO;
import com.logistimo.returns.vo.ReturnsVO;
import com.logistimo.services.ServiceException;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.users.entity.UserAccount;
import com.logistimo.users.service.UsersService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

/**
 * @author Mohan Raja
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest({SecurityUtils.class, EntityAuthoriser.class, DomainConfig.class})
public class ReturnsBuilderTest {

  public static final String STATUS_MODEL_NOT_CREATED = "Status model not created";
  public static final String ITEM_S_RECEIVED_MODEL_NOT_CREATED =
      "Item's Received model not created";
  @Mock
  OrderManagementService orderManagementService;

  @Mock
  MobileOrderBuilder mobileOrderBuilder;

  @Mock
  EntitiesService entitiesService;

  @Mock
  UsersService usersService;

  @Mock
  MaterialCatalogService materialCatalogService;

  @Mock
  DomainsService domainsService;

  @Mock
  InventoryManagementService inventoryManagementService;

  @Spy
  @InjectMocks
  ReturnsBuilder builder = new ReturnsBuilder();

  public static final String TEST_USER = "test";
  public static final String TEST_TIMEZONE = "Asia/Kolkata";
  public static final String TEST_DOMAIN = "test domain";
  public static final Long TEST_DOMAIN_ID = 1L;
  public static final String TEST_MATERIAL = "test material";

  private Date today;
  private Date yesterday;
  private static final Locale TEST_LOCALE = new Locale("en", "IN");

  @Before
  public void setup() throws ClassNotFoundException, IllegalAccessException,
      InstantiationException, ServiceException {
    GregorianCalendar calendar = new GregorianCalendar();
    today = calendar.getTime();
    calendar.add(Calendar.DAY_OF_MONTH, -1);
    yesterday = calendar.getTime();

    mockStatic(SecurityUtils.class);
    PowerMockito.when(SecurityUtils.getTimezone()).thenReturn(TEST_TIMEZONE);
    PowerMockito.when(SecurityUtils.getLocale()).thenReturn(TEST_LOCALE);
    PowerMockito.when(SecurityUtils.getCurrentDomainId()).thenReturn(TEST_DOMAIN_ID);
    PowerMockito.when(SecurityUtils.getUsername()).thenReturn(TEST_USER);

    mockStatic(DomainConfig.class);
    PowerMockito.when(DomainConfig.getInstance(any())).thenReturn(new DomainConfig());

    mockStatic(EntityAuthoriser.class);
    PowerMockito.when(EntityAuthoriser.authoriseEntity(isNull())).thenReturn(true);

  }

  @Test
  public void testBuildReturnsModels() throws ServiceException {
    IDomain domain = new Domain();
    domain.setName(TEST_DOMAIN);
    when(domainsService.getDomain(isNull())).thenReturn(domain);

    ReturnsVO returnsVO = new ReturnsVO();
    doReturn(new ReturnsModel()).when(builder).buildReturnsModel(returnsVO);

    final List<ReturnsModel> returnsModels = builder.buildReturnsModels(
        Collections.singletonList(returnsVO));

    assertEquals(1, returnsModels.size());
    assertEquals(TEST_DOMAIN, returnsModels.get(0).getSourceDomainName());
  }

  @Test
  public void testBuildReturnsModel() throws ServiceException {

    when(orderManagementService.getOrder(isNull())).thenReturn(new Order());

    final Material material = new Material();
    material.setName(TEST_MATERIAL);
    when(materialCatalogService.getMaterial(isNull())).thenReturn(material);

    when(entitiesService.getKiosk(isNull(), eq(false))).thenReturn(new Kiosk());

    final IUserAccount userAccount = new UserAccount();
    userAccount.setFirstName(TEST_USER);
    doReturn(userAccount).when(usersService).getUserAccount(TEST_USER);

    final ReturnsModel returnsModel = builder.buildReturnsModel(getReturnVO());

    assertNotNull("Customer model not created", returnsModel.getCustomer());
    assertNotNull("Vendor model not created", returnsModel.getVendor());
    assertNotNull(STATUS_MODEL_NOT_CREATED, returnsModel.getStatus());
    assertNotNull("User model not created", returnsModel.getStatus().getUpdatedBy());

    SimpleDateFormat sdf = new SimpleDateFormat("d/M/yy h:mm a");
    assertEquals(sdf.format(today), returnsModel.getCreatedAt());
    assertEquals(TEST_USER, returnsModel.getCreatedBy().getFullName());
    assertEquals(sdf.format(yesterday), returnsModel.getUpdatedAt());
    assertEquals(TEST_USER, returnsModel.getUpdatedBy().getFullName());

    assertEquals(1, returnsModel.getItems().size());
    final ReturnsItemModel item = returnsModel.getItems().get(0);
    assertEquals(TEST_MATERIAL, item.getMaterialName());
    assertNotNull(ITEM_S_RECEIVED_MODEL_NOT_CREATED, item.getReceived());
    assertNotNull("Item's User model (createdBy) not created", item.getCreatedBy());
    assertNotNull("Item's User model (updatedBy) not created", item.getUpdatedBy());

    assertEquals(1, item.getBatches().size());
    final ReturnsItemBatchModel batch = item.getBatches().get(0);
    sdf = new SimpleDateFormat("dd/MM/yyyy");
    assertEquals(sdf.format(today), batch.getExpiry());
    assertEquals(sdf.format(yesterday), batch.getManufacturedDate());
    assertNotNull("Batch item's received model not created", batch.getReceived());
  }

  @Test
  public void testBuildReturns() throws ServiceException {
    final Order order = new Order();
    order.setKioskId(1L);
    order.setServicingKiosk(2L);
    when(orderManagementService.getOrder(isNull())).thenReturn(order);
    when(inventoryManagementService.getInventoryBatch(eq(2L), isNull(),
        isNull(), isNull())).thenReturn(null);
    when(inventoryManagementService.getInventoryBatch(eq(1L), isNull(),
        isNull(), isNull())).thenReturn(new InvntryBatch());
    final ReturnsVO returnsModels = builder.buildReturns(getReturnsRequestModel());

    assertEquals(TEST_DOMAIN_ID, returnsModels.getSourceDomain());
    assertEquals(1L, returnsModels.getCustomerId().longValue());
    assertEquals(2L, returnsModels.getVendorId().longValue());
    assertNotNull("Location model not created", returnsModels.getLocation());
    assertNotNull(STATUS_MODEL_NOT_CREATED, returnsModels.getStatus());

    assertEquals(today.getDate(), returnsModels.getCreatedAt().getDate());
    assertEquals(TEST_USER, returnsModels.getCreatedBy());
    assertEquals(today.getDate(), returnsModels.getUpdatedAt().getDate());
    assertEquals(TEST_USER, returnsModels.getUpdatedBy());

    assertEquals(1, returnsModels.getItems().size());
    final ReturnsItemVO item = returnsModels.getItems().get(0);
    assertNotNull(ITEM_S_RECEIVED_MODEL_NOT_CREATED, item.getReceived());
    assertEquals(TEST_USER, item.getCreatedBy());
    assertEquals(today.getDate(), item.getCreatedAt().getDate());
    assertEquals(TEST_USER, item.getUpdatedBy());
    assertEquals(today.getDate(), item.getUpdatedAt().getDate());

    assertEquals(1, item.getReturnItemBatches().size());
    final ReturnsItemBatchVO batchVO = item.getReturnItemBatches().get(0);
    assertNotNull("Batch item's Received model not created", batchVO.getReceived());

  }

  @Test
  public void testBuildMobileReturnsUpdateModel() throws ServiceException {
    when(orderManagementService.getOrder(isNull())).thenReturn(new Order());

    when(mobileOrderBuilder
        .build(any(), any(), anyString(), anyBoolean(), anyBoolean(), anyBoolean(),
            anyBoolean())).thenReturn(new MobileOrderModel());

    final Material material = new Material();
    material.setName(TEST_MATERIAL);
    when(materialCatalogService.getMaterial(isNull())).thenReturn(material);

    when(entitiesService.getKiosk(eq(1L), eq(false))).thenReturn(new Kiosk());

    final IUserAccount userAccount = new UserAccount();
    userAccount.setFirstName(TEST_USER);
    doReturn(userAccount).when(usersService).getUserAccount(TEST_USER);

    final ReturnsUpdateStatusModel returnsModels =
        builder.buildMobileReturnsUpdateModel(getReturnVO(), getReturnsUpdateStatusRequestModel());

    ReturnsModel returnsModel = returnsModels.getReturns();
    assertNotNull("Customer model not created", returnsModel.getCustomer());
    assertNotNull("Vendor model not created", returnsModel.getVendor());
    assertNotNull(STATUS_MODEL_NOT_CREATED, returnsModel.getStatus());
    assertNotNull("User model not created", returnsModel.getStatus().getUpdatedBy());

    SimpleDateFormat sdf = new SimpleDateFormat("d/M/yy h:mm a");
    assertEquals(sdf.format(today), returnsModel.getCreatedAt());
    assertEquals(TEST_USER, returnsModel.getCreatedBy().getFullName());
    assertEquals(sdf.format(yesterday), returnsModel.getUpdatedAt());
    assertEquals(TEST_USER, returnsModel.getUpdatedBy().getFullName());

    assertEquals(1, returnsModel.getItems().size());
    final ReturnsItemModel item = returnsModel.getItems().get(0);
    assertEquals(TEST_MATERIAL, item.getMaterialName());
    assertNotNull(ITEM_S_RECEIVED_MODEL_NOT_CREATED, item.getReceived());
    assertNotNull("Item's User model (createdBy) not created", item.getCreatedBy());
    assertNotNull("Item's User model (updatedBy) not created", item.getUpdatedBy());

    assertEquals(1, item.getBatches().size());
    final ReturnsItemBatchModel batch = item.getBatches().get(0);
    sdf = new SimpleDateFormat("dd/MM/yyyy");
    assertEquals(sdf.format(today), batch.getExpiry());
    assertEquals(sdf.format(yesterday), batch.getManufacturedDate());
    assertNotNull("Batch item's received model not created", batch.getReceived());

    assertNotNull("Order model not created", returnsModels.getOrder());

  }

  @Test
  public void testBuildUpdateStatusModel() throws ServiceException {
    final UpdateStatusModel shipReturnsModels =
        builder.buildUpdateStatusModel(null, "ship", getReturnsUpdateStatusRequestModel());
    final UpdateStatusModel receiveReturnsModels =
        builder.buildUpdateStatusModel(null, "receive", getReturnsUpdateStatusRequestModel());
    final UpdateStatusModel cancelReturnsModels =
        builder.buildUpdateStatusModel(null, "cancel", getReturnsUpdateStatusRequestModel());

    assertEquals(Status.SHIPPED, shipReturnsModels.getStatus());
    assertEquals(Status.RECEIVED, receiveReturnsModels.getStatus());
    assertEquals(Status.CANCELLED, cancelReturnsModels.getStatus());

    assertEquals(TEST_USER, shipReturnsModels.getUserId());

  }


  private ReturnsVO getReturnVO() {
    ReturnsVO returnsVO = new ReturnsVO();
    returnsVO.setCreatedAt(today);
    returnsVO.setUpdatedAt(yesterday);
    returnsVO.setUpdatedBy(TEST_USER);
    returnsVO.setCreatedBy(TEST_USER);

    final ReturnsItemVO returnsItemVO = new ReturnsItemVO();
    returnsItemVO.setCreatedBy(TEST_USER);
    returnsItemVO.setUpdatedBy(TEST_USER);

    final ReturnsReceivedVO returnsReceivedVO = new ReturnsReceivedVO();
    returnsItemVO.setReceived(returnsReceivedVO);

    final BatchVO batch = new BatchVO();
    batch.setExpiryDate(today);
    batch.setManufacturedDate(yesterday);

    final ReturnsItemBatchVO returnsItemBatchVO = new ReturnsItemBatchVO();
    returnsItemBatchVO.setBatch(batch);
    returnsItemBatchVO.setReceived(returnsReceivedVO);

    returnsItemVO.setReturnItemBatches(Collections.singletonList(returnsItemBatchVO));

    returnsVO.setItems(Collections.singletonList(returnsItemVO));

    final ReturnsStatusVO returnsStatusVO = new ReturnsStatusVO();
    returnsStatusVO.setUpdatedBy(TEST_USER);
    returnsVO.setStatus(returnsStatusVO);
    return returnsVO;
  }

  private ReturnsRequestModel getReturnsRequestModel() {
    String model = "{"
        + "items:[{received:{},batches:[{received:{}}]}],"
        + "geo_location:{},"
        + "created_by:{},"
        + "updated_by:{}"
        + "}";
    return new Gson().fromJson(model, ReturnsRequestModel.class);
  }

  private ReturnsUpdateStatusRequestModel getReturnsUpdateStatusRequestModel()
      throws ServiceException {
    ReturnsUpdateStatusRequestModel model = new ReturnsUpdateStatusRequestModel();
    model.setEmbed("returns,order");
    model.setEntityId(1L);
    return model;
  }
}
