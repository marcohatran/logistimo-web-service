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

package com.logistimo.orders.utils;

import com.logistimo.activity.entity.IActivity;
import com.logistimo.activity.service.ActivityService;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.constants.Constants;
import com.logistimo.entities.entity.IKiosk;
import com.logistimo.entities.service.EntitiesService;
import com.logistimo.inventory.entity.IInvntryBatch;
import com.logistimo.inventory.service.InventoryManagementService;
import com.logistimo.logger.XLog;
import com.logistimo.materials.entity.IMaterial;
import com.logistimo.materials.service.MaterialCatalogService;
import com.logistimo.orders.entity.IDemandItem;
import com.logistimo.orders.entity.IOrder;
import com.logistimo.orders.models.InvoiceItem;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;
import com.logistimo.services.storage.StorageUtil;
import com.logistimo.shipments.ShipmentStatus;
import com.logistimo.shipments.entity.IShipment;
import com.logistimo.shipments.entity.IShipmentItem;
import com.logistimo.shipments.entity.IShipmentItemBatch;
import com.logistimo.shipments.service.IShipmentService;
import com.logistimo.utils.BigUtil;
import com.logistimo.utils.LocalDateUtil;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TimeZone;

import javax.imageio.ImageIO;

import static com.logistimo.orders.entity.IOrder.COMPLETED;
import static com.logistimo.orders.entity.IOrder.FULFILLED;

/**
 * Created by nitisha.khandelwal on 16/08/17.
 */

@Component
public class InvoiceUtils {

  private static final XLog xLogger = XLog.getLog(InvoiceUtils.class);

  public static final String UPLOADS = "uploads";
  public static final String DASH = "-";
  public static final String PDF_EXTENSION = ".pdf";


  private static final String VENDOR_NAME = "VendorName";
  private static final String VENDOR_ADDRESS = "VendorAddress";
  private static final String VENDOR_PHONE = "VendorPhone";
  private static final String CUSTOMER_NAME = "CustomerName";
  private static final String CUSTOMER_ADDRESS = "CustomerAddress";
  private static final String CUSTOMER_PHONE = "CustomerPhone";
  private static final String INVOICE_DATE = "InvoiceDate";
  private static final String ORDER_NUMBER = "OrderNumber";
  private static final String SALES_REFERENCE_NUMBER = "SalesReferenceNumber";
  private static final String SHIPMENT_NUMBER = "ShipmentNumber";
  private static final String DATE_OF_SUPPLY = "DateOfSupply";
  private static final String DATE_OF_RECEIPT = "DateOfReceipt";
  private static final String YYYY_MM_DD_HHMM_SS = "yyyy-MM-dd-HHmmSS";
  private static final String OTHER_REFERENCE_NUMBER = "OtherReferenceNumber";
  private static final String REFERENCE_NUMBER_TYPE = "ReferenceNumberType";

  private final InventoryManagementService inventoryService;
  private final MaterialCatalogService materialService;
  private final EntitiesService entitiesService;
  private final StorageUtil storageUtil;
  private IShipmentService shipmentService;
  private ActivityService activityService;

  @Autowired
  public InvoiceUtils(InventoryManagementService inventoryService,
      MaterialCatalogService materialService, EntitiesService entitiesService,
      StorageUtil storageUtil) {
    this.inventoryService = inventoryService;
    this.materialService = materialService;
    this.entitiesService = entitiesService;
    this.storageUtil = storageUtil;
  }

  @Autowired
  public void setShipmentService(IShipmentService shipmentService) {
    this.shipmentService = shipmentService;
  }

  @Autowired
  public void setActivityService(ActivityService activityService) {
    this.activityService = activityService;
  }

  public boolean hasAccessToOrder(SecureUserDetails user, IOrder order) {
    List<Long> domainIds = order.getDomainIds();
    return (domainIds != null && domainIds.contains(user.getCurrentDomainId()));
  }

