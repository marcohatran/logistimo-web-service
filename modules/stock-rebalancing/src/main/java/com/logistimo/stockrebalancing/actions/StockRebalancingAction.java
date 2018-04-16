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

package com.logistimo.stockrebalancing.actions;

import com.logistimo.config.models.DomainConfig;
import com.logistimo.config.models.StockRebalancingConfig;
import com.logistimo.logger.XLog;
import com.logistimo.stockrebalancing.client.IStockRebalancingClient;
import com.logistimo.stockrebalancing.client.internal.models.Configuration;
import com.logistimo.stockrebalancing.client.internal.models.Configuration.MatchingStrategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by charan on 19/03/18.
 */

@Component
public class StockRebalancingAction {

  private static final String CODE = "code";
  private static final String SDET = "SDET";
  private static final String SEBC = "SEBC";
  private static final String SGTM = "SGTM";
  private static final String SGTS = "SGTS";
  private static final String SLTM = "SLTM";
  private static final String STOCK_OUT_THRESHOLD = "stock_out_threshold";
  private static final String MINIMUM_STOCK_EXCESS_DAYS = "minimum_stock_excess_days";
  private static final float MINIMUM_REQUIRED_PERCENTAGE = 10.0F;

  @Autowired
  IStockRebalancingClient stockRebalancingClient;

  private static final XLog LOGGER = XLog.getLog(StockRebalancingAction.class);

  public void invoke(Long domainId) {

    LOGGER.info("Started stock rebalancing for domain {0} - ", domainId);
    DomainConfig domainConfig = DomainConfig.getInstance(domainId);
    StockRebalancingConfig stockRebalancingConfig = domainConfig.getStockRebalancingConfig();

    if (!stockRebalancingConfig.isEnableStockRebalancing()) {
      LOGGER.info("Stock rebalancing not enabled for domain {0} - ", domainId);
      return;
    }

    List<Map<String, String>> destinationTriggers = new ArrayList<>();
    List<Map<String, String>> sourceTriggers = new ArrayList<>();

    if (stockRebalancingConfig.isStockOutDurationExceedsThreshold()) {
      destinationTriggers.add(buildSDETConfigMap(stockRebalancingConfig));
    }

    if (stockRebalancingConfig.isExpiryCheck()) {
      sourceTriggers.add(buildSEBCConfigMap());
    }

    if (stockRebalancingConfig.isMaxStock()) {
      sourceTriggers.add(buildSGTMConfigMap(stockRebalancingConfig));
    }

    configureSecondaryTriggers(destinationTriggers, sourceTriggers);

    Configuration configuration = Configuration.builder()
        .destinationTriggers(destinationTriggers)
        .sourceTriggers(sourceTriggers)
        .materialTags(stockRebalancingConfig.getMtTags())
        .transferMatrix(stockRebalancingConfig.getEntityTagsCombination())
        .handlingCost(BigDecimal.valueOf(stockRebalancingConfig.getHandlingCharges()))
        .transportationCost(BigDecimal.valueOf(stockRebalancingConfig.getTransportationCost()))
        .inventoryHoldingCost(
            BigDecimal.valueOf(stockRebalancingConfig.getInventoryHoldingCost()))
        .matchingStrategy(buildMatchingStrategy(stockRebalancingConfig))
        .build();

    stockRebalancingClient.triggerStockRebalancing(String.valueOf(domainId), configuration);
  }

  private void configureSecondaryTriggers(List<Map<String, String>> destinationTriggers,
                                          List<Map<String, String>> sourceTriggers) {
    sourceTriggers.add(buildByCode(SGTS));
    destinationTriggers.add(buildByCode(SLTM));
  }

  private Map<String, String> buildByCode(String code) {
    Map<String, String> sebcConfig = new HashMap<>(1);
    sebcConfig.put(CODE, code);
    return sebcConfig;
  }

  private Map<String, String> buildSGTMConfigMap(StockRebalancingConfig config) {
    Map<String, String> sgtmConfig = new HashMap<>();
    sgtmConfig.put(CODE, SGTM);
    sgtmConfig.put(MINIMUM_STOCK_EXCESS_DAYS, String.valueOf(config.getMaxStockDays()));
    return sgtmConfig;
  }

  private Map<String, String> buildSEBCConfigMap() {
    Map<String, String> sebcConfig = new HashMap<>();
    sebcConfig.put(CODE, SEBC);
    return sebcConfig;
  }

  private Map<String, String> buildSDETConfigMap(StockRebalancingConfig config) {
    Map<String, String> sdetConfig = new HashMap<>();
    sdetConfig.put(CODE, SDET);
    sdetConfig.put(STOCK_OUT_THRESHOLD,
        String.valueOf(config.getAcceptableLeadTime()));
    return sdetConfig;
  }

  private MatchingStrategy buildMatchingStrategy(StockRebalancingConfig config) {
    return MatchingStrategy.builder()
        .distance(config.getGeoFencing())
        .minimumRequiredPercentage(MINIMUM_REQUIRED_PERCENTAGE)
        .build();
  }
}
