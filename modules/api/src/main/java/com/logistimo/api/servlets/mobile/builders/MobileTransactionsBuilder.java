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

package com.logistimo.api.servlets.mobile.builders;

import com.google.gson.Gson;

import com.logistimo.api.util.RESTUtil;
import com.logistimo.constants.Constants;
import com.logistimo.dao.JDOUtils;
import com.logistimo.entities.entity.IKiosk;
import com.logistimo.entities.service.EntitiesService;
import com.logistimo.inventory.TransactionUtil;
import com.logistimo.inventory.entity.IInvntry;
import com.logistimo.inventory.entity.ITransaction;
import com.logistimo.inventory.models.ErrorDetailModel;
import com.logistimo.inventory.models.ResponseDetailModel;
import com.logistimo.inventory.models.SuccessDetailModel;
import com.logistimo.inventory.service.InventoryManagementService;
import com.logistimo.logger.XLog;
import com.logistimo.materials.entity.IMaterial;
import com.logistimo.materials.service.MaterialCatalogService;
import com.logistimo.proto.MobileGeoModel;
import com.logistimo.proto.MobileInvModel;
import com.logistimo.proto.MobileMaterialTransModel;
import com.logistimo.proto.MobileTransErrModel;
import com.logistimo.proto.MobileTransErrorDetailModel;
import com.logistimo.proto.MobileTransModel;
import com.logistimo.proto.MobileTransSuccessDetailModel;
import com.logistimo.proto.MobileTransSuccessModel;
import com.logistimo.proto.MobileTransactionModel;
import com.logistimo.proto.MobileTransactionsModel;
import com.logistimo.proto.MobileUpdateInvTransRequest;
import com.logistimo.proto.MobileUpdateInvTransResponse;
import com.logistimo.services.ServiceException;
import com.logistimo.tags.TagUtil;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.users.service.UsersService;
import com.logistimo.utils.LocalDateUtil;
import com.logistimo.utils.StringUtil;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by vani on 18/01/17.
 */
@Component
public class MobileTransactionsBuilder {
  private static final XLog xLogger = XLog.getLog(MobileTransactionsBuilder.class);

  private static final int SUCCESS = 0;
  private static final int ERROR = 1;
  private static final int PARTIAL_ERROR = 2;

  private static final String VERSION_01 = "01";

  private EntitiesService entitiesService;
  private UsersService usersService;
  private MaterialCatalogService materialCatalogService;
  private InventoryManagementService inventoryManagementService;
  private MobileInventoryBuilder mobileInventoryBuilder;

  @Autowired
  public void setEntitiesService(EntitiesService entitiesService) {
    this.entitiesService = entitiesService;
  }

  @Autowired
  public void setUsersService(UsersService usersService) {
    this.usersService = usersService;
  }

  @Autowired
  public void setMaterialCatalogService(MaterialCatalogService materialCatalogService) {
    this.materialCatalogService = materialCatalogService;
  }

  @Autowired
  public void setInventoryManagementService(InventoryManagementService inventoryManagementService) {
    this.inventoryManagementService = inventoryManagementService;
  }

  @Autowired
  public void setMobileInventoryBuilder(MobileInventoryBuilder mobileInventoryBuilder) {
    this.mobileInventoryBuilder = mobileInventoryBuilder;
  }

  /**
   * Builds a transactions request object as required by the mobile from a json string
   *
   * @param updateInvTransReqJson - JSON string containing the transactions request as sent by the mobile
   * @return - MobileUpdateInvTransRequest object
   */
  public MobileUpdateInvTransRequest buildMobileUpdateInvTransRequest(
      String updateInvTransReqJson) {
    return new Gson().fromJson(updateInvTransReqJson,
        MobileUpdateInvTransRequest.class);
  }


