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

/**
 *
 */
package com.logistimo.inventory;

import com.logistimo.AppFactory;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.config.models.OptimizerConfig;
import com.logistimo.constants.CharacterConstants;
import com.logistimo.constants.Constants;
import com.logistimo.context.StaticApplicationContext;
import com.logistimo.entities.entity.IKiosk;
import com.logistimo.entities.entity.IKioskLink;
import com.logistimo.entities.service.EntitiesService;
import com.logistimo.entities.service.EntitiesServiceImpl;
import com.logistimo.exception.LogiException;
import com.logistimo.inventory.entity.IInvntry;
import com.logistimo.inventory.entity.IInvntryBatch;
import com.logistimo.inventory.entity.ITransaction;
import com.logistimo.inventory.service.InventoryManagementService;
import com.logistimo.inventory.service.impl.InventoryManagementServiceImpl;
import com.logistimo.logger.XLog;
import com.logistimo.materials.entity.IMaterial;
import com.logistimo.materials.service.MaterialCatalogService;
import com.logistimo.materials.service.impl.MaterialCatalogServiceImpl;
import com.logistimo.pagination.Results;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;
import com.logistimo.services.cache.MemcacheService;
import com.logistimo.services.utils.ConfigUtil;
import com.logistimo.utils.BigUtil;
import com.logistimo.utils.LocalDateUtil;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Utility for transactions
 *
 * @author Arun
 */
public class TransactionUtil {

  // URLs
  public static final String POSTTRANSCOMMIT_URL = "/task/transcommit";
  /**
   * Duration to put finger print in cache for newer versions. default 3 days
   */
  public static final int
      DEDUPLICATION_DURATION_NEW =
      ConfigUtil.getInt("trans.dedup.duration.seconds", 259_200);
  /**
   * Duration to put finger print in cache for older versions. default 30 minutes
   */
  public static final int
      DEDUPLICATION_DURATION =
      ConfigUtil.getInt("trans.dedup.duration.seconds.old", 1_800);
  public static final int COMPLETED = 0;
  public static final int IN_PROGRESS = 1;

  // seconds
  // Logger
  private static final XLog xLogger = XLog.getLog(TransactionUtil.class);
  // seconds
  // Cache-key Prefix
  private static final String TRANSACTION_CHECKSUM_KEY_PREFIX = "trnschk.";

  private TransactionUtil() {
  }

  /**
   * Check if transaction is in progress.
   * @param saveTime - save time of the request
   * @param userId - user id
   * @param kioskId - store id
   * @param partialId - transaction part id, typically comes via SMS.
   * @return - true if it is already in progress, else
   */
  public static boolean deduplicateBySaveTimePartial(String saveTime, String userId,
                                                     String kioskId, String partialId) {
    // Global checking without partial ID - skip write when partial id is available
    return
        deduplicateBySaveTimePartial(saveTime, userId, kioskId, null,
            partialId != null)
            ||
            (partialId != null && deduplicateBySaveTimePartial(saveTime, userId,
                kioskId,
                partialId, false)); //Check with partial ID
  }

  public static boolean deduplicateBySaveTimePartial(String saveTime, String userId,
                                                     String kioskId, String partialId,
                                                     boolean skipWrite) {
    try {
      MemcacheService cache = AppFactory.get().getMemcacheService();
      
      String
          cacheKey =
          TRANSACTION_CHECKSUM_KEY_PREFIX + userId + CharacterConstants.DOT + kioskId
              + CharacterConstants.DOT + saveTime;
      if (partialId != null) {
        cacheKey += CharacterConstants.DOT + partialId;
      }
      if (cache != null) {
        if (skipWrite) {
          // Get last checksum
          return cache.get(cacheKey) != null;
        } else {
          // Put new checksum back into cache
          return !cache.putIfNotExist(cacheKey, TransactionUtil.IN_PROGRESS, DEDUPLICATION_DURATION_NEW);
        }
      }
    } catch (Exception e) {
      xLogger
          .warn("{0} when de duplicating transactions: {1}", e.getClass().getName(), e.getMessage());
    }
    return false;
  }