  public List<InvoiceItem> getInvoiceItems(IOrder order, IShipment shipment)
      throws ServiceException {

    Map<Long, Map<String, BatchInfo>> quantityByBatches = shipment == null
        ? getQuantityByBatches(order.getOrderId()) : getQuantityByBatches(shipment);

    List<InvoiceItem> invoiceItems = new ArrayList<>();

    int sno = 1;

    String shipmentQuantity = null;

    for (IDemandItem demandItem : order.getItems()) {

      String materialStatus = null;

      if (shipment != null) {
        IShipmentItem shipmentItem = getShipmentItemByMaterialId(shipment,
            demandItem.getMaterialId());
        if (shipmentItem == null) {
          continue;
        } else {
          materialStatus = shipmentItem.getShippedMaterialStatus();
          shipmentQuantity = shipmentItem.getQuantity().toBigInteger().toString();
        }
      } else {
        List<IShipment> shipments = shipmentService.getShipmentsByOrderId(order.getOrderId());
        if (shipments.size() == 1) {
          shipmentService.includeShipmentItems(shipments.get(0));
          IShipmentItem shipmentItem = getShipmentItemByMaterialId(shipments.get(0),
              demandItem.getMaterialId());
          if (shipmentItem != null) {
            materialStatus = shipmentItem.getShippedMaterialStatus();
          }
        }
      }

      IMaterial material = materialService.getMaterial(demandItem.getMaterialId());
      if (quantityByBatches.containsKey(demandItem.getMaterialId())) {
        buildInvoiceItemByBatch(order, quantityByBatches, invoiceItems, demandItem, material,
            shipmentQuantity);
      } else {
        InvoiceItem invoiceItem = new InvoiceItem();
        invoiceItem.setItem(material.getName());
        if (shipmentQuantity != null) {
          invoiceItem.setQuantity(shipmentQuantity);
        } else {
          invoiceItem.setQuantity(demandItem.getQuantity().toBigInteger().toString());
        }
        if (BigUtil.greaterThanZero(demandItem.getRecommendedOrderQuantity())) {
          invoiceItem
              .setRecommended(demandItem.getRecommendedOrderQuantity().toBigInteger().toString());
        }
        invoiceItem.setRemarks(getRemarks(order.getOrderType(), demandItem));
        invoiceItems.add(invoiceItem);
        invoiceItem.setBatchEnabled(Boolean.FALSE);
        invoiceItem.setMaterialStatus(materialStatus);
      }
      sno++;
    }

    invoiceItems.sort(Comparator.comparing(InvoiceItem::getItem));
    sno = 0;
    InvoiceItem previousItem = null;
    for (InvoiceItem invoiceItem : invoiceItems) {
      if (previousItem == null || !previousItem.getItem().equals(invoiceItem.getItem())) {
        sno++;
      }
      invoiceItem.setSno(String.valueOf(sno));
      previousItem = invoiceItem;
    }

    return invoiceItems;
  }

  private IShipmentItem getShipmentItemByMaterialId(IShipment shipment, Long materialId) {
    return shipment.getShipmentItems().stream().filter(item -> item.getMaterialId()
        .equals(materialId)).findAny().orElse(null);
  }

