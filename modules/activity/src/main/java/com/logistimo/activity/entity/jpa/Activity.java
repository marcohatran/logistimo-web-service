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

package com.logistimo.activity.entity.jpa;

import org.hibernate.annotations.GenericGenerator;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * Created by pratheeka on 24/04/18.
 */

@Entity
@Table(name="ACTIVITY")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Activity {

  @Id
  @GeneratedValue(generator = "uuid")
  @GenericGenerator(name = "uuid", strategy = "org.hibernate.id.UUIDGenerator")
  @javax.persistence.Column(name = "ID")
  private String id;

  @Column(name = "OBJECTID")
  private String objectId;

  @Column(name = "OBJECTTYPE")
  private String objectType;

  @Column(name = "DOMAINID")
  private Long domainId;

  @Column(name="USERID")
  private String userId;

  @Column(name = "CREATEDATE")
  private Date createDate;

  @Column(name = "FIELD")
  private String field;

  @Column(name="ACTION")
  private String action;

  @Column(name = "PREVVALUE")
  private String prevValue;

  @Column(name = "NEWVALUE")
  private String newValue;

  @Column(name = "MESSAGEID")
  private String messageId;

  @Column(name = "TAG")
  private String tag;
}
