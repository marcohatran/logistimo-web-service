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

package com.logistimo.deliveryrequest.fleet.model.orders;

import com.google.gson.annotations.SerializedName;

import com.logistimo.deliveryrequest.models.ExtDeliveryRequestStatus;
import com.logistimo.exception.InvalidDataException;

/**
 * Created by chandrakant on 06/05/19.
 */
enum FleetOrderStatus implements ExtDeliveryRequestStatus {
  @SerializedName("1")
  DRAFT(1),       // Order hasn't been created - it is only a Draft
  @SerializedName("2")
  APPROVAL_REQUIRED(2), // If we are not sure that provided information is not correct (Currently using for the Customer App)
  @SerializedName("3")
  OPEN(3),
  @SerializedName("4")
  TO_REVIEW(4),   // This is not an actual state, it just says that there is manual intervention needed
  @SerializedName("6")
  PICKED(6),
  @SerializedName("9")
  STG_SD(9),
  @SerializedName("12")
  IN_TRANSIT(12),
  @SerializedName("13")
  STG_ID(13), // Staged at an intermediate depot
  @SerializedName("15")
  STG_DD(15),
  @SerializedName("18")
  OUT_FOR_DELIVERY(18),
  @SerializedName("21")
  DELIVERED(21),
  @SerializedName("24")
  CLOSED(24),
  @SerializedName("27")
  CANCELLED(27);

  private Integer value;
  FleetOrderStatus(Integer value)
  {
    this.value = value;
  }
  public Integer value()
  {
    return this.value;
  }
  @Override
  public String toString()
  {
    return Integer.toString(value);
  }

  public static FleetOrderStatus fromInteger(Integer value)
  {
    switch (value)
    {
      case 1: return DRAFT;
      case 3: return OPEN;
      case 2: return APPROVAL_REQUIRED;
      case 4: return TO_REVIEW;
      case 6: return PICKED;
      case 9: return STG_SD;
      case 12: return IN_TRANSIT;
      case 13: return STG_ID;
      case 15: return STG_DD;
      case 18: return OUT_FOR_DELIVERY;
      case 21: return DELIVERED;
      case 24: return CLOSED;
      case 27: return CANCELLED;
      default: throw new InvalidDataException("Unknown status code received!");
    }
  }
}