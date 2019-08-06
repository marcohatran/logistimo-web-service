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

package com.logistimo.transporters.entity;

import com.logistimo.transporters.model.TransporterType;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import lombok.Data;

@Entity
@Table(name = "TRANSPORTERS")
@Data
public class Transporter {

  public Transporter() {
    this.transporterApiMetadata = new TransporterApiMetadata();
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ID", insertable = false, updatable = false)
  private Long id;

  @Column(name = "NAME")
  private String name;

  @Column(name = "DESCRIPTION", length = 500)
  private String description;

  @Column(name = "SDID", updatable = false)
  private Long sourceDomainId;

  @Column(name = "API_METADATA_ID", insertable = false, updatable = false)
  private Long apiMetadataId;

  @Column(name = "COUNTRY")
  private String country;

  @Column(name = "COUNTRY_ID")
  private String countryId;

  @Column(name = "STATE")
  private String state;

  @Column(name = "STATE_ID")
  private String stateId;

  @Column(name = "DISTRICT")
  private String district;

  @Column(name = "DISTRICT_ID")
  private String districtId;

  @Column(name = "TALUK")
  private String taluk;

  @Column(name = "TALUK_ID")
  private String talukId;

  @Column(name = "CITY")
  private String city;

  @Column(name = "CITY_ID")
  private String cityId;

  @Column(name = "STREET_ADDRESS")
  private String streetAddress;

  @Column(name = "PIN_CODE")
  private String pinCode;

  @Column(name = "PHONE_NUMBER")
  private String phoneNumber;

  @Column(name = "VEHICLE")
  private String Vehicle;

  @Column(name = "TYPE")
  @Enumerated(EnumType.STRING)
  private TransporterType type;

  @Column(name = "IS_API_SUPPORTED")
  private Boolean isApiSupported;

  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "API_METADATA_ID", referencedColumnName = "ID")
  private TransporterApiMetadata transporterApiMetadata;

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

  @Transient
  private String defaultCategoryId;

  public boolean isApiEnabled() {
    return Boolean.TRUE.equals(isApiSupported) && apiMetadataId != null;
  }
}