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

import com.logistimo.config.models.DomainConfig;
import com.logistimo.constants.Constants;
import com.logistimo.exception.InvalidServiceException;
import com.logistimo.exception.SystemException;
import com.logistimo.exception.ValidationException;
import com.logistimo.logger.XLog;
import com.logistimo.orders.entity.IOrder;
import com.logistimo.orders.service.OrderManagementService;
import com.logistimo.returns.actions.CreateReturnsAction;
import com.logistimo.returns.actions.GetReturnsAction;
import com.logistimo.returns.actions.UpdateReturnAction;
import com.logistimo.returns.actions.UpdateReturnsTrackingDetailAction;
import com.logistimo.returns.builders.ReturnsQuantityBuilder;
import com.logistimo.returns.models.ReturnsFilters;
import com.logistimo.returns.transactions.ReturnsTransactionHandler;
import com.logistimo.returns.validators.ReturnsValidationHandler;
import com.logistimo.returns.vo.ReturnsQuantityDetailsVO;
import com.logistimo.returns.vo.ReturnsQuantityVO;
import com.logistimo.returns.vo.ReturnsTrackingDetailsVO;
import com.logistimo.returns.vo.ReturnsVO;
import com.logistimo.services.DuplicationException;
import com.logistimo.services.ServiceException;
import com.logistimo.shipments.FulfilledQuantityModel;
import com.logistimo.shipments.service.impl.ShipmentService;
import com.logistimo.utils.LockUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Created by pratheeka on 13/03/18.
 */
@Service
@EnableTransactionManagement
public class ReturnsService {

  private static final XLog xLogger = XLog.getLog(ReturnsService.class);

  @Autowired
  private ShipmentService shipmentService;

  @Autowired
  private OrderManagementService orderManagementService;

  @Autowired
  private ReturnsTransactionHandler returnsTransactionHandler;

  @Autowired
  private ReturnsCommentService returnsCommentService;

  @Autowired
  private ReturnsStatusHistoryService returnsStatusHistoryService;

  @Autowired
  private CreateReturnsAction createReturnsAction;

  @Autowired
  private UpdateReturnAction updateReturnAction;

  @Autowired
  private ReturnsRepository returnsRepository;

  @Autowired
  private GetReturnsAction getReturnAction;

  @Autowired
  private ReturnsValidationHandler returnsValidationHandler;

  @Autowired
  private UpdateReturnsTrackingDetailAction updateReturnsTrackingDetailAction;


  private ReturnsQuantityBuilder returnsQuantityBuilder=new ReturnsQuantityBuilder();

  @Transactional(transactionManager = "transactionManager", rollbackFor = Exception.class)
  public ReturnsVO createReturns(ReturnsVO returnsVO) {
    LockUtil.LockStatus lockStatus = getLockStatus(returnsVO);
    isLocked(returnsVO, lockStatus);
    try {
      returnsValidationHandler.validateQuantity(returnsVO.getOrderId(),returnsVO.getId(),returnsVO.getItems());
      returnsValidationHandler.validateReturnsPolicy(returnsVO.getOrderId(),
          returnsVO.getVendorId());
      createReturnsAction.invoke(returnsVO);
      String messageId = returnsCommentService.addComment(returnsVO.getId(), returnsVO.getComment(),
          returnsVO.getUpdatedBy(), returnsVO.getSourceDomain());
      returnsStatusHistoryService.addStatusHistory(returnsVO, null, returnsVO.getStatusValue(), messageId);
      return returnsVO;
    } catch (Exception e) {
      xLogger.warn("Error while creating return", e);
      throw new SystemException(e.getMessage());
    } finally {
      releaseLock(returnsVO, LockUtil.shouldReleaseLock(lockStatus) && !LockUtil
          .release(Constants.TX_O + returnsVO.getOrderId()));
    }
  }

