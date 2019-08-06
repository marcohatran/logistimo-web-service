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

import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.deliveryrequest.models.DeliveryRequestModel;
import com.logistimo.deliveryrequest.models.ShippingOption;
import com.logistimo.deliveryrequest.models.ShippingOptions;
import com.logistimo.services.ServiceException;
import com.logistimo.transporters.actions.GetTransporterApiMetadataAction;
import com.logistimo.transporters.entity.TransporterApiMetadata;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

/**
 * Created by chandrakant on 14/06/19.
 */
public abstract class AbstractGetShippingOptionsActions implements IGetShippingOptionsAction {

  @Autowired
  RestTemplate extRestTemplate;
  @Autowired
  private GetTransporterApiMetadataAction getTransporterApiMetadataAction;

  protected abstract ShippingOptions getShippingOptions(DeliveryRequestModel deliveryRequest,
                                                     TransporterApiMetadata metadata);

  public ShippingOptions invoke(DeliveryRequestModel model) throws ServiceException {
    TransporterApiMetadata apiMetadata = getTransporterApiMetadataAction.invoke(model
        .getTrackingDetails().getTransporterId());
    ShippingOptions shippingOptions = getShippingOptions(model, apiMetadata);
    // TODO remove
    String currency = DomainConfig.getInstance(SecurityUtils.getCurrentDomainId()).getCurrency();
    for (ShippingOption shippingOption : shippingOptions.getOptions()) {
      shippingOption.setCurrency(currency);
    }
    return shippingOptions;
  }
}