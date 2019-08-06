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

package com.logistimo.transporters.actions;

import com.logistimo.transporters.entity.Transporter;
import com.logistimo.transporters.mapper.TransporterBuilder;
import com.logistimo.transporters.model.TransporterDetailsModel;
import com.logistimo.transporters.repo.TransporterRepository;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GetTransporterDetailsAction {

  private TransporterRepository repository;
  private TransporterBuilder mapper;

  @Autowired
  public void setRepository(TransporterRepository repository) {
    this.repository = repository;
  }

  @Autowired
  public void setTransporterMapper(TransporterBuilder transporterBuilder) {
    this.mapper = transporterBuilder;
  }

  public TransporterDetailsModel invoke(Long transporterId) {
    Transporter transporter = repository.findOne(transporterId);
    if(transporter == null) {
      return null;
    }
    TransporterDetailsModel transporterDetailsModel = mapper.mapToDetailedModel(transporter);
    String obfuscatedSecretKey = obfuscateSecretKey(transporterDetailsModel.getSecretToken());
    transporterDetailsModel.setSecretToken(obfuscatedSecretKey);
    return transporterDetailsModel;
  }

  private String obfuscateSecretKey(String secretKey) {
    final int totalVisibleLength = 6;
    final int halfVisibleLength = totalVisibleLength/2;
    if(StringUtils.isEmpty(secretKey)) {
      return secretKey;
    }
    int length = secretKey.length();
    if(length > totalVisibleLength) {
      return StringUtils.left(secretKey, halfVisibleLength)
             + StringUtils.repeat("*", length - totalVisibleLength)
             + StringUtils.right(secretKey, halfVisibleLength);
    } else {
      return StringUtils.repeat("*", length);
    }
  }
}