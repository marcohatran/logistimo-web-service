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

import com.logistimo.returns.entity.values.GeoLocation;
import com.logistimo.returns.entity.values.ReturnsStatus;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import lombok.Data;

/**
 * @author Mohan Raja
 */
@Entity
@Table(name="RETURNS")
@Data
public class Returns {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", updatable = false)
  private Long id;

  @Column(name = "source_domain", updatable = false)
  private Long sourceDomain;

  @Column(name = "order_id", updatable = false)
  private Long orderId;

  @Column(name = "customer_id")
  private Long customerId;

  @Column(name = "vendor_id")
  private Long vendorId;

  @Embedded
  private GeoLocation location;

  @Embedded
  private ReturnsStatus status;

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

  @Column(name = "source")
  private Integer source;

  @OneToMany(fetch = FetchType.LAZY,cascade = CascadeType.ALL)
  @JoinColumn(name = "returns_id", referencedColumnName = "id")
  private List<ReturnsItem> itemList;

  @OneToOne(fetch = FetchType.LAZY,mappedBy = "returns")
  @NotFound(action = NotFoundAction.IGNORE)
  private ReturnsTrackingDetails trackingDetails;
}