  @Transactional(transactionManager = "transactionManager", rollbackFor = Exception.class)
  public ReturnsVO updateReturnsStatus(ReturnsVO updatedReturnVO, Long domainId)
      throws ServiceException, DuplicationException {
    ReturnsVO returnsVO = getReturn(updatedReturnVO.getId());
    LockUtil.LockStatus lockStatus = getLockStatus(returnsVO);
    isLocked(returnsVO, lockStatus);
    try {
      returnsValidationHandler
          .validateStatusChange(updatedReturnVO.getStatusValue(), returnsVO.getStatusValue());
      if (returnsValidationHandler
          .checkAccessForStatusChange(updatedReturnVO.getStatusValue(), returnsVO)) {
        if (updatedReturnVO.isShipped()) {
          returnsValidationHandler.validateShippingQtyAgainstAvailableQty(returnsVO);
        }
        if (updatedReturnVO.isReceived()) {
          returnsValidationHandler.validateHandlingUnit(updatedReturnVO.getItems());
        }
        try {
         ReturnsVO savedReturnsVO = updateReturnAction.invoke(updatedReturnVO);
          IOrder order = orderManagementService.getOrder(savedReturnsVO.getOrderId());
          String messageId = returnsCommentService.addComment(savedReturnsVO.getId(), updatedReturnVO.getComment(),
              savedReturnsVO.getUpdatedBy(), savedReturnsVO.getSourceDomain());
          returnsStatusHistoryService.addStatusHistory(savedReturnsVO, returnsVO.getStatusValue(), savedReturnsVO.getStatusValue(), messageId);
          if (!(returnsVO.isOpen() && savedReturnsVO.isCancelled())) {
            postTransactions(order.getOrderType() == IOrder.TRANSFER_ORDER, savedReturnsVO, domainId);
          }
        } catch (Exception e) {
          xLogger.warn("Exception while updating returns status", e);
          throw e;
        }
      } else {
        throw new ValidationException("RT003", (Object[]) null);
      }
    } finally {
      releaseLock(returnsVO, LockUtil.shouldReleaseLock(lockStatus) &&
          !LockUtil.release(Constants.TX_O + returnsVO.getOrderId()));
    }
    return returnsVO;
  }

  @Transactional(transactionManager = "transactionManager", rollbackFor = Exception.class)
  public ReturnsVO updateReturnItems(ReturnsVO updatedReturnsVO) {
    ReturnsVO returnsVO = getReturn(updatedReturnsVO.getId());
    LockUtil.LockStatus lockStatus = getLockStatus(returnsVO);
    isLocked(returnsVO, lockStatus);
    try {
      returnsValidationHandler.validateQuantity(returnsVO.getOrderId(),returnsVO.getId(),updatedReturnsVO.getItems());
      return updateReturnAction.invoke(updatedReturnsVO);
    } finally {
      releaseLock(returnsVO, LockUtil.shouldReleaseLock(lockStatus) &&
          !LockUtil.release(Constants.TX_O + returnsVO.getOrderId()));
    }
  }

  @Transactional(transactionManager = "transactionManager")
  public String addComment(Long returnId, String message, String userId, Long domainId) {
    return returnsCommentService.addComment(returnId, message, userId, domainId);
  }

  @Transactional(transactionManager = "transactionManager", rollbackFor = Exception.class)
  public ReturnsTrackingDetailsVO saveTransporterDetails(
      ReturnsTrackingDetailsVO returnsTrackingDetailsVO, Long returnId) {
    return updateReturnsTrackingDetailAction.invoke(returnsTrackingDetailsVO, returnId);
  }

  public ReturnsVO getReturn(Long returnId) {
    return getReturnAction.invoke(returnId);
  }

  public Long getReturnsCount(ReturnsFilters filters) {
    return returnsRepository.getReturnsCount(filters);
  }

  public List<ReturnsVO> getReturns(ReturnsFilters returnsFilters, boolean includeItems) {
    return returnsRepository.getReturns(returnsFilters, includeItems);
  }

  public List<ReturnsQuantityVO> getReturnsQuantityDetails(Long orderId) throws ServiceException {
    List<FulfilledQuantityModel> shipmentList =
        shipmentService.getFulfilledQuantityByOrderId(orderId, null);
    List<ReturnsQuantityDetailsVO> returnsQuantityDetailsVOs =
        returnsRepository.getAllReturnsQuantityByOrderId(orderId);
    return returnsQuantityBuilder.buildReturnsQuantityList(shipmentList, returnsQuantityDetailsVOs);
  }

  private void isLocked(ReturnsVO returnsVO, LockUtil.LockStatus lockStatus) {
    if (!LockUtil.isLocked(lockStatus)) {
      throw new InvalidServiceException(new ServiceException("O002", returnsVO.getOrderId()));
    }
  }

  private LockUtil.LockStatus getLockStatus(ReturnsVO returnsVO) {
    return LockUtil.lock(Constants.TX_O + returnsVO.getOrderId());
  }

  private void postTransactions(boolean isTransferOrder, ReturnsVO returnsVO, Long currentDomainId)
      throws ServiceException, DuplicationException {
      Long returnsDomainId = returnsVO.getSourceDomain();
      DomainConfig domainConfig = DomainConfig.getInstance(returnsDomainId);
      if (domainConfig.autoGI()) {
        returnsTransactionHandler.postTransactions(isTransferOrder, returnsVO, currentDomainId);
    }
  }

  private void releaseLock(ReturnsVO returnsVO, boolean isLocked) {
    if (isLocked) {
      xLogger.warn("Unable to release lock for key {0}", Constants.TX_O + returnsVO.getOrderId());
    }
  }


}
