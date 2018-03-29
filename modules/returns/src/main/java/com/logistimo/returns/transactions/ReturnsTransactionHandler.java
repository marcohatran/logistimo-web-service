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

package com.logistimo.returns.transactions;

import com.logistimo.dao.JDOUtils;
import com.logistimo.inventory.entity.ITransaction;
import com.logistimo.inventory.service.InventoryManagementService;
import com.logistimo.returns.Status;
import com.logistimo.returns.models.UpdateStatusModel;
import com.logistimo.returns.vo.BatchVO;
import com.logistimo.returns.vo.ReturnsItemVO;
import com.logistimo.returns.vo.ReturnsVO;
import com.logistimo.services.DuplicationException;
import com.logistimo.services.ServiceException;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Created by pratheeka on 18/03/18.
 */
@Component
public class ReturnsTransactionHandler {

  @Autowired
  private InventoryManagementService inventoryManagementService;

  public void postTransactions(UpdateStatusModel statusModel, ReturnsVO returnsVO,
                               Long domainId)
      throws DuplicationException, ServiceException {

    List<ITransaction> transactionsList = new ArrayList<>();
    Long customerId =
        (statusModel.getStatus() == Status.RECEIVED) ? returnsVO.getVendorId()
            : returnsVO.getCustomerId();
    Long vendorId =
        (statusModel.getStatus() == Status.RECEIVED) ? returnsVO.getCustomerId()
            : returnsVO.getVendorId();
    String
        trackingObjectType =
        statusModel.isTransferOrder() ? ITransaction.TRACKING_OBJECT_TYPE_TRANSFER
            : ITransaction.TRACKING_OBJECT_TYPE_ORDER;
    returnsVO.getItems().forEach(returnsItemVO ->
        transactionsList.addAll(
            getTransactions(statusModel.getUserId(), domainId, customerId,
                vendorId, statusModel.getSource(), returnsItemVO, statusModel.getStatus(),
                trackingObjectType)));

    inventoryManagementService
        .updateInventoryTransactions(domainId, transactionsList, null, true, false, null);


  }

  private List<ITransaction> getTransactions(String userId, Long domainId, Long kioskId,
                                             Long linkedKioskId, Integer source,
                                             ReturnsItemVO returnsItemVo, Status status,
                                             String trackingObjType) {
    if (CollectionUtils.isNotEmpty(returnsItemVo.getReturnItemBatches())) {
      return returnsItemVo.getReturnItemBatches().stream().map(returnsItemBatchVO -> {
        ITransaction transaction = JDOUtils.createInstance(ITransaction.class);
        BatchVO batchVO = returnsItemBatchVO.getBatch();
        transaction.setBatchId(batchVO.getBatchId());
        transaction.setBatchExpiry(batchVO.getExpiryDate());
        transaction.setBatchManufacturedDate(batchVO.getManufacturedDate());
        transaction.setBatchManufacturer(batchVO.getManufacturer());
        setType(status, transaction);
        buildTransaction(new TransactionModel(userId, domainId, kioskId, linkedKioskId,
            returnsItemBatchVO.getReason(),
            returnsItemBatchVO.getMaterialStatus(),
            returnsItemVo.getMaterialId(), returnsItemBatchVO.getQuantity(), source,
            returnsItemVo.getReturnsId(), trackingObjType), transaction);
        return transaction;
      }).collect(Collectors.toList());
    } else {
      ITransaction transaction = JDOUtils.createInstance(ITransaction.class);
      setType(status, transaction);
      buildTransaction(
          new TransactionModel(userId, domainId, kioskId, linkedKioskId, returnsItemVo.getReason(),
              returnsItemVo.getMaterialStatus(),
              returnsItemVo.getMaterialId(), returnsItemVo.getQuantity(), source,
              returnsItemVo.getReturnsId(), trackingObjType), transaction);
      return Collections.singletonList(transaction);
    }
  }

  private void setType(Status status, ITransaction transaction) {
    if (status == Status.RECEIVED) {
      transaction.setType(ITransaction.TYPE_RETURNS_INCOMING);
    } else {
      transaction.setType(ITransaction.TYPE_RETURNS_OUTGOING);
    }
  }

  private void buildTransaction(TransactionModel transactionModel, ITransaction transaction) {
    transaction.setSrc(transactionModel.getSource());
    transaction.setDomainId(transactionModel.getDomainId());
    transaction.setKioskId(transactionModel.getKioskId());
    transaction.setMaterialId(transactionModel.getMaterialId());
    transaction.setQuantity(transactionModel.getQuantity());
    transaction.setSourceUserId(transactionModel.getUserId());
    transaction.setTimestamp(new Date());
    transaction.setReason(transactionModel.getReason());
    transaction.setMaterialStatus(transactionModel.getMatStatus());
    transaction.setLinkedKioskId(transactionModel.getLinkedKioskId());
    transaction.setTrackingId(transactionModel.getReturnId().toString());
    transaction.setTrackingObjectType(transactionModel.getTrackingObjectType());
  }

  @Data
  @AllArgsConstructor
  private class TransactionModel {
    String userId;
    Long domainId;
    Long kioskId;
    Long linkedKioskId;
    String reason;
    String matStatus;
    Long materialId;
    BigDecimal quantity;
    Integer source;
    Long returnId;
    String trackingObjectType;
  }
}
