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

package com.logistimo.orders.service.impl;

import com.ibm.icu.util.Calendar;
import com.logistimo.AppFactory;
import com.logistimo.activity.entity.IActivity;
import com.logistimo.activity.service.ActivityService;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.config.models.EventSpec;
import com.logistimo.config.models.LeadTimeAvgConfig;
import com.logistimo.constants.CharacterConstants;
import com.logistimo.constants.Constants;
import com.logistimo.constants.SourceConstants;
import com.logistimo.conversations.entity.IMessage;
import com.logistimo.conversations.service.ConversationService;
import com.logistimo.dao.JDOUtils;
import com.logistimo.deliveryrequest.actions.ICreateDeliveryRequestAction;
import com.logistimo.deliveryrequest.models.DeliveryRequestModel;
import com.logistimo.deliveryrequest.models.DeliveryRequestSource;
import com.logistimo.deliveryrequest.models.DeliveryRequestUpdateWrapper;
import com.logistimo.domains.utils.DomainsUtil;
import com.logistimo.entities.entity.IKiosk;
import com.logistimo.entities.entity.IKioskLink;
import com.logistimo.entities.service.EntitiesService;
import com.logistimo.events.entity.IEvent;
import com.logistimo.events.models.CustomOptions;
import com.logistimo.events.processor.EventPublisher;
import com.logistimo.exception.LogiException;
import com.logistimo.exception.TaskSchedulingException;
import com.logistimo.exception.ValidationException;
import com.logistimo.inventory.TransactionUtil;
import com.logistimo.inventory.entity.IInvAllocation;
import com.logistimo.inventory.entity.IInvntry;
import com.logistimo.inventory.entity.ITransaction;
import com.logistimo.inventory.exceptions.InventoryAllocationException;
import com.logistimo.inventory.service.InventoryManagementService;
import com.logistimo.logger.XLog;
import com.logistimo.materials.entity.IHandlingUnit;
import com.logistimo.materials.entity.IMaterial;
import com.logistimo.materials.service.IHandlingUnitService;
import com.logistimo.materials.service.MaterialCatalogService;
import com.logistimo.models.shipments.ShipmentItemModel;
import com.logistimo.models.shipments.ShipmentModel;
import com.logistimo.orders.OrderResults;
import com.logistimo.orders.OrderUtils;
import com.logistimo.orders.actions.GenerateOrderEventsAction;
import com.logistimo.orders.actions.GenerateOrderInvoiceAction;
import com.logistimo.orders.actions.GetFilteredOrdersAction;
import com.logistimo.orders.actions.GetOrderOverallStatusAction;
import com.logistimo.orders.approvals.actions.OrderVisibilityAction;
import com.logistimo.orders.dao.IOrderDao;
import com.logistimo.orders.dao.OrderUpdateStatus;
import com.logistimo.orders.entity.IDemandItem;
import com.logistimo.orders.entity.IOrder;
import com.logistimo.orders.entity.Order;
import com.logistimo.orders.exception.AllocationNotCompleteException;
import com.logistimo.orders.models.OrderFilters;
import com.logistimo.orders.models.PDFResponseModel;
import com.logistimo.orders.models.ShipNowRequest;
import com.logistimo.orders.models.UpdateOrderTransactionsModel;
import com.logistimo.orders.models.UpdatedOrder;
import com.logistimo.orders.service.IDemandService;
import com.logistimo.orders.service.OrderManagementService;
import com.logistimo.orders.validators.UpdateOrderStatusValidator;
import com.logistimo.pagination.PageParams;
import com.logistimo.pagination.Results;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.ObjectNotFoundException;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;
import com.logistimo.services.impl.PMF;
import com.logistimo.services.taskqueue.ITaskService;
import com.logistimo.services.utils.ConfigUtil;
import com.logistimo.shipments.ShipmentStatus;
import com.logistimo.shipments.ShipmentUtils;
import com.logistimo.shipments.entity.IShipment;
import com.logistimo.shipments.service.IShipmentService;
import com.logistimo.shipments.service.impl.ShipmentService;
import com.logistimo.tags.TagUtil;
import com.logistimo.tags.dao.ITagDao;
import com.logistimo.tags.entity.ITag;
import com.logistimo.utils.BigUtil;
import com.logistimo.utils.LocalDateUtil;
import com.logistimo.utils.LockUtil;
import com.logistimo.utils.QueryUtil;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;

/**
 * @author arun
 */
@Service
public class OrderManagementServiceImpl implements OrderManagementService {

  private static final XLog xLogger = XLog.getLog(OrderManagementServiceImpl.class);
  private static final String
      UPDATE_ENTITYACTIVITYTIMESTAMPS_TASK =
      "/s2/api/entities/task/updateentityactivitytimestamps";

  private ITagDao tagDao;
  private IOrderDao orderDao;
  private IHandlingUnitService handlingUnitService;
  private ConversationService conversationService;
  private IShipmentService shipmentService;
  private EntitiesService entitiesService;
  private MaterialCatalogService materialCatalogService;
  private ActivityService activityService;
  private IDemandService demandService;
  private InventoryManagementService inventoryManagementService;

  private GenerateOrderEventsAction generateOrderEventsAction;
  private GenerateOrderInvoiceAction generateOrderInvoiceAction;
  private GetFilteredOrdersAction getFilteredOrdersAction;
  private UpdateOrderStatusValidator updateOrderStatusValidator;
  private OrderVisibilityAction orderVisibilityAction;
  private GetOrderOverallStatusAction orderOverallStatusAction;
  private ICreateDeliveryRequestAction createDeliveryRequestAction;

  @Autowired
  public void setTagDao(ITagDao tagDao) {
    this.tagDao = tagDao;
  }

  @Autowired
  public void setOrderDao(IOrderDao orderDao) {
    this.orderDao = orderDao;
  }

  @Autowired
  public void setHandlingUnitService(IHandlingUnitService handlingUnitService) {
    this.handlingUnitService = handlingUnitService;
  }

  @Autowired
  public void setConversationService(ConversationService conversationService) {
    this.conversationService = conversationService;
  }

  @Autowired
  public void setShipmentService(IShipmentService shipmentService) {
    this.shipmentService = shipmentService;
  }

  @Autowired
  public void setEntitiesService(EntitiesService entitiesService) {
    this.entitiesService = entitiesService;
  }

  @Autowired
  public void setMaterialCatalogService(MaterialCatalogService materialCatalogService) {
    this.materialCatalogService = materialCatalogService;
  }

  @Autowired
  public void setActivityService(ActivityService activityService) {
    this.activityService = activityService;
  }

  @Autowired
  public void setDemandService(IDemandService demandService) {
    this.demandService = demandService;
  }

  @Autowired
  public void setInventoryManagementService(InventoryManagementService inventoryManagementService) {
    this.inventoryManagementService = inventoryManagementService;
  }

  @Autowired
  public void setGenerateOrderEventsAction(GenerateOrderEventsAction generateOrderEventsAction) {
    this.generateOrderEventsAction = generateOrderEventsAction;
  }

  @Autowired
  public void setGenerateOrderInvoiceAction(GenerateOrderInvoiceAction generateOrderInvoiceAction) {
    this.generateOrderInvoiceAction = generateOrderInvoiceAction;
  }

  @Autowired
  public void setGetFilteredOrdersAction(GetFilteredOrdersAction getFilteredOrdersAction) {
    this.getFilteredOrdersAction = getFilteredOrdersAction;
  }

  @Autowired
  public void setUpdateOrderStatusValidator(UpdateOrderStatusValidator updateOrderStatusValidator) {
    this.updateOrderStatusValidator = updateOrderStatusValidator;
  }

  @Autowired
  public void setOrderVisibilityAction(OrderVisibilityAction orderVisibilityAction) {
    this.orderVisibilityAction = orderVisibilityAction;
  }

  @Autowired
  public void setOrderOverallStatusAction(
      GetOrderOverallStatusAction orderOverallStatusAction) {
    this.orderOverallStatusAction = orderOverallStatusAction;
  }

  @Autowired
  public void setCreateDeliveryRequestAction(ICreateDeliveryRequestAction
                                                 createDeliveryRequestAction) {
    this.createDeliveryRequestAction = createDeliveryRequestAction;
  }

  private static ITaskService getTaskService(){
    return AppFactory.get().getTaskService();
  }

  public IOrder getOrder(Long orderId) throws ServiceException {
    return getOrder(orderId, false, null);
  }

  public IOrder getOrder(Long orderId, boolean includeItems)
      throws ServiceException {
    return getOrder(orderId, includeItems, null);
  }

  /**
   * Get an order, given an order Id
   */
  @Override
  public IOrder getOrder(Long orderId, boolean includeItems, PersistenceManager pm)
      throws ServiceException {
    xLogger.fine("Entered getOrder");
    if (orderId == null) {
      throw new ServiceException("No order ID specified");
    }
    PersistenceManager localPM = pm;
    boolean isLocalPM = false;
    if (localPM == null) {
      localPM = PMF.get().getPersistenceManager();
      isLocalPM = true;
    }
    IOrder o = null;
    try {
      o = JDOUtils.getObjectById(IOrder.class, orderDao.createKey(orderId), localPM);
      o = localPM.detachCopy(o);
      if (includeItems) {
        o.setItems(demandService.getDemandItems(orderId, localPM));
      }
    } catch (JDOObjectNotFoundException e) {
      throw new ObjectNotFoundException("O009", orderId);
    } catch (Exception e) {
      xLogger.severe("Exception in getOrder: {0}", e.getMessage(), e);
      throw new ServiceException(e.getMessage());
    } finally {
      // Close PM
      if (isLocalPM && localPM != null) {
        localPM.close();
      }
    }
    xLogger.fine("Exiting getOrder");
    return o;
  }

  public UpdatedOrder updateOrder(IOrder order, int source) throws LogiException {
    return updateOrder(order, source, false, false);
  }

  public UpdatedOrder updateOrder(IOrder order, int source, boolean isLocked, boolean validateHU)
      throws LogiException {
    return updateOrder(order, source, isLocked, validateHU, null);
  }

  /**
   * Update an order, and post inventory issues/receipts, if needed.
   */
  public UpdatedOrder updateOrder(IOrder order, int source, boolean isLocked, boolean validateHU,
      String userId) throws LogiException {
    return updateOrder(order, source, isLocked, validateHU, userId, null);
  }

  public UpdatedOrder updateOrder(IOrder order, int source, boolean isLocked, boolean validateHU,
      String userId, PersistenceManager pm) throws LogiException {
    xLogger.fine("Entered updateOrder");
    if (order == null) {
      throw new ServiceException("Invalid order");
    }
    UpdatedOrder uo = new UpdatedOrder();
    boolean useLocalPM = pm == null;
    if (useLocalPM) {
      pm = PMF.get().getPersistenceManager();
    }

    LockUtil.LockStatus lockStatus = LockUtil.lock(Constants.TX_O + order.getOrderId());
    if (!LockUtil.isLocked(lockStatus)) {
      throw new ServiceException("O002", order.getOrderId());
    }
    try {
      if (validateHU) {
        validateHU((List<IDemandItem>) order.getItems());
      }

      if (IOrder.BACKORDERED.equals(order.getStatus())) {
        boolean isOrderCompleted = true;
        for (IDemandItem demandItem : order.getItems()) {
          if (BigUtil.notEquals(demandItem.getQuantity(), demandItem.getShippedQuantity())) {
            isOrderCompleted = false;
            break;
          }
        }
        if (isOrderCompleted) {
          List<IShipment> shipments = shipmentService.getShipmentsByOrderId(order.getOrderId());
          String newOrderStatus = orderOverallStatusAction.invoke(shipments, true, order.getOrderId());
          pm.makePersistentAll(order.getItems());
          updateOrderStatus(order.getOrderId(), newOrderStatus, userId, null, null, source, pm,
              null);
          order.setStatus(newOrderStatus);
        }
      }
      OrderUpdateStatus orderUpdateStatus = orderDao.update(order);
      if (order.getItems() != null) {
        pm.makePersistentAll(order.getItems());
      }
      order = uo.order = orderUpdateStatus.order;
      pm.makePersistent(order);
      // Generate event
      if (orderUpdateStatus.paymentChanged || orderUpdateStatus.statusChanged) {
        if (orderUpdateStatus.paymentChanged) {
          generateEvent(order.getDomainId(), IEvent.PAID, order, null, null);
        }
        if (orderUpdateStatus.statusChanged) {
          generateEvent(order.getDomainId(), IEvent.STATUS_CHANGE, order, null, null);
        }
      } else {
        generateEvent(order.getDomainId(), IEvent.MODIFIED, order, null, null);
      }
    } catch (LogiException e) {
      xLogger.severe("Exception in getOrder: {0}", e);
      throw e;
    } catch (Exception e) {
      xLogger.severe("Exception in getOrder: {0}", e);
      throw new ServiceException(e.getMessage());
    } finally {
      if (LockUtil.shouldReleaseLock(lockStatus) && !LockUtil
          .release(Constants.TX_O + order.getOrderId())) {
        xLogger.warn("Unable to release lock for key {0}", Constants.TX_O + order.getOrderId());
      }
      if (useLocalPM) {
        // Close PM
        pm.close();
      }
    }
    xLogger.fine("Exiting updateOrder");
    return uo;
  }

