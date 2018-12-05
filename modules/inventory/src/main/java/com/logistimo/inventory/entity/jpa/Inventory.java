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

package com.logistimo.inventory.entity.jpa;

import com.logistimo.constants.Constants;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import lombok.Data;

/**
 * Created by pratheeka on 25/07/18.
 */
@Entity
@Table(name = "INVNTRY")
@Data
public class Inventory {

  @Column(name = "KEY")
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long key;

  @Column(name = "ATPSTK")
  private BigDecimal availableStock = BigDecimal.ZERO;

  @Column(name="KID")
  private Long kioskId;

  @Column(name = "MID")
  private Long materialId;

  @Column(name = "SDID")
  private Long sourceDomainId;

  @Column(name = "Q")
  private BigDecimal economicOrderQuantity = BigDecimal.ZERO;

  @Column(name = "CRD")
  private BigDecimal consumptionRateDaily = BigDecimal.ZERO;

  @Column(name = "CRMNL")
  private BigDecimal consumptionRateManual;

  @Column(name = "KNM")
  private String kioskName;

  @Column(name = "LDTDMD")
  private BigDecimal leadTimeDemand = BigDecimal.ZERO;

  @Column(name = "LDTM")
  private BigDecimal leadTime = new BigDecimal(Constants.LEADTIME_DEFAULT);

  @Column(name = "LSEV")
  private Long lastStockEvent;

  @Column(name = "MAX")
  private BigDecimal maxLevel = BigDecimal.ZERO;

  @Column(name = "MNM")
  private String materialName;

  @Column(name = "UON")
  @Temporal(TemporalType.TIMESTAMP)
  private Date updatedOn;

  @Column(name = "ASTK")
  private BigDecimal allocatedStock = BigDecimal.ZERO;

  @Column(name = "TSTK")
  private BigDecimal inTransitStock = BigDecimal.ZERO;

  @Column(name = "IAT")
  private Date inventoryActiveTime;

  @Column(name = "MINDUR")
  private BigDecimal minDuration = BigDecimal.ZERO;

  @Column(name = "MAXDUR")
  private BigDecimal maxDuration = BigDecimal.ZERO;

  @Column(name = "PDOS")
  private BigDecimal predictedDaysOfStock;

  @Column(name = "SID")
  private Long shortId;

  @Column(name = "UB")
  private String updatedBy;


  @Column(name = "CON")
  @Temporal(TemporalType.TIMESTAMP)
  private Date createdOn;


  @Column(name = "PRCT")
  @Temporal(TemporalType.TIMESTAMP)
  private Date retailerPriceUpdatedTime;

  @Column(name = "CRMNLT")
  @Temporal(TemporalType.TIMESTAMP)
  private Date consumptionRateManualUpdatedTime;
}
