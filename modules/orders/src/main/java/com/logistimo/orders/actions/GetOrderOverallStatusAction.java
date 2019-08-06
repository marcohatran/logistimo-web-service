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

import com.logistimo.activity.entity.IActivity;
import com.logistimo.activity.models.ActivityModel;
import com.logistimo.activity.service.ActivityService;
import com.logistimo.orders.entity.IOrder;
import com.logistimo.pagination.Results;
import com.logistimo.services.ServiceException;
import com.logistimo.shipments.entity.IShipment;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by kumargaurav on 28/01/19.
 */
@Component
public class GetOrderOverallStatusAction {

  @Autowired
  private ActivityService activityService;

  public String invoke(List<IShipment> shipments, boolean allItemsInShipments, Long orderId) throws ServiceException {
    boolean hasShipped = false;
    boolean hasFulfilled = false;
    boolean allShipped = !shipments.isEmpty();
    boolean allReadyForDispatch = !shipments.isEmpty();
    boolean allFulfilled = !shipments.isEmpty();
    for (IShipment shipment : shipments) {
      switch (shipment.getStatus()) {
        case SHIPPED:
          hasShipped = true;
          allFulfilled = false;
          allReadyForDispatch = false;
          break;
        case FULFILLED:
          hasFulfilled = true;
          allReadyForDispatch = false;
          break;
        case READY_FOR_DISPATCH:
          allShipped = false;
          allFulfilled = false;
          break;
        case CANCELLED:
          break;
        default:
          allShipped = false;
          allReadyForDispatch = false;
          allFulfilled = false;
      }
    }
    return getNewOrderStatus(allItemsInShipments, allFulfilled, allShipped, allReadyForDispatch,
        hasShipped, hasFulfilled, orderId);
  }

  /**
   * Calculates new status refer #updateOrderStatus
   *
   * @param allItemsInShipments - Indicates that demand items quantity is fully processed and there
   *                            is no Yet to create shipment quantity
   * @param fulfilled           - All shipments in the order are marked as fulfilled.
   * @param shipped             - All Shipments in the Order are shipped.
   * @param hasShipped          - There is at least one shipment in Order which is Shipped.
   * @param hasFulfilled        - There is at least one shipment in Order which is fulfilled.
   * @param orderId             - Order Id.
   * @return new order status for the order.
   */
  private String getNewOrderStatus(boolean allItemsInShipments, boolean fulfilled,
                                   boolean shipped, boolean readyForDispatch,
                                   boolean hasShipped, boolean hasFulfilled, Long orderId)
      throws ServiceException {
    String newOrderStatus;
    if (allItemsInShipments) {
      if (fulfilled) {
        newOrderStatus = IOrder.FULFILLED;
      } else if (shipped) {
        newOrderStatus = IOrder.COMPLETED;
      } else if (readyForDispatch) {
        newOrderStatus = IOrder.READY_FOR_DISPATCH;
      } else if (hasShipped || hasFulfilled) {
        newOrderStatus = IOrder.BACKORDERED;
      } else {
        // If current status not pending or confirmed then
        // check Activity and get previous status, else mark as PENDING.
        newOrderStatus = getPreviousStatus(orderId);
      }
    } else {
      if (hasShipped || hasFulfilled) {
        newOrderStatus = IOrder.BACKORDERED;
      } else {
        // If current status not pending or confirmed then
        // check Activity and get previous status, else mark as PENDING.
        newOrderStatus = getPreviousStatus(orderId);
      }
    }
    return newOrderStatus;
  }



  private String getPreviousStatus(Long orderId) throws ServiceException {
    Results res = activityService.getActivity(String.valueOf(orderId), IActivity.TYPE.ORDER.name(),
        null, null, null, null, null);
    List<ActivityModel> activityList = res.getResults();
    if (CollectionUtils.isNotEmpty(activityList)) {
      for (ActivityModel activity : activityList) {
        if (IOrder.CONFIRMED.equals(activity.newValue) || IOrder.PENDING
            .equals(activity.newValue)) {
          return activity.newValue;
        }
      }
    }
    return IOrder.PENDING;
  }
}
