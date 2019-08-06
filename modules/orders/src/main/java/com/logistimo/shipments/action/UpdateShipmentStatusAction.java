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

package com.logistimo.shipments.action;

import com.logistimo.AppFactory;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.constants.CharacterConstants;
import com.logistimo.constants.Constants;
import com.logistimo.constants.SourceConstants;
import com.logistimo.conversations.entity.IMessage;
import com.logistimo.dao.JDOUtils;
import com.logistimo.deliveryrequest.actions.GetDeliveryRequestsAction;
import com.logistimo.deliveryrequest.actions.ICancelDeliveryRequestAction;
import com.logistimo.deliveryrequest.models.DeliveryRequestModel;
import com.logistimo.deliveryrequest.models.DeliveryRequestStatus;
import com.logistimo.entities.entity.IKiosk;
import com.logistimo.entities.service.EntitiesService;
import com.logistimo.events.entity.IEvent;
import com.logistimo.exception.InvalidServiceException;
import com.logistimo.exception.LogiException;
import com.logistimo.exception.ValidationException;
import com.logistimo.inventory.entity.IInvAllocation;
import com.logistimo.inventory.entity.IInvntry;
import com.logistimo.inventory.entity.IInvntryBatch;
import com.logistimo.inventory.entity.ITransaction;
import com.logistimo.inventory.service.InventoryManagementService;
import com.logistimo.logger.XLog;
import com.logistimo.materials.entity.IMaterial;
import com.logistimo.materials.service.MaterialCatalogService;
import com.logistimo.materials.service.MaterialUtils;
import com.logistimo.models.ResponseModel;
import com.logistimo.models.shipments.ShipmentItemBatchModel;
import com.logistimo.orders.actions.GetOrderOverallStatusAction;
import com.logistimo.orders.actions.ValidateFullAllocationAction;
import com.logistimo.orders.approvals.service.IOrderApprovalsService;
import com.logistimo.orders.entity.IDemandItem;
import com.logistimo.orders.entity.IOrder;
import com.logistimo.orders.service.IDemandService;
import com.logistimo.orders.service.OrderManagementService;
import com.logistimo.pagination.Results;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;
import com.logistimo.services.impl.PMF;
import com.logistimo.services.taskqueue.ITaskService;
import com.logistimo.shipments.ShipmentRepository;
import com.logistimo.shipments.ShipmentStatus;
import com.logistimo.shipments.entity.IShipment;
import com.logistimo.shipments.entity.IShipmentItem;
import com.logistimo.shipments.entity.IShipmentItemBatch;
import com.logistimo.shipments.entity.ShipmentItem;
import com.logistimo.utils.BigUtil;
import com.logistimo.utils.LockUtil;
import com.logistimo.utils.MsgUtil;
import com.logistimo.utils.StringUtil;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.jdo.PersistenceManager;
import javax.jdo.Transaction;

import static com.logistimo.shipments.ShipmentUtils.extractOrderId;
import static com.logistimo.shipments.ShipmentUtils.generateEvent;
import static com.logistimo.shipments.ShipmentUtils.includeShipmentItems;


@Component
public class UpdateShipmentStatusAction {

  private static final XLog xLogger = XLog.getLog(UpdateShipmentStatusAction.class);

  @Autowired
  private InventoryManagementService inventoryManagementService;

  @Autowired
  private OrderManagementService orderManagementService;

  @Autowired
  private EntitiesService entitiesService;

  @Autowired
  private MaterialCatalogService materialCatalogService;

  @Autowired
  private ShipmentRepository shipmentRepository;

  @Autowired
  private IDemandService demandService;

  @Autowired
  private ShipmentActivty shipmentActivty;

  @Autowired
  private GetOrderOverallStatusAction orderOverallStatusAction;

  @Autowired
  private ValidateFullAllocationAction validateFullAllocationAction;

  @Autowired
  private GetDeliveryRequestsAction getDeliveryRequestsAction;

  @Autowired
  IOrderApprovalsService orderApprovalsService;

  @Autowired
  @Qualifier("fleetCancelDeliveryRequestAction")
  private ICancelDeliveryRequestAction cancelDeliveryRequestAction;

