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

import com.logistimo.config.models.DomainConfig;
import com.logistimo.dao.JDOUtils;
import com.logistimo.inventory.entity.ITransaction;
import com.logistimo.orders.OrderResults;
import com.logistimo.orders.models.DemandRequest;
import com.logistimo.orders.models.OrderRequest;
import com.logistimo.orders.models.OrderType;
import com.logistimo.orders.models.UpdateOrderTransactionsModel;
import com.logistimo.orders.service.OrderManagementService;
import com.logistimo.services.ServiceException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by charan on 23/03/18.
 */
@Component
public class CreateOrderAction {

  @Autowired
  private OrderManagementService orderManagementService;

  public OrderResults invoke(OrderRequest orderRequest) throws ServiceException {
    Date now = new Date();
    List<ITransaction> transactions = buildTransactions(orderRequest, now);
    DomainConfig domainConfig = DomainConfig.getInstance(orderRequest.getDomainId());
    return orderManagementService
        .updateOrderTransactions(
            new UpdateOrderTransactionsModel(orderRequest.getDomainId(), orderRequest.getUserId(),
                ITransaction.TYPE_ORDER, transactions,
                orderRequest.getCustomerId(), null, orderRequest.getMessage(),
                true, orderRequest.getVendorId(),
                orderRequest.getLatitude(), orderRequest.getLongitude(),
                orderRequest.getGeoAccuracy(),
                orderRequest.getGeoErrorCode(), null,
                null, orderRequest.getPaymentReceived(),
                orderRequest.getPaymentType(), orderRequest.getPackageSize(),
                domainConfig.allowEmptyOrders(), orderRequest.getOrderTags(),
                orderRequest.getOrderType().getOrderTypeCode(), orderRequest.getOrderType().equals(
                OrderType.SALES),
                null, orderRequest.getReqByDate(), orderRequest.getEta(),
                orderRequest.getRequestSource().getSource(), orderRequest.getPersistenceManager(),
                null, null,
                orderRequest.getReferenceId()));
  }

  private List<ITransaction> buildTransactions(OrderRequest orderRequest, Date now) {
    return orderRequest.getDemandRequests().stream().map(
        demandRequest -> getInventoryTransaction(orderRequest.getDomainId(),
            orderRequest.getCustomerId(), orderRequest.getUserId(), now, demandRequest)).collect(
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
