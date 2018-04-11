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

package com.logistimo.orders.models;


import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;


/**
 * Created by charan on 19/07/17.
 */
public class OrderFilters {

  private Long domainId;
  private Long kioskId;

  private List<String> statusList;
  private Date since;
  private Date until;

  //Time after which there is create or update
  private Date updatedSince;

  /**
   * Indicate transfer/order.
   */
  private String otype;
  private String tagType;
  private String tag;
  private List<Long> kioskIds = Collections.emptyList();

  /**
   * Indicates incoming/outgoing or purchase/sale
   */
  private Integer orderType;
  private String salesReferenceId;

  private String approvalStatus;
  boolean withDemand;
  private String userId;
  private Long linkedKioskId;
  private Boolean skipVisibilityCheck = Boolean.FALSE;

  /**
   * Purchase reference Id
   */
  private String purchaseReferenceId;
  /**
   * Transfer reference Id
   */
  private String transferReferenceId;

  private Long materialId;

  public Long getDomainId() {
    return domainId;
  }

  public OrderFilters setDomainId(Long domainId) {
    this.domainId = domainId;
    return this;
  }

  public Long getKioskId() {
    return kioskId;
  }

  public OrderFilters setKioskId(Long kioskId) {
    this.kioskId = kioskId;
    return this;
  }

  public Date getSince() {
    return since;
  }

  public OrderFilters setSince(Date since) {
    this.since = since;
    return this;
  }

  public Date getUntil() {
    return until;
  }

  public OrderFilters setUntil(Date until) {
    this.until = until;
    return this;
  }

  public String getOtype() {
    return otype;
  }

  public OrderFilters setOtype(String otype) {
    this.otype = otype;
    return this;
  }

  public String getTagType() {
    return tagType;
  }

  public OrderFilters setTagType(String tagType) {
    this.tagType = tagType;
    return this;
  }

  public String getTag() {
    return tag;
  }

  public OrderFilters setTag(String tag) {
    this.tag = tag;
    return this;
  }

  public List<Long> getKioskIds() {
    return kioskIds;
  }

  public OrderFilters setKioskIds(List<Long> kioskIds) {
    if (kioskIds != null) {
      this.kioskIds = kioskIds;
    }
    return this;
  }

  public Integer getOrderType() {
    return orderType;
  }

  public OrderFilters setOrderType(Integer orderType) {
    this.orderType = orderType;
    return this;
  }

  public String getSalesReferenceId() {
    return salesReferenceId;
  }

  public OrderFilters setSalesReferenceId(String salesReferenceId) {
    this.salesReferenceId = salesReferenceId;
    return this;
  }

  public String getApprovalStatus() {
    return approvalStatus;
  }

  public OrderFilters setApprovalStatus(String approvalStatus) {
    this.approvalStatus = approvalStatus;
    return this;
  }

  public boolean isWithDemand() {
    return withDemand;
  }

  public OrderFilters setWithDemand(boolean withDemand) {
    this.withDemand = withDemand;
    return this;
  }

  public OrderFilters setUserId(String userId) {
    this.userId = userId;
    return this;
  }

  public String getUserId() {
    return userId;
  }

  public OrderFilters setLinkedKioskId(Long linkedKioskId) {
    this.linkedKioskId = linkedKioskId;
    return this;
  }

  public Long getLinkedKioskId() {
    return linkedKioskId;
  }

  public String getPurchaseReferenceId() {
    return purchaseReferenceId;
  }

  public OrderFilters setPurchaseReferenceId(String purchaseReferenceId) {
    this.purchaseReferenceId = purchaseReferenceId;
    return this;
  }

  public String getTransferReferenceId() {
    return transferReferenceId;
  }

  public OrderFilters setTransferReferenceId(String transferReferenceId) {
    this.transferReferenceId = transferReferenceId;
    return this;
  }

  public Date getUpdatedSince() {
    return updatedSince;
  }

  public OrderFilters setUpdatedSince(Date updatedSince) {
    if (updatedSince != null) {
      this.updatedSince = updatedSince;
    }
    return this;
  }

  public Long getMaterialId() {
    return materialId;
  }

  public OrderFilters setMaterialId(Long materialId) {
    this.materialId = materialId;
    return this;
  }

  public List<String> getStatusList() {
    return statusList;
  }

  public OrderFilters setStatus(String... status) {
    if (!isEmptyOrNull(status)) {
      this.statusList = Arrays.asList(status);
    }
    return this;
  }

  private boolean isEmptyOrNull(String[] status) {
    if (status != null && status.length > 0) {
      for (String status1 : status) {
        if (status1 != null) {
          return false;
        }
      }
    }
    return true;
  }

  public OrderFilters setSkipVisibilityCheck(Boolean skipVisibilityCheck){
    this.skipVisibilityCheck = skipVisibilityCheck;
    return this;
  }

  public Boolean getSkipVisibilityCheck(){
    return this.skipVisibilityCheck;
  }
}
