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

package com.logistimo.api.builders.stockrebalancing;

import com.logistimo.api.builders.EntityBuilder;
import com.logistimo.api.converters.StockRebalancingEventConverter;
import com.logistimo.api.models.stockrebalancing.RecommendedTransferModel;
import com.logistimo.api.models.stockrebalancing.StockRebalancingEventBatchModel;
import com.logistimo.entities.auth.EntityAuthoriser;
import com.logistimo.entities.entity.jpa.Kiosk;
import com.logistimo.inventory.entity.IInvntry;
import com.logistimo.inventory.service.InventoryManagementService;
import com.logistimo.logger.XLog;
import com.logistimo.orders.builders.OrderBuilder;
import com.logistimo.orders.entity.IOrder;
import com.logistimo.orders.models.OrderFilters;
import com.logistimo.orders.models.OrderModel;
import com.logistimo.orders.service.OrderManagementService;
import com.logistimo.pagination.Results;
import com.logistimo.services.ServiceException;
import com.logistimo.stockrebalancing.entity.RecommendedTransfer;
import com.logistimo.utils.BigUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by charan on 02/04/18.
 */
@Component
public class RecommendedTransferBuilder {

  private static final XLog xlogger = XLog.getLog(RecommendedTransferBuilder.class);

  @Autowired
  private OrderManagementService orderManagementService;

  @Autowired
  private InventoryManagementService inventoryManagementService;

  @Autowired
  private StockRebalancingEventConverter stockRebalancingEventConverter;

  @Autowired
  private OrderBuilder orderBuilder;

  @Autowired
  private EntityBuilder entityBuilder;

  public List<RecommendedTransferModel> build(List<RecommendedTransfer> recommendedTransfers,
                                              String eventId) {
    return recommendedTransfers.stream().map(
        recommendedTransfer -> convert(recommendedTransfer, eventId)).filter(Optional::isPresent)
        .map(Optional::get).collect(Collectors.toList());
  }


  private Optional<RecommendedTransferModel> convert(RecommendedTransfer recommendedTransfer,
                                                     String eventId) {
    RecommendedTransferModel model = new RecommendedTransferModel();
    Kiosk kiosk;
    if (recommendedTransfer.getSourceEventId().equals(eventId)) {
      kiosk = recommendedTransfer.getDestinationStockRebalancingEvent().getKiosk();
    } else {
      kiosk = recommendedTransfer.getSourceStockRebalancingEvent().getKiosk();
    }
    model.setEntityId(kiosk.getKioskId());
    if (!EntityAuthoriser.check(kiosk.getKioskId())) {
      return Optional.empty();
    }
    model.setEntityName(kiosk.getName());
    model.setLocation(entityBuilder.getLocation(kiosk));
    model.setSourceShortCode(recommendedTransfer.getSourceStockRebalancingEvent().getShortCode());
    model.setDestinationShortCode(recommendedTransfer.getDestinationStockRebalancingEvent().getShortCode());
    Results<IOrder>
        openOrders =
        orderManagementService.getOrders(new OrderFilters()
            .setKioskId(recommendedTransfer.getSourceStockRebalancingEvent().getKioskId())
            .setLinkedKioskId(
                recommendedTransfer.getDestinationStockRebalancingEvent().getKioskId())
            .setSkipVisibilityCheck(Boolean.TRUE)
            .setOtype(IOrder.TYPE_SALE)
            .setOrderType(IOrder.TRANSFER_ORDER)
            .setStatus(IOrder.PENDING, IOrder.CONFIRMED), null);
    if (openOrders.getSize() > 0) {
      model.setOpenTransfers(
          openOrders.getResults().stream()
              .filter(order -> !order.getOrderId().equals(recommendedTransfer.getTransferId()))
              .map(order -> {
                try {
                  return orderBuilder.build(order, false);
                } catch (ServiceException e) {
                  xlogger.warn("Error occured while building order response model", e);
                }
                return null;
              }).filter(orderModel -> orderModel != null).collect(Collectors.toList()));
    }
    model.setQuantity(recommendedTransfer.getQuantity());
    model.setId(recommendedTransfer.getId());
    IInvntry
        inventory;
    try {
      inventory =
          inventoryManagementService
              .getInventory(kiosk.getKioskId(), recommendedTransfer.getMaterialId());
      if (inventory == null) {
        return Optional.empty();
      }
    } catch (ServiceException e) {
      //TODO: Log that inventory was not found.
      return Optional.empty();
    }

    model.setMinStock(inventory.getReorderLevel());
    model.setMaxStock(inventory.getMaxStock());
    model.setCurrentStock(inventory.getStock());
    model.setCost(recommendedTransfer.getCost());
    model.setValue(recommendedTransfer.getValue());

    List<StockRebalancingEventBatchModel>
        batchDetails =
        stockRebalancingEventConverter.convertBatches(
            recommendedTransfer.getSourceStockRebalancingEvent().getStockRebalancingEventBatches());

    model.setBatchDetails(allocateBatches(batchDetails, recommendedTransfer.getQuantity()));
    if (recommendedTransfer.isTransferInitiated()) {
      try {
        model.setTransfer(orderBuilder.build(orderManagementService.getOrder(
            recommendedTransfer.getTransferId(), false), false));
      } catch (ServiceException e) {
        xlogger.severe("Error occurred while building initiated transfer", e);
        OrderModel orderModel = new OrderModel();
        orderModel.setOrderId(recommendedTransfer.getTransferId());
        model.setTransfer(orderModel);
      }
    }

    return Optional.of(model);
  }

  private List<StockRebalancingEventBatchModel> allocateBatches(
      List<StockRebalancingEventBatchModel> batches,
      BigDecimal quantity) {
    List<StockRebalancingEventBatchModel> allocatedBatches = new ArrayList<>();
    if (batches != null && !batches.isEmpty()) {
      Collections.sort(batches);
      BigDecimal quantityToAllocate = quantity;
      for (StockRebalancingEventBatchModel batch : batches) {
        if (BigUtil.greaterThanZero(quantityToAllocate)) {
          BigDecimal allocatedBatchQuantity = quantityToAllocate.min(batch.getQuantity());
          quantityToAllocate = quantityToAllocate.subtract(allocatedBatchQuantity);
          batch.setQuantity(allocatedBatchQuantity);
          if (BigUtil.greaterThanZero(allocatedBatchQuantity)) {
            allocatedBatches.add(batch);
          }
        }
      }
    }
    return allocatedBatches;
  }

}
