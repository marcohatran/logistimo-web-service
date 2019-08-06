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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public enum DeliveryRequestStatus {
  DRAFT("dr_df"),
  PENDING_CONFIRMATION("dr_op"),
  CONFIRMED("dr_cf"),
  PICKED("dr_pc"),
  OUT_FOR_DELIVERY("dr_od"),
  DELIVERED("dr_dl"),
  CANCELLED("dr_cn");

  private String value;

  DeliveryRequestStatus(String val) {
    this.value = val;
  }

  public String value() {
    return value;
  }

  public static DeliveryRequestStatus fromValue(String value) {
    if(value == null) {
      return null;
    }
    switch (value) {
      case "dr_df":
        return DRAFT;
      case "dr_op":
        return PENDING_CONFIRMATION;
      case "dr_cf":
        return CONFIRMED;
      case "dr_od":
        return OUT_FOR_DELIVERY;
      case "dr_pc":
        return PICKED;
      case "dr_dl":
        return DELIVERED;
      case "dr_cn":
        return CANCELLED;
      default:
        return null;
    }
  }

  public static Collection<String> INACTIVE_DELIVERY_REQUEST_STATUSES =
      Collections.singletonList(CANCELLED.value);
  public static Collection<String> ACTIVE_DELIVERY_REQUEST_STATUSES =
      Arrays.asList(PENDING_CONFIRMATION.value, CONFIRMED.value, PICKED.value, OUT_FOR_DELIVERY
          .value, DELIVERED.value);
}