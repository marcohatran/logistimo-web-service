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

package com.logistimo.shipments.mapper;

import com.logistimo.mapper.Mapper;
import com.logistimo.models.shipments.ConsignmentModel;
import com.logistimo.models.shipments.PackageDimensions;
import com.logistimo.shipments.entity.Consignment;
import com.logistimo.shipments.entity.IConsignment;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ConsignmentMapper implements Mapper<ConsignmentModel, IConsignment> {

  @Override
  public IConsignment mapToEntity(ConsignmentModel model) {
    Consignment consignment = new Consignment();
    populateEntity(consignment, model);
    return consignment;
  }

  @Override
  public ConsignmentModel mapFromEntity(IConsignment consignment) {
    return ConsignmentModel.builder()
        .id(consignment.getId())
        .declaration(consignment.getDeclaration())
        .dimension(new PackageDimensions(consignment.getLength(), consignment.getBreadth(),
            consignment.getHeight()))
        .packageCount(consignment.getNumberOfPackages())
        .value(consignment.getValue())
        .weightInKg(consignment.getWeightInKg().doubleValue())
        .build();
  }

  @Override
  public void populateEntity(IConsignment consignment, ConsignmentModel model) {
    consignment.setId(model.getId());
    consignment.setNumberOfPackages(model.getPackageCount());
    if(model.getWeightInKg() != null) {
      consignment.setWeightInKg(new BigDecimal(model.getWeightInKg()));
    }
    consignment.setDeclaration(model.getDeclaration());
    if(model.getDimension() != null) {
      PackageDimensions dimensions = model.getDimension();
      consignment.setLength(dimensions.getLengthInInches());
      consignment.setBreadth(dimensions.getWidthInInches());
      consignment.setHeight(dimensions.getHeightInInches());
    }
    consignment.setValue(model.getValue());
  }
}