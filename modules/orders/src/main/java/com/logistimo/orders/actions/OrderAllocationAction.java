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

package com.logistimo.orders.actions;

import com.logistimo.constants.CharacterConstants;
import com.logistimo.exception.ValidationException;
import com.logistimo.inventory.entity.IInvAllocation;
import com.logistimo.inventory.service.InventoryManagementService;
import com.logistimo.models.shipments.ShipmentItemBatchModel;
import com.logistimo.orders.OrderUtils;
import com.logistimo.orders.entity.IDemandItem;
import com.logistimo.orders.entity.IOrder;
import com.logistimo.orders.models.DemandAllocationRequest;
import com.logistimo.orders.models.OrderAllocationRequest;
import com.logistimo.orders.service.OrderManagementService;
import com.logistimo.services.ServiceException;
import com.logistimo.utils.BigUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by charan on 23/03/18.
 */
@Component
public class OrderAllocationAction {

  @Autowired
  private InventoryManagementService inventoryManagementService;

  @Autowired
  private OrderManagementService orderManagementService;

  public void invoke(OrderAllocationRequest allocationRequest) throws ServiceException {
    IOrder
        order =
        orderManagementService.getOrder(allocationRequest.getOrderId(), true,
            allocationRequest.getPersistenceManager());
    if (order.isStatus(IOrder.CANCELLED) || order.isStatus(IOrder.FULFILLED)) {
      throw new ValidationException("O011", OrderUtils.getStatusDisplay(order.getStatus(), null));
    }
    String oIdStr = String.valueOf(order.getOrderId());
    String tag = IInvAllocation.Type.ORDER.toString() + CharacterConstants.COLON + oIdStr;
    for (DemandAllocationRequest demandAllocationRequest : allocationRequest.getDemandRequests()) {
      validateRequestedQuantity(order, tag, demandAllocationRequest);
      List<ShipmentItemBatchModel>
          batches = buildShipmentItemBatchModels(demandAllocationRequest);
      inventoryManagementService
          .allocate(order.getServicingKiosk(), demandAllocationRequest.getMaterialId(),
              IInvAllocation.Type.ORDER, oIdStr, tag,
              demandAllocationRequest.getAllocatedQuantity(), batches,
              allocationRequest.getUserId(), allocationRequest.getPersistenceManager(),
              demandAllocationRequest.getMaterialStatus());
    }
  }

  private List<ShipmentItemBatchModel> buildShipmentItemBatchModels(
      DemandAllocationRequest demandAllocationRequest) {
    if (!demandAllocationRequest.getDemandBatchAllocationRequests().isEmpty()) {
      return demandAllocationRequest.getDemandBatchAllocationRequests().stream()
          .map(demandBatchAllocationRequest -> {
            ShipmentItemBatchModel details = new ShipmentItemBatchModel();
            details.id = demandBatchAllocationRequest.getBatchId();
            details.q = demandBatchAllocationRequest.getAllocatedQuantity();
            details.smst = demandBatchAllocationRequest.getMaterialStatus();
            return details;
          }).collect(Collectors.toList());

    }
    return null;
  }

  private void validateRequestedQuantity(IOrder order, String tag,
                                         DemandAllocationRequest demandAllocationRequest) {
    List<IInvAllocation> invAllocations = inventoryManagementService
        .getAllocationsByTagMaterial(demandAllocationRequest.getMaterialId(), tag);
    BigDecimal totalShipmentAllocation = BigDecimal.ZERO;
    for (IInvAllocation invAllocation : invAllocations) {
      if (IInvAllocation.Type.SHIPMENT.toString().equals(invAllocation.getType())) {
        totalShipmentAllocation = totalShipmentAllocation
            .add(invAllocation.getQuantity());
      }
    }
    IDemandItem demandItem = order.getItem(demandAllocationRequest.getMaterialId());
    if (demandItem == null) {
      throw new ValidationException("O013", order.getOrderId(),
          demandAllocationRequest.getMaterialId());
    }
    if (BigUtil.greaterThan(
        demandAllocationRequest.getRequestedAllocationQuantity(), demandItem.getQuantity().subtract(
            demandItem.getShippedQuantity().add(totalShipmentAllocation)))) {
      throw new ValidationException("O012", demandItem.getMaterialId(),
          demandAllocationRequest.getRequestedAllocationQuantity(),
          demandItem.getQuantity().subtract(
              demandItem.getShippedQuantity().add(totalShipmentAllocation)));
    }
  }
}
