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

import com.logistimo.api.servlets.mobile.builders.MobileOrderBuilder;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.constants.CharacterConstants;
import com.logistimo.constants.SourceConstants;
import com.logistimo.entities.entity.IKiosk;
import com.logistimo.entities.service.EntitiesService;
import com.logistimo.logger.XLog;
import com.logistimo.orders.entity.IOrder;
import com.logistimo.orders.service.OrderManagementService;
import com.logistimo.proto.MobileOrderModel;
import com.logistimo.returns.Status;
import com.logistimo.returns.entity.Returns;
import com.logistimo.returns.entity.ReturnsItem;
import com.logistimo.returns.entity.ReturnsItemBatch;
import com.logistimo.returns.entity.values.Batch;
import com.logistimo.returns.entity.values.GeoLocation;
import com.logistimo.returns.entity.values.ReturnsReceived;
import com.logistimo.returns.entity.values.ReturnsStatus;
import com.logistimo.returns.models.MobileReturnsModel;
import com.logistimo.returns.models.MobileReturnsRequestModel;
import com.logistimo.returns.models.MobileReturnsUpdateStatusModel;
import com.logistimo.returns.models.MobileReturnsUpdateStatusRequestModel;
import com.logistimo.returns.models.ReturnsItemBatchModel;
import com.logistimo.returns.models.ReturnsItemModel;
import com.logistimo.returns.models.submodels.EntityModel;
import com.logistimo.returns.models.submodels.StatusModel;
import com.logistimo.returns.models.submodels.UserModel;
import com.logistimo.returns.models.submodels.ReceivedModel;
import com.logistimo.services.ObjectNotFoundException;
import com.logistimo.services.ServiceException;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.users.service.UsersService;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Mohan Raja
 */
@Component
public class ReturnsBuilder {

  private static final XLog xLogger = XLog.getLog(ReturnsBuilder.class);

  private static final String RETURNS = "returns";
  private static final String ORDER = "order";

  @Autowired
  OrderManagementService orderManagementService;

  @Autowired
  MobileOrderBuilder mobileOrderBuilder;

  @Autowired
  EntitiesService entitiesService;

  @Autowired
  UsersService usersService;

  public MobileReturnsModel buildMobileReturnsModel(Returns returns) throws ServiceException {
    MobileReturnsModel mobileReturnsModel = new MobileReturnsModel();
    mobileReturnsModel.setReturnId(returns.getId());
    mobileReturnsModel.setOrderId(returns.getOrderId());
    mobileReturnsModel.setCustomer(getEntityModel(returns.getCustomerId()));
    mobileReturnsModel.setVendor(getEntityModel(returns.getVendorId()));
    mobileReturnsModel.setStatus(getStatusModel(returns.getStatus()));
    mobileReturnsModel.setCreatedAt(returns.getCreatedAt());
    mobileReturnsModel.setCreatedBy(getUserModel(returns.getCreatedBy()));
    mobileReturnsModel.setUpdatedAt(returns.getUpdatedAt());
    mobileReturnsModel.setCreatedBy(getUserModel(returns.getUpdatedBy()));
    mobileReturnsModel.setItems(getItemModels(returns.getItems()));
    return mobileReturnsModel;
  }

  public Returns createNewReturns(MobileReturnsRequestModel mobileReturnRequestModel)
      throws ServiceException {

    Date now = new Date();
    String username = SecurityUtils.getUsername();
    putItemsMeta(mobileReturnRequestModel.getItems(), now, username);

    Returns returns = new Returns();
    returns.setSourceDomain(SecurityUtils.getCurrentDomainId());
    returns.setOrderId(mobileReturnRequestModel.getOrderId());
    IOrder order = orderManagementService.getOrder(mobileReturnRequestModel.getOrderId());
    returns.setCustomerId(order.getKioskId());
    returns.setVendorId(order.getServicingKiosk());
    returns.setLocation(getGeoLocation(mobileReturnRequestModel.getLocation()));
    returns.setStatus(getReturnsStatus(now, username));
    returns.setCreatedAt(now);
    returns.setCreatedBy(username);
    returns.setUpdatedAt(now);
    returns.setUpdatedBy(username);
    returns.setSource(SourceConstants.MOBILE);
    returns.setItems(getItems(mobileReturnRequestModel.getItems()));
    return returns;
  }

  private GeoLocation getGeoLocation(MobileReturnsRequestModel.Location location) {
    return new GeoLocation(
        location.getLatitude(),
        location.getLongitude(),
        location.getGeoAccuracy(),
        location.getGeoError());
  }

