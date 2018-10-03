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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

/**
 * Created by kumargaurav on 06/09/18.
 */
public class GetEventSummaryCommandTest {


  RestTemplate template;

  @Before
  public void setUp() {
    template = Mockito.mock(RestTemplate.class);
  }

  @Test
  public void testRun() {
    String url = "http://localhost:8080/s2/api";
    Mockito.when(
        template.exchange(Mockito.any(URI.class), Mockito.<HttpMethod>any(),
            Mockito.<HttpEntity<?>>any(), Mockito.<Class<EventSummaryResponse>>any()))
        .thenReturn(new ResponseEntity<>(buildResponse(), HttpStatus.OK));
    EventSummaryResponse response = new GetEventSummaryCommand(template, 1l,"test","en").execute();
    Assert.assertNotNull(response);
  }

  private EventSummaryResponse buildResponse () {
    return new EventSummaryResponse();
  }
}
