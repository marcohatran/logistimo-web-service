/*
 * Copyright © 2017 Logistimo.
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

package com.logistimo.orders.builders;

import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.constants.Constants;
import com.logistimo.entities.auth.EntityAuthoriser;
import com.logistimo.entities.builders.EntityMinBuilder;
import com.logistimo.entities.models.CustomerVendor;
import com.logistimo.entities.models.DemandItemModel;
import com.logistimo.entities.models.DurationOfStock;
import com.logistimo.entities.models.Inventory;
import com.logistimo.entities.models.Predictions;
import com.logistimo.inventory.entity.IInvntry;
import com.logistimo.inventory.service.InventoryManagementService;
import com.logistimo.logger.XLog;
import com.logistimo.materials.entity.IMaterial;
import com.logistimo.materials.service.MaterialCatalogService;
import com.logistimo.models.StatusModel;
import com.logistimo.orders.entity.IDemandItem;
import com.logistimo.orders.entity.IOrder;
import com.logistimo.orders.models.OrderModel;
import com.logistimo.orders.service.OrderManagementService;
import com.logistimo.services.ObjectNotFoundException;
import com.logistimo.services.ServiceException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.stream.Collectors;

/**
 * Created by charan on 26/06/17.
 */
@Component
public class OrderBuilder {

  private static final XLog xlogger = XLog.getLog(OrderBuilder.class);

  @Autowired
  OrderManagementService orderManagementService;

  @Autowired
  EntityMinBuilder entityMinBuilder;

  @Autowired
  MaterialCatalogService materialCatalogService;

  @Autowired
  InventoryManagementService inventoryManagementService;

  public OrderModel buildMeta(Long orderId) throws ServiceException, ObjectNotFoundException {
    IOrder order = orderManagementService.getOrder(orderId);
    OrderModel model = build(order, false);
    model.setNumItems(order.getNumberOfItems());
    model.setCreatedAt(order.getCreatedOn());
    model.setType(order.getOrderType());
    return model;
  }

  public OrderModel build(Long orderId) throws ServiceException, ObjectNotFoundException {
    IOrder order = orderManagementService.getOrder(orderId, true);
    return build(order, true);
  }

  public OrderModel build(IOrder order, boolean isExtended) throws ServiceException {

    OrderModel model = new OrderModel();
    if (isExtended) {
      StatusModel status = new StatusModel();
      status.setStatus(order.getStatus());
      status.setUpdatedBy(order.getUpdatedBy());
      status.setUpdatedAt(order.getUpdatedOn());
      model.setStatus(status);
      buildOrderItems(order, model);
    } else {
      model = new OrderModel();
    }
    if (isExtended) {
      model.setCustomer(entityMinBuilder.build(order.getKioskId(), true));
      if (order.getServicingKiosk() != null) {
        model.setVendor(entityMinBuilder.build(order.getServicingKiosk(), true));
      }
    } else {
      model.setCustomer(entityMinBuilder.buildMeta(order.getKioskId()));
      if (order.getServicingKiosk() != null) {
        model.setVendor(entityMinBuilder.buildMeta(order.getServicingKiosk()));
      }
    }
    model.setOrderId(order.getOrderId());
    model.setCreatedAt(order.getCreatedOn());
    model.setExpectedArrivalDate(order.getExpectedArrivalDate());
    model.setDueDate(order.getDueDate());
    model.setNumItems(order.getNumberOfItems());

    return model;
  }

  private void buildOrderItems(IOrder order, OrderModel model) {

    if (order.getItems() != null && !order.getItems().isEmpty()) {
      DomainConfig dc = DomainConfig.getInstance(order.getKioskDomainId());
      String crFreq = dc.getInventoryConfig().getDisplayCRFreq();
      model.setItems(
          order.getItems().stream()
              .map(demandItem -> buildDemandItemModel(order, demandItem, crFreq))
              .filter(demandItemModel -> demandItemModel != null)
              .collect(Collectors.toList()));
    }
  }

