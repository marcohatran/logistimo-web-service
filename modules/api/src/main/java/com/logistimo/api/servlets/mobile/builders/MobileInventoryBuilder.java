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

package com.logistimo.api.servlets.mobile.builders;

import com.google.gson.Gson;

import com.logistimo.config.models.DomainConfig;
import com.logistimo.config.models.InventoryConfig;
import com.logistimo.constants.Constants;
import com.logistimo.entities.entity.IKiosk;
import com.logistimo.entities.service.EntitiesService;
import com.logistimo.inventory.entity.IInvntry;
import com.logistimo.inventory.entity.IInvntryBatch;
import com.logistimo.inventory.service.InventoryManagementService;
import com.logistimo.logger.XLog;
import com.logistimo.materials.entity.IMaterial;
import com.logistimo.materials.service.MaterialCatalogService;
import com.logistimo.pagination.PageParams;
import com.logistimo.pagination.Results;
import com.logistimo.proto.MobileConsRateModel;
import com.logistimo.proto.MobileInvBatchModel;
import com.logistimo.proto.MobileInvModel;
import com.logistimo.proto.MobileUpdateInvTransResponse;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.users.service.UsersService;
import com.logistimo.utils.BigUtil;
import com.logistimo.utils.LocalDateUtil;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by charan on 10/11/17.
 */

@Component
public class MobileInventoryBuilder {

  private static final XLog xLogger = XLog.getLog(MobileInventoryBuilder.class);

  private UsersService usersService;
  private InventoryManagementService inventoryManagementService;
  private EntitiesService entitiesService;
  private MaterialCatalogService materialCatalogService;

  @Autowired
  public void setUsersService(UsersService usersService) {
    this.usersService = usersService;
  }

  @Autowired
  public void setInventoryManagementService(InventoryManagementService inventoryManagementService) {
    this.inventoryManagementService = inventoryManagementService;
  }

  @Autowired
  public void setEntitiesService(EntitiesService entitiesService) {
    this.entitiesService = entitiesService;
  }

  @Autowired
  public void setMaterialCatalogService(MaterialCatalogService materialCatalogService) {
    this.materialCatalogService = materialCatalogService;
  }

  public MobileInvModel buildMobileInvModel(IInvntry inventory,
                                            Long domainId, String userId) {
    MobileInvModel inv = new MobileInvModel();
    try {
      DomainConfig dc = DomainConfig.getInstance(domainId);
      IUserAccount user = usersService.getUserAccount(userId);
      Locale locale = user.getLocale();
      String timezone = user.getTimezone();
      inv.mid = inventory.getMaterialId();
      inv.smid = inventory.getShortId();
      inv.q = inventory.getStock();
      if (dc.autoGI()) {
        inv.alq = inventory.getAllocatedStock();
        inv.itq = inventory.getInTransitStock();
        inv.avq = inventory.getAvailableStock();
      }
      BigDecimal stockAvailPeriod = inventoryManagementService.getStockAvailabilityPeriod(inventory, dc);
      if (stockAvailPeriod != null && BigUtil.notEqualsZero(stockAvailPeriod)) {
        inv.dsq = stockAvailPeriod;
      }
      if (inventory.getMinDuration() != null && BigUtil
          .notEqualsZero(inventory.getMinDuration())) {
        inv.dmin = inventory.getMinDuration();
      }
      if (inventory.getMaxDuration() != null && BigUtil
          .notEqualsZero(inventory.getMaxDuration())) {
        inv.dmax = inventory.getMaxDuration();
      }
      MobileConsRateModel crModel = buildMobileConsRateModel(domainId, inventory);
      if (crModel != null) {
        inv.cr = crModel;
      }
      inv.t = LocalDateUtil.format(inventory.getTimestamp(), locale,
          timezone);
      Long kid = inventory.getKioskId();
      Long mid = inventory.getMaterialId();
      IKiosk k = entitiesService.getKiosk(kid, false);
      IMaterial m = materialCatalogService.getMaterial(mid);
      if (k.isBatchMgmtEnabled() && m.isBatchEnabled()) {
        inv.bt = buildMobileInvBatchModelList(kid, mid, locale, timezone,
            dc.autoGI(), true);
        inv.xbt = buildMobileInvBatchModelList(kid, mid, locale, timezone, dc.autoGI(),
            false);
      }
      return inv;
    } catch (Exception e) {
      xLogger.warn(
          "Exception while building inventory model in update inventory transactions response", e);
      return null;
    }
  }

