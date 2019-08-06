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

import com.logistimo.services.ServiceException;
import com.logistimo.transporters.entity.Transporter;
import com.logistimo.transporters.mapper.TransporterBuilder;
import com.logistimo.transporters.model.TransporterDetailsModel;
import com.logistimo.transporters.model.TransporterType;
import com.logistimo.transporters.repo.TransporterRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class UpdateTransporterAction {

  private TransporterBuilder mapper;
  private TransporterRepository repository;

  @Autowired
  public void setRepository(TransporterRepository repository) {
    this.repository = repository;
  }

  @Autowired
  public void setTransporterMapper(TransporterBuilder transporterBuilder) {
    this.mapper = transporterBuilder;
  }

  public void invoke(Long transporterId, TransporterDetailsModel model, String userId)
      throws ServiceException {
    Transporter transporter = repository.findOne(transporterId);
    if (transporter == null) {
      throw new ServiceException("Error while updating transporter");
    }
    mapper.populateEntity(transporter, model);
    if(transporter.getType() != TransporterType.THIRD_PARTY) {
      transporter.setIsApiSupported(false);
    }
    transporter.setUpdatedBy(userId);
    transporter.setUpdatedAt(new Date());
    repository.save(transporter);
  }

}