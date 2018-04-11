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

package com.logistimo.config.models;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by naveensnair on 20/03/18.
 */

public class StockRebalancingConfig implements Serializable {

  public static final String ENABLE_STOCK_REBALANCING = "enable_stock_rebalancing";
  public static final String MATERIAL_TAGS = "material_tags";
  public static final String ENTITY_TAGS = "entity_tags";
  public static final String ENTITY_TAGS_COMBINATION = "entity_tags_combination";
  public static final String GEO_FENCING = "geo_fencing";
  public static final String STOCK_OUT_DURATION_EXCEEDS_THRESHOLD = "stock_out_duration_exceeds_threshold";
  public static final String ACCEPTABLE_LEAD_TIME = "acceptable_lead_time";
  public static final String EXPIRY_CHECK = "expiry_check";
  public static final String MAX_STOCK = "max_stock";
  public static final String MAX_STOCK_DAYS = "max_stock_days";
  private static final String TRANSPORTATION_COST = "transportation_cost";
  private static final String HANDLING_CHARGES = "handling_charges";
  private static final String INVENTORY_HOLDING_COST = "inventory_holding_cost";

  private boolean enableStockRebalancing;
  private List<String> mtTags;
  private List<List<String>> entityTagsCombination;
  private int geoFencing;
  private boolean stockOutDurationExceedsThreshold;
  private int acceptableLeadTime;
  private boolean expiryCheck;
  private boolean maxStock;
  private int maxStockDays;
  private int transportationCost;
  private int handlingCharges;
  private int inventoryHoldingCost;

  public StockRebalancingConfig() {

  }

  private void setGeneralConfigurations(JSONObject jsonObject) {
    if (jsonObject.get(ENABLE_STOCK_REBALANCING) != null) {
      setEnableStockRebalancing(jsonObject.getBoolean(ENABLE_STOCK_REBALANCING));
    }

    if (jsonObject.get(MATERIAL_TAGS) != null) {
      JSONArray jsonArray = jsonObject.getJSONArray(MATERIAL_TAGS);
      List<String> mTags = new ArrayList<>();
      for (int i = 0; i < jsonArray.length(); i++) {
        mTags.add(jsonArray.get(i).toString());
      }
      setMtTags(mTags);
    }

    if (jsonObject.get(ENTITY_TAGS_COMBINATION) != null) {
      JSONArray jsonArray = jsonObject.getJSONArray(ENTITY_TAGS_COMBINATION);
      List<String> entityTags = new ArrayList<>();
      List<List<String>> entityTagsList = new ArrayList<>();
      for (int i = 0; i < jsonArray.length(); i++) {
        JSONObject obj = (JSONObject) jsonArray.get(i);
        if(obj != null ) {
          JSONArray entityArray = obj.getJSONArray(ENTITY_TAGS);
          for(int j=0; j< entityArray.length(); j++) {
            entityTags.add((String) entityArray.get(j));
          }
        }
      }
      entityTagsList.add(entityTags);
      setEntityTagsCombination(entityTagsList);
    }

    if (jsonObject.getInt(GEO_FENCING) > 0) {
      setGeoFencing(jsonObject.getInt(GEO_FENCING));
    }

  }

  private void setTriggerConfigurations(JSONObject jsonObject) {
    if (jsonObject.getInt(TRANSPORTATION_COST) > 0) {
      setTransportationCost(jsonObject.getInt(TRANSPORTATION_COST));
    }

    if (jsonObject.getInt(HANDLING_CHARGES) > 0) {
      setHandlingCharges(jsonObject.getInt(HANDLING_CHARGES));
    }

    if (jsonObject.getInt(INVENTORY_HOLDING_COST) > 0) {
      setInventoryHoldingCost(jsonObject.getInt(INVENTORY_HOLDING_COST));
    }
  }

  private void setCostBenefitConfigurations(JSONObject jsonObject) {
    if (jsonObject.getBoolean(STOCK_OUT_DURATION_EXCEEDS_THRESHOLD)) {
      setStockOutDurationExceedsThreshold(
          jsonObject.getBoolean(STOCK_OUT_DURATION_EXCEEDS_THRESHOLD));
      setAcceptableLeadTime(jsonObject.getInt(ACCEPTABLE_LEAD_TIME));
    }

    if (jsonObject.getBoolean(EXPIRY_CHECK)) {
      setExpiryCheck(jsonObject.getBoolean(EXPIRY_CHECK));
    }

    if (jsonObject.getBoolean(MAX_STOCK)) {
      setMaxStock(jsonObject.getBoolean(MAX_STOCK));
      setMaxStockDays(jsonObject.getInt(MAX_STOCK_DAYS));
    }
  }

  public StockRebalancingConfig(JSONObject jsonObject) {
    if (jsonObject != null && jsonObject.length() > 0) {
      setGeneralConfigurations(jsonObject);
      setTriggerConfigurations(jsonObject);
      setCostBenefitConfigurations(jsonObject);
    }
  }

