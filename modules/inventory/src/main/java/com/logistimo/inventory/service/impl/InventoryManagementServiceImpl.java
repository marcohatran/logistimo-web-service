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

/**
 *
 */
package com.logistimo.inventory.service.impl;

import com.logistimo.AppFactory;
import com.logistimo.auth.service.AuthorizationService;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.config.models.ConfigurationException;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.config.models.InventoryConfig;
import com.logistimo.config.models.ReturnsConfig;
import com.logistimo.constants.CharacterConstants;
import com.logistimo.constants.Constants;
import com.logistimo.constants.QueryConstants;
import com.logistimo.constants.SourceConstants;
import com.logistimo.dao.JDOUtils;
import com.logistimo.domains.utils.DomainsUtil;
import com.logistimo.domains.utils.EntityRemover;
import com.logistimo.entities.entity.IKiosk;
import com.logistimo.entities.models.LocationSuggestionModel;
import com.logistimo.entities.service.EntitiesService;
import com.logistimo.events.EventConstants;
import com.logistimo.events.entity.IEvent;
import com.logistimo.events.exceptions.EventGenerationException;
import com.logistimo.events.processor.EventPublisher;
import com.logistimo.exception.InvalidServiceException;
import com.logistimo.exception.LogiException;
import com.logistimo.exception.TaskSchedulingException;
import com.logistimo.exception.ValidationException;
import com.logistimo.inventory.TransactionUtil;
import com.logistimo.inventory.actions.UpdateMultipleInventoryTransactionsAction;
import com.logistimo.inventory.dao.IInvntryDao;
import com.logistimo.inventory.dao.ITransDao;
import com.logistimo.inventory.entity.IInvAllocation;
import com.logistimo.inventory.entity.IInventoryMinMaxLog;
import com.logistimo.inventory.entity.IInvntry;
import com.logistimo.inventory.entity.IInvntryBatch;
import com.logistimo.inventory.entity.IInvntryEvntLog;
import com.logistimo.inventory.entity.IInvntryLog;
import com.logistimo.inventory.entity.ITransaction;
import com.logistimo.inventory.exceptions.InventoryAllocationException;
import com.logistimo.inventory.models.CreateTransactionsReturnModel;
import com.logistimo.inventory.models.InventoryFilters;
import com.logistimo.inventory.models.ResponseDetailModel;
import com.logistimo.inventory.service.InventoryManagementService;
import com.logistimo.logger.XLog;
import com.logistimo.materials.entity.IHandlingUnit;
import com.logistimo.materials.entity.IMaterial;
import com.logistimo.materials.service.IHandlingUnitService;
import com.logistimo.materials.service.MaterialCatalogService;
import com.logistimo.models.shipments.ShipmentItemBatchModel;
import com.logistimo.pagination.PageParams;
import com.logistimo.pagination.QueryParams;
import com.logistimo.pagination.Results;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.DuplicationException;
import com.logistimo.services.ObjectNotFoundException;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;
import com.logistimo.services.UnauthorizedRequestException;
import com.logistimo.services.impl.PMF;
import com.logistimo.services.taskqueue.ITaskService;
import com.logistimo.services.utils.ConfigUtil;
import com.logistimo.tags.TagUtil;
import com.logistimo.tags.dao.ITagDao;
import com.logistimo.tags.entity.ITag;
import com.logistimo.utils.BigUtil;
import com.logistimo.utils.LocalDateUtil;
import com.logistimo.utils.LockUtil;
import com.logistimo.utils.QueryUtil;
import com.logistimo.utils.StringUtil;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;



/**
 * @author arun, juhee
 */
@Service
public class InventoryManagementServiceImpl implements InventoryManagementService {

  // Logger
  private static final XLog xLogger = XLog.getLog(InventoryManagementServiceImpl.class);
  private static final String UPDATE_PREDICTION_TASK = "/s2/api/inventory/task/prediction";
  private static final String
      UPDATE_ENTITYACTIVITYTIMESTAMPS_TASK =
      "/s2/api/entities/task/updateentityactivitytimestamps";
  private static final int LOCK_RETRY_COUNT = 25;
  private static final int LOCK_RETRY_DELAY_IN_MILLISECONDS = 2400;
  public static final String BACKEND_MESSAGES = "BackendMessages";
  private static Set<String> vldTransTypes = new HashSet<>(Arrays.asList(ITransaction.TYPE_ISSUE,
      ITransaction.TYPE_RECEIPT, ITransaction.TYPE_PHYSICALCOUNT, ITransaction.TYPE_TRANSFER,
      ITransaction.TYPE_WASTAGE, ITransaction.TYPE_RETURNS_INCOMING, ITransaction.TYPE_RETURNS_OUTGOING));

  private static final Map<String,Integer> eventIdByTransType = Collections.unmodifiableMap(new HashMap<String,Integer>() {
    {
      put(ITransaction.TYPE_ISSUE, IEvent.STOCK_ISSUED);
      put(ITransaction.TYPE_RECEIPT, IEvent.STOCK_RECEIVED);
      put(ITransaction.TYPE_PHYSICALCOUNT, IEvent.STOCK_COUNTED);
      put(ITransaction.TYPE_WASTAGE, IEvent.STOCK_WASTED);
      put(ITransaction.TYPE_TRANSFER, IEvent.STOCK_TRANSFERRED);
      put(ITransaction.TYPE_RETURNS_INCOMING, IEvent.INCOMING_RETURN_ENTERED);
      put(ITransaction.TYPE_RETURNS_OUTGOING, IEvent.OUTGOING_RETURN_ENTERED);
    }
  });

  private ITagDao tagDao;
  private IInvntryDao invntryDao;
  private ITransDao transDao;

  private MaterialCatalogService materialCatalogService;
  private EntitiesService entitiesService;
  private IHandlingUnitService handlingUnitService;
  private UpdateMultipleInventoryTransactionsAction updateMultipleInventoryTransactionsAction;
  private AuthorizationService authorizationService;

  @Autowired
  public void setTagDao(ITagDao tagDao) {
    this.tagDao = tagDao;
  }

  @Autowired
  public void setInvntryDao(IInvntryDao invntryDao) {
    this.invntryDao = invntryDao;
  }

  @Autowired
  public void setTransDao(ITransDao transDao) {
    this.transDao = transDao;
  }

  @Autowired
  public void setMaterialCatalogService(MaterialCatalogService materialCatalogService) {
    this.materialCatalogService = materialCatalogService;
  }

  @Autowired
  public void setEntitiesService(EntitiesService entitiesService) {
    this.entitiesService = entitiesService;
  }

  @Autowired
  public void setHandlingUnitService(IHandlingUnitService handlingUnitService) {
    this.handlingUnitService = handlingUnitService;
  }

  @Autowired
  public void setUpdateMultipleInventoryTransactionsAction(
      UpdateMultipleInventoryTransactionsAction updateMultipleInventoryTransactionsAction) {
    this.updateMultipleInventoryTransactionsAction = updateMultipleInventoryTransactionsAction;
  }

