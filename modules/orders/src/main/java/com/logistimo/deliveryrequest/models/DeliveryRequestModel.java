/*
 * Copyright © 2019 Logistimo.
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

package com.logistimo.deliveryrequest.models;

import com.google.gson.annotations.SerializedName;

import com.logistimo.models.shipments.ConsignmentModel;

import java.util.Date;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by kumargaurav on 12/02/19.
 */
@Data
@NoArgsConstructor
public class DeliveryRequestModel extends DeliveryRequestBase {

  @SerializedName("dId")
  private Long domainId;

  @SerializedName("oId")
  private Long orderId;

  @SerializedName("shipper")
  private Place shipper;

  @SerializedName("receiver")
  private Place receiver;

  @SerializedName("consignment")
  private ConsignmentModel consignment;

  @SerializedName("trckdtls")
  private TrackingDetails trackingDetails;

  @SerializedName("stUpdOn")
  private Date statusUpdatedOn;

  @SerializedName("pickupTime")
  private Date pickupReadyBy;

  @SerializedName("instructions")
  private String instructions;

  @SerializedName("ref_num")
  private String tpRefNum; // third party ref no

  @SerializedName("trURL")
  private String trackingURL;

  @SerializedName("ctgId")
  private String categoryId;
}
