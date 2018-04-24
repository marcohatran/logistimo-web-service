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

import com.logistimo.activity.entity.IActivity;
import com.logistimo.activity.models.ActivityModel;
import com.logistimo.activity.service.impl.ActivitiesServiceImpl;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.config.models.ReturnsConfig;
import com.logistimo.constants.Constants;
import com.logistimo.conversations.service.impl.ConversationsServiceImpl;
import com.logistimo.exception.InvalidServiceException;
import com.logistimo.exception.ValidationException;
import com.logistimo.inventory.service.InventoryManagementService;
import com.logistimo.logger.XLog;
import com.logistimo.materials.model.HandlingUnitModel;
import com.logistimo.materials.service.IHandlingUnitService;
import com.logistimo.orders.entity.IOrder;
import com.logistimo.orders.service.OrderManagementService;
import com.logistimo.orders.service.impl.DemandService;
import com.logistimo.pagination.Results;
import com.logistimo.returns.Status;
import com.logistimo.returns.models.ReturnsFilters;
import com.logistimo.returns.models.UpdateStatusModel;
import com.logistimo.returns.transactions.ReturnsTransactionHandler;
import com.logistimo.returns.validators.ReturnsValidator;
import com.logistimo.returns.vo.ReturnsItemBatchVO;
import com.logistimo.returns.vo.ReturnsItemVO;
import com.logistimo.returns.vo.ReturnsStatusVO;
import com.logistimo.returns.vo.ReturnsVO;
import com.logistimo.services.DuplicationException;
import com.logistimo.services.ServiceException;
import com.logistimo.shipments.FulfilledQuantityModel;
import com.logistimo.shipments.service.impl.ShipmentService;
import com.logistimo.utils.LockUtil;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by pratheeka on 13/03/18.
 */
@Service
@EnableTransactionManagement
public class ReturnsService {

  private static final XLog xLogger = XLog.getLog(ReturnsService.class);

  @Autowired
  private ReturnsValidator returnsValidator;

  @Autowired
  private DemandService demandService;

  @Autowired
  private ReturnsRepository returnsRepository;

  @Autowired
  private ActivitiesServiceImpl activityService;

  @Autowired
  private ConversationsServiceImpl conversationService;

  @Autowired
  private InventoryManagementService inventoryManagementService;

  @Autowired
  private ShipmentService shipmentService;

  @Autowired
  private IHandlingUnitService handlingUnitService;

  @Autowired
  private OrderManagementService orderManagementService;

  @Autowired
  private ReturnsTransactionHandler returnsTransactionHandler;


  @Transactional(transactionManager = "transactionManager")
  public ReturnsVO createReturns(ReturnsVO returnsVO) throws ServiceException {

    LockUtil.LockStatus lockStatus = LockUtil.lock(Constants.TX_O + returnsVO.getOrderId());
    if (!LockUtil.isLocked(lockStatus)) {
      throw new InvalidServiceException(new ServiceException("O002", returnsVO.getOrderId()));
    }
    try {
      List<ReturnsItemVO> returnsItemVOList = returnsVO.getItems();
      validateQuantity(returnsVO, returnsItemVOList);
      validateReturnsPolicy(returnsVO.getOrderId(), returnsVO.getVendorId());
      returnsRepository.saveReturns(returnsVO);
      Map<Long, BigDecimal>
          quantityByMaterial =
          saveReturnItems(returnsVO.getId(), returnsItemVOList);
      demandService.updateDemandReturns(returnsVO.getOrderId(), quantityByMaterial, false);

      String messageId = addComment(returnsVO.getId(), returnsVO.getComment(),
          returnsVO.getCreatedBy(), returnsVO.getSourceDomain());
      addStatusHistory(returnsVO, null, returnsVO.getStatus().getStatus().toString(), messageId);

      return returnsVO;
    } finally {
      if (LockUtil.shouldReleaseLock(lockStatus) && !LockUtil
          .release(Constants.TX_O + returnsVO.getOrderId())) {
        xLogger.warn("Unable to release lock for key {0}", Constants.TX_O + returnsVO.getOrderId());
      }
    }
  }

