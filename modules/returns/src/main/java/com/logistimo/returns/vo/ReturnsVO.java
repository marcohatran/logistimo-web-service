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

package com.logistimo.returns.vo;


import com.logistimo.returns.Status;

import org.apache.commons.collections.CollectionUtils;

import java.util.Date;
import java.util.List;

import lombok.Data;

/**
 * Created by pratheeka on 13/03/18.
 */


@Data
public class ReturnsVO {
  private Long id;
  private Long sourceDomain;
  private Long orderId;
  private Long customerId;
  private Long vendorId;
  private Date createdAt;
  private String createdBy;
  private Date updatedAt;
  private String updatedBy;
  private Integer source;
  private GeoLocationVO location;
  private ReturnsStatusVO status;
  private List<ReturnsItemVO> items;
  private String comment;
  private String orderType;
  private ReturnsTrackingDetailsVO returnsTrackingDetailsVO;
  private Long updatedTime;

  public Status getStatusValue() {
    return status.getStatus();
  }

  public void setStatusValue(Status status) {
    this.status.setStatus(status);
  }

  public boolean isShipped() {
    return status.getStatus() == Status.SHIPPED;
  }

  public boolean isReceived() {
    return status.getStatus() == Status.RECEIVED;
  }

  public boolean isOpen() {
    return status.getStatus() == Status.OPEN;
  }

  public boolean isCancelled() {
    return status.getStatus() == Status.CANCELLED;
  }

  public boolean hasItems() {
    return CollectionUtils.isNotEmpty(items);
  }

  public boolean hasTrackingDetails() {
    return returnsTrackingDetailsVO != null;
  }
}
