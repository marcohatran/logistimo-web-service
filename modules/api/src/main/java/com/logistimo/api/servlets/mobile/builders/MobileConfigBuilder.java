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

import com.logistimo.config.models.AccountingConfig;
import com.logistimo.config.models.ApprovalsConfig;
import com.logistimo.config.models.InventoryConfig;
import com.logistimo.config.models.ReasonConfig;
import com.logistimo.config.models.ReturnsConfig;
import com.logistimo.constants.CharacterConstants;
import com.logistimo.context.StaticApplicationContext;
import com.logistimo.inventory.entity.ITransaction;
import com.logistimo.proto.JsonTagsZ;
import com.logistimo.proto.MobileAccountingConfigModel;
import com.logistimo.proto.MobileApprovalsConfigModel;
import com.logistimo.proto.MobileApproversModel;
import com.logistimo.proto.MobilePurchaseSalesOrdersApprovalModel;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by vani on 28/06/17.
 */
@Component
public class MobileConfigBuilder {
  private static final int ENABLED = 1;
  private static final int DISABLED = 0;

  /**
   * Builds the approval configuration model as required by the mobile from approvals configuration as obtained from domain configuration
   * @param approvalsConfig
   * @return
   */
  public MobileApprovalsConfigModel buildApprovalConfiguration(ApprovalsConfig approvalsConfig) {
    if (approvalsConfig == null) {
      return null;
    }
    MobileApprovalsConfigModel mobileApprovalsConfigModel = new MobileApprovalsConfigModel();
    ApprovalsConfig.OrderConfig ordersConfig = approvalsConfig.getOrderConfig();
    mobileApprovalsConfigModel.ords = buildPurchaseSalesOrderApprovalConfigModel(
        ordersConfig);
    mobileApprovalsConfigModel.trf = buildTransfersApprovalConfigModel(ordersConfig);
    return mobileApprovalsConfigModel;
  }

  /**
   * Builds the accounting configuration model as required by the mobile from accounting configuration as obtained from domain configuration
   * @param isAccountingEnabled
   * @param accountingConfig
   * @return
   */
  public MobileAccountingConfigModel buildAccountingConfiguration(boolean isAccountingEnabled, AccountingConfig accountingConfig) {
    MobileAccountingConfigModel mobileAccountingConfigModel = new MobileAccountingConfigModel();
    mobileAccountingConfigModel.enb = isAccountingEnabled;
    if (isAccountingEnabled) {
      if (accountingConfig.enforceConfirm()) {
        mobileAccountingConfigModel.enfcrl = MobileAccountingConfigModel.ENFORCE_CREDIT_LIMIT_ON_CONFIRM_ORDER;
      } else if (accountingConfig.enforceShipped()) {
        mobileAccountingConfigModel.enfcrl = MobileAccountingConfigModel.ENFORCE_CREDIT_LIMIT_ON_SHIP_ORDER;
      }
    }
    return mobileAccountingConfigModel;
  }

  private Map<String, MobilePurchaseSalesOrdersApprovalModel> buildPurchaseSalesOrderApprovalConfigModel(
      ApprovalsConfig.OrderConfig orderConfig) {
    if (orderConfig == null) {
      return null;
    }
    List<ApprovalsConfig.PurchaseSalesOrderConfig>
        psoConfigList =
        orderConfig.getPurchaseSalesOrderApproval();
    if (psoConfigList == null || psoConfigList.isEmpty()) {
      return null;
    }
    // Iterate through each item in the list and build the model
    Map<String, MobilePurchaseSalesOrdersApprovalModel>
        ordersApprovalConfigModelMap =
        new HashMap<>(psoConfigList.size());
    for (ApprovalsConfig.PurchaseSalesOrderConfig psoConfig : psoConfigList) {
      boolean isPOApproval = psoConfig.isPurchaseOrderApproval();
      boolean isSOApproval = psoConfig.isSalesOrderApproval();
      List<String> eTags = psoConfig.getEntityTags();
      if (eTags != null && !eTags.isEmpty()) {
        for (String eTag : eTags) {
          MobilePurchaseSalesOrdersApprovalModel
              mobPurSleOrdApprvlModel = new MobilePurchaseSalesOrdersApprovalModel();
          mobPurSleOrdApprvlModel.prc = new MobileApprovalsConfigModel.MobileOrderApprovalModel();
          mobPurSleOrdApprvlModel.sle = new MobileApprovalsConfigModel.MobileOrderApprovalModel();
          mobPurSleOrdApprvlModel.prc.enb = isPOApproval ? ENABLED : DISABLED;
          mobPurSleOrdApprvlModel.sle.enb = isSOApproval ? ENABLED : DISABLED;
          ordersApprovalConfigModelMap.put(eTag, mobPurSleOrdApprvlModel);
        }
      }
    }
    return ordersApprovalConfigModelMap;
  }

