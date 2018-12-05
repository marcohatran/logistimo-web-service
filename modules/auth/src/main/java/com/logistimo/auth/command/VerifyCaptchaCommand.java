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

package com.logistimo.auth.command;

import com.logistimo.exception.ErrorResponse;
import com.logistimo.exception.HttpBadRequestException;
import com.logistimo.services.utils.ConfigUtil;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.exception.HystrixBadRequestException;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;

/**
 * @author smriti
 */
public class VerifyCaptchaCommand extends HystrixCommand<Boolean> {
  private final RestTemplate restTemplate;
  private final String url;

  public VerifyCaptchaCommand(RestTemplate restTemplate, String captchaResponse) {
    super(HystrixCommandGroupKey.Factory.asKey("verify-captcha"), 5000);
    this.restTemplate = restTemplate;
    this.url = getURL(captchaResponse);
  }

  private String getURL(String captchaResponse) {
    return ConfigUtil.get("captcha.url")
        .concat("?secret=").concat(ConfigUtil.get("captcha.key"))
        .concat("&response=").concat(captchaResponse);
  }


  @Override
  protected Boolean run() throws Exception {
    try {
      HashMap response = restTemplate.getForEntity(url, HashMap.class).getBody();
      return response.get("error-codes") == null;
    } catch (HttpClientErrorException exception) {
      throw new HystrixBadRequestException(exception.getMessage(),
          new HttpBadRequestException(ErrorResponse.getErrorResponse(exception), exception));
    }
  }
}