  // Get a map from JSON string
  @SuppressWarnings("rawtypes")
  public static Map<String, String> getMap(String jsonString) {
    if (jsonString == null || jsonString.isEmpty()) {
      return null;
    }
    Map<String, String> map = new HashMap<>();
    try {
      JSONObject json = new JSONObject(jsonString);
      Iterator en = json.keys();
      while (en.hasNext()) {
        String key = (String) en.next();
        map.put(key, json.getString(key));
      }
    } catch (JSONException e) {
      xLogger
          .warn("Exception when getting map for JSON string {0}: {1}", jsonString, e.getMessage());
    }
    return map;
  }

  // Get a JSON String from a map
  public static String getString(Map<String, String> map) {
    String str = null;
    if (map == null || map.isEmpty()) {
      return null;
    }
    try {
      JSONObject json = new JSONObject();
      for (String key : map.keySet()) {
        String value;
        if ((value = map.get(key)) != null) {
          json.put(key, value);
        }
      }
      str = json.toString();
    } catch (JSONException e) {
      xLogger.warn("Exception when getting JSON string from map {0}: {1}", map, e.getMessage());
    }
    return str;
  }

  public static boolean isPostTransOptimizationReqd(DomainConfig dc, String transType) {
    if (ITransaction.TYPE_ORDER.equals(transType) || ITransaction.TYPE_REORDER.equals(transType)) {
      return false;
    }
    OptimizerConfig oc = dc.getOptimizerConfig();
    return (oc != null && (oc.getCompute() == OptimizerConfig.COMPUTE_FORECASTEDDEMAND
        || oc.getCompute() == OptimizerConfig.COMPUTE_EOQ));
  }


