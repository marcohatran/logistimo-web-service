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

import com.logistimo.models.RequestSource;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import javax.jdo.PersistenceManager;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Data
@Builder
public class OrderRequest {
  private Long domainId;
  private String userId;
  @Singular private List<DemandRequest> demandRequests;
  private Long customerId;
  private String message;
  private Long vendorId;
  private Double latitude;
  private Double longitude;
  private Double geoAccuracy;
  private String geoErrorCode;
  private BigDecimal paymentReceived;
  private String paymentType;
  private String packageSize;
  private List<String> orderTags;
  private OrderType orderType;
  private String referenceId;
  private Date reqByDate;
  private Date eta;
  private RequestSource requestSource;
  private PersistenceManager persistenceManager;
}