  private MobileApprovalsConfigModel.MobileTransfersApprovalModel buildTransfersApprovalConfigModel(
      ApprovalsConfig.OrderConfig orderConfig) {
    if (orderConfig == null) {
      return null;
    }
    MobileApprovalsConfigModel.MobileTransfersApprovalModel
        transfersApprovalModel =
        new MobileApprovalsConfigModel.MobileTransfersApprovalModel();
    List<String> primaryApprovers = orderConfig.getPrimaryApprovers();
    List<String> secondaryApprovers = orderConfig.getSecondaryApprovers();
    if ((primaryApprovers == null || primaryApprovers.isEmpty()) && (secondaryApprovers == null
        || secondaryApprovers.isEmpty())) {
      transfersApprovalModel.enb = DISABLED;
      return transfersApprovalModel;
    }
    transfersApprovalModel.enb = ENABLED;
    transfersApprovalModel.apprvrs =
        buildTransfersApproversModel(primaryApprovers, secondaryApprovers);
    return transfersApprovalModel;
  }

  private MobileApproversModel buildTransfersApproversModel(List<String> primaryApprovers,
                                                            List<String> secApprovers) {
    if ((primaryApprovers == null || primaryApprovers.isEmpty()) && (secApprovers == null
        || secApprovers.isEmpty())) {
      return null;
    }
    MobileUserBuilder mobileUserBuilder = StaticApplicationContext.getBean(MobileUserBuilder.class);
    MobileApproversModel approversModel = new MobileApproversModel();
    if (primaryApprovers != null && !primaryApprovers.isEmpty()) {
      approversModel.prm =
          mobileUserBuilder
              .buildMobileUserModels(mobileUserBuilder.constructUserAccount(primaryApprovers));
    }
    if (secApprovers != null && !secApprovers.isEmpty()) {
      approversModel.scn =
          mobileUserBuilder
              .buildMobileUserModels(mobileUserBuilder.constructUserAccount(secApprovers));
    }
    return approversModel;
  }

  /**
   * Builds the returns configuration model as required by the mobile from returns configuration as obtained from the domain configuration
   * @param returnsConfigs
   * @return
   */
  public Optional<MobileReturnsConfigModel> buildMobileReturnsConfigModel(List<ReturnsConfig> returnsConfigs) {
    if (CollectionUtils.isEmpty(returnsConfigs)) {
      return Optional.empty();
    }
    MobileReturnsConfigModel mobileReturnsConfigModel = new MobileReturnsConfigModel();
    mobileReturnsConfigModel.setPolicies(buildMobileReturnsPolicyModels(returnsConfigs));
    return Optional.of(mobileReturnsConfigModel);
  }

  private List<MobileReturnsPolicyModel> buildMobileReturnsPolicyModels(List<ReturnsConfig> returnsConfigs) {
    if (CollectionUtils.isEmpty(returnsConfigs)) {
      return Collections.emptyList();
    }
    return returnsConfigs.stream()
        .map(this::buildMobileReturnsPolicyModel)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
  }

  private Optional<MobileReturnsPolicyModel> buildMobileReturnsPolicyModel(ReturnsConfig returnsConfig) {
    if (returnsConfig == null) {
      return Optional.empty();
    }
    MobileReturnsPolicyModel mobileReturnsPolicyModel = new MobileReturnsPolicyModel();
    mobileReturnsPolicyModel.setEntityTags(returnsConfig.getEntityTags());
    mobileReturnsPolicyModel.setDurations(buildMobileReturnsDurationModel(returnsConfig));
    return Optional.of(mobileReturnsPolicyModel);
  }

