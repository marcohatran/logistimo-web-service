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
import com.logistimo.returns.models.MobileReturnsModel;
import com.logistimo.returns.models.ReturnsRequestModel;
import com.logistimo.returns.models.MobileReturnsUpdateStatusModel;
import com.logistimo.returns.models.MobileReturnsUpdateStatusRequestModel;
import com.logistimo.returns.models.ReturnsItemBatchModel;
import com.logistimo.returns.models.ReturnsItemModel;
import com.logistimo.returns.models.UpdateStatusModel;
import com.logistimo.returns.models.submodels.EntityModel;
import com.logistimo.returns.models.submodels.StatusModel;
import com.logistimo.returns.models.submodels.UserModel;
import com.logistimo.returns.models.submodels.ReceivedModel;
import com.logistimo.returns.vo.BatchVO;
import com.logistimo.returns.vo.GeoLocationVO;
import com.logistimo.returns.vo.ReturnsItemBatchVO;
import com.logistimo.returns.vo.ReturnsItemVO;
import com.logistimo.returns.vo.ReturnsReceivedVO;
import com.logistimo.returns.vo.ReturnsStatusVO;
import com.logistimo.returns.vo.ReturnsVO;
import com.logistimo.services.ObjectNotFoundException;
import com.logistimo.services.ServiceException;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.users.service.UsersService;