  private void putItemsMeta(List<ReturnsItemModel> items, Date now, String username) {
    UserModel userModel = new UserModel(username);
    for (ReturnsItemModel item : items) {
      item.setUpdatedAt(now);
      item.setUpdatedBy(userModel);
      item.setCreatedAt(now);
      item.setCreatedBy(userModel);
    }
  }

  private List<ReturnsItem> getItems(List<ReturnsItemModel> itemModels) {
    List<ReturnsItem> returnsItems = new ArrayList<>(itemModels.size());
    for (ReturnsItemModel itemModel : itemModels) {
      ReturnsItem returnsItem = new ReturnsItem();
      returnsItem.setMaterialId(itemModel.getMaterialId());
      returnsItem.setQuantity(itemModel.getReturnQuantity());
      returnsItem.setMaterialStatus(itemModel.getMaterialStatus());
      returnsItem.setReason(itemModel.getReason());
      returnsItem.setReceived(getReturnsReceived(itemModel.getReceived()));
      returnsItem.setCreatedAt(itemModel.getCreatedAt());
      returnsItem.setCreatedBy(itemModel.getCreatedBy().getUserId());
      returnsItem.setUpdatedAt(itemModel.getUpdatedAt());
      returnsItem.setUpdatedBy(itemModel.getUpdatedBy().getUserId());
      returnsItem.setBatches(getItemBatches(itemModel.getBatches()));
      returnsItems.add(returnsItem);
    }
    return returnsItems;
  }

  private List<ReturnsItemBatch> getItemBatches(List<ReturnsItemBatchModel> itemBatchModels) {
    List<ReturnsItemBatch> itemBatches = new ArrayList<>(itemBatchModels.size());
    for (ReturnsItemBatchModel itemBatchModel : itemBatchModels) {
      ReturnsItemBatch itemBatch = new ReturnsItemBatch();
      itemBatch.setBatch(getBatch(itemBatchModel));
      itemBatch.setQuantity(itemBatchModel.getReturnQuantity());
      itemBatch.setMaterialStatus(itemBatchModel.getMaterialStatus());
      itemBatch.setReason(itemBatchModel.getReason());
      itemBatch.setReceived(getReturnsReceived(itemBatchModel.getReceived()));
      itemBatches.add(itemBatch);
    }
    return itemBatches;
  }

  private Batch getBatch(ReturnsItemBatchModel itemBatchModel) {
    return new Batch(
        itemBatchModel.getBatchId(),
        itemBatchModel.getExpiry(),
        itemBatchModel.getManufacturer(),
        itemBatchModel.getManufacturedDate());
  }

  private ReturnsReceived getReturnsReceived(ReceivedModel receivedModel) {
    return new ReturnsReceived(
        receivedModel.getReceivedQuantity(),
        receivedModel.getMaterialStatus(),
        receivedModel.getReason()
    );
  }

  public MobileReturnsUpdateStatusModel buildMobileReturnsUpdateModel(
      Returns returns, MobileReturnsUpdateStatusRequestModel updateStatusModel)
      throws ServiceException {
    MobileReturnsUpdateStatusModel mobileReturnsUpdateStatusModel =
        new MobileReturnsUpdateStatusModel();
    if (StringUtils.isBlank(updateStatusModel.getEmbed())) {
      String[] embedValues = updateStatusModel.getEmbed().split(CharacterConstants.COMMA);
      for (String embedValue : embedValues) {
        if (RETURNS.equals(embedValue)) {
          mobileReturnsUpdateStatusModel.setReturns(buildMobileReturnsModel(returns));
        } else if (ORDER.equals(embedValue)) {
          IOrder order = orderManagementService.getOrder(returns.getOrderId());
          mobileReturnsUpdateStatusModel
              .setOrder(buildMobileOrderModel(order, updateStatusModel.getEntityId()));
        }
      }
    }
    return mobileReturnsUpdateStatusModel;
  }

  private ReturnsStatus getReturnsStatus(Date now, String username) {
    return new ReturnsStatus(Status.OPEN, null, now, username);
  }

  private List<ReturnsItemModel> getItemModels(List<ReturnsItem> items) {
    List<ReturnsItemModel> itemModels = new ArrayList<>(items.size());
    itemModels.addAll(items.stream().map(this::getReturnsItemModel).collect(Collectors.toList()));
    return itemModels;
  }

