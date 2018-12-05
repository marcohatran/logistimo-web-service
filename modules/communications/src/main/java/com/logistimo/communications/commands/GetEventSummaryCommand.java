/*
 * Copyright © 2018 Logistimo.
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

package com.logistimo.communications.commands;

import com.logistimo.communications.models.EventSummaryResponse;
import com.logistimo.exception.ErrorResponse;
import com.logistimo.exception.HttpBadRequestException;
import com.logistimo.services.utils.ConfigUtil;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.exception.HystrixBadRequestException;

import org.glassfish.jersey.uri.internal.JerseyUriBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

/**
 * Created by kumargaurav on 05/09/18.
 */
public class GetEventSummaryCommand extends HystrixCommand<EventSummaryResponse> {

  private final Long domainId;
  private final String userId;
  private final String language;
  private final RestTemplate restTemplate;

  public GetEventSummaryCommand(RestTemplate restTemplate, Long domainId, String userId, String lang) {
    super(HystrixCommandGroupKey.Factory.asKey("LSCEventSummaryClient"),
        5000);
    this.restTemplate = restTemplate;
    this.domainId = domainId;
    this.userId = userId;
    this.language = lang;
  }

  private HttpEntity<String> requestHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.add("X-app-name", "logistimo");
    return new HttpEntity<>(headers);
  }

  @Override
  protected EventSummaryResponse run() throws Exception {
    String url = eventSummaryServiceUrl() + "?cn_domain_id=" + domainId + "&user_id=" + userId + "&lang="+language;
    URI link = JerseyUriBuilder.fromUri(url).build();
    try {
      ResponseEntity<EventSummaryResponse>
          entity =
          restTemplate.exchange(link, HttpMethod.GET,requestHeaders(),EventSummaryResponse.class);
      return entity.getBody();
    } catch (HttpClientErrorException exception) {
      throw new HystrixBadRequestException(exception.getMessage(),
          new HttpBadRequestException(ErrorResponse.getErrorResponse(exception), exception));
    }
  }

  private String eventSummaryServiceUrl() {
    return ConfigUtil
        .get("eventsummary-service.url","http://localhost:9010/v1/event-summaries");
  }

}
