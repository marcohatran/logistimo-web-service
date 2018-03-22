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

import com.logistimo.returns.Status;

import java.sql.Timestamp;
import java.util.Date;

import lombok.Data;

/**
 * Created by pratheeka on 14/03/18.
 */
@Data
public class ReturnsFilters {
  private Long customerId;
  private Status status;
  private Date startDate;
  private Date endDate;
  private Long orderId;
  private Long vendorId;

  private Integer offset;
  private Integer size;

  private Long domainId;
  private boolean limitToUserKiosks;
  private String userId;

  public boolean hasVendorId() {
    return vendorId != null;
  }

  public boolean hasCustomerId() {
    return customerId != null;
  }

  public boolean hasOrderId() {
    return orderId != null;
  }

  public boolean hasStatus() {
    return status != null;
  }

  public boolean hasStartDate() {
    return startDate != null;
  }

  public boolean hasEndDate() {
    return endDate != null;
  }
}
