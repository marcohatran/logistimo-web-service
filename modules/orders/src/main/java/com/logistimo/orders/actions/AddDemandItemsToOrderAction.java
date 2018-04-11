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

import com.logistimo.constants.SourceConstants;
import com.logistimo.dao.JDOUtils;
import com.logistimo.exception.BadRequestException;
import com.logistimo.exception.LogiException;
import com.logistimo.inventory.entity.ITransaction;
import com.logistimo.logger.XLog;
import com.logistimo.orders.OrderUtils;
import com.logistimo.orders.entity.IOrder;
import com.logistimo.orders.models.AddDemandItemsRequest;
import com.logistimo.orders.models.DemandRequest;
import com.logistimo.orders.service.OrderManagementService;
import com.logistimo.services.ServiceException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Created by charan on 30/03/18.
 */
@Component
public class AddDemandItemsToOrderAction {

  private static final XLog LOGGER = XLog.getLog(AddDemandItemsToOrderAction.class);

  @Autowired
  private OrderManagementService orderManagementService;

  /**
   * Add demand items to existing orders
   * Note: This assumes request is being run in a transactional scope.
   *
   * @param addDemandItemsRequest - new demand items request
   */
  public IOrder invoke(AddDemandItemsRequest addDemandItemsRequest) throws LogiException {
    assert
        addDemandItemsRequest.getPersistenceManager() != null : "Persistence manager is required";
    IOrder order = orderManagementService.getOrder(addDemandItemsRequest.getOrderId(), true);
    if (order.isStatus(IOrder.CANCELLED) || order.isStatus(IOrder.FULFILLED)) {
      LOGGER.warn("User {0} tried to add materials to {1} order {2}",
          addDemandItemsRequest.getUserId(),
          OrderUtils.getStatusDisplay(order.getStatus(), Locale.getDefault()),
          order.getOrderId());
      throw new BadRequestException(new ServiceException("O003", order.getOrderId(),
          OrderUtils.getStatusDisplay(order.getStatus(), Locale.getDefault())));
    }
    orderManagementService.modifyOrder(order, addDemandItemsRequest.getUserId(),
        buildTransactions(addDemandItemsRequest, order), new Date(), order.getDomainId(),
        ITransaction.TYPE_REORDER, addDemandItemsRequest.getMessage(), null, null,
        null,
        null,
        true, null, order.getSalesReferenceID(), addDemandItemsRequest.getPersistenceManager(),
        order.getPurchaseReferenceId(), order.getTransferReferenceId());
    orderManagementService
        .updateOrder(order, SourceConstants.WEB, true, true, addDemandItemsRequest.getUserId(),
            addDemandItemsRequest.getPersistenceManager());
    LOGGER.info("Added new materials to order {0} for kiosk {1} with message {2}",
        addDemandItemsRequest.getOrderId(),
        order.getKioskId(), addDemandItemsRequest.getMessage());

    return order;
  }

  private List<ITransaction> buildTransactions(AddDemandItemsRequest addDemandItemsRequest,
                                               IOrder order) {
    Date now = new Date();
    return addDemandItemsRequest.getDemandRequests().stream().map(
        demandRequest -> getInventoryTransaction(order.getDomainId(),
            order.getKioskId(), addDemandItemsRequest.getUserId(), now, demandRequest)).collect(
        Collectors.toList());
  }

  private ITransaction getInventoryTransaction(Long domainId, Long kioskId, String userId,
                                               Date now, DemandRequest demandRequest) {
    ITransaction t = JDOUtils.createInstance(ITransaction.class);
    t.setKioskId(kioskId);
    t.setMaterialId(demandRequest.getMaterialId());
    t.setQuantity(demandRequest.getQuantity());
    t.setType(ITransaction.TYPE_ORDER);
    t.setDomainId(domainId);
    t.setSourceUserId(userId);
    t.setTimestamp(now);
    t.setBatchId(null);
    t.setBatchExpiry(null);
    t.setReason(demandRequest.getReason());
    return t;
  }
}
