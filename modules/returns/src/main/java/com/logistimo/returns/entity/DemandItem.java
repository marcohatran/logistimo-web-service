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

package com.logistimo.returns.entity;

import com.logistimo.orders.entity.Order;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

/**
 * Created by pratheeka on 29/06/18.
 */

@Entity
@Table(name = "DEMANDITEM")
@Data
public class DemandItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ID")
  private Long id;

  @Column(name = "AST")
  private Integer aggregationStatus;

  @Column(name = "CR")
  private String currency;

  @Column(name = "DCT")
  private BigDecimal discountInPercentage;

  @Column(name = "KID")
  private Long kioskId;

  @Column(name = "MID")
  private Long materialId;

  @Column(name = "MS")
  private String message;

  @Column(name = "OID")
  private Long orderId;

  @Column(name = "OQ")
  private BigDecimal originalOrderedQuantity=BigDecimal.ZERO;

  @Column(name = "Q")
  private BigDecimal quantity=BigDecimal.ZERO;

  @Column(name = "SDID")
  private Long sourceDomainId;

  @Column(name = "ST")
  private String status = Order.PENDING;

  @Column(name = "T")
  private Date updatedTime;

  @Column(name = "TX")
  private BigDecimal taxRateInPercentage;

  @Column(name = "UID")
  private String userId;

  @Column(name = "UP")
  private BigDecimal unitPrice;

  @Column(name = "ROQ")
  private BigDecimal recommendedOrderQuantity=BigDecimal.ZERO;

  @Column(name = "ARCAT")
  private Date archivedAt;

  @Column(name = "ARCBY")
  private String archivedBy;

  @Column(name = "RSN")
  private String reason;

  @Column(name = "TTO")
  private Long timeToOrder;

  @Column(name = "FQ")
  private BigDecimal fulfilledQuantity=BigDecimal.ZERO;

  @Column(name = "DQ")
  private BigDecimal discrepancyQuantity=BigDecimal.ZERO;

  @Column(name = "RQ")
  private BigDecimal returnedQuantity = BigDecimal.ZERO;

  @Column(name = "SQ")
  private BigDecimal shippedQuantity;

  @Column(name = "ISQ")
  private BigDecimal inShipmentQuantity;

  @Column(name = "SDRSN")
  public String shippedDiscrepancyReason;

}