  public MobileTransactionsModel build(List<ITransaction> transactions, Long kioskId, Locale locale,
                                       String timezone) {
    MobileTransactionsModel mtsm = new MobileTransactionsModel();
    mtsm.kid = kioskId;
    if (transactions == null || transactions.isEmpty()) {
      return mtsm;
    }
    List<MobileTransactionModel> mtmList = new ArrayList<>(transactions.size());
    for (ITransaction transaction : transactions) {
      IMaterial m;
      try {
        m = materialCatalogService.getMaterial(transaction.getMaterialId());
      } catch (ServiceException e) {
        xLogger.warn("Exception while getting material for material ID {0}",
            transaction.getMaterialId()); // Material may have been deleted, skip this transaction
        continue;
      }
      MobileTransactionModel mtm = new MobileTransactionModel();
      if (!RESTUtil.materialExistsInKiosk(transaction.getKioskId(), transaction.getMaterialId())) {
        mtm.mnm = m.getName();
      }
      mtm.mid = transaction.getMaterialId();
      mtm.ty = transaction.getType();
      mtm.q = transaction.getQuantity();
      mtm.t = LocalDateUtil.format(transaction.getTimestamp(), locale, timezone);
      if (StringUtils.isNotEmpty(transaction.getReason())) {
        mtm.rsn = transaction.getReason();
      }
      mtm.ostk = transaction.getOpeningStock();
      mtm.cstk = transaction.getClosingStock();
      mtm.uid = transaction.getSourceUserId();
      try {
        IUserAccount u = usersService.getUserAccount(mtm.uid);
        mtm.u = u.getFullName();
      } catch (Exception e) {
        xLogger.warn("Exception while getting user name for userId: {0}: ",
            mtm.uid, e);
      }
      mtm.lkid = transaction.getLinkedKioskId();
      if (mtm.lkid != null) {
        try {
          IKiosk lk = entitiesService.getKiosk(mtm.lkid, false);
          mtm.lknm = lk.getName();
        } catch (Exception e) {
          xLogger.warn("Exception while getting kiosk name for linked kiosk Id: {0}: ",
              mtm.lkid, e);
        }
      }
      if (StringUtils.isNotEmpty(transaction.getBatchId())) {
        mtm.bid = transaction.getBatchId();
        mtm.ostkb = transaction.getOpeningStockByBatch();
        if (transaction.getBatchExpiry() != null) {
          mtm.bexp =
              LocalDateUtil.formatCustom(transaction.getBatchExpiry(), Constants.DATE_FORMAT, null);
        }
        mtm.bmfnm = transaction.getBatchManufacturer();
        if (transaction.getBatchManufacturedDate() != null) {
          mtm.bmfdt =
              LocalDateUtil
                  .formatCustom(transaction.getBatchManufacturedDate(), Constants.DATE_FORMAT,
                      null);
        }
        mtm.cstkb = transaction.getClosingStockByBatch();
      }
      if (StringUtils.isNotEmpty(transaction.getMaterialStatus())) {
        mtm.mst = transaction.getMaterialStatus();
      }
      if (transaction.getAtd() != null) {
        mtm.atd = LocalDateUtil.formatCustom(transaction.getAtd(), Constants.DATE_FORMAT, null);
      }
      if (transaction.getTrackingObjectType() != null) {
        mtm.troty = transaction.getTrackingObjectType();
      }
      if (transaction.getTrackingId() != null) {
        mtm.trid = transaction.getTrackingId();
      }
      List<String> mTags = transaction.getTags(TagUtil.TYPE_MATERIAL);
      mtm.tg = StringUtil.getCSV(mTags);
      // If entry time is null set the svtm field to transaction timestamp
      mtm.svtm =
          transaction.getEntryTime() != null ? transaction.getEntryTime().getTime()
              : transaction.getTimestamp().getTime();
      mtm.key = transaction.getKeyString();

      // Add mtm to mtmList
      mtmList.add(mtm);
    }
    mtsm.trn = mtmList;
    mtsm.kid = kioskId;
    return mtsm;
  }

