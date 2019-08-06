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

package com.logistimo.deliveryrequest.fleet;

import com.logistimo.deliveryrequest.actions.AbstractCreateDeliveryRequestAction;
import com.logistimo.deliveryrequest.actions.CreateExtOrderCommand;
import com.logistimo.deliveryrequest.fleet.model.OrderApiResponse;
import com.logistimo.deliveryrequest.fleet.model.orders.FleetExtDeliveryRequest;
import com.logistimo.deliveryrequest.fleet.model.orders.FleetOrderMapper;
import com.logistimo.deliveryrequest.fleet.model.orders.Order;
import com.logistimo.deliveryrequest.models.DeliveryRequestModel;
import com.logistimo.deliveryrequest.models.DeliveryRequestUpdateWrapper;
import com.logistimo.transporters.entity.TransporterApiMetadata;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

/**
 * Created by chandrakant on 13/06/19.
 */
@Component(value = "fleetCreateDeliveryRequestAction")
@Transactional
public class CreateDeliveryRequestAction extends AbstractCreateDeliveryRequestAction {

  private FleetOrderMapper fleetOrderMapper;
  private RestTemplate extRestTemplate;

  @Autowired
  public final void setFleetOrderMapper(FleetOrderMapper fleetOrderMapper) {
    this.fleetOrderMapper = fleetOrderMapper;
  }

  @Autowired
  @Qualifier(value = "extRestTemplate")
  public final void setExtRestTemplate(RestTemplate extRestTemplate) {
    this.extRestTemplate = extRestTemplate;
  }

  @Override
  protected DeliveryRequestUpdateWrapper createDeliveryRequestWithTransporter(
      DeliveryRequestModel model, TransporterApiMetadata transporterApiMetadata) {
    Order fleetOrder = fleetOrderMapper.mapToExtDeliveryRequest(model);
    FleetExtDeliveryRequest<Order> request = new FleetExtDeliveryRequest<>(fleetOrder);
    OrderApiResponse orderResponse = new CreateExtOrderCommand(request, extRestTemplate,
        transporterApiMetadata).execute();
    return fleetOrderMapper.mapToDeliveryRequestInfo(orderResponse.getPayload());
  }
}