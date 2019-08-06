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

import java.util.List;

/**
 * Created by vani on 03/11/16.
 */
public class MobileShipmentModel {
  /**
   * Shipment ID
   */
  public String sid;
  /**
   * Status
   */
  public String st;
  /**
   * Shipment status updated on
   */
  public String t;
  /**
   * Status changed to shipped on
   */
  public String ssht;
  /**
   * Shipment updated by user id
   */
  public String uid;
  /**
   * Shipment updated by user name
   */
  public String n;
  /**
   * Transporter name
   */
  public String trsp;
  /**
   * Tracking ID
   */
  public String trid;
  /**
   * Reason for partial shipment
   */
  public String rsnps;
  /**
   * Reason for cancelling shipment
   */
  public String rsnco;
  /**
   * Estimated date of arrival
   */
  public String ead;
  /**
   * Date of actual receipt
   */
  public String dar;
  /**
   * Shipment items
   */
  public List<MobileShipmentItemModel> mt;
  /**
   * Package size
   */
  public String pksz;
  /**
   * Comments
   */
  public MobileConversationModel cmnts;
  /**
   * Reference id
   */
  public String rid;
  /**
   * Sales reference id
   */
  @SerializedName(value = "sales_ref_id")
  private String salesReferenceId;

  public void setSalesReferenceId(String salesReferenceId) {
    this.salesReferenceId = salesReferenceId;
  }

  @SerializedName(value = "dlrqs")
  public List<MobileDeliveryRequestModel> deliveryRequests;

  @SerializedName(value = "cnsg")
  public MobileConsignmentModel  mobileConsignmentModel;

  @SerializedName(value = "trnsp")
  public MobileTransporterModel mobileTransporterModel;

}
