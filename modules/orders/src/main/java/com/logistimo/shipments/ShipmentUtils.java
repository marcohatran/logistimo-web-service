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

package com.logistimo.shipments;

import com.logistimo.config.models.EventSpec;
import com.logistimo.constants.CharacterConstants;
import com.logistimo.dao.JDOUtils;
import com.logistimo.events.EventConstants;
import com.logistimo.events.entity.IEvent;
import com.logistimo.events.models.CustomOptions;
import com.logistimo.events.processor.EventPublisher;
import com.logistimo.logger.XLog;
import com.logistimo.shipments.entity.IShipment;
import com.logistimo.shipments.entity.IShipmentItem;
import com.logistimo.shipments.entity.IShipmentItemBatch;

import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

public class ShipmentUtils {

  private static final XLog xLogger = XLog.getLog(ShipmentUtils.class);


  public static long extractOrderId(String shipmentId) {
    return Long.parseLong(shipmentId.substring(0, shipmentId.indexOf(CharacterConstants.HYPHEN)));
  }

  public static void includeShipmentItems(IShipment shipment, PersistenceManager pm) {
    if (shipment == null) {
      return;
    }
    Query src = null;
    try {
      src = pm.newQuery("javax.jdo.query.SQL", "SELECT * FROM SHIPMENTITEM WHERE sid = ?");
      src.setClass(JDOUtils.getImplClass(IShipmentItem.class));
      shipment
          .setShipmentItems((List<IShipmentItem>) src.executeWithArray(shipment.getShipmentId()));
      shipment
          .setShipmentItems((List<IShipmentItem>) pm.detachCopyAll(shipment.getShipmentItems()));
      src = pm.newQuery("javax.jdo.query.SQL", "SELECT * FROM SHIPMENTITEMBATCH WHERE siId = ?");
      src.setClass(JDOUtils.getImplClass(IShipmentItemBatch.class));
      for (IShipmentItem iShipmentItem : shipment.getShipmentItems()) {
        List<IShipmentItemBatch>
            sb =
            (List<IShipmentItemBatch>) src.executeWithArray(iShipmentItem.getShipmentItemId());
        iShipmentItem.setShipmentItemBatch((List<IShipmentItemBatch>) pm.detachCopyAll(sb));
      }
    } catch (Exception e) {
      xLogger
          .severe("Error while fetching shipment items for shipment: {0}", shipment.getShipmentId(),
              e);
    } finally {
      if (src != null) {
        src.closeAll();
      }
    }
  }

  // Generate shipment events, if configured
  public static void generateEvent(Long domainId, int eventId, IShipment s, String message,
                             List<String> userIds) {
    try {
      Map<String, Object> params = null;

      if (eventId == IEvent.STATUS_CHANGE) {
        params = new HashMap<>(1);
        params.put(EventConstants.PARAM_STATUS, s.getStatus().toString());
      }
      // Custom options
      CustomOptions customOptions = new CustomOptions();
      if (StringUtils.isNotEmpty(message) || (userIds != null && !userIds.isEmpty())) {
        customOptions.message = message;
        if (userIds != null && !userIds.isEmpty()) {
          Map<Integer, List<String>> userIdsMap = new HashMap<>();
          userIdsMap.put(EventSpec.NotifyOptions.IMMEDIATE, userIds);
          customOptions.userIds = userIdsMap;
        }
      }
      // Generate event, if needed
      EventPublisher.generate(domainId, eventId, params,
          JDOUtils.getImplClass(IShipment.class).getName(), s.getShipmentId(), customOptions);
    } catch (Exception e) {
      xLogger.severe("{0} when generating Shipment event {1} for shipment {2} in domain {3}: {4}",
          e.getClass().getName(), eventId, s.getShipmentId(), domainId, e);
    }
  }

}
