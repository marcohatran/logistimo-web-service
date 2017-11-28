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

package com.logistimo.entities.models;

import java.util.Date;

/**
 * Created by vani on 11/09/17.
 */
public class KioskLinkFilters {
  Long kioskId;
  String linkType;
  String routeTag;
  String startsWith;
  Long linkedKioskId;
  String entityTag;
  Date modifiedSince;
  Boolean routeEnabled = false;

  public Long getKioskId() {
    return kioskId;
  }

  public String getLinkType() {
    return linkType;
  }

  public String getRouteTag() {
    return routeTag;
  }

  public String getStartsWith() {
    return startsWith;
  }

  public Long getLinkedKioskId() {
    return linkedKioskId;
  }

  public String getEntityTag() {
    return entityTag;
  }

  public Date getModifiedSince() {
    return modifiedSince;
  }

  public Boolean getRouteEnabled() {
    return routeEnabled;
  }


  public KioskLinkFilters withKioskId(Long kioskId) {
    this.kioskId = kioskId;
    return this;
  }

  public KioskLinkFilters withLinkType(String linkType) {
    this.linkType = linkType;
    return this;
  }

  public KioskLinkFilters withRouteTag(String routeTag) {
    this.routeTag = routeTag;
    return this;
  }

  public KioskLinkFilters withStartsWith(String startsWith) {
    this.startsWith = startsWith;
    return this;
  }

  public KioskLinkFilters withLinkedKioskId(Long linkedKioskId) {
    this.linkedKioskId = linkedKioskId;
    return this;
  }

  public KioskLinkFilters withEntityTag(String entityTag) {
    this.entityTag = entityTag;
    return this;
  }

  public KioskLinkFilters withModifiedSince(Date modifiedSince) {
    this.modifiedSince = modifiedSince;
    return this;
  }

  public KioskLinkFilters withRouteEnabled(Boolean routeEnabled) {
    this.routeEnabled = routeEnabled;
    return this;
  }
}