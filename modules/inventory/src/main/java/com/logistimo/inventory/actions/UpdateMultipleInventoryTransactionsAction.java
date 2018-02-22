/*
 * Copyright © 2017 Logistimo.
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

package com.logistimo.inventory.actions;

import com.logistimo.config.models.ConfigurationException;
import com.logistimo.config.models.GeneralConfig;
import com.logistimo.constants.Constants;
import com.logistimo.context.StaticApplicationContext;
import com.logistimo.exception.LogiException;
import com.logistimo.inventory.TransactionUtil;
import com.logistimo.inventory.entity.ITransaction;
import com.logistimo.inventory.models.ErrorDetailModel;
import com.logistimo.inventory.models.ResponseDetailModel;
import com.logistimo.inventory.models.SuccessDetailModel;
import com.logistimo.inventory.policies.InventoryUpdatePolicy;
import com.logistimo.inventory.service.InventoryManagementService;
import com.logistimo.logger.XLog;
import com.logistimo.pagination.PageParams;
import com.logistimo.pagination.Results;
import com.logistimo.services.DuplicationException;
import com.logistimo.services.ServiceException;
import com.logistimo.services.impl.PMF;
import com.logistimo.utils.LockUtil;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.jdo.PersistenceManager;

/**
 * Created by charan on 14/11/17.
 */
@Component
public class UpdateMultipleInventoryTransactionsAction {

  private static final XLog xLogger = XLog.getLog(UpdateMultipleInventoryTransactionsAction.class);

  private InventoryManagementService inventoryManagementService;

  @Autowired
  public void setInventoryManagementService(InventoryManagementService inventoryManagementService) {
    this.inventoryManagementService = inventoryManagementService;
  }

  private static final int LOCK_RETRY_COUNT = 25;
  private static final int LOCK_RETRY_DELAY_IN_MILLISECONDS = 2400;

  public Map<Long, ResponseDetailModel> execute(
      Map<Long, List<ITransaction>> materialTransactionsMap, Long domainId, String userId)
      throws ServiceException, ConfigurationException {
    if (materialTransactionsMap == null || materialTransactionsMap.isEmpty() || domainId == null
        || StringUtils
        .isEmpty(userId)) {
      throw new ServiceException(
          "Missing or invalid mandatory attributes while updating multiple inventory transactions");
    }
    PersistenceManager pm = null;
    javax.jdo.Transaction tx = null;
    Map<Long, LockUtil.LockStatus> locks = new HashMap<>();
    Map<Long, List<ErrorDetailModel>> materialErrorDetailModelsMap = new HashMap<>(1);
    Map<Long, List<SuccessDetailModel>> materialSuccessDetailModelsMap = new HashMap<>(1);
    try {
      pm = getPersistenceManager();
      tx = pm.currentTransaction();
      tx.begin();

      Map<Long, Integer> midCountMap = new HashMap<>();
      Map<Long, Integer> midFailedFromPositionMap = new HashMap<>();
      InventoryUpdatePolicy
          inventoryUpdatePolicy =
          StaticApplicationContext.getBean(GeneralConfig.getInstance().getInventoryPolicy(),
              InventoryUpdatePolicy.class);

      List<ITransaction>
          validTransactions =
          validateAndApplyPolicy(materialTransactionsMap, locks, materialErrorDetailModelsMap,
              midCountMap, midFailedFromPositionMap, inventoryUpdatePolicy);

      // If no validTransactions, return
      if (validTransactions.isEmpty()) {
        return getMaterialResponseDetailModel(materialSuccessDetailModelsMap,materialErrorDetailModelsMap);
      }

      // Shuffle and sort the transactions by entry time
      Collections.sort(validTransactions, new EntryTimeComparator());

      executeTransactions(domainId, pm, materialErrorDetailModelsMap, materialSuccessDetailModelsMap, midCountMap,
          validTransactions,
          midFailedFromPositionMap, inventoryUpdatePolicy);

      tx.commit();
    } finally {
      if (tx != null && tx.isActive()) {
        tx.rollback();
      }
      if (pm != null && !pm.isClosed()) {
        pm.close();
      }
      LockUtil.releaseLocks(locks, Constants.TX);
    }
    return getMaterialResponseDetailModel(materialSuccessDetailModelsMap,materialErrorDetailModelsMap);
  }

  private PersistenceManager getPersistenceManager() {
    return PMF.get().getPersistenceManager();
  }

