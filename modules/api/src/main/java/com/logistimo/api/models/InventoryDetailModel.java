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

package com.logistimo.api.models;

import com.logistimo.api.models.mobile.CurrentStock;
import com.logistimo.events.entity.IEvent;
import com.logistimo.materials.model.HandlingUnitModel;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import lombok.Data;

/**
 * Created by yuvaraj on 04/05/17.
 */
@Data
public class InventoryDetailModel {

  /**
   * Material id
   */
  public Long mId;
  /**
   * Material name
   */
  public String mnm;
  /**
   * Entity id
   */
  public Long eId;
  /**
   * Entity Name
   */
  public String enm; //Entity Name
  /**
   * Material Tags
   */
  public List<String> mat_tgs;
  /**
   * Current Stock
   */
  public CurrentStock currentStock;
  /**
   * Min stock
   */
  public BigDecimal min = BigDecimal.ZERO; // re-order levels or MIN
  /**
   * Max Stock
   */
  public BigDecimal max = BigDecimal.ZERO;
  /**
   * Material description
   */
  public String description;
  /**
   * Material Image path
   */
  public List<String> imgPath;
  /**
   * Stock event
   */
  public int se = IEvent.NORMAL;
  /**
   * Event duration
   */
  public long ed;
  /**
   * Allocated Stock
   */
  public BigDecimal as;
  /**
   * In transit stock
   */
  public BigDecimal it;
  /**
   * Likely to stock out (or) Predicted Days Of Stock
   */
  public BigDecimal ls;
  /**
   * Stock Availability Days
   */
  public BigDecimal sad;
  /**
   * Stock availability period
   */
  public String sap;
  public EntityModel loc;
  /**
   * Valid batches meta information
   */
  private List<InvntryBatchModel> batches;
  /**
   * Expired batches meta information
   */
  private List<InvntryBatchModel> expiredBatches;
  /**
   * Handling unit
   */
  private HandlingUnitModel handlingUnitModel;
  /**
   * Material is temperature sensitive or not
   */
  private boolean isTemperatureSensitive;
  /**
   * Min temperature
   */
  private float minTemperature;
  /**
   * Max temperature
   */
  private float maxTemperature;
  /**
   * Short material Id
   */
  private String shortMaterialId;
  /**
   * Available stock
   */
  private BigDecimal availableStock;
  /**
   * Consumption rate - daily
   */
  private BigDecimal consumptionRateDaily;
  /**
   * Consumption rate - weekly
   */
  private BigDecimal consumptionRateWeekly;
  /**
   * Consumption rate - monthly
   */
  private BigDecimal consumptionRateMonthly;
  /**
   * Last updated timestamp
   */
  private String lastUpdatedTimestamp;
  /**
   * Manufacturer price
   */
  private BigDecimal manufacturerPrice;
  /**
   * Retailer price
   */
  private BigDecimal retailerPrice;
  /**
   * Custom material Id
   */
  private String customMaterialId;
  /**
   * Batch enabled - true/false
   */
  private boolean isBatchEnabled;
  /**
   * Enforce handling unit
   */
  private boolean enforceHandlingUnit;
  /**
   * Minimum duration stock
   */
  private BigDecimal minimumDurationStock;
  /**
   * Maximum duration stock
   */
  private BigDecimal maximumDurationStock;
}