  public ResponseModel invoke(String shipmentId, ShipmentStatus status,
                              String message,
                              String userId,
                              PersistenceManager pm, String reason,
                              IShipment shipment,
                              boolean updateOrderStatus,
                              boolean isOrderFulfil,
                              int source, String salesRefId,
                              Date estimatedDateOfArrival,
                              boolean updateOrderFields,
                              boolean transferAllocations)
      throws LogiException {
    Long orderId = extractOrderId(shipmentId);
    LockUtil.LockStatus lockStatus = LockUtil.lock(Constants.TX_O + orderId);
    if (!LockUtil.isLocked(lockStatus)) {
      throw new InvalidServiceException(new ServiceException("O002", orderId));
    }
    boolean closePM = false;
    boolean localShipObject = shipment == null;
    ResponseModel responseModel;
    Transaction tx = null;
    if (pm == null) {
      pm = PMF.get().getPersistenceManager();
      tx = pm.currentTransaction();
      closePM = true;
    }
    try {
      if (closePM) {
        tx.begin();
      }
      if (shipment == null) {
        shipment = fetchShipment(shipmentId, pm);
      }

      ShipmentStatus prevStatus = shipment.getStatus();
      validateStatusTransition(prevStatus, status);
      validateAllocations(status, shipment);
      validateApproval(status, shipment);
      responseModel = validateStatusChange(shipment, status.toString(), pm);

      if (ShipmentStatus.CANCELLED == status) {
        cancel(shipmentId, status, message, userId, pm, reason, shipment, updateOrderStatus, source, prevStatus, transferAllocations);
      } else {
        shipment.setStatus(status);
        if (status == ShipmentStatus.SHIPPED || (prevStatus != ShipmentStatus.SHIPPED &&
            status == ShipmentStatus.FULFILLED)) {
          ship(shipmentId, status, userId, pm, shipment, source, orderId, localShipObject, prevStatus);
        }

        if (status == ShipmentStatus.FULFILLED && prevStatus != ShipmentStatus.FULFILLED) {
          //Only post fulfilled transaction here.
          postInventoryTransaction(shipment, userId, pm, ShipmentStatus.SHIPPED, source);
        }

        generateEvent(shipment.getDomainId(), IEvent.STATUS_CHANGE, shipment.getShipmentId(),
            shipment.getStatus(), null, null);

        updateMessageAndActivity(shipmentId, message, userId, pm, shipment, isOrderFulfil, prevStatus);

        if (updateOrderStatus) {
          updateOrderStatus(shipment.getOrderId(), shipment.getStatus(), userId, pm);
        }

        shipment.setUpdatedBy(userId);
        shipment.setUpdatedOn(new Date());

        orderManagementService
            .updateOrderMetadata(orderId, userId, pm, salesRefId, estimatedDateOfArrival,
                updateOrderFields);
      }
      if (closePM) {
        tx.commit();
      }

    } catch (LogiException e) {
      throw e;
    } catch (Exception e) {
      xLogger.severe("Error while getting shipment details.", e);
      throw new ServiceException(e);
    } finally {
      if (closePM && tx.isActive()) {
        tx.rollback();
      }
      if (closePM) {
        PMF.close(pm);
      }
      if (LockUtil.shouldReleaseLock(lockStatus) && !LockUtil.release(Constants.TX_O + orderId)) {
        xLogger.warn("Unable to release lock for key {0}", Constants.TX_O + orderId);
      }
    }
    responseModel.status = true;
    return responseModel;
  }

  private void validateApproval(ShipmentStatus status, IShipment shipment) throws ServiceException {
    if (status == ShipmentStatus.READY_FOR_DISPATCH) {
      IOrder order = orderManagementService.getOrder(shipment.getOrderId());
      if (!orderApprovalsService.isShippingApprovalComplete(order)) {
        throw new ValidationException("O007", new Object[0]);
      }
    }
  }

  private void validateAllocations(ShipmentStatus status, IShipment shipment)
      throws ServiceException {
    // Validate if items are fully allocated if shipment status moves to Ready for dispatch
    if (status == ShipmentStatus.READY_FOR_DISPATCH) {
      validateFullAllocationAction.invoke(IInvAllocation.Type.SHIPMENT, shipment.getShipmentId());
    }
  }

  private void updateMessageAndActivity(String shipmentId, String message, String userId, PersistenceManager pm, IShipment shipment, boolean isOrderFulfil,
                                        ShipmentStatus prevStatus) throws ServiceException {
    if (isOrderFulfil) {
      IMessage msg = null;
      if (message != null) {
        msg = orderManagementService.addMessageToOrder(shipment.getOrderId(), message, userId);
      }
      shipmentActivty.addActivity(shipmentId, userId, shipment.getOrderId(),
          shipment.getDomainId(), prevStatus, shipment.getStatus(), null, pm, msg);
    } else {
      shipmentActivty.updateMessageAndActivity(shipmentId, message, userId, shipment.getOrderId(),
          shipment.getDomainId(), prevStatus, shipment.getStatus(), null, pm);
    }
  }

