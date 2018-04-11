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

package com.logistimo.api.models.configuration;

import java.util.List;

/**
 * Created by naveensnair on 20/03/18.
 */
public class StockRebalancingConfigModel {

  private boolean enableStockRebalancing;
  private List<String> mtTags;
  private List<EntityTagsCombination> entityTagsCombination;
  private int geoFencing;
  private boolean stockOutDurationExceedsThreshold;
  private int acceptableLeadTime;
  private boolean expiryCheck;
  private boolean maxStock;
  private int maxStockDays;
  private int transportationCost;
  private int handlingCharges;
  private int inventoryHoldingCost;
  private String createdBy; //user who last updated config
  private String lastUpdated; //last updated time
  private String fn; //first name

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

  public List<EntityTagsCombination> getEntityTagsCombination() {
    return entityTagsCombination;
  }

  public void setEntityTagsCombination(
      List<EntityTagsCombination> entityTagsCombination) {
    this.entityTagsCombination = entityTagsCombination;
  }

  public int getGeoFencing() {
    return geoFencing;
  }

  public void setGeoFencing(int geoFencing) {
    this.geoFencing = geoFencing;
  }

  public static class EntityTagsCombination {

    private List<String> enTags;

    public List<String> getEntityTags() {
      return enTags;
    }

    public void setEntityTags(List<String> enTags) {
      this.enTags = enTags;
    }
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

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public String getLastUpdated() {
    return lastUpdated;
  }

  public void setLastUpdated(String lastUpdated) {
    this.lastUpdated = lastUpdated;
  }

  public String getFirstName() {
    return fn;
  }

  public void setFirstName(String fn) {
    this.fn = fn;
  }
}