  protected List<ITransaction> validateAndApplyPolicy(
      Map<Long, List<ITransaction>> materialTransactionsMap,
      Map<Long, LockUtil.LockStatus> locks,
      Map<Long, List<ErrorDetailModel>> materialErrorDetailModelsMap,
      Map<Long, Integer> midCountMap,
      Map<Long, Integer> midFailedFromPositionMap, InventoryUpdatePolicy inventoryUpdatePolicy)
      throws ServiceException {

    List<ITransaction> validTransactions = new ArrayList<>(1);
    // Iterate over the map and validate transactions for each material
    Set<Long> mids = materialTransactionsMap.keySet();
    for (Long mid : mids) {
      List<ITransaction> transactions = materialTransactionsMap.get(mid);

      filterInvalidTransaction(materialErrorDetailModelsMap, mid, transactions);

      if (transactions.isEmpty()) {
        // No more transactions to process. Continue to the next material.
        continue;
      }
      ITransaction transaction = transactions.get(0);
      Long kioskId = transaction.getKioskId();
      Long materialId = transaction.getMaterialId();
      acquireLocksOfKiosks(locks, transactions);
      ITransaction lastWebTrans = getLastWebTransaction(kioskId, materialId, null);

      int rejectUntilPosition = inventoryUpdatePolicy.apply(transactions, lastWebTrans);

      if (rejectUntilPosition != -1) {
        updateMaterialErrorDetailModelsMap(mid, materialErrorDetailModelsMap, "M011",
            rejectUntilPosition, null);
        midCountMap.put(mid, rejectUntilPosition);
      }
      if (transactions.isEmpty()) {
        // No more transactions to process. Continue to next material.
        continue;
      }
      try {
        // If the transaction has batch, then get a map of batch id  and the first transaction for that batch
        if (transaction.hasBatch()) {
          Map<String, List<ITransaction>>
              bidTransactionsMap =
              getBatchIdFirstTransactionMap(transactions);
          // Iterate through the map and get lastWebTrans for every bid and add stock count if needed for every batch
          for (Map.Entry<String, List<ITransaction>> entry : bidTransactionsMap.entrySet()) {
            ITransaction
                lastWebTransactionForBatch =
                getLastWebTransaction(kioskId, materialId, entry.getKey());
            inventoryUpdatePolicy
                .addStockCountIfNeeded(lastWebTransactionForBatch, entry.getValue());
            if (entry.getValue().size() == 2) {
              // Stock count has been added. Update the transactions
              transactions.add(0, entry.getValue().get(0));
            }
          }
        } else {
          inventoryUpdatePolicy.addStockCountIfNeeded(lastWebTrans, transactions);
        }
      } catch (LogiException e) {
        // Reject all transactions
        transactions.clear();
        updateFailedFromPositionMap(midFailedFromPositionMap, midCountMap,
            materialErrorDetailModelsMap, transaction.getMaterialId(), null, null);
      }
      if (transactions.isEmpty()) {
        continue;
      }
      // Add valid transactions for this material into the main list
      validTransactions.addAll(transactions);
    }
    return validTransactions;
  }

  protected void filterInvalidTransaction(
      Map<Long, List<ErrorDetailModel>> materialErrorDetailModelsMap, Long mid,
      List<ITransaction> transactions) {
    // Pass it through data validation
    int invalidStartPosition = TransactionUtil.filterInvalidTransactions(transactions);
    if (invalidStartPosition != -1) {
      updateMaterialErrorDetailModelsMap(mid, materialErrorDetailModelsMap, "M010",
          invalidStartPosition, null);
    }
  }

  public class EntryTimeComparator implements Comparator<ITransaction> {
    @Override
    public int compare(ITransaction o1, ITransaction o2) {
      return o1.getSortEt().compareTo(o2.getSortEt());
    }
  }

  private void updateMaterialErrorDetailModelsMap(Long mid,
                                                  Map<Long, List<ErrorDetailModel>> materialErrorDetailModelsMap,
                                                  String errorCode, int position, String message) {
    if (materialErrorDetailModelsMap == null) {
      materialErrorDetailModelsMap = new HashMap<>(1);
    }
    List<ErrorDetailModel> errorDetailModels;
    if (materialErrorDetailModelsMap.containsKey(mid)) {
      errorDetailModels = materialErrorDetailModelsMap.get(mid);
    } else {
      errorDetailModels = new ArrayList<>(1);
    }
    errorDetailModels.add(new ErrorDetailModel(errorCode, position, message));
    materialErrorDetailModelsMap.put(mid, errorDetailModels);
  }

