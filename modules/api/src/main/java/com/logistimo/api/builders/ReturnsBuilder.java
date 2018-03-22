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
import com.logistimo.constants.Constants;
import com.logistimo.domains.service.DomainsService;
import com.logistimo.entities.auth.EntityAuthoriser;
import com.logistimo.entities.entity.IKiosk;
import com.logistimo.entities.service.EntitiesService;
import com.logistimo.exception.InvalidDataException;
import com.logistimo.inventory.entity.IInvntryBatch;
import com.logistimo.inventory.service.InventoryManagementService;
import com.logistimo.logger.XLog;
import com.logistimo.materials.service.MaterialCatalogService;
import com.logistimo.orders.entity.IOrder;
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
import com.logistimo.returns.models.submodels.EntityModel;
import com.logistimo.returns.models.submodels.ReceivedModel;
import com.logistimo.returns.models.submodels.StatusModel;
import com.logistimo.returns.models.submodels.UserModel;
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
import com.logistimo.utils.LocalDateUtil;

import org.apache.commons.lang.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

  @Autowired
  MaterialCatalogService materialCatalogService;

  @Autowired
  DomainsService domainsService;

  @Autowired
  InventoryManagementService inventoryManagementService;

  public List<ReturnsModel> buildMobileReturnsModels(List<ReturnsVO> returnsVOs) throws ServiceException {
    List<ReturnsModel> returnsModels = new ArrayList<>(returnsVOs.size());
    for (ReturnsVO returnsVO : returnsVOs) {
      returnsModels.add(buildMobileReturnsModel(returnsVO));
    }
    addSourceDomainName(returnsModels);
    return returnsModels;
  }

  private void addSourceDomainName(List<ReturnsModel> returnsModels) {
    Map<Long, String> domainNames = new HashMap<>();
    for (ReturnsModel returnsModel : returnsModels) {
      String domainName = domainNames.get(returnsModel.getSourceDomain());
      if (domainName == null) {
        try {
          domainName = domainsService.getDomain(returnsModel.getSourceDomain()).getName();
          domainNames.put(returnsModel.getSourceDomain(), domainName);
        } catch (Exception ignored) {}
      } else {
        returnsModel.setSourceDomainName(domainName);
      }
    }
  }

  public ReturnsModel buildMobileReturnsModel(ReturnsVO returnsVO) throws ServiceException {
    ReturnsModel returnsModel = new ReturnsModel();
    returnsModel.setReturnId(returnsVO.getId());
    returnsModel.setOrderId(returnsVO.getOrderId());
    IOrder order = orderManagementService.getOrder(returnsVO.getOrderId());
    returnsModel.setOrderType(order.getOrderType());
    returnsModel.setCustomer(getEntityModel(returnsVO.getCustomerId()));
    returnsModel.setVendor(getEntityModel(returnsVO.getVendorId()));
    returnsModel.setStatus(getStatusModel(returnsVO.getStatus()));
    returnsModel.setCreatedAt(
        LocalDateUtil.format(returnsVO.getCreatedAt(), SecurityUtils.getLocale(), SecurityUtils.getTimezone()));
    returnsModel.setCreatedBy(getUserModel(returnsVO.getCreatedBy()));
    returnsModel.setUpdatedAt(
        LocalDateUtil.format(returnsVO.getUpdatedAt(), SecurityUtils.getLocale(), SecurityUtils.getTimezone()));
    returnsModel.setUpdatedBy(getUserModel(returnsVO.getUpdatedBy()));
    if(returnsVO.getItems()!=null) {
      returnsModel.setItems(getItemModels(returnsVO.getItems()));
    }
    returnsModel.setSourceDomain(returnsVO.getSourceDomain());
    return returnsModel;
  }

  public ReturnsVO buildReturns(ReturnsRequestModel returnRequestModel)
      throws ServiceException {

    Date now = new Date();
    String username = SecurityUtils.getUsername();
    putItemsMeta(returnRequestModel.getItems(), now, username);
    ReturnsVO returnsVO = new ReturnsVO();
    returnsVO.setSourceDomain(SecurityUtils.getCurrentDomainId());
    returnsVO.setOrderId(returnRequestModel.getOrderId());
    IOrder order = orderManagementService.getOrder(returnRequestModel.getOrderId());
    returnsVO.setCustomerId(order.getKioskId());
    returnsVO.setVendorId(order.getServicingKiosk());
    if(returnRequestModel.getLocation() != null) {
      returnsVO.setLocation(getGeoLocation(returnRequestModel.getLocation()));
    }
    returnsVO.setStatus(getReturnsStatus(now, username));
    returnsVO.setCreatedAt(now);
    returnsVO.setCreatedBy(username);
    returnsVO.setUpdatedAt(now);
    returnsVO.setUpdatedBy(username);
    returnsVO.setSource(returnRequestModel.getSource());
    returnsVO.setItems(getItems(returnRequestModel.getItems(), returnsVO.getVendorId(), returnsVO.getCustomerId()));
    returnsVO.setComment(returnRequestModel.getComment());
    return returnsVO;
  }

  private GeoLocationVO getGeoLocation(ReturnsRequestModel.Location location) {

    return modelMapper.map(location,GeoLocationVO.class);
  }

  private void putItemsMeta(List<ReturnsItemModel> items, Date now, String username) {
    UserModel userModel = new UserModel();
    userModel.setUserId(username);
    for (ReturnsItemModel item : items) {
      item.setUpdatedAt(now);
      item.setUpdatedBy(userModel);
      item.setCreatedAt(now);
      item.setCreatedBy(userModel);
      item.setCreatedBy(userModel);
    }
  }

  private List<ReturnsItemVO> getItems(List<ReturnsItemModel> itemModels, Long vendorId, Long customerId) {
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
        returnsItem.setReturnItemBatches(getItemBatches(itemModel.getBatches(), vendorId, customerId, itemModel.getMaterialId()));
      }
      returnsItems.add(returnsItem);
    }
    return returnsItems;
  }

  private List<ReturnsItemBatchVO> getItemBatches(List<ReturnsItemBatchModel> itemBatchModels,
                                                  Long vendorId, Long customerId, Long materialId) {
    List<ReturnsItemBatchVO> itemBatches = new ArrayList<>(itemBatchModels.size());
    for (ReturnsItemBatchModel itemBatchModel : itemBatchModels) {
      ReturnsItemBatchVO itemBatch = new ReturnsItemBatchVO();
      itemBatch.setBatch(getBatch(itemBatchModel, vendorId, customerId, materialId));
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

  private BatchVO getBatch(ReturnsItemBatchModel itemBatchModel, Long vendorId, Long customerId, Long materialId) {
    IInvntryBatch
        inventoryBatch;
    try {
      inventoryBatch = inventoryManagementService
          .getInventoryBatch(vendorId, materialId, itemBatchModel.getBatchId(), null);
    } catch (ObjectNotFoundException e) {
      inventoryBatch = inventoryManagementService
          .getInventoryBatch(customerId, materialId, itemBatchModel.getBatchId(), null);
    }
    return new BatchVO(
        itemBatchModel.getBatchId(),
        inventoryBatch.getBatchExpiry(),
        inventoryBatch.getBatchManufacturer(),
        inventoryBatch.getBatchManufacturedDate());
  }

  private ReturnsReceivedVO getReturnsReceived(ReceivedModel receivedModel) {
    return new ReturnsReceivedVO(
        receivedModel.getReceivedQuantity(),
        receivedModel.getMaterialStatus(),
        receivedModel.getReason()
    );
  }

  public ReturnsUpdateStatusModel buildMobileReturnsUpdateModel(
      ReturnsVO returns, ReturnsUpdateStatusRequestModel updateStatusModel)
      throws ServiceException {
    ReturnsUpdateStatusModel returnsUpdateStatusModel =
        new ReturnsUpdateStatusModel();
    if (StringUtils.isNotBlank(updateStatusModel.getEmbed())) {
      String[] embedValues = updateStatusModel.getEmbed().split(CharacterConstants.COMMA);
      for (String embedValue : embedValues) {
        if (RETURNS.equals(embedValue)) {
          returnsUpdateStatusModel.setReturns(buildMobileReturnsModel(returns));
        } else if (ORDER.equals(embedValue)) {
          IOrder order = orderManagementService.getOrder(returns.getOrderId());
          returnsUpdateStatusModel
              .setOrder(buildMobileOrderModel(order, updateStatusModel.getEntityId()));
        }
      }
    }
    return returnsUpdateStatusModel;
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
    try {
      itemModel.setMaterialName(materialCatalogService.getMaterial(item.getMaterialId()).getName());
    } catch (ServiceException e) {
      xLogger.warn("Error while fetching material name", e);
    }
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
    itemBatchModel.setExpiry(LocalDateUtil.formatCustom(batch.getExpiryDate(), Constants.DATE_FORMAT,null));
    itemBatchModel.setManufacturer(batch.getManufacturer());
    itemBatchModel.setManufacturedDate(LocalDateUtil.formatCustom(batch.getManufacturedDate(), Constants.DATE_FORMAT,null));
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
      entityModel.setAddress(kiosk.getFormattedAddress());
      entityModel.setHasAccess(EntityAuthoriser.authoriseEntity(entityId));
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

  public UpdateStatusModel buildUpdateStatusModel(Long returnId,String status,ReturnsUpdateStatusRequestModel returnsUpdateStatusRequestModel){
    UpdateStatusModel updateStatusModel=new UpdateStatusModel();
    Status actualStatus;
    switch (status) {
      case "ship": actualStatus = Status.SHIPPED; break;
      case "receive": actualStatus = Status.RECEIVED; break;
      case "cancel": actualStatus = Status.CANCELLED; break;
      default: throw new InvalidDataException("Invalid status " + status + " for returns " + returnId);
    }
    updateStatusModel.setStatus(actualStatus);
    updateStatusModel.setReturnId(returnId);
    updateStatusModel.setUserId(SecurityUtils.getUsername());
    updateStatusModel.setComment(returnsUpdateStatusRequestModel.getComment());
    updateStatusModel.setSource(returnsUpdateStatusRequestModel.getSource());
    return updateStatusModel;
  }
}