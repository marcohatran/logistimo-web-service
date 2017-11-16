package com.logistimo.shipments.service.impl;

import com.logistimo.dao.JDOUtils;
import com.logistimo.inventory.entity.ITransaction;
import com.logistimo.orders.dao.IOrderDao;
import com.logistimo.orders.dao.impl.OrderDao;
import com.logistimo.orders.entity.IOrder;
import com.logistimo.orders.entity.Order;
import com.logistimo.shipments.entity.Shipment;

import junit.framework.TestCase;

import org.junit.Test;

import javax.jdo.PersistenceManager;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by smriti on 15/11/17.
 */
public class ShipmentServiceTest extends TestCase {

  ShipmentService shipmentService = new ShipmentService();
  PersistenceManager pm = mock(PersistenceManager.class);
  IOrderDao orderDao = new OrderDao();
  Order order = new Order();
  Shipment shipment;

  @Test
  public void testGetTrackingObjectType() throws Exception {
    Long orderId = 100l;
    shipment = getShipment("100-1", orderId);
    order.setOrderType(IOrder.NONTRANSFER);
    when(JDOUtils.getObjectById(IOrder.class, orderDao.createKey(orderId), pm)).thenReturn(order);
    when(pm.detachCopy(order)).thenReturn(order);
    String trackingObjectType = shipmentService.getTrackingObjectType(shipment, pm);
    assertEquals(trackingObjectType, ITransaction.TYPE_ORDER_SHIPMENT);
    assertEquals(pm.isClosed(), false);
  }

  @Test
  public void testGetTrackingObjectTypeForTransferOrder() throws Exception {
    Long orderId = 101l;
    shipment = getShipment("101-1", orderId);
    order.setOrderType(IOrder.TRANSFER_ORDER);
    when(JDOUtils.getObjectById(IOrder.class, orderDao.createKey(orderId), pm)).thenReturn(order);
    when(pm.detachCopy(order)).thenReturn(order);
    String trackingObjectType = shipmentService.getTrackingObjectType(shipment, pm);
    assertEquals(trackingObjectType, ITransaction.TYPE_TRANSFER_SHIPMENT);
    assertEquals(pm.isClosed(), false);
  }

  private Shipment getShipment(String shipmentId, Long orderId) {
    Shipment shipment = new Shipment();
    shipment.setShipmentId(shipmentId);
    shipment.setOrderId(orderId);
    return shipment;
  }

}