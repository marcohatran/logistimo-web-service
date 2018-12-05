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

package com.logistimo.api.migrators;

import com.logistimo.AppFactory;
import com.logistimo.config.entity.IConfig;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.config.models.InventoryConfig;
import com.logistimo.config.models.ReasonConfig;
import com.logistimo.config.service.ConfigurationMgmtService;
import com.logistimo.config.service.impl.ConfigurationMgmtServiceImpl;
import com.logistimo.context.StaticApplicationContext;
import com.logistimo.domains.entity.IDomain;
import com.logistimo.domains.service.DomainsService;
import com.logistimo.domains.service.impl.DomainsServiceImpl;
import com.logistimo.inventory.entity.ITransaction;
import com.logistimo.logger.XLog;
import com.logistimo.pagination.Results;
import com.logistimo.services.ServiceException;
import com.logistimo.services.cache.MemcacheService;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by vani on 10/05/18.
 */
public class ReasonMandatoryConfigMigrator {
  private static final XLog LOGGER = XLog.getLog(ReasonMandatoryConfigMigrator.class);
  private static Set<String> transactionTypes = new HashSet<>(Arrays
      .asList(ITransaction.TYPE_ISSUE, ITransaction.TYPE_RECEIPT, ITransaction.TYPE_PHYSICALCOUNT,
          ITransaction.TYPE_WASTAGE, ITransaction.TYPE_TRANSFER, ITransaction.TYPE_RETURNS_INCOMING,
          ITransaction.TYPE_RETURNS_OUTGOING));

  /**
   * Migrate reason mandatory config
   */
  public void migrateReasonMandatoryConfig() throws ServiceException {
    DomainsService ds = StaticApplicationContext.getBean(DomainsServiceImpl.class);
    Results domains = ds.getAllDomains(null);
    List domainList = domains.getResults();
    if (CollectionUtils.isNotEmpty(domainList)) {
      for(Object domainObj: domainList){
        migrateReasonMandatoryConfigForEveryDomain((IDomain) domainObj);
      }
    }
    LOGGER.info("Migration of reason mandatory config completed");
  }

  private void migrateReasonMandatoryConfigForEveryDomain(IDomain domain) throws ServiceException {
    DomainConfig domainConfig = DomainConfig.getInstance(domain.getId());
    InventoryConfig inventoryConfig = domainConfig.getInventoryConfig();
    Set<String> transactionTypesWithReasonMandatory = transactionTypes.stream()
            .filter(transactionType -> isReasonMandatory(inventoryConfig, transactionType))
            .collect(Collectors.toSet());
    if (CollectionUtils.isNotEmpty(transactionTypesWithReasonMandatory)) {
      inventoryConfig.setTransactionTypesWithReasonMandatory(transactionTypesWithReasonMandatory);
      domainConfig.setInventoryConfig(inventoryConfig);
      ConfigurationMgmtService cms =
          StaticApplicationContext.getBean(ConfigurationMgmtServiceImpl.class);
      IConfig config;
      try {
        config = cms.getConfiguration(IConfig.CONFIG_PREFIX + domain.getId());
        config.setConfig(domainConfig.toJSONSring());
        cms.updateConfiguration(config);
        MemcacheService cache = AppFactory.get().getMemcacheService();
        if (cache != null) {
          cache.put(DomainConfig.getCacheKey(domain.getId()), domainConfig);
        }
        LOGGER.info("Inventory config changed for domain {0}:{1}",domain.getId(), domain.getName());
      } catch (Exception e) {
        LOGGER.severe("{2}: Failed to update config for {0}:{1}",domain.getId(),domain.getName(),e);
      }
    }
  }

  /**
   * For a transaction type, returns true if default reason is configured either without tags or with tags and false otherwise.
   * @param inventoryConfig
   * @param transactionType
   * @return
   */
  private boolean isReasonMandatory(InventoryConfig inventoryConfig, String transactionType) {
    ReasonConfig reasonConfig = inventoryConfig.getTransactionReasonsConfigByType(transactionType);
    if (reasonConfig != null) {
      if (StringUtils.isNotEmpty(reasonConfig.getDefaultReason())) {
        return true;
      }
    }
    Map<String,ReasonConfig> reasonConfigByMaterialTagMap = null;
    switch(transactionType) {
      case ITransaction.TYPE_ISSUE:
        reasonConfigByMaterialTagMap = inventoryConfig.getImTransReasons();
        break;
      case ITransaction.TYPE_RECEIPT:
        reasonConfigByMaterialTagMap = inventoryConfig.getRmTransReasons();
        break;
      case ITransaction.TYPE_PHYSICALCOUNT:
        reasonConfigByMaterialTagMap = inventoryConfig.getSmTransReasons();
        break;
      case ITransaction.TYPE_WASTAGE:
        reasonConfigByMaterialTagMap = inventoryConfig.getDmTransReasons();
        break;
      case ITransaction.TYPE_TRANSFER:
        reasonConfigByMaterialTagMap = inventoryConfig.getTmTransReasons();
        break;
      case ITransaction.TYPE_RETURNS_INCOMING:
        reasonConfigByMaterialTagMap = inventoryConfig.getMtagRetIncRsns();
        break;
      case ITransaction.TYPE_RETURNS_OUTGOING:
        reasonConfigByMaterialTagMap = inventoryConfig.getMtagRetOutRsns();
        break;
      default:
        LOGGER.severe("Invalid transaction type: {0}", transactionType);
        break;
    }
    if (MapUtils.isNotEmpty(reasonConfigByMaterialTagMap)) {
      return (reasonConfigByMaterialTagMap.entrySet().stream().anyMatch(entry -> StringUtils.isNotEmpty(entry.getValue().getDefaultReason())));
    }
    return false;
  }
}