  private void updateFailedFromPositionMap(Map<Long, Integer> midFailedFromPositionMap,
                                           Map<Long, Integer> midCountMap,
                                           Map<Long, List<ErrorDetailModel>> materialErrorDetailModelsMap,
                                           Long mid, String errorCode, String message) {
    int failedFromPosition = 0;
    if (midCountMap.containsKey(mid)) {
      failedFromPosition = midCountMap.get(mid) + 1;
    }
    midFailedFromPositionMap.put(mid, failedFromPosition);
    updateMaterialErrorDetailModelsMap(mid, materialErrorDetailModelsMap, errorCode != null?errorCode:"M012",
        failedFromPosition, message);
  }

  private void acquireLocksOfKiosks(Map<Long, LockUtil.LockStatus> locks,
                                    List<ITransaction> transactions) throws ServiceException {
    Set<Long> kiosksToLock = getKioskIdsToLock(transactions);
    Set<Long>
        finalKiosksToLock =
        kiosksToLock.stream().filter(locks::containsKey).collect(Collectors.toSet());
    Map<Long, LockUtil.LockStatus> kidLockStatusMap = LockUtil
        .lock(finalKiosksToLock, Constants.TX, LOCK_RETRY_COUNT, LOCK_RETRY_DELAY_IN_MILLISECONDS);
    for (Map.Entry<Long, LockUtil.LockStatus> entry : kidLockStatusMap.entrySet()) {
      if (!locks.containsKey(entry.getKey())) {
        locks.put(entry.getKey(), entry.getValue());
      }
      if (!LockUtil.isLocked(entry.getValue())) {
        throw new ServiceException("G005", new Object[]{null});
      }
    }
  }

  public ITransaction getLastWebTransaction(Long kid, Long mid, String bid)
      throws ServiceException {
    // Get the latest transaction
    PageParams pageParams = new PageParams(0, 1);
    Results
        results =
        inventoryManagementService.getInventoryTransactions(null, null, null, kid, mid,
            null,
            null, null, null, null, pageParams, bid, false, null);
    ITransaction lastWebTrans = null;
    if (results.getSize() == 1) {
      lastWebTrans = (ITransaction) results.getResults().get(0);
    }
    return lastWebTrans;
  }

  // Get the unique list of kiosk ids to lock. This includes kid and lkid (in case the transaction type is transfer)
  private Set<Long> getKioskIdsToLock(List<ITransaction> transactions) {
    Set<Long> kidsToLock = new HashSet<>(1);
    for (ITransaction trn : transactions) {
      kidsToLock.add(trn.getKioskId());
      if (ITransaction.TYPE_TRANSFER.equals(trn.getType()) && !kidsToLock
          .contains(trn.getLinkedKioskId())) {
        kidsToLock.add(trn.getLinkedKioskId());
      }
    }
    return kidsToLock;
  }

  private Map<String, List<ITransaction>> getBatchIdFirstTransactionMap(
      List<ITransaction> transactions) {
    // Iterate through transactions and form a map of bid and first transaction for that batch
    Map<String, List<ITransaction>> bidFirstTransactionMap = new HashMap<>();
    transactions.stream()
        .filter(transaction -> !bidFirstTransactionMap.containsKey(transaction.getBatchId()))
        .forEach(transaction -> bidFirstTransactionMap.put(transaction.getBatchId(),
            new ArrayList<>(Arrays.asList(transaction))));
    return bidFirstTransactionMap;
  }