  private ReturnsItemModel getReturnsItemModel(ReturnsItem item) {
    ReturnsItemModel itemModel = new ReturnsItemModel();
    itemModel.setMaterialId(item.getMaterialId());
    itemModel.setReturnQuantity(item.getQuantity());
    itemModel.setMaterialStatus(item.getMaterialStatus());
    itemModel.setReason(item.getReason());
    itemModel.setReceived(getReceivedModel(item.getReceived()));
    itemModel.setCreatedAt(item.getCreatedAt());
    itemModel.setCreatedBy(getUserModel(item.getCreatedBy()));
    itemModel.setUpdatedAt(item.getUpdatedAt());
    itemModel.setUpdatedBy(getUserModel(item.getUpdatedBy()));
    itemModel.setBatches(getItemBatchModels(item.getBatches()));
    return itemModel;
  }

  private List<ReturnsItemBatchModel> getItemBatchModels(List<ReturnsItemBatch> batches) {
    List<ReturnsItemBatchModel> itemBatchModels = new ArrayList<>(batches.size());
    itemBatchModels
        .addAll(batches.stream().map(this::getReturnsItemBatchModel).collect(Collectors.toList()));
    return itemBatchModels;
  }

  private ReturnsItemBatchModel getReturnsItemBatchModel(ReturnsItemBatch itemBatch) {
    ReturnsItemBatchModel itemBatchModel = new ReturnsItemBatchModel();
    Batch batch = itemBatch.getBatch();
    itemBatchModel.setBatchId(batch.getBatchId());
    itemBatchModel.setExpiry(batch.getExpiryDate());
    itemBatchModel.setManufacturer(batch.getManufacturer());
    itemBatchModel.setManufacturedDate(batch.getManufacturedDate());
    itemBatchModel.setReturnQuantity(itemBatch.getQuantity());
    itemBatchModel.setMaterialStatus(itemBatch.getMaterialStatus());
    itemBatchModel.setReason(itemBatch.getReason());
    itemBatchModel.setReceived(getReceivedModel(itemBatch.getReceived()));
    return itemBatchModel;
  }

  private UserModel getUserModel(String userId) {
    UserModel userModel = new UserModel();
    userModel.setUserId(userId);
    try {
      IUserAccount userAccount = usersService.getUserAccount(userId);
      userModel.setFullName(userAccount.getFullName());
    } catch (ObjectNotFoundException e) {
      xLogger.warn("User {0} not found.", userId, e);
    }
    return userModel;
  }

  private EntityModel getEntityModel(Long entityId) throws ServiceException {
    EntityModel entityModel = new EntityModel();
    entityModel.setId(entityId);
    IKiosk kiosk = entitiesService.getKiosk(entityId, false);
    if (kiosk != null) {
      entityModel.setName(kiosk.getName());
      entityModel.setCity(kiosk.getCity());
    }
    return entityModel;
  }

  private StatusModel getStatusModel(ReturnsStatus returnStatus) {
    StatusModel statusModel = new StatusModel();
    statusModel.setStatus(returnStatus.getStatus());
    statusModel.setCancelReason(returnStatus.getCancelReason());
    statusModel.setUpdatedAt(returnStatus.getUpdatedAt());
    statusModel.setUpdatedBy(getUserModel(returnStatus.getUpdatedBy()));
    return statusModel;
  }

  private ReceivedModel getReceivedModel(ReturnsReceived received) {
    ReceivedModel model = new ReceivedModel();
    model.setReceivedQuantity(received.getQuantity());
    model.setMaterialStatus(received.getMaterialStatus());
    model.setReason(received.getReason());
    return model;
  }

  private MobileOrderModel buildMobileOrderModel(IOrder order, Long entityId)
      throws ServiceException {
    DomainConfig dc = DomainConfig.getInstance(order.getDomainId());
    boolean isAccounting = dc.isAccountingEnabled();
    boolean isBatchEnabled;
    if (entityId != null) {
      IKiosk k = entitiesService.getKiosk(entityId, false);
      isBatchEnabled = k.isBatchMgmtEnabled();
    } else {
      IKiosk k = entitiesService.getKiosk(order.getKioskId(), false);
      isBatchEnabled = k.isBatchMgmtEnabled();
    }
    return mobileOrderBuilder.build(order, SecurityUtils.getLocale(), SecurityUtils.getTimezone(),
        true, isAccounting, true, isBatchEnabled);
  }
}