  private void ship(String shipmentId, ShipmentStatus status, String userId, PersistenceManager pm, IShipment shipment, int source, Long orderId,
                    boolean localShipObject, ShipmentStatus prevStatus) throws ServiceException {
    //To check for batch enabled materials and vendor

    if (status == ShipmentStatus.SHIPPED) {

      checkShipmentRequest(shipment.getKioskId(), shipment.getServicingKiosk(),
          shipment.getShipmentItems());
    }

    DomainConfig dc = DomainConfig.getInstance(shipment.getDomainId());
    if (dc.autoGI()) {
      inventoryManagementService.clearAllocation(null, null, IInvAllocation.Type.SHIPMENT,
          shipmentId, pm);
    }

    Map<Long, IDemandItem> demandItems = demandService.getDemandMetadata(orderId, pm);
    for (IShipmentItem shipmentItem : shipment.getShipmentItems()) {
      IDemandItem demandItem = demandItems.get(shipmentItem.getMaterialId());
      List<IShipmentItemBatch> batch =
          (List<IShipmentItemBatch>) shipmentItem.getShipmentItemBatch();
      if (CollectionUtils.isNotEmpty(batch)) {
        Results<IInvntryBatch>
            rs =
            inventoryManagementService.getBatches(shipmentItem.getMaterialId(),
                shipment.getServicingKiosk(), null);
        List<IInvntryBatch> results = rs.getResults();
        for (IShipmentItemBatch ib : batch) {
          for (IInvntryBatch invntryBatch : results) {
            if (ib.getBatchId().equals(invntryBatch.getBatchId())) {
              ib.setBatchExpiry(invntryBatch.getBatchExpiry());
              ib.setBatchManufacturedDate(invntryBatch.getBatchManufacturedDate());
              ib.setBatchManufacturer(invntryBatch.getBatchManufacturer());
              if (localShipObject) {
                pm.makePersistent(ib);
              }
              break;
            }
          }
        }
      }
      demandItem
          .setShippedQuantity(demandItem.getShippedQuantity().add(shipmentItem.getQuantity()));
    }
    pm.makePersistentAll(demandItems.values());
    postInventoryTransaction(shipment, userId, pm, prevStatus, source);
  }

  private void cancel(String shipmentId, ShipmentStatus status, String message, String userId, PersistenceManager pm, String reason,
                      IShipment shipment, boolean updateOrderStatus, int source,
                      ShipmentStatus prevStatus, boolean transferAllocations) throws Exception {

    cancelShipment(shipmentId, message, userId, pm, reason, transferAllocations);
    cancelDeliveryRequests(userId, shipmentId);

    if (ShipmentStatus.SHIPPED.equals(prevStatus) ||
        ShipmentStatus.FULFILLED.equals(prevStatus)) {
      postInventoryTransaction(shipment, userId, pm, prevStatus, source);
    } else if (ShipmentStatus.PRE_SHIP_STATUSES.contains(prevStatus)) {
      DomainConfig dc = DomainConfig.getInstance(shipment.getDomainId());
      if (dc.autoGI()) {
        inventoryManagementService
            .clearAllocation(null, null, IInvAllocation.Type.SHIPMENT, shipmentId, pm);
      }
    }
    if (updateOrderStatus) {
      updateOrderStatus(shipment.getOrderId(), status, userId, pm);
    }
    generateEvent(shipment.getDomainId(), IEvent.STATUS_CHANGE, shipment.getShipmentId(),
        shipment.getStatus(), null, null);
  }

  private void cancelDeliveryRequests(String userId, String shipmentId) {
    Results<DeliveryRequestModel> drs =
        getDeliveryRequestsAction.getByShipmentId(shipmentId, false);
    if(CollectionUtils.isNotEmpty(drs.getResults())) {
      drs.getResults().stream()
          .filter(dr -> !DeliveryRequestStatus.INACTIVE_DELIVERY_REQUEST_STATUSES.contains(dr
              .getStatus()))
          .forEach(dr -> {
            try {
              cancelDeliveryRequestAction.invoke(userId, dr.getId());
            } catch (ServiceException e) {
              xLogger.warn("Error while cancelling delivery request with Id: " + dr.getId(), e);
            }
          });
    }
  }

  private IShipment fetchShipment(String shipmentId, PersistenceManager pm) {
    IShipment shipment;
    shipment = JDOUtils.getObjectById(IShipment.class, shipmentId, pm);
    if (shipment == null) {
      ResourceBundle
          backendMessages =
          Resources.getBundle("BackendMessages", SecurityUtils.getLocale());
      throw new IllegalArgumentException(
          backendMessages.getString("shipment.unavailable.db") + " " + backendMessages
              .getString("shipment.id") + " : "
              + shipmentId);
    }
    includeShipmentItems(shipment, pm);
    return shipment;
  }

