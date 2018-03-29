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

import com.logistimo.inventory.entity.ITransaction;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import javax.jdo.PersistenceManager;

public class UpdateOrderTransactionsModel {
  private final Long domainId;
  private final String userId;
  private final String transType;
  private final List<ITransaction> inventoryTransactions;
  private final Long kioskId;
  private final Long trackingId;
  private final String message;
  private final boolean createOrder;
  private final Long servicingKiosk;
  private final Double latitude;
  private final Double longitude;
  private final Double geoAccuracy;
  private final String geoErrorCode;
  private final String utcExpectedFulfillmentTimeRangesCSV;
  private final String utcConfirmedFulfillmentTimeRange;
  private final BigDecimal payment;
  private final String paymentOption;
  private final String packageSize;
  private final boolean allowEmptyOrders;
  private final List<String> orderTags;
  private final Integer orderType;
  private final Boolean isSalesOrder;
  private final String salesReferenceId;
  private final Date reqByDate;
  private final Date eta;
  private final int source;
  private PersistenceManager pm;
  private final String purchaseReferenceId;
  private final String transferReferenceId;
  private final String referenceId;

  public UpdateOrderTransactionsModel(Long domainId, String userId, String transType,
                                        List<ITransaction> inventoryTransactions, Long kioskId,
                                        Long trackingId, String message, boolean createOrder,
                                        Long servicingKiosk, Double latitude, Double longitude,
                                        Double geoAccuracy, String geoErrorCode,
                                        String utcExpectedFulfillmentTimeRangesCSV,
                                        String utcConfirmedFulfillmentTimeRange, BigDecimal payment,
                                        String paymentOption, String packageSize,
                                        boolean allowEmptyOrders, List<String> orderTags,
                                        Integer orderType, Boolean isSalesOrder, String salesReferenceId,
                                        Date reqByDate, Date eta, int source, PersistenceManager pm,
                                        String purchaseReferenceId, String transferReferenceId, String referenceId) {
    this.domainId = domainId;
    this.userId = userId;
    this.transType = transType;
    this.inventoryTransactions = inventoryTransactions;
    this.kioskId = kioskId;
    this.trackingId = trackingId;
    this.message = message;
    this.createOrder = createOrder;
    this.servicingKiosk = servicingKiosk;
    this.latitude = latitude;
    this.longitude = longitude;
    this.geoAccuracy = geoAccuracy;
    this.geoErrorCode = geoErrorCode;
    this.utcExpectedFulfillmentTimeRangesCSV = utcExpectedFulfillmentTimeRangesCSV;
    this.utcConfirmedFulfillmentTimeRange = utcConfirmedFulfillmentTimeRange;
    this.payment = payment;
    this.paymentOption = paymentOption;
    this.packageSize = packageSize;
    this.allowEmptyOrders = allowEmptyOrders;
    this.orderTags = orderTags;
    this.orderType = orderType;
    this.isSalesOrder = isSalesOrder;
    this.salesReferenceId = salesReferenceId;
    this.reqByDate = reqByDate;
    this.eta = eta;
    this.source = source;
    this.pm = pm;
    this.purchaseReferenceId = purchaseReferenceId;
    this.transferReferenceId = transferReferenceId;
    this.referenceId = referenceId;
  }

  public Long getDomainId() {
    return domainId;
  }

  public String getUserId() {
    return userId;
  }

  public String getTransType() {
    return transType;
  }

  public List<ITransaction> getInventoryTransactions() {
    return inventoryTransactions;
  }

  public Long getKioskId() {
    return kioskId;
  }

  public Long getTrackingId() {
    return trackingId;
  }

  public String getMessage() {
    return message;
  }

  public boolean isCreateOrder() {
    return createOrder;
  }

  public Long getServicingKiosk() {
    return servicingKiosk;
  }

  public Double getLatitude() {
    return latitude;
  }

  public Double getLongitude() {
    return longitude;
  }

  public Double getGeoAccuracy() {
    return geoAccuracy;
  }

  public String getGeoErrorCode() {
    return geoErrorCode;
  }

  public String getUtcExpectedFulfillmentTimeRangesCSV() {
    return utcExpectedFulfillmentTimeRangesCSV;
  }

  public String getUtcConfirmedFulfillmentTimeRange() {
    return utcConfirmedFulfillmentTimeRange;
  }

  public BigDecimal getPayment() {
    return payment;
  }

  public String getPaymentOption() {
    return paymentOption;
  }

  public String getPackageSize() {
    return packageSize;
  }

  public boolean isAllowEmptyOrders() {
    return allowEmptyOrders;
  }

  public List<String> getOrderTags() {
    return orderTags;
  }

  public Integer getOrderType() {
    return orderType;
  }

  public Boolean getIsSalesOrder() {
    return isSalesOrder;
  }

  public String getSalesReferenceId() {
    return salesReferenceId;
  }

  public Date getReqByDate() {
    return reqByDate;
  }

  public Date getEta() {
    return eta;
  }

  public int getSource() {
    return source;
  }

  public PersistenceManager getPm() {
    return pm;
  }

  public String getPurchaseReferenceId() {
    return purchaseReferenceId;
  }

  public String getTransferReferenceId() { return transferReferenceId; }

  public void setPm(PersistenceManager pm) {
    this.pm = pm;
  }

  public String getReferenceId() { return referenceId; }
}
