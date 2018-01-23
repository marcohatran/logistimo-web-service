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

package com.logistimo.api.servlets.mobile.builders;

import com.logistimo.constants.Constants;
import com.logistimo.inventory.entity.IInvAllocation;
import com.logistimo.inventory.entity.IInvntryBatch;
import com.logistimo.inventory.service.InventoryManagementService;
import com.logistimo.logger.XLog;
import com.logistimo.materials.entity.IMaterial;
import com.logistimo.materials.service.MaterialCatalogService;
import com.logistimo.orders.entity.IDemandItem;
import com.logistimo.orders.entity.IOrder;
import com.logistimo.orders.service.IDemandService;
import com.logistimo.orders.service.OrderManagementService;
import com.logistimo.proto.MobileDemandItemBatchModel;
import com.logistimo.proto.MobileDemandItemModel;
import com.logistimo.utils.LocalDateUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by vani on 04/11/16.
 */
@Component
public class MobileDemandBuilder {
  private static final XLog xLogger = XLog.getLog(MobileDemandBuilder.class);

  private IDemandService demandService;
  private MaterialCatalogService materialCatalogService;
  private InventoryManagementService inventoryManagementService;
  private OrderManagementService orderManagementService;

  @Autowired
  public void setDemandService(IDemandService demandService) {
    this.demandService = demandService;
  }

  @Autowired
  public void setMaterialCatalogService(MaterialCatalogService materialCatalogService) {
    this.materialCatalogService = materialCatalogService;
  }

  @Autowired
  public void setInventoryManagementService(InventoryManagementService inventoryManagementService) {
    this.inventoryManagementService = inventoryManagementService;
  }

  @Autowired
  public void setOrderManagementService(OrderManagementService orderManagementService) {
    this.orderManagementService = orderManagementService;
  }

  List<MobileDemandItemModel> buildMobileDemandItemModels(List<IDemandItem> items, Locale locale,
                                                          String timezone,
                                                          boolean includeBatchDetails) {
    if (items == null || items.isEmpty()) {
      return null;
    }
    List<MobileDemandItemModel> mdimList = new ArrayList<>(1);
    for (IDemandItem item : items) {
      MobileDemandItemModel
          mdim =
          buildMobileDemandItemModel(item, locale, timezone, includeBatchDetails);
      if (mdim != null) {
        mdimList.add(mdim);
      }
    }
    return mdimList;
  }

  MobileDemandItemModel buildMobileDemandItemModel(IDemandItem item, Locale locale, String timezone,
                                                   boolean includeBatchDetails) {
    if (item == null) {
      return null;
    }
    MobileDemandItemModel mdim = new MobileDemandItemModel();
    mdim.mid = item.getMaterialId();
    mdim.q = item.getQuantity();
    mdim.oq = item.getOriginalQuantity();
    mdim.roq = item.getRecommendedOrderQuantity();
    try {
      mdim.alq =
          demandService.getAllocatedQuantityForDemandItem(item.getIdAsString(), item.getOrderId(),
              item.getMaterialId());
      mdim.mst =
          demandService.getMaterialStatusForDemandItem(item.getIdAsString(), item.getOrderId(),
              item.getMaterialId());
    } catch (Exception e) {
      xLogger
          .warn("Exception while trying to get allocations for demand item with demand item id {0}",
              item.getIdAsString(), e);
    }

    mdim.flq = item.getFulfilledQuantity();
    mdim.rsneoq = item.getShippedDiscrepancyReason();
    mdim.rp = item.getUnitPrice();
    mdim.cu = item.getCurrency();
    mdim.ost = item.getStatus();
    mdim.t = LocalDateUtil.format(item.getTimestamp(), locale, timezone);
    mdim.ms = item.getMessage();
    mdim.rsn = item.getReason();
    try {
      IMaterial m = materialCatalogService.getMaterial(item.getMaterialId());
      mdim.mnm = m.getName();
      if (m.getCustomId() != null && !m.getCustomId().isEmpty()) {
        mdim.cmid = m.getCustomId();
      }
      if (m.isBatchEnabled() && includeBatchDetails) {
        List<MobileDemandItemBatchModel>
            bt =
            buildMobileDemandItemBatchList(item.getOrderId(), item.getMaterialId(), locale,
                timezone);
        if (bt != null && !bt.isEmpty()) {
          mdim.bt = bt;
        }
      }
    } catch (Exception e) {
      xLogger.warn(
          "Ignoring Exception while building mobile demand item model for order id {0}, material {1}",
          item.getOrderId(), item.getMaterialId(), e);
    }

    mdim.rsnirq = item.getReason();
    return mdim;
  }

  List<MobileDemandItemBatchModel> buildMobileDemandItemBatchList(Long oid, Long mid, Locale locale,
                                                                  String timezone) {
    List<MobileDemandItemBatchModel> batches = new ArrayList<>(1);
    try {
      IOrder o = orderManagementService.getOrder(oid);
      Long lkId = o.getServicingKiosk();
      List<IInvAllocation>
          iAllocs =
          inventoryManagementService.getAllocationsByTypeId(lkId, mid, IInvAllocation.Type.ORDER, oid.toString());

      if (iAllocs != null && !iAllocs.isEmpty()) {
        for (IInvAllocation iAlloc : iAllocs) {
          if (iAlloc.getBatchId() != null && !iAlloc.getBatchId().isEmpty()) {
            MobileDemandItemBatchModel mdibm = new MobileDemandItemBatchModel();
            IInvntryBatch b = inventoryManagementService.getInventoryBatch(lkId, mid, iAlloc.getBatchId(), null);
            if (b != null) {
              mdibm.bid = b.getBatchId();
              if (b.getBatchExpiry() != null) {
                mdibm.bexp =
                    LocalDateUtil.formatCustom(b.getBatchExpiry(), Constants.DATE_FORMAT, timezone);
              }
              if (b.getBatchManufacturer() != null && !b.getBatchManufacturer().isEmpty()) {
                mdibm.bmfnm = b.getBatchManufacturer();
              }
              if (b.getBatchManufacturedDate() != null) {
                mdibm.bmfdt =
                    LocalDateUtil.formatCustom(b.getBatchManufacturedDate(), Constants.DATE_FORMAT,
                        timezone);
              }
              if (b.getTimestamp() != null) {
                mdibm.t = LocalDateUtil.format(b.getTimestamp(), locale, timezone);

              }
            }
            mdibm.alq = iAlloc.getQuantity();
            mdibm.mst = iAlloc.getMaterialStatus();
            batches.add(mdibm);
          }
        }
      }
    } catch (Exception e) {
      xLogger
          .warn("Exception while getting inventory allocation for order {0} for batch material {1}",
              oid, mid, e);
    }
    return batches;
  }


}
