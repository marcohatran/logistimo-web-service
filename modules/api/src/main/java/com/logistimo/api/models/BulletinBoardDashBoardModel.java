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
 * @author Mohan Raja
 */
public class BulletinBoardDashBoardModel {
  private Long dbId;
  private Long dId;
  private String name;
  private String desc;
  private String widgets;
  private String title;
  private String info;
  private String cBy;
  private Date cOn;
  private String uBy;
  private Date uOn;

  public Long getDashboardId() {
    return dbId;
  }

  public void setDashboardId(Long dbId) {
    this.dbId = dbId;
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

  public String getWidgets() {
    return widgets;
  }

  public void setWidgets(String widgets) {
    this.widgets = widgets;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getInfo() {
    return info;
  }

  public void setInfo(String info) {
    this.info = info;
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
