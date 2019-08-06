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

package com.logistimo.shipments.mapper;

import com.logistimo.constants.Constants;
import com.logistimo.entities.entity.IKiosk;
import com.logistimo.entities.service.EntitiesService;
import com.logistimo.inventory.entity.IInvAllocation;
import com.logistimo.inventory.service.InventoryManagementService;
import com.logistimo.materials.entity.IMaterial;
import com.logistimo.materials.service.MaterialCatalogService;
import com.logistimo.models.shipments.ShipmentItemBatchModel;
import com.logistimo.models.shipments.ShipmentItemModel;
import com.logistimo.models.shipments.ShipmentMaterialsModel;
import com.logistimo.services.ServiceException;
import com.logistimo.shipments.ShipmentRepository;
import com.logistimo.shipments.ShipmentStatus;
import com.logistimo.shipments.entity.IShipment;
import com.logistimo.shipments.entity.IShipmentItem;
import com.logistimo.shipments.entity.IShipmentItemBatch;
import com.logistimo.utils.BigUtil;

import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class FulfillmentShipmentEntityModelMapper {

  private static SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT);

  private MaterialCatalogService materialCatalogService;
  private InventoryManagementService inventoryManagementService;
  private ShipmentRepository shipmentRepository;
  private EntitiesService entitiesService;


  public ShipmentMaterialsModel from(String shipmentId, String userId) throws ServiceException {
    IShipment shipment = shipmentRepository.getById(shipmentId, true);
    ShipmentMaterialsModel sModel = new ShipmentMaterialsModel();
    sModel.sId = shipmentId;
    sModel.userId = userId;
    sModel.afd = sdf.format(new Date());
    sModel.items = new ArrayList<>();
    for (IShipmentItem sItem : shipment.getShipmentItems()) {
      mapShipmentItem(shipmentId, shipment, sModel, sItem);
    }
    return sModel;
  }

  private void mapShipmentItem(String shipmentId, IShipment shipment, ShipmentMaterialsModel sModel, IShipmentItem sItem) throws ServiceException {
    ShipmentItemModel siModel = new ShipmentItemModel();
    siModel.mId = sItem.getMaterialId();
    siModel.fq = siModel.q = sItem.getQuantity();
    IMaterial material = materialCatalogService.getMaterial(sItem.getMaterialId());
    IKiosk kiosk = entitiesService.getKiosk(shipment.getServicingKiosk(), false);
    siModel.isBa = material.isBatchEnabled() && kiosk.isBatchMgmtEnabled();
    mapShipmentModelBatches(shipmentId, shipment, sItem, siModel);
    sModel.items.add(siModel);
  }

  private void mapShipmentModelBatches(String shipmentId, IShipment shipment, IShipmentItem sItem, ShipmentItemModel siModel) {
    if (siModel.isBa) {
      siModel.bq = new ArrayList<>();
      if (ShipmentStatus.ALLOCATABLE_STATUSES.contains(shipment.getStatus())) {
        mapAllocatedBatchQuantities(shipmentId, shipment.getServicingKiosk(), siModel);
      } else {
        for (IShipmentItemBatch shipmentItemBatch : sItem.getShipmentItemBatch()) {
          ShipmentItemBatchModel sbModel = new ShipmentItemBatchModel();
          sbModel.fq = sbModel.q = shipmentItemBatch.getQuantity();
          sbModel.id = shipmentItemBatch.getBatchId();
          siModel.bq.add(sbModel);
        }
      }
    }
  }

  private void mapAllocatedBatchQuantities(String shipmentId, Long servicingKiosk,
                                           ShipmentItemModel siModel) {
    List<IInvAllocation> allocations =
        inventoryManagementService.getAllocationsByTypeId(servicingKiosk,
            siModel.mId, IInvAllocation.Type.SHIPMENT, shipmentId);
    if (allocations != null && !allocations.isEmpty()) {
      for (IInvAllocation alloc : allocations) {
        if (BigUtil.greaterThanZero(alloc.getQuantity())) {
          ShipmentItemBatchModel sbModel = new ShipmentItemBatchModel();
          sbModel.fq = sbModel.q = alloc.getQuantity();
          sbModel.id = alloc.getBatchId();
          siModel.bq.add(sbModel);
        }
      }
    }
  }
}
