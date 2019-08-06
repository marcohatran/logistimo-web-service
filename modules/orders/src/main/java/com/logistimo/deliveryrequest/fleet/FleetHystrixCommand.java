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

import com.logistimo.exception.InvalidDataException;
import com.logistimo.transporters.entity.TransporterApiMetadata;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;

import org.springframework.stereotype.Component;

/**
 * Created by chandrakant on 28/05/19.
 */
@Component
public abstract class FleetHystrixCommand<T> extends HystrixCommand<T> {
  private TransporterApiMetadata apiMetadata;

  public FleetHystrixCommand(HystrixCommandGroupKey hystrixCommandGroupKey, int timeout,
                      TransporterApiMetadata apiMetadata) {
    super(hystrixCommandGroupKey, timeout);
    if(apiMetadata == null || apiMetadata.getSecret() == null || apiMetadata.getUrl() == null) {
      throw new InvalidDataException("Selected transporter is not API enabled.");
    }
    this.apiMetadata = apiMetadata;
  }

  protected String getToken() {
    return apiMetadata.getSecret();
  }

  protected String getBaseUrl() {
    return apiMetadata.getUrl();
  }
}