  private DemandItemModel buildDemandItemModel(IOrder order, IDemandItem demandItem,
                                               String crFreq) {
    DemandItemModel i = new DemandItemModel();
    Long mid = demandItem.getMaterialId();
    IMaterial m;
    try {
      m = materialCatalogService.getMaterial(mid);
    } catch (Exception e) {
      xlogger.warn("WARNING: " + e.getClass().getName() + " when getting material "
          + demandItem.getMaterialId() + ": " + e.getMessage());
      return null;
    }
    i.setMaterialId(String.valueOf(mid));
    i.setName(m.getName());
    i.setOriginallyOrdered(demandItem.getOriginalQuantity());
    i.setRecommended(demandItem.getRecommendedOrderQuantity());
    i.setOrdered(demandItem.getQuantity());
    Long customerId = order.getKioskId();
    CustomerVendor inventory = new CustomerVendor();
    try {
      if (customerId != null && EntityAuthoriser.authoriseEntity(customerId)) {
        inventory.setCustomer(buildInventory(customerId, mid, crFreq));
      }
    } catch (ServiceException e) {
      xlogger.warn("Exception while checking the Customer {1} access for {0}",
          SecurityUtils.getUserDetails(), customerId, e);
    }
    Long vendorId = order.getKioskId();
    try {
      if (vendorId != null && EntityAuthoriser.authoriseEntity(vendorId)) {
        inventory.setVendor(buildInventory(vendorId, mid));
      }
    } catch (ServiceException e) {
      xlogger.warn("Exception while checking the vendor {1} access for {0}",
          SecurityUtils.getUserDetails(), vendorId, e);
    }
    i.setCustomerVendor(inventory);
    return i;
  }

  private Inventory buildInventory(Long kioskId, Long materialId) {
    return buildInventory(kioskId, materialId, null);
  }

  private Inventory buildInventory(Long kioskId, Long materialId, String crFreq) {
    Inventory inventory = null;
    IInvntry
        iInvntry =
        null;
    try {
      iInvntry = inventoryManagementService.getInventory(kioskId, materialId);
    } catch (ServiceException e) {
      xlogger
          .warn("Issue with getting inventory for kiosk {0},material {1}", kioskId, materialId, e);
    }
    if (iInvntry != null) {
      inventory = new Inventory();
      inventory.setMax(iInvntry.getMaxStock());
      inventory.setMin(iInvntry.getReorderLevel());
      inventory.setStockOnHand(iInvntry.getStock());
      inventory.setAvailableStock(iInvntry.getAvailableStock());
      inventory.setAllocatedStock(iInvntry.getAllocatedStock());
      if (crFreq != null) {
        BigDecimal consumptionRate = getStockConsumptionRate(iInvntry, crFreq);
        BigDecimal sap = inventoryManagementService
            .getStockAvailabilityPeriod(consumptionRate, iInvntry.getStock());
        if (null != sap) {
          DurationOfStock ds = new DurationOfStock();
          ds.setDurationUnit(crFreq);
          ds.setDuration(sap);
          inventory.setDurationOfStock(ds);
        }
        Predictions predictions = new Predictions();
        predictions.setDurationUnit(crFreq);
        predictions.setConsumptionRate(consumptionRate);
        if (iInvntry.getPredictedDaysOfStock() != null) {
          predictions.setStockOutDuration(iInvntry.getPredictedDaysOfStock().intValue());
        }
        inventory.setPredictions(predictions);
      }
    }
    return inventory;
  }

  private BigDecimal getStockConsumptionRate(IInvntry invntry, String freq) {
    BigDecimal crate = null;
    if (Constants.FREQ_DAILY.equals(freq)) {
      crate = invntry.getConsumptionRateDaily();
    } else if (Constants.FREQ_WEEKLY.equals(freq)) {
      crate = invntry.getConsumptionRateWeekly();
    } else if (Constants.FREQ_MONTHLY.equals(freq)) {
      crate = invntry.getConsumptionRateMonthly();
    }
    return crate;
  }

}