  private void buildInvoiceItemByBatch(IOrder order,
      Map<Long, Map<String, BatchInfo>> quantityByBatches, List<InvoiceItem> invoiceItems,
      IDemandItem demandItem, IMaterial material, String shipmentQuantity) {

    Map<String, BatchInfo> shipmentItemBatchQuantityMap = quantityByBatches
        .get(demandItem.getMaterialId());
    for (Map.Entry<String, BatchInfo> batchEntry : shipmentItemBatchQuantityMap.entrySet()) {
      if (BigUtil.greaterThanZero(batchEntry.getValue().getQuantity())) {
        String batchId = batchEntry.getKey();
        InvoiceItem invoiceItem = new InvoiceItem();
        invoiceItem.setItem(material.getName());
        if (shipmentQuantity != null) {
          invoiceItem.setQuantity(shipmentQuantity);
        } else {
          invoiceItem.setQuantity(demandItem.getQuantity().toBigInteger().toString());
        }
        invoiceItem.setBatchId(batchId);
        IInvntryBatch batch = inventoryService.getInventoryBatch(
            order.getServicingKiosk(), demandItem.getMaterialId(), batchId, null);
        if (batch == null) {
          batch = inventoryService.getInventoryBatch(order.getKioskId(),
              demandItem.getMaterialId(), batchId, null);
        }
        if (batch == null) {
          xLogger.warn("Error while getting inventory batch for kiosk {0}, material {1}, "
                  + "batch id {2}, order id: {3}", order.getServicingKiosk(),
              demandItem.getMaterialId(), batchId, order.getOrderId());
          continue;
        }

        invoiceItem.setExpiry(LocalDateUtil
            .formatCustom(batch.getBatchExpiry(), Constants.DATE_FORMAT, null));
        invoiceItem.setManufacturer(batch.getBatchManufacturer());
        invoiceItem.setBatchQuantity(
            batchEntry.getValue().getQuantity().toBigInteger().toString());
        if (BigUtil.greaterThanZero(demandItem.getRecommendedOrderQuantity())) {
          invoiceItem.setRecommended(
              demandItem.getRecommendedOrderQuantity().toBigInteger().toString());
        }
        invoiceItem.setMaterialStatus(batchEntry.getValue().getMaterialStatus());
        invoiceItem.setRemarks(getRemarks(order.getOrderType(), demandItem));
        invoiceItems.add(invoiceItem);
        invoiceItem.setBatchEnabled(Boolean.TRUE);
      }
    }
  }

  private String getRemarks(Integer orderType, IDemandItem demandItem) {
    if (StringUtils.isNotEmpty(demandItem.getShippedDiscrepancyReason())) {
      return demandItem.getShippedDiscrepancyReason();
    } else if (orderType == IOrder.SALES_ORDER) {
      return demandItem.getReason();
    } else {
      return "";
    }
  }

  private Map<Long, Map<String, BatchInfo>> getQuantityByBatches(Long orderId) {

    List<IShipment> shipments = shipmentService.getShipmentsByOrderId(orderId);
    Map<Long, Map<String, BatchInfo>> quantityByBatches = new HashMap<>();

    shipments.forEach(shipmentService::includeShipmentItems);

    shipments.stream()
        .filter(shipment -> ShipmentStatus.SHIPPED.equals(shipment.getStatus()) ||
            ShipmentStatus.FULFILLED.equals(shipment.getStatus()))
        .filter(shipment -> shipment.getShipmentItems() != null
            && !shipment.getShipmentItems().isEmpty())
        .forEach(shipment -> shipment.getShipmentItems().stream()
            .filter(shipmentItem -> shipmentItem.getShipmentItemBatch() != null &&
                !shipmentItem.getShipmentItemBatch().isEmpty())
            .forEach(shipmentItem -> getQuantityByBatches(quantityByBatches, shipmentItem)));
    return quantityByBatches;
  }

  private Map<Long, Map<String, BatchInfo>> getQuantityByBatches(IShipment shipment) {

    Map<Long, Map<String, BatchInfo>> quantityByBatches = new LinkedHashMap<>();

    shipment.getShipmentItems().stream()
        .filter(shipmentItem -> shipmentItem.getShipmentItemBatch() != null &&
            !shipmentItem.getShipmentItemBatch().isEmpty())
        .forEach(shipmentItem -> getQuantityByBatches(quantityByBatches, shipmentItem));
    return quantityByBatches;
  }