  /**
   * Builds the mobile update inventory transactions response object
   * @param domainId
   * @param userId
   * @param kioskId
   * @param errorMessage
   * @param responseDetailModelByMaterial
   * @param mobMatTransList
   * @return
   */
  public MobileUpdateInvTransResponse buildMobileUpdateInvTransResponse(Long domainId,
                                                                           String userId, Long kioskId, String partialId,
                                                                           String errorMessage,
                                                                           Map<Long,ResponseDetailModel> responseDetailModelByMaterial,
                                                                           List<MobileMaterialTransModel> mobMatTransList, boolean isDuplicate) {
    if (domainId == null) {
      return null;
    }
    MobileUpdateInvTransResponse mobUpdateInvTransResp = null;
    try {
      mobUpdateInvTransResp = new MobileUpdateInvTransResponse();
      mobUpdateInvTransResp.v = VERSION_01;
      if (StringUtils.isNotEmpty(errorMessage)) {
        mobUpdateInvTransResp.st = ERROR;
        mobUpdateInvTransResp.ms = errorMessage;
      } else if (isSuccess(responseDetailModelByMaterial)) {
        mobUpdateInvTransResp.st = SUCCESS;
      } else {
        if (!isDuplicate) {
          mobUpdateInvTransResp.st = PARTIAL_ERROR;
        } else {
          mobUpdateInvTransResp.st = SUCCESS;
        }
      }
      mobUpdateInvTransResp.kid = kioskId;
      Map<Long,List<SuccessDetailModel>> successDetailModelsByMaterial = getSuccessDetailModelsByMaterial(
          responseDetailModelByMaterial);
      Map<Long,List<ErrorDetailModel>> errorDetailModelsByMaterial = getErrorDetailModelsByMaterial(
          responseDetailModelByMaterial);
      if (mobUpdateInvTransResp.st == ERROR || mobUpdateInvTransResp.st == PARTIAL_ERROR) {
        mobUpdateInvTransResp.errs = buildErrorModelList(errorDetailModelsByMaterial);
      }
      List<MobileTransSuccessModel> successDetailModels = buildSuccessModels(
          successDetailModelsByMaterial);
      mobUpdateInvTransResp.success = CollectionUtils.isEmpty(successDetailModels) ? null : successDetailModels;

      mobUpdateInvTransResp.inv =
          buildMobileInvModelList(domainId, userId, kioskId, mobMatTransList);
      mobUpdateInvTransResp.pid = partialId;
    } catch (Exception e) {
      xLogger
          .warn("Exception while building mobile update inventory transactions response model", e);
    }
    return mobUpdateInvTransResp;
  }

 /**
   * Builds the mobile inventory model
   * @param domainId
   * @param userId
   * @param kioskId
   * @param mobMatTransList
   * @return
   */
 public List<MobileInvModel> buildMobileInvModelList(Long domainId, String userId, Long kioskId,
                                                      List<MobileMaterialTransModel> mobMatTransList) {
   if (mobMatTransList == null || mobMatTransList.isEmpty()) {
     return Collections.emptyList();
   }
   Set<Long> mids = mobMatTransList.stream()
            .map(mobileMaterialTransModel->mobileMaterialTransModel.mid)
            .collect(Collectors.toSet());
   List<MobileInvModel> mobInvList = new ArrayList<>();
   for (Long mid : mids) {
     IInvntry inventory;
     try {
       inventory = inventoryManagementService.getInventory(kioskId, mid);
       if (inventory == null) {
          xLogger.warn("Inventory does not exist for kid {0} and mid {1}", kioskId, mid);
          continue;
       }
     } catch (ServiceException e) {
        xLogger.warn("Exception while getting inventory for kid: {0}, mid: {1}", kioskId, mid, e);
        continue;
     }
     MobileInvModel
         inv =
         mobileInventoryBuilder.buildMobileInvModel(inventory, domainId, userId);
     if (inv != null) {
       mobInvList.add(inv);
     }
   }
   return mobInvList;
 }

  /**
   * Builds a map of material to list of server transaction objects from list of mobile transaction objects
   *
   * @param userId                    User ID of the user sending the mobile transactions
   * @param kid                       Kiosk ID of the kiosk for which the transactions are sent
   * @param mobileMaterialTransModels List of mobile transactions grouped by material
   * @return - Map of material id to list of transactions as required by the server
   */
  public Map<Long, List<ITransaction>> buildMaterialTransactionsMap(String userId, Long kid,
                                                                    List<MobileMaterialTransModel> mobileMaterialTransModels) {
    if (mobileMaterialTransModels == null || mobileMaterialTransModels.isEmpty()) {
      return null;
    }
    Map<Long, List<ITransaction>> midTransModelMap = new HashMap<>(mobileMaterialTransModels.size());
    for (MobileMaterialTransModel mobileMaterialTransModel : mobileMaterialTransModels) {
      Long mid = mobileMaterialTransModel.mid;
      List<ITransaction> transactionList = buildTransactions(userId, kid, mobileMaterialTransModel);
      midTransModelMap.put(mid, transactionList);
    }
    return midTransModelMap;
  }


