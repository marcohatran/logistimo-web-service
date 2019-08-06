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

import com.google.gson.Gson;

import com.logistimo.config.entity.IConfig;
import com.logistimo.config.models.TransportersSystemConfig;
import com.logistimo.config.service.impl.ConfigurationMgmtServiceImpl;
import com.logistimo.deliveryrequest.actions.AbstractGetShippingOptionsActions;
import com.logistimo.deliveryrequest.actions.GetExtOrderQuotationCommand;
import com.logistimo.deliveryrequest.models.DeliveryRequestModel;
import com.logistimo.deliveryrequest.models.ShippingOptions;
import com.logistimo.deliveryrequest.fleet.model.estimates.Quotations;
import com.logistimo.deliveryrequest.fleet.model.orders.FleetOrderMapper;
import com.logistimo.deliveryrequest.fleet.model.orders.FleetShippingOptionsMapper;
import com.logistimo.deliveryrequest.fleet.model.orders.Order;
import com.logistimo.logger.XLog;
import com.logistimo.services.ServiceException;
import com.logistimo.transporters.entity.TransporterApiMetadata;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;

/**
 * Created by chandrakant on 14/06/19.
 */
@Component(value = "fleetGetShippingOptionsAction")
public class GetQuotationAction extends AbstractGetShippingOptionsActions {

  private static final XLog xLogger = XLog.getLog(GetQuotationAction.class);

  @Autowired
  private FleetOrderMapper orderMapper;
  @Autowired
  @Qualifier(value = "extRestTemplate")
  private RestTemplate extRestTemplate;
  @Autowired
  private FleetShippingOptionsMapper shippingOptionsMapper;

  private ConfigurationMgmtServiceImpl configurationMgmtService;

  @Autowired
  public void setConfigurationMgmtService(ConfigurationMgmtServiceImpl configurationMgmtService) {
    this.configurationMgmtService = configurationMgmtService;
  }

  @Override
  protected ShippingOptions getShippingOptions(DeliveryRequestModel deliveryRequest,
                                            TransporterApiMetadata metadata) {
    Order order = orderMapper.mapToExtDeliveryRequest(deliveryRequest);
    Quotations quotations = new GetExtOrderQuotationCommand(order, extRestTemplate, metadata)
        .execute();
    ShippingOptions shippingOptions = shippingOptionsMapper.map(quotations);
    try {
      IConfig systemConfig = configurationMgmtService.getConfiguration(IConfig.TRANSPORTER_CONFIG);
      TransportersSystemConfig tConfig =
          new Gson().fromJson(systemConfig.getConfig(), TransportersSystemConfig.class);
      if(CollectionUtils.isNotEmpty(tConfig.getTransporters())) {
        tConfig.getTransporters().stream()
            .filter(t -> Objects.equals(t.getId(), metadata.getTspId()))
            .limit(1)
            .forEach(t->shippingOptions.setTspName(t.getName()));
      }
    } catch (ServiceException e) {
      xLogger.warn("Exception while fetching transporter system config");
    }
    return shippingOptions;
  }
}