  private boolean cancelShipment(String shipmentId, String message, String userId,
                                 PersistenceManager pm, String reason, boolean transferAllocations) throws ServiceException {
    Long orderId = extractOrderId(shipmentId);
    LockUtil.LockStatus lockStatus = LockUtil.lock(Constants.TX_O + orderId);
    if (!LockUtil.isLocked(lockStatus)) {
      throw new InvalidServiceException(new ServiceException("O002", orderId));
    }
    boolean closePM = false;
    Transaction tx = null;
    if (pm == null) {
      pm = PMF.get().getPersistenceManager();
      tx = pm.currentTransaction();
      closePM = true;
    }
    try {
      IShipment shipment = shipmentRepository.getById(shipmentId);
      ShipmentStatus prevStatus = shipment.getStatus();
      shipment.setStatus(ShipmentStatus.CANCELLED);
      shipment.setCancelledDiscrepancyReasons(reason);
      shipment.setUpdatedBy(userId);
      shipment.setUpdatedOn(new Date());
      includeShipmentItems(shipment, pm);
      boolean
          decrementShipped =
          prevStatus == ShipmentStatus.SHIPPED || prevStatus == ShipmentStatus.FULFILLED;
      Map<Long, IDemandItem> demandItems = demandService.getDemandMetadata(orderId, pm);
      for (IShipmentItem shipmentItem : shipment.getShipmentItems()) {
        if (ShipmentStatus.PRE_SHIP_STATUSES.contains(prevStatus) && transferAllocations) {
          List<IInvAllocation> allocations =
              inventoryManagementService.getAllocationsByTypeId(shipment.getServicingKiosk(),
                  shipmentItem.getMaterialId(),
                  IInvAllocation.Type.SHIPMENT, shipmentId);
          List<ShipmentItemBatchModel> bq = new ArrayList<>(1);
          BigDecimal q = BigDecimal.ZERO;
          for (IInvAllocation allocation : allocations) {
            if (allocation.getBatchId() != null) {
              ShipmentItemBatchModel m = new ShipmentItemBatchModel();
              m.id = allocation.getBatchId();
              m.q = allocation.getQuantity();
              bq.add(m);
            } else {
              q = allocation.getQuantity();
            }
          }
          if (BigUtil.greaterThanZero(q)) {
            inventoryManagementService
                .transferAllocation(shipment.getServicingKiosk(), shipmentItem.getMaterialId(),
                    IInvAllocation.Type.SHIPMENT, shipmentId,
                    IInvAllocation.Type.ORDER, String.valueOf(orderId), q, null, userId, null, pm,
                    null, false);
          }
          if (CollectionUtils.isNotEmpty(bq)) {
            inventoryManagementService
                .transferAllocation(shipment.getServicingKiosk(), shipmentItem.getMaterialId(),
                    IInvAllocation.Type.SHIPMENT, shipmentId,
                    IInvAllocation.Type.ORDER, String.valueOf(orderId), null, bq, userId, null, pm,
                    null, false);
          }
        }
        IDemandItem demandItem = demandItems.get(shipmentItem.getMaterialId());
        if (decrementShipped) {
          demandItem.setShippedQuantity(
              demandItem.getShippedQuantity().subtract(shipmentItem.getQuantity()));
        }
        demandItem.setInShipmentQuantity(
            demandItem.getInShipmentQuantity().subtract(shipmentItem.getQuantity()));
        //TODO reset fulfilled quantities.
      }
      if (closePM) {
        tx.begin();
      }
      pm.makePersistentAll(demandItems.values());
      shipmentActivty.updateMessageAndActivity(shipmentId, message, userId, orderId, shipment.getDomainId(),
          prevStatus,
          shipment.getStatus(), null, pm);
      pm.makePersistent(shipment);
      orderManagementService.updateOrderMetadata(orderId, userId, pm);
      if (closePM) {
        tx.commit();
      }
    } catch (Exception e) {
      xLogger.severe("Error while cancelling the shipment {0}", shipmentId, e);
      ResourceBundle
          backendMessages =
          Resources.getBundle("BackendMessages", SecurityUtils.getLocale());
      throw new ServiceException(
          backendMessages.getString("shipment.cancel.error") + " " + shipmentId, e);
    } finally {
      if (closePM && tx.isActive()) {
        tx.rollback();
      }
      if (closePM) {
        pm.close();
      }
      if (LockUtil.shouldReleaseLock(lockStatus) && !LockUtil.release(Constants.TX_O + orderId)) {
        xLogger.warn("Unable to release lock for key {0}", Constants.TX_O + orderId);
      }
    }
    return true;
  }