  private List<ITransaction> buildTransactions(String userId, Long kid, MobileMaterialTransModel mobileMaterialTransModel) {
    if (mobileMaterialTransModel == null || mobileMaterialTransModel.trns == null) {
      return Collections.emptyList();
    }
    List<ITransaction> transactions = new ArrayList<>(mobileMaterialTransModel.trns.size());
    for (MobileTransModel mobileTransModel : mobileMaterialTransModel.trns) {
      ITransaction transaction = buildTransaction(userId, kid, mobileTransModel);
      transaction.setMaterialId(mobileMaterialTransModel.mid);
      transactions.add(transaction);
    }
    return transactions;
  }

  /**
   *
   * Builds an ITransaction object from a MobileTransModel object
   * @param userId
   * @param kid
   * @param mobTrans
   * @return
   */
  private ITransaction buildTransaction(String userId, Long kid, MobileTransModel mobTrans) {
    ITransaction trans = JDOUtils.createInstance(ITransaction.class);
    trans.setSourceUserId(userId);
    trans.setKioskId(kid);
    if (mobTrans.entm != null) {
      trans.setEntryTime(new Date(mobTrans.entm));
      trans.setSortEt(trans.getEntryTime());
    }
    trans.setType(mobTrans.ty);
    trans.setQuantity(mobTrans.q);
    if (StringUtils.isNotEmpty(mobTrans.bid)) {
      trans.setOpeningStockByBatch(mobTrans.ostk);
    } else {
      trans.setOpeningStock(mobTrans.ostk);
    }
    TransactionUtil
        .setBatchData(trans, mobTrans.bid, mobTrans.bexp, mobTrans.bmfnm, mobTrans.bmfdt);
    trans.setReason(mobTrans.rsn);
    trans.setMaterialStatus(mobTrans.mst);
    String atd = mobTrans.atd;
    if (StringUtils.isNotEmpty(atd)) {
      try {
        Date actTransDt = LocalDateUtil.parseCustom(atd, Constants.DATE_FORMAT, null);
        trans.setAtd(actTransDt);
      } catch (ParseException e) {
        xLogger.severe("Error while parsing actual transaction date string for kid {0}", kid, e);
      }
    }
    trans.setLinkedKioskId(mobTrans.lkid);
    MobileGeoModel mobileGeo = mobTrans.geo;
    if (mobileGeo != null) {
      if (mobileGeo.lat != null) {
        trans.setLatitude(mobileGeo.lat);
      }
      if (mobileGeo.lng != null) {
        trans.setLongitude(mobileGeo.lng);
      }
      if (mobileGeo.galt != null) {
      trans.setAltitude(mobileGeo.galt);
      }
      if (mobileGeo.gacc != null) {
      trans.setGeoAccuracy(mobileGeo.gacc);
      }
      trans.setGeoErrorCode(mobileGeo.gerr);
    }
    trans.setTrackingObjectType(mobTrans.troty);
    trans.setTrackingId(mobTrans.tid);
    trans.setLocalTrackingID(mobTrans.local_tid);
    return trans;
  }

  private List<MobileTransErrModel> buildErrorModelList(Map<Long,List<ErrorDetailModel>> midErrorDetailModelsMap) {
    if (MapUtils.isEmpty(midErrorDetailModelsMap)) {
      return Collections.emptyList();
    }
    List<MobileTransErrModel> mobileTransErrModels = new ArrayList<>(midErrorDetailModelsMap.size());
    Set<Long> mids = midErrorDetailModelsMap.keySet();
    for (Long mid : mids) {
      MobileTransErrModel mobileTransErrModel = buildMobileTransErrorModel(mid, midErrorDetailModelsMap.get(mid));
      mobileTransErrModels.add(mobileTransErrModel);
    }
    return mobileTransErrModels;
  }

  private MobileTransErrModel buildMobileTransErrorModel(Long mid,
                                                       List<ErrorDetailModel> errorDetailModels) {
    if (mid == null || CollectionUtils.isEmpty(errorDetailModels)) {
      return null;
    }
    MobileTransErrModel mobileTransErrModel = new MobileTransErrModel();
    mobileTransErrModel.mid = mid;
    mobileTransErrModel.errdtl = buildMobileTransErrorDetailModel(errorDetailModels);
    return mobileTransErrModel;
  }