  private void getQuantityByBatches(Map<Long, Map<String, BatchInfo>> quantityByBatches,
      IShipmentItem shipmentItem) {
    for (IShipmentItemBatch shipmentItemBatch : shipmentItem.getShipmentItemBatch()) {
      if (!quantityByBatches.containsKey(shipmentItem.getMaterialId())) {
        quantityByBatches.put(shipmentItem.getMaterialId(), new HashMap<>());
      }
      Map<String, BatchInfo> batches = quantityByBatches.get(shipmentItem.getMaterialId());
      if(BigUtil.greaterThanZero(shipmentItemBatch.getQuantity())) {
        if (batches.containsKey(shipmentItemBatch.getBatchId())) {
          BatchInfo batchInfo = batches.get(shipmentItemBatch.getBatchId());
          batchInfo.addQuantity(shipmentItemBatch.getQuantity());
          batches.put(shipmentItemBatch.getBatchId(), batchInfo);
        } else {
          batches.put(shipmentItemBatch.getBatchId(), new BatchInfo(shipmentItemBatch.getQuantity(),
              shipmentItemBatch.getShippedMaterialStatus()));
        }
      }
    }
  }

  private class BatchInfo {

    private BigDecimal quantity;

    private String materialStatus;

    BatchInfo(BigDecimal quantity, String materialStatus) {
      this.quantity = quantity;
      this.materialStatus = materialStatus;
    }

    public BigDecimal getQuantity() {
      return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
      this.quantity = quantity;
    }

    public String getMaterialStatus() {
      return materialStatus;
    }

    public void setMaterialStatus(String materialStatus) {
      this.materialStatus = materialStatus;
    }

    public void addQuantity(BigDecimal x) {
      quantity = quantity.add(x);
    }
  }

  public void addLogo(SecureUserDetails user, Map<String, Object> parameters)
      throws IOException, ClassNotFoundException {
    String invoiceLogo = getInvoiceLogo(user.getCurrentDomainId());
    BufferedImage image = ImageIO.read(storageUtil.getInputStream("uploads",
        invoiceLogo != null ? invoiceLogo : "logo.png"));
    parameters.put("logo", image);
  }

  private String getInvoiceLogo(Long domainId) {
    return DomainConfig.getInstance(domainId).getOrdersConfig().getInvoiceLogo();
  }

  public String getDateSuffix(SecureUserDetails user) {
    DateFormat format = new SimpleDateFormat(YYYY_MM_DD_HHMM_SS);
    format.setTimeZone(TimeZone.getTimeZone(user.getTimezone()));
    return format.format(new Date());
  }

  public Map<String, Object> getParameters(SecureUserDetails user, IOrder order,
      IShipment shipment)
      throws IOException, ServiceException, ClassNotFoundException {

    IKiosk customer = entitiesService.getKiosk(order.getKioskId());

    Map<String, Object> parameters = new HashMap<>();

    addLogo(user, parameters);

    if (order.getServicingKiosk() != null) {
      IKiosk vendor = entitiesService.getKiosk(order.getServicingKiosk());

      parameters.put(VENDOR_NAME, vendor.getName());
      parameters.put(VENDOR_ADDRESS, vendor.getFormattedAddress());
      parameters.put(VENDOR_PHONE, vendor.getUser().getMobilePhoneNumber());
    }

    parameters.put(CUSTOMER_NAME, customer.getName());
    parameters.put(CUSTOMER_ADDRESS, customer.getFormattedAddress());
    parameters.put(CUSTOMER_PHONE, customer.getUser().getMobilePhoneNumber());

    parameters.put(INVOICE_DATE, LocalDateUtil.format(new Date(), user.getLocale(),
        user.getTimezone(), true));

    parameters.put(ORDER_NUMBER, order.getOrderId().toString());

    if (shipment != null) {
      parameters.put(SHIPMENT_NUMBER, shipment.getShipmentId());
      updateShipmentDateOfSupplyAndReceipt(user, shipment, activityService, parameters);
      parameters.put(SALES_REFERENCE_NUMBER, shipment.getSalesReferenceId());
    } else {
      updateOrderDateOfSupplyAndReceipt(user, order, activityService, parameters);
      updateOrderReferenceNumber(order, parameters);
    }

    return parameters;
  }