  private void validateHU(List<IDemandItem> items) throws LogiException {
    if (items != null) {
      for (IDemandItem dm : items) {
        validateHU(dm.getQuantity(), dm.getMaterialId(), null);
      }
    }
  }

  private IOrder getDetached(IOrder o, PersistenceManager pm) {
    //Required for SQL only..
    if (!ConfigUtil.getBoolean(Constants.GAE_DEPLOYMENT, false)) {
      List<IDemandItem> demandItems = new ArrayList<>();
      if (o.getItems() != null) {
        for (IDemandItem item : o.getItems()) {
          demandItems.add(pm.detachCopy(item));
        }
      }
      o = pm.detachCopy(o);
      o.setItems(demandItems);
    }
    return o;
  }

  public UpdatedOrder updateOrderStatus(Long orderId, String newStatus, String updatingUserId,
      String message, List<String> userIdsToBeNotified,
      int source) throws ServiceException {
    return updateOrderStatus(orderId, newStatus, updatingUserId, message, userIdsToBeNotified,
        source, null, null);
  }

  /**
   * Update an order's status, and post inventory issues/receipts, if needed.
   */
  public UpdatedOrder updateOrderStatus(Long orderId, String newStatus, String updatingUserId,
      String message, List<String> userIdsToBeNotified,
      int source, PersistenceManager pm, String crsn)
      throws ServiceException {

    boolean isLocalPM = pm == null;
    UpdatedOrder uo = null;
    Long domainId = null;
    LockUtil.LockStatus lockStatus = LockUtil.lock(Constants.TX_O + orderId);
    if (!LockUtil.isLocked(lockStatus)) {
      throw new ServiceException("O002", orderId);
    }
    Transaction tx = null;
    try {
      if (isLocalPM) {
        pm = PMF.get().getPersistenceManager();
        tx = pm.currentTransaction();
        tx.begin();
      }
      IOrder o = JDOUtils.getObjectById(IOrder.class, orderId, pm);
      if (newStatus == null || o.getStatus().equals(newStatus)) {
        return new UpdatedOrder(o);
      }

      List<IDemandItem> demandList = demandService.getDemandItems(orderId, pm);
      o.setItems(demandList);
      validateOrderStatusChangeAndAutoAllocateIfRequired(newStatus, updatingUserId, o);
      domainId = o.getDomainId();
      DomainConfig dc = DomainConfig.getInstance(domainId);
      String tag = IInvAllocation.Type.ORDER + CharacterConstants.COLON + orderId;
      if (newStatus.equals(IOrder.CANCELLED)) {
        o.setCancelledDiscrepancyReason(crsn);
        List<IShipment> shipments = shipmentService.getShipmentsByOrderId(orderId, pm);
        for (IShipment shipment : shipments) {
          xLogger
              .info("Cancelling shipment {0} for order {1}", orderId, shipment.getShipmentId());
          shipmentService.updateShipmentStatus(shipment.getShipmentId(), ShipmentStatus.CANCELLED, message,
              updatingUserId, crsn, false, pm, source, false);
        }
        if (dc.autoGI()) {
          inventoryManagementService.clearAllocationByTag(null, null, tag, pm);
        }

      } else if (newStatus.equals(IOrder.CONFIRMED) && dc.autoGI()
                 && dc.getOrdersConfig().allocateStockOnConfirmation()) {
        boolean autoAssignStatus = dc.getOrdersConfig().autoAssignFirstMatStatus();
        for (IDemandItem d : demandList) {
          try {
            inventoryManagementService.allocateAutomatically(o.getServicingKiosk(), d.getMaterialId(),
                IInvAllocation.Type.ORDER,
                String.valueOf(d.getOrderId()), tag, d.getQuantity(), d.getUserId(),
                autoAssignStatus, pm);
          } catch (InventoryAllocationException ie) {
            xLogger.warn("Unable to auto allocate for order {0}, k: {1}, m: {2}, q: {3}"
                , o.getOrderId(), d.getMaterialId(), d.getQuantity(), ie);
          }
        }
      } else if (newStatus.equals(IOrder.READY_FOR_DISPATCH)) {
        // ToDo: decide what to do with shipment status if present

      }

      uo = updateOrderStatus(o, newStatus, updatingUserId, message, pm);
      if (isLocalPM) {
        tx.commit();
      }
      uo.order = getDetached(o, pm);
    } catch (ValidationException e) {
      throw new ServiceException(e.getCode(), e.getArguments());
    } catch (ServiceException e) {
      throw e;
    } catch (Exception e) {
      xLogger.severe("Exception update order status {0}", e.getMessage(), e);
      throw new ServiceException(e.getClass().getName() + ": " + e.getMessage());
    } finally {
      if (LockUtil.shouldReleaseLock(lockStatus) && !LockUtil.release(Constants.TX_O + orderId)) {
        xLogger.warn("Unable to release lock for key {0}", Constants.TX_O + orderId);
      }
      if (isLocalPM) {
        if (tx != null && tx.isActive()) {
          tx.rollback();
        }
        pm.close();
      }
    }

    // Schedule a status change notification
    // NOTE: Do this after pm is closed so that the order status is persisted
    generateEvent(domainId, IEvent.STATUS_CHANGE, uo.order, null, userIdsToBeNotified);
    return uo;
  }

  private void validateOrderStatusChangeAndAutoAllocateIfRequired(String newStatus,
                                                                  String updatingUserId,
                                                                  IOrder o) throws ServiceException {
    try {
      updateOrderStatusValidator.validateOrderStatusChange(o, newStatus);
    } catch (AllocationNotCompleteException e) {
      if(Objects.equals(newStatus, IOrder.READY_FOR_DISPATCH)) {
        allocateInventoryAutomatically(updatingUserId, o.getServicingKiosk(),
            o.getOrderId(), o.getItems());
      }
      updateOrderStatusValidator.validateOrderStatusChange(o, newStatus);
    }
  }

  private void allocateInventoryAutomatically(String userId, Long vendorId, Long orderId,
                                              List<? extends IDemandItem> items)
      throws AllocationNotCompleteException {
    String tag = IInvAllocation.Type.ORDER + CharacterConstants.COLON + orderId;
    PersistenceManager localPM = PMF.get().getPersistenceManager();
    Transaction tx = localPM.currentTransaction();
    try {
      tx.begin();
      for(IDemandItem item : items) {
        inventoryManagementService.allocateAutomatically(vendorId, item.getMaterialId(),
            IInvAllocation.Type.ORDER, String.valueOf(orderId), tag, item.getQuantity(), userId,
            true, localPM);
      }
      tx.commit();
    } catch(ServiceException e) {
      xLogger.warn("Automatic allocation failed", e);
      throw new AllocationNotCompleteException("O018");
    } finally {
      if (tx.isActive()) {
        tx.rollback();
      }
      localPM.close();
    }
  }

  private UpdatedOrder updateOrderStatus(IOrder o, String newStatus, String updatingUserId,
      String message, PersistenceManager pm)
      throws ServiceException {
    xLogger.fine("Entered updateOrderStatus");
    if (o == null || newStatus == null || newStatus.isEmpty() || o.isStatus(newStatus)) {
      throw new IllegalArgumentException(
          "Invalid order or order status: " + newStatus + " old status: " + (o != null ? o
              .getStatus() : "'Order is null'"));
    }
    UpdatedOrder uo = new UpdatedOrder();
    try {
      String oldStatus = o.getStatus();

      // Change status
      o.setStatus(newStatus);
      o.commitStatus(); // NOTE: This method takes care of propagating status, setting order processing times, and/or updating accounts if accounting is enabled
      pm.makePersistentAll(o.getItems());
      o.setUpdatedBy(updatingUserId);
      o.setUpdatedOn(new Date(o.getStatusUpdatedOn().getTime()));
      IMessage iMessage = null;
      if (message != null && !message.isEmpty()) {
        iMessage = addMessageToOrder(o.getOrderId(), o.getDomainId(), message, updatingUserId, pm);
      }
      addStatusHistory(o.getOrderId(), oldStatus, newStatus, o.getDomainId(), iMessage,
          updatingUserId, pm);

    } catch (Exception e) {
      xLogger.severe("Exception in updateOrderStatus: {0} : {1}", e.getClass().getName(),
          e.getMessage(), e);
      throw new ServiceException(e);
    }
    xLogger.fine("Exiting updateOrderStatus");
    return uo;
  }

  public IMessage addMessageToOrder(Long orderId, String message, String userId)
      throws ServiceException {
    IOrder order = getOrder(orderId);
    return addMessageToOrder(orderId, order.getDomainId(), message, userId, null);
  }

  @Override
  public String shipNow(IOrder order, String userId, ShipNowRequest shipmentNowRequest,
                        int source, Boolean updateOrderFields, Date expectedFulfilmentDate)
      throws ServiceException {

    ShipmentModel model = buildShipmentModel(order, userId, ShipmentStatus.SHIPPED);
    if (expectedFulfilmentDate != null) {
      model.ead = new SimpleDateFormat(Constants.DATE_FORMAT).format(expectedFulfilmentDate);
    }
    populateShipOrderDataToShipment(shipmentNowRequest, model);
    return shipmentService.createShipment(model, source, updateOrderFields, null);
  }

  private void populateShipOrderDataToShipment(ShipNowRequest shipmentNowRequest,
                                               ShipmentModel model) {
    if(shipmentNowRequest != null) {
      model.transporterId = shipmentNowRequest.getTransporterId();
      model.transporter = shipmentNowRequest.getTransporter();
      model.phonenum = shipmentNowRequest.getPhoneNum();
      model.trackingId = shipmentNowRequest.getTrackingId();
      model.ps = shipmentNowRequest.getPackageSize();
      model.reason = shipmentNowRequest.getReason();
      model.salesRefId = shipmentNowRequest.getSalesRefId();
      model.isCustomerPickup = shipmentNowRequest.isCustomerPickup();
      model.vehicle = shipmentNowRequest.getVehicle();
      model.consignment = shipmentNowRequest.getConsignment();
    }
  }

  private ShipmentModel buildShipmentModel(Long orderId, String userId, PersistenceManager pm)
      throws ServiceException {
    IOrder order = getOrder(orderId, true, pm);
    return buildShipmentModel(order, userId, ShipmentStatus.OPEN);
  }

