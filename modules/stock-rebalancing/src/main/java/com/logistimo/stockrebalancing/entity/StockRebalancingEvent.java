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

import com.logistimo.entities.entity.jpa.Kiosk;
import com.logistimo.materials.entity.jpa.Material;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import java.math.BigDecimal;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Created by nitisha.khandelwal on 23/03/18.
 */

@Data
@Entity
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"executionMetadata", "material", "kiosk",
    "stockRebalancingEventBatches"})
@Table(name = "stock_rebalancing_events")
public class StockRebalancingEvent {

  @Id
  @GeneratedValue(generator = "uuid")
  @GenericGenerator(name = "uuid", strategy = "org.hibernate.id.UUIDGenerator")
  @Column(name = "id", unique = true, nullable = false, updatable = false)
  private String id;

  @NotNull
  @Column(name = "execution_id", nullable = false, updatable = false, insertable = false)
  private String executionId;

  @NotNull
  @Column(name = "kiosk_id", nullable = false, insertable = false, updatable = false)
  private Long kioskId;

  @NotNull
  @Column(name = "material_id", nullable = false, insertable = false, updatable = false)
  private Long materialId;

  @NotNull
  @Column(name = "short_code", nullable = false, insertable = false, updatable = false)
  private String shortCode;

  @NotNull
  @Column(name = "type", nullable = false)
  @Enumerated(EnumType.STRING)
  private TriggerType triggerType;

  @Column(name = "priority")
  @Enumerated(EnumType.STRING)
  private TriggerPriority triggerPriority;

  @NotNull
  @Column(name = "quantity", nullable = false)
  private BigDecimal quantity;

  @NotNull
  @Column(name = "value")
  private BigDecimal value;

  @NotNull
  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private StockRebalancingEventStatus status;


  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "stock_rebalancing_event_id", referencedColumnName = "id")
  @NotFound(action = NotFoundAction.IGNORE)
  private Set<StockRebalancingEventBatch> stockRebalancingEventBatches;

  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "kiosk_id", referencedColumnName = "KIOSKID")
  private Kiosk kiosk;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "material_id", referencedColumnName = "MATERIALID")
  private Material material;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "execution_id", referencedColumnName = "id")
  private ExecutionMetadata executionMetadata;

  public boolean isPrimary() {
    return triggerPriority == TriggerPriority.PRIMARY;
  }
}