  private MobileConsRateModel buildMobileConsRateModel(Long domainId, IInvntry inv) {
    DomainConfig dc = DomainConfig.getInstance(domainId);
    InventoryConfig ic = dc.getInventoryConfig();
    String displayFreq = ic.getDisplayCRFreq();
    try {
      BigDecimal cr = inventoryManagementService.getDailyConsumptionRate(inv);
      if (BigUtil.greaterThanZero(cr)) {
        MobileConsRateModel mobConRateModel = new MobileConsRateModel();
        mobConRateModel.val = BigUtil.getFormattedValue(cr);
        switch (displayFreq) {
          case Constants.FREQ_DAILY:
            mobConRateModel.ty = Constants.FREQ_TYPE_DAILY;
            break;
          case Constants.FREQ_WEEKLY:
            mobConRateModel.ty = Constants.FREQ_TYPE_WEEKLY;
            break;
          case Constants.FREQ_MONTHLY:
            mobConRateModel.ty = Constants.FREQ_TYPE_MONTHLY;
            break;
          default:
            xLogger.warn(
                "Invalid displayFrequency: {0} while building mobile consumption rate model for inventory with kid: {1} and mid: {1}",
                displayFreq, inv.getKioskId(), inv.getMaterialId());
            break;
        }
        return mobConRateModel;
      }
    } catch (Exception e) {
      return null;
    }
    return null;
  }

  /**
   * Sets the partial id in the mobile update inventory transaction response json string (if not set already)
   */
  public String buildUpdateInvTransResponseWithPartialID(String mobUpdateInvTransRespJsonStr,
                                                         String pid) {
    MobileUpdateInvTransResponse
        mobileUpdateInvTransResponse =
        new Gson().fromJson(mobUpdateInvTransRespJsonStr,
            MobileUpdateInvTransResponse.class);
    if (StringUtils.isEmpty(mobileUpdateInvTransResponse.pid)) {
      mobileUpdateInvTransResponse.pid = pid;
      return new Gson().toJson(mobileUpdateInvTransResponse);
    } else {
      return mobUpdateInvTransRespJsonStr;
    }
  }

  private List<MobileInvBatchModel> buildMobileInvBatchModelList(Long kid, Long mid, Locale locale,
                                                                 String timezone,
                                                                 boolean isAutoPostingIssuesEnabled,
                                                                 boolean buildValidBatchModel) {
    List<MobileInvBatchModel> mobileInvBatchModelList = null;
    try {
      // NOTE: Get only up to the 50 last batches
      Results<IInvntryBatch>
          results =
          inventoryManagementService.getBatches(mid, kid, new PageParams(null,
              PageParams.DEFAULT_SIZE));
      if (results != null) {
        List<IInvntryBatch> batches = results.getResults();
        if (batches != null && !batches.isEmpty()) {
          mobileInvBatchModelList = new ArrayList<>(batches.size());
          for (IInvntryBatch invBatch : batches) {
            MobileInvBatchModel
                mibm =
                buildMobileInvBatchModel(invBatch, isAutoPostingIssuesEnabled, locale, timezone,
                    buildValidBatchModel);
            if (mibm != null) {
              mobileInvBatchModelList.add(mibm);
            }
          }
        }
      }
    } catch (Exception e) {
      xLogger
          .warn("Exception when trying to get batch information for kid {0} mid {1}", kid, mid, e);
      return null;
    }
    return mobileInvBatchModelList;
  }

  private MobileInvBatchModel buildMobileInvBatchModel(IInvntryBatch invBatch,
                                                       boolean isAutoPostingIssuesEnabled,
                                                       Locale locale, String timezone,
                                                       boolean validBatchesOnly) {
    if (validBatchesOnly && invBatch.isExpired()) {
      return null;
    }
    if (!validBatchesOnly && !invBatch.isExpired()) {
      return null;
    }
    // For any batch whether valid or expired, if q is not > 0, return null
    if (!BigUtil.greaterThanZero(invBatch.getQuantity())) {
      return null;
    }
    MobileInvBatchModel mobileInvBatchModel = new MobileInvBatchModel();
    mobileInvBatchModel.bid = invBatch.getBatchId();
    if (invBatch.getBatchManufacturedDate() != null) {
      mobileInvBatchModel.bmfdt =
          LocalDateUtil
              .formatCustom(invBatch.getBatchManufacturedDate(), Constants.DATE_FORMAT, null);
    }
    mobileInvBatchModel.bmfnm = invBatch.getBatchManufacturer();
    if (invBatch.getBatchExpiry() != null) {
      mobileInvBatchModel.bexp =
          LocalDateUtil.formatCustom(invBatch.getBatchExpiry(), Constants.DATE_FORMAT, null);
    } else {
      xLogger.warn(
          "Null Batch expiry date when building mobile inventory batch model for kid: {0}, mid: {1}, bid: {2}, bexp: {3}",
          invBatch.getKioskId(), invBatch.getMaterialId(), invBatch.getBatchId(),
          invBatch.getBatchExpiry());
    }

    mobileInvBatchModel.q = invBatch.getQuantity();
    if (isAutoPostingIssuesEnabled && validBatchesOnly) {
      mobileInvBatchModel.alq = invBatch.getAllocatedStock();
      mobileInvBatchModel.avq = invBatch.getAvailableStock();
    }
    mobileInvBatchModel.t = LocalDateUtil.format(invBatch.getTimestamp(), locale,
        timezone);

    return mobileInvBatchModel;
  }


}
