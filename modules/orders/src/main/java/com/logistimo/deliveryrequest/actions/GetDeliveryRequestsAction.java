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

package com.logistimo.deliveryrequest.actions;

import com.logistimo.deliveryrequest.entities.DeliveryRequest;
import com.logistimo.deliveryrequest.models.DeliveryRequestModel;
import com.logistimo.deliveryrequest.fleet.model.orders.DeliveryRequestMapper;
import com.logistimo.deliveryrequest.repository.DeliveryRequestRepository;
import com.logistimo.pagination.Results;
import com.logistimo.services.ObjectNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;

/**
 * Created by kumargaurav on 06/02/19.
 */
@Component
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class GetDeliveryRequestsAction {

  private DeliveryRequestRepository repository;
  private DeliveryRequestMapper mapper;

  public Results<DeliveryRequestModel> getByShipmentId(String shipmentId, boolean countOnly) {
    List<DeliveryRequestModel> drs = new ArrayList<>();
    int resultSize;
    if(countOnly) {
      resultSize = getDeliveryRequestCountForShipment(shipmentId);
    } else {
      getDeliveryRequestsForShipment(shipmentId).stream()
          .map(mapper::mapFromEntity)
          .forEach(drs::add);
      resultSize = drs.size();
    }
    return new Results<>(drs, resultSize, 0);
  }

  public List<DeliveryRequestModel> getByOrderId(Long orderId) {
    List<DeliveryRequestModel> drs = new ArrayList<>();
    repository.findByOrderId(orderId)
        .stream()
        .map(mapper::mapFromEntity)
        .forEach(drs::add);
    return drs;
  }

  public DeliveryRequestModel get(String trackingId) {
    DeliveryRequest entity = repository.findOneByTrackingId(trackingId);
    if(entity == null) {
      throw new ObjectNotFoundException("No delivery request found with provided service Id");
    }
    return mapper.mapFromEntity(entity);
  }

  public DeliveryRequestModel getById(Long id) {
    DeliveryRequest entity = repository.findOne(id);
    if(entity == null) {
      throw new ObjectNotFoundException("No delivery request found with provided service Id");
    }
    return mapper.mapFromEntity(entity);
  }

  private List<DeliveryRequest> getDeliveryRequestsForShipment(String shipmentId) {
    return repository.findByShipmentId(shipmentId);
  }

  private Integer getDeliveryRequestCountForShipment(String shipmentId) {
    return repository.countByShipmentId(shipmentId);
  }
}

