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

package com.logistimo.api.builders;

import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.constants.CharacterConstants;
import com.logistimo.constants.Constants;
import com.logistimo.domains.service.DomainsService;
import com.logistimo.entities.auth.EntityAuthoriser;
import com.logistimo.entities.entity.IKiosk;
import com.logistimo.entities.service.EntitiesService;
import com.logistimo.inventory.entity.IInvAllocation;
import com.logistimo.inventory.entity.IInvntry;
import com.logistimo.inventory.entity.IInvntryBatch;
import com.logistimo.inventory.service.InventoryManagementService;
import com.logistimo.logger.XLog;
import com.logistimo.materials.entity.IHandlingUnit;
import com.logistimo.materials.entity.IMaterial;
import com.logistimo.materials.service.IHandlingUnitService;
import com.logistimo.materials.service.MaterialCatalogService;
import com.logistimo.models.shipments.ShipmentItemBatchModel;
import com.logistimo.models.shipments.ShipmentItemModel;
import com.logistimo.models.shipments.ShipmentModel;
import com.logistimo.orders.entity.IDemandItem;
import com.logistimo.orders.entity.IOrder;
import com.logistimo.orders.service.IDemandService;
import com.logistimo.orders.service.OrderManagementService;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.ServiceException;
import com.logistimo.shipments.ShipmentStatus;
import com.logistimo.shipments.entity.IShipment;
import com.logistimo.shipments.entity.IShipmentItem;
import com.logistimo.shipments.entity.IShipmentItemBatch;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.users.service.UsersService;
import com.logistimo.utils.CommonUtils;
import com.logistimo.utils.LocalDateUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by yuvaraj on 11/10/16.
 */
@Component
public class ShipmentBuilder {
  private static final XLog xLogger = XLog.getLog(ShipmentBuilder.class);

  private UsersService usersService;
  private EntitiesService entitiesService;
  private OrderManagementService orderManagementService;
  private InventoryManagementService inventoryManagementService;
  private IDemandService demandService;
  private DomainsService domainsService;
  private MaterialCatalogService materialCatalogService;
  private IHandlingUnitService handlingUnitService;

  @Autowired
  public void setUsersService(UsersService usersService) {
    this.usersService = usersService;
  }

  @Autowired
  public void setEntitiesService(EntitiesService entitiesService) {
    this.entitiesService = entitiesService;
  }

  @Autowired
  public void setOrderManagementService(OrderManagementService orderManagementService) {
    this.orderManagementService = orderManagementService;
  }

  @Autowired
  public void setInventoryManagementService(InventoryManagementService inventoryManagementService) {
    this.inventoryManagementService = inventoryManagementService;
  }

  @Autowired
  public void setDemandService(IDemandService demandService) {
    this.demandService = demandService;
  }

  @Autowired
  public void setDomainsService(DomainsService domainsService) {
    this.domainsService = domainsService;
  }

  @Autowired
  public void setMaterialCatalogService(MaterialCatalogService materialCatalogService) {
    this.materialCatalogService = materialCatalogService;
  }

  @Autowired
  public void setHandlingUnitService(IHandlingUnitService handlingUnitService) {
    this.handlingUnitService = handlingUnitService;
  }

  public List<ShipmentModel> buildShipmentModels(List<IShipment> shipmentList) {
    List<ShipmentModel> smList = new ArrayList<>(shipmentList.size());
    for (IShipment s : shipmentList) {
      ShipmentModel shipmentModel = buildShipmentModel(s, false);
      if (shipmentModel == null) {
        continue;
      }
      smList.add(shipmentModel);
    }
    return smList;
  }

