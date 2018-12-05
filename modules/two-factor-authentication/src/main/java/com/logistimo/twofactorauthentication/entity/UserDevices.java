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

package com.logistimo.twofactorauthentication.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import lombok.Data;


/**
 * @author smriti
 */

@Entity
@Table(name="USER_DEVICES")
@Data
public class UserDevices {

  @Id
  @Column(name = "id", updatable = false, nullable = false)
  private String id;

  @Column(name = "user_id", updatable = false)
  private String userId;

  @Column(name = "application_name", updatable = false)
  private Integer applicationName;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "created_on", updatable = false)
  private Date createdOn;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "expires_on", updatable = false)
  private Date expiresOn;
}
