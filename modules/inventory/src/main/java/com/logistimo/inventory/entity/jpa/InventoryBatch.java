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
@Table(name = "INVNTRYBATCH")
@Data
public class InventoryBatch {

  @Column(name = "KEY")
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long key;

  @Column(name = "BEXP")
  private Date batchExpiry;

  @Column(name = "BID")
  private String batchId;

  @Temporal(TemporalType.DATE)
  @Column(name = "BMFDT")
  private Date manufacturedDate;

  @Column(name = "BMFNM")
  private String manufacturer;

  @Column(name = "KID")
  private Long kioskId;

  @Column(name = "MID")
  private Long materialId;

  @Column(name = "Q")
  private BigDecimal quantity = BigDecimal.ZERO;

  @Column(name = "SDID")
  private Long sourceDomainId;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "T")
  private Date lastUpdatedTime;


  @Column(name = "VLD")
  private Boolean vld = false;

  @Column(name = "ASTK")
  private BigDecimal allocatedStock = BigDecimal.ZERO;

  @Column(name = "ATPSTK")
  private BigDecimal availableStock = BigDecimal.ZERO;
}
