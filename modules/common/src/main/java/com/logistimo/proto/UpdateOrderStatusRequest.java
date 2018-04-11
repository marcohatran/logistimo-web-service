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

package com.logistimo.proto;


import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * Created by vani on 05/11/16.
 */
public class UpdateOrderStatusRequest {
  /**
   * String version
   */
  public String v;
  /**
   * String userId
   */
  public String uid;
  /**
   * Kiosk/Entity Id
   */
  public Long kid;
  /**
   * Order type
   */
  public String oty = "prc";
  /**
   * Vendor id
   */
  public Long vid;
  /**
   * Tracking ID-Order ID
   */
  public Long tid;
  /**
   * Shipment ID
   */
  public String sid;
  /**
   * Order status
   */
  public String ost;
  /**
   * Message
   */
  public String ms;
  /**
   * Transporter
   */
  public String trsp;
  /**
   * Transporter tracking ID
   */
  public String trid;
  /**
   * Expected Date of arrival
   */
  public Date ead;
  /**
   * Reason for cancellation
   */
  public String rsnco;
  /**
   * Materials for partial fulfillment
   */
  public List<FulfillmentMaterialRequest> mt;
  /**
   * Date of actual receipt
   */
  public Date dar;
  /**
   * Package size
   */
  public String pksz;
  /**
   * Updated time sent in the last response
   */
  public String tm;
  /**
   * Store of the user
   */
  public Long ukid;
  /**
   * Embed request: presently supports only inventory
   */
  public String embed;

  /**
   * Reference Id
   */
  public String rid;

  /**
   * Sales reference Id
   *
   * @return
   */
  @SerializedName(value = "sales_ref_id")
  private String salesRefId;

  public boolean hasPackageSize() {
    return StringUtils.isNotEmpty(pksz);
  }

  public boolean hasTransporter() {
    return StringUtils.isNotEmpty(trsp);
  }

  public boolean hasTrackingId() {
    return StringUtils.isNotEmpty(trid);
  }

  public boolean hasEAD() {
    return ead != null;
  }

  public boolean hasReferenceId() {
    return StringUtils.isNotBlank(rid) || StringUtils.isNotBlank(salesRefId);
  }

  public boolean hasShipmentId() {
    return StringUtils.isNotEmpty(sid);
  }

  public boolean hasOrderId() {
    return tid != null;
  }

  public String getSalesRefId() {
    return salesRefId;
  }

  public void setSalesrefId(String sales_ref_id) {
    this.salesRefId = sales_ref_id; }
}