  private List<MobileTransErrorDetailModel> buildMobileTransErrorDetailModel(List<ErrorDetailModel> errorDetailModels) {
    if(CollectionUtils.isEmpty(errorDetailModels)) {
      return Collections.emptyList();
    }
    List<MobileTransErrorDetailModel> mobileTransErrorDetailModels = new ArrayList<>(errorDetailModels.size());
    for (ErrorDetailModel errorDetailModel : errorDetailModels) {
      MobileTransErrorDetailModel mobileTransErrorDetailModel = new MobileTransErrorDetailModel();
      mobileTransErrorDetailModel.ec = getAppUnderstandableErrorCode(errorDetailModel.errorCode);
      mobileTransErrorDetailModel.msg = errorDetailModel.message;
      mobileTransErrorDetailModel.idx = errorDetailModel.index;
      mobileTransErrorDetailModels.add(mobileTransErrorDetailModel);
    }
    return mobileTransErrorDetailModels;
  }

  private String getAppUnderstandableErrorCode(String errorCode) {
    switch(errorCode){
      case "M004" :
        return "M012";
      case "M011" :
        return "M011";
      case "M012" :
        return "M012";
      default:
        return "M010";
    }
  }

  protected boolean isSuccess(Map<Long,ResponseDetailModel> responseDetailModelByMaterial) {
    if (MapUtils.isEmpty(responseDetailModelByMaterial)) {
      return false;
    }
    return !(responseDetailModelByMaterial.entrySet().stream().anyMatch(
        entry -> !entry.getValue().errorDetailModels.isEmpty()));
  }

  protected Map<Long,List<ErrorDetailModel>> getErrorDetailModelsByMaterial(Map<Long,ResponseDetailModel> responseDetailModelByMaterial) {
    if (MapUtils.isEmpty(responseDetailModelByMaterial)) {
      return Collections.emptyMap();
    }
    return responseDetailModelByMaterial.entrySet()
        .stream()
        .filter(entry -> !CollectionUtils.isEmpty(entry.getValue().errorDetailModels))
        .collect(Collectors.toMap(Entry::getKey, entry-> entry.getValue().errorDetailModels));
  }

  protected Map<Long,List<SuccessDetailModel>> getSuccessDetailModelsByMaterial(Map<Long,ResponseDetailModel> responseDetailModelByMaterial) {
    if (MapUtils.isEmpty(responseDetailModelByMaterial)) {
      return Collections.emptyMap();
    }
    return responseDetailModelByMaterial.entrySet()
        .stream()
        .filter(entry -> !CollectionUtils.isEmpty(entry.getValue().successDetailModels))
        .collect(Collectors.toMap(Entry::getKey, entry-> entry.getValue().successDetailModels));
  }

  protected List<MobileTransSuccessModel> buildSuccessModels(Map<Long,List<SuccessDetailModel>> successDetailModelsByMaterial) {
    if (MapUtils.isEmpty(successDetailModelsByMaterial)) {
      return Collections.emptyList();
    }
    return (successDetailModelsByMaterial.entrySet().stream().map(
              entry -> buildMobileTransSuccessModel(
              entry.getKey(), entry.getValue()))
              .filter(Optional::isPresent)
              .map(Optional::get)
              .collect(Collectors.toList()));
  }

  protected Optional<MobileTransSuccessModel> buildMobileTransSuccessModel(Long mid, List<SuccessDetailModel> successDetailModels) {
    if (mid == null || CollectionUtils.isEmpty(successDetailModels)) {
      return Optional.empty();
    }
    MobileTransSuccessModel mobileTransSuccessModel = new MobileTransSuccessModel();
    mobileTransSuccessModel.mid = mid;
    mobileTransSuccessModel.successDetails = buildMobileTransSuccessDetailModels(successDetailModels);
    return Optional.of(mobileTransSuccessModel);

  }

  protected List<MobileTransSuccessDetailModel> buildMobileTransSuccessDetailModels(List<SuccessDetailModel> successDetailModels) {
    if(CollectionUtils.isEmpty(successDetailModels)) {
      return Collections.emptyList();
    }
    return successDetailModels.stream().map(
        this::buildMobileTransSuccessDetailModel)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
  }

  protected Optional<MobileTransSuccessDetailModel> buildMobileTransSuccessDetailModel(SuccessDetailModel successDetailModel) {
    if (successDetailModel == null) {
      return Optional.empty();
    }
    MobileTransSuccessDetailModel mobileTransSuccessDetailModel = new MobileTransSuccessDetailModel();
    mobileTransSuccessDetailModel.successCode = successDetailModel.successCode;
    mobileTransSuccessDetailModel.index = successDetailModel.index;
    mobileTransSuccessDetailModel.keys = successDetailModel.keys;
    return Optional.of(mobileTransSuccessDetailModel);
  }
}
