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

import com.logistimo.returns.entity.values.GeoLocation;
import com.logistimo.returns.entity.values.ReturnsStatus;

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
@Table(name="RETURNS")
public class Returns {

  @Id
  @GeneratedValue(strategy= GenerationType.AUTO)
  @Column(name = "id")
  private Long id;

  @Column(name = "source_domain")
  private Long sourceDomain;

  @Column(name = "order_id")
  private Long orderId;

  @Column(name = "customer_id")
  private Long customerId;

  @Column(name = "vendor_id")
  private Long vendorId;

  @Embedded
  private GeoLocation location;

  @Embedded
  private ReturnsStatus status;

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

  @Column(name = "source")
  private Integer source;

  @Transient
  private List<ReturnsItem> items;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    if(this.id != null) {
      throw new IllegalStateException("Id can't be modified");
    }
    this.id = id;
  }

  public Long getSourceDomain() {
    return sourceDomain;
  }

  public void setSourceDomain(Long sourceDomain) {
    if(this.sourceDomain != null) {
      throw new IllegalStateException("Source domain can't be modified");
    }
    this.sourceDomain = sourceDomain;
  }

  public Long getOrderId() {
    return orderId;
  }

  public void setOrderId(Long orderId) {
    if(this.orderId != null) {
      throw new IllegalStateException("Order can't be modified");
    }
    this.orderId = orderId;
  }

  public Long getCustomerId() {
    return customerId;
  }

  public void setCustomerId(Long customerId) {
    this.customerId = customerId;
  }

  public Long getVendorId() {
    return vendorId;
  }

  public void setVendorId(Long vendorId) {
    this.vendorId = vendorId;
  }

  public GeoLocation getLocation() {
    return location;
  }

  public void setLocation(GeoLocation location) {
    this.location = location;
  }

  public ReturnsStatus getStatus() {
    return status;
  }

  public void setStatus(ReturnsStatus status) {
    this.status = status;
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

  public Integer getSource() {
    return source;
  }

  public void setSource(Integer source) {
    this.source = source;
  }

  public List<ReturnsItem> getItems() {
    return items;
  }

  public void setItems(List<ReturnsItem> items) {
    this.items = items;
  }
}
