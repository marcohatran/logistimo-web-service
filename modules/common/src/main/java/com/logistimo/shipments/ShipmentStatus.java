/*
 * Copyright © 2017 Logistimo.
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

package com.logistimo.shipments;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * Created by Mohan Raja on 29/09/16
 */
public enum ShipmentStatus {
  // PENDING is introduced only for display purpose. Not to be used as a status.
  // Just to maintain consistency with Order. Will be removed once Order status use OPEN.
  OPEN("op"), PENDING("op"), CONFIRMED("cf"), READY_FOR_DISPATCH("rp"), SHIPPED("sp"),
  FULFILLED("fl"), CANCELLED("cn");
  private String value;

  ShipmentStatus(String value) {
    this.value = value;
  }

  public static ShipmentStatus getStatus(String value) {
    for (ShipmentStatus shipmentStatus : ShipmentStatus.values()) {
      if (shipmentStatus.value.equals(value)) {
        return shipmentStatus;
      }
    }
    return null;
  }

  @Override
  public String toString() {
    return value;
  }

  public static final Collection<ShipmentStatus> INACTIVE_SHIPMENT_STATUSES =
      Collections.singletonList(CANCELLED);
  public static final Collection<ShipmentStatus> ALLOCATABLE_STATUSES =
      Arrays.asList(OPEN, PENDING);
  public static final Collection<ShipmentStatus> PRE_SHIP_STATUSES =
      Arrays.asList(OPEN, PENDING, READY_FOR_DISPATCH);
}