  private void updateOrderReferenceNumber(IOrder order, Map<String, Object> parameters) {
    parameters.put(SALES_REFERENCE_NUMBER, order.getSalesReferenceID());
    ResourceBundle messages = Resources.getBundle(Locale.ENGLISH);
    String referenceId = Constants.EMPTY;
    String referenceType;
    if (IOrder.TRANSFER_ORDER == order.getOrderType()) {
      if (StringUtils.isNotEmpty(order.getTransferReferenceId())) {
        referenceId = order.getTransferReferenceId();
      }
      DomainConfig domainConfig = DomainConfig.getInstance(SecurityUtils.getCurrentDomainId());
      if (domainConfig.getOrdersConfig().isTransferRelease()) {
        referenceType = messages.getString("release.reference.id");
      } else {
        referenceType = messages.getString("transfer.reference.id");
      }
    } else {
      if (StringUtils.isNotEmpty(order.getPurchaseReferenceId())) {
        referenceId = order.getPurchaseReferenceId();
      }
      referenceType = messages.getString("purchase.reference.id");
    }
    parameters.put(OTHER_REFERENCE_NUMBER, referenceId);
    parameters.put(REFERENCE_NUMBER_TYPE, referenceType);
  }

  private void updateOrderDateOfSupplyAndReceipt(SecureUserDetails user, IOrder order,
      ActivityService activityService, Map<String, Object> parameters) throws ServiceException {

    if (COMPLETED.equalsIgnoreCase(order.getStatus()) ||
        FULFILLED.equalsIgnoreCase(order.getStatus())) {
      IActivity activity = activityService.getLatestActivityWithStatus(
          IActivity.TYPE.ORDER.name(), order.getOrderId().toString(), COMPLETED);
      parameters.put(DATE_OF_SUPPLY, activity.getCreateDate() != null ? LocalDateUtil.format
          (activity.getCreateDate(), user.getLocale(), user.getTimezone(), true) : Constants.EMPTY);
    } else {
      parameters.put(DATE_OF_SUPPLY, Constants.EMPTY);
    }

    if (FULFILLED.equalsIgnoreCase(order.getStatus())) {
      IActivity activity = activityService.getLatestActivityWithStatus(IActivity.TYPE.ORDER.name(),
          order.getOrderId().toString(), FULFILLED);
      parameters.put(DATE_OF_RECEIPT, activity.getCreateDate() != null ? LocalDateUtil.format
          (activity.getCreateDate(), user.getLocale(), user.getTimezone(), true) : Constants.EMPTY);
    } else {
      parameters.put(DATE_OF_RECEIPT, Constants.EMPTY);
    }
  }

  private void updateShipmentDateOfSupplyAndReceipt(SecureUserDetails user, IShipment shipment,
      ActivityService activityService, Map<String, Object> parameters) throws ServiceException {

    if (shipment.getStatus().equals(ShipmentStatus.SHIPPED) ||
        shipment.getStatus().equals(ShipmentStatus.FULFILLED)) {
      IActivity activity = activityService.getLatestActivityWithStatus(
          IActivity.TYPE.SHIPMENT.name(), shipment.getShipmentId(),
          ShipmentStatus.SHIPPED.toString());
      parameters.put(DATE_OF_SUPPLY, activity.getCreateDate() != null ? LocalDateUtil.format
          (activity.getCreateDate(), user.getLocale(), user.getTimezone(), true) : Constants.EMPTY);
    } else {
      parameters.put(DATE_OF_SUPPLY, Constants.EMPTY);
    }

    if (shipment.getStatus().equals(ShipmentStatus.FULFILLED)) {
      IActivity activity = activityService.getLatestActivityWithStatus(
          IActivity.TYPE.SHIPMENT.name(), shipment.getShipmentId(),
          ShipmentStatus.FULFILLED.toString());
      parameters.put(DATE_OF_RECEIPT, activity.getCreateDate() != null ? LocalDateUtil.format
          (activity.getCreateDate(), user.getLocale(), user.getTimezone(), true) : Constants.EMPTY);
    } else {
      parameters.put(DATE_OF_RECEIPT, Constants.EMPTY);
    }
  }

}
