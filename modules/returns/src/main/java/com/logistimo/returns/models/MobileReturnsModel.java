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

package com.logistimo.returns.models;

import com.google.gson.annotations.SerializedName;

import com.logistimo.returns.models.submodels.EntityModel;
import com.logistimo.returns.models.submodels.UserModel;
import com.logistimo.returns.models.submodels.StatusModel;

import java.util.Date;
import java.util.List;

/**
 * @author Mohan Raja
 */
public class MobileReturnsModel {

  @SerializedName("return_id")
  private Long returnId;

  @SerializedName("order_id")
  private Long orderId;

  @SerializedName("order_type")
  private Integer orderType;

  private EntityModel customer;

  private EntityModel vendor;

  private StatusModel status;

  @SerializedName("created_at")
  private String createdAt;

  @SerializedName("created_by")
  private UserModel createdBy;

  @SerializedName("updated_at")
  private String updatedAt;

  @SerializedName("updated_by")
  private UserModel updatedBy;

  private List<ReturnsItemModel> items;

  public Long getReturnId() {
    return returnId;
  }

  public void setReturnId(Long returnId) {
    this.returnId = returnId;
  }

  public Long getOrderId() {
    return orderId;
  }

  public void setOrderId(Long orderId) {
    this.orderId = orderId;
  }

  public Integer getOrderType() {
    return orderType;
  }

  public void setOrderType(Integer orderType) {
    this.orderType = orderType;
  }

  public EntityModel getCustomer() {
    return customer;
  }

  public void setCustomer(EntityModel customer) {
    this.customer = customer;
  }

  public EntityModel getVendor() {
    return vendor;
  }

  public void setVendor(EntityModel vendor) {
    this.vendor = vendor;
  }

  public StatusModel getStatus() {
    return status;
  }

  public void setStatus(StatusModel statusModel) {
    this.status = statusModel;
  }

  public String getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }

  public UserModel getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(UserModel createdBy) {
    this.createdBy = createdBy;
  }

  public String getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(String updatedAt) {
    this.updatedAt = updatedAt;
  }

  public UserModel getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(UserModel updatedBy) {
    this.updatedBy = updatedBy;
  }

  public List<ReturnsItemModel> getItems() {
    return items;
  }

  public void setItems(List<ReturnsItemModel> items) {
    this.items = items;
  }
}
