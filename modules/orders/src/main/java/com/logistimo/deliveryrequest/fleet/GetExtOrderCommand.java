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

import com.logistimo.deliveryrequest.commands.FleetOrderSearchRequest;
import com.logistimo.deliveryrequest.constants.FleetConstants;
import com.logistimo.deliveryrequest.fleet.model.OrdersApiResponse;
import com.logistimo.deliveryrequest.fleet.model.orders.Order;
import com.logistimo.deliveryrequest.fleet.model.orders.Orders;
import com.logistimo.exception.ErrorResponse;
import com.logistimo.exception.HttpBadRequestException;
import com.logistimo.services.ObjectNotFoundException;
import com.logistimo.transporters.entity.TransporterApiMetadata;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.exception.HystrixBadRequestException;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Created by kumargaurav on 01/03/19.
 */
public class GetExtOrderCommand extends FleetHystrixCommand<Order> {

  private final FleetOrderSearchRequest request;
  private final RestTemplate restTemplate;

  public GetExtOrderCommand(FleetOrderSearchRequest request, RestTemplate restTemplate,
                            TransporterApiMetadata apiMetadata) {
    super(HystrixCommandGroupKey.Factory.asKey(FleetConstants.CLIENT_ID), 20000, apiMetadata);
    this.request = request;
    this.restTemplate = restTemplate;
  }

  @Override
  protected Order run() throws Exception {
    HttpEntity<FleetOrderSearchRequest> httpRequest = new HttpEntity<>(request, headers());
    try {
      ResponseEntity<OrdersApiResponse> entity =
          restTemplate.exchange(url(), HttpMethod.POST, httpRequest, OrdersApiResponse.class);
      Orders orders = entity.getBody().getPayload();
      if(orders == null || CollectionUtils.isEmpty(orders.getOrders())) {
        throw new ObjectNotFoundException("No delivery request found for the provided tracking Id");
      }
      return orders.getOrders().stream().findAny().get();
    } catch (HttpClientErrorException exception) {
      throw new HystrixBadRequestException(exception.getMessage(),
          new HttpBadRequestException(ErrorResponse.getErrorResponse(exception), exception));
    }
  }

  private String url() {
    return getBaseUrl() + FleetConstants.GET_ORDER_ENDPOINT;
  }

  private HttpHeaders headers() {
    HttpHeaders headers =  new HttpHeaders();
    headers.set(FleetConstants.TOKEN_HEADER_NAME, getToken());
    return headers;
  }
}
