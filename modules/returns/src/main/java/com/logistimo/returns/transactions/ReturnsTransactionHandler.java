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
import com.logistimo.exception.SystemException;
import com.logistimo.inventory.entity.ITransaction;
import com.logistimo.inventory.models.CreateTransactionsReturnModel;
import com.logistimo.inventory.service.InventoryManagementService;
import com.logistimo.returns.Status;
import com.logistimo.returns.vo.BatchVO;
import com.logistimo.returns.vo.ReturnsItemBatchVO;
import com.logistimo.returns.vo.ReturnsItemVO;
import com.logistimo.returns.vo.ReturnsVO;
import com.logistimo.services.DuplicationException;
import com.logistimo.services.ServiceException;
import com.logistimo.services.impl.PMF;
import com.logistimo.utils.BigUtil;
import com.logistimo.utils.MsgUtil;
import com.logistimo.utils.Stream;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.jdo.PersistenceManager;
import javax.jdo.Transaction;

import lombok.Builder;
import lombok.Getter;

/**
 * Created by pratheeka on 18/03/18.
 */
@Component
public class ReturnsTransactionHandler {

  @Autowired
  private InventoryManagementService inventoryManagementService;

  public void setInventoryManagementService(InventoryManagementService inventoryManagementService) {
    this.inventoryManagementService = inventoryManagementService;
  }

  public void postTransactions(boolean isTransferOrder, ReturnsVO returnsVO, Long domainId)
      throws DuplicationException, ServiceException {
    List<ITransaction> transactionsList = returnsVO.getItems().stream()
        .filter(returnsItemVO -> BigUtil.greaterThanZero(returnsItemVO.getReceived().getQuantity()))
        .flatMap(returnsItemVO ->
                getTransactions(domainId, returnsItemVO, returnsVO, isTransferOrder).stream())
        .collect(Collectors.toList());

    PersistenceManager pm = getPersistenceManager();
    Transaction tx = pm.currentTransaction();
    try {
      tx.begin();
      CreateTransactionsReturnModel createTransactionsReturnModel = inventoryManagementService
          .updateInventoryTransactions(domainId, transactionsList, null, true, false, pm);

      List<ITransaction> errorTransactions = createTransactionsReturnModel.getErrorTransactions();
      if (CollectionUtils.isNotEmpty(errorTransactions)) {
        StringBuilder errorMsg = new StringBuilder(MsgUtil.newLine());
        for (ITransaction error : errorTransactions) {
          errorMsg.append("- ").append(error.getMessage()).append(MsgUtil.newLine());
        }
        throw new SystemException("RT008", errorMsg.toString());
      }
      tx.commit();
    } catch (Exception e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      throw e;
    } finally {
      pm.close();
    }
  }

  public PersistenceManager getPersistenceManager() {
    return PMF.get().getPersistenceManager();
  }

  private List<ITransaction> getTransactions(Long domainId, ReturnsItemVO returnsItemVo,
                                             ReturnsVO returnsVO, boolean isTransferOrder) {
    Long kioskId = (returnsVO.isReceived()) ? returnsVO.getVendorId()
            : returnsVO.getCustomerId();
    Long linkedKioskId = null;
    if (!returnsVO.isCancelled()) {
      linkedKioskId = (returnsVO.isReceived()) ? returnsVO.getCustomerId()
              : returnsVO.getVendorId();
    }
    final Long vendorKioskId = linkedKioskId;
    String trackingObjectType =
        isTransferOrder ? ITransaction.TRACKING_OBJECT_TYPE_TRANSFER
            : ITransaction.TRACKING_OBJECT_TYPE_ORDER;
    Integer source = returnsVO.getSource();
    String userId = returnsVO.getUpdatedBy();
    Status status = returnsVO.getStatusValue();
    Long returnId = returnsVO.getId();
    if (returnsItemVo.hasBatches()) {
      return Stream.toList(returnsItemVo.getReturnItemBatches(), returnsItemBatchVO -> {
        ITransaction transaction = JDOUtils.createInstance(ITransaction.class);
        setBatch(returnsItemBatchVO, transaction);
        setType(status, transaction);
        BigDecimal quantity = returnsItemBatchVO.getQuantity();
        if (status == Status.RECEIVED) {
          quantity = returnsItemBatchVO.getReceived().getQuantity();
        }
        TransactionModel transactionModel = TransactionModel.builder()
            .userId(userId)
            .domainId(domainId)
            .kioskId(kioskId)
            .linkedKioskId(vendorKioskId)
            .reason(returnsItemBatchVO.getReason())
            .matStatus(returnsItemBatchVO.getMaterialStatus())
            .materialId(returnsItemVo.getMaterialId())
            .quantity(quantity)
            .source(source)
            .returnId(returnId)
            .trackingObjectType(trackingObjectType)
            .build();
        buildTransaction(transactionModel, transaction);
        return transaction;
      });
    } else {
      ITransaction transaction = JDOUtils.createInstance(ITransaction.class);
      setType(status, transaction);
      BigDecimal quantity = returnsItemVo.getQuantity();
      if (status == Status.RECEIVED) {
        quantity = returnsItemVo.getReceived().getQuantity();
      }
      TransactionModel transactionModel = TransactionModel.builder()
          .userId(userId)
          .domainId(domainId)
          .kioskId(kioskId)
          .linkedKioskId(vendorKioskId)
          .reason(returnsItemVo.getReason())
          .matStatus(returnsItemVo.getMaterialStatus())
          .materialId(returnsItemVo.getMaterialId())
          .quantity(quantity)
          .source(source)
          .returnId(returnId)
          .trackingObjectType(trackingObjectType)
          .build();
      buildTransaction(transactionModel, transaction);
      return Collections.singletonList(transaction);
    }
  }

  private void setBatch(ReturnsItemBatchVO returnsItemBatchVO, ITransaction transaction) {
    BatchVO batchVO = returnsItemBatchVO.getBatch();
    transaction.setBatchId(batchVO.getBatchId());
    transaction.setBatchExpiry(batchVO.getExpiryDate());
    transaction.setBatchManufacturedDate(batchVO.getManufacturedDate());
    transaction.setBatchManufacturer(batchVO.getManufacturer());
  }

  private void setType(Status status, ITransaction transaction) {
    if (status == Status.RECEIVED) {
      transaction.setType(ITransaction.TYPE_RETURNS_INCOMING);
    } else if (status == Status.CANCELLED) {
      transaction.setType(ITransaction.TYPE_RECEIPT);
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
}

@Getter
@Builder
class TransactionModel {
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
