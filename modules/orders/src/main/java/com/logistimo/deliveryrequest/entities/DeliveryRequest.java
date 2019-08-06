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

package com.logistimo.deliveryrequest.entities;

import com.logistimo.deliveryrequest.models.DeliveryRequestStatus;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import lombok.Data;

/**
 * Created by kumargaurav on 06/02/19.
 */
@Entity
@Table(name = "DELIVERY_REQUEST")
@Data
public class DeliveryRequest {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ID", insertable = false, updatable = false)
  private Long id;

  @Column(name = "SHIPMENT_ID")
  private String shipmentId;

  @Column(name = "ORDER_ID")
  private Long orderId;

  @Column(name="DOMAIN_ID")
  private Long domainId;

  @Column(name="SHIPPER_KID")
  private Long shipperKid;

  @Column(name="RECEIVER_KID")
  private Long receiverKid;

  @Column(name="CATEGORY_ID")
  private String categoryId;

  @Column(name = "NO_OF_PACKAGES")
  private Integer numberOfPackages;

  @Column(name = "PICKUP_READY_BY")
  private Date pickupReadyBy;

  @Column(name = "ETA")
  private Date eta;

  @Column(name = "WEIGHT")
  private Double weight;

  @Column(name = "LENGTH")
  private Double length;

  @Column(name = "BREADTH")
  private Double breadth;

  @Column(name = "HEIGHT")
  private Double height;

  @Column(name = "VALUE")
  private Double value;

  @Column(name = "STATUS")
  @Enumerated(EnumType.STRING)
  private DeliveryRequestStatus status;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name="STATUS_UPDATED_AT")
  private Date statusUpdatedAt;

  @Column(name = "DESCRIPTION", length = 500)
  private String description;

  @Column(name = "INSTRUCTIONS", length = 500)
  private String instructions;

  @Column(name = "TRANSPORTER_ID")
  private Long transporterId;

  @Column(name = "TRACKING_ID")
  private String trackingId;

  @Column(name = "DELIVERY_TYPE")
  private String deliveryType;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name="CREATED_AT", updatable = false)
  private Date createdAt;

  @Column(name="CREATED_BY", updatable = false)
  private String createdBy;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name="UPDATED_AT")
  private Date updatedAt;

  @Column(name="UPDATED_BY")
  private String updatedBy;

  @Column(name="TP_REF_NUM")
  private String tpRefNum;
}