  public ShipmentModel buildShipmentModel(IShipment s, boolean deep) {
    try {
      SecureUserDetails sUser = SecurityUtils.getUserDetails();
      ShipmentModel model = new ShipmentModel();
      IOrder order;
      try {
        order = orderManagementService.getOrder(s.getOrderId());
        model.oty = order.getOrderType();
      } catch (Exception e) {
        xLogger.warn("Order not available for shipment.", e);
        return null;
      }
      model.sId = s.getShipmentId();
      model.status = s.getStatus() == ShipmentStatus.OPEN ? ShipmentStatus.PENDING : s.getStatus();
      model.statusCode = s.getStatus().toString();
      model.noi = s.getNumberOfItems();
      model.transporter = s.getTransporter();
      model.trackingId = s.getTrackingId();
      model.ps = s.getPackageSize();
      model.reason = s.getReason();
      model.cdrsn = s.getCancelledDiscrepancyReasons();
      model.src=s.getSrc();
      if (order.getUpdatedOn() != null) {
        model.orderUpdatedAt =
            LocalDateUtil.formatCustom(order.getUpdatedOn(), Constants.DATETIME_FORMAT, null);
      }
      if (s.getExpectedArrivalDate() != null) {
        SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT_CSV);
        model.ead = sdf.format(s.getExpectedArrivalDate());
        model.eadLabel =
            LocalDateUtil
                .format(s.getExpectedArrivalDate(), sUser.getLocale(), sUser.getTimezone(), true);
      }
      if (s.getActualFulfilmentDate() != null) {
        model.afd = LocalDateUtil.format(s.getActualFulfilmentDate(), sUser.getLocale(),
            sUser.getTimezone(), true);
      }
      model.userID = s.getCreatedBy();
      try {
        IUserAccount cUser = usersService.getUserAccount(s.getCreatedBy());
        model.createdBy = cUser.getFullName();
      } catch (Exception e) {
        xLogger.warn("Failed to get created sUser Id {0}", s.getCreatedBy(), e);
        model.createdBy = s.getCreatedBy();
      }

      model.cOn = LocalDateUtil.format(s.getCreatedOn(), sUser.getLocale(), sUser.getTimezone());

      if (s.getUpdatedBy() != null) {
        try {
          IUserAccount uUSer = usersService.getUserAccount(s.getUpdatedBy());
          model.upBy = uUSer.getFullName();
        } catch (Exception e) {
          xLogger.warn("Failed to get updated sUser Id", e);
          model.upBy = s.getUpdatedBy();
        }
        model.upId = s.getUpdatedBy();
      }

      if (s.getUpdatedOn() != null) {
        model.upOn = LocalDateUtil.format(s.getUpdatedOn(), sUser.getLocale(), sUser.getTimezone());
      }

      model.customerId = s.getKioskId();
      model.vendorId = s.getServicingKiosk();
      if (model.customerId != null) {
        try {
          IKiosk kiosk = entitiesService.getKiosk(model.customerId, false);
          if(kiosk != null) {
            model.customerAdd = CommonUtils
                .getAddress(kiosk.getCity(), kiosk.getTaluk(), kiosk.getDistrict(),
                    kiosk.getState());
            model.customerName = kiosk.getName();
            if (deep) {
              Integer vPermission = kiosk.getVendorPerm();
              if (vPermission < 2 && model.vendorId != null) {
                vPermission =
                    EntityAuthoriser
                        .authoriseEntityPerm(model.vendorId, sUser.getRole(),
                            sUser.getUsername(), sUser.getDomainId());
              }
              model.atv = vPermission > 1;
              model.atvv = vPermission > 0;
            }
          }
        } catch (ServiceException e) {
          xLogger.warn("Kiosk is not available {0}", model.customerId, e);
        }
      }
      IKiosk kiosk = null;
      if (model.vendorId != null) {
        try {
          kiosk = entitiesService.getKiosk(model.vendorId, false);
          if(kiosk != null) {
            model.vendorAdd = CommonUtils
                .getAddress(kiosk.getCity(), kiosk.getTaluk(), kiosk.getDistrict(),
                    kiosk.getState());
            model.vendorName = kiosk.getName();
            if (deep) {
              Integer cPermission = kiosk.getCustomerPerm();
              if (cPermission < 2) {
                cPermission =
                    EntityAuthoriser
                        .authoriseEntityPerm(model.customerId, sUser.getRole(),
                            sUser.getUsername(), sUser.getDomainId());
              }
              model.atc = cPermission > 1;
              model.atvc = cPermission > 0;
            }
          }
        } catch (ServiceException e) {
          xLogger.warn("Kiosk is not available {0}", model.vendorId, e);
        }
      }
      model.orderId = s.getOrderId();
      model.sdid = s.getDomainId();
      model.sdname = domainsService.getDomain(s.getDomainId()).getName();
      DomainConfig dc = DomainConfig.getInstance(s.getDomainId());

      List<IShipmentItem> items = (List<IShipmentItem>) s.getShipmentItems();
      boolean isOpen = s.getStatus() == ShipmentStatus.OPEN;
      Map<String, BigDecimal> orderAllocations = null;
      Map<Long, BigDecimal> availableQuantity = null;
      if (isOpen) {
        List<IDemandItem> dItems = demandService.getDemandItems(model.orderId);
        availableQuantity = new HashMap<>(dItems.size());
        for (IDemandItem dItem : dItems) {
          availableQuantity
              .put(dItem.getMaterialId(), dItem.getQuantity().subtract(dItem.getShippedQuantity()));
        }
        String tag = IInvAllocation.Type.ORDER + CharacterConstants.COLON + model.orderId;
        List<IInvAllocation> orderAllocationList = inventoryManagementService.getAllocationsByTag(tag);
        if (orderAllocationList != null && orderAllocationList.size() > 0) {
          orderAllocations = new HashMap<>();
          for (IInvAllocation allocation : orderAllocationList) {
            if (IInvAllocation.Type.ORDER.toString().equals(allocation.getType())) {
              if (allocation.getBatchId() == null) {
                orderAllocations
                    .put(String.valueOf(allocation.getMaterialId()), allocation.getQuantity());
              } else {
                orderAllocations.put(allocation.getMaterialId() + allocation.getBatchId(),
                    allocation.getQuantity());
              }
            }
            if (availableQuantity.get(allocation.getMaterialId()) != null) {
              availableQuantity.put(allocation.getMaterialId(),
                  availableQuantity.get(allocation.getMaterialId())
                      .subtract(allocation.getQuantity()));
            }
          }
        }
      }
      if (items != null) {
        model.items = new ArrayList<>(items.size());
        for (IShipmentItem item : items) {
          Long mid = item.getMaterialId();

          IMaterial m = materialCatalogService.getMaterial(item.getMaterialId());
          ShipmentItemModel sim = new ShipmentItemModel();
          sim.mId = mid;
          sim.q = item.getQuantity();
          if (isOpen) {
            sim.aaq = availableQuantity.get(mid);
          }
          sim.mnm = m.getName();
          sim.isBa = (kiosk == null || kiosk.isBatchMgmtEnabled()) && m.isBatchEnabled();
          sim.frsn = item.getFulfilledDiscrepancyReason();
          sim.fq = item.getFulfilledQuantity();
          sim.tm = m.isTemperatureSensitive();
          if (!sim.isBa) {
            sim.fmst = item.getFulfilledMaterialStatus();
            sim.smst = item.getShippedMaterialStatus();
          }
          List<IInvAllocation> allocationList = null;
          List<IShipmentItemBatch> bList = null;
          if (isOpen) {
            allocationList =
                inventoryManagementService.getAllocationsByTypeId(model.vendorId, item.getMaterialId(),
                    IInvAllocation.Type.SHIPMENT, model.sId);
          } else {
            if (sim.isBa) {
              bList = (List<IShipmentItemBatch>) item.getShipmentItemBatch();
            }
          }
          sim.aq = BigDecimal.ZERO;
          if (sim.isBa) {
            sim.bq =
                new ArrayList<>(isOpen ? (allocationList == null ? 0 : allocationList.size())
                    : (bList == null ? 0 : bList.size()));
            if (isOpen) {
              if (allocationList != null && allocationList.size() > 0) {
                for (IInvAllocation iInvAllocation : allocationList) {
                  ShipmentItemBatchModel sibm = getShipmentItemBatchModel(model.vendorId,
                      model.customerId, iInvAllocation.getMaterialId(), iInvAllocation.getBatchId(),
                      iInvAllocation.getQuantity(), null, iInvAllocation.getMaterialStatus(),
                      null, null);
                  String key = iInvAllocation.getMaterialId() + iInvAllocation.getBatchId();
                  if (orderAllocations != null && orderAllocations.containsKey(key)) {
                    sibm.oastk = orderAllocations.get(key);
                  }
                  sim.bq.add(sibm);
                  sim.aq = sim.aq.add(sibm.q);
                }
              }
            } else {
              if (bList != null) {
                for (IShipmentItemBatch sib : bList) {
                  ShipmentItemBatchModel sibm = getShipmentItemBatchModel(model.vendorId,
                      model.customerId, sib.getMaterialId(), sib.getBatchId(), sib.getQuantity(),
                      sib.getFulfilledQuantity(), sib.getShippedMaterialStatus(),
                      sib.getFulfilledMaterialStatus(), sib.getFulfilledDiscrepancyReason());
                  sim.bq.add(sibm);
                  sim.aq = sim.aq.add(sibm.q);
                }
              }
            }
          } else {
            if (isOpen && allocationList.size() > 0) {
              sim.aq = allocationList.get(0).getQuantity();
              sim.smst = allocationList.get(0).getMaterialStatus();
              String key = String.valueOf(item.getMaterialId());
              if (orderAllocations.containsKey(key)) {
                sim.oastk = orderAllocations.get(key);
              }
            }
          }
          IInvntry invVendor = inventoryManagementService.getInventory(s.getServicingKiosk(), sim.mId);
          IInvntry custVendor = inventoryManagementService.getInventory(s.getKioskId(), sim.mId);
          if (invVendor != null) {
            sim.vs = invVendor.getStock();
            sim.vmax = invVendor.getMaxStock();
            sim.vmin = invVendor.getReorderLevel();
            sim.vsavibper = inventoryManagementService.getStockAvailabilityPeriod(invVendor, dc);
            sim.atpstk = invVendor.getAvailableStock();
          }
          if (custVendor != null) {
            sim.stk = custVendor.getStock();
            sim.max = custVendor.getMaxStock();
            sim.min = custVendor.getReorderLevel();
            sim.csavibper = inventoryManagementService.getStockAvailabilityPeriod(custVendor, dc);
          }
          try {
            Map<String, String> hu = handlingUnitService.getHandlingUnitDataByMaterialId(mid);
            if (hu != null) {
              sim.huQty = new BigDecimal(hu.get(IHandlingUnit.QUANTITY));
              sim.huName = hu.get(IHandlingUnit.NAME);
            }
          } catch (Exception e) {
            xLogger.warn("Error while fetching Handling Unit {0}", mid, e);
          }
          model.items.add(sim);
        }
      }
      model.rid = s.getReferenceId();
      return model;
    } catch (Exception e) {
      xLogger.warn("Error while building shipment model", e);
    }
    return null;
  }

  private ShipmentItemBatchModel getShipmentItemBatchModel(Long vendorId, Long customerId,
      Long materialId, String batchId, BigDecimal quantity, BigDecimal fulfilQty, String smst,
      String fmst, String frsn) throws ServiceException {
    ShipmentItemBatchModel sibm = new ShipmentItemBatchModel();
    IInvntryBatch batch;
    batch = inventoryManagementService.getInventoryBatch(vendorId, materialId, batchId, null);
    if (batch == null) {
      batch = inventoryManagementService.getInventoryBatch(customerId, materialId, batchId, null);
    }
    if (batch != null) {
      sibm.e = batch.getBatchExpiry() != null ? LocalDateUtil.formatCustom(batch.getBatchExpiry(),
          "dd/MM/yyyy", null) : "";
      sibm.mid = batch.getMaterialId();
      sibm.bmfdt = batch.getBatchManufacturedDate() != null ? LocalDateUtil
          .formatCustom(batch.getBatchManufacturedDate(), "dd/MM/yyyy", null) : "";
      sibm.bmfnm = batch.getBatchManufacturer();
      sibm.id = batch.getBatchId();
    }
    sibm.q = quantity;
    sibm.fq = fulfilQty;
    sibm.smst = smst;
    sibm.fmst = fmst;
    sibm.frsn = frsn;
    try {
      if (batch != null) {
        Map<String, String> hu = handlingUnitService.getHandlingUnitDataByMaterialId(batch.getMaterialId());
        if (hu != null) {
        sibm.huQty = new BigDecimal(hu.get(IHandlingUnit.QUANTITY));
        sibm.huName = hu.get(IHandlingUnit.NAME);
        }
      }
    } catch (Exception e) {
      xLogger.warn("Error while fetching Handling Unit {0}", materialId, e);
    }
    return sibm;
  }
}
