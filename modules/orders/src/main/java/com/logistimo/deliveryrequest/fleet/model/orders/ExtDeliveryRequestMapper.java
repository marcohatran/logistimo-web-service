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

package com.logistimo.deliveryrequest.fleet.model.orders;

import com.logistimo.deliveryrequest.models.ExtDeliveryRequest;
import com.logistimo.deliveryrequest.mapper.DeliveryRequestStatusMapper;
import com.logistimo.deliveryrequest.models.DeliveryRequestModel;
import com.logistimo.deliveryrequest.models.DeliveryRequestUpdateWrapper;

/**
 * Created by chandrakant on 08/05/19.
 */
public abstract class ExtDeliveryRequestMapper<T extends ExtDeliveryRequest> {

  private DeliveryRequestStatusMapper orderStatusMapper;

  public ExtDeliveryRequestMapper(DeliveryRequestStatusMapper orderStatusMapper) {
    this.orderStatusMapper = orderStatusMapper;
  }

  public DeliveryRequestUpdateWrapper mapToDeliveryRequestInfo(T t) {
    DeliveryRequestUpdateWrapper deliveryRequest = new DeliveryRequestUpdateWrapper();
    deliveryRequest.setStatus(orderStatusMapper.map(t.getStatus()).value());
    populateDeliveryRequestModel(t, deliveryRequest);
    return deliveryRequest;
  }

  /**
   * Implement this function to map delivery request to the external delivery request model
   * @param model
   * @return
   */
  public abstract T mapToExtDeliveryRequest(DeliveryRequestModel model);

  /**
   * Implement this method to populate data from external delivery request to the
   * DeliveryRequestBase model
   * @param t
   * @param deliveryRequest
   */
  abstract void populateDeliveryRequestModel(T t, DeliveryRequestUpdateWrapper deliveryRequest);
}