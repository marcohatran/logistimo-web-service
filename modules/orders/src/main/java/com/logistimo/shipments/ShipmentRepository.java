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

import com.logistimo.dao.JDOUtils;
import com.logistimo.logger.XLog;
import com.logistimo.services.impl.PMF;
import com.logistimo.shipments.entity.IShipment;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

@Component
public class ShipmentRepository {

  private static final XLog xLogger = XLog.getLog(ShipmentRepository.class);

  public IShipment getById(String shipmentId) {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    try {
      IShipment shipment = getById(shipmentId, false, pm);
      return pm.detachCopy(shipment);
    } finally {
      pm.close();
    }
  }

  public IShipment getById(String shipmentId, boolean includeShipmentItems,
                                    PersistenceManager pm) {
    IShipment shipment = JDOUtils.getObjectById(IShipment.class, shipmentId, pm);
    if (includeShipmentItems) {
      ShipmentUtils.includeShipmentItems(shipment, pm);
    }
    return shipment;
  }

  public List<IShipment> getByOrderId(Long orderId, PersistenceManager pm) {
    Query query = null;
    try {
      query = pm.newQuery("javax.jdo.query.SQL", "SELECT * FROM SHIPMENT WHERE ORDERID = ?");
      query.setClass(JDOUtils.getImplClass(IShipment.class));
      List list = (List) query.executeWithArray(orderId);
      List<IShipment> shipments = new ArrayList<>(list.size());
      for (Object shipment : list) {
        shipments.add((IShipment) shipment);
      }
      return shipments;
    } catch (Exception e) {
      xLogger.severe("Error while fetching shipments by order id: {0}", orderId, e);
    } finally {
      if (query != null) {
        query.closeAll();
      }
    }
    return Collections.emptyList();
  }
}