  private void constructGeneralConfigObject(JSONObject jsonObject) {
    jsonObject.put(ENABLE_STOCK_REBALANCING, isEnableStockRebalancing());
    if (getMtTags() != null && !getMtTags().isEmpty()) {
      jsonObject.put(MATERIAL_TAGS, getMtTags());
    }
    if (getEntityTagsCombination() != null && !getEntityTagsCombination().isEmpty()) {
      JSONArray jsonArray = new JSONArray();
      for (List<String> entityTagCombination : getEntityTagsCombination()) {
        JSONObject json = new JSONObject();
        json.put(ENTITY_TAGS, entityTagCombination);
        jsonArray.put(json);
      }
      jsonObject.put(ENTITY_TAGS_COMBINATION, jsonArray);
    }
    if (getGeoFencing() > 0) {
      jsonObject.put(GEO_FENCING, getGeoFencing());
    }
  }

  private void constructTriggerConfigObject(JSONObject jsonObject) {
    if (getTransportationCost() > 0) {
      jsonObject.put(TRANSPORTATION_COST, getTransportationCost());
    }

    if (getHandlingCharges() > 0) {
      jsonObject.put(HANDLING_CHARGES, getHandlingCharges());
    }

    if (getInventoryHoldingCost() > 0) {
      jsonObject.put(INVENTORY_HOLDING_COST, getInventoryHoldingCost());
    }
  }

  private void constructCostBenefitConfigObject(JSONObject jsonObject) {
    if (isStockOutDurationExceedsThreshold()) {
      jsonObject.put(STOCK_OUT_DURATION_EXCEEDS_THRESHOLD, isStockOutDurationExceedsThreshold());
      jsonObject.put(ACCEPTABLE_LEAD_TIME, getAcceptableLeadTime());
    }

    if (isExpiryCheck()) {
      jsonObject.put(EXPIRY_CHECK, isExpiryCheck());
    }

    if (isMaxStock()) {
      jsonObject.put(MAX_STOCK, isMaxStock());
      jsonObject.put(MAX_STOCK_DAYS, getMaxStockDays());
    }
    if (getTransportationCost() > 0) {
      jsonObject.put(TRANSPORTATION_COST, getTransportationCost());
    }

    if (getHandlingCharges() > 0) {
      jsonObject.put(HANDLING_CHARGES, getHandlingCharges());
    }

    if (getInventoryHoldingCost() > 0) {
      jsonObject.put(INVENTORY_HOLDING_COST, getInventoryHoldingCost());
    }
  }


  public JSONObject toJSONObject() {
    JSONObject jsonObject = new JSONObject();
    constructGeneralConfigObject(jsonObject);
    constructTriggerConfigObject(jsonObject);
    constructCostBenefitConfigObject(jsonObject);

    return jsonObject;
  }

  public boolean isEnableStockRebalancing() {
    return enableStockRebalancing;
  }

  public void setEnableStockRebalancing(boolean enableStockRebalancing) {
    this.enableStockRebalancing = enableStockRebalancing;
  }

  public List<String> getMtTags() {
    return mtTags;
  }

  public void setMtTags(List<String> mtTags) {
    this.mtTags = mtTags;
  }

  public List<List<String>> getEntityTagsCombination() {
    return entityTagsCombination;
  }

  public void setEntityTagsCombination(
      List<List<String>> entityTagsCombination) {
    this.entityTagsCombination = entityTagsCombination;
  }

  public int getGeoFencing() {
    return geoFencing;
  }

  public void setGeoFencing(int geoFencing) {
    this.geoFencing = geoFencing;
  }


  public boolean isStockOutDurationExceedsThreshold() {
    return stockOutDurationExceedsThreshold;
  }

  public void setStockOutDurationExceedsThreshold(boolean stockOutDurationExceedsThreshold) {
    this.stockOutDurationExceedsThreshold = stockOutDurationExceedsThreshold;
  }

  public int getAcceptableLeadTime() {
    return acceptableLeadTime;
  }

  public void setAcceptableLeadTime(int acceptableLeadTime) {
    this.acceptableLeadTime = acceptableLeadTime;
  }

  public boolean isExpiryCheck() {
    return expiryCheck;
  }

  public void setExpiryCheck(boolean expiryCheck) {
    this.expiryCheck = expiryCheck;
  }

  public boolean isMaxStock() {
    return maxStock;
  }

  public void setMaxStock(boolean maxStock) {
    this.maxStock = maxStock;
  }

  public int getMaxStockDays() {
    return maxStockDays;
  }

  public void setMaxStockDays(int maxStockDays) {
    this.maxStockDays = maxStockDays;
  }

  public int getTransportationCost() {
    return transportationCost;
  }

  public void setTransportationCost(int transportationCost) {
    this.transportationCost = transportationCost;
  }

  public int getHandlingCharges() {
    return handlingCharges;
  }

  public void setHandlingCharges(int handlingCharges) {
    this.handlingCharges = handlingCharges;
  }

  public int getInventoryHoldingCost() {
    return inventoryHoldingCost;
  }

  public void setInventoryHoldingCost(int inventoryHoldingCost) {
    this.inventoryHoldingCost = inventoryHoldingCost;
  }

}
