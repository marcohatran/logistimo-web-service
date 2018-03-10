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

import com.logistimo.returns.entity.values.ReturnsReceived;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

/**
 * @author Mohan Raja
 */
@Entity
@Table(name="RETURNS_ITEM")
public class ReturnsItem {

  @Id
  @GeneratedValue(strategy= GenerationType.AUTO)
  @Column(name = "id")
  private Long id;

  @Column(name = "returns_id")
  private Long returnsId;

  @Column(name = "material_id")
  private Long materialId;

  @Column(name = "quantity")
  private BigDecimal quantity = BigDecimal.ZERO;

  @Column(name = "material_status")
  private String materialStatus;

  @Column(name = "reason")
  private String reason;

  @Embedded
  private ReturnsReceived received;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name="created_at")
  private Date createdAt;

  @Column(name="created_by")
  private String createdBy;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name="updated_at")
  private Date updatedAt;

  @Column(name="updated_by")
  private String updatedBy;

  @Transient
  private List<ReturnsItemBatch> batches;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    if(this.id != null) {
      throw new IllegalStateException("Id can't be modified");
    }
    this.id = id;
  }

  public Long getReturnsId() {
    return returnsId;
  }

  public void setReturnsId(Long returnsId) {
    this.returnsId = returnsId;
  }

  public Long getMaterialId() {
    return materialId;
  }

  public void setMaterialId(Long materialId) {
    if(this.materialId != null) {
      throw new IllegalStateException("Material Id can't be modified");
    }
    this.materialId = materialId;
  }

  public BigDecimal getQuantity() {
    return quantity;
  }

  public void setQuantity(BigDecimal quantity) {
    this.quantity = quantity;
  }

  public String getMaterialStatus() {
    return materialStatus;
  }

  public void setMaterialStatus(String materialStatus) {
    this.materialStatus = materialStatus;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public ReturnsReceived getReceived() {
    return received;
  }

  public void setReceived(ReturnsReceived received) {
    this.received = received;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Date createdAt) {
    if(this.createdAt != null) {
      throw new IllegalStateException("Created date can't be modified");
    }
    this.createdAt = createdAt;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    if(this.createdBy != null) {
      throw new IllegalStateException("Created by can't be modified");
    }
    this.createdBy = createdBy;
  }

  public Date getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
  }

  public String getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(String updatedBy) {
    this.updatedBy = updatedBy;
  }

  public List<ReturnsItemBatch> getBatches() {
    return batches;
  }

  public void setBatches(List<ReturnsItemBatch> batches) {
    this.batches = batches;
  }
}