  protected void executeTransactions(Long domainId, PersistenceManager pm,
                                   Map<Long, List<ErrorDetailModel>> materialErrorDetailModelsMap,
                                   Map<Long, List<SuccessDetailModel>> materialSuccessDetailModelsMap,
                                   Map<Long, Integer> midCountMap,
                                   List<ITransaction> validTransactions,
                                   Map<Long, Integer> midFailedFromPositionMap,
                                   InventoryUpdatePolicy inventoryUpdatePolicy) {

    boolean shouldDeduplicate = inventoryUpdatePolicy.shouldDeduplicate();
    // Iterate through every valid transaction
    for (ITransaction transaction : validTransactions) {
      if (midFailedFromPositionMap.containsKey(transaction.getMaterialId())) {
        // Do not process for this material
        continue;
      }
      ITransaction createdTransaction = null;
      try {
            createdTransaction =
            inventoryManagementService.updateInventoryTransaction(domainId, transaction, false,
                !shouldDeduplicate, pm);
        if (createdTransaction != null && createdTransaction.getKeyString() == null) {
          // Error
          xLogger.warn("Error while updating inventory, errorCode: {0}, errorMessage: {1}",
              createdTransaction.getMsgCode(), createdTransaction.getMessage());
          updateFailedFromPositionMap(midFailedFromPositionMap, midCountMap,
              materialErrorDetailModelsMap, transaction.getMaterialId(),
              createdTransaction.getMsgCode(),
              createdTransaction.getMessage());
          continue;
        }
      } catch (DuplicationException e) {
        xLogger.warn(
            "Duplicate inventory transaction for kid: {0}, mid: {1}, bid: {2}",
            transaction.getKioskId(), transaction.getMaterialId(), transaction.getBatchId());
      } catch (ServiceException e) {
        xLogger.severe(
            "Exception while updating inventory transaction for kid: {0}, mid: {1}, bid: {2}",
            transaction.getKioskId(), transaction.getMaterialId(), transaction.getBatchId(), e);
        updateFailedFromPositionMap(midFailedFromPositionMap, midCountMap,
            materialErrorDetailModelsMap, transaction.getMaterialId(), null, null);
        continue;
      }
      if (!transaction.isSystemCreated()) {
        if (midCountMap.containsKey(transaction.getMaterialId())) {
          int rejectUntilPosition = midCountMap.get(transaction.getMaterialId());
          midCountMap.put(transaction.getMaterialId(), rejectUntilPosition + 1);
        } else {
          midCountMap.put(transaction.getMaterialId(), 0);
        }
        if (createdTransaction != null && createdTransaction.getKeyString() != null) {
          updateSuccessFromPositionMap(materialSuccessDetailModelsMap,
              midCountMap.get(transaction.getMaterialId()), transaction.getMaterialId(),
              createdTransaction.getKeyString());
        }
      }
    }
  }

  private void updateSuccessFromPositionMap(
                                           Map<Long, List<SuccessDetailModel>> materialSuccessDetailModelsMap, int position, Long mid,
                                           String transactionKey) {
    if (materialSuccessDetailModelsMap == null) {
      materialSuccessDetailModelsMap = new HashMap<>();
    }
    List<SuccessDetailModel> successDetailModels;
    if (materialSuccessDetailModelsMap.containsKey(mid)) {
      successDetailModels = materialSuccessDetailModelsMap.get(mid);
      successDetailModels.get(0).keys.add(transactionKey);
    } else {
      successDetailModels = new ArrayList<>(1);
      List<String> transactionKeys = new ArrayList<>(1);
      transactionKeys.add(transactionKey);
      successDetailModels.add(new SuccessDetailModel("M015", position, transactionKeys));
    }
    materialSuccessDetailModelsMap.put(mid, successDetailModels);
  }

  protected Map<Long,ResponseDetailModel> getMaterialResponseDetailModel(Map<Long,List<SuccessDetailModel>> midSuccessDetailModelMap, Map<Long,List<ErrorDetailModel>> midErrorDetailModelMap) {
    Map<Long,ResponseDetailModel> midResponseDetailModel = new HashMap<>();
    midSuccessDetailModelMap.entrySet().forEach(entry -> {
          ResponseDetailModel responseDetailModel = new ResponseDetailModel();
          if (midResponseDetailModel.containsKey(entry.getKey())) {
            responseDetailModel = midResponseDetailModel.get(entry.getKey());
          } else {
            responseDetailModel = new ResponseDetailModel();
          }
          responseDetailModel.successDetailModels.addAll(midSuccessDetailModelMap.get(entry.getKey()));
          midResponseDetailModel.put(entry.getKey(),responseDetailModel);
        }
    );
    midErrorDetailModelMap.entrySet().forEach(entry -> {
          ResponseDetailModel responseDetailModel = new ResponseDetailModel();
          if (midResponseDetailModel.containsKey(entry.getKey())) {
            responseDetailModel = midResponseDetailModel.get(entry.getKey());
          } else {
            responseDetailModel = new ResponseDetailModel();
          }
          responseDetailModel.errorDetailModels.addAll(midErrorDetailModelMap.get(entry.getKey()));
          midResponseDetailModel.put(entry.getKey(), responseDetailModel);
        }
    );
    return midResponseDetailModel;
  }
}
