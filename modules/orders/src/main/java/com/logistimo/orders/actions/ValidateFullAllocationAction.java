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

package com.logistimo.orders.actions;

import com.logistimo.constants.CharacterConstants;
import com.logistimo.inventory.entity.IInvAllocation;
import com.logistimo.inventory.service.InventoryManagementService;
import com.logistimo.logger.XLog;
import com.logistimo.orders.entity.IDemandItem;
import com.logistimo.orders.entity.IOrder;
import com.logistimo.orders.exception.AllocationNotCompleteException;
import com.logistimo.orders.service.OrderManagementService;
import com.logistimo.services.ServiceException;
import com.logistimo.shipments.entity.IShipment;
import com.logistimo.shipments.entity.IShipmentItem;
import com.logistimo.shipments.service.IShipmentService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;

/**
 * Created by chandrakant on 04/06/19.
 */
@Component
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class ValidateFullAllocationAction {

  private InventoryManagementService inventoryManagementService;
  private OrderManagementService orderService;
  private IShipmentService shipmentService;
  private static final XLog xLogger = XLog.getLog(ValidateFullAllocationAction.class);

  /**
   *
   * @param type allocation type
   * @param typeId orderId/shipmentId
   * @throws ServiceException if at least one of the item is not fully allocated
   */
  public void invoke(IInvAllocation.Type type, String typeId) throws
      ServiceException {
    if(type == IInvAllocation.Type.ORDER) {
      IOrder order = orderService.getOrder(Long.valueOf(typeId), true);
      for(IDemandItem demandItem : order.getItems()) {
        validateItemAllocationForOrder(demandItem);
      }
    } else if (type == IInvAllocation.Type.SHIPMENT) {
      IShipment shipment = shipmentService.getShipment(typeId);
      shipmentService.includeShipmentItems(shipment);
      for(IShipmentItem item : shipment.getShipmentItems()) {
        validateItemAllocationForShipment(shipment, item);
      }
    }
  }

  private void validateItemAllocationForOrder(IDemandItem demandItem)
      throws AllocationNotCompleteException {
    List<IInvAllocation> allocationList =
        inventoryManagementService.getAllocationsByTagMaterial(demandItem.getMaterialId(),
            IInvAllocation.Type.ORDER + CharacterConstants.COLON + demandItem.getOrderId());
    BigDecimal allocatedQuantity = BigDecimal.ZERO;
    for(IInvAllocation ia : allocationList) {
      allocatedQuantity = allocatedQuantity.add(ia.getQuantity());
    }
    if(allocatedQuantity.compareTo(demandItem.getQuantity()) != 0) {
      xLogger.warn("Items are fully allocated for order" + String.valueOf(demandItem.getOrderId()));
      throw new AllocationNotCompleteException("O018");
    }
  }

  private void validateItemAllocationForShipment(IShipment shipment, IShipmentItem item)
      throws AllocationNotCompleteException {
    List<IInvAllocation> allocationList =
        inventoryManagementService.getAllocationsByTypeId(shipment.getServicingKiosk(),
            item.getMaterialId(), IInvAllocation.Type.SHIPMENT, shipment.getShipmentId());
    BigDecimal allocatedQuantity = BigDecimal.ZERO;
    for(IInvAllocation ia : allocationList) {
      allocatedQuantity = allocatedQuantity.add(ia.getQuantity());
    }
    if(allocatedQuantity.compareTo(item.getQuantity()) != 0) {
      xLogger.warn("Items are fully allocated for shipment" + shipment.getShipmentId());
      throw new AllocationNotCompleteException("O018");
    }
  }
}