  private MobileReturnsDurationModel buildMobileReturnsDurationModel(ReturnsConfig returnsConfig) {
    MobileReturnsDurationModel mobileReturnsDurationModel = new MobileReturnsDurationModel();
    if (returnsConfig == null) {
      return mobileReturnsDurationModel;
    }
    mobileReturnsDurationModel.setIncoming(returnsConfig.getIncomingDuration());
    mobileReturnsDurationModel.setOutgoing(returnsConfig.getOutgoingDuration());
    return mobileReturnsDurationModel;
  }

  public Map<String, Map<String, String>> buildReasonsByTag(InventoryConfig ic) {
    Map<String, Map<String, String>> rsnsByMtag = new HashMap<>();
    if (MapUtils.isNotEmpty(ic.getImTransReasons())) {
      rsnsByMtag.put(JsonTagsZ.ISSUES, buildReasonsAsCSVByTag(ic.getImTransReasons()));
    }
    if (MapUtils.isNotEmpty(ic.getRmTransReasons())) {
      rsnsByMtag.put(JsonTagsZ.RECEIPTS, buildReasonsAsCSVByTag(ic.getRmTransReasons()));
    }
    if (MapUtils.isNotEmpty(ic.getSmTransReasons())) {
      rsnsByMtag.put(JsonTagsZ.PHYSICAL_STOCK, buildReasonsAsCSVByTag(ic.getSmTransReasons()));
    }
    if (MapUtils.isNotEmpty(ic.getDmTransReasons())) {
      rsnsByMtag.put(JsonTagsZ.DISCARDS, buildReasonsAsCSVByTag(ic.getDmTransReasons()));
    }
    if (MapUtils.isNotEmpty(ic.getTmTransReasons())) {
      rsnsByMtag.put(JsonTagsZ.TRANSFER, buildReasonsAsCSVByTag(ic.getTmTransReasons()));
    }
    if (MapUtils.isNotEmpty(ic.getMtagRetIncRsns())) {
      rsnsByMtag.put(JsonTagsZ.RETURNS_INCOMING, buildReasonsAsCSVByTag(ic.getMtagRetIncRsns()));
    }
    if (MapUtils.isNotEmpty(ic.getMtagRetOutRsns())) {
      rsnsByMtag.put(JsonTagsZ.RETURNS_OUTGOING, buildReasonsAsCSVByTag(ic.getMtagRetOutRsns()));
    }
    return rsnsByMtag;
  }

  private Map<String,String> buildReasonsAsCSVByTag(Map<String,ReasonConfig> reasonConfigByTagMap) {
    if (MapUtils.isEmpty(reasonConfigByTagMap)) {
      return Collections.emptyMap();
    }
    Map<String,String> reasonsByTag = new HashMap<>();
    reasonConfigByTagMap.entrySet()
        .stream()
        .filter(entry->CollectionUtils.isNotEmpty(entry.getValue().getReasons()))
        .forEach(entry->reasonsByTag.put(entry.getKey(), StringUtils.join(
            entry.getValue().getReasons(), CharacterConstants.COMMA)));
    return reasonsByTag;
  }

