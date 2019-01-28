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

package com.logistimo.shipments.service.impl;

import com.logistimo.orders.dao.impl.OrderDao;
import com.logistimo.orders.entity.IOrder;
import com.logistimo.orders.entity.Order;
import com.logistimo.orders.service.impl.DemandService;
import com.logistimo.orders.service.impl.OrderManagementServiceImpl;
import com.logistimo.shipments.entity.IShipment;
import com.logistimo.shipments.entity.Shipment;

import org.junit.BeforeClass;

import javax.jdo.PersistenceManager;

import static org.mockito.Mockito.mock;

/**
 * Created by smriti on 15/11/17.
 */

public class ShipmentServiceTest {

  private static ShipmentService shipmentService;
  private PersistenceManager pm = mock(PersistenceManager.class);
  private static OrderDao orderDao = new OrderDao();
  private IOrder order = new Order();
  private IShipment shipment;

  @BeforeClass
  public static void initServices() {
    shipmentService = new ShipmentService();
    OrderManagementServiceImpl orderManagementService = new OrderManagementServiceImpl();
    orderManagementService.setOrderDao(orderDao);
    orderManagementService.setDemandService(new DemandService());
    shipmentService.setOrderManagementService(orderManagementService);
  }

  private Shipment getShipment(String shipmentId, Long orderId) {
    Shipment shipment = new Shipment();
    shipment.setShipmentId(shipmentId);
    shipment.setOrderId(orderId);
    return shipment;
  }

}
