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

package com.logistimo.deliveryrequest.webhook;

import com.logistimo.deliveryrequest.fleet.model.orders.Order;
import com.logistimo.deliveryrequest.webhook.command.WebhookCommand;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by kumargaurav on 25/06/19.
 */
@Component
public class FleetWebhookEventListener {

  @Autowired(required = false)
  private Map<String, WebhookCommand> commands;

  private static final Map<String,String> eventToCommandMapper;

  static {
    eventToCommandMapper = new HashMap<>();
    eventToCommandMapper.put("PICKED","PickedCommand");
    eventToCommandMapper.put("DELIVERED","DeliveredCommand");
    eventToCommandMapper.put("CANCELLED","CancelledCommand");
    eventToCommandMapper.put("ETA_EDIT","ETACommand");
  }

  public void onEvent(Order order, String event) {

    String command = eventToCommandMapper.get(event);
    if (StringUtils.isEmpty(command )) {
      throw new RuntimeException("Invalid event :" + event);
    }

    WebhookCommand webhookCommand = null;
    if(commands != null) {
      webhookCommand = commands.get(command);
    }

    if (webhookCommand == null) {
      throw new RuntimeException("Unsupported update: "+event +"!!");
    }
    webhookCommand.execute(order);
  }
}

