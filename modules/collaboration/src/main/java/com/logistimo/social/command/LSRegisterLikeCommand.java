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

package com.logistimo.social.command;

import com.logistimo.collaboration.core.models.RegisterLikeRequestModel;
import com.logistimo.collaboration.core.models.RegisterLikeResponseModel;
import com.logistimo.exception.ErrorResponse;
import com.logistimo.exception.HttpBadRequestException;
import com.logistimo.services.utils.ConfigUtil;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.exception.HystrixBadRequestException;

import org.glassfish.jersey.uri.internal.JerseyUriBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

/**
 * Created by kumargaurav on 15/11/17.
 */
public class LSRegisterLikeCommand extends HystrixCommand<RegisterLikeResponseModel> {

  private final RegisterLikeRequestModel request;
  private final RestTemplate restTemplate;

  public LSRegisterLikeCommand(RestTemplate restTemplate, RegisterLikeRequestModel request) {
    super(HystrixCommandGroupKey.Factory.asKey("LSCollaborationClient"),
        5000);
    this.restTemplate = restTemplate;
    this.request = request;
  }

  @Override
  protected RegisterLikeResponseModel run() throws Exception {
    URI link = JerseyUriBuilder.fromUri(registerLikeUrl()).build();
    try {
      ResponseEntity<RegisterLikeResponseModel>
          entity =
          restTemplate.postForEntity(link, request, RegisterLikeResponseModel.class);
      return entity.getBody();
    } catch (HttpClientErrorException exception) {
      throw new HystrixBadRequestException(exception.getMessage(),
          new HttpBadRequestException(ErrorResponse.getErrorResponse(exception), exception));
    }
  }

  private String registerLikeUrl() {
    return ConfigUtil
        .get("collaboration-service.url", "http://localhost:9070/v1/collaboration/likes");
  }
}