  @Autowired
  public void setAuthorizationService(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  private static ITaskService getTaskService() {
    return AppFactory.get().getTaskService();
  }

  // Post inv.-transaction-commit hook
  private static void doPostTransactionCommitHook(List<ITransaction> transList,
                                                  List<IInvntry> toBeOptimized) {
    xLogger.fine("Entered doPostTransactionCommitHook");
    if (transList == null || transList.isEmpty()) {
      return;
    }
    updateEntityActivityTimestamps(transList);

    ITransaction trans = transList.get(0);
    Long domainId = trans.getDomainId();
    String type = trans.getType();
    // Check if notification is to done post transaction commit
    DomainConfig dc = DomainConfig.getInstance(domainId);
    boolean
        optimize =
        !toBeOptimized.isEmpty() && TransactionUtil.isPostTransOptimizationReqd(dc, type);
    if (!optimize) // earlier: !notify && !optimize
    {
      return; // do nothing
    }
    String transType = trans.getType();
    // Form the parameters
    Map<String, String> params = new HashMap<>();
    params.put("action", "transcommit");
    params.put("domainid", domainId.toString());
    params.put("transtype", transType);
    // Add optimization parameters
    String invIdsCSV = StringUtil
        .getCSV(toBeOptimized.stream().map(IInvntry::getKeyString).collect(Collectors.toList()));
    params.put("inventoryids", invIdsCSV);

    xLogger
        .info("SCHEDULING: url = {0}, params = {1}", TransactionUtil.POSTTRANSCOMMIT_URL, params);
    try {
      getTaskService()
          .schedule(ITaskService.QUEUE_OPTIMZER, TransactionUtil.POSTTRANSCOMMIT_URL, params, null,
              ITaskService.METHOD_POST, domainId, trans.getSourceUserId(), "POST_TRANSACTION");
    } catch (Exception e) {
      xLogger.severe(
          "{0} when scheduling task to notify inventroy update by user {1} in domain {2}: {3}",
          e.getClass().getName(), trans.getSourceUserId(), trans.getDomainId(), e.getMessage());
    }
    xLogger.fine("Exiting doPostTransactionCommitHook");
  }

  private static void updateEntityActivityTimestamps(List<ITransaction> transList) {
    if (transList == null || transList.isEmpty()) {
      return;
    }
    Map<Long, Long> kidTransTimeMap = getUniqueEntityIdTimestampMap(transList);
    if (!kidTransTimeMap.isEmpty()) {
      Map<String, String> params = new HashMap<>(3);
      Set<Long> kids = kidTransTimeMap.keySet();
      for (Long kid : kids) {
        try {
          params.put("entityId", String.valueOf(kid));
          params.put("timestamp", String.valueOf(kidTransTimeMap.get(kid)));
          params.put("actType", String.valueOf(IKiosk.TYPE_INVENTORYACTIVITY));
          getTaskService()
              .schedule(ITaskService.QUEUE_DEFAULT, UPDATE_ENTITYACTIVITYTIMESTAMPS_TASK, params,
                  ITaskService.METHOD_POST);
        } catch (TaskSchedulingException e) {
          xLogger
              .warn("Error while scheduling update entity activity timestamp for entityId {0}", kid,
                  e);
        }
      }
    }
  }

  private static Map<Long, Long> getUniqueEntityIdTimestampMap(
      List<ITransaction> committedTransList) {
    Map<Long, Long> kidTransTimeMap = new HashMap<>(1);
    for (ITransaction iTrans : committedTransList) {
      String type = iTrans.getType();
      if (ITransaction.TYPE_PHYSICALCOUNT.equals(type) || ITransaction.TYPE_RECEIPT.equals(type)
          || ITransaction.TYPE_TRANSFER.equals(type)) {
        Long kid = iTrans.getKioskId();
        // Form a list of unique entity ids
        if (!kidTransTimeMap.containsKey(kid)) {
          kidTransTimeMap.put(kid, iTrans.getTimestamp().getTime());
        }
        if (ITransaction.TYPE_TRANSFER.equals(type)) {
          Long lkid = iTrans.getLinkedKioskId();
          if (!kidTransTimeMap.containsKey(lkid)) {
            kidTransTimeMap.put(lkid, iTrans.getTimestamp().getTime());
          }
        }
      }
    }
    return kidTransTimeMap;
  }

  public IInvntry getInventory(Long kioskId, Long materialId) throws ServiceException {
    if (kioskId == null || materialId == null) {
      throw new IllegalArgumentException("Invalid kiosk ID or material ID");
    }
    IInvntry inv = null;
    PersistenceManager pm = PMF.get().getPersistenceManager();
    try {
      inv = getInventory(kioskId, materialId, pm);
      inv = pm.detachCopy(inv);
    } finally {
      pm.close();
    }
    return inv;
  }

  @Override
  public IInvntry getInvntryByShortID(Long kioskId, Long shortId) throws ServiceException {
    if (kioskId == null || shortId == null) {
      throw new IllegalArgumentException("Invalid kiosk ID or shortId ID");
    }
    IInvntry inv = null;
    PersistenceManager pm = PMF.get().getPersistenceManager();
    try {
      inv = invntryDao.findShortId(kioskId, shortId, pm);
      inv = pm.detachCopy(inv);
    } finally {
      pm.close();
    }
    return inv;
  }


  public Results getInventoryByKiosk(Long kioskId, PageParams pageParams) throws ServiceException {
    return getInventoryByKiosk(kioskId, null, pageParams);
  }

  public Results getInventoryByKiosk(Long kioskId, String materialTag, PageParams pageParams)
      throws ServiceException {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    Results results = null;
    try {
      results = getInventoryByIds(kioskId, null, null, materialTag, null, pageParams, pm);
    } finally {
      pm.close();
    }
    return results;
  }

  @Override
  public Results searchKioskInventory(Long kioskId, String materialTag, String materialQueryText,
                                      PageParams params, String materialQueryType,
                                      boolean includeMaterialDescriptionQuery)
      throws ServiceException {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    Results results = null;
    try {
      results =
          invntryDao.getInventory(
              new InventoryFilters().withKioskId(kioskId)
                  .withMaterialTags(materialTag)
                  .withMaterialQueryType(materialQueryType)
                  .withMaterialQueryText(materialQueryText)
                  .withMaterialDescriptionQuery(includeMaterialDescriptionQuery)
              , params, pm);
    } finally {
      pm.close();
    }
    return results;
  }

  public Results getInventoryByMaterial(Long materialId, String kioskTag, List<Long> kioskIds,
                                        PageParams params) throws ServiceException {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    Results results = null;
    try {
      results = getInventoryByIds(null, materialId, kioskTag, null, kioskIds, params, pm);
    } finally {
      pm.close();
    }
    return results;
  }

  public Results getInventoryByMaterialDomain(Long materialId, String kioskTag, List<Long> kioskIds,
                                              PageParams params, Long domainId)
      throws ServiceException {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    Results results = null;
    try {
      results =
          invntryDao.getInventory(
              new InventoryFilters().withMaterialId(materialId)
                  .withKioskTags(kioskTag)
                  .withKioskIds(kioskIds)
                  .withDomainId(domainId), params, pm);
    } finally {
      pm.close();
    }
    return results;
  }

  @SuppressWarnings("unchecked")
  public Results getInventoryByBatchId(Long materialId, String batchId, PageParams pageParams,
                                       Long domainId, String kioskTags, String excludedKioskTags,
                                       LocationSuggestionModel location)
      throws ServiceException {
    xLogger.fine("Entered getInventoryByBatch");
    if (materialId == null || batchId == null || batchId.isEmpty()) {
      throw new IllegalArgumentException(
          "Material ID and batch ID are mandatory and either or both not specified.");
    }
    // Get the query
    Map<String, Object> params = new HashMap<>();
    StringBuilder locSubQuery = new StringBuilder();
    String kioskVariableDeclaration = CharacterConstants.EMPTY;
    StringBuilder locationParamDeclaration = new StringBuilder();
    if (location != null && location.isNotEmpty()) {
      kioskVariableDeclaration =
          " VARIABLES " + JDOUtils.getImplClass(IKiosk.class).getName() + " kiosk";
      locSubQuery.append(" && kId == kiosk.kioskId");
      if (StringUtils.isNotEmpty(location.state)) {
        locSubQuery.append(" && kiosk.state == stateParam");
        params.put("stateParam", location.state);
        locationParamDeclaration.append(", String stateParam");
      }
      if (StringUtils.isNotEmpty(location.district)) {
        locSubQuery.append(" && kiosk.district == districtParam");
        params.put("districtParam", location.district);
        locationParamDeclaration.append(", String districtParam");
      }
      if (StringUtils.isNotEmpty(location.taluk)) {
        locSubQuery.append(" && kiosk.taluk == talukParam");
        params.put("talukParam", location.taluk);
        locationParamDeclaration.append(", String talukParam");
      }
    }
    StringBuilder queryStr = new StringBuilder(
        "SELECT FROM "
            + JDOUtils.getImplClass(IInvntryBatch.class).getName()
            + " WHERE dId.contains(dIdParam) && mId == mIdParam && bid == bidParam && "
            + "vld == vldParam && bexp >= bexpParam"
            + locSubQuery.toString());
    String
        declaration =
        " PARAMETERS Long dIdParam, Long mIdParam, String bidParam, Boolean vldParam, Date bexpParam"
            + locationParamDeclaration.toString();
    String ordering = " ORDER BY bexp ASC";
    params.put("mIdParam", materialId);
    params.put("bidParam", batchId);
    params.put("vldParam", Boolean.TRUE);
    params.put("dIdParam", domainId);
    if (StringUtils.isNotEmpty(kioskTags) || StringUtils.isNotEmpty(excludedKioskTags)) {
      boolean isExcluded = StringUtils.isNotEmpty(excludedKioskTags);
      String value = isExcluded ? excludedKioskTags : kioskTags;
      List<String> tags = StringUtil.getList(value, true);
      queryStr.append(QueryConstants.AND).append(CharacterConstants.O_BRACKET);
      int i = 0;
      for (String iTag : tags) {
        String tagParam = "kTgsParam" + (++i);
        if (i != 1) {
          queryStr.append(isExcluded ? QueryConstants.AND : QueryConstants.OR)
              .append(CharacterConstants.SPACE);
        }
        queryStr.append(isExcluded ? QueryConstants.NEGATION
            : CharacterConstants.EMPTY).append("ktgs").append(QueryConstants.DOT_CONTAINS)
            .append(tagParam).append(CharacterConstants.C_BRACKET).append(CharacterConstants.SPACE);
        declaration += ", Long " + tagParam;
        params.put(tagParam, tagDao.getTagFilter(iTag, ITag.KIOSK_TAG));
      }
      queryStr.append(CharacterConstants.C_BRACKET);
    }
    declaration += " import java.util.Date;";
    Calendar cal = LocalDateUtil.getZeroTime(DomainConfig.getInstance(domainId).getTimezone());
    params.put("bexpParam", cal.getTime());
    queryStr.append(kioskVariableDeclaration).append(declaration).append(ordering);
    PersistenceManager pm = PMF.get().getPersistenceManager();
    Query q = pm.newQuery(queryStr.toString());
    if (pageParams != null) {
      QueryUtil.setPageParams(q, pageParams);
    }
    // Execute
    List<IInvntryBatch> results = null;
    String cursor = null;
    try {
      results = (List<IInvntryBatch>) q.executeWithMap(params);
      if (results != null) {
        results.size();
        cursor = QueryUtil.getCursor(results);
        results = (List<IInvntryBatch>) pm.detachCopyAll(results);
      }
    } finally {
      try {
        q.closeAll();
      } catch (Exception ignored) {
        xLogger.warn("Exception while closing query", ignored);
      }
      pm.close();
    }
    xLogger.fine("Exiting getInventoryByBatch");
    return new Results(results, cursor);
  }

  // Get inv. batches that are expiring within a specified time
  @SuppressWarnings("unchecked")
  public Results getInventoryByBatchExpiry(Long domainId, Long materialId, Date start, Date end,
                                           String kioskTags, String excludedKioskTags,
                                           String materialTag,
                                           LocationSuggestionModel location, PageParams pageParams)
      throws ServiceException {
    xLogger.fine("Entered getInventoryByBatchExpiry");
    if (domainId == null && materialId == null) {
      throw new IllegalArgumentException(
          "Neither domain ID nor material ID provided. At least one of them must be available.");
    }
    if (start == null && end == null) {
      throw new IllegalArgumentException(
          "Start or end dates are mandatory, and neither or some were not specified.");
    }
    // Get query
    Map<String, Object> params = new HashMap<>();
    StringBuilder locSubQuery = new StringBuilder();
    String kioskVariableDeclaration = CharacterConstants.EMPTY;
    StringBuilder locationParamDeclaration = new StringBuilder();
    if (location != null && location.isNotEmpty()) {
      kioskVariableDeclaration =
          " VARIABLES " + JDOUtils.getImplClass(IKiosk.class).getName() + " kiosk";
      locSubQuery.append(" && kId == kiosk.kioskId");
      if (StringUtils.isNotEmpty(location.state)) {
        locSubQuery.append(" && kiosk.state == stateParam");
        params.put("stateParam", location.state);
        locationParamDeclaration.append(", String stateParam");
      }
      if (StringUtils.isNotEmpty(location.district)) {
        locSubQuery.append(" && kiosk.district == districtParam");
        params.put("districtParam", location.district);
        locationParamDeclaration.append(", String districtParam");
      }
      if (StringUtils.isNotEmpty(location.taluk)) {
        locSubQuery.append(" && kiosk.taluk == talukParam");
        params.put("talukParam", location.taluk);
        locationParamDeclaration.append(", String talukParam");
      }
    }
    StringBuilder queryStr =
        new StringBuilder("SELECT FROM "
            + JDOUtils.getImplClass(IInvntryBatch.class).getName()
            + " WHERE vld == vldParam");
    String declaration = "Boolean vldParam";
    String ordering = " import java.util.Date; ORDER BY bexp ASC";
    params.put("vldParam", Boolean.TRUE);
    if (materialId != null) {
      queryStr.append(" && mId == mIdParam");
      declaration += ", Long mIdParam";
      params.put("mIdParam", materialId);
    }
    if (domainId != null) {
      queryStr.append(" && dId.contains(dIdParam)");
      declaration += ", Long dIdParam";
      params.put("dIdParam", domainId);
    }
    if (start != null) {
      queryStr.append(" && bexp > startParam");
      declaration += ", Date startParam";
      params.put("startParam", LocalDateUtil.getOffsetDate(start, -1, Calendar.MILLISECOND));
    }
    if (end != null) {
      queryStr.append(" && bexp < endParam");
      declaration += ", Date endParam";
      params.put("endParam", end);
    }
    if (StringUtils.isNotEmpty(kioskTags) || StringUtils.isNotEmpty(excludedKioskTags)) {
      boolean isExcluded = StringUtils.isNotEmpty(excludedKioskTags);
      String value = isExcluded ? excludedKioskTags : kioskTags;
      List<String> tags = StringUtil.getList(value, true);
      queryStr.append(QueryConstants.AND).append(CharacterConstants.O_BRACKET);
      int i = 0;
      for (String iTag : tags) {
        String tagParam = "kTgsParam" + (++i);
        if (i != 1) {
          queryStr.append(isExcluded ? QueryConstants.AND : QueryConstants.OR)
              .append(CharacterConstants.SPACE);
        }
        queryStr
            .append(isExcluded ? QueryConstants.NEGATION
                : CharacterConstants.EMPTY).append("ktgs").append(QueryConstants.DOT_CONTAINS)
            .append(tagParam).append(CharacterConstants.C_BRACKET).append(CharacterConstants.SPACE);
        declaration += ", Long " + tagParam;
        params.put(tagParam, tagDao.getTagFilter(iTag, ITag.KIOSK_TAG));
      }
      queryStr.append(CharacterConstants.C_BRACKET);
    }
    if (StringUtils.isNotEmpty(materialTag)) {
      List<String> tags = StringUtil.getList(materialTag, true);
      queryStr.append(QueryConstants.AND).append(CharacterConstants.O_BRACKET);
      int i = 0;
      for (String iTag : tags) {
        String tagParam = "mTgsParam" + (++i);
        if (i != 1) {
          queryStr.append(QueryConstants.OR).append(CharacterConstants.SPACE);
        }
        queryStr.append("mtgs").append(QueryConstants.DOT_CONTAINS)
            .append(tagParam).append(CharacterConstants.C_BRACKET).append(CharacterConstants.SPACE);
        declaration += ", Long " + tagParam;
        params.put(tagParam, tagDao.getTagFilter(iTag, ITag.MATERIAL_TAG));
      }
      queryStr.append(CharacterConstants.C_BRACKET);
    }
    declaration += locationParamDeclaration.toString();
    queryStr.append(locSubQuery.toString()).append(kioskVariableDeclaration).append(" PARAMETERS ")
        .append(declaration).append(ordering);
    PersistenceManager pm = PMF.get().getPersistenceManager();
    Query q = pm.newQuery(queryStr.toString());
    if (pageParams != null) {
      QueryUtil.setPageParams(q, pageParams);
    }
    // Execute
    List<IInvntryBatch> results = null;
    String cursor = null;
    try {
      results = (List<IInvntryBatch>) q.executeWithMap(params);
      if (results != null) {
        results.size();
        cursor = QueryUtil.getCursor(results);
        results = (List<IInvntryBatch>) pm.detachCopyAll(results);
      }
    } finally {
      try {
        q.closeAll();
      } catch (Exception ignored) {
        xLogger.warn("Exception while closing query", ignored);
      }
      pm.close();
    }
    xLogger.fine("Exiting getInventoryByBatchExpiry");
    return new Results(results, cursor);
  }

  public Results<IInvntryBatch> getBatches(Long materialId, Long kioskId, PageParams pageParams)
      throws ServiceException {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    String
        queryStr =
        "SELECT FROM " + JDOUtils.getImplClass(IInvntryBatch.class).getName()
            + " WHERE mId == mIdParam && kId == kIdParam && q > 0 PARAMETERS Long mIdParam, Long kIdParam ORDER BY bexp ASC";
    Query q = pm.newQuery(queryStr);
    if (pageParams != null) {
      QueryUtil.setPageParams(q, pageParams);
    }
    Map<String, Object> params = new HashMap<>();
    params.put("mIdParam", materialId);
    params.put("kIdParam", kioskId);
    xLogger.fine("Params: {0}", params);
    List<IInvntryBatch> results = null;
    String cursor = null;
    try {
      results = (List<IInvntryBatch>) q.executeWithMap(params);
      xLogger.fine("Results: {0}", results);
      if (results != null) {
        results.size();
        cursor = QueryUtil.getCursor(results);
        results = (List<IInvntryBatch>) pm.detachCopyAll(results);
      }
    } finally {
      try {
        q.closeAll();
      } catch (Exception ignored) {
        xLogger.warn("Exception while closing query", ignored);
      }
      pm.close();
    }
    return new Results<>(results, cursor);
  }

  /**
   * Get valid batches for a given inventory item - this includes active batches with non-zero stock
   */
  public Results<IInvntryBatch> getValidBatches(Long materialId, Long kioskId,
                                                PageParams pageParams) throws ServiceException {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    try {
      return getValidBatches(materialId, kioskId, pageParams, pm);
    } finally {
      pm.close();
    }
  }

  @SuppressWarnings("unchecked")
  private Results<IInvntryBatch> getValidBatches(Long materialId, Long kioskId,
                                                 PageParams pageParams, PersistenceManager pm)
      throws ServiceException {
    xLogger.fine("Entered getValidBatches");
    // Form query
    String queryStr = "SELECT FROM " + JDOUtils.getImplClass(IInvntryBatch.class).getName()
        + " WHERE mId == mIdParam && kId == kIdParam && vld == vldParam PARAMETERS Long mIdParam, "
        + "Long kIdParam, Boolean vldParam ORDER BY bexp ASC";
    Query q = pm.newQuery(queryStr);
    if (pageParams != null) {
      QueryUtil.setPageParams(q, pageParams);
    }
    Map<String, Object> params = new HashMap<>();
    params.put("mIdParam", materialId);
    params.put("kIdParam", kioskId);
    params.put("vldParam", Boolean.TRUE);
    xLogger.fine("Params: {0}", params);
    List<IInvntryBatch> results = null;
    String cursor = null;
    try {
      results = (List<IInvntryBatch>) q.executeWithMap(params);
      xLogger.fine("Results: {0}", results);
      if (results != null) {
        results.size();
        cursor = QueryUtil.getCursor(results);
        results = (List<IInvntryBatch>) pm.detachCopyAll(results);
      }
    } finally {
      q.closeAll();
    }
    xLogger.fine("Exiting getValidBatches");
    return new Results<>(results, cursor);
  }

  /**
   * @param domainId Get valid batches for a given batchId - this includes active batches or expired
   * batches based on expiry param with non-zero stock
   */
  public Results getValidBatchesByBatchId(String batchId, Long materialId, Long kioskId,
                                          Long domainId, boolean excludeExpired,
                                          PageParams pageParams) {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    String
        queryStr =
        "SELECT FROM " + JDOUtils.getImplClass(IInvntryBatch.class).getName()
            + " WHERE bid == bIdParam";
    Map<String, Object> params = new HashMap<>();
    String declaration = Constants.EMPTY;
    if (batchId != null) {
      declaration = "String bIdParam";
      params.put("bIdParam", batchId);
    }
    if (excludeExpired) {
      queryStr += " && vld == vldParam";
      declaration += ", Boolean vldParam";
      params.put("vldParam", Boolean.TRUE);
    }
    if (materialId != null) {
      queryStr += " && mId == mIdParam";
      declaration += ", Long mIdParam";
      params.put("mIdParam", materialId);
    }
    if (kioskId != null) {
      queryStr += " && kId == kIdParam";
      declaration += ", Long kIdParam";
      params.put("kIdParam", kioskId);
    }
    if (excludeExpired) {
      Date date = new Date();
      Date expires = null;
      queryStr += " && bexp > startParam";
      declaration += ", java.util.Date startParam";
      DomainConfig dc = DomainConfig.getInstance(domainId);
      String dateStr = LocalDateUtil.formatCustom(date, Constants.DATE_FORMAT, dc.getTimezone());
      try {
        expires =
            LocalDateUtil.getOffsetDate(
                LocalDateUtil.parseCustom(dateStr, Constants.DATE_FORMAT, dc.getTimezone()), -1,
                Calendar.MILLISECOND);
      } catch (ParseException e) {
        xLogger.warn(
            "Exception while parsing date while getting batches by batch ID:" + e.getMessage(), e);
      }
      params.put("startParam", expires);
    }
    queryStr += " PARAMETERS " + declaration;
    xLogger.fine("Params: {0}", params);
    List<IInvntryBatch> results = null;
    String cursor = null;
    Query q = pm.newQuery(queryStr);
    if (pageParams != null) {
      QueryUtil.setPageParams(q, pageParams);
    }
    try {
      results = (List<IInvntryBatch>) q.executeWithMap(params);
      xLogger.fine("Results: {0}", results);
      if (results != null) {
        results.size();
        cursor = QueryUtil.getCursor(results);
        results = (List<IInvntryBatch>) pm.detachCopyAll(results);
      }
    } finally {
      try {
        q.closeAll();
      } catch (Exception ignored) {
        xLogger.warn("Exception while closing query", ignored);
      }
      pm.close();
    }
    xLogger.fine("Exiting getValidBatchesByBatchId");
    return new Results<>(results, cursor);
  }

  /**
   * Add inventory to a given kiosk. This is used when a given material is associated with a kiosk
   * for the first time, with default parameters. Throws an exception if which indicates all the
   * entities already in the data store.
   */
  public void addInventory(Long domainId, List<IInvntry> items, boolean overwrite, String user)
      throws ServiceException {
    xLogger.fine("addInventory: Entering addInventory( List<Inventory> )");
    if (domainId == null || items == null || items.isEmpty()) {
      throw new ServiceException("Invalid parameters");
    }
    Long kioskId = items.get(0).getKioskId();
    for (IInvntry invntry : items) {
      if (invntry.getKioskId() == null) {
        throw new ServiceException("Kiosk id is null for item");
      }
      if (!invntry.getKioskId().equals(kioskId)) {
        throw new ServiceException(
            "Add Inventory supports addition of materials to single Kiosk at a time, " +
                "found multiple kiosks :" + kioskId + "," + invntry.getKioskId());
      }
    }
    LockUtil.LockStatus lockStatus = LockUtil.lock(String.valueOf(kioskId));
    if (!LockUtil.isLocked(lockStatus)) {
      xLogger.severe("Error locking kiosk id  {0} while adding inventory", kioskId);
      throw new ServiceException("Error locking kiosk id  {0} while adding inventory", kioskId);
    }
    PersistenceManager pm = PMF.get().getPersistenceManager();
    List<String> errorMaterialIds = new ArrayList<>(1);
    List<IInvntry> validItems = new ArrayList<>(1);
    List<IInvntry> mmValidItems = new ArrayList<>(1);

    try {

      Date now = new Date();
      // Form the inventory objects and update them
      Iterator<IInvntry> it = items.iterator();

      Long shortId = getShortId(kioskId);
      if (shortId == null) {
        xLogger.severe("Error in setting shortId while adding inventory for {0}", kioskId);
        throw new ServiceException("Error in setting shortId while adding inventory for {0}",
            kioskId);
      }
      while (it.hasNext()) {
        IInvntry item = it.next();
        // Check if this inventory item is already in the data store
        IInvntry in = getInventory(item.getKioskId(), item.getMaterialId(), pm);
        if (in != null
            && overwrite) { // This item already exists in the data store; include as part of error list
          in = pm.detachCopy(in);
          if (BigUtil.notEquals(item.getReorderLevel(), in.getReorderLevel()) ||
              BigUtil.notEquals(item.getMaxStock(), in.getMaxStock()) ||
              BigUtil.notEquals(item.getMaxDuration(), in.getMaxDuration()) ||
              BigUtil.notEquals(item.getMinDuration(), in.getMinDuration())) {
            mmValidItems.add(in);
          }
          in.setReorderLevel(item.getReorderLevel());
          in.setMaxStock(item.getMaxStock());
          in.setMinDuration(item.getMinDuration());
          in.setMaxDuration(item.getMaxDuration());
          in.setRetailerPrice(item.getRetailerPrice());
          in.setTax(item.getTax());
          in.setConsumptionRateManual(item.getConsumptionRateManual());
          in.setUpdatedBy(user);
          in.setUpdatedOn(new Date());
          // Add to valid list
          validItems.add(in);
        } else if (in != null) {
          errorMaterialIds.add(item.getMaterialId().toString());
        } else { // item does not exist for the kiosk
          item.setCreatedOn(now); // NOTE: This internally sets stock update timestamp (t) as well
          item.setShortId(++shortId);
          DomainsUtil.addToDomain(item, domainId, null);
          item.setUpdatedBy(user);
          // Update the material tags
          List<String> tags;
          if (item.getTags(TagUtil.TYPE_MATERIAL) == null) {
            try {
              IMaterial m = JDOUtils.getObjectById(IMaterial.class, item.getMaterialId(), pm);
              tags = m.getTags();
              item.setTgs(tagDao.getTagsByNames(tags, ITag.MATERIAL_TAG),
                  TagUtil.TYPE_MATERIAL); // [tags]
            } catch (Exception e1) {
              xLogger.warn("{0} when getting material {1}: {2}", e1.getClass().getName(),
                  item.getMaterialId(), e1.getMessage());
            }
          }

          // Update the kiosk tags (only for querying inventory on kiosk tags, given a material)
          if (item.getTags(TagUtil.TYPE_ENTITY) == null) {
            try {
              IKiosk k = JDOUtils.getObjectById(IKiosk.class, item.getKioskId(), pm);
              tags = k.getTags();
              item.setTgs(tagDao.getTagsByNames(tags, ITag.KIOSK_TAG),
                  TagUtil.TYPE_ENTITY); // [tags]
            } catch (Exception e1) {
              xLogger.warn("{0} when getting material {1}: {2}", e1.getClass().getName(),
                  item.getMaterialId(), e1.getMessage());
            }
          }

          // Add to valid list
          validItems.add(item);
          mmValidItems.add(item);
        }
      }

      if (!validItems.isEmpty()) {
        // Persist
        pm.makePersistentAll(validItems);

        persistMinMaxLog(mmValidItems);

        for (IInvntry inv : validItems) {
          try {
            EventPublisher.generate(domainId, IEvent.CREATED, null,
                JDOUtils.getImplClass(IInvntry.class).getName(),
                invntryDao.getInvKeyAsString(inv), null);
          } catch (EventGenerationException e) {
            xLogger.warn(
                "Exception when generating creation event for inventory-creation for material-kiosk {0}:{1} in domain {2}: {3}",
                inv.getMaterialId(), inv.getKioskId(), domainId, e.getMessage());
          }
        }
        if (overwrite) {
          for (IInvntry inv : validItems) {
            if (inv != null) {
              updateStockEventLog(inv.getStock(), inv, pm, IInvntryEvntLog.SOURCE_MINMAXUPDATE,
                  domainId);
            }
          }
        }
      }
    } catch (Exception e) {
      xLogger
          .severe("Add Inventory Exception: {0}: {1}", e.getClass().getName(), e.getMessage(), e);
      throw new ServiceException(e);
    } finally {
      pm.close();
      if (LockUtil.shouldReleaseLock(lockStatus) && !LockUtil.release(String.valueOf(kioskId))) {
        xLogger
            .severe("Error while removing kiosk id from cache while adding inventory to kiosk {0}",
                kioskId);
      }
    }

    if (!errorMaterialIds.isEmpty()) {
      throw new ServiceException(
          "Added " + validItems.size() + " material(s). " + errorMaterialIds.size()
              + " material(s) already exist, and were not added.", errorMaterialIds);
    }

  }

  public void updateInventory(List<IInvntry> items, String user) throws ServiceException {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    updateInventory(items, user, pm, true, false);
  }

  public void updateInventory(List<IInvntry> items, String user, PersistenceManager pm,
                              boolean closePM, boolean skipMinMaxLog) throws ServiceException {
    if (items == null || items.isEmpty()) {
      // nothing to remove
      throw new ServiceException("No inventory items to update");
    }
    xLogger.fine("Entering updateInventory - num. of items = {0}", items.size());
    Map<Long, LockUtil.LockStatus> kidLockStatusMap = new HashMap<>();
    try {
      List<IInvntry> updatedItems = new ArrayList<>();
      List<IInvntry> mmUpdatedItems = new ArrayList<>();
      for (IInvntry inventory : items) {
        Long kioskId = inventory.getKioskId();
        if (!kidLockStatusMap.containsKey(kioskId)) {
          LockUtil.LockStatus lockStatus =
              LockUtil.lock(String.valueOf(kioskId), LOCK_RETRY_COUNT,
                  LOCK_RETRY_DELAY_IN_MILLISECONDS);
          if (!LockUtil.isLocked(lockStatus)) {
            ResourceBundle backendMessages = Resources.getBundle(SecurityUtils.getLocale());
            throw new ServiceException(backendMessages.getString("lockinventory.failed"));
          }
          kidLockStatusMap.put(kioskId, lockStatus);
        }
        // Get the data store object
        IInvntry in = invntryDao.getDBInvntry(inventory, pm);
        in = pm.detachCopy(in);
        if (in == null) {
          xLogger.warn("Unable to find inventory with key {0}", inventory.getKeyString());
          continue;
        }
        if (!skipMinMaxLog && (BigUtil.notEquals(inventory.getReorderLevel(), in.getReorderLevel())
            ||
            BigUtil.notEquals(inventory.getMaxStock(), in.getMaxStock()) ||
            BigUtil.notEquals(inventory.getMaxDuration(), in.getMaxDuration()) ||
            BigUtil.notEquals(inventory.getMinDuration(), in.getMinDuration()))) {
          mmUpdatedItems.add(in);
        }
        // Update the fields
        setInventoryFields(in, inventory, user);
        updatedItems.add(in);
      }
      try {
        // Persist changes
        pm.makePersistentAll(updatedItems);
        persistMinMaxLog(mmUpdatedItems);
        updatedItems = (List<IInvntry>) pm.detachCopyAll(updatedItems);
        Long domainId = updatedItems.get(0).getDomainId();
        // Generate events for all items, as needed
        updatedItems.stream().forEach(inv -> {
          try {
            EventPublisher.generate(domainId, IEvent.MODIFIED, null,
                JDOUtils.getImplClass(IInvntry.class).getName(), invntryDao.getInvKeyAsString(inv),
                null);
          } catch (EventGenerationException e) {
            xLogger.warn(
                "Exception when generating inventory-updation event for material-kiosk {0}:{1} in domain {2}: {3}",
                inv.getMaterialId(), inv.getKioskId(), domainId, e.getMessage());
          }
          updateStockEventLog(inv.getStock(), inv, pm, IInvntryEvntLog.SOURCE_MINMAXUPDATE,
              domainId);
        });
      } catch (Exception e) {
        xLogger.severe("Exception: {0}: {1}", e.getClass().getName(), e);
        throw new ServiceException(e);
      } finally {
        if (closePM) {
          // Close the persistence manager
          pm.close();
        }
      }
    } catch (Exception e) {
      xLogger.severe("Exception: ", e);
      throw new ServiceException(e);
    } finally {
      // Release locks
      if (!kidLockStatusMap.isEmpty()) {
        LockUtil.releaseLocks(kidLockStatusMap, CharacterConstants.EMPTY);
      }
    }
    xLogger.fine("Exiting updateInventory");
  }

  public void removeInventory(Long domainId, Long kioskId, List<Long> materialIds)
      throws ServiceException {
    if (kioskId == null || materialIds == null) {
      throw new ServiceException("Invalid kiosk ID or material IDs"); // nothing to remove
    }
    xLogger.fine("Entered removeInventory");
    for (Long materialId : materialIds) {
      Object[] keys = new Object[2];
      keys[0] = kioskId;
      keys[1] = materialId;
      PersistenceManager pm = PMF.get().getPersistenceManager();
      try {
        IInvntry inv = getInventory(kioskId, materialId, pm);
        if (inv == null) {
          continue;
        }
        if (!closeOpenEvent(inv.getKey(), pm)) {
          continue;
        }
        try {
          EventPublisher.generate(domainId, IEvent.DELETED, null,
              JDOUtils.getImplClassName(IInvntry.class), inv.getKeyString(), null, inv);
        } catch (EventGenerationException e) {
          xLogger.warn(
              "Exception when generating event for inventory-deletion for material-kiosk {0}:{1} in domain {2}: {3}",
              inv.getMaterialId(), inv.getKioskId(), domainId, e.getMessage());
        }
        // Remove the entities from Inventory table all related tables
        EntityRemover
            .removeRelatedEntities(domainId, JDOUtils.getImplClass(IInvntry.class).getName(),
                keys,
                false);
      } catch (Exception e) {
        xLogger.severe("Exception {0} when removing inventory for kiosk-material {1}-{2} : {3}",
            e.getClass().getName(), kioskId, materialId, e.getMessage(), e);
      } finally {
        if (pm != null) {
          pm.close();
        }
      }
    }
    xLogger.fine("Exiting removeInventory");
  }


  /**
   * Get inventory transactions for a given kiosk.
   * kioskId is mandatory.
   */
  public Results getInventoryTransactionsByKiosk(Long kioskId, String materialTag, Date sinceDate,
                                                 Date untilDate, String transType,
                                                 PageParams pageParams, String bid, boolean atd,
                                                 String reason) throws ServiceException {
    return getInventoryTransactionsByKiosk(kioskId, null, materialTag, sinceDate, untilDate,
        transType, pageParams, bid, atd, reason);
  }

  /**
   * Get inventory transactions for a given kiosk.
   * kioskId is mandatory.
   */
  public Results getInventoryTransactionsByKiosk(Long kioskId, Long materialId, String materialTag,
                                                 Date sinceDate, Date untilDate, String transType,
                                                 PageParams pageParams, String bid,
                                                 boolean atd, String reason)
      throws ServiceException {
    return getInventoryTransactions(sinceDate, untilDate, null, kioskId, materialId, transType,
        null,
        null, materialTag, null, pageParams, bid, atd, reason);
  }

  /**
   * Get inventory transactions for a given material.
   * materialId and sinceDate are mandatory. transType is optional
   */
  public Results getInventoryTransactionsByMaterial(Long materialId, String kioskTag,
                                                    Date sinceDate, Date untilDate,
                                                    String transType, PageParams pageParams,
                                                    Long domainId,
                                                    String bid, boolean atd, String reason)
      throws ServiceException {
    return getInventoryTransactions(sinceDate, untilDate, domainId, null, materialId, transType,
        null, kioskTag, null, null, pageParams, bid, atd, reason);
  }

  /**
   * Get inventory transactions for a given domain.
   * domainId and sinceDate are mandatory. transType is optional
   */
  public Results getInventoryTransactionsByDomain(Long domainId, String kioskTag,
                                                  String materialTag, Date sinceDate,
                                                  Date untilDate, String transType,
                                                  List<Long> kioskIds,
                                                  PageParams pageParams, String bid, boolean atd,
                                                  String reason) throws ServiceException {
    return getInventoryTransactions(sinceDate, untilDate, domainId, null, null, transType, null,
        kioskTag, materialTag, kioskIds, pageParams, bid, atd, reason);
  }

  public Results getInventoryTransactionsByKioskLink(Long kioskId, Long linkedKioskId,
                                                     String materialTag, Date sinceDate,
                                                     Date untilDate, String transType,
                                                     PageParams pageParams,
                                                     String bid, boolean atd, String reason)
      throws ServiceException {
    if (kioskId == null || linkedKioskId == null) {
      throw new ServiceException("Kiosk IDs or linked Kiosk IDs date not specified");
    }
    return getInventoryTransactions(sinceDate, untilDate, null, kioskId, null, transType,
        linkedKioskId, null, materialTag, null, pageParams, bid, atd, reason);
  }

  /**
   * Get inventory transactions by a given user
   */
  @SuppressWarnings("unchecked")
  public Results getInventoryTransactionsByUser(String userId, Date fromDate, Date toDate,
                                                PageParams pageParams) throws ServiceException {
    xLogger.fine("Entered getInventoryTransactionsByUser");
    if (userId == null || fromDate == null) {
      throw new IllegalArgumentException("User ID and from date are not supplied");
    }
    // Get the query and params
    String query = "SELECT FROM " + JDOUtils.getImplClass(ITransaction.class).getName()
        + " WHERE uId == uIdParam && t > fromParam";
    Map<String, Object> params = new HashMap<>();
    params.put("uIdParam", userId);
    params.put("fromParam", LocalDateUtil.getOffsetDate(fromDate, -1, Calendar.MILLISECOND));
    if (toDate != null) {
      query += " && t < toParam";
      params.put("toParam", toDate);
    }
    query += " PARAMETERS String uIdParam, Date fromParam";
    if (toDate != null) {
      query += ", Date toParam";
    }
    query += " import java.util.Date; ORDER BY t desc";
    PersistenceManager pm = PMF.get().getPersistenceManager();
    Query q = pm.newQuery(query);
    if (pageParams != null) {
      QueryUtil.setPageParams(q, pageParams);
    }
    // Execute query
    List<ITransaction> transactions = null;
    String cursor = null;
    try {
      transactions = (List<ITransaction>) q.executeWithMap(params);
      if (transactions != null) {
        transactions.size();
        cursor = QueryUtil.getCursor(transactions);
        transactions = (List<ITransaction>) pm.detachCopyAll(transactions);
      }
    } finally {
      try {
        q.closeAll();
      } catch (Exception ignored) {
        xLogger.warn("Exception while closing query", ignored);
      }
      pm.close();
    }
    xLogger.fine("Exiting getInventoryTransactionsByUser");
    return new Results(transactions, cursor);
  }

  @Override
  public Results getInventoryTransactions(Date sinceDate, Date untilDate, Long domainId,
                                          Long kioskId, Long materialId, List<String> transTypes,
                                          Long linkedKioskId, String kioskTag, String materialTag,
                                          List<Long> kioskIds, PageParams pageParams, String bid,
                                          boolean atd, String reason, List<String> excludeReasons,
                                          PersistenceManager pm) throws ServiceException {
    return getInventoryTransactions(sinceDate, untilDate, domainId, kioskId, materialId, transTypes,
        linkedKioskId, kioskTag, materialTag, kioskIds, pageParams, bid, atd, reason,
        excludeReasons, pm);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Results getInventoryTransactions(Date sinceDate, Date untilDate, Long domainId,
                                          Long kioskId, Long materialId, String transType,
                                          Long linkedKioskId, String kioskTag,
                                          String materialTag, List<Long> kioskIds,
                                          PageParams pageParams, String bid, boolean atd,
                                          String reason, boolean ignoreLkid)
      throws ServiceException {
    return getInventoryTransactions(sinceDate, untilDate, domainId, kioskId, materialId,
        transType != null ? Collections.singletonList(transType) : null, linkedKioskId, kioskTag,
        materialTag, kioskIds, pageParams, bid, atd, reason, null, ignoreLkid, null);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Results getInventoryTransactions(Date sinceDate, Date untilDate, Long domainId,
                                          Long kioskId, Long materialId, String transType,
                                          Long linkedKioskId, String kioskTag,
                                          String materialTag, List<Long> kioskIds,
                                          PageParams pageParams, String bid, boolean atd,
                                          String reason) throws ServiceException {
    return getInventoryTransactions(sinceDate, untilDate, domainId, kioskId, materialId,
        transType != null ? Collections.singletonList(transType) : null, linkedKioskId, kioskTag,
        materialTag, kioskIds, pageParams, bid, atd, reason, null, false, null);
  }

  @Override
  public Results getInventoryTransactions(Date sinceDate, Date untilDate, Long domainId,
                                          Long kioskId, Long materialId, List<String> transTypes,
                                          Long linkedKioskId, String kioskTag,
                                          String materialTag, List<Long> kioskIds,
                                          PageParams pageParams, String bid,
                                          boolean atd, String reason, List<String> reasons,
                                          boolean ignoreLkid,
                                          PersistenceManager pm) throws ServiceException {
    return getInventoryTransactions(sinceDate, untilDate, domainId, kioskId, materialId, transTypes,
        linkedKioskId, kioskTag, materialTag, kioskIds, pageParams, bid, atd, reason, reasons,
        ignoreLkid, pm, true);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Results getInventoryTransactions(Date sinceDate, Date untilDate, Long domainId,
                                          Long kioskId, Long materialId, List<String> transTypes,
                                          Long linkedKioskId, String kioskTag,
                                          String materialTag, List<Long> kioskIds,
                                          PageParams pageParams, String bid,
                                          boolean atd, String reason, List<String> reasons,
                                          boolean ignoreLkid,
                                          PersistenceManager pm, boolean excludeReasons)
      throws ServiceException {
    xLogger.fine("Entering getInventoryTransactions");
    Results results = transDao.getInventoryTransactions(sinceDate, untilDate, domainId, kioskId,
        materialId, transTypes, linkedKioskId, kioskTag, materialTag, kioskIds, pageParams, bid,
        atd, reason, reasons, ignoreLkid, pm, excludeReasons);
    xLogger.fine("Exiting getInventoryTransactions");
    return results;
  }

  /**
   * Get inventory transactions by tracking-id
   */
  @SuppressWarnings("unchecked")
  public Results getInventoryTransactionsByTrackingId(Long trackingId) throws ServiceException {
    xLogger.fine("Entered getInventoryTransactionsByTrackingId");
    List<ITransaction> trans = null;
    // Get PM
    PersistenceManager pm = PMF.get().getPersistenceManager();
    Query q = pm.newQuery(JDOUtils.getImplClass(ITransaction.class));
    q.setFilter("tid == tidParam");
    q.declareParameters("Long tidParam");
    try {
      trans = (List<ITransaction>) q.execute(trackingId);
      trans = (List<ITransaction>) pm.detachCopyAll(trans);
      trans.size();
    } finally {
      try {
        q.closeAll();
      } catch (Exception ignored) {
        xLogger.warn("Exception while closing query", ignored);
      }
      pm.close();
    }
    Results r = new Results(trans, null);
    xLogger.fine("Exiting getInventoryTransactionsByTrackingId");
    return r;
  }

  /**
   * Update a list of inventory transactions
   *
   * @return List of error transactions - i.e. those that could not be updated
   */

  public CreateTransactionsReturnModel updateInventoryTransactions(Long domainId,
                                                                   List<ITransaction> inventoryTransactions)
      throws ServiceException, DuplicationException {
    return updateInventoryTransactions(domainId, inventoryTransactions, false);
  }

  public CreateTransactionsReturnModel updateInventoryTransactions(Long domainId,
                                                                   List<ITransaction> inventoryTransactions,
                                                                   PersistenceManager pm)
      throws ServiceException, DuplicationException {
    return updateInventoryTransactions(domainId, inventoryTransactions, false, pm);
  }

  public CreateTransactionsReturnModel updateInventoryTransactions(Long domainId,
                                                                   List<ITransaction> inventoryTransactions,
                                                                   boolean skipVal)
      throws ServiceException, DuplicationException {
    return updateInventoryTransactions(domainId, inventoryTransactions, skipVal, false);
  }

  public CreateTransactionsReturnModel updateInventoryTransactions(Long domainId,
                                                                   List<ITransaction> inventoryTransactions,
                                                                   boolean skipVal,
                                                                   PersistenceManager pm)
      throws ServiceException, DuplicationException {
    return updateInventoryTransactions(domainId, inventoryTransactions, skipVal, false, pm);
  }

  public CreateTransactionsReturnModel updateInventoryTransactions(Long domainId,
                                                                   List<ITransaction> inventoryTransactions,
                                                                   boolean skipVal,
                                                                   boolean skipPred)
      throws ServiceException, DuplicationException {
    return updateInventoryTransactions(domainId, inventoryTransactions, skipVal, skipPred, null);
  }

  public CreateTransactionsReturnModel updateInventoryTransactions(Long domainId,
                                                                   List<ITransaction> inventoryTransactions,
                                                                   boolean skipVal,
                                                                   boolean skipPred,
                                                                   PersistenceManager pm)
      throws DuplicationException, ServiceException {
    return updateInventoryTransactions(domainId, inventoryTransactions, null, skipVal, skipPred,
        pm);
  }

  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  public CreateTransactionsReturnModel updateInventoryTransactions(Long domainId,
                                                                   List<ITransaction> inventoryTransactions,
                                                                   List<IInvntry> invntryList,
                                                                   boolean skipVal,
                                                                   boolean skipPred,
                                                                   PersistenceManager pm)
      throws ServiceException, DuplicationException {
    xLogger.fine("Entering updateInventoryTransactions");
    boolean closePM = pm == null;
    if (CollectionUtils.isEmpty(inventoryTransactions)) {
      throw new ServiceException("Transaction list cannot be empty");
    }
    if (domainId == null) {
      throw new ServiceException("Unknown domain");
    }
    // Get type of transactions
    String tType = inventoryTransactions.get(0).getType();
    String userId = inventoryTransactions.get(0).getSourceUserId();
    String trkType = inventoryTransactions.get(0).getTrackingObjectType();
    boolean isOrder =
        ITransaction.TYPE_ORDER.equals(trkType) || IInvAllocation.Type.SHIPMENT.toString()
            .equals(trkType)
            || ITransaction.TRACKING_OBJECT_TYPE_ORDER_SHIPMENT.equals(trkType)
            || ITransaction.TRACKING_OBJECT_TYPE_TRANSFER_SHIPMENT.equals(trkType);
    boolean isReturn = ITransaction.TYPE_RETURNS_INCOMING.equals(tType) || ITransaction.TYPE_RETURNS_OUTGOING.equals(tType);
    Locale locale = SecurityUtils.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    try {
      if (!isOrder && !authorizationService.authoriseTransactionAccess(tType, domainId,
          userId)) {
        throw new UnauthorizedRequestException("UA001", locale,
            TransactionUtil.getDisplayName(tType, locale), userId);
      }
    } catch (ObjectNotFoundException e) {
      xLogger.severe("Error loading UserAccount in Authoriser updateInventoryTransaction", e);
      throw new ServiceException(backendMessages.getString("user.none"));
    }

    // Validate transaction type
    if (!isTransactionTypeValid(tType)) {
      throw new ServiceException("Invalid transaction type: " + tType);
    }
    // Deduplicate the transactions, except for stock counts
    if (!skipVal && !ITransaction.TYPE_PHYSICALCOUNT.equals(tType) && TransactionUtil
        .deduplicate(domainId, inventoryTransactions)) {
      throw new DuplicationException(
          "The transaction updates appear to be a duplication of what was tried within the last "
              + (TransactionUtil.DEDUPLICATION_DURATION / 60)
              + " minutes. If this is not the case, please check the stock levels and re-try after "
              + (TransactionUtil.DEDUPLICATION_DURATION / 60) + " minutes");
    }
    if (closePM) {
      pm = PMF.get().getPersistenceManager();
    }

    List<ITransaction> errors = new ArrayList<>(1); // holds transactions in error
    Iterator<ITransaction> it = inventoryTransactions.iterator();
    List<ITransaction> committedTransList = new ArrayList<>(1);
    List<IInvntry> toBeOptimized = new ArrayList<>(1);
    List<IInvntry> toBePredicted = new ArrayList<>(inventoryTransactions.size());
    try {
      while (it.hasNext()) {
        ITransaction trans = it.next();
        String lockKey = null;
        String destLockKey = null;
        LockUtil.LockStatus lockStatus = null;
        try {
          // Get the kiosk and material Ids
          Long kioskId = trans.getKioskId();
          Long materialId = trans.getMaterialId();
          lockKey = Constants.TX + kioskId;
          if (ITransaction.TYPE_TRANSFER.equals(tType)) {
            destLockKey = Constants.TX + trans.getLinkedKioskId();
            lockStatus = LockUtil.doubleLock(lockKey, destLockKey, 25, 200);
          } else {
            lockStatus = LockUtil.lock(lockKey, 25, 200);
          }
          if (!LockUtil.isLocked(lockStatus)) {
            trans.setMessage(
                "Unable to lock inventory to perform transaction. Please retry after sometime");
            trans.setMsgCode("M004");
            errors.add(trans);
            lockKey = destLockKey = null;
            continue;
          }
          // Get the inventory object
          IInvntry in = null;
          if (invntryList != null && !invntryList.isEmpty()) {
            for (IInvntry invntry : invntryList) {
              if (invntry.getKioskId().equals(kioskId) && invntry.getMaterialId()
                  .equals(materialId)) {
                in = invntry;
                break;
              }
            }
          }
          if (in == null) {
            in = getInventory(kioskId, materialId, pm);
          }
          // If no inventory configured, then add this transaction to error list
          if (in == null) {
            trans.setMessage(backendMessages.getString("error.nomaterialinkiosk"));
            trans.setMsgCode("M005");
            errors.add(trans);
            continue; // continue with the next object in the list
          }
          if (trans.getAtd() != null && isAtdNotValid(domainId, trans.getAtd())) {
            trans.setMessage(backendMessages.getString("error.adt"));
            trans.setMsgCode("M006");
            errors.add(trans);
            continue; // continue with the next object in the list
          }
          try {
            checkHandlinkUnitErrors(trans);
          } catch (LogiException e) {
            trans.setMessage(e.getMessage());
            trans.setMsgCode(e.getCode());
            errors.add(trans);
            continue;
          }
          if (isReturn) {
            try {
              checkReturnsErrors(trans, pm);
            } catch (LogiException e) {
              trans.setMessage(e.getMessage());
              trans.setMsgCode(e.getCode());
              errors.add(trans);
              continue;
            }
          }
          // Get the current time
          Date timestamp = new Date();
          // Check if stock is being updated for the first time (used during event generation)
          boolean
              isStockUpdatedFirstTime =
              BigUtil.equalsZero(in.getStock()) && in.getTimestamp() != null
                  && in.getCreatedOn() != null && in.getCreatedOn().equals(in.getTimestamp());
          // Check if a batch is involved
          IInvntryBatch invBatch = null;
          if (trans.hasBatch()) {
            invBatch = getInventoryBatch(kioskId, materialId, trans.getBatchId(), pm);
          }
          // Get the stock-on-hand for this inventory item
          BigDecimal stockOnHand = in.getAvailableStock();
          BigDecimal stockOnHandTotal = in.getAvailableStock();

          if (invBatch != null) {
            stockOnHand = invBatch.getAvailableStock();
          } else if (trans.hasBatch()) {
            stockOnHand = BigDecimal.ZERO;
          }

          try {
            checkTransactionErrors(stockOnHand, trans);
          } catch (LogiException e) {
            trans.setMessage(e.getMessage());
            trans.setMsgCode(e.getCode());
            errors.add(trans);
            continue;
          }

          BigDecimal linkedKioskStockOnHand = BigDecimal.ZERO;
          IInvntry linkedKioskInv = null;
          IInvntryBatch linkedInvBatch = null;

          if (ITransaction.TYPE_TRANSFER.equals(tType)) {
            linkedKioskInv = getInventory(trans.getLinkedKioskId(), materialId, pm);
            if (linkedKioskInv == null) {
              IMaterial m = materialCatalogService.getMaterial(materialId);
              StringBuilder message = new StringBuilder(m.getName());
              if (trans.hasBatch()) {
                message.append("(").append(trans.getBatchId()).append(")");
              }
              message.append(" ")
                  .append(backendMessages.getString("error.doesnotexistindestinationentity"));
              trans.setMessage(message.toString());
              trans.setMsgCode("M007");
              errors.add(trans);
              continue; // continue with the next object in the list
            }
            linkedKioskStockOnHand = linkedKioskInv.getStock();
            if (trans.hasBatch()) {
              linkedInvBatch =
                  getInventoryBatch(trans.getLinkedKioskId(), materialId, trans.getBatchId(), pm);
            }
          }
          if (!trans.useCustomTimestamp()) {
            trans.setTimestamp(timestamp);
          }
          // Update tags for querying purpose
          List<String> tags = in.getTags(TagUtil.TYPE_MATERIAL);
          if (tags != null && !tags.isEmpty()) {
            trans.setTgs(tagDao.getTagsByNames(tags, ITag.MATERIAL_TAG), TagUtil.TYPE_MATERIAL);
          }
          tags = in.getTags(TagUtil.TYPE_ENTITY);
          if (tags != null && !tags.isEmpty()) {
            trans.setTgs(tagDao.getTagsByNames(tags, ITag.KIOSK_TAG), TagUtil.TYPE_ENTITY);
          }
          trans.setDomainId(in.getDomainId());
          /**** Transactional object creation - trans, inv. update, inv. log creation ***/
          List objects;
          try {
            if (ITransaction.TYPE_TRANSFER.equals(tType)) {
              objects = createTransactableObjectsForTransfer(trans, in, invBatch, linkedKioskInv,
                  linkedInvBatch, timestamp, pm);
            } else {
              // update stock and get related logs
              objects = createTransactableObjects(trans, in, invBatch, timestamp, pm);
            }
          } catch (Exception e) {
            trans.setMessage(e.getMessage());
            errors.add(trans);
            continue;
          }
          /**** Transaction persistence ***/

          javax.jdo.Transaction tx = null;
          try {
            if (closePM) {
              tx = pm.currentTransaction();
              tx.begin();
            }
            pm.makePersistentAll(objects);
            if (objects.get(0) instanceof ITransaction
                && ((ITransaction) objects.get(0)).getTransactionId() == null) {
              pm.makePersistentAll(objects);
            }
            if (closePM) {
              tx.commit();
            }
            committedTransList.add(trans);
          } catch (Exception e) {
            xLogger
                .warn("Exception {0} when committing transaction {1}: {2}", e.getClass().getName(),
                    transDao.getKeyAsString(trans), e.getMessage(), e);
            trans.setMessage("System error occurred while persisting Transactions.");
            trans.setMsgCode("M004");
            errors.add(trans);
            continue; // Go to the next transaction
          } finally {
            if (closePM && tx.isActive()) {
              xLogger.warn("Could not commit...rolling back transaction {0}",
                  transDao.getKeyAsString(trans));
              tx.rollback();
            } else {
              //Post commit link transfers
              if (ConfigUtil.isLogi() && ITransaction.TYPE_TRANSFER.equals(tType)) {
                for (Object object : objects) {
                  if (object instanceof ITransaction && !((ITransaction) object).getKeyString()
                      .equals(trans.getKeyString())) {
                    ITransaction
                        linkedTransaction =
                        JDOUtils.getObjectById(ITransaction.class,
                            ((ITransaction) object).getKeyString(), pm);
                    transDao.linkTransactions(linkedTransaction, trans);
                  }
                }
              }
            }
          }
          // Check if this item need optimization computation (D&Q) - i.e. if it goes below safety stock
          BigDecimal safetyStock = in.getSafetyStock();
          if (BigUtil.notEqualsZero(safetyStock) && BigUtil
              .lesserThanEquals(in.getStock(), safetyStock)) {
            toBeOptimized.add(in);
          }
          if (!skipPred) {
            toBePredicted.add(in);
          }
          // Generate the necessary events
          generateEvents(trans, in, stockOnHandTotal, linkedKioskInv, linkedKioskStockOnHand,
              isStockUpdatedFirstTime, domainId);
        } catch (ServiceException e) {
          xLogger.warn("ServiceException when processing transaction {0}: {1}",
              transDao.getKeyAsString(trans), e.getMessage());
          trans.setMessage(e.getMessage());
          trans.setMsgCode("M004");
          errors.add(trans);
        } finally {
          if (destLockKey != null && LockUtil.shouldReleaseLock(lockStatus)) {
            LockUtil.release(lockKey, destLockKey);
          } else if (lockKey != null && LockUtil.shouldReleaseLock(lockStatus)) {
            LockUtil.release(lockKey);
          }
        }
      }
      committedTransList = (List<ITransaction>) pm.detachCopyAll(committedTransList);
    } finally {
      // Close PM
      if (closePM) {
        pm.close();
      }

    }
    if (!toBeOptimized.isEmpty() || !committedTransList.isEmpty()) {
      // Invoke the post-transaction commit hook, as required
      doPostTransactionCommitHook(committedTransList, toBeOptimized);
    }
    if (!toBePredicted.isEmpty()) {
      doPostTransactionPredictions(domainId, toBePredicted);
    }

    xLogger.fine("Exiting updateInventoryTransactions");

    return new CreateTransactionsReturnModel(committedTransList, errors);
  }

  private boolean isAtdNotValid(Long domainId, Date actualTransactionDate) {
    return isAtdNotValid(
        DomainConfig.getInstance(domainId).getTimezone(), actualTransactionDate,
        Calendar.getInstance().getTime());
  }

  protected boolean isAtdNotValid(String timeZone, Date actualTransactionDate, Date currentDate) {
    Date atd;
    if (StringUtils.isNotBlank(timeZone)) {
      atd = LocalDateUtil.convertTZDateToUTC(timeZone, actualTransactionDate);
    } else {
      atd = actualTransactionDate;
    }
    return atd.after(currentDate);
  }

  private void doPostTransactionPredictions(Long domainId, List<IInvntry> toBePredicted) {
    DomainConfig dc = DomainConfig.getInstance(domainId);
    if (dc.getInventoryConfig().isCREnabled()) {
      Map<String, String> params = new HashMap<>(1);
      for (IInvntry iInvntry : toBePredicted) {
        try {
          params.put("invId", iInvntry.getKeyString());
          //Added 30 sec delay to let update inventory during post transaction
          getTaskService().schedule(ITaskService.QUEUE_OPTIMZER, UPDATE_PREDICTION_TASK, params,
              null, ITaskService.METHOD_POST, System.currentTimeMillis() + 30000);
        } catch (TaskSchedulingException e) {
          xLogger.warn(
              "Error while scheduling prediction update during transaction for inventory {0}",
              iInvntry.getKeyString(), e);
        }
      }
    }
  }

  private void checkHandlinkUnitErrors(ITransaction trans) throws LogiException {
    Map<String, String> huMap = handlingUnitService.getHandlingUnitDataByMaterialId(
        trans.getMaterialId());
    if (huMap != null && BigUtil.notEqualsZero(
        trans.getQuantity().remainder(new BigDecimal(huMap.get(IHandlingUnit.QUANTITY))))) {
      String matName = materialCatalogService.getMaterial(trans.getMaterialId()).getName();
      throw new LogiException("T001", trans.getQuantity().stripTrailingZeros().toPlainString(),
          matName, huMap.get(IHandlingUnit.NAME), huMap.get(IHandlingUnit.QUANTITY), matName);
    }
  }

  /**
   * This method checks whether there are any errors in case the transaction is of type incoming returns or outgoing returns.
   * It checks if tracking id, tracking object type are present. The quantity returned cannot be greater than quantity of the corresponding issue or receipt transaction
   * against which the return is being posted.
   * @param trans
   * @param pm
   * @throws LogiException in case validation fails.
   */
  protected void checkReturnsErrors(ITransaction trans, PersistenceManager pm) throws LogiException {
    if (StringUtils.isEmpty(trans.getTrackingId()) || StringUtils.isEmpty(trans.getTrackingObjectType())) {
      throw new LogiException("M017", (Object[]) null);
    }
    if (!(ITransaction.TRACKING_OBJECT_TYPE_ORDER.equals(trans.getTrackingObjectType())
        || ITransaction.TRACKING_OBJECT_TYPE_TRANSFER.equals(trans.getTrackingObjectType())
        || ITransaction.TRACKING_OBJECT_TYPE_ISSUE_TRANSACTION.equals(trans.getTrackingObjectType())
        || ITransaction.TRACKING_OBJECT_TYPE_RECEIPT_TRANSACTION
        .equals(trans.getTrackingObjectType()))) {
      throw new LogiException("M017", (Object[]) null);
    }
    if (!(ITransaction.TRACKING_OBJECT_TYPE_ORDER.equals(trans.getTrackingObjectType())
        || ITransaction.TRACKING_OBJECT_TYPE_TRANSFER.equals(trans.getTrackingObjectType()))) {
      ITransaction linkedTransaction = transDao.getById(trans.getTrackingId(), pm);
      if (BigUtil.greaterThan(trans.getQuantity(), linkedTransaction.getQuantity())) {
        throw new LogiException("M019", trans.getQuantity(), linkedTransaction.getQuantity());
      }
      try {
        Optional<ReturnsConfig> returnsConfiguration = this.getReturnsConfig(trans.getKioskId());
        if (!returnsConfiguration.isPresent()) {
          return;
        }
        validateReturnsPolicy(trans.getTrackingObjectType(), linkedTransaction,
            returnsConfiguration.get());
      } catch (ValidationException e) {
        throw new LogiException("M018", (Object[]) null);
      }
    }
  }

  /**
   * Validates if return for a kiosk allowed based on return policy.
   * @param trackingObjType - Tracking object type based on which validation is done against incoming or outgoing duration
   * @param linkedTransaction - The transaction against which the return is being posted
   * @throws ValidationException
   */
  protected void validateReturnsPolicy(String trackingObjType, ITransaction linkedTransaction,
                                       ReturnsConfig returnsConfiguration) throws
      ValidationException {
    if (!(ITransaction.TRACKING_OBJECT_TYPE_ISSUE_TRANSACTION.equals(trackingObjType)
        || ITransaction.TRACKING_OBJECT_TYPE_RECEIPT_TRANSACTION.equals(trackingObjType))) {
      return;
    }

    Date linkedTransactionDate = linkedTransaction.getTimestamp();
    Integer
        duration =
        ITransaction.TRACKING_OBJECT_TYPE_ISSUE_TRANSACTION.equals(trackingObjType)
            ? returnsConfiguration.getIncomingDuration()
            : returnsConfiguration.getOutgoingDuration();
    if (duration == null || duration.compareTo(0) == 0) {
      return;
    }
    Long
        durationInMillis = TimeUnit.DAYS.toMillis(duration);
    if ((System.currentTimeMillis() - linkedTransactionDate.getTime()) > durationInMillis) {
      throw new ValidationException("M018",
          "Duration cannot be greater than the duration configured for returns");
    }
  }

  /**
   * Update a single inventory transaction
   */
  public ITransaction updateInventoryTransaction(Long domainId, ITransaction inventoryTransaction)
      throws ServiceException, DuplicationException {
    return updateInventoryTransaction(domainId, inventoryTransaction, false);
  }

  public ITransaction updateInventoryTransaction(Long domainId, ITransaction inventoryTransaction,
                                                 PersistenceManager pm)
      throws ServiceException, DuplicationException {
    return updateInventoryTransaction(domainId, inventoryTransaction, false, pm);
  }

  public ITransaction updateInventoryTransaction(Long domainId, ITransaction inventoryTransaction,
                                                 boolean skipPred)
      throws ServiceException, DuplicationException {
    if (inventoryTransaction == null) {
      throw new ServiceException("Invalid parameter passed");
    }

    List<ITransaction> list = new ArrayList<>();
    list.add(inventoryTransaction);
    List<ITransaction> errorList = updateInventoryTransactions(domainId, list, false, skipPred).getErrorTransactions();
    if (errorList != null && !errorList.isEmpty()) {
      return errorList.get(
          0); // Since the list has only one Transaction, the errorList also cannot have more than one Transaction
    } else {
      return null;
    }
  }

  public ITransaction updateInventoryTransaction(Long domainId, ITransaction inventoryTransaction,
                                                 boolean skipPred, boolean skipVal,
                                                 PersistenceManager pm)
      throws ServiceException, DuplicationException {
    if (inventoryTransaction == null) {
      throw new ServiceException("Invalid parameter passed");
    }

    List<ITransaction> list = new ArrayList<>(1);
    list.add(inventoryTransaction);
    CreateTransactionsReturnModel createTransactionsReturnModel = updateInventoryTransactions(domainId, list, skipVal, skipPred,
        pm);
    return convertResponseToTransaction(createTransactionsReturnModel);
  }

  private ITransaction convertResponseToTransaction(CreateTransactionsReturnModel createTransactionsReturnModel) throws ServiceException {
    if (!CollectionUtils.isEmpty(createTransactionsReturnModel.getErrorTransactions())) {
      // Since the list has only one Transaction, the errorList also cannot have more than one Transaction
      return createTransactionsReturnModel.getErrorTransactions().get(
          0);
    } else if (!CollectionUtils.isEmpty(createTransactionsReturnModel.getSuccessfulTransactions())) {
      return createTransactionsReturnModel.getSuccessfulTransactions().get(
          0);
    } else {
      throw new ServiceException("ServiceException while updating inventory transaction");
    }
  }

  public ITransaction updateInventoryTransaction(Long domainId, ITransaction inventoryTransaction,
                                                 boolean skipPred, PersistenceManager pm)
      throws ServiceException, DuplicationException {
    if (inventoryTransaction == null) {
      throw new ServiceException("Invalid parameter passed");
    }

    List<ITransaction> list = new ArrayList<>();
    list.add(inventoryTransaction);
    CreateTransactionsReturnModel createTransactionsReturnModel = updateInventoryTransactions(domainId, list, false, skipPred, pm);
    return convertResponseToTransaction(createTransactionsReturnModel);
  }

  /**
   * Undo inventory transactions. Returns a list of transasctions that could NOT be un-done.
   */
  public List<ITransaction> undoTransactions(List<String> transactionIds) throws ServiceException {
    xLogger.fine("Entered undoTransactions");
    if (transactionIds == null || transactionIds.isEmpty()) {
      throw new ServiceException("No transaction IDs specified");
    }
    Date now = new Date();
    List<ITransaction> deleteList = new ArrayList<>(1);
    List<ITransaction> errorList = new ArrayList<>(1);
    List<IInvntryLog> logList = new ArrayList<>(1);
    List<IInvntry> updateList = new ArrayList<>(1);
    Iterator<String> ids = transactionIds.iterator();
    List<ITransaction> issues = new ArrayList<>(1);
    List<ITransaction> receipts = new ArrayList<>(1);
    PersistenceManager pm = PMF.get().getPersistenceManager();
    try {
      while (ids.hasNext()) {
        String key = ids.next();
        try {
          ITransaction trans = transDao.getById(key, pm);
          String type = trans.getType();
          if (ITransaction.TYPE_ISSUE.equals(type) || ITransaction.TYPE_WASTAGE.equals(type)) {
            issues.add(trans);
          } else if (ITransaction.TYPE_RECEIPT.equals(type)) {
            receipts.add(trans);
          } else {
            errorList.add(trans);
            xLogger
                .warn("Invalid transaction type {0} to attempt undo. for transaction with key {1}",
                    type, key);
          }
        } catch (JDOObjectNotFoundException e) {
          xLogger.warn("Could not find inventory transaction object with key: {0}", key);
        }
      }
      // Add receipts after issues, so that stock counts do not go negative when undoing transactions
      if (!receipts.isEmpty()) {
        issues.addAll(receipts);
      }
      Long domainId = null;
      if (!issues.isEmpty()) {
        domainId = issues.get(0).getDomainId();
      }
      // Undo transactions
      for (ITransaction t : issues) {
        String type = t.getType();
        Long kioskId = t.getKioskId();
        Long materialId = t.getMaterialId();
        try {
          IInvntry inv = getInventory(kioskId, materialId, pm);
          if (inv == null) {
            continue;
          }
          BigDecimal oldStock = inv.getStock();
          IInvntryBatch invBatch = null;
          if (t.hasBatch()) {
            invBatch = getInventoryBatch(kioskId, materialId, t.getBatchId(), pm);
          }
          boolean isUpdated = false;
          if (ITransaction.TYPE_ISSUE.equals(type) || ITransaction.TYPE_WASTAGE.equals(type)) {
            BigDecimal stock = inv.getStock().add(t.getQuantity());
            inv.setStock(stock);
            deleteList.add(t);
            if (invBatch != null) {
              invBatch.setQuantity(invBatch.getQuantity().add(t.getQuantity()));
            }
            isUpdated = true;
          } else if (ITransaction.TYPE_RECEIPT.equals(type)) {
            BigDecimal stock = inv.getStock().subtract(t.getQuantity());
            if (BigUtil.lesserThanZero(stock)) {
              stock = BigDecimal.ZERO;
            }
            inv.setStock(stock);
            deleteList.add(t);
            if (invBatch != null) {
              BigDecimal batchStock = invBatch.getQuantity().subtract(t.getQuantity());
              if (BigUtil.lesserThanZero(batchStock)) {
                batchStock = BigDecimal.ZERO;
              }
              invBatch.setQuantity(batchStock);
            }
            isUpdated = true;
          }
          inv.setUpdatedOn(now);
          inv.setUpdatedBy(SecurityUtils.getUsername());
          // If updated, create a new inventory log
          if (isUpdated) {
            logList.add(createInventoryLevelLog(inv, invBatch, now));
            updateList.add(inv);
            updateStockEventLog(oldStock, inv, pm, IInvntryEvntLog.SOURCE_STOCKUPDATE, domainId);
          }
        } catch (Exception e) {
          xLogger.warn("{0} when undoing transaction of type {1} for {2}-{3} in domain {4}: {5}",
              e.getClass().getName(), type, kioskId, materialId, t.getDomainId(), e.getMessage());
        }
      } // end while
      // Update the objects
      if (!deleteList.isEmpty()) {
        pm.deletePersistentAll(deleteList);
      }
      if (!logList.isEmpty()) {
        pm.makePersistentAll(logList);
      }
      if (!updateList.isEmpty()) {
        //Required for SQL.. else updates to stock after undo not reflecting.
        pm.makePersistentAll(updateList);
      }
    } finally {
      pm.close();
    }
    xLogger.fine("Existing undoTransactions");
    return errorList;
  }

  public float getKValue(float serviceLevel) {
    return (float) 2.72;
  }


  @SuppressWarnings("unchecked")
  @Override
  public int getOutOfStockCounts(Long entityId) {
    int count = 0;
    PersistenceManager pm = null;
    Query query = null;
    try {
      pm = PMF.get().getPersistenceManager();
      Map<String, Object> params = new HashMap<>(1);
      query =
          pm.newQuery("select key from " + JDOUtils.getImplClass(IInvntry.class).getName()
              + " where kId == kIdParam && stk == 0f PARAMETERS Long kIdParam");
      params.put("kIdParam", entityId);
      List<Long> results = (List<Long>) query.executeWithMap(params);
      if (results != null) {
        count = results.size();
      }
    } catch (Exception e) {
      xLogger.warn("Failed to get out of stock counts for Entity: {0}", entityId, e);
    } finally {
      if (query != null) {
        try {
          query.closeAll();
        } catch (Exception ignored) {
          xLogger.warn("Exception while closing query", ignored);
        }
      }
      if (pm != null) {
        pm.close();
      }
    }
    return count;
  }

  // Get the inventory given either of kioskId or materialId
  @Override
  public IInvntry getInventory(Long kioskId, Long materialId, PersistenceManager pm)
      throws ServiceException {
    IInvntry inv;
    try {
      inv = invntryDao.findId(kioskId, materialId, pm);
    } catch (JDOObjectNotFoundException e) {
      return null;
    }
    return inv;
  }


  private Results getInventoryByIds(Long kioskId, Long materialId, String kioskTag,
                                    String materialTag, List<Long> kioskIds, PageParams pageParams,
                                    PersistenceManager pm) throws ServiceException {
    return invntryDao
        .getInventory(
            new InventoryFilters().withKioskId(kioskId)
                .withMaterialId(materialId)
                .withKioskTags(kioskTag)
                .withMaterialTags(materialTag)
                .withKioskIds(kioskIds)
            , pageParams, pm);
  }


  public Results getInvntryByLocation(Long domainId, LocationSuggestionModel location,
                                      String kioskTags, String excludedKioskTags,
                                      String materialTags, String pdos,
                                      PageParams params) throws ServiceException {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    try {
      return invntryDao
          .getInventory(
              new InventoryFilters().withKioskTags(kioskTags)
                  .withExcludedKioskTags(excludedKioskTags)
                  .withMaterialTags(materialTags)
                  .withDomainId(domainId)
                  .withLocation(location)
                  .withPdos(pdos), params, pm);
    } finally {
      pm.close();
    }
  }


  // Check the quantitative errors in a given transaction; return an error message, or null if no errors
  private void checkTransactionErrors(BigDecimal stockOnHand, ITransaction trans)
      throws LogiException {
    BigDecimal quantity = trans.getQuantity();
    String transType = trans.getType();
    // Positive range check
    if (BigUtil.isInvalidQ(quantity)) {
      throw new LogiException("M001", quantity);
    }
    // Issue checks
    if (ITransaction.TYPE_ISSUE.equals(transType) || ITransaction.TYPE_WASTAGE.equals(transType)
        || ITransaction.TYPE_TRANSFER.equals(transType) || ITransaction.TYPE_RETURNS_OUTGOING.equals(transType)) {
      if (BigUtil.equalsZero(quantity)) {
        throw new LogiException("M001", quantity);
      }
      // Over issue check
      if (BigUtil.lesserThanZero(stockOnHand.subtract(quantity))) {
        throw new LogiException("M002", quantity, stockOnHand);
      }
      // Check of linked kiosk, if transfer
      if (ITransaction.TYPE_TRANSFER.equals(transType) && trans.getLinkedKioskId() == null) {
        throw new LogiException("M003", (Object[]) null);
      }
    } else if (ITransaction.TYPE_RECEIPT.equals(transType) || ITransaction.TYPE_ORDER
        .equals(transType) || ITransaction.TYPE_RETURNS_INCOMING.equals(transType)) {
      if (BigUtil.equalsZero(quantity)) {
        throw new LogiException("M001", quantity);
      }
    }

    IKiosk kiosk = entitiesService.getKiosk(trans.getKioskId(), false);
    IMaterial material = materialCatalogService.getMaterial(trans.getMaterialId());
    if (kiosk.isBatchMgmtEnabled() && material.isBatchEnabled() && !trans.hasBatch()) {
      throw new LogiException("M009", kiosk.getName(), material.getName());
    }
  }

  private List<Object> createTransactableObjects(ITransaction trans, IInvntry inv,
                                                 IInvntryBatch invBatch, Date timestamp,
                                                 PersistenceManager pm) throws ServiceException {
    return createTransactableObjects(trans, inv, invBatch, timestamp, pm, true);
  }

  // Create a list of trans, inv., inv. log objects that can be transacted together as an entity group of inv (parent)
  @SuppressWarnings({"rawtypes", "unchecked"})
  private List<Object> createTransactableObjects(ITransaction trans, IInvntry inv,
                                                 IInvntryBatch invBatch, Date timestamp,
                                                 PersistenceManager pm, boolean isBatchEnabled)
      throws ServiceException {
    BigDecimal stockOnHand = inv.getStock();
    // Update the inventory stock
    invBatch = updateStock(inv, invBatch, trans, timestamp, pm);
    // Create the inventory level log
    IInvntryLog iLog = createInventoryLevelLog(inv, invBatch, timestamp);
    // Create/update the inventory stock event log, if required
    IInvntryEvntLog lastStockEvent = invntryDao.getLastStockEvent(inv, pm);
    if (inv.getStockEvent() == -1) {
      updateLastStockEvent(inv, stockOnHand, lastStockEvent);
    } else {
      createNewStockEvent(inv, IInvntryEvntLog.SOURCE_STOCKUPDATE, lastStockEvent, pm);
    }
    // Collect the objects for updates
    if (!isBatchEnabled) {
      trans.setBatchId(null);
      trans.setBatchExpiry(null);
      trans.setBatchManufacturedDate(null);
      trans.setBatchManufacturer(null);
    }
    List<Object> objects = new ArrayList<>();
    objects.add(trans);
    objects.add(inv);
    objects.add(iLog);
    if (isBatchEnabled && invBatch != null) {
      objects.add(invBatch);
    } else if (ITransaction.TYPE_PHYSICALCOUNT.equals(trans.getType()) && BigUtil.equalsZero(trans
        .getQuantity())) { // If a zero stock count is done without batch, then ensure all existing batches are set to zero
      IMaterial
          m =
          JDOUtils.getObjectById(IMaterial.class, trans.getMaterialId(),
              pm); // get the material to check batch-enablement
      if (m.isBatchEnabled()) {
        List<IInvntryBatch> zeroedBatches = zeroExistingBatches(inv, pm);
        if (zeroedBatches != null && !zeroedBatches.isEmpty()) {
          objects.addAll(zeroedBatches);
        }
      }
    }
    return objects;
  }

  /**
   * Create transactable objects for a Transfer transaction
   *
   * @param lkInv - applicable for transfer
   * @param lkInvBatch - applicable for transfer
   */
  private List<Object> createTransactableObjectsForTransfer(ITransaction trans, IInvntry inv,
                                                            IInvntryBatch invBatch, IInvntry lkInv,
                                                            IInvntryBatch lkInvBatch,
                                                            Date timestamp,
                                                            PersistenceManager pm)
      throws ObjectNotFoundException, ServiceException {
    Long localts = System.currentTimeMillis() + 1;
    // Ensure issue and receipt have different timestamps, so they are sequenced appropriately in views.
    List<Object> objects = new ArrayList<>();
    // Update the opening/closing stock for the transfer transaction (same as current stock, since this trans. does not update stock directly)
    updateStockInTransferTrans(trans, inv, invBatch);
    objects.add(trans);
    // Create issues for this kiosk
    ITransaction issue = trans.clone();
    issue.setType(ITransaction.TYPE_ISSUE);
    if (!issue.useCustomTimestamp()) {
      issue.setTimestamp(new Date(
          localts)); // important to set new time, so that the time sequence of transfer, issue and receipt is preserved
    }
    transDao.setKey(issue);
    transDao.linkTransactions(issue, trans);
    // Create transactable objects
    objects.addAll(createTransactableObjects(issue, inv, invBatch, timestamp,
        pm)); // update stock and get related logs

    // Create a receipt for the destination kiosk
    ITransaction receipt = trans.clone();
    receipt.setType(ITransaction.TYPE_RECEIPT);
    receipt.setKioskId(trans.getLinkedKioskId());
    receipt.setLinkedKioskId(trans.getKioskId());
    if (!receipt.useCustomTimestamp()) {
      receipt.setTimestamp(new Date(localts + 1));
    }

    // Get the tags/domain for the linkedKiosk and set it here, if any
    boolean isBatchEnabled = true;
    try {
      IKiosk linkedKiosk = JDOUtils.getObjectById(IKiosk.class, trans.getLinkedKioskId(), pm);
      isBatchEnabled = linkedKiosk.isBatchMgmtEnabled();

      if (linkedKiosk.getTags() != null) {
        receipt.setTgs(tagDao.getTagsByNames(linkedKiosk.getTags(), ITag.KIOSK_TAG),
            TagUtil.TYPE_ENTITY);
      }
      // Check if the linked kiosk is of a different domain than the source kiosk (superdomain)
      if (!linkedKiosk.getDomainId().equals(receipt.getDomainId())) {
        receipt.setDomainId(linkedKiosk.getDomainId());
      }
    } catch (Exception e) {
      xLogger.warn("{0} when getting linked kiosk {1} during transfer: {2}", e.getClass().getName(),
          trans.getLinkedKioskId(), e.getMessage());
    }
    // Update key and links
    transDao.setKey(receipt);
    transDao.linkTransactions(receipt, trans);

    // Get inventory corresponding to receipt
    try {
      // Update transactable objects
      objects.addAll(
          createTransactableObjects(receipt, lkInv, lkInvBatch, timestamp, pm, isBatchEnabled));
    } catch (JDOObjectNotFoundException e) {
      throw new ObjectNotFoundException(
          "This material is not configured in the destination entity");
    }

    return objects;
  }

  // Update the opening/closing stock in a transfer transaction
  private void updateStockInTransferTrans(ITransaction trans, IInvntry inv,
                                          IInvntryBatch invBatch) {
    BigDecimal stock = inv.getStock();
    trans.setOpeningStock(stock);
    trans.setClosingStock(stock.subtract(trans.getQuantity()));
    if (invBatch != null) {
      BigDecimal stockInBatch = invBatch.getQuantity();
      trans.setOpeningStockByBatch(stockInBatch);
      trans.setClosingStockByBatch(stockInBatch.subtract(trans.getQuantity()));
    }
  }

  // Update the stock of a given inventory item (and transaction, in case of stock count)
  private IInvntryBatch updateStock(IInvntry in, IInvntryBatch invBatch, ITransaction trans,
                                    Date time, PersistenceManager pm) throws ServiceException {
    // Set the opening stock in trans.
    trans.setOpeningStock(in.getStock());
    if (invBatch != null) {
      trans.setOpeningStockByBatch(invBatch.getQuantity());
    } else {
      trans.setOpeningStockByBatch(BigDecimal.ZERO);
    }
    // Get trans. params.
    BigDecimal quantity = trans.getQuantity();
    String transType = trans.getType();
    if (ITransaction.TYPE_ISSUE.equals(transType) || ITransaction.TYPE_WASTAGE.equals(transType) || ITransaction.TYPE_RETURNS_OUTGOING.equals(transType)) {
      // Reduce the stock-on-hand by issued amount
      in.setStock(in.getStock().subtract(quantity));
      in.setAvailableStock(in.getAvailableStock().subtract(quantity));
      if (invBatch != null) {
        invBatch.setQuantity(invBatch.getQuantity().subtract(quantity));
        invBatch.setAvailableStock(invBatch.getAvailableStock().subtract(quantity));
      }
    } else if (ITransaction.TYPE_RECEIPT.equals(transType) || ITransaction.TYPE_RETURNS_INCOMING
        .equals(transType)) {
      // Increase the stock-on-hand by the received amount
      in.setStock(in.getStock().add(quantity));
      in.setAvailableStock(in.getAvailableStock().add(quantity));
      if (invBatch == null && trans.hasBatch()) {
        invBatch = createInventoryBatch(trans, in);
      }
      if (invBatch != null) {
        invBatch.setQuantity(invBatch.getQuantity().add(quantity));
        invBatch.setAvailableStock(invBatch.getAvailableStock().add(quantity));
        updateInventoryBatchMetadata(invBatch, trans);
      }
    } else if (ITransaction.TYPE_PHYSICALCOUNT.equals(transType)) {
      // Stock & Stock difference at inv. level
      BigDecimal invQuantity;
      BigDecimal stockDifference;
      BigDecimal invATP;
      // Check batch presence
      if (invBatch == null && trans.hasBatch()) {
        invBatch = createInventoryBatch(trans, in);
      }
      if (invBatch == null) {
        if (BigUtil.lesserThan(quantity, in.getAllocatedStock())) {
          clearAllocation(in.getKioskId(), in.getMaterialId(), null, null, null, null, pm, in,
              null);
          in.setAllocatedStock(BigDecimal.ZERO);
        }
        invQuantity = quantity;
        stockDifference = quantity.subtract(in.getStock());
        invATP = in.getAvailableStock().add(stockDifference);
      } else {
        if (BigUtil.lesserThan(quantity, invBatch.getAllocatedStock())) {
          clearAllocation(in.getKioskId(), in.getMaterialId(), null, null, null, trans.getBatchId(),
              pm, in, invBatch);
          in.setAllocatedStock(in.getAllocatedStock().subtract(invBatch.getAllocatedStock()));
          invBatch.setAllocatedStock(BigDecimal.ZERO);
        }
        stockDifference = quantity.subtract(invBatch.getQuantity());
        invBatch.setQuantity(quantity);
        invBatch.setAvailableStock(invBatch.getAvailableStock().add(stockDifference));
        invQuantity = in.getStock().add(stockDifference);
        invATP = in.getAvailableStock().add(stockDifference);
        if (BigUtil.greaterThan(invATP, invQuantity)) {
          xLogger.severe(
              "Available to promise {0} is greater than Inventory quantity {1} for material {2}" +
                  " and entity {3}", invATP, invQuantity, in.getMaterialId(), in.getKioskId());
          invATP = invQuantity;
        }
        updateInventoryBatchMetadata(invBatch, trans);
      }
      in.setStock(invQuantity);
      in.setAvailableStock(invATP);
      // Set the stock adjustment
      trans.setStockDifference(stockDifference);
    }
    in.setTimestamp(time);
    in.setUpdatedBy(SecurityUtils.getUsername());
    if (in.getInventoryActiveTime() == null) {
      in.setInventoryActiveTime(time);
    }
    checkAndResetInventoryLevels(in, invBatch);
    // Update closing stock in transactions
    trans.setClosingStock(in.getStock());
    if (invBatch != null) {
      trans.setClosingStockByBatch(invBatch.getQuantity());
      invBatch.setTimestamp(time);
    }

    return invBatch;
  }

  /**
   * resets and logs inventory and inventory batch to zero's if they go below zero.
   */
  private void checkAndResetInventoryLevels(IInvntry in, IInvntryBatch invBatch) {
    if (BigUtil.lesserThanZero(in.getStock())) { // don't let the main stock go below zero
      xLogger.severe(
          "INVERR001 Inventory for mat: {0} and ent : {1} went below zero {2}, resetting to zero",
          in.getMaterialId(), in.getKioskId(), in.getStock());
      in.setStock(BigDecimal.ZERO);
    }

    if (BigUtil.lesserThanZero(in.getAvailableStock())) {
      xLogger.severe(
          "INVERR002 Inventory  available stock for mat: {0} and ent : {1} went below zero {2}, " +
              "resetting to zero", in.getMaterialId(), in.getKioskId(), in.getAvailableStock());
      in.setAvailableStock(BigDecimal.ZERO);
    }

    if (BigUtil.lesserThanZero(in.getAllocatedStock())) {
      xLogger.severe(
          "INVERR002 Inventory allocated stock for mat: {0} and ent : {1} went below zero {2}, " +
              "resetting to zero", in.getMaterialId(), in.getKioskId(), in.getAllocatedStock());
      in.setAllocatedStock(BigDecimal.ZERO);
    }

    if (invBatch != null) {
      if (BigUtil.lesserThanZero(invBatch.getQuantity())) {
        xLogger.severe(
            "INVERR001 InvBatch {0} for mat: {1} and ent : {2} went below zero {3}, resetting to zero",
            invBatch.getBatchId(), in.getMaterialId(), in.getKioskId(), invBatch.getQuantity());
        invBatch.setQuantity(BigDecimal.ZERO);
      }

      if (BigUtil.lesserThanZero(invBatch.getAvailableStock())) {
        xLogger.severe(
            "INVERR001 InvBatch {0} available stock for mat: {1} and ent : {2} went below zero {3}(${4}),"
                +
                " resetting to zero", invBatch.getBatchId(), in.getMaterialId(), in.getKioskId()
            , invBatch.getAvailableStock(), invBatch.getQuantity());
        invBatch.setAvailableStock(BigDecimal.ZERO);
      }

      if (BigUtil.lesserThanZero(invBatch.getAllocatedStock())) {
        xLogger.severe(
            "INVERR002 InvBatch {0} allocated stock for mat: {1} and ent : {2} went below zero {3}(${4}),"
                +
                " resetting to zero", invBatch.getBatchId(), in.getMaterialId(), in.getKioskId()
            , invBatch.getAllocatedStock(), invBatch.getQuantity());
        invBatch.setAllocatedStock(BigDecimal.ZERO);
      }
    }
  }

  // Create the inventory log level object
  private IInvntryLog createInventoryLevelLog(IInvntry in, IInvntryBatch invBatch, Date time) {
    IInvntryLog logItem = JDOUtils.createInstance(IInvntryLog.class);
    Long kioskId = in.getKioskId();
    Long materialId = in.getMaterialId();
    logItem.setKioskId(kioskId);
    logItem.setMaterialId(materialId);
    logItem.setDomainId(in.getDomainId());
    logItem.setStock(in.getStock());
    logItem.setCreatedOn(time);
    invntryDao.setInvntryLogKey(logItem);
    if (invBatch != null) {
      logItem.setBatchData(invBatch.getBatchId(), invBatch.getQuantity());
    }

    return logItem;
  }

  // Create new inventory batch object
  private IInvntryBatch createInventoryBatch(ITransaction trans, IInvntry inv) {
    IInvntryBatch invBatch = JDOUtils.createInstance(IInvntryBatch.class);
    invBatch.setBatchExpiry(trans.getBatchExpiry());
    invBatch.setBatchId(trans.getBatchId());
    invBatch.setBatchManufacturedDate(trans.getBatchManufacturedDate());
    invBatch.setBatchManufacturer(trans.getBatchManufacturer());
    invBatch.setDomainId(trans.getDomainId());
    invBatch.addDomainIds(inv.getDomainIds());
    invBatch.setKioskId(trans.getKioskId(), inv.getKioskName());
    invBatch.setMaterialId(trans.getMaterialId());
    invntryDao.setInvBatchKey(invBatch);
    invBatch.setTimestamp(trans.getTimestamp());
    invBatch.setTgs(tagDao.getTagsByNames(inv.getTags(TagUtil.TYPE_ENTITY), ITag.KIOSK_TAG),
        TagUtil.TYPE_ENTITY);
    invBatch.setTgs(tagDao.getTagsByNames(inv.getTags(TagUtil.TYPE_MATERIAL), ITag.MATERIAL_TAG),
        TagUtil.TYPE_MATERIAL);
    return invBatch;
  }

  // Update an inventory batch with metadata from transaction
  private void updateInventoryBatchMetadata(IInvntryBatch invBatch, ITransaction trans) {
    invBatch.setBatchExpiry(trans.getBatchExpiry());
    invBatch.setBatchManufacturedDate(trans.getBatchManufacturedDate());
    invBatch.setBatchManufacturer(trans.getBatchManufacturer());
  }

  // Zero existing batches - i.e. set them to zero
  @SuppressWarnings("unchecked")
  private List<IInvntryBatch> zeroExistingBatches(IInvntry inv, PersistenceManager pm) {
    try {
      Results results = getValidBatches(inv.getMaterialId(), inv.getKioskId(), null, pm);
      if (results != null && results.getSize() > 0) {
        List<IInvntryBatch> list = results.getResults();
        Iterator<IInvntryBatch> it = list.iterator();
        Date now = inv.getTimestamp();
        while (it.hasNext()) {
          IInvntryBatch invBatch = it.next();
          invBatch.setQuantity(BigDecimal.ZERO);
          invBatch.setTimestamp(now);
        }
        return list;
      }
    } catch (Exception e) {
      xLogger.warn(
          "{0} when trying to get valid batches for kiosk-material {1}-{2} in domain {3} when zeroing existing batches: {4}",
          e.getClass().getName(), inv.getKioskId(), inv.getMaterialId(), inv.getDomainId(),
          e.getMessage());
    }
    return null;
  }

  private boolean updateLastStockEvent(IInvntry inv, BigDecimal oldStock,
                                       IInvntryEvntLog lastStockEvent) {
    xLogger.fine("Entered updateStockEventLog");
    BigDecimal newStock = inv.getStock();
    BigDecimal minStock = inv.getNormalizedSafetyStock();
    BigDecimal maxStock = inv.getMaxStock();
    xLogger.fine("oldStock: {3}, newStock: {0}, minStock: {1}, maxStock: {2}", newStock, minStock,
        maxStock, oldStock);

    if (lastStockEvent != null && lastStockEvent.isOpen()) {
      lastStockEvent.setEndDate(inv.getTimestamp());
      return true;
    }
    return false;
  }

  private boolean createNewStockEvent(IInvntry inv, int updateSource,
                                      IInvntryEvntLog lastStockEvent, PersistenceManager pm) {
    int type = inv.getStockEvent();
    boolean
        createNewEvent =
        !(updateSource == IInvntryEvntLog.SOURCE_MINMAXUPDATE && type == IEvent.STOCKOUT) &&
            (lastStockEvent == null || lastStockEvent.getType() != type || !lastStockEvent
                .isOpen());
    boolean
        closeLastEvent =
        lastStockEvent != null && lastStockEvent.getType() != type && lastStockEvent.isOpen();

    if (closeLastEvent) { // close the last stock event, if different from the current event (e.g. earlier it was 0, now it is <min)
      lastStockEvent.setEndDate(inv.getTimestamp());
    }
    if (createNewEvent) {
      invntryDao.createInvntryEvntLog(type, inv);
      //Required to save stock event to inventory
      pm.makePersistent(inv);
      return true;
    }
    return false;
  }

  // Create an inventory event log, if a stockout, under/over stock has happened. 'updateSource' is the source of the update, either IInvntryLog.SOURCE_MINMAXUPDATE or IInvntryLog.SOURCE_STOCKUPDATE
  // NOTE: Stock event state can move from normal to event, event to normal or event to event
  private void updateStockEventLog(BigDecimal oldStock, IInvntry inv, PersistenceManager pm,
                                   int updateSource, Long domainId) {

    IInvntryEvntLog lastStockEvent = invntryDao.getLastStockEvent(inv, pm);
    // Close the last open stock event, if no event
    if (inv.getStockEvent() == -1) {
      if (updateLastStockEvent(inv, oldStock, lastStockEvent)) {
        try {
          EventPublisher.generate(domainId, IEvent.STOCK_REPLENISHED, null,
              JDOUtils.getImplClass(IInvntry.class).getName(), invntryDao.getInvKeyAsString(inv),
              null);
        } catch (EventGenerationException e) {
          xLogger.warn(
              "EventHandlingException when generating stock-replenished event for material-kiosk {0}:{1} in domain {2}: {3}",
              inv.getMaterialId(), inv.getKioskId(), inv.getDomainId(), e.getMessage());
        }
      }
      return;
    }
    // Create or update inventory stock event log
    // create a new event, if there no stock event in the past, or the last stock event was of a different type, or
    // it was of the same type but closed, or just min/max was updated and stock was not zero
    if (createNewStockEvent(inv, updateSource, lastStockEvent, pm)) {
      try {
        EventPublisher
            .generate(domainId, inv.getStockEvent(), null,
                JDOUtils.getImplClass(IInvntry.class).getName(),
                invntryDao.getInvKeyAsString(inv), null);
      } catch (EventGenerationException e) {
        xLogger.warn(
            "EventHandlingException when generating stock-replenished event for material-kiosk {0}:{1} in domain {2}: {3}",
            inv.getMaterialId(), inv.getKioskId(), inv.getDomainId(), e.getMessage());
      }
    }
    xLogger.fine("Exiting updateStockEventLog");
  }

  /**
   * Generate the necessary inventory events
   * @param trans
   * @param inv
   * @param prevStock
   * @param linkedKioskInv
   * @param lkPrevStock
   * @param isStockUpdatedFirstTime
   * @param domainId
   */
  private void generateEvents(ITransaction trans, IInvntry inv, BigDecimal prevStock,
                              IInvntry linkedKioskInv, BigDecimal lkPrevStock,
                              boolean isStockUpdatedFirstTime, Long domainId) {
    if (!isTransactionTypeValid(trans.getType())) {
      throw new IllegalArgumentException("Invalid transaction type: " + trans.getType() + " while generating events");
    }
    int eventId = eventIdByTransType.containsKey(trans.getType()) ? eventIdByTransType.get(trans.getType()) : -1;
    Map<String, Object> params = new HashMap<>();
    // Transaction creation event
    if (eventId != -1) {
      try {
        if (StringUtils.isNotEmpty(trans.getReason())) {
          params.put(EventConstants.PARAM_REASON, trans.getReason());
        }
        if (StringUtils.isNotEmpty(trans.getMaterialStatus())) {
          params.put(EventConstants.PARAM_STATUS, trans.getMaterialStatus());
        }
        EventPublisher.generate(domainId, eventId, params,
            JDOUtils.getImplClassName(ITransaction.class), transDao.getKeyAsString(trans), null);
      } catch (EventGenerationException e) {
        xLogger.warn(
            "Exception when generating transaction event of type {0} for material-kiosk {1}:{2} in domain {3}",
            trans.getType(), trans.getMaterialId(), trans.getKioskId(), trans.getDomainId(),
            e);
      }
    }
    // Stock count differs from current stock
    if (trans.getType().equals(ITransaction.TYPE_PHYSICALCOUNT)) {
      BigDecimal stock = inv.getStock();
      if (BigUtil.notEqualsZero(stock) && BigUtil.greaterThanZero(prevStock)) {
        float
            stockCountThreshold =
            Math.abs((trans.getStockDifference().divide(stock, BigDecimal.ROUND_HALF_UP))
                .multiply(BigUtil.HUNDRED).floatValue());
        if (stockCountThreshold > 0) {
          params.put(EventConstants.PARAM_STOCKCOUNTTHRESHOLD,
              String.valueOf(stockCountThreshold));
          try {
            EventPublisher.generate(domainId, IEvent.STOCKCOUNT_EXCEEDS_THRESHOLD, params,
                JDOUtils.getImplClassName(ITransaction.class), transDao.getKeyAsString(trans),
                null);
          } catch (EventGenerationException e) {
            xLogger.warn(
                "EventHandlingException when generating stockcount-exceeds-threshold event of type {0} for material-kiosk {1}:{2} in domain {3}: {4}",
                trans.getType(), trans.getMaterialId(), trans.getKioskId(), trans.getDomainId(),
                e.getMessage());
          }
        }
      }
    }
    generateAbnormalStockEvent(trans, inv, prevStock, domainId, isStockUpdatedFirstTime);
    if (ITransaction.TYPE_TRANSFER.equals(trans.getType())) {
      generateAbnormalStockEvent(trans, linkedKioskInv, lkPrevStock, domainId,
          isStockUpdatedFirstTime);
    }
  }

  private void generateAbnormalStockEvent(ITransaction trans, IInvntry inv, BigDecimal prevStock,
                                          Long domainId, boolean isStockUpdatedFirstTime) {

    BigDecimal stock = inv.getStock();
    // Out of stock
    if (BigUtil.equalsZero(stock)) {
      try {
        EventPublisher.generate(domainId, IEvent.STOCKOUT, null,
            JDOUtils.getImplClassName(IInvntry.class), invntryDao.getInvKeyAsString(inv), null);
      } catch (EventGenerationException e) {
        xLogger.warn(
            "EventHandlingException when generating stock-out event for material-kiosk {0}:{1} in domain {2}: {3}",
            inv.getMaterialId(), inv.getKioskId(), inv.getDomainId(), e.getMessage());
      }
    }
    // Unsafe stock
    BigDecimal safetyStock = inv.getNormalizedSafetyStock();
    xLogger.fine(
        "Stock: {0}, safetyStock: {1}, prevStock: {2}, material: {3}, inv.safetyStock: {4}, inv.reorderLevel: {5}",
        stock, safetyStock, prevStock, trans.getMaterialId(), inv.getSafetyStock(),
        inv.getReorderLevel());
    if (inv.isStockUnsafe() && (BigUtil.equalsZero(prevStock) || BigUtil
        .greaterThan(prevStock, safetyStock))) {
      try {
        EventPublisher.generate(domainId, IEvent.UNDERSTOCK, null,
            JDOUtils.getImplClassName(IInvntry.class), invntryDao.getInvKeyAsString(inv), null);
      } catch (EventGenerationException e) {
        xLogger.warn(
            "EventHandlingException when generating unsafe-stock event for material-kiosk {0}:{1} in domain {2}: {3}",
            inv.getMaterialId(), inv.getKioskId(), inv.getDomainId(), e.getMessage());
      }
    }
    // Excess stock
    BigDecimal maxStock = inv.getMaxStock();
    if (inv.isStockExcess() && BigUtil.lesserThanEquals(prevStock, maxStock)) {
      try {
        EventPublisher.generate(domainId, IEvent.OVERSTOCK, null,
            JDOUtils.getImplClassName(IInvntry.class), invntryDao.getInvKeyAsString(inv), null);
      } catch (EventGenerationException e) {
        xLogger.warn(
            "EventHandlingException when generating over-stock event for material-kiosk {0}:{1} in domain {2}: {3}",
            inv.getMaterialId(), inv.getKioskId(), inv.getDomainId(), e.getMessage());
      }
    }
    // Stock back to normal levels
    boolean
        isPrevStockAbnormal =
        BigUtil.equalsZero(prevStock) || (BigUtil.greaterThanZero(safetyStock) && BigUtil
            .lesserThanEquals(prevStock, safetyStock)) || (BigUtil.greaterThanZero(maxStock)
            && BigUtil.greaterThan(prevStock, maxStock));
    boolean
        isCurStockAbnormal =
        BigUtil.equalsZero(stock) || inv.isStockUnsafe() || inv.isStockExcess();
    xLogger.fine("isCurStockAbnormal: {0}, isPrevStockAbnormal: {1}", isCurStockAbnormal,
        isPrevStockAbnormal);
    if (!isStockUpdatedFirstTime && isPrevStockAbnormal && !isCurStockAbnormal) {
      try {
        EventPublisher.generate(domainId, IEvent.STOCK_REPLENISHED, null,
            JDOUtils.getImplClassName(IInvntry.class), invntryDao.getInvKeyAsString(inv), null);
      } catch (EventGenerationException e) {
        xLogger.warn(
            "EventHandlingException when generating stock-replenished event for material-kiosk {0}:{1} in domain {2}: {3}",
            inv.getMaterialId(), inv.getKioskId(), inv.getDomainId(), e.getMessage());
      }
    }
  }

  public IInvntryBatch getInventoryBatch(Long kioskId, Long materialId, String batchId,
                                         PersistenceManager pm) {
    IInvntryBatch invBatch = null;
    boolean closePM = false;
    if (pm == null) {
      pm = PMF.get().getPersistenceManager();
      closePM = true;
    }
    try {
      invBatch = invntryDao.findInvBatch(kioskId, materialId, batchId, pm);
    } catch (JDOObjectNotFoundException e) {
      return null;
    } catch (Exception e) {
      xLogger
          .warn("{0} when getting batch {1} for inv. {2}-{3}: {4}", e.getClass().getName(), batchId,
              kioskId, materialId, e.getMessage(), e);
    } finally {
      if (closePM) {
        pm.close();
      }
    }
    return invBatch;
  }

  public Long getShortId(Long kioskId) {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    Query q = null;
    try {
      q = pm.newQuery(JDOUtils.getImplClass(IInvntry.class));
      q.setResult("max(sId)");
      q.setFilter("kId == " + kioskId);
      Long count = (Long) q.execute();
      return count == null ? -1 : count;
    } catch (Exception e) {
      xLogger.warn("Error while getting short id for kiosk {0}", kioskId, e);
      return null;
    } finally {
      if (q != null) {
        try {
          q.closeAll();
        } catch (Exception ignored) {
          xLogger.warn("Exception while closing query", ignored);
        }
      }
      if (pm != null) {
        pm.close();
      }
    }
  }

  public BigDecimal getStockAvailabilityPeriod(IInvntry inv, DomainConfig currentDC) {
    DomainConfig dc = DomainConfig.getInstance(inv.getDomainId());
    int consumptionRate = dc.getInventoryConfig().getConsumptionRate();
    String displayFreq = currentDC.getInventoryConfig().getDisplayCRFreq();
    String
        entFrequency =
        InventoryConfig.CR_MANUAL == consumptionRate ? dc.getInventoryConfig().getManualCRFreq()
            : null;
    return getStockAvailabilityPeriod(inv, consumptionRate, displayFreq, entFrequency);
  }

  private BigDecimal getStockAvailabilityPeriod(IInvntry inv, int crType, String dispFreq,
                                                String entFrequency) {
    if (crType == InventoryConfig.CR_NONE) {
      return BigDecimal.ZERO;
    }
    BigDecimal cr = getDailyConsumptionRate(inv, crType, entFrequency);
    if (Constants.FREQ_WEEKLY.equals(dispFreq)) {
      cr = cr.multiply(Constants.WEEKLY_COMPUTATION);
    } else if (Constants.FREQ_MONTHLY.equals(dispFreq)) {
      cr = cr.multiply(Constants.MONTHLY_COMPUTATION);
    }
    return getStockAvailabilityPeriod(cr, inv.getStock());
  }

  public BigDecimal getStockAvailabilityPeriod(BigDecimal cr, BigDecimal stk) {
    BigDecimal consumptionRate = BigUtil.getZeroIfNull(cr);
    if (BigUtil.greaterThanZero(consumptionRate)) {
      return stk.divide(consumptionRate, 3, BigDecimal.ROUND_HALF_UP);
    }
    return BigDecimal.ZERO;
  }

  public BigDecimal getDailyConsumptionRate(IInvntry inv) {
    DomainConfig dc = DomainConfig.getInstance(inv.getDomainId());
    int consumptionRate = dc.getInventoryConfig().getConsumptionRate();
    String
        frequency =
        InventoryConfig.CR_MANUAL == consumptionRate ? dc.getInventoryConfig().getManualCRFreq()
            : null;
    return getDailyConsumptionRate(inv, consumptionRate, frequency);
  }

  public BigDecimal getDailyConsumptionRate(IInvntry inv, int crType, String manualCRFrequency) {
    if (InventoryConfig.CR_MANUAL == crType) {
      BigDecimal crMnl = inv.getConsumptionRateManual();
      if (Constants.FREQ_WEEKLY.equals(manualCRFrequency)) {
        crMnl = crMnl.divide(Constants.WEEKLY_COMPUTATION, 3, BigDecimal.ROUND_HALF_UP);
      } else if (Constants.FREQ_MONTHLY.equals(manualCRFrequency)) {
        crMnl = crMnl.divide(Constants.MONTHLY_COMPUTATION, 3, BigDecimal.ROUND_HALF_UP);
      }
      return crMnl;
    } else if (InventoryConfig.CR_AUTOMATIC == crType) {
      return inv.getConsumptionRateDaily();
    }
    return null;
  }

  /**
   * Creates new instance for {@code IInventoryMinMaxLog} and initialize with given inventory {@code
   * inv}
   *
   * @param inv Inventory
   * @param type Min Max type [ absolute quantity:1 or duration of stock:2 ]
   * @return IInventoryMinMaxLog
   */
  @Override
  public IInventoryMinMaxLog createMinMaxLog(IInvntry inv, int type, int crType) {
    return createMinMaxLog(inv, type, crType, SourceConstants.USER);
  }

  /**
   * Creates new instance for {@code IInventoryMinMaxLog} and initialize with given inventory {@code
   * inv}
   *
   * @param inv Inventory
   * @param type Min Max type [ absolute quantity:1 or duration of stock:2 ]
   * @param source Whether min max updated by user or system
   * @return IInventoryMinMaxLog
   */
  public IInventoryMinMaxLog createMinMaxLog(IInvntry inv, int type, int crType, int source) {
    IInventoryMinMaxLog ip = JDOUtils.createInstance(IInventoryMinMaxLog.class);
    ip.setInventoryId(Long.valueOf(inv.getKeyString()));
    ip.setMaterialId(inv.getMaterialId());
    ip.setKioskId(inv.getKioskId());
    ip.setCreatedTime(new Date());
    if (InventoryConfig.CR_MANUAL == crType) {
      ip.setConsumptionRate(inv.getConsumptionRateManual());
    } else {
      ip.setConsumptionRate(inv.getConsumptionRateDaily());
    }
    ip.setMin(inv.getReorderLevel());
    ip.setMax(inv.getMaxStock());
    ip.setMinDuration(inv.getMinDuration());
    ip.setMaxDuration(inv.getMaxDuration());
    ip.setType(type);
    if (SourceConstants.USER == source) {
      ip.setUser(inv.getUpdatedBy());
    }
    ip.setSource(source);
    DomainConfig dc = DomainConfig.getInstance(inv.getDomainId());
    String fr = dc.getInventoryConfig().getMinMaxDur();
    if (InventoryConfig.FREQ_DAILY.equals(fr)) {
      ip.setMinMaxFrequency(IInventoryMinMaxLog.Frequency.DAILY);
    } else if (InventoryConfig.FREQ_WEEKLY.equals(fr)) {
      ip.setMinMaxFrequency(IInventoryMinMaxLog.Frequency.WEEKLY);
    } else if (InventoryConfig.FREQ_MONTHLY.equals(fr)) {
      ip.setMinMaxFrequency(IInventoryMinMaxLog.Frequency.MONTHLY);
    }
    return ip;
  }

  /**
   * Fetch the List<IInventoryMinMaxLog> for a given inventoryId
   */
  public List<IInventoryMinMaxLog> fetchMinMaxLog(String invId) {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    Query query = null;
    List<IInventoryMinMaxLog> logs = null;
    try {
      String sqlQuery = "SELECT * FROM INVENTORYMINMAXLOG WHERE invId =? ORDER BY T DESC limit 50";
      query = pm.newQuery("javax.jdo.query.SQL", sqlQuery);
      query.setClass(JDOUtils.getImplClass(IInventoryMinMaxLog.class));
      logs = (List<IInventoryMinMaxLog>) query.executeWithArray(invId);
      logs = (List<IInventoryMinMaxLog>) pm.detachCopyAll(logs);

    } catch (Exception e) {
      xLogger.warn("Exception {0} when getting InvntryItem for invId {1}. Message: {2}",
          e.getClass().getName(), invId, e.getMessage());
      return null;
    } finally {
      if (query != null) {
        query.closeAll();
      }
      pm.close();
    }

    return logs;
  }

  @Override
  public IInvntryEvntLog adjustInventoryEvents(IInvntryEvntLog iEvntLog) throws ServiceException {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    Transaction tx = null;
    try {
      tx = pm.currentTransaction();
      tx.begin();
      List<IInvntryEvntLog> logs = invntryDao.removeInvEventLogs(iEvntLog.getKioskId(),
          iEvntLog.getMaterialId(), iEvntLog.getStartDate(), iEvntLog.getEndDate(), pm);
      IInvntry invntry = invntryDao.findId(iEvntLog.getKioskId(), iEvntLog.getMaterialId(), pm);
      iEvntLog.setInvId(invntry.getKey());
      DomainsUtil.addToDomain(iEvntLog, iEvntLog.getDomainId(), pm);
      pm.makePersistent(iEvntLog);
      if (iEvntLog.getKey() == null) {
        pm.makePersistent(iEvntLog);
      }
      for (IInvntryEvntLog log : logs) {
        if (Objects.equals(log.getKey(), invntry.getLastStockEvent())) {
          invntry.setLastStockEvent(iEvntLog.getKey());
          pm.makePersistent(invntry);
          break;
        }
      }
      tx.commit();
    } catch (Exception e) {
      xLogger.severe("Failed to adjust inventory log events for {0}:{1}", iEvntLog.getKioskId(),
          iEvntLog.getDomainId(), e);
      throw new ServiceException(e);
    } finally {
      if (tx != null && tx.isActive()) {
        tx.rollback();
      }
      pm.close();
    }
    return iEvntLog;
  }

  /**
   * Checks for changes in min and max of given inventories, and logs it if finds any change.
   *
   * @param invs list of inventory to check for min and max change
   * @return true when all changes persisted successfully.
   */
  private boolean persistMinMaxLog(List<IInvntry> invs) {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    try {
      List<IInventoryMinMaxLog> logs = new ArrayList<>(invs.size());
      for (IInvntry inv : invs) {
        DomainConfig dc = DomainConfig.getInstance(inv.getDomainId());
        logs.add(createMinMaxLog(inv, dc.getInventoryConfig().getMinMaxType(),
            dc.getInventoryConfig().getConsumptionRate()));
      }
      if (!logs.isEmpty()) {
        pm.makePersistentAll(logs);
      }
      return true;
    } catch (Exception e) {
      xLogger.severe("Error while persisting logs for min and max history", e);
    } finally {
      pm.close();
    }
    return false;
  }

  /**
   * Checks if transaction type is valid
   *
   * @param tType type of transaction
   * @return true if the transaction type is present in the vldTransTypes set
   */
  boolean isTransactionTypeValid(String tType) {
    return tType != null && vldTransTypes.contains(tType);
  }

  @Override
  public Long getDurationFromRP(Long invId) {
    List<IInvntryEvntLog> invEventLogs = invntryDao.getInvntryEvntLog(invId, 2, 0);
    if (invEventLogs != null && !invEventLogs.isEmpty()) {
      IInvntryEvntLog currEvent = invEventLogs.get(0);
      if (currEvent.isOpen()) {
        switch (currEvent.getType()) {
          case IEvent.STOCKOUT:
            if (invEventLogs.size() > 1 && invEventLogs.get(1).getType() == IEvent.UNDERSTOCK) {
              return System.currentTimeMillis() - invEventLogs.get(1).getStartDate().getTime();
            } else {
              return System.currentTimeMillis() - currEvent.getStartDate().getTime();
            }
          case IEvent.UNDERSTOCK:
            return System.currentTimeMillis() - currEvent.getStartDate().getTime();
        }
      }
    }
    return -1l;
  }

  /**
   * Fetch allocations by tag
   *
   * @param tag Name of tag
   * @return -
   */
  @Override
  @SuppressWarnings("unchecked")
  public List<IInvAllocation> getAllocationsByTag(String tag) {
    return getAllocations(null, null, null, null, tag);
  }

  @Override
  public List<IInvAllocation> getAllocationsByTagMaterial(Long mId, String tag) {
    return getAllocations(null, mId, null, null, tag);
  }

  /**
   * invoi
   * Fetch lists of allocations by inventory
   *
   * @param kid Kiosk id
   * @param mid Material id
   * @return -
   */
  @Override
  @SuppressWarnings("unchecked")
  public List<IInvAllocation> getAllocations(Long kid, Long mid) {
    return getAllocations(kid, mid, null, null, null);
  }

  /**
   * Fetch an allocations by inventory of specific type
   *
   * @param kid Kiosk id
   * @param mid Material Id
   * @param type Type
   * @param typeId ID of Type object
   * @return -
   */
  @Override
  @SuppressWarnings("unchecked")
  public List<IInvAllocation> getAllocationsByTypeId(Long kid, Long mid, IInvAllocation.Type type,
                                                     String typeId) {
    return getAllocations(kid, mid, type, typeId, null);
  }

  private List<IInvAllocation> getAllocations(Long kid, Long mid, IInvAllocation.Type type,
                                              String typeId, String tag) {
    return getAllocations(kid, mid, type, typeId, tag, null, null);
  }

  @SuppressWarnings("unchecked")
  private List<IInvAllocation> getAllocations(Long kid, Long mid, IInvAllocation.Type type,
                                              String typeId, String tag, String batchId,
                                              PersistenceManager pm) {
    boolean useLocalpm = pm == null;
    if (useLocalpm) {
      pm = PMF.get().getPersistenceManager();
    }

    try {
      QueryParams queryParams =
          constructAllocationQuery(kid, mid, batchId, type, typeId, tag);
      return JDOUtils.getMultiple(queryParams, pm, null);

    } catch (Exception e) {
      xLogger.warn(
          "Error while fetching allocations. kid:{0}, mid:{1}, type: {2}, typeId: {3}, tag:{4}",
          kid, mid, type, typeId, tag, e);
    } finally {

      if (useLocalpm) {
        pm.close();
      }
    }
    return null;
  }

  @Override
  public void allocate(Long kid, Long mid, IInvAllocation.Type type, String typeId, String tag,
                       BigDecimal quantity,
                       List<ShipmentItemBatchModel> batchDetails, String userId,
                       PersistenceManager pm) throws ServiceException {
    allocate(kid, mid, type, typeId, tag, quantity, batchDetails, userId, pm, null);
  }

  /**
   * Allocate inventory by specified quantity. Either quantity or ShipmentItemBatchModel are
   * allowed, bot not both.
   *
   * @param kid Kiosk id
   * @param mid Material Id
   * @param type Type
   * @param typeId ID of type object
   * @param tag Tag
   * @param quantity Quantity to allocate, if non batch material
   * @param batchDetails Batch details with Batch ID, Quantity and other meta, if batch material
   * @param userId ID of user who is allocating the inventory
   */
  @Override
  public void allocate(Long kid, Long mid, IInvAllocation.Type type, String typeId, String tag,
                       BigDecimal quantity, List<ShipmentItemBatchModel> batchDetails,
                       String userId,
                       PersistenceManager pm, String materialStatus) throws ServiceException {
    if ((quantity == null && batchDetails == null) || (quantity != null && batchDetails != null)) {
      throw new IllegalArgumentException(
          "Invalid data. Allocation cannot happen. Both quantity and batch " +
              "details are not allowed and both cant be empty.");
    }
    boolean closePM = false;
    if (pm == null) {
      pm = PMF.get().getPersistenceManager();
      closePM = true;
    }
    try {
      IKiosk ksk = entitiesService.getKiosk(kid, false);
      IMaterial mat = materialCatalogService.getMaterial(mid);
      List<IInvAllocation> allocations;
      boolean isQuantityChanged = false;
      if (quantity != null) {
        IInvntry invntry = getInventory(kid, mid);
        if (invntry != null) {
          allocations = new ArrayList<>(1);
          IInvAllocation
              allocation =
              getOrCreateAllocation(kid, mid, null, type, typeId, userId, pm);
          if (BigUtil.equalsZero(quantity)) {
            clearAllocation(kid, mid, type, typeId);
          } else if (BigUtil.notEquals(quantity, allocation.getQuantity())) {
            BigDecimal allocDiff = quantity.subtract(allocation.getQuantity());
            if (BigUtil.greaterThanEquals(invntry.getAvailableStock(), allocDiff)) {
              incrementInvAllocation(allocation, allocDiff, tag, userId, null, pm);
              isQuantityChanged = true;
            } else {
              String kiosk;
              String materialName;
              try {
                kiosk = getKioskName(kid);
              } catch (Exception e) {
                kiosk = "(" + kid + ")";
              }
              try {
                materialName = getMaterialName(mid);
              } catch (Exception e) {
                materialName = "(" + mid + ")";
              }
              throw new InventoryAllocationException("IA001", kiosk,
                  materialName, invntry.getAvailableStock(), allocDiff);
            }
          }
          if (isQuantityChanged || !Objects
              .equals(materialStatus, allocation.getMaterialStatus())) {
            allocation.setMaterialStatus(materialStatus);
            allocations.add(allocation);
          }
        } else {
          throw new InventoryAllocationException("I001", ksk.getName(), mat.getName());
        }
      } else {
        allocations = new ArrayList<>(batchDetails.size());
        for (ShipmentItemBatchModel batchDetail : batchDetails) {
          IInvAllocation allocation = getOrCreateAllocation(kid, mid, batchDetail.id, type, typeId,
              userId, pm);
          if (batchDetail.q == null) {
            batchDetail.q = BigDecimal.ZERO;
          }
          if (BigUtil.equalsZero(batchDetail.q)) {
            List<String> typeIds = Collections.singletonList(typeId);
            clearAllocation(kid, mid, type, typeIds, tag, allocation.getBatchId(), pm);
          } else {
            if (BigUtil.notEquals(batchDetail.q, allocation.getQuantity())) {
              IInvntryBatch batch = getInventoryBatch(kid, mid, batchDetail.id, pm);
              BigDecimal allocDiff = batchDetail.q.subtract(allocation.getQuantity());
              if (BigUtil.greaterThanEquals(batch.getQuantity(), allocDiff)) {
                incrementInvAllocation(allocation, allocDiff, tag, userId, null, pm);
                isQuantityChanged = true;
              } else {
                String kiosk;
                String material;
                try {
                  kiosk = getKioskName(kid);
                } catch (Exception e) {
                  kiosk = "(" + kid + ")";
                }
                try {
                  material = getMaterialName(mid);
                } catch (Exception e) {
                  material = "(" + mid + ")";
                }
                throw new InventoryAllocationException("IA002", kiosk, material, batch.getBatchId(),
                    batch.getAvailableStock(), allocDiff);
              }
            }
            if (isQuantityChanged || !Objects
                .equals(batchDetail.smst, allocation.getMaterialStatus())) {
              allocation.setMaterialStatus(batchDetail.smst);
              allocations.add(allocation);
            }
          }
        }
      }
      pm.makePersistentAll(allocations);
    } catch (InventoryAllocationException se) {
      throw se;
    } catch (Exception e) {
      xLogger.severe("Error while allocating inventory", e);
      throw new ServiceException("System error while allocating inventory", e);
    } finally {
      if (closePM) {
        pm.close();
      }
    }
  }

  @Override
  public void allocateAutomatically(Long kid, Long mid, IInvAllocation.Type type, String typeId,
                                    String tag,
                                    BigDecimal quantity, String userId, boolean autoAssignStatus,
                                    PersistenceManager pm)
      throws ServiceException {
    boolean useLocalPM = pm == null;

    Transaction tx = null;
    try {
      IInvntry inv = getInventory(kid, mid);
      if (inv == null || BigUtil.lesserThan(inv.getAvailableStock(), quantity)) {
        throw new InventoryAllocationException(
            "Available stock " + (inv != null ? inv.getAvailableStock() : null)
                + " is less than requested " +
                "quantity " + quantity + " for kiosk " + kid + " and material " + mid);
      }
      if (useLocalPM) {
        pm = PMF.get().getPersistenceManager();
        tx = pm.currentTransaction();
        tx.begin();
      }

      final IMaterial material = materialCatalogService.getMaterial(mid);

      boolean isBatch = material.isBatchEnabled();

      if (isBatch) {
        isBatch = entitiesService.getKiosk(kid).isBatchMgmtEnabled();
      }
      String matStatus = null;
      if (autoAssignStatus) {
        DomainConfig dc = DomainConfig.getInstance(inv.getDomainId());
        matStatus =
            dc.getInventoryConfig().getFirstMaterialStatus(material.isTemperatureSensitive());
      }
      if (isBatch) {
        List<IInvAllocation> bAllocations = getAllocations(kid, mid, type, typeId, tag);
        BigDecimal existingAllocation = BigDecimal.ZERO;
        Map<String, IInvAllocation> ba = new HashMap<>(bAllocations.size());
        for (IInvAllocation bAllocation : bAllocations) {
          ba.put(bAllocation.getBatchId(), bAllocation);
          existingAllocation = existingAllocation.add(bAllocation.getQuantity());
        }
        quantity = quantity.subtract(existingAllocation);
        List<ShipmentItemBatchModel> shipmentModel = new ArrayList<>(1);

        Results<IInvntryBatch> rs = getValidBatches(mid, kid, null);
        List<IInvntryBatch> batches = rs.getResults();
        for (IInvntryBatch ib : batches) {
          if (ib.isExpired()) {
            continue;
          }
          ShipmentItemBatchModel model = new ShipmentItemBatchModel();
          model.id = ib.getBatchId();
          if (BigUtil.lesserThanEquals(quantity, ib.getAvailableStock())) {
            model.q = quantity;
          } else {
            model.q = ib.getAvailableStock();
          }
          quantity = quantity.subtract(model.q);
          if (ba.containsKey(model.id)) {
            model.q = model.q.add(ba.get(model.id).getQuantity());
            model.smst = ba.get(model.id).getMaterialStatus();
          } else if (autoAssignStatus) {
            model.smst = matStatus;
          }
          shipmentModel.add(model);
          if (BigUtil.equalsZero(quantity)) {
            break;
          }
        }
        if (shipmentModel.size() > 0 && BigUtil.equalsZero(quantity)) {
          allocate(kid, mid, type, typeId, tag, null, shipmentModel, userId, pm);
        }
      } else {
        if (autoAssignStatus) {
          IInvAllocation allocation = getInvAllocation(kid, mid, null, type, typeId, pm);
          if (allocation != null && BigUtil.greaterThanZero(allocation.getQuantity())) {
            matStatus = allocation.getMaterialStatus();
          }
        }
        allocate(kid, mid, type, typeId, tag, quantity, null, userId, pm, matStatus);
      }
      if (useLocalPM) {
        tx.commit();
      }
    } catch (InventoryAllocationException e) {
      throw e;
    } catch (Exception e) {
      xLogger.warn("Error while auto allocating quantity", e);
      throw new ServiceException("Error occured while auto allocation", e);
    } finally {
      if (useLocalPM && tx != null && tx.isActive()) {
        tx.rollback();
      }
      if (useLocalPM && pm != null) {
        pm.close();
      }
    }
  }

  /**
   * Clears the allocation from inventory by type
   *
   * @param kid Kiosk Id
   * @param mid Material Id
   * @param type Type
   * @param typeId ID of type object
   */
  @Override
  public void clearAllocation(Long kid, Long mid, IInvAllocation.Type type, String typeId)
      throws ServiceException {
    clearAllocation(kid, mid, type, Collections.singletonList(typeId), null, null, null);
  }

  public void clearAllocation(Long kid, Long mid, IInvAllocation.Type type, String typeId,
                              PersistenceManager persistenceManager) throws ServiceException {
    clearAllocation(kid, mid, type, Collections.singletonList(typeId), null, null,
        persistenceManager);
  }

  @Override
  public void clearBatchAllocation(Long kid, Long mid, IInvAllocation.Type type, String typeId,
                                   String batchId,
                                   PersistenceManager persistenceManager) throws ServiceException {
    clearAllocation(kid, mid, type, Collections.singletonList(typeId), null, batchId,
        persistenceManager);
  }

  /**
   * Clears the allocation by tag
   *
   * @param kid Kiosk Id
   * @param mid Material Id
   * @param tag Tag
   */
  @Override
  public void clearAllocationByTag(Long kid, Long mid, String tag) throws ServiceException {
    clearAllocation(kid, mid, null, null, tag, null, null);
  }

  @Override
  public void clearAllocationByTag(Long kid, Long mid, String tag, PersistenceManager pm)
      throws ServiceException {
    clearAllocation(kid, mid, null, null, tag, null, pm);
  }

  private void clearAllocation(Long kid, Long mid, IInvAllocation.Type type, List<String> typeIds,
                               String tag,
                               String batchId, PersistenceManager pm) throws ServiceException {
    clearAllocation(kid, mid, type, typeIds, tag, batchId, pm, null, null);
  }


  private void clearAllocation(Long kid, Long mid, IInvAllocation.Type type, List<String> typeIds,
                               String tag,
                               String batchId, PersistenceManager pm, IInvntry inv,
                               IInvntryBatch invBatch)
      throws ServiceException {
    boolean closePM = pm == null;
    if (closePM) {
      pm = PMF.get().getPersistenceManager();
    }
    try {
      List<IInvAllocation> allAllocations = new ArrayList<>();
      if (typeIds == null) {
        List<IInvAllocation> allocations = getAllocations(kid, mid, null, null, tag, batchId, pm);
        if (allocations != null) {
          allAllocations.addAll(allocations);
        }
      } else {
        for (String typeId : typeIds) {
          List<IInvAllocation>
              allocations =
              getAllocations(kid, mid, type, typeId, tag, batchId, pm);
          if (allocations != null) {
            allAllocations.addAll(allocations);
          }
        }
      }
      if (allAllocations.size() > 0) {
        for (IInvAllocation alc : allAllocations) {
          // Check if inventory exists
          if (getInventory(alc.getKioskId(), alc.getMaterialId(), pm) != null) {
            incrementInventoryAvailableQuantity(alc.getKioskId(), alc.getMaterialId(),
                alc.getBatchId(), alc.getQuantity(), pm, inv, invBatch, true);
          } else {
            xLogger.warn(
                "Inventory does not exist for kiosk ID {0}, material ID: {1}. Cannot incrementInventoryAvailableQuantity",
                alc.getKioskId(), alc.getMaterialId());
          }
          xLogger.info("Inventory allocation queued for deletion for kiosk {0} and material {1} with type {2} and typeid {3}"
              , alc.getKioskId(), alc.getMaterialId(), type, StringUtil.getCSV(typeIds));
        }
        pm.deletePersistentAll(allAllocations);
        xLogger.info("No of Inventory allocations {0} deleted successfully ", allAllocations.size());
      }
    } catch (InventoryAllocationException ie) {
      throw ie;
    } catch (Exception e) {
      xLogger
          .warn("Error while clearing allocations. kid:{0}, mid{1}, type:{2}, typeIds{3}, tag {4}",
              kid, mid,
              type, typeIds, tag, e);
      throw new ServiceException(e);
    } finally {
      if (closePM) {
        pm.close();
      }
    }
  }

  @Override
  public void transferAllocation(Long kid, Long mid, IInvAllocation.Type srcType, String srcTypeId,
                                 IInvAllocation.Type destType, String destTypeId,
                                 BigDecimal quantity,
                                 List<ShipmentItemBatchModel> batchDetails, String userId,
                                 String tag,
                                 PersistenceManager pm, String materialStatus, boolean isSet)
      throws ServiceException {
    if ((quantity == null && batchDetails == null) || (quantity != null && batchDetails != null)) {
      throw new IllegalArgumentException(
          "Invalid data. Transfer allocation cannot happen. Both quantity " +
              "and batch details are not allowed.");
    }
    boolean closePM = false;
    if (pm == null) {
      pm = PMF.get().getPersistenceManager();
      closePM = true;
    }
    try {
      List<IInvAllocation> allocations;
      if (quantity != null) {
        allocations = new ArrayList<>(2);
        transferAllocate(kid, mid, null, srcType, srcTypeId, destType, destTypeId, quantity, userId,
            allocations, tag, pm, isSet);
        for (IInvAllocation invAllocation : allocations) {
          if (invAllocation.getType().equals(destType.toString())) {
            invAllocation.setMaterialStatus(materialStatus);
            break;
          }
        }
      } else {
        allocations = new ArrayList<>(batchDetails.size() * 2);
        for (ShipmentItemBatchModel batchDetail : batchDetails) {
          transferAllocate(kid, mid, batchDetail.id, srcType, srcTypeId, destType, destTypeId,
              batchDetail.q, userId, allocations, tag, pm, isSet);
        }
        for (ShipmentItemBatchModel batchDetail : batchDetails) {
          for (IInvAllocation invAllocation : allocations) {
            if (batchDetail.id.equals(invAllocation.getBatchId()) && invAllocation.getType()
                .equals(destType.toString())) {
              invAllocation.setMaterialStatus(batchDetail.smst);
              break;
            }
          }
        }
      }

      pm.makePersistentAll(allocations);
      xLogger.info("Inventory allocations transferred for kiosk {0}, material {1}, source type {2}"
          + " ,source type id {3}, dest type {4} , dest type id {5}", kid, mid, srcType, srcTypeId, destType, destTypeId);
    } catch (ServiceException ie) {
      throw ie;
    } catch (Exception e) {
      xLogger.severe("Error while transferring allocated inventory", e);
      throw new ServiceException("System error while transferring allocated inventory");
    } finally {
      if (closePM) {
        pm.close();
      }
    }
  }

  private void transferAllocate(Long kid, Long mid, String bid, IInvAllocation.Type srcType,
                                String srcTypeId,
                                IInvAllocation.Type destType, String destTypeId, BigDecimal q,
                                String userId,
                                List<IInvAllocation> allocations, String tag, PersistenceManager pm,
                                boolean isSet)
      throws Exception {
    if (BigUtil.equalsZero(q)) {
      return;
    }
    IInvAllocation
        srcAllocation =
        getOrCreateAllocation(kid, mid, bid, srcType, srcTypeId, userId, pm);
    IInvAllocation
        destAllocation =
        getOrCreateAllocation(kid, mid, bid, destType, destTypeId, userId, pm);
    if (isSet && BigUtil.greaterThanZero(destAllocation.getQuantity())) {
      q = q.subtract(destAllocation.getQuantity());
    }
    BigDecimal reqQtyFromInv = null;
    if (BigUtil.greaterThanZero(q)) {
      reqQtyFromInv = q.subtract(srcAllocation.getQuantity()).max(BigDecimal.ZERO);
    }
    List<String> tags = srcAllocation.getTags();
    decrementInvAllocation(srcAllocation, q, (tags != null && tags.size() > 0) ? tags.get(0) : tag,
        userId);
    allocations.add(srcAllocation);
    incrementInvAllocation(destAllocation, q, (tags != null && tags.size() > 0) ? tags.get(0) : tag,
        userId, reqQtyFromInv, pm);
    allocations.add(destAllocation);
  }

  private IInvAllocation getOrCreateAllocation(Long kid, Long mid, String bid,
                                               IInvAllocation.Type type,
                                               String typeId, String userId,
                                               PersistenceManager pm) {
    IInvAllocation allocation = getInvAllocation(kid, mid, bid, type, typeId, pm);
    if (allocation == null) {
      allocation = createAllocateInstance(kid, mid, bid, type, typeId, userId);
    }
    return allocation;
  }

  private IInvAllocation getInvAllocation(Long kid, Long mid, String bid, IInvAllocation.Type type,
                                          String typeId, PersistenceManager pm) {
    QueryParams query = constructAllocationQuery(kid, mid, bid, type, typeId, null);
    return JDOUtils.getSingle(query, pm, null);
  }

  private QueryParams constructAllocationQuery(Long kid, Long mid, String bid,
                                               IInvAllocation.Type type,
                                               String typeId, String tag) {
    StringBuilder query = new StringBuilder("SELECT * FROM INVALLOCATION WHERE ");
    final String AND = " AND ";
    boolean firstCondition = true;
    List<String> parameters = new ArrayList<>(1);
    if (kid != null) {
      query.append(" KID =").append(CharacterConstants.QUESTION);
      parameters.add(String.valueOf(kid));
      firstCondition = false;
    }
    if (mid != null) {
      query.append(firstCondition ? CharacterConstants.EMPTY : AND).append("MID =")
          .append(CharacterConstants.QUESTION);
      parameters.add(String.valueOf(mid));
      firstCondition = false;
    }
    if (type != null) {
      query.append(firstCondition ? CharacterConstants.EMPTY : AND).append("TYPE=").append(
          CharacterConstants.QUESTION);
      parameters.add(String.valueOf(type));
      firstCondition = false;
    }
    if (StringUtils.isNotBlank(typeId)) {
      query.append(firstCondition ? CharacterConstants.EMPTY : AND).append("TYPEID=")
          .append(CharacterConstants.QUESTION);
      parameters.add(String.valueOf(typeId));
      firstCondition = false;
    }
    if (StringUtils.isNotBlank(bid)) {
      query.append(firstCondition ? CharacterConstants.EMPTY : AND).append("BID = ").append(
          CharacterConstants.QUESTION);
      parameters.add(bid);
      firstCondition = false;
    }
    if (StringUtils.isNotBlank(tag)) {
      query.append(firstCondition ? CharacterConstants.EMPTY : AND)
          .append("ID IN (SELECT ID_OID FROM INV_ALLOC_TAG WHERE TAG =").append(
          CharacterConstants.QUESTION).append(")");
      parameters.add(tag);
    }
    return new QueryParams(query.toString(), parameters,
        QueryParams.QTYPE.SQL, IInvAllocation.class);
  }

  private IInvAllocation createAllocateInstance(Long kid, Long mid, String bId,
                                                IInvAllocation.Type type, String typeId,
                                                String userId) {
    IInvAllocation allocation = JDOUtils.createInstance(IInvAllocation.class);
    allocation.setMaterialId(mid);
    allocation.setKioskId(kid);
    allocation.setBatchId(bId);
    allocation.setQuantity(BigDecimal.ZERO);
    allocation.setType(type.toString());
    allocation.setTypeId(typeId);
    Date now = new Date();
    allocation.setCreatedBy(userId);
    allocation.setCreatedOn(now);
    return allocation;
  }

  private void incrementInvAllocation(IInvAllocation allocation, BigDecimal quantity, String tag,
                                      String userId,
                                      BigDecimal invAllocQty, PersistenceManager pm)
      throws Exception {
    if (tag != null) {
      allocation.setTags(Collections.singletonList(tag));
    }
    allocation.setQuantity(allocation.getQuantity().add(quantity));
    allocation.setUpdatedBy(userId);
    allocation.setUpdatedOn(new Date());
    if (invAllocQty == null) {
      invAllocQty = quantity;
    }
    incrementInventoryAvailableQuantity(allocation.getKioskId(), allocation.getMaterialId(),
        allocation.getBatchId(), invAllocQty.negate(), pm);
  }

  private void decrementInvAllocation(IInvAllocation allocation, BigDecimal quantity, String tag,
                                      String userId) throws Exception {
    if (tag != null) {
      allocation.setTags(Collections.singletonList(tag));
    }
    BigDecimal avlToAllocate = allocation.getQuantity().min(quantity);
    if (BigUtil.greaterThanZero(avlToAllocate)) {
      allocation.setQuantity(allocation.getQuantity().subtract(avlToAllocate));
      allocation.setUpdatedBy(userId);
      allocation.setUpdatedOn(new Date());
    }
  }

  private void incrementInventoryAvailableQuantity(Long kioskId, Long materialId, String batchId,
                                                   BigDecimal quantity,
                                                   PersistenceManager pm) throws Exception {
    incrementInventoryAvailableQuantity(kioskId, materialId, batchId, quantity, pm, null, null,
        false);
  }

  public void incrementInventoryAvailableQuantity(Long kioskId, Long materialId, String batchId,
                                                  BigDecimal quantity,
                                                  PersistenceManager pm, IInvntry inv,
                                                  IInvntryBatch invBatch, boolean isClear)
      throws Exception {
    if (BigUtil.equalsZero(quantity)) {
      return;
    }
    boolean closePM = pm == null;
    if (closePM) {
      pm = PMF.get().getPersistenceManager();
    }
    try {
      if (inv == null) {
        inv = getInventory(kioskId, materialId, pm);
      }
      if (inv == null) {
        throw new ServiceException("I002", getMaterialName(materialId), getKioskName(kioskId));
      }
      BigDecimal newQuantity = inv.getAvailableStock().add(quantity);
      boolean isCorrected = false;
      if (BigUtil.lesserThanZero(newQuantity) || BigUtil.greaterThan(newQuantity, inv.getStock())) {
        if (isClear) {
          xLogger.warn(
              "Resetting available stock for material {0} at entity {1} inv(stk,astk,atpstk,q): " +
                  "inv({2},{3},{4},{5}", getMaterialName(inv.getMaterialId()), getKioskName(inv.getKioskId()), inv.getStock(),
              inv.getAllocatedStock(), inv.getAvailableStock(), quantity);
          newQuantity = BigUtil.lesserThanZero(newQuantity) ? BigDecimal.ZERO : inv.getStock();
          isCorrected = true;
        } else {
          throw new InventoryAllocationException("IA001", SecurityUtils.getLocale(), getKioskName(kioskId),
              getMaterialName(materialId), inv.getAvailableStock(), quantity.abs());
        }
      }
      inv.setAvailableStock(newQuantity);
      inv.setAllocatedStock(inv.getAllocatedStock().subtract(quantity));
      if (BigUtil.greaterThan(inv.getAllocatedStock(), inv.getStock()) || BigUtil
          .lesserThanZero(newQuantity)) {
        if (isClear) {
          xLogger.warn(
              "Resetting allocated stock for material {0} at entity {1} inv(stk,astk,atpstk,q): " +
                  "inv({2},{3},{4},{5}", getMaterialName(inv.getMaterialId()), getKioskName(inv.getKioskId()), inv.getStock(),
              inv.getAllocatedStock(), inv.getAvailableStock(), quantity);
          if (isCorrected) {
            inv.setAllocatedStock(inv.getStock().subtract(inv.getAvailableStock()));
          } else {
            inv.setAllocatedStock(inv.getStock());
          }
        } else {
          throw new InventoryAllocationException("IA003", SecurityUtils.getLocale(), getKioskName(inv.getKioskId()),
              getMaterialName(inv.getMaterialId()), inv.getAllocatedStock(), quantity.abs(), inv.getStock());
        }
      } else if (isClear && BigUtil.lesserThanZero(inv.getAllocatedStock())) {
        xLogger.warn(
            "Resetting allocated stock to ZERO for material {0} at entity {1} inv(stk,astk,atpstk,q): "
                +
                "inv({2},{3},{4},{5}", getMaterialName(inv.getMaterialId()), getKioskName(inv.getKioskId()), inv.getStock(),
            inv.getAllocatedStock(), inv.getAvailableStock(), quantity);
        inv.setAllocatedStock(BigDecimal.ZERO);
      }
      if (batchId != null) {
        isCorrected = false;
        if (invBatch == null) {
          invBatch = getInventoryBatch(inv.getKioskId(), inv.getMaterialId(), batchId, pm);
        }
        if (invBatch == null) {
          throw new InvalidServiceException("Error while getting inventory batch for allocation");
        }
        newQuantity = invBatch.getAvailableStock().add(quantity);
        if (BigUtil.greaterThan(newQuantity, invBatch.getQuantity())) {
          if (isClear) {
            xLogger.warn(
                "Resetting available stock for material {0} - batch {6} at entity {1} invbatch(stk,astk,atpstk,q): "
                    +
                    "invbatch({2},{3},{4},{5}", getMaterialName(inv.getMaterialId()), getKioskName(inv.getKioskId()),
                invBatch.getQuantity(),
                invBatch.getAllocatedStock(), invBatch.getAvailableStock(), quantity,
                invBatch.getBatchId());
            newQuantity = invBatch.getQuantity();
            isCorrected = true;
          } else {
            throw new InventoryAllocationException("IA002", SecurityUtils.getLocale(), getKioskName(inv.getKioskId()),
                getMaterialName(inv.getMaterialId()), invBatch.getBatchId(),
                invBatch.getAvailableStock(), newQuantity);
          }
        } else if (isClear && BigUtil.lesserThanZero(newQuantity)) {
          xLogger.warn(
              "Resetting available stock to ZERO for material {0} - batch {6} at entity {1} invbatch(stk,astk,atpstk,q): "
                  +
                  "invbatch({2},{3},{4},{5}", getMaterialName(inv.getMaterialId()), getKioskName(inv.getKioskId()),
              invBatch.getQuantity(),
              invBatch.getAllocatedStock(), invBatch.getAvailableStock(), quantity,
              invBatch.getBatchId());
          newQuantity = BigDecimal.ZERO;
          isCorrected = true;
        }

        invBatch.setAvailableStock(newQuantity);
        invBatch.setAllocatedStock(invBatch.getAllocatedStock().subtract(quantity));
        if (BigUtil.greaterThan(invBatch.getAllocatedStock(), invBatch.getQuantity())) {
          if (isClear) {
            xLogger.warn(
                "Resetting allocated stock for material {0} - batch {6} at entity {1} invbatch(stk,astk,atpstk,q): "
                    +
                    "invbatch({2},{3},{4},{5}", getMaterialName(inv.getMaterialId()), getKioskName(inv.getKioskId()),
                invBatch.getQuantity(),
                invBatch.getAllocatedStock(), invBatch.getAvailableStock(), quantity,
                invBatch.getBatchId());
            if (isCorrected) {
              invBatch
                  .setAllocatedStock(invBatch.getQuantity().subtract(invBatch.getAvailableStock()));
            } else {
              invBatch.setAllocatedStock(invBatch.getQuantity());
            }
          } else {
            throw new InventoryAllocationException("IA004", SecurityUtils.getLocale(), getKioskName(inv.getKioskId()),
                getMaterialName(inv.getMaterialId()), invBatch.getBatchId(), invBatch.getAllocatedStock(),
                invBatch.getQuantity());
          }
        } else if (isClear && BigUtil.lesserThanZero(invBatch.getAllocatedStock())) {
          xLogger.warn(
              "Resetting allocated stock to ZERO for material {0} - batch {6} at entity {1} invbatch(stk,astk,atpstk,q): "
                  +
                  "invbatch({2},{3},{4},{5}", getMaterialName(inv.getMaterialId()), getKioskName(inv.getKioskId()),
              invBatch.getQuantity(),
              invBatch.getAllocatedStock(), invBatch.getAvailableStock(), quantity,
              invBatch.getBatchId());
          invBatch.setAllocatedStock(BigDecimal.ZERO);
        }
        pm.makePersistent(invBatch);
      }
      inv.setUpdatedOn(new Date());
      inv.setUpdatedBy(SecurityUtils.getUsername());
      pm.makePersistent(inv);
    } finally {
      if (closePM) {
        pm.close();
      }
    }
  }

  private String getMaterialName(Long materialId) throws ServiceException {
    return materialCatalogService.getMaterial(materialId).getName();
  }

  private String getKioskName(Long kioskId) throws ServiceException {
    return entitiesService.getKiosk(kioskId, false).getName();
  }

  private boolean closeOpenEvent(Long invId, PersistenceManager pm) {
    boolean useLocalPm = pm == null;
    if (useLocalPm) {
      pm = PMF.get().getPersistenceManager();
    }
    Query query = null;
    try {
      query = pm.newQuery("javax.jdo.query.SQL",
          "SELECT * FROM INVNTRYEVNTLOG WHERE INVID = ? AND ED IS NULL");
      query.setClass(JDOUtils.getImplClass(IInvntryEvntLog.class));
      query.setUnique(true);
      IInvntryEvntLog invntryEvntLog = (IInvntryEvntLog) query.execute(invId);
      if (invntryEvntLog != null) {
        // Update the end date to now
        invntryEvntLog.setEndDate(new Date());
        pm.makePersistent(invntryEvntLog);
      }
    } catch (Exception e) {
      xLogger.warn(
          "Exception while closing open event for inventory item, invId: {0}", invId, e);
      return false;
    } finally {
      if (query != null) {
        query.closeAll();
      }
      if (useLocalPm) {
        pm.close();
      }
    }
    return true;
  }

  public Results getInventory(Long domainId, Long kioskId, List<Long> kioskIds, String kioskTags,
                              String excludedKioskTags,
                              Long materialId, String materialTag, int matType,
                              boolean onlyNonZeroStk, String pdos, LocationSuggestionModel location,
                              PageParams params) throws ServiceException {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    Results results = null;
    try {
      results =
          invntryDao.getInventory(new InventoryFilters().withDomainId(domainId)
              .withKioskId(kioskId)
              .withKioskIds(kioskIds)
              .withKioskTags(kioskTags)
              .withExcludedKioskTags(excludedKioskTags)
              .withMaterialId(materialId)
              .withMaterialTags(materialTag)
              .withMatType(matType)
              .withOnlyNonZeroStk(onlyNonZeroStk)
              .withPdos(pdos)
                  .withLocation(location)
              , params, pm);
    } finally {
      pm.close();
    }
    return results;
  }

  @Override
  public Results<IInvntry> getInventory(InventoryFilters filters, PageParams pageParams)
      throws ServiceException {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    Results<IInvntry> results = null;
    try {
      results =
          invntryDao.getInventory(filters, pageParams, pm);
    } finally {
      pm.close();
    }
    return results;
  }

  public boolean validateEntityBatchManagementUpdate(Long kioskId) throws ServiceException {
    if (kioskId == null) {
      throw new ServiceException(
          "Invalid or null kioskId while changing batch management on entity");
    }
    boolean allowUpdate = false;
    PersistenceManager pm = PMF.get().getPersistenceManager();
    try {
      allowUpdate = invntryDao.validateEntityBatchManagementUpdate(kioskId, pm);
    } finally {
      pm.close();
    }
    return allowUpdate;
  }

  public Map<Long, ResponseDetailModel> updateMultipleInventoryTransactions(
      Map<Long, List<ITransaction>> materialTransactionsMap, Long domainId, String userId)
      throws ServiceException {
    try {
      return updateMultipleInventoryTransactionsAction.execute(materialTransactionsMap,
          domainId, userId);
    } catch (ConfigurationException e) {
      throw new ServiceException(e);
    }
  }


  public boolean validateMaterialBatchManagementUpdate(Long materialId) throws ServiceException {
    if (materialId == null) {
      throw new ServiceException(
          "Invalid or null materialId while changing batch management on material");
    }
    PersistenceManager pm = PMF.get().getPersistenceManager();
    boolean allowUpdate = false;
    try {
      allowUpdate = invntryDao.validateMaterialBatchManagementUpdate(materialId, pm);
    } finally {
      pm.close();
    }
    return allowUpdate;
  }

  public BigDecimal getUnusableStock(Long kId, Long mId) throws ServiceException {
    if (kId == null || mId == null) {
      xLogger.warn("Either Kiosk ID or material ID is null, kid: {0}, mid: {1}", kId, mId);
      return BigDecimal.ZERO;
    }
    Map<Date, BigDecimal> expDateBatchQtyMap = getExpDateBatchQtyMap(kId, mId);
    if (expDateBatchQtyMap == null || expDateBatchQtyMap.isEmpty()) {
      return BigDecimal.ZERO;
    }
    IInvntry inv = getInventory(kId, mId);
    return calculateUnconsumableStock(expDateBatchQtyMap, inv.getConsumptionRateDaily());
  }

  private Map<Date, BigDecimal> getExpDateBatchQtyMap(Long kId, Long mId) throws ServiceException {
    IInvntry inv = getInventory(kId, mId);
    Long domainId = inv.getDomainId();
    DomainConfig dc = DomainConfig.getInstance(domainId);
    BigDecimal orderPeriodicity = inv.getOrderPeriodicity();
    if (BigUtil.equalsZero(orderPeriodicity)) {
      orderPeriodicity = new BigDecimal(dc.getOptimizerConfig().getMinAvgOrderPeriodicity());
    }
    Date currentDate = new Date();
    Date nextOrderDate = LocalDateUtil.getOffsetDate(currentDate, orderPeriodicity.intValue());
    String nextOrderDateStr = LocalDateUtil.formatDateForQueryingDatabase(nextOrderDate);
    if (StringUtils.isEmpty(nextOrderDateStr)) {
      xLogger.warn(
          "Invalid nextOrderDateStr: {0} when trying to calculate unusable stock for kId: {1}, mid: {2}",
          nextOrderDateStr, kId, mId);
      return null;
    }
    List<String> parameters = new ArrayList<>(1);
    StringBuilder sqlQuery = new StringBuilder(
        "SELECT BEXP, SUM(Q) FROM INVNTRYBATCH WHERE KID = ?");
    parameters.add(String.valueOf(kId));
    sqlQuery.append(" AND MID = ?");
    parameters.add(String.valueOf(mId));
    sqlQuery.append(" AND BEXP <= ?");
    parameters.add(nextOrderDateStr);
    sqlQuery.append(" GROUP BY BEXP ORDER BY BEXP ASC");
    PersistenceManager pm = PMF.get().getPersistenceManager();
    Query query = pm.newQuery("javax.jdo.query.SQL", sqlQuery.toString());
    Map<Date, BigDecimal> expDateBatchQtyMap = null;
    try {
      List queryResults = (List) query.executeWithArray(parameters.toArray());
      if (queryResults != null && !queryResults.isEmpty()) {
        Iterator iterator = queryResults.iterator();
        expDateBatchQtyMap = new LinkedHashMap<>(1);
        while (iterator.hasNext()) {
          Object[] objArray = (Object[]) iterator.next();
          Date expDate = (Date) objArray[0];
          BigDecimal expStockQty = (BigDecimal) objArray[1];
          expDateBatchQtyMap.put(expDate, expStockQty);
        }
      }
    } catch (Exception e) {
      xLogger.warn("Error while getting expiry date/batch quantity map for kid: {0}, mid: {1}", kId,
          mId, e);
    } finally {
      query.closeAll();
      pm.close();
    }
    return expDateBatchQtyMap;
  }

  private BigDecimal calculateUnconsumableStock(Map<Date, BigDecimal> expDateBatchQtyMap,
                                                BigDecimal c) {
    // CD = CONSUMPTION_DAYS (days required to consume the stock)
    // BQ = BATCH QUANTITY
    // C = CONSUMPTION RATE
    // UCD = UNCONSUMABLE DAYS (days where stock cannot be consumed)
    // UCS = Un consumable stock (Stock remaining after expiry)
    // CSD = CONSUMPTION START DATE
    // FDRC = FIRST_DAY_REMAINING_CONSUMPTION
    Date csd = new Date();
    BigDecimal fdrc = c;
    BigDecimal bq;
    BigDecimal ucs = BigDecimal.ZERO;

    Set<Date> keySet = expDateBatchQtyMap.keySet();
    for (Date ed : keySet) {
      bq = expDateBatchQtyMap.get(ed);
      int cd;
      if (BigUtil.equalsZero(c)) {
        ucs = ucs.add(bq);
        continue;
      }
      if (BigUtil.lesserThan(fdrc, c)) {
        if (BigUtil.lesserThan(bq, fdrc)) {
          fdrc = fdrc.subtract(bq);
          continue;
        } else {
          bq = bq.subtract(fdrc);
        }
      }
      BigDecimal[] quoAndRem = bq.divideAndRemainder(c);
      cd = quoAndRem[0].intValue();
      BigDecimal remainderStock = quoAndRem[1];
      float ucd = cd - (Math.abs(LocalDateUtil.daysBetweenDates(ed, csd)) + 1);
      if (ucd > 0) {
        ucs = ucs.add(
            bq.subtract(
                c.multiply(new BigDecimal(Math.abs(LocalDateUtil.daysBetweenDates(ed, csd)) + 1))));
        csd = LocalDateUtil.getOffsetDate(ed, 1);
        fdrc = BigDecimal.ZERO;
      } else {
        csd = LocalDateUtil.getOffsetDate(csd, cd);
        fdrc = new BigDecimal(c.subtract(remainderStock).toBigInteger());
      }
    }
    return ucs;
  }

  /**
   * Method to get the count of unique materials for a given domain id
   *
   * @param domainId Domain Id
   * @return Count of unique materials
   */
  public Long getInvMaterialCount(Long domainId, Long tagId) throws ServiceException {
    if (domainId == null) {
      return 0L;
    }
    StringBuilder
        queryBuilder =
        new StringBuilder(
            "SELECT COUNT(DISTINCT(MID)) FROM INVNTRY WHERE KID IN (SELECT KIOSKID_OID "
                + "FROM KIOSK_DOMAINS WHERE DOMAIN_ID=?)");
    Long count;
    try {
      List<String> parameters = new ArrayList<>(2);
      PersistenceManager pm = PMF.get().getPersistenceManager();
      Query query;
      if (tagId != null) {
        queryBuilder.append(" AND MID IN (SELECT MATERIALID FROM MATERIAL_TAGS WHERE ID=?)");
        parameters.add(String.valueOf(tagId));
      }
      query = pm.newQuery("javax.jdo.query.SQL", queryBuilder.toString());
      parameters.add(String.valueOf(domainId));
      query.setUnique(true);
      count = (Long) query.executeWithArray(parameters.toArray());
    } catch (Exception e) {
      xLogger.warn("Exception fetching the count", e);
      throw new ServiceException("Failed to fetch count");
    }
    return count;
  }

  private void setInventoryFields(IInvntry in, IInvntry inventory, String user) {
    in.setInventoryModel(inventory.getInventoryModel());
    in.setServiceLevel(inventory.getServiceLevel());
    in.setLeadTime(inventory.getLeadTime());
    in.setLeadTimeDemand(inventory.getLeadTimeDemand());
    in.setRevPeriodDemand(inventory.getRevPeriodDemand());
    in.setSafetyStock(inventory.getSafetyStock());
    in.setStdevRevPeriodDemand(inventory.getStdevRevPeriodDemand());
    in.setOrderPeriodicity(inventory.getOrderPeriodicity());
    in.setKioskName(inventory.getKioskName());
    in.setMaterialName(inventory.getMaterialName());
    in.setReorderLevel(inventory.getReorderLevel());
    in.setMaxStock(inventory.getMaxStock());
    in.setMinDuration(inventory.getMinDuration());
    in.setMaxDuration(inventory.getMaxDuration());
    in.setConsumptionRateManual(inventory.getConsumptionRateManual());
    // Prices
    in.setRetailerPrice(inventory.getRetailerPrice());
    in.setTax(inventory.getTax());
    // Optimization parameters
    in.setConsumptionRateDaily(inventory.getConsumptionRateDaily());
    in.setEconomicOrderQuantity(inventory.getEconomicOrderQuantity());
    in.setPSTimestamp(inventory.getPSTimestamp());
    in.setDQTimestamp(inventory.getDQTimestamp());
    in.setOptMessage(inventory.getOptMessage());
    in.setTgs(tagDao.getTagsByNames(inventory.getTags(TagUtil.TYPE_MATERIAL), ITag.MATERIAL_TAG),
        TagUtil.TYPE_MATERIAL);
    in.setTgs(tagDao.getTagsByNames(inventory.getTags(TagUtil.TYPE_ENTITY), ITag.KIOSK_TAG),
        TagUtil.TYPE_ENTITY);
    if (user != null) {
      in.setUpdatedBy(user);
    }
    in.setUpdatedOn(new Date());
  }

  public Optional<ReturnsConfig> getReturnsConfig(Long entityId) {
    if (entityId == null) {
      throw new IllegalArgumentException(
          "Invalid input parameter while building return configuration: Entity id is not available");
    }
    try {
      SecureUserDetails sUser = SecurityUtils.getUserDetails();
      IKiosk kiosk = entitiesService.getKiosk(entityId, false);
      Long domainId = kiosk.getDomainId();
      if (domainId == null) {
        Locale locale = sUser.getLocale();
        ResourceBundle backendMessages = Resources.getBundle(locale);
        xLogger.severe("Error while fetching Return configuration");
        throw new InvalidServiceException(
            backendMessages
                .getString("error.fetching.returns.configuration") + CharacterConstants.COLON + entityId);
      }
      DomainConfig dc = DomainConfig.getInstance(domainId);
      List<String> kioskTags = kiosk.getTags();
      return dc.getInventoryConfig().getReturnsConfig(kioskTags);
    } catch (ServiceException e) {
      xLogger.warn("Exception while getting entity with id: {0}", entityId, e);
    }
    return Optional.empty();
  }

  public List<IInventoryMinMaxLog> fetchMinMaxLogByInterval(Long entityId, Long materialId, Date
      fromDate, Date toDate) throws ServiceException {
    if (entityId == null || materialId == null || fromDate == null || toDate == null) {
      throw new IllegalArgumentException("Invalid arguments while fetching min max log");
    }
    PersistenceManager pm = getPM();
    Query query = null;
    List<IInventoryMinMaxLog> logs = null;
    try {
      String sqlQuery = "SELECT * FROM INVENTORYMINMAXLOG WHERE KID = :kidParam AND "
          + "MID = :midParam AND T BETWEEN :fromParam AND :toParam UNION ALL (SELECT * "
          + "FROM INVENTORYMINMAXLOG WHERE KID = :kidParam AND MID = :midParam AND "
          + "T < :fromParam ORDER BY T DESC LIMIT 1) ORDER BY T DESC";
      query = pm.newQuery("javax.jdo.query.SQL", sqlQuery);
      query.setClass(JDOUtils.getImplClass(IInventoryMinMaxLog.class));
      Map parameters = new HashMap<>(4);
      parameters.put("kidParam", String.valueOf(entityId));
      parameters.put("midParam", String.valueOf(materialId));
      SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATETIME_CSV_FORMAT);
      parameters.put("fromParam", sdf.format(fromDate));
      parameters.put("toParam", sdf.format(toDate));
      logs = (List) query.executeWithMap(parameters);
      logs = (List<IInventoryMinMaxLog>) pm.detachCopyAll(logs);
    } catch (Exception e) {
      xLogger.warn("Exception {0} when getting InventoryMinMaxLog for entityId {1}, materialId {2}, "
              + "fromDate: {3}, toDate: {4}. Message: {5}",
          e.getClass().getName(), entityId, materialId, fromDate, toDate, e.getMessage(), e);
      throw new ServiceException(e);
    } finally {
      if (query != null) {
        query.closeAll();
      }
      pm.close();
    }

    return logs;
  }

  public PersistenceManager getPM() {
    return PMF.get().getPersistenceManager();
  }




}
