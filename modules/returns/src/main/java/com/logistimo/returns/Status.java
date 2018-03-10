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

package com.logistimo.returns;

import com.google.gson.annotations.SerializedName;

/**
 * @author Mohan Raja
 */
public enum Status {
  // PENDING is introduced only for display purpose. Not to be used as a status.
  // Just to maintain consistency with Order. Will be removed once Order status use OPEN.

  @SerializedName("op")
  OPEN("op"),
  @SerializedName("op")
  PENDING("op"),
  @SerializedName("sp")
  SHIPPED("sp"),
  @SerializedName("rd")
  RECEIVED("rd"),
  @SerializedName("cn")
  CANCELLED("cn");

  private String value;

  Status(String value) {
    this.value = value;
  }

  public static Status getStatus(String value) {
    for (Status returnStatus : Status.values()) {
      if (returnStatus.value.equals(value)) {
        return returnStatus;
      }
    }
    return null;
  }

  @Override
  public String toString() {
    return value;
  }
}