import org.apache.commons.lang.StringUtils;
import org.modelmapper.ModelMapper;
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

  private  ModelMapper modelMapper=new ModelMapper();

  @Autowired
  OrderManagementService orderManagementService;

  @Autowired
  MobileOrderBuilder mobileOrderBuilder;

  @Autowired
  EntitiesService entitiesService;

  @Autowired
  UsersService usersService;

  public MobileReturnsModel buildMobileReturnsModel(ReturnsVO returnsVO) throws ServiceException {
    MobileReturnsModel mobileReturnsModel = new MobileReturnsModel();
    mobileReturnsModel.setReturnId(returnsVO.getId());
    mobileReturnsModel.setOrderId(returnsVO.getOrderId());
    mobileReturnsModel.setCustomer(getEntityModel(returnsVO.getCustomerId()));
    mobileReturnsModel.setVendor(getEntityModel(returnsVO.getVendorId()));
    mobileReturnsModel.setStatus(getStatusModel(returnsVO.getStatus()));
    mobileReturnsModel.setCreatedAt(returnsVO.getCreatedAt());
    mobileReturnsModel.setCreatedBy(getUserModel(returnsVO.getCreatedBy()));
    mobileReturnsModel.setUpdatedAt(returnsVO.getUpdatedAt());
    mobileReturnsModel.setCreatedBy(getUserModel(returnsVO.getUpdatedBy()));
    mobileReturnsModel.setItems(getItemModels(returnsVO.getItems()));
    return mobileReturnsModel;
  }

  public ReturnsVO buildReturns(ReturnsRequestModel returnRequestModel)
      throws ServiceException {

    Date now = new Date();
    String username = SecurityUtils.getUsername();
    putItemsMeta(returnRequestModel.getItems(), now, username);

    ReturnsVO returns = new ReturnsVO();
    returns.setSourceDomain(SecurityUtils.getCurrentDomainId());
    returns.setOrderId(returnRequestModel.getOrderId());
    IOrder order = orderManagementService.getOrder(returnRequestModel.getOrderId());
    returns.setCustomerId(order.getKioskId());
    returns.setVendorId(order.getServicingKiosk());
    if(returnRequestModel.getLocation() != null) {
      returns.setLocation(getGeoLocation(returnRequestModel.getLocation()));
    }
    returns.setStatus(getReturnsStatus(now, username));
    returns.setCreatedAt(now);
    returns.setCreatedBy(username);
    returns.setUpdatedAt(now);
    returns.setUpdatedBy(username);
    returns.setSource(returnRequestModel.getSource());
    returns.setItems(getItems(returnRequestModel.getItems()));
    return returns;
  }

  private GeoLocationVO getGeoLocation(ReturnsRequestModel.Location location) {

    return modelMapper.map(location,GeoLocationVO.class);
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

  private List<ReturnsItemVO> getItems(List<ReturnsItemModel> itemModels) {
    List<ReturnsItemVO> returnsItems = new ArrayList<>(itemModels.size());
    for (ReturnsItemModel itemModel : itemModels) {
      ReturnsItemVO returnsItem = new ReturnsItemVO();
      returnsItem.setMaterialId(itemModel.getMaterialId());
      returnsItem.setQuantity(itemModel.getReturnQuantity());
      returnsItem.setMaterialStatus(itemModel.getMaterialStatus());
      returnsItem.setReason(itemModel.getReason());
      if(itemModel.getReceived()!=null) {
        returnsItem.setReceived(getReturnsReceived(itemModel.getReceived()));
      }
      returnsItem.setCreatedAt(itemModel.getCreatedAt());
      returnsItem.setCreatedBy(itemModel.getCreatedBy().getUserId());
      returnsItem.setUpdatedAt(itemModel.getUpdatedAt());
      returnsItem.setUpdatedBy(itemModel.getUpdatedBy().getUserId());
      if(itemModel.getBatches()!= null) {
        returnsItem.setReturnItemBatches(getItemBatches(itemModel.getBatches()));
      }
      returnsItems.add(returnsItem);
    }
    return returnsItems;
  }

  private List<ReturnsItemBatchVO> getItemBatches(List<ReturnsItemBatchModel> itemBatchModels) {
    List<ReturnsItemBatchVO> itemBatches = new ArrayList<>(itemBatchModels.size());
    for (ReturnsItemBatchModel itemBatchModel : itemBatchModels) {
      ReturnsItemBatchVO itemBatch = new ReturnsItemBatchVO();
      itemBatch.setBatch(getBatch(itemBatchModel));
      itemBatch.setQuantity(itemBatchModel.getReturnQuantity());
      itemBatch.setMaterialStatus(itemBatchModel.getMaterialStatus());
      itemBatch.setReason(itemBatchModel.getReason());
      if(itemBatchModel.getReceived()!=null) {
        itemBatch.setReceived(getReturnsReceived(itemBatchModel.getReceived()));
      }
      itemBatches.add(itemBatch);
    }
    return itemBatches;
  }

  private BatchVO getBatch(ReturnsItemBatchModel itemBatchModel) {
    return new BatchVO(
        itemBatchModel.getBatchId(),
        itemBatchModel.getExpiry(),
        itemBatchModel.getManufacturer(),
        itemBatchModel.getManufacturedDate());
  }

  private ReturnsReceivedVO getReturnsReceived(ReceivedModel receivedModel) {
    return new ReturnsReceivedVO(
        receivedModel.getReceivedQuantity(),
        receivedModel.getMaterialStatus(),
        receivedModel.getReason()
    );
  }

  public MobileReturnsUpdateStatusModel buildMobileReturnsUpdateModel(
      ReturnsVO returns, MobileReturnsUpdateStatusRequestModel updateStatusModel)
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

  private ReturnsStatusVO getReturnsStatus(Date now, String username) {
    return new ReturnsStatusVO(Status.OPEN, null, now, username);
  }

  private List<ReturnsItemModel> getItemModels(List<ReturnsItemVO> items) {
    List<ReturnsItemModel> itemModels = new ArrayList<>(items.size());
    itemModels.addAll(items.stream().map(this::getReturnsItemModel).collect(Collectors.toList()));
    return itemModels;
  }

  private ReturnsItemModel getReturnsItemModel(ReturnsItemVO item) {
    ReturnsItemModel itemModel = new ReturnsItemModel();
    itemModel.setMaterialId(item.getMaterialId());
    itemModel.setReturnQuantity(item.getQuantity());
    itemModel.setMaterialStatus(item.getMaterialStatus());
    itemModel.setReason(item.getReason());
    if(item.getReceived()!=null) {
      itemModel.setReceived(getReceivedModel(item.getReceived()));
    }
    itemModel.setCreatedAt(item.getCreatedAt());
    itemModel.setCreatedBy(getUserModel(item.getCreatedBy()));
    itemModel.setUpdatedAt(item.getUpdatedAt());
    itemModel.setUpdatedBy(getUserModel(item.getUpdatedBy()));
    if(item.getReturnItemBatches() != null) {
      itemModel.setBatches(getItemBatchModels(item.getReturnItemBatches()));
    }
    return itemModel;
  }

  private List<ReturnsItemBatchModel> getItemBatchModels(List<ReturnsItemBatchVO> batches) {
    List<ReturnsItemBatchModel> itemBatchModels = new ArrayList<>(batches.size());
    itemBatchModels
        .addAll(batches.stream().map(this::getReturnsItemBatchModel).collect(Collectors.toList()));
    return itemBatchModels;
  }

  private ReturnsItemBatchModel getReturnsItemBatchModel(ReturnsItemBatchVO itemBatch) {
    ReturnsItemBatchModel itemBatchModel = new ReturnsItemBatchModel();
    BatchVO batch = itemBatch.getBatch();
    itemBatchModel.setBatchId(batch.getBatchId());
    itemBatchModel.setExpiry(batch.getExpiryDate());
    itemBatchModel.setManufacturer(batch.getManufacturer());
    itemBatchModel.setManufacturedDate(batch.getManufacturedDate());
    itemBatchModel.setReturnQuantity(itemBatch.getQuantity());
    itemBatchModel.setMaterialStatus(itemBatch.getMaterialStatus());
    itemBatchModel.setReason(itemBatch.getReason());
    if(itemBatch.getReceived() != null) {
      itemBatchModel.setReceived(getReceivedModel(itemBatch.getReceived()));
    }
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

  private StatusModel getStatusModel(ReturnsStatusVO returnStatus) {
    StatusModel statusModel = new StatusModel();
    statusModel.setStatus(returnStatus.getStatus());
    statusModel.setCancelReason(returnStatus.getCancelReason());
    statusModel.setUpdatedAt(returnStatus.getUpdatedAt());
    statusModel.setUpdatedBy(getUserModel(returnStatus.getUpdatedBy()));
    return statusModel;
  }

  private ReceivedModel getReceivedModel(ReturnsReceivedVO received) {
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

  public UpdateStatusModel buildUpdateStatusModel(Long returnId,String status,MobileReturnsUpdateStatusRequestModel mobileReturnsUpdateStatusRequestModel){
    UpdateStatusModel updateStatusModel=new UpdateStatusModel();
    updateStatusModel.setStatus(Status.getStatus(status));
    updateStatusModel.setReturnId(returnId);
    updateStatusModel.setUserId(SecurityUtils.getUsername());
    updateStatusModel.setComment(mobileReturnsUpdateStatusRequestModel.getComment());
    return updateStatusModel;
  }
}