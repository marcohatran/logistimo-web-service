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

import com.logistimo.deliveryrequest.constants.FleetConstants;
import com.logistimo.deliveryrequest.fleet.FleetHystrixCommand;
import com.logistimo.deliveryrequest.fleet.model.QuotationsApiResponse;
import com.logistimo.deliveryrequest.fleet.model.estimates.Quotations;
import com.logistimo.deliveryrequest.fleet.model.orders.FleetExtDeliveryRequest;
import com.logistimo.deliveryrequest.fleet.model.orders.Order;
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
 * Created by chandrakant on 05/06/19.
 */
public class GetExtOrderQuotationCommand extends FleetHystrixCommand<Quotations> {

  private final Order order;
  private final RestTemplate restTemplate;

  public GetExtOrderQuotationCommand(Order order, RestTemplate restTemplate,
                                     TransporterApiMetadata apiMetadata) {
    super(HystrixCommandGroupKey.Factory.asKey(FleetConstants.CLIENT_ID), 20000, apiMetadata);
    this.order = order;
    this.restTemplate = restTemplate;
  }

  @Override
  protected Quotations run() throws Exception {
    HttpEntity<FleetExtDeliveryRequest> httpRequest
        = new HttpEntity<>(new FleetExtDeliveryRequest<>(order), headers());
    try {
      ResponseEntity<QuotationsApiResponse> entity =
          restTemplate.exchange(url(), HttpMethod.POST, httpRequest, QuotationsApiResponse.class);
      return entity.getBody().getPayload();
    } catch (HttpClientErrorException exception) {
      throw new HystrixBadRequestException(exception.getMessage(),
          new HttpBadRequestException(ErrorResponse.getErrorResponse(exception), exception));
    }
  }

  private String url() {
    return getBaseUrl() + FleetConstants.QUOTATION_ENDPOINT;
  }


  private HttpHeaders headers() {
    HttpHeaders headers =  new HttpHeaders();
    headers.set(FleetConstants.TOKEN_HEADER_NAME, getToken());
    return headers;
  }
}