  private Map<Long, BigDecimal> saveReturnItems(Long returnId,
                                                List<ReturnsItemVO> returnsItemVOList) {
    Map<Long, BigDecimal> quantityByMaterial = new HashMap<>();
    returnsItemVOList.forEach(returnsItemVO -> {
      returnsItemVO.setReturnsId(returnId);
      returnsRepository.saveReturnsItems(returnsItemVO);
      BigDecimal quantity = BigDecimal.ZERO;
      List<ReturnsItemBatchVO>
          returnsItemBatchVOList = returnsItemVO.getReturnItemBatches();
      if (CollectionUtils.isNotEmpty(returnsItemBatchVOList)) {
        Long itemId = returnsItemVO.getId();
        quantity = saveBatchItems(itemId, returnsItemBatchVOList);
        returnsItemVO.setReturnItemBatches(returnsItemBatchVOList);
      } else {
        quantity = returnsItemVO.getQuantity();
      }
      quantityByMaterial.put(returnsItemVO.getMaterialId(), quantity);
    });
    return quantityByMaterial;
  }

  private BigDecimal saveBatchItems(Long itemId, List<ReturnsItemBatchVO> returnsItemBatchVOList) {
    return returnsItemBatchVOList.stream().map(returnsItemBatchVO -> {
      returnsItemBatchVO.setItemId(itemId);
      returnsRepository.saveReturnBatchItems(returnsItemBatchVO);
      return returnsItemBatchVO.getQuantity();
    }).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private void validateQuantity(ReturnsVO returnsVO, List<ReturnsItemVO> returnsItemVOList)
      throws ServiceException {
    if (CollectionUtils.isEmpty(returnsItemVOList)) {
      throw new ValidationException("RT001", (Object[]) null);
    }
    List<Long> materialIdList = returnsItemVOList.stream().map(ReturnsItemVO::getMaterialId)
        .collect(Collectors.toList());

    List<FulfilledQuantityModel> shipmentList =
        shipmentService.getFulfilledQuantityByOrderId(returnsVO.getOrderId(), materialIdList);

    List<HandlingUnitModel> handlingUnitModelList =
        handlingUnitService.getHandlingUnitDataByMaterialIds(materialIdList);

    returnsValidator
        .validateReturnedQuantity(returnsVO.getItems(), shipmentList, handlingUnitModelList);
  }

  private void validateReturnsPolicy(Long orderId, Long vendorId) throws ServiceException {
    Optional<ReturnsConfig> returnsConfiguration =
        inventoryManagementService.getReturnsConfig(vendorId);

    if (!returnsConfiguration.isPresent()) {
      return;
    }
    Date fulfilDate = orderManagementService.getOrder(orderId).getStatusUpdatedOn();
    returnsValidator.validateReturnsPolicy(returnsConfiguration.get(), fulfilDate.getTime());

  }

  public List<ReturnsItemVO> getReturnsItem(Long returnId) {
    if (returnId == null) {
      throw new ValidationException("RT002", (Object[]) null);
    }
    return returnsRepository.getReturnedItems(returnId);
  }

  @Transactional
  public ReturnsVO updateReturnsStatus(UpdateStatusModel statusModel)
      throws ServiceException, DuplicationException {
    ReturnsVO returnsVO = returnsRepository.getReturnsById(statusModel.getReturnId());
    Status newStatus = statusModel.getStatus();
    Status oldStatus = returnsVO.getStatus().getStatus();
    returnsValidator.validateStatusChange(newStatus, oldStatus);
    if (returnsValidator.checkAccessForStatusChange(statusModel, returnsVO)) {

      buildReturns(statusModel, returnsVO, newStatus);
      List<ReturnsItemVO> returnsItemVOList = getReturnsItem(returnsVO.getId());
      if (statusModel.getStatus() == Status.SHIPPED) {
        Results results =
            inventoryManagementService.getInventoryByKiosk(returnsVO.getCustomerId(), null);
        returnsValidator.validateShippedQuantity(returnsItemVOList, results.getResults());
      }
      returnsRepository.updateReturns(returnsVO);
      returnsVO.setItems(returnsItemVOList);
      updateDemandItems(returnsVO, statusModel.getStatus());
      IOrder order = orderManagementService.getOrder(returnsVO.getOrderId());
      statusModel.setTransferOrder(order.getOrderType() == IOrder.TRANSFER_ORDER);
      postTransactions(statusModel, returnsVO);
      String
          messageId =
          addComment(returnsVO.getId(), statusModel.getComment(), returnsVO.getUpdatedBy(),
              returnsVO.getSourceDomain());
      addStatusHistory(returnsVO, oldStatus.toString(), newStatus.toString(), messageId);
    } else {
      throw new ValidationException("RT003", (Object[]) null);
    }
    return returnsVO;
  }

  private void updateDemandItems(ReturnsVO returnsVO, Status status) {
    if (status == Status.CANCELLED) {
      Map<Long, BigDecimal> returnedQuantity = new HashMap<>();
      returnsVO.getItems().forEach(returnsItemVO -> {
        BigDecimal quantity = BigDecimal.ZERO;
        if (CollectionUtils.isNotEmpty(returnsItemVO.getReturnItemBatches())) {
          quantity =
              returnsItemVO.getReturnItemBatches().stream().map(ReturnsItemBatchVO::getQuantity)
                  .reduce(BigDecimal.ZERO, BigDecimal::add);
        } else {
          quantity = returnsItemVO.getQuantity();
        }
        returnedQuantity.put(returnsItemVO.getMaterialId(), quantity);
      });
      demandService.updateDemandReturns(returnsVO.getOrderId(), returnedQuantity, true);
    }
  }


  private void buildReturns(UpdateStatusModel statusModel, ReturnsVO returnsVO,
                            Status newStatus) {
    ReturnsStatusVO statusVO = new ReturnsStatusVO();
    statusVO.setStatus(newStatus);
    Timestamp updatedAt = new Timestamp(new Date().getTime());
    statusVO.setUpdatedAt(updatedAt);
    statusVO.setUpdatedBy(statusModel.getUserId());
    returnsVO.setStatus(statusVO);
    returnsVO.setUpdatedBy(statusModel.getUserId());
    returnsVO.setUpdatedAt(updatedAt);
  }

  private void postTransactions(UpdateStatusModel statusModel, ReturnsVO returnsVO)
      throws ServiceException, DuplicationException {
    if (statusModel.getStatus() == Status.RECEIVED || statusModel.getStatus() == Status.SHIPPED) {
      Long domainId = SecurityUtils.getCurrentDomainId();
      Long returnsDomainId=returnsVO.getSourceDomain();
      DomainConfig domainConfig=DomainConfig.getInstance(returnsDomainId);
      if(domainConfig.autoGI()) {
        returnsTransactionHandler.postTransactions(statusModel, returnsVO, domainId);
      }
    }
  }

  public ReturnsVO getReturnsById(Long returnId) {
    ReturnsVO returnsVO = returnsRepository.getReturnsById(returnId);
    returnsVO.setItems(getReturnsItem(returnId));
    return returnsVO;
  }

  public List<ReturnsVO> getReturns(ReturnsFilters filters) {
    return returnsRepository.getReturns(filters);
  }

  private void addStatusHistory(ReturnsVO returnVO, String oldStatus, String newStatus,
                                String messageId) {
    ActivityModel activityModel=new ActivityModel();
    activityModel.objectType=IActivity.TYPE.RETURNS.name();
    activityModel.objectId= String.valueOf(returnVO.getId());
    activityModel.field="STATUS";
    activityModel.prevValue=oldStatus;
    activityModel.newValue=newStatus;
    activityModel.userId=returnVO.getCreatedBy();
    activityModel.domainId=returnVO.getSourceDomain();
    activityModel.messageId=messageId;
    activityModel.tag="RETURNS:" + returnVO.getId();
    activityService
        .saveActivity(activityModel);

  }


  private String addComment(Long returnId, String message, String userId, Long domainId)
      throws ServiceException {
    if (StringUtils.isNotBlank(message)) {
     return conversationService
          .addMessageToConversation(IActivity.TYPE.RETURNS.name(), String.valueOf(returnId),
              message, userId, Collections.singleton("RETURNS:" + returnId), domainId, null);
    }
    return null;
  }
  @Transactional(transactionManager = "transactionManager")
  public String postComment(Long returnId, String message, String userId, Long domainId) throws ServiceException{
    return addComment(returnId,message,userId,domainId);
  }

  public Long getReturnsCount(ReturnsFilters returnsFilters) {
    return returnsRepository.getReturnsCount(returnsFilters);
  }

}
