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

package com.logistimo.entities.entity;

import java.util.Date;
import java.util.List;

/**
 * Created by naveensnair on 19/05/17.
 */
public interface IApprover {

  int PRIMARY_APPROVER = 0;
  int SECONDARY_APPROVER = 1;
  String PURCHASE_ORDER = "p";
  String SALES_ORDER = "s";

  Long getId();

  Long getKioskId();

  void setKioskId(Long kid);

  String getUserId();

  void setUserId(String uid);

  Integer getType();

  void setType(Integer type);

  String getOrderType();

  void setOrderType(String orderType);

  Date getCreatedOn();

  void setCreatedOn(Date con);

  String getCreatedBy();

  void setCreatedBy(String cby);

  String getUpdatedBy();

  void setUpdatedBy(String uby);

  List<String> getPrimaryApprovers();

  void setPrimaryApprovers(List<String> pa);

  List<String> getSecondaryApprovers();

  void setSecondaryApprovers(List<String> sa);

  Long getSourceDomainId();

  void setSourceDomainId(Long sdid);
}