  private Boolean validateStatusTransition(ShipmentStatus prevStatus, ShipmentStatus status) {
    if (ShipmentStatus.OPEN.equals(prevStatus) && (ShipmentStatus.READY_FOR_DISPATCH.equals(status)
        || ShipmentStatus.SHIPPED.equals(status) || ShipmentStatus.CANCELLED.equals(status))) {
      return Boolean.TRUE;
    }

    if (ShipmentStatus.READY_FOR_DISPATCH.equals(prevStatus) &&
        (ShipmentStatus.SHIPPED.equals(status) || ShipmentStatus.CANCELLED.equals(status))) {
      return Boolean.TRUE;
    }

    if (ShipmentStatus.SHIPPED.equals(prevStatus) && (ShipmentStatus.FULFILLED.equals(status)
        || ShipmentStatus.CANCELLED.equals(status))) {
      return Boolean.TRUE;
    }

    if (ShipmentStatus.FULFILLED.equals(prevStatus) && ShipmentStatus.CANCELLED.equals(status)) {
      return Boolean.TRUE;
    }

    return Boolean.FALSE;
  }

  private void postInventoryTransaction(IShipment shipment, String userId, PersistenceManager pm,
                                        ShipmentStatus prevStatus, int src)
      throws ServiceException {
    DomainConfig dc = DomainConfig.getInstance(shipment.getDomainId());
    if (!dc.autoGI()) {
      return;
    }
    List<ITransaction> transactionList = new ArrayList<>();
    List<IInvntry> inTransitList = new ArrayList<>(1);
    List<IInvntry> unFulfilledTransitList = null;
    List<ITransaction> errors;
    boolean checkBatch = true;
    for (IShipmentItem shipmentItem : shipment.getShipmentItems()) {
      IInvntry
          inv =
          inventoryManagementService
              .getInventory(shipment.getKioskId(), shipmentItem.getMaterialId(), pm);
      IInvntry vndInv =
          inventoryManagementService
              .getInventory(shipment.getServicingKiosk(), shipmentItem.getMaterialId(), pm);
      // If inventory is removed in the customer, do not try to post receipts for that material.
      if (inv == null && ShipmentStatus.FULFILLED.equals(shipment.getStatus())) {
        xLogger.warn(
            "Material with ID {0} does not exist in customer with ID {1} while posting transactions for shipment ID {2}",
            shipmentItem.getMaterialId(), shipment.getKioskId(), shipment.getShipmentId());
        continue;
      }
      if (vndInv == null && (ShipmentStatus.CANCELLED.equals(shipment.getStatus())
          || ShipmentStatus.SHIPPED.equals(shipment.getStatus()))) {
        xLogger.warn(
            "Material with ID {0} does not exist in vendor with ID {1} while posting transactions for shipment ID {2}",
            shipmentItem.getMaterialId(), shipment.getKioskId(), shipment.getShipmentId());
        continue;
      }
      ITransaction t = JDOUtils.createInstance(ITransaction.class);
      if (ShipmentStatus.SHIPPED.equals(shipment.getStatus()) || (
          prevStatus != ShipmentStatus.SHIPPED &&
              ShipmentStatus.FULFILLED.equals(shipment.getStatus()))) {
        t.setType(ITransaction.TYPE_ISSUE);
        t.setQuantity(shipmentItem.getQuantity());
        t.setReason(shipment.getReason());
        t.setKioskId(shipment.getServicingKiosk());
        t.setLinkedKioskId(shipment.getKioskId());
        t.setDomainId(shipment.getLinkedDomainId());
        if (inv != null) {
          inv.setInTransitStock(inv.getInTransitStock().add(shipmentItem.getQuantity()));
          inv.setUpdatedOn(new Date());
          inv.setUpdatedBy(userId);
          inTransitList.add(inv);
        }
      } else if (ShipmentStatus.CANCELLED.equals(shipment.getStatus())) {
        t.setType(ITransaction.TYPE_RECEIPT);
        t.setQuantity(shipmentItem.getQuantity());
        t.setReason(shipment.getCancelledDiscrepancyReasons());
        t.setKioskId(shipment.getServicingKiosk());
        t.setDomainId(shipment.getLinkedDomainId());
        if (ShipmentStatus.SHIPPED.equals(prevStatus) && inv != null) {
          BigDecimal inTransStock = inv.getInTransitStock().subtract(shipmentItem.getQuantity());
          inv.setInTransitStock(
              BigUtil.greaterThanEqualsZero(inTransStock) ? inTransStock : BigDecimal.ZERO);
          inTransitList.add(inv);
        }
      } else if (ShipmentStatus.FULFILLED.equals(shipment.getStatus())) {
        t.setType(ITransaction.TYPE_RECEIPT);
        t.setQuantity(shipmentItem.getFulfilledQuantity());
        t.setKioskId(shipmentItem.getKioskId());
        t.setLinkedKioskId(shipment.getServicingKiosk());
        t.setReason(shipmentItem.getFulfilledDiscrepancyReason());
        t.setDomainId(shipment.getKioskDomainId());
        t.setAtd(shipment.getActualFulfilmentDate());
        //Reduce quantity (instead of fulfilled quantity) , since in-transit is updated based on issue.
        BigDecimal inTransStock = inv.getInTransitStock().subtract(shipmentItem.getQuantity());
        inv.setInTransitStock(
            BigUtil.greaterThanEqualsZero(inTransStock) ? inTransStock : BigDecimal.ZERO);
        if (BigUtil.equalsZero(shipmentItem.getFulfilledQuantity())) {
          if (unFulfilledTransitList == null) {
            unFulfilledTransitList = new ArrayList<>(1);
          }
          unFulfilledTransitList.add(inv);
        } else {
          inTransitList.add(inv);
        }
        checkBatch = entitiesService.getKiosk(shipment.getKioskId(), false).isBatchMgmtEnabled();
      } else {
        ResourceBundle
            backendMessages =
            Resources.getBundle("BackendMessages", SecurityUtils.getLocale());
        throw new ServiceException(backendMessages.getString("inventory.post"));
      }
      t.setMaterialId(shipmentItem.getMaterialId());
      t.setSrc(src);
      t.setTrackingId(shipment.getShipmentId());
      t.setTrackingObjectType(getTrackingObjectType(shipment, pm));
      t.setSourceUserId(userId);
      List<IShipmentItemBatch>
          shipmentItemBatches =
          checkBatch ? (List<IShipmentItemBatch>) shipmentItem.getShipmentItemBatch() : null;
      if (shipmentItemBatches != null && !shipmentItemBatches.isEmpty()) {
        for (IShipmentItemBatch ib : shipmentItemBatches) {
          if ((ShipmentStatus.FULFILLED.equals(shipment.getStatus()) && BigUtil
              .equalsZero(ib.getFulfilledQuantity())) || (
              !(ShipmentStatus.FULFILLED.equals(shipment.getStatus())) && BigUtil
                  .equalsZero(ib.getQuantity()))) {
            continue;
          }
          ITransaction batchTrans = t.clone();
          batchTrans.setBatchId(ib.getBatchId());
          batchTrans.setBatchExpiry(ib.getBatchExpiry());
          if (ib.getBatchManufacturedDate() != null) {
            batchTrans.setBatchManufacturedDate(ib.getBatchManufacturedDate());
          }
          batchTrans.setBatchManufacturer(ib.getBatchManufacturer());
          if (ShipmentStatus.FULFILLED.equals(shipment.getStatus())) {
            batchTrans.setQuantity(ib.getFulfilledQuantity());
            batchTrans.setReason(ib.getFulfilledDiscrepancyReason());
            batchTrans.setMaterialStatus(ib.getFulfilledMaterialStatus());
          } else {
            batchTrans.setQuantity(ib.getQuantity());
            batchTrans.setMaterialStatus(ib.getShippedMaterialStatus());
          }
          if (BigUtil.greaterThanZero(batchTrans.getQuantity())) {
            transactionList.add(batchTrans);
          }
        }
      } else {
        t.setMaterialStatus(ShipmentStatus.FULFILLED.equals(shipment.getStatus()) ?
            shipmentItem.getFulfilledMaterialStatus() : shipmentItem.getShippedMaterialStatus());
        if (BigUtil.greaterThanZero(t.getQuantity())) {
          transactionList.add(t);
        }
      }
    }
    if (unFulfilledTransitList != null && !unFulfilledTransitList.isEmpty()) {
      pm.makePersistentAll(unFulfilledTransitList);
    }
    if (!transactionList.isEmpty()) {
      try {
        if (!ShipmentStatus.FULFILLED.equals(shipment.getStatus())) {
          pm.makePersistentAll(inTransitList);
          //Need to send inTransitlist only if order is fulfilled.
          inTransitList = null;
        }
        errors = inventoryManagementService.updateInventoryTransactions(shipment.getDomainId(),
            transactionList, inTransitList, true, false, pm).getErrorTransactions();

        if (dc.getInventoryConfig().isCREnabled() &&
            !(ShipmentStatus.FULFILLED.equals(shipment.getStatus()))) {
          Map<String, String> params = new HashMap<>(1);
          params.put("orderId", String.valueOf(shipment.getOrderId()));
          //Added 40 sec delay to let update inventory during post transaction
          AppFactory.get().getTaskService().schedule(ITaskService.QUEUE_OPTIMZER,
              Constants.UPDATE_PREDICTION_TASK, params,
              null, ITaskService.METHOD_POST, System.currentTimeMillis() + 40000);
        }
      } catch (ServiceException e) {
        xLogger.warn("ServiceException when doing auto {0} for order {1}: {2}",
            shipment.getShipmentId(),
            shipment.getOrderId(), e);
        throw e;
      } catch (Exception e) {
        xLogger.warn("Error in posting transactions", e);
        ResourceBundle
            backendMessages =
            Resources.getBundle("BackendMessages", SecurityUtils.getLocale());
        throw new ServiceException(backendMessages.getString("order.post.exception"), e);
      }
      if (CollectionUtils.isNotEmpty(errors)) {
        StringBuilder errorMsg = new StringBuilder();
        for (ITransaction error : errors) {
          errorMsg.append("-").append(error.getMessage());
        }
        xLogger.warn("Inventory posting failed {0}", errorMsg.toString());
        throw new ServiceException("T002", errorMsg.toString());
      }
    }
  }

