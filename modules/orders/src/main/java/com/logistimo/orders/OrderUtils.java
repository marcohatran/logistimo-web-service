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

package com.logistimo.orders;

import com.logistimo.config.models.DomainConfig;
import com.logistimo.constants.Constants;
import com.logistimo.constants.SourceConstants;
import com.logistimo.context.StaticApplicationContext;
import com.logistimo.exception.InvalidServiceException;
import com.logistimo.exception.LogiException;
import com.logistimo.exception.ValidationException;
import com.logistimo.inventory.entity.IInvntry;
import com.logistimo.logger.XLog;
import com.logistimo.models.shipments.ShipmentItemBatchModel;
import com.logistimo.models.shipments.ShipmentItemModel;
import com.logistimo.models.shipments.ShipmentMaterialsModel;
import com.logistimo.orders.entity.IOrder;
import com.logistimo.orders.models.UpdatedOrder;
import com.logistimo.orders.service.OrderManagementService;
import com.logistimo.orders.service.impl.OrderManagementServiceImpl;
import com.logistimo.proto.FulfillmentBatchMaterialRequest;
import com.logistimo.proto.FulfillmentMaterialRequest;
import com.logistimo.proto.UpdateOrderStatusRequest;
import com.logistimo.services.ObjectNotFoundException;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;
import com.logistimo.shipments.ShipmentStatus;
import com.logistimo.shipments.entity.IShipment;
import com.logistimo.shipments.service.IShipmentService;
import com.logistimo.shipments.service.impl.ShipmentService;
import com.logistimo.utils.BigUtil;
import com.logistimo.utils.LocalDateUtil;

import org.apache.commons.lang.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import static com.logistimo.orders.entity.IOrder.NONTRANSFER;


/**
 * Created by charan on 05/03/17.
 */
public class OrderUtils {

  private static final XLog xLogger = XLog.getLog(OrderUtils.class);

  // Update an order
  public static UpdatedOrder updateOrder(IOrder o) throws LogiException {
    if (o == null) {
      throw new ServiceException("Order not specified");
    }
    // Get the OMS
    OrderManagementService oms =
        StaticApplicationContext.getBean(OrderManagementServiceImpl.class);
    return oms.updateOrder(o, SourceConstants.WEB);
  }

  // Update an order's status - used only in viewOrder.jsp
  public static UpdatedOrder updateOrderStatus(Long orderId, String newStatus,
                                               String updatingUserId, String message,
                                               List<String> recevingUserIds,
                                               int source)
      throws ObjectNotFoundException, ServiceException {
    OrderManagementService oms =
        StaticApplicationContext.getBean(OrderManagementServiceImpl.class);
    IOrder o = oms.getOrder(orderId);
    o.setStatus(
        newStatus); // required, given isPostingInventoryTransRequired() requires an order with the current status
    return oms
        .updateOrderStatus(orderId, newStatus, updatingUserId, message, recevingUserIds, source);
  }

  // Update an order's status - to support old apk
  public static UpdatedOrder updateOrderStatus(Long orderId, String newStatus,
                                               String updatingUserId, String message,
                                               int source,
                                               ResourceBundle backendMessages)
      throws ObjectNotFoundException, ServiceException, ValidationException {
    OrderManagementService oms =
        StaticApplicationContext.getBean(OrderManagementServiceImpl.class);
    IOrder o = oms.getOrder(orderId, true);
    UpdatedOrder uo = new UpdatedOrder();
    if (IOrder.FULFILLED.equals(newStatus)) {
      IShipmentService ss;
      boolean updated = false;
      try {
        ss = StaticApplicationContext.getBean(ShipmentService.class);
        List<IShipment> shipments = ss.getShipmentsByOrderId(orderId);
        if (shipments != null && !shipments.isEmpty()) {
          IShipment s = shipments.get(0);
          updated = ss.fulfillShipment(s.getShipmentId(), updatingUserId, source).status;
        }
      } catch (Exception e) {
        uo.inventoryError = true;
        uo.message = backendMessages.getString("error.unabletofulfilorder");
      }
      if (updated) {
        uo.order = oms.getOrder(orderId, true);
      } else {
        uo.inventoryError = true;
        uo.message = backendMessages.getString("error.unabletofulfilorder");
      }
    } else if (IOrder.COMPLETED.equals(newStatus)) {
      oms.shipNow(o, null, null, null, null, updatingUserId, null, source, null, true);
      if (message != null && !message.isEmpty()) {
        oms.addMessageToOrder(orderId, message, updatingUserId);
      }
      uo.order = oms.getOrder(orderId, true);
    } else {
      o.setStatus(
          newStatus); // required, given isPostingInventoryTransRequired() requires an order with the current status
      uo =
          oms.updateOrderStatus(orderId, newStatus, updatingUserId, message, null, source, null,
              null);
    }
    return uo;
  }

