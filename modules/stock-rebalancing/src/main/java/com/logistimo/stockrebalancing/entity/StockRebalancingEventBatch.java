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

package com.logistimo.stockrebalancing.entity;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


/**
 * Created by charan on 30/03/18.
 */
@Data
@Entity
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"stockRebalancingEvent"})
@Table(name = "stock_rebalancing_event_batches")
public class StockRebalancingEventBatch {

  @Id
  @GeneratedValue(generator = "uuid")
  @GenericGenerator(name = "uuid", strategy = "org.hibernate.id.UUIDGenerator")
  @Column(name = "id", unique = true, nullable = false, updatable = false)
  private String id;

  @NotNull
  @Column(name = "stock_rebalancing_event_id", nullable = false, updatable = false, insertable = false)
  private String stockRebalancingEventId;

  @Column(name = "batch_id")
  private String batchId;

  @Column(name = "manufacture_date")
  private Date manufactureDate;

  @Column(name = "expiry_date")
  private Date expiryDate;

  @Column(name = "manufacturer_name")
  private String manufacturerName;

  @Column(name = "transfer_quantity")
  private BigDecimal transferQuantity;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "stock_rebalancing_event_id", referencedColumnName = "id")
  @NotFound(action = NotFoundAction.IGNORE)
  private StockRebalancingEvent stockRebalancingEvent;

}