  private ResponseModel validateStatusChange(IShipment shipment, String newStatus,
                                             PersistenceManager pm)
      throws ServiceException {
    // Validate vendor inventory if newStatus is shipped (throw exception) or cancelled (show warning if previous status is not pending)
    // Validate customer inventory if newstatus is fulfilled. (show warning)
    ResponseModel responseModel = new ResponseModel();
    List<IMaterial> materialsNotExistingInCustomer = getMaterialsNotExistingInKiosk(
        shipment.getKioskId(), shipment, pm);
    List<IMaterial>
        materialsNotExistingInVendor =
        getMaterialsNotExistingInKiosk(shipment.getServicingKiosk(), shipment, pm);
    // If auto posting of transactions is configured
    DomainConfig dc = DomainConfig.getInstance(shipment.getDomainId());
    if (ShipmentStatus.FULFILLED.toString().equals(newStatus)
        && materialsNotExistingInCustomer != null
        && !materialsNotExistingInCustomer.isEmpty()) {
      IKiosk cst = entitiesService.getKiosk(shipment.getKioskId(), false);
      responseModel.status = true;
      if (dc.autoGI()) {
        ResourceBundle
            backendMessages =
            Resources.getBundle("BackendMessages", SecurityUtils.getLocale());
        responseModel.message =
            backendMessages.getString("the.following.items")
                + CharacterConstants.SPACE + MsgUtil.bold(cst.getName())
                + CharacterConstants.DOT + CharacterConstants.SPACE + backendMessages
                .getString("receipts.not.posted") + CharacterConstants.DOT
                + MaterialUtils.getMaterialNamesString(
                materialsNotExistingInCustomer);
      }
      return responseModel;
    }
    if ((ShipmentStatus.SHIPPED.toString().equals(newStatus) || ShipmentStatus.CANCELLED.toString()
        .equals(newStatus)) && materialsNotExistingInVendor != null && !materialsNotExistingInVendor
        .isEmpty()) {
      IKiosk vnd = entitiesService.getKiosk(shipment.getServicingKiosk(), false);
      if (ShipmentStatus.SHIPPED.toString().equals(newStatus)) {
        throw new ServiceException("I006", MsgUtil.bold(vnd.getName()),
            MaterialUtils.getMaterialNamesString(
                materialsNotExistingInVendor));

      }
      if (ShipmentStatus.CANCELLED.toString().equals(newStatus) && !ShipmentStatus.PENDING
          .toString().equals(shipment.getStatus().toString()) && !ShipmentStatus.OPEN
          .toString().equals(shipment.getStatus().toString())) {
        responseModel.status = true;
        if (dc.autoGI()) {
          ResourceBundle
              backendMessages =
              Resources.getBundle("BackendMessages", SecurityUtils.getLocale());
          responseModel.message = backendMessages.getString("the.following.items")
              + CharacterConstants.SPACE + MsgUtil.bold(vnd.getName())
              + CharacterConstants.DOT + CharacterConstants.SPACE + backendMessages
              .getString("receipts.not.posted") + CharacterConstants.DOT + MaterialUtils
              .getMaterialNamesString(
                  materialsNotExistingInVendor);
        }
        return responseModel;
      }
    }
    responseModel.status = true;
    return responseModel;
  }

