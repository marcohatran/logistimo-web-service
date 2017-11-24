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

package com.logistimo.bulletinboard.entity;

import java.util.Date;

/**
 * Created by naveensnair on 14/11/17.
 */
public interface IBulletinBoard {

  Long getBulletinBoardId();

  void setBulletinBoardId(Long bbId);

  Long getDomainId();

  void setDomainId(Long dId);

  String getName();

  void setName(String name);

  String getDescription();

  void setDescription(String desc);

  Long getMinScrollTime();

  void setMinScrollTime(Long min);

  Long getMaxScrollTime();

  void setMaxScrollTime(Long max);

  String getConfiguration();

  void setConfiguration(String conf);

  String getCreatedBy();

  void setCreatedBy(String cBy);

  Date getCreatedOn();

  void setCreatedOn(Date cOn);

  String getUpdatedBy();

  void setUpdatedBy(String uBy);

  Date getUpdatedOn();

  void setUpdatedOn(Date uOn);
}