  public Map<String,MobileDefaultReasonsConfigModel> buildMobileDefaultReasonsConfigModelByTransType(InventoryConfig inventoryConfig) {
    if (inventoryConfig == null) {
      return Collections.emptyMap();
    }
    Map<String,MobileDefaultReasonsConfigModel> mobDefRsnsCfgModelByTransTypeMap = new HashMap<>();
    Optional<MobileDefaultReasonsConfigModel> mobileDefaultReasonsConfigModel = buildMobileDefaultReasonsConfigModel(ITransaction.TYPE_ISSUE, inventoryConfig);
    if (mobileDefaultReasonsConfigModel.isPresent()) {
      mobDefRsnsCfgModelByTransTypeMap.put(JsonTagsZ.ISSUES,mobileDefaultReasonsConfigModel.get());
    }
    mobileDefaultReasonsConfigModel = buildMobileDefaultReasonsConfigModel(ITransaction.TYPE_RECEIPT, inventoryConfig);
    if (mobileDefaultReasonsConfigModel.isPresent()) {
      mobDefRsnsCfgModelByTransTypeMap.put(JsonTagsZ.RECEIPTS,mobileDefaultReasonsConfigModel.get());
    }
    mobileDefaultReasonsConfigModel = buildMobileDefaultReasonsConfigModel(ITransaction.TYPE_PHYSICALCOUNT, inventoryConfig);
    if (mobileDefaultReasonsConfigModel.isPresent()) {
      mobDefRsnsCfgModelByTransTypeMap.put(JsonTagsZ.PHYSICAL_STOCK,mobileDefaultReasonsConfigModel.get());
    }
    mobileDefaultReasonsConfigModel = buildMobileDefaultReasonsConfigModel(ITransaction.TYPE_WASTAGE, inventoryConfig);
    if (mobileDefaultReasonsConfigModel.isPresent()) {
      mobDefRsnsCfgModelByTransTypeMap.put(JsonTagsZ.DISCARDS,mobileDefaultReasonsConfigModel.get());
    }
    mobileDefaultReasonsConfigModel = buildMobileDefaultReasonsConfigModel(ITransaction.TYPE_TRANSFER, inventoryConfig);
    if (mobileDefaultReasonsConfigModel.isPresent()) {
      mobDefRsnsCfgModelByTransTypeMap.put(JsonTagsZ.TRANSFER,mobileDefaultReasonsConfigModel.get());
    }
    mobileDefaultReasonsConfigModel = buildMobileDefaultReasonsConfigModel(ITransaction.TYPE_RETURNS_INCOMING, inventoryConfig);
    if (mobileDefaultReasonsConfigModel.isPresent()) {
      mobDefRsnsCfgModelByTransTypeMap.put(JsonTagsZ.RETURNS_INCOMING,mobileDefaultReasonsConfigModel.get());
    }
    mobileDefaultReasonsConfigModel = buildMobileDefaultReasonsConfigModel(ITransaction.TYPE_RETURNS_OUTGOING, inventoryConfig);
    if (mobileDefaultReasonsConfigModel.isPresent()) {
      mobDefRsnsCfgModelByTransTypeMap.put(JsonTagsZ.RETURNS_OUTGOING,mobileDefaultReasonsConfigModel.get());
    }
    return mobDefRsnsCfgModelByTransTypeMap;
  }

  private Optional<MobileDefaultReasonsConfigModel> buildMobileDefaultReasonsConfigModel(String transType, InventoryConfig inventoryConfig) {
    if (StringUtils.isEmpty(transType)) {
      return Optional.empty();
    }
    Map<String,ReasonConfig> reasonConfigByTagMap = new HashMap<>();
    switch(transType) {
      case ITransaction.TYPE_ISSUE:
          reasonConfigByTagMap = inventoryConfig.getImTransReasons();
          break;
      case ITransaction.TYPE_RECEIPT:
        reasonConfigByTagMap = inventoryConfig.getRmTransReasons();
        break;
      case ITransaction.TYPE_PHYSICALCOUNT:
        reasonConfigByTagMap = inventoryConfig.getSmTransReasons();
        break;
      case ITransaction.TYPE_WASTAGE:
        reasonConfigByTagMap = inventoryConfig.getDmTransReasons();
        break;
      case ITransaction.TYPE_TRANSFER:
        reasonConfigByTagMap = inventoryConfig.getTmTransReasons();
        break;
      case ITransaction.TYPE_RETURNS_INCOMING:
        reasonConfigByTagMap = inventoryConfig.getMtagRetIncRsns();
        break;
      case ITransaction.TYPE_RETURNS_OUTGOING:
        reasonConfigByTagMap = inventoryConfig.getMtagRetOutRsns();
        break;
      default:
        break;
    }
    MobileDefaultReasonsConfigModel mobileDefaultReasonsConfigModel = new MobileDefaultReasonsConfigModel();
    Map<String,String> defaultReasonByTagMap = new HashMap<>(reasonConfigByTagMap.size(),1);
    reasonConfigByTagMap.entrySet().stream().forEach(entry->defaultReasonByTagMap.put(entry.getKey(),entry.getValue().getDefaultReason()));
    mobileDefaultReasonsConfigModel.tags = MapUtils.isNotEmpty(defaultReasonByTagMap) ? defaultReasonByTagMap : null ;
    mobileDefaultReasonsConfigModel.general = inventoryConfig.getTransactionReasonsConfigByType(transType) != null ? inventoryConfig.getTransactionReasonsConfigByType(transType).getDefaultReason() : null;
    return Optional.of(mobileDefaultReasonsConfigModel);
  }
}
