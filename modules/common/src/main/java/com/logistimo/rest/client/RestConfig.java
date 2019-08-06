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

package com.logistimo.rest.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

import com.logistimo.services.utils.ConfigUtil;

import org.apache.http.Header;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by charan on 22/06/17.
 */
public class RestConfig {

  private RestConfig() {
  }

  public static RestTemplate restTemplate() {
    int timeout = ConfigUtil.getInt("api.client.timeout", 5000);
    return restTemplate(timeout);
  }

  public static RestTemplate restTemplate(int timeout) {
    RestTemplate restTemplate = new RestTemplate(getClientHttpRequestFactory(timeout));
    restTemplate.setMessageConverters(Collections.singletonList(createGsonHttpMessageConverter()));
    restTemplate.setInterceptors(Collections.singletonList(new LocaleRequestInterceptor()));
    return restTemplate;
  }

  private static GsonHttpMessageConverter createGsonHttpMessageConverter() {
    Gson gson = new GsonBuilder()
        .registerTypeAdapter(Date.class,
            (JsonDeserializer<Date>) (json, typeOfT, context) -> new Date(
                json.getAsJsonPrimitive().getAsLong()))
        .registerTypeAdapter(Date.class,
            (JsonSerializer<Date>) (date, type, jsonSerializationContext) -> new JsonPrimitive(
                date.getTime()))
        .create();
    GsonHttpMessageConverter gsonConverter = new GsonHttpMessageConverter();
    gsonConverter.setGson(gson);
    return gsonConverter;
  }


  private static ClientHttpRequestFactory getClientHttpRequestFactory(int timeout) {
    RequestConfig config = RequestConfig.custom()
        .setConnectTimeout(timeout)
        .setConnectionRequestTimeout(timeout)
        .setSocketTimeout(timeout)
        .build();
    CloseableHttpClient client = HttpClientBuilder
        .create()
        .setDefaultRequestConfig(config)
        .setDefaultHeaders(Arrays.asList(headers()))
        .setConnectionTimeToLive(60, TimeUnit.SECONDS)
        .build();
    return new HttpComponentsClientHttpRequestFactory(client);
  }

  private static Header[] headers() {
    Header[] headers = {new BasicHeader("Access-Control-Request-Method","POST"),
        new BasicHeader("Access-Control-Request-Method","GET")};
    return headers;
  }
}