  @Override
  public DeliveryRequestModel createDeliveryRequest(DeliveryRequestModel model)
      throws LogiException {
    DeliveryRequestSource source = StringUtils.isBlank(model.getShipmentId()) ?
        DeliveryRequestSource.order : DeliveryRequestSource.shipment;
    SecureUserDetails user = SecurityUtils.getUserDetails();
    DeliveryRequestUpdateWrapper updateModel = raiseDeliveryRequest(model, source, user);
    String shipmentId = model.getShipmentId();
    shipmentService.updateShipmentDetails(shipmentId, user.getUsername(), updateModel);
    if (source == DeliveryRequestSource.order) {
      ResourceBundle backendMessages = Resources.getBundle(user.getLocale());
      shipmentService.updateShipmentStatus(shipmentId, ShipmentStatus.READY_FOR_DISPATCH,
          backendMessages.getString("dr.cr.success"), Constants.SYSTEM_USER_ID,
          CharacterConstants.EMPTY, false, null, SourceConstants.WEB, true);
    }
    return null;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public DeliveryRequestUpdateWrapper raiseDeliveryRequest(DeliveryRequestModel model,
                                                            DeliveryRequestSource source,
                                                            SecureUserDetails user)
      throws LogiException {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    Transaction tx = pm.currentTransaction();
    try {
      tx.begin();
      if (source == DeliveryRequestSource.order) {
        validateAndCreateShipment(user, model, pm);
      } else if(source == DeliveryRequestSource.shipment) {
        validateAndUpdateShipmentConsignmentDetails(user.getUsername(), model, pm);
      }
      DeliveryRequestUpdateWrapper updateModel = createDeliveryRequestAction.invoke(user,
          user.getCurrentDomainId(), model);
      tx.commit();
      return updateModel;
    } finally {
      if(tx.isActive()) {
        tx.rollback();
      }
      PMF.close(pm);
    }
  }

  private void validateAndUpdateShipmentConsignmentDetails(String userId,
                                                           DeliveryRequestModel model,
                                                           PersistenceManager pm)
      throws LogiException {
    model.setOrderId(ShipmentUtils.extractOrderId(model.getShipmentId()));
    IShipment shipment = shipmentService.getShipment(model.getShipmentId());
    if(shipment != null) {
      if(ShipmentStatus.READY_FOR_DISPATCH != shipment.getStatus()) {
        throw new ServiceException("O019", new Object[]{});
      }
      Long consignmentId = shipment.getConsignmentId();
      if (consignmentId != null) {
        shipmentService.updateConsignmentDetails(consignmentId, model.getConsignment(), pm);
        Map<String, String> updData = new HashMap<>();
        updData.put(ShipmentService.SHIPMENT_TRANSPORTER_ID,
            String.valueOf(model.getTrackingDetails().getTransporterId()));
        updData.put(ShipmentService.SHIPMENT_TRANSPORTER_NAME,
            String.valueOf(model.getTrackingDetails().getTransporter()));
        updData.put(ShipmentService.SHIPMENT_CONTACT_PHONE_NUM,
            String.valueOf(model.getTrackingDetails().getPhoneNumber()));
        shipmentService.updateShipmentData(updData, null, model.getShipmentId(), userId);
      }
    } else {
      throw new ServiceException("O020", new Object[]{});
    }
  }

  /**
   * Performs status and existing shipment validations
   * Creates a new shipment if all validations are passed
   * @param user
   * @param model
   * @param pm
   * @throws ServiceException if any validation fails
   */
  private void validateAndCreateShipment(SecureUserDetails user, DeliveryRequestModel model,
                                         PersistenceManager pm)
      throws ServiceException {
    IOrder order = getOrder(model.getOrderId(), false, pm);
    validateNonCancelledShipmentShouldNotExist(user.getLocale(), model.getOrderId(), pm);
    if(!IOrder.READY_FOR_DISPATCH.equals(order.getStatus())) {
      throw new ServiceException("O019", new Object[]{});
    }
    model.setShipmentId(createShipment(user, model, pm));
  }

  private void validateNonCancelledShipmentShouldNotExist(Locale locale, Long orderId,
                                                          PersistenceManager pm) throws ServiceException {
    List<IShipment> shipments = shipmentService.getShipmentsByOrderId(orderId, pm);
    if(CollectionUtils.isNotEmpty(shipments)) {
      Optional<IShipment> activeShipment = shipments.stream()
          .filter(shipment -> !ShipmentStatus.INACTIVE_SHIPMENT_STATUSES
              .contains(shipment.getStatus()))
          .findAny();
      if(activeShipment.isPresent()) {
        ResourceBundle backendMessages = Resources.getBundle(locale);
        throw new ServiceException(backendMessages.getString("dr.err.active.shipment"));
      }
    }
  }

  private String createShipment(SecureUserDetails user, DeliveryRequestModel model,
                                PersistenceManager pm)
      throws ServiceException, ValidationException {
    ShipmentModel shipmentModel = buildShipmentModel(model.getOrderId(), user.getUsername(), pm);
    shipmentModel.consignment = model.getConsignment();
    if(model.getTrackingDetails() == null) {
      throw new ServiceException("O020", new Object[]{});
    }
    shipmentModel.transporter = model.getTrackingDetails().getTransporter();
    shipmentModel.transporterId = model.getTrackingDetails().getTransporterId();
    shipmentModel.phonenum = model.getTrackingDetails().getPhoneNumber();
    shipmentModel.status = ShipmentStatus.OPEN;
    shipmentModel.createdBy = user.getUsername();
    return shipmentService.createShipment(shipmentModel, SourceConstants.WEB, true, pm);
  }

  private ShipmentModel buildShipmentModel(IOrder order, String userId, ShipmentStatus status) throws ServiceException {
    ShipmentModel model = new ShipmentModel();
    model.orderId = order.getOrderId();
    model.customerId = order.getKioskId();
    model.vendorId = order.getServicingKiosk();
    IKiosk vendor = entitiesService.getKiosk(model.vendorId, false);
    model.status = status;
    model.tags = order.getTags(TagUtil.TYPE_ORDER);
    model.userID = userId;
    model.items = new ArrayList<>();
    model.sdid = order.getDomainId();
    DomainConfig dc = DomainConfig.getInstance(order.getDomainId());
    for (IDemandItem demandItem : order.getItems()) {
      if (BigUtil.greaterThanZero(demandItem.getQuantity())) {
        ShipmentItemModel shipmentItemModel = new ShipmentItemModel();
        shipmentItemModel.mId = demandItem.getMaterialId();
        shipmentItemModel.q = demandItem.getQuantity();
        shipmentItemModel.afo = dc.autoGI();
        IMaterial material = materialCatalogService.getMaterial(shipmentItemModel.mId);
        shipmentItemModel.isBa = material.isBatchEnabled() && vendor.isBatchMgmtEnabled();
        shipmentItemModel.mnm = material.getName();
        model.items.add(shipmentItemModel);
      }
    }
    return model;
  }

  private IMessage addMessageToOrder(Long orderId, Long domainId, String message,
      String updatingUserId,
      PersistenceManager pm) throws ServiceException {
    IMessage
        iMessage =
        conversationService.addMsgToConversation("ORDER", orderId.toString(), message, updatingUserId,
            Collections.singleton("ORDER:" + orderId), domainId, pm);
    generateOrderCommentEvent(domainId, IEvent.COMMENTED, JDOUtils.getImplClassName(IOrder.class),
        orderId.toString(), null, null);
    return iMessage;

  }

  // Generate shipment events, if configured
  @Override
  public void generateOrderCommentEvent(Long domainId, int eventId, String objectType,
      String objectId, String message,
      List<String> userIds) {
    try {
      // Custom options
      CustomOptions customOptions = new CustomOptions();
      if (message != null && !message.isEmpty() || (userIds != null && !userIds.isEmpty())) {
        customOptions.message = message;
        if (userIds != null && !userIds.isEmpty()) {
          Map<Integer, List<String>> userIdsMap = new HashMap<>();
          userIdsMap.put(EventSpec.NotifyOptions.IMMEDIATE, userIds);
          customOptions.userIds = userIdsMap;
        }
      }
      // Generate event, if needed
      EventPublisher.generate(domainId, eventId, null, objectType, objectId,
          customOptions);
    } catch (Exception e) {
      xLogger.severe("{0} when generating Comment event {1} for object {2} in domain {3}: {4}",
          e.getClass().getName(), eventId, objectId, domainId, e);
    }
  }

  private void addStatusHistory(Long orderId, String oldStatus, String newStatus, Long domainId,
      IMessage iMessage,
      String userId, PersistenceManager pm) {
    activityService
        .createActivity(IActivity.TYPE.ORDER.name(), String.valueOf(orderId), "STATUS", oldStatus,
            newStatus,
            userId, domainId, iMessage != null ? iMessage.getMessageId() : null,
            "ORDER:" + orderId, pm);
  }

  /**
   * Get orders for a given kiosk, status (optional) and a time limit (optional)
   */
  @SuppressWarnings("unchecked")
  public Results getOrders(Long domainId, Long kioskId, String status, Date since, Date until,
      String otype, String tagType, String tag, List<Long> kioskIds,
                           PageParams pageParams, Integer orderType, String salesReferenceId,
                           String approvalStatus,
                           String purchaseReferenceId, String transferReferenceId)
      throws ServiceException {
    return getOrders(domainId, kioskId, status, since, until, otype, tagType, tag, kioskIds,
        pageParams, orderType, salesReferenceId, approvalStatus, false, purchaseReferenceId,
        transferReferenceId, null);
  }

  public Results getOrders(Long domainId, Long kioskId, String status, Date since, Date until,
                           String otype, String tagType, String tag, List<Long> kioskIds,
                           PageParams pageParams, Integer orderType, String salesReferenceId,
                           String approvalStatus,
                           String purchaseReferenceId, String transferReferenceId, Long linkedKioskId)
      throws ServiceException {
    return getOrders(domainId, kioskId, status, since, until, otype, tagType, tag, kioskIds,
        pageParams, orderType, salesReferenceId, approvalStatus, false, purchaseReferenceId,
        transferReferenceId, linkedKioskId);
  }

  public Results getOrders(Long domainId, Long kioskId, String status, Date since, Date until,
                           String otype, String tagType, String tag, List<Long> kioskIds,
                           PageParams pageParams, Integer orderType, String salesReferenceId,
                           String approvalStatus, boolean withDemand, String purchaseReferenceId,
                           String transferReferenceId, Long linkedKioskId) {
    OrderFilters filters = new OrderFilters().setDomainId(domainId)
        .setKioskId(kioskId)
        .setStatus(status)
        .setSince(since)
        .setUntil(until)
        .setOtype(otype)
        .setTagType(tagType)
        .setTag(tag)
        .setKioskIds(kioskIds)
        .setOrderType(orderType)
        .setSalesReferenceId(salesReferenceId)
        .setApprovalStatus(approvalStatus)
        .setWithDemand(withDemand)
        .setPurchaseReferenceId(purchaseReferenceId)
        .setTransferReferenceId(transferReferenceId)
        .setLinkedKioskId(linkedKioskId);
    return getOrders(filters, pageParams);
  }

  @Override
  public Results getOrders(OrderFilters orderFilters, PageParams pageParams) {
    return getFilteredOrdersAction.invoke(orderFilters, pageParams);
  }

  /**
   * Get orders based on kiosk,status,ordertype
   *
   * @param kioskId - Kiosk ID
   * @param status -Order status
   * @param pageParams -Page params with max results and offset
   * @param orderType -Order type sle for sales and prc for purchase
   * @param isTransfer - True for transfers, false if it is sales/purchase
   * @return List of IOrder
   * @throws ServiceException from service layer
   */
  public List<IOrder> getOrders(Long kioskId, String status, PageParams pageParams,
      String orderType, boolean isTransfer) throws ServiceException {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    List<String> parameters = new ArrayList<>(1);
    StringBuilder queryBuilder = new StringBuilder("SELECT * FROM `ORDER` ");
    Query query = null;
    List<IOrder> results;
    try {

      //Set the oty based on transfer or not
      queryBuilder.append(" WHERE OTY").append(isTransfer ? "=?" : "!=?");
      parameters.add(String.valueOf(IOrder.TRANSFER));

      //If the order type is purchase append kid, if it is sales append the lkid
      if (OrderUtils.isValidOrderType(orderType)) {
        if (orderType.equalsIgnoreCase(IOrder.TYPE_PURCHASE)) {
          queryBuilder.append(" AND KID =?");
        } else {
          queryBuilder.append(" AND SKID =?");
        }
        parameters.add(String.valueOf(kioskId));
      }
      //Append status information
      if (StringUtils.isNotBlank(status) && OrderUtils.isValidOrderStatus(status)) {
        queryBuilder.append("AND ST=?");
        parameters.add(status);
      }

      queryBuilder.append(" ORDER BY UON DESC");
      queryBuilder.append(" LIMIT ")
          .append(pageParams.getOffset())
          .append(CharacterConstants.COMMA)
          .append(pageParams.getSize());
      query = pm.newQuery(Constants.JAVAX_JDO_QUERY_SQL, queryBuilder.toString());
      query.setClass(Order.class);
      results = (List<IOrder>) query.executeWithArray(parameters.toArray());
      results = (List<IOrder>) pm.detachCopyAll(results);
    } catch (Exception e) {
      xLogger.warn("Exception while fetching orders minimum response", e);
      throw new ServiceException("Service exception fetching order details");
    } finally {
      if (query != null) {
        try {
          query.closeAll();
        } catch (Exception e) {
          xLogger.warn("Exception while closing query", e);
        }
      }
      pm.close();
    }
    return results;
  }

  @Override
  public void updateOrderReferenceId(Long orderId, String salesReferenceId, String userId,
                                     PersistenceManager pm) throws ServiceException {
    PersistenceManager localPm = pm;
    if (pm == null) {
      localPm = PMF.get().getPersistenceManager();
    }
    try {
      IOrder order = JDOUtils.getObjectById(IOrder.class, orderDao.createKey(orderId), localPm);
      order.setSalesReferenceID(salesReferenceId);
    } finally {
      if (pm == null && localPm != null) {
        localPm.close();
      }
    }
  }

  /**
   * Get orders placed by a certain user
   */
  @SuppressWarnings("unchecked")
  public Results getOrders(String userId, Date fromDate, Date toDate, PageParams pageParams)
      throws ServiceException {
    xLogger.fine("Entered getOrders (by user)");
    if (userId == null || userId.isEmpty() || fromDate == null) {
      throw new IllegalArgumentException("User ID or 'from' date not specified");
    }
    PersistenceManager pm = PMF.get().getPersistenceManager();
    String
        query =
        "SELECT FROM " + JDOUtils.getImplClass(IOrder.class).getName()
            + " WHERE uId == uIdParam && cOn > fromParam";
    Map<String, Object> params = new HashMap<>();
    params.put("uIdParam", userId);
    params.put("fromParam", LocalDateUtil.getOffsetDate(fromDate, -1, Calendar.MILLISECOND));
    if (toDate != null) {
      query += " && cOn < toParam";
      params.put("toParam", toDate);
    }
    query += " PARAMETERS String uIdParam, Date fromParam";
    if (toDate != null) {
      query += ", Date toParam";
    }
    query += " import java.util.Date; ORDER by cOn desc";
    // Form query
    Query q = pm.newQuery(query);
    if (pageParams != null) {
      QueryUtil.setPageParams(q, pageParams);
    }
    // Execute query
    List<IOrder> orders = null;
    String cursor = null;
    try {
      orders = (List<IOrder>) q.executeWithMap(params);
      if (orders != null) {
        orders.size();
        cursor = QueryUtil.getCursor(orders);
        orders = (List<IOrder>) pm.detachCopyAll(orders);
      }
    } finally {
      try {
        q.closeAll();
      } catch (Exception ignored) {
        xLogger.warn("Exception while closing query", ignored);
      }
      pm.close();
    }
    xLogger.fine("Exiting getOrders (by user)");
    return new Results(orders, cursor);
  }

  /**
   * Get demand items according to specified criteria (returns only unfulfilled demand) NOTE:
   * domainId is the mandatory attribute, all others are optional; either kiosk or material id can
   * be specified, but NOT both
   */
  @SuppressWarnings("unchecked")
  public Results getDemandItems(Long domainId, Long kioskId, Long materialId, String kioskTag,
      String materialTag, Date since, PageParams pageParams)
      throws ServiceException {
    xLogger.fine("Entered getDemandItems");
    if (domainId == null && kioskId == null && materialId == null) {
      throw new ServiceException(
          "Neither domain Id, kiosk Id or material Id is specified. At least one of them must be specified.");
    }
    if (kioskId != null && materialId != null) {
      throw new ServiceException(
          "Both kiosk and material are specified. Only one of them is allowed");
    }
    OrderResults results = null;
    PersistenceManager pm = PMF.get().getPersistenceManager();
    try {
      // Get query criteria
      String filter = "";
      String declaration = "";
      String imports = null;
      Map<String, Object> paramMap = new HashMap<>();
      if (domainId != null) {
        filter += "dId.contains(dIdParam)";
        declaration += "Long dIdParam";
        paramMap.put("dIdParam", domainId);
      }
      if (kioskId != null) {
        if (!filter.isEmpty()) {
          filter += " && ";
        }
        filter += "kId == kIdParam";
        if (!declaration.isEmpty()) {
          declaration += ", ";
        }
        declaration += "Long kIdParam";
        paramMap.put("kIdParam", kioskId);
      } else if (materialId != null) {
        if (!filter.isEmpty()) {
          filter += " && ";
        }
        filter += "mId == mIdParam";
        if (!declaration.isEmpty()) {
          declaration += ", ";
        }
        declaration += "Long mIdParam";
        paramMap.put("mIdParam", materialId);
      }
      // Add tags, if present
      if (kioskId == null && kioskTag != null && !kioskTag.isEmpty()) {
        if (!filter.isEmpty()) {
          filter += " && ";
        }
        filter += "ktgs.contains(ktgsParam)";
        if (!declaration.isEmpty()) {
          declaration += ", ";
        }
        declaration += "Long ktgsParam";
        paramMap.put("ktgsParam", tagDao.getTagFilter(kioskTag, ITag.KIOSK_TAG));
      } else if (materialId == null && materialTag != null && !materialTag.isEmpty()) {
        if (!filter.isEmpty()) {
          filter += " && ";
        }
        filter += "mtgs.contains(mtgsParam)";
        if (!declaration.isEmpty()) {
          declaration += ", ";
        }
        declaration += "Long mtgsParam";
        paramMap.put("mtgsParam", tagDao.getTagFilter(materialTag, ITag.MATERIAL_TAG));
      }
      // Add the time filter
      if (since != null) {
        filter += " && t > tParam";
        declaration += ", Date tParam";
        imports = "import java.util.Date;";
        paramMap.put("tParam", LocalDateUtil.getOffsetDate(since, -1, Calendar.MILLISECOND));
      }

      // Filter Order status other than Fulfilled or Cancelled
      List<String> orderStatus = Arrays.asList("cm", "cf", "pn");
      if (!filter.isEmpty()) {
        filter += " && ";
        declaration += ",";
      }
      filter += " ost.contains( st )";
      declaration += "java.util.Collection ost";
      paramMap.put("ost", orderStatus);

      // Form query
      Query q = pm.newQuery(JDOUtils.getImplClass(IDemandItem.class));
      q.setFilter(filter);
      q.declareParameters(declaration);
      q.setOrdering("t desc");
      if (imports != null) {
        q.declareImports(imports);
      }
      // Add pagination parameters, if needed
      if (pageParams != null) {
        QueryUtil.setPageParams(q, pageParams);
      }
      // Execute query
      try {
        List<IDemandItem> items = (List<IDemandItem>) q.executeWithMap(paramMap);
        items.size(); // to ensure objects are retrieved before PM is closed.
        // Get the cursor of the next element in the result set (for future iteration, efficiently)
        String cursorStr = QueryUtil.getCursor(items);
        items = (List<IDemandItem>) pm.detachCopyAll(items);
        // Create the result set
        results = new OrderResults(items, cursorStr);
      } finally {
        q.closeAll();
      }
    } catch (Exception e) {
      xLogger.severe("Exception in getDemandItems: {0}", e.getMessage());
      throw new ServiceException(e.getMessage());
    } finally {
      // Close PM
      pm.close();
    }
    xLogger.fine("Exiting getDemandItems");
    return results;
  }

  public OrderResults updateOrderTransactions(
      Long domainId, String userId, String transType, List<ITransaction> inventoryTransactions,
      Long kioskId, Long trackingId,
      String message, boolean createOrder, Long servicingKioskId, Double latitude, Double longitude,
      Double geoAccuracy, String geoErrorCode,
      String utcExpectedFulfillmentTimeRangesCSV, String utcConfirmedFulfillmentTimeRange,
      BigDecimal payment, String paymentOption, String packageSize,
      boolean allowEmptyOrders, int src) throws ServiceException {

    return updateOrderTransactions(domainId, userId, transType, inventoryTransactions, kioskId,
        trackingId, message, createOrder, servicingKioskId, latitude, longitude,
        geoAccuracy, geoErrorCode, utcExpectedFulfillmentTimeRangesCSV,
        utcConfirmedFulfillmentTimeRange, payment, paymentOption,
        packageSize, allowEmptyOrders, null, null, null, null, null, null, src);
  }

  @Override
  public OrderResults updateOrderTransactions(
      Long domainId, String userId, String transType, List<ITransaction> inventoryTransactions,
      Long kioskId,
      Long trackingId, String message, boolean createOrder, Long servicingKioskId, Double latitude,
      Double longitude, Double geoAccuracy, String geoErrorCode,
      String utcExpectedFulfillmentTimeRangesCSV,
      String utcConfirmedFulfillmentTimeRange, BigDecimal payment, String paymentOption,
      String packageSize,
      boolean allowEmptyOrders, List<String> orderTags, Integer orderType, Boolean isSalesOrder,
      String referenceId, Date reqByDate, Date eta, int src) throws ServiceException {
    return updateOrderTransactions(
        new UpdateOrderTransactionsModel(domainId, userId, transType, inventoryTransactions,
            kioskId, trackingId, message, createOrder, servicingKioskId, latitude, longitude,
            geoAccuracy, geoErrorCode, utcExpectedFulfillmentTimeRangesCSV,
            utcConfirmedFulfillmentTimeRange, payment, paymentOption, packageSize, allowEmptyOrders,
            orderTags, orderType, isSalesOrder, referenceId, reqByDate, eta, src, null, null, null,
            null));
  }

  @Override
  public OrderResults updateOrderTransactions(
      UpdateOrderTransactionsModel updateOrderTransactionsRequest)
      throws ServiceException {
    xLogger.fine("Entering updateOrderTransactions");
    if (updateOrderTransactionsRequest.getDomainId() == null) {
      throw new ServiceException("Unknown domain");
    }
    boolean useLocalPM = updateOrderTransactionsRequest.getPm() == null;
    if (useLocalPM) {
      updateOrderTransactionsRequest.setPm(PMF.get().getPersistenceManager());
    }
    Date now = new Date(); // timestamp for transactions
    IOrder o = null;
    List<IDemandItem> items = null;
    // Flag for re-ordering
    boolean reorder = ITransaction.TYPE_REORDER.equals(
        updateOrderTransactionsRequest.getTransType());
    javax.jdo.Transaction tx = null;
    // Check transaction availability
    if ((updateOrderTransactionsRequest.getInventoryTransactions() == null
        || updateOrderTransactionsRequest
        .getInventoryTransactions().isEmpty()) && !(reorder
        || updateOrderTransactionsRequest.isAllowEmptyOrders())) {
      throw new ServiceException("Transaction list cannot be empty");
    }
    // Update or create order
    if (reorder) {
      if (updateOrderTransactionsRequest.getTrackingId() == null) {
        xLogger.severe("No tracking id sent on re-order for kiosk {0}",
            updateOrderTransactionsRequest.getKioskId());
        throw new ServiceException("Order id was not specified");
      }
      LockUtil.LockStatus lockStatus = LockUtil.lock(Constants.TX_O + updateOrderTransactionsRequest
          .getTrackingId());
      if (!LockUtil.isLocked(lockStatus)) {
        throw new ServiceException("O002", updateOrderTransactionsRequest.getTrackingId());
      }
      try {
        // Get the order
        o = JDOUtils.getObjectById(IOrder.class, updateOrderTransactionsRequest.getTrackingId(),
            updateOrderTransactionsRequest.getPm());
        o.setItems(demandService.getDemandItems(o.getOrderId(),
            updateOrderTransactionsRequest.getPm()));
        xLogger.fine("inventoryTransactions: {0}, order size: {1}",
            (updateOrderTransactionsRequest.getInventoryTransactions() == null ? "NULL"
                : updateOrderTransactionsRequest
                    .getInventoryTransactions().size()), o.size());
        o.setDueDate(updateOrderTransactionsRequest.getReqByDate());
        o.setExpectedArrivalDate(updateOrderTransactionsRequest.getEta());
        modifyOrder(o, updateOrderTransactionsRequest.getUserId(),
            updateOrderTransactionsRequest.getInventoryTransactions(), now,
            updateOrderTransactionsRequest.getDomainId(),
            updateOrderTransactionsRequest.getTransType(),
            updateOrderTransactionsRequest.getMessage(),
            updateOrderTransactionsRequest.getUtcExpectedFulfillmentTimeRangesCSV(),
            updateOrderTransactionsRequest.getUtcConfirmedFulfillmentTimeRange(),
            updateOrderTransactionsRequest.getPayment(),
            updateOrderTransactionsRequest.getPaymentOption(),
            updateOrderTransactionsRequest.isAllowEmptyOrders(),
            updateOrderTransactionsRequest.getOrderTags(),
            updateOrderTransactionsRequest.getSalesReferenceId(),
            updateOrderTransactionsRequest.getPm(),
            updateOrderTransactionsRequest.getPurchaseReferenceId(),
            updateOrderTransactionsRequest.getTransferReferenceId());
        // Prevent an order from being edited out of all items, unless empty orders are allowed
        if (!updateOrderTransactionsRequest.isAllowEmptyOrders() && o.size() == 0) {
          throw new ServiceException("Order has no items with a quantity greater than zero");
        }
        // Persist the order and item updates
        if (useLocalPM) {
          tx = updateOrderTransactionsRequest.getPm().currentTransaction();
          tx.begin();
        }
        UpdatedOrder uo = updateOrder(o, updateOrderTransactionsRequest.getSource(), true, true,
            updateOrderTransactionsRequest.getUserId(),
            updateOrderTransactionsRequest.getPm());
        o = uo.order;

        List<IDemandItem> localItems = (List<IDemandItem>) o.getItems();
        o.setNumberOfItems(localItems.size());
        o = updateOrderTransactionsRequest.getPm().detachCopy(o);
        o.setItems((List<IDemandItem>) updateOrderTransactionsRequest.getPm()
            .detachCopyAll(localItems));
        if (useLocalPM) {
          tx.commit();
        }
      } catch (JDOObjectNotFoundException e) {
        xLogger
            .severe("Order with i {0} not found while re-ordering for kiosk {1}: {2}",
                updateOrderTransactionsRequest.getTrackingId(),
                updateOrderTransactionsRequest.getKioskId(), e.getMessage(), e);
        final Locale locale = SecurityUtils.getLocale();
        ResourceBundle messages = Resources.getBundle(locale);

        throw new ServiceException(
            messages.getString("order") + " " + updateOrderTransactionsRequest.getTrackingId() + " "
                + messages
                .getString("error.notfound"));
      } catch (Exception e) {
        xLogger.severe("Exception while re-ordering: {0}", e.getMessage(), e);
        throw new ServiceException(e);
      } finally {
        if (LockUtil.shouldReleaseLock(lockStatus) && !LockUtil
            .release(Constants.TX_O + updateOrderTransactionsRequest.getTrackingId())) {
          xLogger.warn("Unable to release lock for key {0}",
              Constants.TX_O + updateOrderTransactionsRequest
                  .getTrackingId());
        }
        if (useLocalPM) {
          if (tx != null && tx.isActive()) {
            tx.rollback();
          }
          updateOrderTransactionsRequest.getPm().close();
        }
      }
    } else {
      // First time order
      List<IDemandItem> demandList = new ArrayList<>(); // demand list
      if (updateOrderTransactionsRequest.getInventoryTransactions()
          != null && !updateOrderTransactionsRequest
          .getInventoryTransactions().isEmpty()) {
        // Get the transactions and demand items
        for (ITransaction trans : updateOrderTransactionsRequest.getInventoryTransactions()) {
          // Update timestamp, if needed
          if (trans.getTimestamp() == null) {
            trans.setTimestamp(now);
          }
          trans.setDomainId(updateOrderTransactionsRequest.getDomainId());
          // Update trans. type
          trans.setType(updateOrderTransactionsRequest.getTransType());
          // Get material
          Long materialId = trans.getMaterialId();
          IMaterial m = materialCatalogService.getMaterial(materialId);
          // Get inventory
          IInvntry inv = inventoryManagementService.getInventory(trans.getKioskId(), materialId);
          if (inv == null) {
            xLogger.warn(
                "Inv. for kiosk-material {0}-{1} in domain {2} is not available. Cannot process order.",
                trans.getKioskId(), materialId, updateOrderTransactionsRequest.getDomainId());
            throw new ServiceException("Material " + m.getName()
                + " is not available at this entity. Please contact administrator and ensure it is configured.");
          }
          try {
            validateHU(trans.getQuantity(), m.getMaterialId(), m.getName());
          } catch (LogiException e) {
            throw new ServiceException(e.getCode(), e.getMessage());
          }
          // Add to demand list
          demandList.add(getDemandItem(trans, m, inv)); // if transaction has batch, then a DemandItemBatch is also created within the demand item
        } // end while
      }
      // Make the updated objects persistent
      try {
        // Update demand items and orders, if necessary
        if (updateOrderTransactionsRequest.isCreateOrder()) {
          // Get tax info. if present (from kiosk)
          BigDecimal taxPercent = BigDecimal.ZERO;
          String currency = null;
          List<String> kioskTags = null;
          // Get kiosk
          if (updateOrderTransactionsRequest.getKioskId() != null) {
            IKiosk k = entitiesService.getKiosk(updateOrderTransactionsRequest.getKioskId(), false);
            taxPercent = k.getTax();
            currency = k.getCurrency();
            kioskTags = k.getTags();
          }
          // Create dummy order, so we can get order Id (created by system)
          o = JDOUtils.createInstance(IOrder.class);
          o = updateOrderTransactionsRequest.getPm().makePersistent(o);
          o = getDetached(o, updateOrderTransactionsRequest.getPm());
          // Persist the order and its items (via transaction, given demand items will also be updated)
          if (useLocalPM) {
            tx = updateOrderTransactionsRequest.getPm().currentTransaction();
            tx.begin();
          }
          // Update order data
          createOrder(o, updateOrderTransactionsRequest.getDomainId(),
              updateOrderTransactionsRequest.getKioskId(),
              updateOrderTransactionsRequest.getUserId(), demandList, taxPercent, currency,
              updateOrderTransactionsRequest.getServicingKiosk(),
              updateOrderTransactionsRequest.getLatitude(),
              updateOrderTransactionsRequest.getLongitude(),
              updateOrderTransactionsRequest.getGeoAccuracy(),
              updateOrderTransactionsRequest.getGeoErrorCode(),
              updateOrderTransactionsRequest.getUtcExpectedFulfillmentTimeRangesCSV(),
              updateOrderTransactionsRequest.getUtcConfirmedFulfillmentTimeRange(),
              updateOrderTransactionsRequest.getPayment(),
              updateOrderTransactionsRequest.getPaymentOption(), kioskTags,
              updateOrderTransactionsRequest.getOrderTags(),
              updateOrderTransactionsRequest.getOrderType(),
              updateOrderTransactionsRequest.getReferenceId());
          o.setNumberOfItems(demandList.size());
          o.setExpectedArrivalDate(updateOrderTransactionsRequest.getEta());
          o.setDueDate(updateOrderTransactionsRequest.getReqByDate());
          o.setSrc(updateOrderTransactionsRequest.getSource());
          DomainConfig dc = DomainConfig.getInstance(updateOrderTransactionsRequest.getDomainId());
          orderVisibilityAction.invoke(o, updateOrderTransactionsRequest.getDomainId());
          o = updateOrderTransactionsRequest.getPm().makePersistent(o);
          demandList = (List<IDemandItem>) updateOrderTransactionsRequest.getPm()
              .makePersistentAll(demandList);
          demandList = (List<IDemandItem>) updateOrderTransactionsRequest.getPm()
              .detachCopyAll(demandList);
          if (updateOrderTransactionsRequest.getIsSalesOrder()
              != null && updateOrderTransactionsRequest.getIsSalesOrder() && dc.getOrdersConfig()
              .allowSalesOrderAsConfirmed()) {
            o.setStatus(IOrder.CONFIRMED);
            if (dc.autoGI() && dc.getOrdersConfig().allocateStockOnConfirmation()) {
              boolean autoAssignStatus = dc.getOrdersConfig().autoAssignFirstMatStatus();
              for (IDemandItem d : demandList) {
                String tag = IInvAllocation.Type.ORDER.toString().concat(":")
                    .concat(String.valueOf(d.getOrderId()));
                try {
                  inventoryManagementService.allocateAutomatically(o.getServicingKiosk(), d.getMaterialId(),
                      IInvAllocation.Type.ORDER, String.valueOf(d.getOrderId()), tag,
                      d.getOriginalQuantity(), d.getUserId(), autoAssignStatus,
                      updateOrderTransactionsRequest.getPm());
                  d.setStatus(IOrder.CONFIRMED);
                } catch (InventoryAllocationException invException) {
                  xLogger.warn("Could not allocate fully to Order for k: {0}, m: {1}, q: {2}",
                      o.getServicingKiosk(), d.getMaterialId(), d.getOriginalQuantity(),
                      invException);
                }
              }
            }
          }
          IMessage iMessage = null;
          if (updateOrderTransactionsRequest.getMessage() != null && !updateOrderTransactionsRequest
              .getMessage().isEmpty()) {
            iMessage = addMessageToOrder(o.getOrderId(), o.getDomainId(),
                updateOrderTransactionsRequest.getMessage(),
                updateOrderTransactionsRequest.getUserId(),
                updateOrderTransactionsRequest.getPm());
          }
          addStatusHistory(o.getOrderId(), null, o.getStatus(), o.getDomainId(), iMessage,
              updateOrderTransactionsRequest.getUserId(),
              updateOrderTransactionsRequest.getPm());
          o = updateOrderTransactionsRequest.getPm().detachCopy(o);
          o.setItems(demandList);
          if (useLocalPM) {
            tx.commit();
          }
          // Generate event only if order is visible to both parties.
          if (o.isVisibleToCustomer() && o.isVisibleToVendor()) {
            generateEvent(updateOrderTransactionsRequest.getDomainId(), IEvent.CREATED, o, null,
                null);
          }
          if (BigUtil.notEqualsZero(updateOrderTransactionsRequest.getPayment())) {
            generateEvent(updateOrderTransactionsRequest.getDomainId(), IEvent.PAID, o, null, null);
          }
        } else {
          // Simply persist demand items (without order)
          updateOrderTransactionsRequest.getPm().makePersistentAll(demandList);
          // Get demand list
          items = new ArrayList<>();
          for (IDemandItem aDemandList : demandList) {
            items.add(aDemandList);
          }
        }
      } catch (Exception e) {
        xLogger.severe("Exception: {0}", e.getMessage(), e);
        throw new ServiceException(e);
      } finally {
        if (useLocalPM) {
          if (tx != null && tx.isActive()) {
            tx.rollback();
          }
          // Close the persistence manager
          updateOrderTransactionsRequest.getPm().close();
        }
      }
    }
    // Update kiosk activity timestamp
    updateEntityActivityTimestamps(o);
    return new OrderResults(items, null, o);
  }

  public OrderResults createOrder(UpdateOrderTransactionsModel updateOrderTransactionsRequest)
      throws ServiceException {
    xLogger.fine("Entering updateOrderTransactions");
    if (updateOrderTransactionsRequest.getDomainId() == null) {
      throw new ServiceException("Unknown domain");
    }
    boolean useLocalPM = updateOrderTransactionsRequest.getPm() == null;
    if (useLocalPM) {
      updateOrderTransactionsRequest.setPm(PMF.get().getPersistenceManager());
    }
    Date now = new Date(); // timestamp for transactions
    IOrder o = null;
    List<IDemandItem> items = null;
    javax.jdo.Transaction tx = null;
    // Check transaction availability
    if ((updateOrderTransactionsRequest.getInventoryTransactions() == null
        || updateOrderTransactionsRequest
        .getInventoryTransactions().isEmpty()) && !(updateOrderTransactionsRequest
        .isAllowEmptyOrders())) {
      throw new ServiceException("Transaction list cannot be empty");
    }
    List<IDemandItem> demandList = new ArrayList<>(); // demand list
    if (updateOrderTransactionsRequest.getInventoryTransactions()
        != null && !updateOrderTransactionsRequest
        .getInventoryTransactions().isEmpty()) {
      // Get the transactions and demand items
      for (ITransaction trans : updateOrderTransactionsRequest.getInventoryTransactions()) {
        // Update timestamp, if needed
        if (trans.getTimestamp() == null) {
          trans.setTimestamp(now);
        }
        trans.setDomainId(updateOrderTransactionsRequest.getDomainId());
        // Update trans. type
        trans.setType(updateOrderTransactionsRequest.getTransType());
        // Get material
        Long materialId = trans.getMaterialId();
        IMaterial m = materialCatalogService.getMaterial(materialId);
        // Get inventory
        IInvntry inv = inventoryManagementService.getInventory(trans.getKioskId(), materialId);
        if (inv == null) {
          xLogger.warn(
              "Inv. for kiosk-material {0}-{1} in domain {2} is not available. Cannot process order.",
              trans.getKioskId(), materialId, updateOrderTransactionsRequest.getDomainId());
          throw new ServiceException("Material " + m.getName()
              + " is not available at this entity. Please contact administrator and ensure it is configured.");
        }
        try {
          validateHU(trans.getQuantity(), m.getMaterialId(), m.getName());
        } catch (LogiException e) {
          throw new ServiceException(e.getCode(), e.getMessage());
        }
        // Add to demand list
        demandList.add(getDemandItem(trans, m,
            inv)); // if transaction has batch, then a DemandItemBatch is also created within the demand item
      } // end while
    }
    // Make the updated objects persistent
    try {
      // Update demand items and orders, if necessary
      if (updateOrderTransactionsRequest.isCreateOrder()) {
        // Get tax info. if present (from kiosk)
        BigDecimal taxPercent = BigDecimal.ZERO;
        String currency = null;
        List<String> kioskTags = null;
        // Get kiosk
        if (updateOrderTransactionsRequest.getKioskId() != null) {
          IKiosk k = entitiesService.getKiosk(updateOrderTransactionsRequest.getKioskId(), false);
          taxPercent = k.getTax();
          currency = k.getCurrency();
          kioskTags = k.getTags();
        }
        // Create dummy order, so we can get order Id (created by system)
        o = JDOUtils.createInstance(IOrder.class);
        o = updateOrderTransactionsRequest.getPm().makePersistent(o);
        o = getDetached(o, updateOrderTransactionsRequest.getPm());
        // Persist the order and its items (via transaction, given demand items will also be updated)
        if (useLocalPM) {
          tx = updateOrderTransactionsRequest.getPm().currentTransaction();
          tx.begin();
        }
        // Update order data
        createOrder(o, updateOrderTransactionsRequest.getDomainId(),
            updateOrderTransactionsRequest.getKioskId(),
            updateOrderTransactionsRequest.getUserId(), demandList, taxPercent, currency,
            updateOrderTransactionsRequest.getServicingKiosk(),
            updateOrderTransactionsRequest.getLatitude(),
            updateOrderTransactionsRequest.getLongitude(),
            updateOrderTransactionsRequest.getGeoAccuracy(),
            updateOrderTransactionsRequest.getGeoErrorCode(),
            updateOrderTransactionsRequest.getUtcExpectedFulfillmentTimeRangesCSV(),
            updateOrderTransactionsRequest.getUtcConfirmedFulfillmentTimeRange(),
            updateOrderTransactionsRequest.getPayment(),
            updateOrderTransactionsRequest.getPaymentOption(), kioskTags,
            updateOrderTransactionsRequest.getOrderTags(),
            updateOrderTransactionsRequest.getOrderType(),
            updateOrderTransactionsRequest.getReferenceId());
        o.setNumberOfItems(demandList.size());
        o.setExpectedArrivalDate(updateOrderTransactionsRequest.getEta());
        o.setDueDate(updateOrderTransactionsRequest.getReqByDate());
        o.setSrc(updateOrderTransactionsRequest.getSource());
        DomainConfig dc = DomainConfig.getInstance(updateOrderTransactionsRequest.getDomainId());
        orderVisibilityAction.invoke(o, updateOrderTransactionsRequest.getDomainId());
        o = updateOrderTransactionsRequest.getPm().makePersistent(o);
        demandList = (List<IDemandItem>) updateOrderTransactionsRequest.getPm()
            .makePersistentAll(demandList);
        demandList = (List<IDemandItem>) updateOrderTransactionsRequest.getPm()
            .detachCopyAll(demandList);
        if (updateOrderTransactionsRequest.getIsSalesOrder()
            != null && updateOrderTransactionsRequest.getIsSalesOrder() && dc.getOrdersConfig()
            .allowSalesOrderAsConfirmed()) {
          o.setStatus(IOrder.CONFIRMED);
          if (dc.autoGI() && dc.getOrdersConfig().allocateStockOnConfirmation()) {
            boolean autoAssignStatus = dc.getOrdersConfig().autoAssignFirstMatStatus();
            for (IDemandItem d : demandList) {
              String tag = IInvAllocation.Type.ORDER.toString().concat(":")
                  .concat(String.valueOf(d.getOrderId()));
              try {
                inventoryManagementService
                    .allocateAutomatically(o.getServicingKiosk(), d.getMaterialId(),
                        IInvAllocation.Type.ORDER, String.valueOf(d.getOrderId()), tag,
                        d.getOriginalQuantity(), d.getUserId(), autoAssignStatus,
                        updateOrderTransactionsRequest.getPm());
                d.setStatus(IOrder.CONFIRMED);
              } catch (InventoryAllocationException invException) {
                xLogger.warn("Could not allocate fully to Order for k: {0}, m: {1}, q: {2}",
                    o.getServicingKiosk(), d.getMaterialId(), d.getOriginalQuantity(),
                    invException);
              }
            }
          }
        }
        IMessage iMessage = null;
        if (updateOrderTransactionsRequest.getMessage() != null && !updateOrderTransactionsRequest
            .getMessage().isEmpty()) {
          iMessage = addMessageToOrder(o.getOrderId(), o.getDomainId(),
              updateOrderTransactionsRequest.getMessage(),
              updateOrderTransactionsRequest.getUserId(),
              updateOrderTransactionsRequest.getPm());
        }
        addStatusHistory(o.getOrderId(), null, o.getStatus(), o.getDomainId(), iMessage,
            updateOrderTransactionsRequest.getUserId(),
            updateOrderTransactionsRequest.getPm());
        o = updateOrderTransactionsRequest.getPm().detachCopy(o);
        o.setItems(demandList);
        if (useLocalPM) {
          tx.commit();
        }
        // Generate event only if order is visible to both parties.
        if (o.isVisibleToCustomer() && o.isVisibleToVendor()) {
          generateEvent(updateOrderTransactionsRequest.getDomainId(), IEvent.CREATED, o, null,
              null);
        }
        if (BigUtil.notEqualsZero(updateOrderTransactionsRequest.getPayment())) {
          generateEvent(updateOrderTransactionsRequest.getDomainId(), IEvent.PAID, o, null, null);
        }
      } else {
        // Simply persist demand items (without order)
        updateOrderTransactionsRequest.getPm().makePersistentAll(demandList);
        // Get demand list
        items = new ArrayList<>();
        for (IDemandItem aDemandList : demandList) {
          items.add(aDemandList);
        }
      }
    } catch (Exception e) {
      xLogger.severe("Exception: {0}", e.getMessage(), e);
      throw new ServiceException(e);
    } finally {
      if (useLocalPM) {
        if (tx != null && tx.isActive()) {
          tx.rollback();
        }
        // Close the persistence manager
        updateOrderTransactionsRequest.getPm().close();
      }
    }
    // Update kiosk activity timestamp
    updateEntityActivityTimestamps(o);
    return new OrderResults(items, null, o);
  }

  private void validateHU(BigDecimal quantity, Long mId, String mName) throws LogiException {
    Map<String, String> huData = handlingUnitService.getHandlingUnitDataByMaterialId(mId);
    if (huData != null) {
      if (BigUtil.notEqualsZero(quantity.remainder(new BigDecimal(huData.get("quantity"))))) {
        if (mName == null) {
          IMaterial m = materialCatalogService.getMaterial(mId);
          mName = m.getName();
        }
        throw new LogiException("T001", quantity.stripTrailingZeros().toPlainString(), mName,
            huData.get(IHandlingUnit.NAME), huData.get(IHandlingUnit.QUANTITY), mName);
      }
    }
  }

  // Get an order given a demand list
  private void createOrder(
      IOrder o, Long domainId, Long kioskId, String userId, List<IDemandItem> items,
      BigDecimal taxPercent, String currency, Long servicingKioskId, Double latitude,
      Double longitude,
      Double geoAccuracy, String geoErrorCode, String utcEstimatedFulfillmentTimeRangesCSV,
      String utcConfirmedFulfillmentTimeRange, BigDecimal payment, String paymentOption,
      List<String> kioskTags, List<String> orderTags, Integer orderType, String referenceId
  ) throws ServiceException {
    xLogger.fine("Entered createOrder");
    Date t;
    if (items != null && !items.isEmpty()) {
      t = items.iterator().next().getTimestamp();
    } else {
      t = new Date();
    }

    o.setKioskId(kioskId);
    try {
      servicingKioskId = TransactionUtil.getDefaultVendor(domainId, kioskId, servicingKioskId);
    } catch (ServiceException se) {
      xLogger.warn("{0} while getting default vendor for an order. Message: {1}",
          se.getClass().getName(), se.getMessage());
    }
    o.setServicingKiosk(servicingKioskId);
    o.setCreatedOn(t);
    o.setUpdatedOn(t);
    o.setStatus(IOrder.PENDING);
    if (currency != null) {
      o.setCurrency(currency);
    }
    o.setUserId(userId);
    o.setUpdatedBy(userId);
    o.setTax(taxPercent);
    if (latitude != null) {
      o.setLatitude(latitude);
    }
    if (longitude != null) {
      o.setLongitude(longitude);
    }
    if (geoAccuracy != null) {
      o.setGeoAccuracy(geoAccuracy);
    }
    if (geoErrorCode != null) {
      o.setGeoErrorCode(geoErrorCode);
    }
    // Set tags
    o.setTgs(tagDao.getTagsByNames(kioskTags, ITag.KIOSK_TAG), TagUtil.TYPE_ENTITY);
    o.setTgs(tagDao.getTagsByNames(orderTags, ITag.ORDER_TAG), TagUtil.TYPE_ORDER);
    o.setOrderType(orderType);

    // Update the demand items with order id and compute price
    if (items != null && !items.isEmpty()) {
      for (IDemandItem item : items) {
        item.updateOId(o);
      }
      o.setItems(items);
      o.setTotalPrice(o.computeTotalPrice());
      if (IOrder.SALES_ORDER == o.getOrderType()) {
        o.setSalesReferenceID(referenceId);
      } else if (IOrder.PURCHASE_ORDER == o.getOrderType()) {
        o.setPurchaseReferenceId(referenceId);
      } else if (IOrder.TRANSFER_ORDER == o.getOrderType()) {
        o.setTransferReferenceId(referenceId);
      }
    }
    o.setDomainId(domainId);
    updateOrderMetadata(o, utcEstimatedFulfillmentTimeRangesCSV, utcConfirmedFulfillmentTimeRange,
        payment, paymentOption);
  }

  @Override
  public void modifyOrder(IOrder o, String userId, List<ITransaction> transactions, Date timestamp,
                          Long domainId, String transType, String message,
                          String utcEstimatedFulfillmentTimeRanges,
                          String utcConfirmedFulfillmentTimeRange, BigDecimal payment,
                          String paymentOption, boolean allowEmptyOrders,
                          List<String> orderTags, String referenceId)
      throws ServiceException {
    modifyOrder(o, userId, transactions, timestamp, domainId, transType, message,
        utcEstimatedFulfillmentTimeRanges, utcConfirmedFulfillmentTimeRange, payment, paymentOption,
        allowEmptyOrders, orderTags, referenceId, null, null, null);
  }

  @Override
  // Modify order status and its items
  public void modifyOrder(IOrder o, String userId, List<ITransaction> transactions, Date timestamp,
                          Long domainId,
                          String transType, String message,
                          String utcEstimatedFulfillmentTimeRanges,
                          String utcConfirmedFulfillmentTimeRange,
                          BigDecimal payment, String paymentOption,
                          boolean allowEmptyOrders,
                          List<String> orderTags, String salesReferenceId,
                          PersistenceManager pm, String purchaseReferenceId,
                          String transferReferenceId) throws ServiceException {
    Date t = null;
    if (transactions != null && !transactions.isEmpty()) {
      for (ITransaction trans : transactions) {
        if (trans.getTimestamp() == null) {
          trans.setTimestamp(timestamp);
          t = timestamp;
        } else {
          t = trans.getTimestamp();
        }
        if (userId == null) {
          userId = trans.getSourceUserId();
        }
        trans.setDomainId(domainId);
        trans.setType(transType);

        try {
          validateHU(trans.getQuantity(), trans.getMaterialId(), null);
        } catch (LogiException e) {
          throw new ServiceException(e.getCode(), e.getMessage());
        }
        // Get the demand item
        IDemandItem item = o.getItem(trans.getMaterialId());
        if (item == null) {
          Long materialId = trans.getMaterialId();
          // A new item has to be added to the order
          try {
            item = getDemandItem(trans, materialCatalogService.getMaterial(trans.getMaterialId()),
                    inventoryManagementService.getInventory(trans.getKioskId(), materialId));
            if (item.getOrderId() == null) {
              item.updateOId(o);
            }
            // Add item to order
            ((List<IDemandItem>) o.getItems()).add(item);
          } catch (Exception e) {
            xLogger.warn(
                "{0} when getting material/inventory for a newly added item to order {1}: materialId = {2}, kioskId = {3}:",
                e.getClass().getName(), o.getOrderId(), materialId, trans.getKioskId(), e);
          }
        } else {
          // Update item data
          BigDecimal q = trans.getQuantity();
          if (BigUtil.lesserThanZero(q)) {
            xLogger.warn("Invalid quantity for re-order for material {0} in order {1}",
                trans.getMaterialId(), o.getIdString());
            continue; // go to next item
          } else {
            item.setQuantity(q);
          }
          if (trans.getReason() != null && !trans.getReason().isEmpty()) {
            item.setReason(trans.getReason());
          }
          item.setTimestamp(t);
          item.setUserId(trans.getSourceUserId());
          if (trans.getMessage() != null) {
            item.setMessage(trans.getMessage());
          }
          if (BigUtil.equalsZero(q)) {
            // Cancel this item
            item.setStatus(IOrder.CANCELLED);
          } else {
            // Set changed status
            item.setStatus(IOrder.CHANGED); // we will keep the CHANGED status at the item level
          }
          if (trans.getEditOrderQtyReason() != null && !trans.getEditOrderQtyReason().isEmpty()) {
            item.setShippedDiscrepancyReason(trans.getEditOrderQtyReason());
          }
        }
      }
    }
    // Iterate through demand items for the order. If all items have quantity 0 and if allowEmptyOrders is false then throw exception
    if (allItemsZeroQty(o) && !allowEmptyOrders) {
      throw new ServiceException("An order should have atleast one item");
    }

    // Recompute the order's price based on the above
    o.setTotalPrice(o.computeTotalPrice());
    o.setTgs(tagDao.getTagsByNames(orderTags, ITag.ORDER_TAG), TagUtil.TYPE_ORDER);
    DomainsUtil.addToDomain(o, o.getDomainId(), null);
    // Update other order metadata
    updateOrderMetadata(o, utcEstimatedFulfillmentTimeRanges, utcConfirmedFulfillmentTimeRange,
        payment, paymentOption);
    // Set timestamp and user
    if (t == null) {
      t = new Date();
    }
    o.setUpdatedOn(t);
    o.setUpdatedBy(userId);
    if (message != null) {
      conversationService.addMsgToConversation("ORDER", String.valueOf(o.getOrderId()), message, userId,
          Collections.singleton("ORDER:" + o.getOrderId())
          , o.getDomainId(), pm);
      generateOrderCommentEvent(domainId, IEvent.COMMENTED, JDOUtils.getImplClassName(IOrder.class),
          o.getOrderId().toString(), null, null);
    }
    setReferenceId(o, salesReferenceId, purchaseReferenceId, transferReferenceId);
  }

  private void setReferenceId(IOrder o, String salesReferenceId, String purchaseReferenceId,
                              String transferReferenceId) {
    o.setSalesReferenceID(salesReferenceId);
    if (IOrder.TRANSFER_ORDER != o.getOrderType()) {
      o.setPurchaseReferenceId(purchaseReferenceId);
    } else {
      o.setTransferReferenceId(transferReferenceId);
    }
  }

  // Update other order metadata (fulfillment times, payment options, package size, etc.)
  private void updateOrderMetadata(IOrder o, String utcEstimatedFulfillmentTimeRangesCSV,
                                   String utcConfirmedFulfillmentTimeRange, BigDecimal payment,
                                   String paymentOption) {
    // Update estimated fulfillment time ranges
    if (utcEstimatedFulfillmentTimeRangesCSV != null) {
      o.setExpectedFulfillmentTimeRangesCSV(utcEstimatedFulfillmentTimeRangesCSV);
    }
    // Update the order with fulfillment time ranges, payment options, package size, etc.
    if (utcConfirmedFulfillmentTimeRange != null) {
      o.setConfirmedFulfillmentTimeRange(utcConfirmedFulfillmentTimeRange);
    }
    // Add payment to order
    if (payment != null && BigUtil.notEqualsZero(payment)) {
      o.addPayment(payment);
      o.commitPayment(payment);
    }
    // Payment option
    if (paymentOption != null) {
      o.setPaymentOption(paymentOption);
    }

  }

  @Override
  public BigDecimal computeRecommendedOrderQuantity(IInvntry invntry) throws ServiceException {
    BigDecimal roq = new BigDecimal(-1);
    if (IInvntry.MODEL_SQ.equals(invntry.getInventoryModel())) {
      roq =
          BigUtil.lesserThanZero(invntry.getEconomicOrderQuantity()) ? BigDecimal.ZERO
              : invntry.getEconomicOrderQuantity();
    } else if (BigUtil.greaterThanZero(invntry.getMaxStock())) {
      BigDecimal usableStock = invntry.getStock().subtract(invntry.getExpiredStock());
      if (BigUtil
          .lesserThan(usableStock.add(invntry.getInTransitStock()), invntry.getMaxStock())) {
        roq =
            invntry.getMaxStock().subtract(usableStock)
                .subtract(invntry.getInTransitStock());
      } else {
        roq = BigDecimal.ZERO;
      }
    }
    return roq;
  }

  // Get a demand item, given a transaction
  private IDemandItem getDemandItem(ITransaction trans, IMaterial m, IInvntry inv) throws ServiceException {
    IDemandItem di = JDOUtils.createInstance(IDemandItem.class);
    di.setDomainId(trans.getDomainId());
    di.setKioskId(trans.getKioskId());
    di.setMaterialId(trans.getMaterialId());
    di.setDomainId(trans.getDomainId());
    di.addDomainIds(inv.getDomainIds());

    BigDecimal q = trans.getQuantity();
    di.setQuantity(q);
    if (BigUtil.equalsZero(
        di.getOriginalQuantity())) { // must be first-time order, set the original quantity
      di.setOriginalQuantity(q);
      //Update recommended order quantity
      di.setRecommendedOrderQuantity(computeRecommendedOrderQuantity(inv));
    }
    di.setReason(trans.getReason());

    di.setStatus(IOrder.PENDING);
    di.setCreatedOn(trans.getTimestamp());
    di.setTimestamp(trans.getTimestamp());
    di.setMessage(trans.getMessage());
    di.setCreatedBy(trans.getSourceUserId());
    di.setUserId(trans.getSourceUserId());
    // Set tags
    di.setTgs(tagDao.getTagsByNames(inv.getTags(TagUtil.TYPE_ENTITY), ITag.KIOSK_TAG),
        TagUtil.TYPE_ENTITY);
    di.setTgs(tagDao.getTagsByNames(inv.getTags(TagUtil.TYPE_MATERIAL), ITag.MATERIAL_TAG),
        TagUtil.TYPE_MATERIAL);
    // Add price metadata
    BigDecimal p = m.getRetailerPrice();
    if (BigUtil.notEqualsZero(inv.getRetailerPrice())) {
      p = inv.getRetailerPrice();
    }
    if (BigUtil.greaterThanZero(p)) {
      di.setUnitPrice(p);
      di.setCurrency(m.getCurrency());
    }
    // If inventory available, check/set tax rate
    di.setTax(inv.getTax());
    di.setTimeToOrder(inventoryManagementService.getDurationFromRP(inv.getKey()));
    di.setShippedDiscrepancyReason(trans.getEditOrderQtyReason());

    return di;
  }

  // Generate order events, if configured
  private void generateEvent(Long domainId, int eventId, IOrder o, String message,
      List<String> userIds) {
    generateOrderEventsAction.invoke(domainId, eventId, o.getOrderId(), o.getStatus(),
        message, userIds);
  }

  @Override
  public List<IDemandItem> getDemandItemByStatus(Long kioskId, Long materialId,
      Collection<String> status)
      throws ServiceException {
    if (kioskId == null && materialId == null || status == null) {
      throw new ServiceException("One of KioskId and MaterialId along with status are mandatory");
    }
    PersistenceManager pm = PMF.get().getPersistenceManager();
    Query q = pm.newQuery(JDOUtils.getImplClass(IDemandItem.class));
    Map<String, Object> paramMap = new HashMap<>(3);
    paramMap.put("ost", status);
    String filter = "ost.contains(st)";
    String declaration = "java.util.Collection ost";
    if (kioskId != null) {
      filter += " && kId == kIdParam";
      paramMap.put("kIdParam", kioskId);
      declaration += ", Long kIdParam";
    }
    if (materialId != null) {
      filter += " && mId == mIdParam";
      paramMap.put("mIdParam", materialId);
      declaration += ", Long mIdParam";
    }

    try {
      q.setFilter(filter);
      q.declareParameters(declaration);
      List<IDemandItem> di = (List<IDemandItem>) q.executeWithMap(paramMap);
      return (List<IDemandItem>) pm.detachCopyAll(di);
    } catch (Exception e) {
      xLogger.warn("Error while getting demand item by status for kioskID {0}, materialId {1}",
          kioskId, materialId, e);
    } finally {
      try {
        q.closeAll();
      } catch (Exception ignored) {
        //ignored
      }
      pm.close();
    }
    return Collections.emptyList();
  }

  private String buildReferenceIdFilterQuery(List<Object> parameters, Long domainId, String referenceId, String referenceIdSearchText) {

    String query = "(SELECT ID_OID FROM ORDER_DOMAINS WHERE DOMAIN_ID = ?) AND " + referenceId + " LIKE ?";
    parameters.add(domainId);
    parameters.add(referenceIdSearchText.concat("%"));
    return query;
  }


  public List<String> getIdSuggestions(Long domainId, String id, String type, Integer oty,
      List<Long> kioskIds) throws ServiceException {
    List<String> filterIds = new ArrayList<>();
    List<Object> parameters = new ArrayList<>();

    StringBuilder sqlQuery = new StringBuilder();
    if (StringUtils.isNotEmpty(type)) {
      if ("salesRefId".equals(type)) {
        sqlQuery.append("SELECT DISTINCT SALES_REF_ID FROM `ORDER` WHERE ID IN ")
            .append(buildReferenceIdFilterQuery(parameters, domainId, "SALES_REF_ID", id));
      } else if ("purchaseRefId".equals(type)) {
        sqlQuery.append("SELECT DISTINCT PURCHASE_REF_ID FROM `ORDER` WHERE ID IN ")
            .append(buildReferenceIdFilterQuery(parameters, domainId,"PURCHASE_REF_ID", id));
      } else if ("transferRefId".equals(type)) {
        sqlQuery.append("SELECT DISTINCT TRANSFER_REF_ID FROM `ORDER` WHERE ID IN ")
            .append(buildReferenceIdFilterQuery(parameters, domainId,"TRANSFER_REF_ID", id));
      } else if ("oid".equals(type)) {
        sqlQuery.append("SELECT ID FROM `ORDER` WHERE ID IN (SELECT ID_OID FROM ORDER_DOMAINS WHERE DOMAIN_ID = ? AND ID_OID LIKE ?)");
        parameters.add(domainId);
        parameters.add(id.concat("%"));
      }
    }
    if (oty != null) {
      if (oty == IOrder.PURCHASE_ORDER) {
        sqlQuery.append(" AND OTY IN (").append(IOrder.PURCHASE_ORDER)
            .append(CharacterConstants.COMMA).append(IOrder.SALES_ORDER).append(")");
      } else {
        sqlQuery.append(" AND OTY = ?");
        parameters.add(String.valueOf(oty));
      }
    }
    if (kioskIds != null && !kioskIds.isEmpty()) {
      List<Object> kidParams = new ArrayList<>(kioskIds.size());
      StringBuilder kids = new StringBuilder();
      for (Long kid : kioskIds) {
        kids.append(CharacterConstants.QUESTION).append(CharacterConstants.COMMA);
        kidParams.add(kid);
      }
      kids.setLength(kids.length() - 1);
      sqlQuery.append(" AND ((KID IN(")
          .append(kids.toString())
          .append(") AND VTC = 1) OR (SKID IN(")
          .append(kids.toString())
          .append(") AND VTV = 1))");
      parameters.addAll(kidParams); // for KID
      parameters.addAll(kidParams); // for SKID
    }
    sqlQuery.append(" LIMIT 0,8");
    PersistenceManager pm = PMF.get().getPersistenceManager();
    Query query = pm.newQuery(Constants.JAVAX_JDO_QUERY_SQL, sqlQuery.toString());
    try {
      List rs = (List) query.executeWithArray(parameters.toArray());
      for (Object r : rs) {
        String a = String.valueOf(r);
        if (a != null) {
          filterIds.add(a);
        }
      }
    } catch (Exception e) {
      xLogger.warn("Error in fetching id suggestions for domain:{0}", domainId, e);
    } finally {
      query.closeAll();
      pm.close();
    }
    return filterIds;
  }

  public BigDecimal getLeadTime(Long kid, Long mid, float orderPeriodicityInConfig,
      LeadTimeAvgConfig leadTimeAvgConfig, float leadTimeDefaultInConfig)
      throws ServiceException {
    BigDecimal avgLeadTime = BigDecimal.ZERO;
    if (kid == null || mid == null) {
      xLogger.warn("Either Kiosk ID or material ID is null, kid: {0}, mid: {1}", kid, mid);
      return avgLeadTime;
    }
    float maxOrderPeriods = LeadTimeAvgConfig.MAX_ORDER_PERIODS_DEFAULT;
    int minNumberOfOrders = LeadTimeAvgConfig.MINIMUM_NUMBER_OF_ORDERS_DEFAULT;
    int maxNumberOfOrders = LeadTimeAvgConfig.MAXIMUM_NUMBER_OF_ORDERS_DEFAULT;
    boolean excludeProcessingTime = false;
    if (leadTimeAvgConfig != null) {
      maxOrderPeriods = leadTimeAvgConfig.getMaxOrderPeriods();
      maxNumberOfOrders = leadTimeAvgConfig.getMaxNumOfOrders();
      minNumberOfOrders = leadTimeAvgConfig.getMinNumOfOrders();
      excludeProcessingTime = leadTimeAvgConfig.getExcludeOrderProcTime();
    }

    Results results = entitiesService.getLinkedKiosks(kid, IKioskLink.TYPE_VENDOR, null, null);
    boolean kskHasMoreThanOneVnd = false;
    if (results.getResults().size() > 1) {
      kskHasMoreThanOneVnd = true;
    }
    IInvntry inv = inventoryManagementService.getInventory(kid, mid);
    BigDecimal orderPeriodicity = inv.getOrderPeriodicity();
    if (BigUtil.equalsZero(orderPeriodicity)) {
      orderPeriodicity = BigDecimal.valueOf(orderPeriodicityInConfig);
    }
    int
        maxHistoricalPeriod =
        orderPeriodicity.multiply(BigDecimal.valueOf(maxOrderPeriods)).intValue();
    List<String> parameters = new ArrayList<>(1);
    StringBuilder sqlQuery = new StringBuilder("SELECT AVG(DLT_ALIAS)");
    if (!excludeProcessingTime) {
      sqlQuery.append(" + AVG(PT_ALIAS)");
    }
    sqlQuery.append(", COUNT(1) FROM (");
    sqlQuery.append("SELECT DLT DLT_ALIAS");
    if (!excludeProcessingTime) {
      sqlQuery.append(", PT PT_ALIAS");
    }
    sqlQuery.append(" FROM `ORDER` WHERE ");
    if (kskHasMoreThanOneVnd) {
      sqlQuery.append("ID IN (SELECT DISTINCT OID FROM DEMANDITEM WHERE KID = ?");
      parameters.add(String.valueOf(kid));
      sqlQuery.append(" AND MID = ?)");
      parameters.add(String.valueOf(mid));
    } else {
      sqlQuery.append("KID = ?");
      parameters.add(String.valueOf(kid));
    }
    sqlQuery.append(" AND ST = ?");
    parameters.add(IOrder.FULFILLED);

    sqlQuery.append(" AND UON >= (DATE_SUB(NOW(),INTERVAL ?")
        .append(" DAY))").append(" ORDER BY UON DESC LIMIT 0,?");
    parameters.add(String.valueOf(maxHistoricalPeriod));
    parameters.add(String.valueOf(maxNumberOfOrders));
    sqlQuery.append(") ALIAS");
    PersistenceManager pm = PMF.get().getPersistenceManager();
    Query query = pm.newQuery(Constants.JAVAX_JDO_QUERY_SQL, sqlQuery.toString());
    try {
      List queryResults = (List) query.executeWithArray(parameters.toArray());
      if (queryResults != null && !queryResults.isEmpty()) {
        for (Object queryResult : queryResults) {
          Object[] resultsArray = (Object[]) queryResult;
          avgLeadTime = (BigDecimal) resultsArray[0];
          long numberOfOrders = (long) resultsArray[1];
          if (numberOfOrders < minNumberOfOrders) {
            avgLeadTime = BigDecimal.valueOf(leadTimeDefaultInConfig);
          }
        }
      }
    } catch (Exception e) {
      xLogger.warn("Error while calculating average lead time for kid: {0}, mid: {1}", kid, mid, e);
    } finally {
      query.closeAll();
      pm.close();
    }
    return avgLeadTime;
  }

  @Override
  public void updateOrderMetadata(Long orderId, String updatedBy, PersistenceManager pm) {
    updateOrderMetadata(orderId, updatedBy, pm, null, null, false);
  }

  @Override
  public void updateOrderMetadata(Long orderId, String updatedBy,
                                  PersistenceManager persistenceManager, String salesReferenceId,
                                  Date expectedArrivalDate, boolean updateOrderFields) {
    Boolean isLocalPersistentManager = Boolean.FALSE;
    if (persistenceManager == null) {
      persistenceManager = PMF.get().getPersistenceManager();
      isLocalPersistentManager = Boolean.TRUE;
    }
    IOrder order = JDOUtils.getObjectById(IOrder.class, orderId, persistenceManager);
    order.setUpdatedBy(updatedBy);
    order.setUpdatedOn(new Date());
    if(updateOrderFields) {
      order.setSalesReferenceID(salesReferenceId);
      order.setExpectedArrivalDate(expectedArrivalDate);
    }

    if (isLocalPersistentManager) {
      persistenceManager.close();
    }
  }

  private void updateEntityActivityTimestamps(IOrder o) {
    // Modify the active time stamps of the entities in the order.
    Set<Long> kids = new HashSet<>(1);
    kids.add(o.getKioskId());
    if (o.getServicingKiosk() != null) {
      kids.add(o.getServicingKiosk());
    }
    for (Long kid : kids) {
      Map<String, String> params = new HashMap<>(3);
      try {
        params.put("entityId", String.valueOf(kid));
        params.put("timestamp", String.valueOf(o.getCreatedOn().getTime()));
        params.put("actType", String.valueOf(IKiosk.TYPE_ORDERACTIVITY));
        getTaskService()
            .schedule(ITaskService.QUEUE_DEFAULT, UPDATE_ENTITYACTIVITYTIMESTAMPS_TASK, params,
                ITaskService.METHOD_POST);
      } catch (TaskSchedulingException e) {
        xLogger.warn(
            "Error while scheduling update entity activity timestamp for entityId {0} in order {1}",
            o.getKioskId(), o.getOrderId(), e);
      }
    }
  }

  private boolean allItemsZeroQty(IOrder o) {
    List<IDemandItem> its = (List<IDemandItem>) o.getItems();
    boolean allQtyZero = true;
    if (its != null) {
      for (IDemandItem it : its) {
        if (BigUtil.notEqualsZero(it.getQuantity())) {
          allQtyZero = false;
          break;
        }
      }
    }
    return allQtyZero;
  }

  @Override
  public PDFResponseModel generateInvoiceForOrder(Long orderId)
      throws ServiceException, IOException {
    IOrder order = getOrder(orderId, true);
    SecureUserDetails user = SecurityUtils.getUserDetails();
    return generateOrderInvoiceAction.invoke(order, user);
  }

}
