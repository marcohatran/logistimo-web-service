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

import com.logistimo.deliveryrequest.constants.FleetConstants;
import com.logistimo.deliveryrequest.fleet.model.orders.Root;
import com.logistimo.exception.ErrorResponse;
import com.logistimo.exception.HttpBadRequestException;
import com.logistimo.transporters.entity.TransporterApiMetadata;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.exception.HystrixBadRequestException;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Created by chandrakant on 21/05/19.
 */
class CancelExtOrderCommand extends FleetHystrixCommand<Root> {

  private final RestTemplate restTemplate;
  private final String trackingId;

  public CancelExtOrderCommand(RestTemplate restTemplate, String trackingId,
                               TransporterApiMetadata apiMetadata) {
    super(HystrixCommandGroupKey.Factory.asKey(FleetConstants.CLIENT_ID), 5000, apiMetadata);
    this.trackingId = trackingId;
    this.restTemplate = restTemplate;
  }

  @Override
  protected Root run() throws Exception {
    HttpEntity<Root> httpRequest = new HttpEntity<>(new Root(), headers());
    try {
      ResponseEntity<Root> entity = restTemplate.exchange(url(), HttpMethod.POST,
          httpRequest, Root.class);
      return entity.getBody();
    } catch (HttpClientErrorException exception) {
      throw new HystrixBadRequestException(exception.getMessage(),
          new HttpBadRequestException(ErrorResponse.getErrorResponse(exception), exception));
    }
  }

  private String url() {
    return getBaseUrl() + String.format(FleetConstants.CANCEL_ORDER_ENDPOINT, trackingId);
  }

  private HttpHeaders headers() {
    HttpHeaders headers =  new HttpHeaders();
    headers.set(FleetConstants.TOKEN_HEADER_NAME, getToken());
    return headers;
  }
}