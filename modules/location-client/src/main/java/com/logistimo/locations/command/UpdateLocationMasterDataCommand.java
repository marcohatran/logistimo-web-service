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

package com.logistimo.locations.command;

import com.logistimo.exception.ErrorResponse;
import com.logistimo.exception.HttpBadRequestException;
import com.logistimo.locations.constants.LocationConstants;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.exception.HystrixBadRequestException;

import org.glassfish.jersey.uri.internal.JerseyUriBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

/**
 * Created by kumargaurav on 13/07/17.
 */
public class UpdateLocationMasterDataCommand extends HystrixCommand<Void> {

  private String locationsJson;

  private RestTemplate restClient;

  private MultiValueMap headers;

  private static final Integer TIMED_OUT= 6500;

  public UpdateLocationMasterDataCommand(RestTemplate restClient, String locationsJson,
                                         MultiValueMap headers) {
    super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(LocationConstants.CLIENT_NAME))
        .andCommandPropertiesDefaults(
            HystrixCommandProperties.Setter().
                withExecutionTimeoutInMilliseconds(TIMED_OUT)));
    this.locationsJson = locationsJson;
    this.restClient = restClient;
    this.headers = headers;
  }

  @Override
  protected Void run() throws Exception {
    URI url = JerseyUriBuilder.fromUri(LocationConstants.ADD_LOCATIONS_URL).build();
    HttpEntity<String> request = new HttpEntity<>(locationsJson, headers);
    try {
       restClient.postForEntity(url, request, String.class);
      return null;
    } catch (HttpClientErrorException exception) {
      throw new HystrixBadRequestException(exception.getMessage(),
          new HttpBadRequestException(ErrorResponse.getErrorResponse(exception), exception));
    }
  }
}