  public static UpdatedOrder updateOrderStatus(Long orderId, String newStatus,
                                               String updatingUserId,
                                               String message, List<String> recevingUserIds,
                                               int source, String reason)
      throws ObjectNotFoundException, ServiceException {
    OrderManagementService oms =
        StaticApplicationContext.getBean(OrderManagementServiceImpl.class);
    IOrder o = oms.getOrder(orderId);
    o.setStatus(
        newStatus); // required, given isPostingInventoryTransRequired() requires an order with the current status
    return oms
        .updateOrderStatus(orderId, newStatus, updatingUserId, message, recevingUserIds, source,
            null, reason);
  }

  public static UpdatedOrder updateOrdStatus(UpdateOrderStatusRequest uosReq, DomainConfig dc,
                                             int source, ResourceBundle backendMessages)
      throws ObjectNotFoundException, LogiException {
    OrderManagementService oms =
        StaticApplicationContext.getBean(OrderManagementServiceImpl.class);
    IOrder o = oms.getOrder(uosReq.tid, true);
    if (!OrderUtils.validateOrderUpdatedTime(uosReq.tm, o.getUpdatedOn())) {
      throw new LogiException("O004", uosReq.uid,uosReq.tm);
    }
    UpdatedOrder uo = new UpdatedOrder();
    if (IOrder.FULFILLED.equals(uosReq.ost)) {
      if (fulfillOrder(uosReq, source, backendMessages, oms, uo)) {
        return uo;
      }
    } else if (IOrder.COMPLETED.equals(uosReq.ost)) {
      if (shipOrder(uosReq, dc, source, oms, o, uo)) {
        return uo;
      }
    } else {
      o.setStatus(
          uosReq.ost); // required, given isPostingInventoryTransRequired() requires an order with the current status
      uo =
          oms.updateOrderStatus(uosReq.tid, uosReq.ost, uosReq.uid, uosReq.ms, null, source, null,
              uosReq.rsnco);
    }
    return uo;
  }

  private static boolean shipOrder(UpdateOrderStatusRequest uosReq, DomainConfig dc, int source,
                                   OrderManagementService oms, IOrder o, UpdatedOrder uo)
      throws ServiceException {
    if (dc.getOrdersConfig()
        .isReferenceIdMandatory() && !o.hasSalesReferenceId() && !uosReq.hasReferenceId()) {
      uo.inventoryError = true;
      uo.message = "Reference id is required before shipping";
      return true;
    }
    if (dc.getOrdersConfig().isExpectedArrivalDateMandatory() && !uosReq
        .hasEAD()) {
      uo.inventoryError = true;
      uo.message = "Estimated arrival date is required before shipping.";
      return true;
    }

    if (uosReq.hasReferenceId()) {
      oms.updateOrderReferenceId(uosReq.tid, uosReq.rid, uosReq.uid, null);
    }
    oms.shipNow(o, uosReq.trsp, uosReq.trid, null, uosReq.ead, uosReq.uid, uosReq.pksz, source, uosReq.rid, true);
    if (uosReq.ms != null && !uosReq.ms.isEmpty()) {
      oms.addMessageToOrder(uosReq.tid, uosReq.ms, uosReq.uid);
    }
    uo.order = oms.getOrder(uosReq.tid, true);
    return false;
  }

