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

package com.logistimo.api.models;

import java.util.Date;

/**
 * Created by naveensnair on 14/11/17.
 */
public class BulletinBoardModel {
  private Long bbId;
  private Long dId;
  private String name;
  private String desc;
  private String conf;
  private Long min;
  private Long max;
  private String cBy;
  private Date cOn;
  private String uBy;
  private Date uOn;

  public Long getBulletinBoardId() {
    return bbId;
  }

  public void setBulletinBoardId(Long bbId) {
    this.bbId = bbId;
  }

  public Long getDomainId() {
    return dId;
  }

  public void setDomainId(Long dId) {
    this.dId = dId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return desc;
  }

  public void setDescription(String desc) {
    this.desc = desc;
  }

  public String getDashboards() {
    return conf;
  }

  public void setDashboards(String conf) {
    this.conf = conf;
  }

  public Long getMinScrollTime() {
    return min;
  }

  public void setMinScrollTime(Long min) {
    this.min = min;
  }

  public Long getMaxScrollTime() {
    return max;
  }

  public void setMaxScrollTime(Long max) {
    this.max = max;
  }

  public String getCreatedBy() {
    return cBy;
  }

  public void setCreatedBy(String cBy) {
    this.cBy = cBy;
  }

  public Date getCreatedOn() {
    return cOn;
  }

  public void setCreatedOn(Date cOn) {
    this.cOn = cOn;
  }

  public String getUpdatedBy() {
    return uBy;
  }

  public void setUpdatedBy(String uBy) {
    this.uBy = uBy;
  }

  public Date getUpdatedOn() {
    return uOn;
  }

  public void setUpdatedOn(Date uOn) {
    this.uOn = uOn;
  }
}
