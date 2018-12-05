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

package com.logistimo.returns.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import lombok.Data;

/**
 * Created by pratheeka on 12/06/18.
 */

@Table(name="RETURNS_TRACKING_DETAILS")
@Entity
@Data
public class ReturnsTrackingDetails {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", updatable = false)
  private Long id;

  @Column(name = "returns_id", updatable = true,insertable = true)
  private Long returnsId;

  @OneToOne
  @JoinColumn(name = "returns_id", updatable = false,insertable = false)
  private Returns returns;

  @Column(name = "tracking_id")
  private String trackingId;

  @Temporal(TemporalType.DATE)
  @Column(name="expected_on")
  private Date estimatedArrivalDate;

  @Column(name = "transporter")
  private String transporter;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name="created_at")
  private Date createdAt;

  @Column(name="created_by")
  private String createdBy;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name="updated_at")
  private Date updatedAt;

  @Column(name="updated_by")
  private String updatedBy;

}