  private static boolean fulfillOrder(UpdateOrderStatusRequest uosReq, int source,
                                      ResourceBundle backendMessages, OrderManagementService oms,
                                      UpdatedOrder uo) throws ServiceException {
    IShipmentService ss;
    boolean updated = false;
    try {
      ShipmentMaterialsModel smm = getShipmentMaterialsModel(uosReq);
      if (smm == null) {
        uo.inventoryError = true;
        uo.message = backendMessages.getString("error.unabletofulfilorder");
        return true;
      }
      ss = StaticApplicationContext.getBean(ShipmentService.class);
      updated = ss.fulfillShipment(smm, uosReq.uid, source).status;
    } catch (Exception e) {
      uo.inventoryError = true;
      uo.message = backendMessages.getString("error.unabletofulfilorder");
    }
    if (updated) {
      uo.order = oms.getOrder(uosReq.tid, true);
    } else {
      uo.inventoryError = true;
      uo.message = backendMessages.getString("error.unabletofulfilorder");
    }
    return false;
  }

  private static ShipmentMaterialsModel getShipmentMaterialsModel(UpdateOrderStatusRequest uosReq) {
    if (uosReq.mt == null || uosReq.mt.isEmpty()) {
      return null;
    }
    List<FulfillmentMaterialRequest> mt = uosReq.mt;
    ShipmentMaterialsModel smm = new ShipmentMaterialsModel();
    List<ShipmentItemModel> simList = new ArrayList<>(1);
    for (FulfillmentMaterialRequest material : mt) {
      ShipmentItemModel sim = new ShipmentItemModel();
      sim.mId = material.mid;
      sim.frsn = material.rsnpf;
      sim.fmst = material.fmst;
      List<FulfillmentBatchMaterialRequest> bt = material.bt;
      if (bt != null && !bt.isEmpty()) {
        List<ShipmentItemBatchModel> sibmList = new ArrayList<>(1);
        for (FulfillmentBatchMaterialRequest bm : bt) {
          ShipmentItemBatchModel sibm = new ShipmentItemBatchModel();
          sibm.id = bm.bid;
          sibm.fq = bm.q;
          if (BigUtil.lesserThanZero(sibm.fq)) {
            xLogger.severe(
                "Exception while updating order status to fulfilled. Batch ID: {0}, Fulfilled quantity: {1}",
                sibm.id, sibm.fq);
            return null;
          }
          if (bm.fmst != null && !bm.fmst.isEmpty()) {
            sibm.fmst = bm.fmst;
          }
          if (bm.rsnpf != null && !bm.rsnpf.isEmpty()) {
            sibm.frsn = bm.rsnpf;
          }
          sibmList.add(sibm);
        }
        sim.bq = sibmList;
        sim.isBa = true;
      }
      if (material.q != null) {
        sim.fq = material.q;
        if (BigUtil.lesserThanZero(sim.fq)) {
          xLogger.severe(
              "Exception while updating order status to fulfilled. Shipment ID: {0}, Fulfilled quantity {1}",
              sim.sid, sim.fq);
          return null;
        }
      }
      simList.add(sim);

    }
    smm.items = simList;
    smm.sId = uosReq.sid;
    SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT);
    smm.afd = sdf.format(uosReq.dar);
    smm.userId = uosReq.uid;
    smm.msg = uosReq.ms;
    if (uosReq.tid != null) {
      smm.isOrderFulfil = true;
    }
    return smm;
  }

  public static UpdatedOrder updateShpStatus(UpdateOrderStatusRequest uosReq, DomainConfig dc,
                                             int source, ResourceBundle backendMessages,
                                             String previousUpdatedTime)
      throws LogiException {
    IShipmentService ss = StaticApplicationContext.getBean(ShipmentService.class);
    OrderManagementService oms = StaticApplicationContext.getBean(OrderManagementServiceImpl.class);
    IShipment s = ss.getShipment(uosReq.sid);
    ShipmentStatus shipmentStatus;
    if (ShipmentStatus.SHIPPED.toString().equals(uosReq.ost)) {
      shipmentStatus = ShipmentStatus.SHIPPED;
    } else if (ShipmentStatus.CANCELLED.toString().equals(uosReq.ost)) {
      shipmentStatus = ShipmentStatus.CANCELLED;
    } else if (ShipmentStatus.CONFIRMED.toString().equals(uosReq.ost)) {
      shipmentStatus = ShipmentStatus.CONFIRMED;
    } else if (ShipmentStatus.FULFILLED.toString().equals(uosReq.ost)) {
      shipmentStatus = ShipmentStatus.FULFILLED;
    } else {
      throw new InvalidServiceException("Invalid status to update");
    }
    boolean updated = false;
    UpdatedOrder uo = new UpdatedOrder();
    if (shipmentStatus.toString().equals(ShipmentStatus.FULFILLED.toString())) {
      try {
        ShipmentMaterialsModel smm = getShipmentMaterialsModel(uosReq);
        if (smm == null) {
          uo.inventoryError = true;
          uo.message = backendMessages.getString("error.unabletofulfilorder");
          return uo;
        }
        ss = StaticApplicationContext.getBean(ShipmentService.class);
        updated = ss.fulfillShipment(smm, uosReq.uid, source).status;
      } catch (Exception e) {
        uo.inventoryError = true;
        uo.message = backendMessages.getString("error.unabletofulfilorder");
      }
    } else {
      if (ShipmentStatus.SHIPPED.equals(shipmentStatus)) {
        if (dc.getOrdersConfig()
            .isReferenceIdMandatory()) {
          IOrder order = oms.getOrder(s.getOrderId());
          if (!order.hasSalesReferenceId() && !uosReq.hasReferenceId()) {
            uo.inventoryError = true;
            uo.message = "Reference # is mandatory before shipping.";
            return uo;
          }
        }
        if (dc.getOrdersConfig().isExpectedArrivalDateMandatory() && !s.hasEAD() && !uosReq
            .hasEAD()) {
          uo.inventoryError = true;
          uo.message = "Estimated arrival data is mandatory before shipping.";
          return uo;
        }
      }
      Map<String, String> shipmentMetadata = new HashMap<>(2);
      if (uosReq.hasReferenceId()) {
        shipmentMetadata.put("rid", uosReq.rid);
      }
      if (uosReq.hasPackageSize()) {
        shipmentMetadata.put("ps", uosReq.pksz);
      }
      if (uosReq.hasTransporter()) {
        shipmentMetadata.put("tpName", uosReq.trsp);
      }
      if (uosReq.hasTrackingId()) {
        shipmentMetadata.put("tId", uosReq.trid);
      }
      if (uosReq.hasEAD()) {
        SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT);
        String eadStr = sdf.format(uosReq.ead);
        shipmentMetadata.put("date", eadStr);
      }
      if (!shipmentMetadata.isEmpty()) {
        ss.updateShipmentData(shipmentMetadata, previousUpdatedTime, uosReq.sid, uosReq.uid);
      }
      updated =
          ss.updateShipmentStatus(uosReq.sid, shipmentStatus, uosReq.ms, uosReq.uid,
              uosReq.rsnco, source).status;
    }

    if (updated) {
      uo.order = oms.getOrder(s.getOrderId(), true);
    } else {
      uo.inventoryError = true;
      uo.message = "Error while updating shipment status";
    }
    return uo;
  }


  // Check if setting re-order level is allowed
  public static boolean isReorderAllowed(String invModel) {
    return (invModel == null || invModel.isEmpty() ||
        IInvntry.MODEL_KANBAN.equals(invModel));
  }

  public static String getStatusDisplay(String status, Locale locale) {
    ResourceBundle messages = Resources.get().getBundle("Messages", locale);
    if (messages == null) {
      return "unknown";
    }
    String name;
    if (IOrder.CANCELLED.equals(status)) {
      name = messages.getString("order.cancelled");
    } else if (IOrder.CHANGED.equals(status)) {
      name = messages.getString("order.changed");
    } else if (IOrder.COMPLETED.equals(status)) {
      name = messages.getString("order.shipped");
    } else if (IOrder.CONFIRMED.equals(status)) {
      name = messages.getString("order.confirmed");
    } else if (IOrder.FULFILLED.equals(status)) {
      name = messages.getString("order.fulfilled");
    } else if (IOrder.PENDING.equals(status)) {
      name = messages.getString("order.pending");
    } else if (IOrder.BACKORDERED.equals(status)) {
      name = messages.getString("order.backordered");
    } else {
      name = "unknown";
    }

    return name;
  }

  public static String getShipmentStatusDisplay(ShipmentStatus status, Locale locale) {
    ResourceBundle messages = Resources.get().getBundle("Messages", locale);
    if (messages == null) {
      return "unknown";
    }
    String name;
    if (ShipmentStatus.CANCELLED.equals(status)) {
      name = messages.getString("order.cancelled");
    } else if (ShipmentStatus.FULFILLED.equals(status)) {
      name = messages.getString("order.fulfilled");
    } else if (ShipmentStatus.PENDING.equals(status) || ShipmentStatus.OPEN.equals(status)) {
      name = messages.getString("order.pending");
    } else if (ShipmentStatus.SHIPPED.equals(status)) {
      name = messages.getString("order.shipped");
    } else {
      name = "unknown";
    }

    return name;
  }


  /**
   * Validate order status
   *
   * @param status Order status from request
   * @return true if status is valid, false otherwise
   */
  public static boolean isValidOrderStatus(String status) {
    return status.equalsIgnoreCase(IOrder.PENDING) || status.equalsIgnoreCase(IOrder.COMPLETED) ||
        status.equalsIgnoreCase(IOrder.CHANGED) || status.equalsIgnoreCase(IOrder.BACKORDERED)
        || status.equalsIgnoreCase(IOrder.CANCELLED) || status.equalsIgnoreCase(IOrder.FULFILLED);
  }

  /**
   * Validate order type. Valid types are sle for sales and prc for purchase
   *
   * @param orderType - Sle/prc
   * @return boolean to indicate if valid or no
   */
  public static boolean isValidOrderType(String orderType) {
    return StringUtils.isNotBlank(orderType) && (
        orderType.equalsIgnoreCase(IOrder.TYPE_PURCHASE) || orderType
            .equalsIgnoreCase(IOrder.TYPE_SALE));
  }

  /**
   * Set the order type as sales,purchase and transfers for approvals
   *
   * @param orderType - Sle/prc
   * @return 0-TRANSFER, 1- PURCHASE, 2-SALES
   */
  public static int getOrderApprovalType(String orderType, boolean isTransfer) {
    int status = -1;
    if (isTransfer) {
      return 0;
    } else if (StringUtils.isNotBlank(orderType) && orderType
        .equalsIgnoreCase(IOrder.TYPE_PURCHASE)) {
      return 1;
    } else if (StringUtils.isNotBlank(orderType) && orderType.equalsIgnoreCase(IOrder.TYPE_SALE)) {
      return 2;
    }
    return status;
  }

  /**
   * @param transfers has 1 or 0
   * @return If the transfer value is set to 1, set type as transfers. Default type is non transfers
   */
  public static boolean isTransfer(String transfers) {
    try {
      return StringUtils.isNotBlank(transfers) && Integer.parseInt(transfers) == NONTRANSFER;
    } catch (NumberFormatException e) {
      xLogger.warn("Number format exception: {0}", e.getMessage());
    }
    return false;
  }

  /**
   * Method to validate the last updated time received in the request with the actual order updated time.
   *
   * @param lastUpdatedTime  Time received from client
   * @param orderUpdatedTime Last updated time present in db
   * @return true if lastUpdatedTime is blank or equal to orderUpdatedTime
   */
  public static boolean validateOrderUpdatedTime(String lastUpdatedTime, Date orderUpdatedTime) {
    return StringUtils.isBlank(lastUpdatedTime) || lastUpdatedTime.equalsIgnoreCase(
        LocalDateUtil.formatCustom(orderUpdatedTime, Constants.DATETIME_FORMAT, null));
  }


  /**
   * Returns the string constants for the order types
   *
   * @param type 0-TRANSFER, 1- PURCHASE, 2-SALES
   * @return try-TRANSFER, prc- PURCHASE,sle- SALES
   */
  public static String getOrderType(Integer type) {
    if (type == 0) {
      return IOrder.TYPE_TRANSFER;
    } else if (type == 1) {
      return IOrder.TYPE_PURCHASE;
    } else if (type == 2) {
      return IOrder.TYPE_SALE;
    }
    return StringUtils.EMPTY;
  }

}