  /**
   * Determines the new Order status based on the current status of Shipments, It invoked Order
   * management service to update the same - If all Shipments are SHIPPED and the Demand Items have
   * no remaining quantity to be SHIPPED, then mark order as SHIPPED - If all Shipments are either
   * SHIPPED or FULFILLED and there is at least one SHIPPED item and Demand Items have no remaining
   * quantity to be SHIPPED, then marke Order as SHIPPED. - If all Shipments are FULFILLED and
   * Demand Items have no remaining quantity, then mark Order as FULFILLED. - If there is remaining
   * quantity on Demand Items and at least on Shipment is SHIPPED , then mark Order as BACKORDERED.
   * - If there is remaining quantity on Demand Items and all Shipments or OPEN or CANCELLED, then
   * Order status should have be in CONFIRMED/PENDING. If the Order status is not CONFIMED/PENDING
   * then change it to one of the previous available status between CONFIRMED and PENDING from
   * activity history of this order.
   *
   * @param orderId - Order Id of the order for which Status should be updated
   * @param status  - Shipment status
   * @param userId  - User Id who triggered this status change.
   * @param pm      - Persistence Manager.
   */
  private void updateOrderStatus(Long orderId, ShipmentStatus status, String userId,
                                 PersistenceManager pm)
      throws Exception {
    boolean fulfilled = status == ShipmentStatus.FULFILLED;
    boolean shipped = status == ShipmentStatus.SHIPPED;
    boolean readyForDispatch = status == ShipmentStatus.READY_FOR_DISPATCH;
    boolean cancelled = status == ShipmentStatus.CANCELLED;

    if (fulfilled || shipped || cancelled || readyForDispatch) {
      List<IShipment> shipments = shipmentRepository.getByOrderId(orderId, pm);
      Map<Long, IDemandItem> demandMap = demandService.getDemandMetadata(orderId, pm);
      boolean allItemsInShipments = true;
      for (IDemandItem demand : demandMap.values()) {
        if (BigUtil.notEquals(demand.getQuantity(), demand.getInShipmentQuantity())) {
          allItemsInShipments = false;
          break;
        }
      }
      String newOrderStatus = orderOverallStatusAction.invoke(shipments, allItemsInShipments, orderId);
      if (newOrderStatus != null) {
        try {
          orderManagementService
              .updateOrderStatus(orderId, newOrderStatus, userId, null, null, SourceConstants.WEB,
                  pm,
                  null);
        } catch (Exception e) {
          xLogger
              .warn("Error while updating order status from shipments for order {0}", orderId, e);
          throw e;
        }
      }
    }
  }