  // Get transaction list checksum - a MD5 digest
  public static String checksum(Long domainId, List<ITransaction> list) {
    xLogger.fine("Entered checksum: {0}", (list == null ? "NULL" : list.size()));
    if (list == null || list.isEmpty()) {
      return null;
    }
    Iterator<ITransaction> it = list.iterator();
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      while (it.hasNext()) {
        ITransaction trans = it.next();
        if (trans.getDomainId() == null) {
          trans.setDomainId(domainId);
        }
        byte[] fingerprint = trans.fingerprint();
        if (fingerprint != null) {
          md.update(fingerprint);
        }
      }
      return new String(Hex.encodeHex(md.digest()));
    } catch (Exception e) {
      xLogger.warn("{0} when getting checksum: {1}", e.getClass().getName(), e.getMessage());
      return null;
    }
  }

  // De-duplicate, and if duplicates are found, returns true; otherwise, false
  public static boolean deduplicate(Long domainId, List<ITransaction> list) {
    if (list == null || list.isEmpty()) {
      return false;
    }
    // Get the checksum of the list
    String checkSum = checksum(domainId, list);
    if (checkSum == null) {
      return false;
    }
    // Check if check sum exists for this entity in cache
    try {
      ITransaction trans = list.get(0);
      Date timestamp = trans.getTimestamp();
      if (timestamp == null) {
        timestamp = new Date();
      }
      MemcacheService cache = AppFactory.get().getMemcacheService();
      String
          cacheKey =
          TRANSACTION_CHECKSUM_KEY_PREFIX + trans.getKioskId() + "." + trans.getSourceUserId()+"."+checkSum;
      if (cache != null) {
        return cache.putIfNotExist(cacheKey, new Checksum(checkSum, timestamp), DEDUPLICATION_DURATION);
      }
    } catch (Exception e) {
      xLogger
          .warn("{0} when deduplicating transactions: {1}", e.getClass().getName(), e.getMessage());
    }
    return false;
  }

  // Set batch data in a transaction
  public static void setBatchData(ITransaction trans, String batchIdStr, String batchExpiryStr,
                                  String batchManufacturer, String batchManufacturedDateStr) {
    if (batchIdStr != null && !batchIdStr.isEmpty()) {
      trans.setBatchId(batchIdStr);
    }
    if (batchExpiryStr != null && !batchExpiryStr.isEmpty()) {
      try {
        Date batchExpiry = LocalDateUtil.parseCustom(batchExpiryStr, Constants.DATE_FORMAT, null);
        trans.setBatchExpiry(batchExpiry);
      } catch (Exception e) {
        xLogger.severe(
            "{0} when parsing batch expiry date {1} for transaction {2} for {3}-{4} in domain {5}: {6}",
            e.getClass().getName(), batchExpiryStr, trans.getType(), trans.getKioskId(),
            trans.getMaterialId(), trans.getDomainId(), e.getMessage());
      }
    }
    if (batchManufacturer != null && !batchManufacturer.isEmpty()) {
      trans.setBatchManufacturer(batchManufacturer);
    }
    if (batchManufacturedDateStr != null && !batchManufacturedDateStr.isEmpty()) {
      try {
        Date
            batchManufacturedDate =
            LocalDateUtil.parseCustom(batchManufacturedDateStr, Constants.DATE_FORMAT, null);
        trans.setBatchManufacturedDate(batchManufacturedDate);
      } catch (Exception e) {
        xLogger.severe(
            "{0} when parsing batch manufacturer date {1} for transaction {2} for {3}-{4} in domain {5}: {6}",
            e.getClass().getName(), batchManufacturedDateStr, trans.getType(), trans.getKioskId(),
            trans.getMaterialId(), trans.getDomainId(), e.getMessage());
      }
    }
  }

  // Method that returns a default vendor Id for an order.
  public static Long getDefaultVendor(Long domainId, Long kioskId, Long vendorId)
      throws ServiceException {
    xLogger.fine("Entering getDefaultVendor, domainId: {0}, kioskId: {1}, vendorId: {2}", domainId,
        kioskId, vendorId);
    // Get the service
    EntitiesService as = StaticApplicationContext.getBean(EntitiesServiceImpl.class);
    if (kioskId == null) {
      return null;
    }
    if (domainId == null) {
      domainId = as.getKiosk(kioskId, false).getDomainId();
    }
    DomainConfig dc = DomainConfig.getInstance(domainId);
    // If vendorId is null, if the kiosk has only one vendor, return that vendor Id as default.
    // If the kiosk has more than one vendors, return the default vendor in the config as vendorId
    if (vendorId == null) {
      // Get the vendor kiosk links
      Results results = as.getKioskLinks(kioskId, IKioskLink.TYPE_VENDOR, null, null, null);
      List<IKioskLink> links = results.getResults();
      if (links != null && !links.isEmpty()) {
        if (links.size() == 1) {
          return links.get(0).getLinkedKioskId();
        } else {
          return dc.getVendorId();
        }
      } else {
        return dc.getVendorId();
      }
    }
    xLogger.fine("Exiting getDefaultVendor, vendorId: {0}", vendorId);
    return vendorId;
  }

  public static String getDisplayName(String transType, Locale locale) {
    return getDisplayName(transType, DomainConfig.TRANSNAMING_DEFAULT, locale);
  }

  public static String getDisplayName(String transType, String transNaming, Locale locale) {
    String name = "";
    ResourceBundle messages = Resources.getBundle(locale);
    if (messages == null) {
      return "";
    }
    if (ITransaction.TYPE_ISSUE.equals(transType)) {
      name =
          DomainConfig.TRANSNAMING_ISSUESRECEIPTS.equals(transNaming) ? messages
              .getString("transactions.issue") : messages.getString("transactions.sale");
    } else if (ITransaction.TYPE_RECEIPT.equals(transType)) {
      name =
          DomainConfig.TRANSNAMING_ISSUESRECEIPTS.equals(transNaming) ? messages
              .getString("transactions.receipt") : messages.getString("transactions.purchase");
    } else if (ITransaction.TYPE_PHYSICALCOUNT.equals(transType)) {
      name = messages.getString("transactions.stockcount");
    } else if (ITransaction.TYPE_ORDER.equals(transType)) {
      name = messages.getString("transactions.order");
    } else if (ITransaction.TYPE_REORDER.equals(transType)) {
      name = messages.getString("transactions.reorder");
    } else if (ITransaction.TYPE_WASTAGE.equals(transType)) {
      name = messages.getString("transactions.wastage");
    } else if (ITransaction.TYPE_TRANSFER.equals(transType)) {
      name = messages.getString("transactions.transfer");
    } else if (ITransaction.TYPE_RETURNS_INCOMING.equals(transType)) {
      name = messages.getString("transactions.return.incoming");
    } else if (ITransaction.TYPE_RETURNS_OUTGOING.equals(transType)) {
      name = messages.getString("transactions.return.outgoing");
    }
    return name;
  }

  public static void setObjectInCache(String timestampSendMillis, String userId, Long kioskId,
                                      String partialId,
                                      Integer status) {
    try {
      MemcacheService cache = AppFactory.get().getMemcacheService();
      String cacheKey = createKey(timestampSendMillis, userId, kioskId, partialId);
      if (cache != null) {
        // Get last checksum
        cache
            .put(cacheKey, status, TransactionUtil.DEDUPLICATION_DURATION_NEW);
      }
    } catch (Exception e) {
      xLogger
          .warn(
              "Exception when clearing cache. userId: {0}, timestampSendMillis: {1}, partialId: {2}",
              userId, timestampSendMillis, partialId, e);
    }
  }

  public static Integer getObjectFromCache(String timestampSendMillis,
                                                               String userId, Long kioskId,
                                                               String partialId) {
    try {
      MemcacheService cache = AppFactory.get().getMemcacheService();
      if (cache != null) {
        String cacheKey = createKey(timestampSendMillis, userId, kioskId, partialId);
        Object value = cache.get(cacheKey);
        if (value == null) {
          cacheKey = createKey(timestampSendMillis, userId, kioskId, null);
          value = cache.get(cacheKey);
        }
          return  (Integer)value;
      }
    } catch (Exception e) {
      xLogger
          .warn(
              "Exception when getting status from cache. userId: {0}, timestampSendMillis: {1}, partialId: {2}",
              userId, timestampSendMillis, partialId, e);
    }
    return null;
  }

  public static String createKey(String timestampSendMillis, String userId, Long kioskId,
                                 String partialId) {
    String
        cacheKey =
        TRANSACTION_CHECKSUM_KEY_PREFIX + userId + CharacterConstants.DOT + (kioskId != null ?
            kioskId + CharacterConstants.DOT : CharacterConstants.EMPTY) + timestampSendMillis;
    if (partialId != null) {
      cacheKey += CharacterConstants.DOT + partialId;
    }
    return cacheKey;
  }

  /**
   * Validates the transactions and filters out the transactions for which data is invalid.
   * @param transactions
   * @return -1 if all transactions are valid, 0 if all transactions are invalid, the index of the invalid transaction otherwise
   */
  public static int filterInvalidTransactions(List<ITransaction> transactions) {
    if (transactions == null || transactions.isEmpty()) {
      return 0;
    }
    try {
      EntitiesService es = StaticApplicationContext.getBean(EntitiesServiceImpl.class);
      MaterialCatalogService mcs = StaticApplicationContext.getBean(
          MaterialCatalogServiceImpl.class);
      InventoryManagementService ims = StaticApplicationContext.getBean(
          InventoryManagementServiceImpl.class);
      Long kid = transactions.get(0).getKioskId();
      Long mid = transactions.get(0).getMaterialId();
      IKiosk k = es.getKiosk(kid, false);
      Long domainId = k.getDomainId();
      IMaterial m = mcs.getMaterial(mid);
      int index = 0;
      for (ITransaction trans : transactions) {
        // Validate data
        try {
          try {
            IInvntry inv = ims.getInventory(kid, trans.getMaterialId());
            if (inv == null) {
              throw new ServiceException("Inventory does not exist");
            }
          } catch (ServiceException e) {
            xLogger.warn("Inventory does not exist for kid {0}, mid {1} at index {2}",
                trans.getKioskId(), trans.getMaterialId(), index, e);
            throw new LogiException("M010", (Object[]) null);
          }
          boolean hasBatch = trans.hasBatch();
          boolean isBatchEnabled = k.isBatchMgmtEnabled() && m.isBatchEnabled();
          if (isBatchEnabled && !hasBatch) {
            xLogger.warn(
                "Batch information is not present although kid {0} and mid id {1} are batch enabled at index {2}",
                kid, trans.getMaterialId(), index);
            throw new LogiException("M010", (Object[]) null);
          }
          if (!isBatchEnabled && hasBatch) {
            xLogger.warn(
                "Batch information is present although kid {0} and mid {1} are both not batch enabled at index {2}",
                kid, trans.getMaterialId(), index);
            throw new LogiException("M010", (Object[]) null);
          }
          String ty = trans.getType();
          boolean isTransTypeReturn = ITransaction.TYPE_RETURNS_INCOMING.equals(ty) || ITransaction.TYPE_RETURNS_OUTGOING.equals(ty);
          if (StringUtils.isEmpty(ty) || !(ITransaction.TYPE_ISSUE.equals(ty)
              || ITransaction.TYPE_RECEIPT.equals(ty) || ITransaction.TYPE_PHYSICALCOUNT.equals(ty)
              || ITransaction.TYPE_WASTAGE.equals(ty) || ITransaction.TYPE_TRANSFER.equals(ty)
              || ITransaction.TYPE_RETURNS_INCOMING.equals(ty) || ITransaction.TYPE_RETURNS_OUTGOING.equals(ty)))  {
            xLogger.warn("Invalid or missing transaction type at index {0}", index);
            throw new LogiException("M010", (Object[]) null);
          }
          if (ITransaction.TYPE_TRANSFER.equals(ty) && trans.getLinkedKioskId() == null) {
            xLogger.warn("Missing lkid for transfer at index {0}", index);
            throw new LogiException("M010", (Object[]) null);
          }
          if (ITransaction.TYPE_TRANSFER.equals(ty)) {
            try {
              IKiosk lk = es.getKiosk(trans.getLinkedKioskId(), false);
              if (!k.isBatchMgmtEnabled() && lk != null && lk.isBatchMgmtEnabled() && m
                  .isBatchEnabled()) {
                xLogger.warn(
                    "Cannot transfer batch enabled material from batch disabled entity {0} to batch enabled entity {1} in transaction at index {2}",
                    trans.getKioskId(), trans.getLinkedKioskId(), index);
                throw new LogiException("M010", (Object[]) null);
              }
            } catch (ServiceException e) {
              xLogger.warn("Linked kiosk with lkid {0} does not exist at index {1}",
                  trans.getLinkedKioskId(), index);
              throw new LogiException("M010", (Object[]) null);
            }
          }
          if ((ITransaction.TYPE_ISSUE.equals(ty) || ITransaction.TYPE_RETURNS_INCOMING.equals(ty)) && trans.getLinkedKioskId() != null && !es
              .hasKioskLink(kid, IKioskLink.TYPE_CUSTOMER, trans.getLinkedKioskId())) {
            xLogger.warn(
                "Linked entity specified by lkid {0} is not a customer of entity id {1} at index {2}",
                trans.getLinkedKioskId(), kid, index);
            throw new LogiException("M010", (Object[]) null);
          }
          if ((ITransaction.TYPE_RECEIPT.equals(ty) || ITransaction.TYPE_RETURNS_OUTGOING.equals(ty)) && trans.getLinkedKioskId() != null && !es
              .hasKioskLink(kid, IKioskLink.TYPE_VENDOR, trans.getLinkedKioskId())) {
            xLogger.warn(
                "Linked entity specified by lkid {0} is not a vendor of entity id {1} at index {2}",
                trans.getLinkedKioskId(), kid, index);
            throw new LogiException("M010", (Object[]) null);
          }
          if ((ITransaction.TYPE_PHYSICALCOUNT.equals(trans.getType()) || ITransaction.TYPE_WASTAGE
              .equals(trans.getType())) && (trans.getLinkedKioskId() != null)) {
            xLogger.warn(
                "Linked entity is specified even when it is not required for transaction type {0} at index {1}",
                trans.getType(), index);
            throw new LogiException("M010", (Object[]) null);
          }
          if (isTransTypeReturn && !isTrackingDetailsPresent(trans)) {
              xLogger.warn(
                  "Tracking ID, Local tracking ID or Tracking object type is not specified for transaction type {0} at index {1}",
                  trans.getType(), index);
              throw new LogiException("M010", (Object[]) null);
          }
          if (!isTransTypeReturn && (StringUtils.isNotEmpty(trans.getTrackingObjectType()) || StringUtils.isNotEmpty(trans.getTrackingId()))) {
            xLogger.warn(
                "Tracking id and/or tracking object type is specified even when it is not required for transaction type {0} at index {1}",
                trans.getType(), index);
            trans.setTrackingId(null);
            trans.setTrackingObjectType(null);
          }
          if (!ITransaction.TYPE_PHYSICALCOUNT.equals(trans.getType()) && BigUtil.equals(
              trans.getQuantity(), BigDecimal.ZERO)) {
            xLogger.warn("Missing or invalid quantity at index {0}", index);
            throw new LogiException("M010", (Object[]) null);
          }
          if (!hasBatch && trans.getOpeningStock(true) == null) {
            xLogger.warn("Missing or invalid opening stock at index {0}", index);
            throw new LogiException("M010", (Object[]) null);
          }
          if (hasBatch && trans.getOpeningStockByBatch(true) == null) {
            xLogger.warn("Missing or invalid opening stock by batch at index {0}", index);
            throw new LogiException("M010", (Object[]) null);
          }
          if (trans.getEntryTime() == null) {
            xLogger.warn("Missing or invalid entry time at index {0}", index);
            throw new LogiException("M010", (Object[]) null);
          }
          if (hasBatch && (trans.getBatchExpiry() == null
              || trans.getBatchManufacturer() == null)) {
            Results
                results =
                ims.getValidBatchesByBatchId(trans.getBatchId(), trans.getMaterialId(),
                    trans.getKioskId(), domainId, false, null);
            if (results.getResults() == null || results.getResults().isEmpty()) {
              xLogger.warn("Missing or invalid batch metadata at index {0}", index);
              throw new LogiException("M010", (Object[]) null);
            }
            IInvntryBatch invBatch = (IInvntryBatch) results.getResults().get(0);
            trans.setBatchExpiry(invBatch.getBatchExpiry());
            trans.setBatchManufacturer(invBatch.getBatchManufacturer());
            trans.setBatchManufacturedDate(invBatch.getBatchManufacturedDate());
          }
        } catch (LogiException e) {
          xLogger.warn("Exception while validating transactions for material {0}",
              trans.getMaterialId(), e);
          // Remove rejected transactions from the list transactions
          transactions.subList(index, transactions.size()).clear();
          // Return the index from which the transactions got rejected.
          return index;
        }
        index++;
      }
    } catch (ServiceException e) {
      xLogger.warn("Exception while validating transactions", e);
      // Reject all transactions
      transactions.clear();
      return 0;
    }
    return -1;
  }

  /**
   * Check if tracking object type and either tracking id or local tracking ID are present
   * @param transaction
   * @return - true or false
   */
  protected static boolean isTrackingDetailsPresent(ITransaction transaction) {
    return StringUtils.isNotEmpty(transaction.getTrackingObjectType()) && (StringUtils.isNotEmpty(
        transaction.getTrackingId()) || StringUtils.isNotEmpty(transaction.getLocalTrackingID()));
  }

  public static class Checksum implements Serializable {
    private static final long serialVersionUID = 1L;

    String c = null;
    Date t = null;

    public Checksum(String checksum, Date date) {
      c = checksum;
      t = date;
    }
  }
}
