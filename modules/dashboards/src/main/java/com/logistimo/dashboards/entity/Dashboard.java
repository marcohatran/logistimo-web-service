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

package com.logistimo.dashboards.entity;

import java.util.Date;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

/**
 * @author Mohan Raja
 */
@PersistenceCapable(identityType = IdentityType.APPLICATION, detachable = "true")
public class Dashboard implements IDashboard {

  @PrimaryKey
  @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
  private Long dbId; // Dashboard ID
  @Persistent
  private Long dId;
  @Persistent
  private String name;
  @Persistent
  private String desc;
  @Persistent
  @Column(length = 24576)
  private String conf;
  @Persistent
  private String title;
  @Persistent
  private String info;
  @Persistent
  private String cBy; // userId of owner of domain
  @Persistent
  private Date cOn;
  @Persistent
  private String uBy;
  @Persistent
  private Date uOn;

  @Override
  public Long getDashboardId() {
    return dbId;
  }

  @Override
  public void setDashboardId(Long dbId) {
    this.dbId = dbId;
  }

  @Override
  public Long getDomainId() {
    return dId;
  }

  @Override
  public void setDomainId(Long dId) {
    this.dId = dId;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String getDescription() {
    return desc;
  }

  @Override
  public void setDescription(String desc) {
    this.desc = desc;
  }

  @Override
  public String getConfiguration() {
    return conf;
  }

  @Override
  public void setConfiguration(String conf) {
    this.conf = conf;
  }

  @Override
  public String getCreatedBy() {
    return cBy;
  }

  @Override
  public void setCreatedBy(String createdBy) {
    this.cBy = createdBy;
  }

  @Override
  public Date getCreatedOn() {
    return cOn;
  }

  @Override
  public void setCreatedOn(Date createdOn) {
    this.cOn = createdOn;
  }

  @Override
  public String getTitle() {
    return title;
  }

  @Override
  public void setTitle(String title) {
    this.title = title;
  }

  @Override
  public String getInfo() {
    return info;
  }

  @Override
  public void setInfo(String info) {
    this.info = info;
  }

  @Override
  public void setUpdatedBy(String updatedBy) {
    this.uBy = updatedBy;
  }

  @Override
  public String getUpdatedBy() {
    return uBy;
  }

  @Override
  public Date getUpdatedOn() {
    return uOn;
  }

  @Override
  public void setUpdatedOn(Date updatedOn) {
    this.uOn = updatedOn;
  }
}