  public void checkShipmentRequest(Long customerKioskId, Long vendorKioskId, List itemList)
      throws ServiceException {

    IKiosk customerKiosk = entitiesService.getKiosk(customerKioskId);
    IKiosk vendorKiosk = entitiesService.getKiosk(vendorKioskId);

    boolean
        checkBEMaterials =
        customerKiosk.isBatchMgmtEnabled() && !vendorKiosk.isBatchMgmtEnabled();
    if (checkBEMaterials) {
      List<String> berrorMaterials = new ArrayList<>(1);

      Long materialId = null;
      BigDecimal quantity = null;
      for (Object item : itemList) {

        if (item instanceof ShipmentItem) {
          materialId = ((ShipmentItem) item).getMaterialId();
          quantity = ((ShipmentItem) item).getQuantity();
        } else if (item instanceof IDemandItem) {
          materialId = ((IDemandItem) item).getMaterialId();
          quantity = ((IDemandItem) item).getQuantity();
        }
        if (materialId != null && quantity != null) {
          IMaterial material = materialCatalogService.getMaterial(materialId);
          if (material.isBatchEnabled() && BigUtil.greaterThanZero(quantity)) {
            berrorMaterials.add(material.getName());
          }
        }
      }

      if (!berrorMaterials.isEmpty()) {
        throw new ServiceException("O005", berrorMaterials.size(), customerKiosk.getName(),
            StringUtil.getCSV(berrorMaterials));
      }

    }
  }

  private List<IMaterial> getMaterialsNotExistingInKiosk(Long kioskId, IShipment shipment,
                                                         PersistenceManager pm) {
    List<IMaterial> materialsNotExisting = new ArrayList<>(1);
    try {
      for (IShipmentItem shipmentItem : shipment.getShipmentItems()) {
        IInvntry
            inv =
            inventoryManagementService.getInventory(kioskId, shipmentItem.getMaterialId(), pm);
        if (inv == null && BigUtil.greaterThanZero(shipmentItem.getQuantity())) {
          IMaterial material = materialCatalogService.getMaterial(shipmentItem.getMaterialId());
          materialsNotExisting.add(material);
        }
      }
    } catch (ServiceException e) {
      xLogger.warn("Exception while getting materials not existing in kioskId {0}", kioskId, e);
    }
    return materialsNotExisting;
  }

  protected String getTrackingObjectType(IShipment shipment, PersistenceManager pm)
      throws ServiceException {
    return
        orderManagementService.getOrder(shipment.getOrderId(), false, pm).getOrderType()
            == IOrder.TRANSFER_ORDER
            ? ITransaction.TRACKING_OBJECT_TYPE_TRANSFER_SHIPMENT
            : ITransaction.TRACKING_OBJECT_TYPE_ORDER_SHIPMENT;
  }

}
