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

package com.logistimo.api.controllers;

import com.google.gson.internal.LinkedTreeMap;

import com.logistimo.AppFactory;
import com.logistimo.api.builders.DemandBuilder;
import com.logistimo.api.builders.OrdersAPIBuilder;
import com.logistimo.api.models.DemandItemBatchModel;
import com.logistimo.api.models.DemandModel;
import com.logistimo.api.models.OrderMaterialsModel;
import com.logistimo.api.models.OrderModel;
import com.logistimo.api.models.OrderResponseModel;
import com.logistimo.api.models.OrderStatusModel;
import com.logistimo.api.models.OrderUpdateModel;
import com.logistimo.api.models.PaymentModel;
import com.logistimo.api.models.UserContactModel;
import com.logistimo.api.util.DedupUtil;
import com.logistimo.api.util.ResponseUtils;
import com.logistimo.auth.SecurityConstants;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.config.models.EventsConfig;
import com.logistimo.config.models.OrdersConfig;
import com.logistimo.constants.CharacterConstants;
import com.logistimo.constants.Constants;
import com.logistimo.constants.SourceConstants;
import com.logistimo.dao.JDOUtils;
import com.logistimo.deliveryrequest.models.DeliveryRequestModel;
import com.logistimo.entities.service.EntitiesService;
import com.logistimo.events.generators.EventGeneratorFactory;
import com.logistimo.events.generators.OrdersEventGenerator;
import com.logistimo.exception.BadRequestException;
import com.logistimo.exception.ForbiddenAccessException;
import com.logistimo.exception.InvalidDataException;
import com.logistimo.exception.InvalidServiceException;
import com.logistimo.exception.LogiException;
import com.logistimo.exception.ValidationException;
import com.logistimo.inventory.entity.IInvAllocation;
import com.logistimo.inventory.entity.ITransaction;
import com.logistimo.inventory.exceptions.InventoryAllocationException;
import com.logistimo.inventory.service.InventoryManagementService;
import com.logistimo.logger.XLog;
import com.logistimo.models.shipments.ShipmentItemBatchModel;
import com.logistimo.orders.OrderResults;
import com.logistimo.orders.OrderUtils;
import com.logistimo.orders.actions.OrderAutomationAction;
import com.logistimo.orders.actions.ScheduleOrderAutomationAction;
import com.logistimo.orders.approvals.service.IOrderApprovalsService;
import com.logistimo.orders.entity.IDemandItem;
import com.logistimo.orders.entity.IOrder;
import com.logistimo.orders.exception.AllocationNotCompleteException;
import com.logistimo.orders.models.PDFResponseModel;
import com.logistimo.orders.models.ShipNowRequest;
import com.logistimo.orders.models.UpdateOrderTransactionsModel;
import com.logistimo.orders.models.UpdatedOrder;
import com.logistimo.orders.service.IDemandService;
import com.logistimo.orders.service.OrderManagementService;
import com.logistimo.pagination.Navigator;
import com.logistimo.pagination.PageParams;
import com.logistimo.pagination.Results;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.ObjectNotFoundException;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;
import com.logistimo.services.cache.MemcacheService;
import com.logistimo.services.impl.PMF;
import com.logistimo.shipments.entity.IShipment;
import com.logistimo.shipments.service.IShipmentService;
import com.logistimo.tags.TagUtil;
import com.logistimo.tags.dao.ITagDao;
import com.logistimo.tags.entity.ITag;
import com.logistimo.utils.BigUtil;
import com.logistimo.utils.LocalDateUtil;
import com.logistimo.utils.LockUtil;
import com.logistimo.utils.MsgUtil;
import com.logistimo.utils.StringUtil;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.jdo.PersistenceManager;
import javax.jdo.Transaction;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/orders")
public class OrdersController {

  private static final XLog xLogger = XLog.getLog(OrdersController.class);
  private static final String CACHE_KEY = "order";

  private OrdersAPIBuilder orderAPIBuilder;

  private DemandBuilder demandBuilder;

  private IOrderApprovalsService orderApprovalsService;

  private OrderAutomationAction orderAutomationAction;

  private ScheduleOrderAutomationAction scheduleOrderAutomation;

  private IShipmentService shipmentService;

  private OrderManagementService orderManagementService;

  private InventoryManagementService inventoryManagementService;

  private IDemandService demandService;

  private EntitiesService entitiesService;

  private ITagDao tagDao;

  @Autowired
  public void setOrderAPIBuilder(OrdersAPIBuilder orderAPIBuilder) {
    this.orderAPIBuilder = orderAPIBuilder;
  }

  @Autowired
  public void setDemandBuilder(DemandBuilder demandBuilder) {
    this.demandBuilder = demandBuilder;
  }

  @Autowired
  public void setOrderApprovalsService(
      IOrderApprovalsService orderApprovalsService) {
    this.orderApprovalsService = orderApprovalsService;
  }

  @Autowired
  public void setOrderAutomationAction(
      OrderAutomationAction orderAutomationAction) {
    this.orderAutomationAction = orderAutomationAction;
  }

  @Autowired
  public OrdersController setScheduleOrderAutomation(
      ScheduleOrderAutomationAction scheduleOrderAutomation) {
    this.scheduleOrderAutomation = scheduleOrderAutomation;
    return this;
  }

  @Autowired
  public void setShipmentService(IShipmentService shipmentService) {
    this.shipmentService = shipmentService;
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
  public void setEntitiesService(EntitiesService entitiesService) {
    this.entitiesService = entitiesService;
  }

  @Autowired
  public void setTagDao(ITagDao tagDao) {
    this.tagDao = tagDao;
  }

  @RequestMapping("/order/{orderId}")
  public
  @ResponseBody
  OrderModel getOrder(@PathVariable Long orderId,
                      @RequestParam(required = false, value = "embed") String[] embed) throws Exception {
    OrderModel model;
    Long domainId = SecurityUtils.getCurrentDomainId();
    IOrder order = orderManagementService.getOrder(orderId);
    if (order == null || domainId == null) {
      throw new InvalidServiceException(CharacterConstants.EMPTY);
    }
    List<Long> domainIds = order.getDomainIds();
    if (domainIds != null && !domainIds.contains(domainId)) {
      throw new ForbiddenAccessException("Forbidden");
    }
    model = orderAPIBuilder.buildFullOrderModel(order, domainId, embed);
    return model;

  }

  @RequestMapping(value = "/order/{orderId}/approvers", method = RequestMethod.GET)
  public
  @ResponseBody
  List<UserContactModel> getPrimaryApprovers(@PathVariable Long orderId)
      throws ServiceException, ObjectNotFoundException {
    IOrder order = orderManagementService.getOrder(orderId);
    return orderAPIBuilder.buildPrimaryApprovers(order, orderApprovalsService.getApprovalType(order));
  }

  @RequestMapping("/")
  public
  @ResponseBody
  Results getDomainOrders(@RequestParam(defaultValue = PageParams.DEFAULT_OFFSET_STR) int offset,
                          @RequestParam(defaultValue = PageParams.DEFAULT_SIZE_STR) int size,
                          @RequestParam(required = false) String status,
                          @RequestParam(required = false) String from,
                          @RequestParam(required = false) String until,
                          @RequestParam(required = false) String otype,
                          @RequestParam(required = false) String tgType,
                          @RequestParam(required = false) String tag,
                          @RequestParam(required = false) Integer oty,
                          @RequestParam(required = false) String salesRefId,
                          @RequestParam(required = false) String approval_status,
                          @RequestParam(required = false) String purchaseRefId,
                          @RequestParam(required = false) String transferRefId,
                          HttpServletRequest request) {
    return getOrders(null, offset, size, status, from, until, otype, tgType, tag, oty, salesRefId,
        approval_status, purchaseRefId, transferRefId, null, request);
  }

  @RequestMapping("/entity/{entityId}")
  public
  @ResponseBody
  Results getEntityOrders(@PathVariable Long entityId,
                          @RequestParam(defaultValue = PageParams.DEFAULT_OFFSET_STR) int offset,
                          @RequestParam(defaultValue = PageParams.DEFAULT_SIZE_STR) int size,
                          @RequestParam(required = false) String status,
                          @RequestParam(required = false) String from,
                          @RequestParam(required = false) String until,
                          @RequestParam(defaultValue = IOrder.TYPE_SALE) String otype,
                          @RequestParam(required = false) String tgType,
                          @RequestParam(required = false) String tag,
                          @RequestParam(required = false) Integer oty,
                          @RequestParam(required = false) String salesRefId,
                          @RequestParam(required = false) String approval_status,
                          @RequestParam(required = false) String purchaseRefId,
                          @RequestParam(required = false) String transferRefId,
                          @RequestParam(required = false) Long linkedKioskId,
                          HttpServletRequest request) {
    return getOrders(entityId, offset, size, status, from, until, otype, tgType, tag, oty,
        salesRefId,
        approval_status, purchaseRefId, transferRefId, linkedKioskId, request);
  }


  @RequestMapping(value = "/order/{orderId}/vendor", method = RequestMethod.POST)
  public
  @ResponseBody
  OrderResponseModel updateVendor(@PathVariable Long orderId, @RequestBody OrderUpdateModel model,
                                  HttpServletRequest request) {
    return updateOrder("vend", orderId, model.orderUpdatedAt, null, Long.valueOf(model.updateValue),
        null, null, null, request);
  }

  @RequestMapping(value = "/order/{orderId}/invoice", method = RequestMethod.GET)
  public
  @ResponseBody
  void generateInvoice(@PathVariable Long orderId, HttpServletResponse response)
      throws ServiceException, IOException, ValidationException, ObjectNotFoundException {
    PDFResponseModel invoiceModel = orderManagementService.generateInvoiceForOrder(orderId);
    ResponseUtils.serveInlineFile(response, invoiceModel.getFileName(), "application/pdf",
        invoiceModel.getBytes());
  }

  @RequestMapping(value = "/order/{orderId}/transporter", method = RequestMethod.POST)
  public
  @ResponseBody
  OrderResponseModel updateTransporter(@PathVariable Long orderId,
                                       @RequestBody OrderUpdateModel model,
                                       HttpServletRequest request) {
    return updateOrder("trans", orderId, model.orderUpdatedAt, null, null, null,
        null, null, request);
  }

  @RequestMapping(value = "/order/{orderId}/status", method = RequestMethod.POST)
  public
  @ResponseBody
  OrderResponseModel updateStatus(@PathVariable Long orderId, @RequestBody OrderStatusModel status,
                                  HttpServletRequest request) {
    SecureUserDetails user = SecurityUtils.getUserDetails();
    Locale locale = user.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    Long domainId = SecurityUtils.getCurrentDomainId();
    try {
      UpdatedOrder updOrder;
      IOrder o = orderManagementService.getOrder(orderId, true);
      if (status.orderUpdatedAt != null && !status.orderUpdatedAt
          .equals(LocalDateUtil.formatCustom(o.getUpdatedOn(), Constants.DATETIME_FORMAT, null))) {
        throw new LogiException("O004", o.getUpdatedBy(),
            LocalDateUtil.format(o.getUpdatedOn(), user.getLocale(), user.getTimezone()));
      }
      if (IOrder.COMPLETED.equals(status.st) || IOrder.FULFILLED.equals(status.st)) {
        List<IShipment> shipments = shipmentService.getShipmentsByOrderId(orderId);
        String shipmentId;
        if (status.st.equals(IOrder.COMPLETED)) {
          if (shipments != null && !shipments.isEmpty()) {
            xLogger.warn("Order already has shipments, hence cannot be shipped", orderId);
            throw new BadRequestException(
                backendMessages.getString("error.unabletoshiporder.shipments"));
          }
          if (o.isStatus(IOrder.COMPLETED) || o.isStatus(IOrder.CANCELLED) ||
              o.isStatus(IOrder.FULFILLED)) {
            xLogger.warn("Invalid order {0} status {1} cannot ship ", orderId, o.getStatus());
            throw new BadRequestException(backendMessages.getString("error.unabletoshiporder"));
          }
        }
        Date efd = null;
        if(StringUtils.isNotEmpty(status.efd)) {
          SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT);
          efd = sdf.parse(status.efd);
        }
        if (!o.isStatus(IOrder.COMPLETED) && !o.isStatus(IOrder.CANCELLED) && !o
            .isStatus(IOrder.FULFILLED)) {
          ShipNowRequest shipOrderData = populateShipOrderDataWrapper(status);
          shipmentId = orderManagementService.shipNow(o, user.getUsername(), shipOrderData,
                  SourceConstants.WEB, true, efd);
        } else if (o.isStatus(IOrder.COMPLETED)) {
          if (shipments == null || shipments.size() > 1) {
            xLogger.warn("Invalid order {0} ({1}) cannot fulfill, already has more shipments or " +
                "no shipments ", orderId, o.getStatus());
            throw new BadRequestException(backendMessages.getString("error.unabletofulfilorder"));
          }
          shipmentId = shipments.get(0).getShipmentId();
        } else {
          xLogger.warn("Invalid order {0} status ({1}) cannot fulfill ", orderId, o.getStatus());
          throw new BadRequestException(backendMessages.getString("error.unabletofulfilorder"));
        }

        if (IOrder.FULFILLED.equals(status.st)) {
          if (shipmentId != null) {
            shipmentService.fulfillShipment(shipmentId, user.getUsername(),
                CharacterConstants.EMPTY, SourceConstants.WEB);
          } else {
            xLogger.warn("Invalid order {0} status ({1}) cannot fulfill ", orderId, o.getStatus());
            throw new BadRequestException(backendMessages.getString("error.unabletofulfilorder"));
          }
        }

        //Messages added to order, anyone using ship now will not be using shipments.
        if (status.msg != null) {
          orderManagementService.addMessageToOrder(orderId, status.msg, user.getUsername());
        }
        o = orderManagementService.getOrder(orderId);
        updOrder = new UpdatedOrder(o);
      } else {
        updOrder = orderManagementService.updateOrderStatus(orderId, status.st,
            user.getUsername(), status.msg, status.users,
            SourceConstants.WEB, null, status.cdrsn);
      }
      return orderAPIBuilder.buildOrderResponseModel(updOrder, true, domainId, true,
          OrdersAPIBuilder.DEFAULT_EMBED);
    } catch (AllocationNotCompleteException ie) {
      xLogger.warn("Items are not allocated for order {0}, order status update requested by {1}",
          orderId, user.getUsername());
      throw new InvalidServiceException(ie);
    } catch (ServiceException ie) {
      xLogger.severe("Error in updating order status", ie);
      if (ie.getCode() != null) {
        throw new ValidationException(ie.getCode(), ie.getArguments());
      } else {
        throw new InvalidServiceException(backendMessages.getString("order.status.update.error"));
      }
    } catch (BadRequestException e) {
      throw e;
    } catch (LogiException le) {
      xLogger.severe("Failed to update order status", le);
      throw new InvalidServiceException(le.getMessage());
    } catch (Exception e) {
      xLogger.severe("Failed to update order status", e);
      throw new InvalidServiceException(backendMessages.getString("order.status.update.error"));
    }
  }

  private ShipNowRequest populateShipOrderDataWrapper(OrderStatusModel status) {
    return ShipNowRequest.builder()
                .consignment(status.consignment)
                .trackingId(status.tid)
                .reason(status.cdrsn)
                .transporterId(status.transporterId)
                .transporter(status.t)
                .packageSize(status.ps)
                .salesRefId(status.salesRefId)
                .vehicle(status.vehicle)
                .isCustomerPickup(status.customerPickup)
                .phoneNum(status.phone).build();
  }


  @RequestMapping(value = "/order/{orderId}/payment", method = RequestMethod.POST)
  public
  @ResponseBody
  OrderResponseModel updatePayment(@PathVariable Long orderId,
                                   @RequestBody PaymentModel paymentDetails,
                                   HttpServletRequest request) {
    return updateOrder("pmt", orderId, paymentDetails.orderUpdatedAt, paymentDetails, null, null,
        null, null, request);
  }

  @RequestMapping(value = "/order/{orderId}/fulfillmenttime", method = RequestMethod.POST)
  public
  @ResponseBody
  OrderResponseModel updateFulfillmentTime(@PathVariable Long orderId,
                                           @RequestBody OrderUpdateModel model,
                                           HttpServletRequest request) {
    return updateOrder("cft", orderId, model.orderUpdatedAt, null, null, model.updateValue,
        null, null, request);
  }

  @RequestMapping(value = "/order/{orderId}/efd", method = RequestMethod.POST)
  public
  @ResponseBody
  OrderResponseModel updateExpectedFulfillmentDate(@PathVariable Long orderId,
                                                   @RequestBody OrderUpdateModel model,
                                                   HttpServletRequest request) {
    return updateOrder("efd", orderId, model.orderUpdatedAt, null, null, model.updateValue,
        null, null, request);
  }

  @RequestMapping(value = "/order/{orderId}/edd", method = RequestMethod.POST)
  public
  @ResponseBody
  OrderResponseModel updateDueDate(@PathVariable Long orderId, @RequestBody OrderUpdateModel model,
                                   HttpServletRequest request) {
    return updateOrder("edd", orderId, model.orderUpdatedAt, null, null, model.updateValue,
        null, null, request);
  }

  @RequestMapping(value = "/order/{orderId}/statusJSON", method = RequestMethod.GET)
  public
  @ResponseBody
  String getOrderStatusJSON(@PathVariable Long orderId, HttpServletRequest request) {
    SecureUserDetails user = SecurityUtils.getUserDetails();
    Locale locale = user.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    Long domainId = SecurityUtils.getCurrentDomainId();
    try {
      IOrder order = orderManagementService.getOrder(orderId);
      List<String> excludeVars = new ArrayList<>(1);
      excludeVars.add(EventsConfig.VAR_ORDERSTATUS);
      OrdersEventGenerator eg = (OrdersEventGenerator) EventGeneratorFactory
          .getEventGenerator(domainId, JDOUtils.getImplClassName(IOrder.class));
      return eg.getOrderStatusJSON(order, user.getLocale(), user.getTimezone(), excludeVars);
    } catch (ServiceException | ObjectNotFoundException e) {
      xLogger.severe("Error in fetching order status for order {0}", orderId, e);
      throw new InvalidServiceException(backendMessages.getString("order.status.fetch.error"));
    }
  }

  @RequestMapping(value = "/order/{orderId}/items", method = RequestMethod.POST)
  public
  @ResponseBody
  OrderResponseModel updateDemandItems(@PathVariable Long orderId,
                                       @RequestBody OrderMaterialsModel model,
                                       HttpServletRequest request) {
    SecureUserDetails user = SecurityUtils.getUserDetails();
    Locale locale = user.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    Long domainId = SecurityUtils.getCurrentDomainId();
    LockUtil.LockStatus lockStatus = LockUtil.lock(Constants.TX_O + orderId);
    if (!LockUtil.isLocked(lockStatus)) {
      throw new InvalidServiceException(new ServiceException("O002", orderId));
    }
    PersistenceManager pm = null;
    Transaction tx = null;
    try {
      pm = PMF.get().getPersistenceManager();
      tx = pm.currentTransaction();
      tx.begin();
      IOrder order = orderManagementService.getOrder(orderId, false, pm);
      if (order == null) {
        throw new BadRequestException(backendMessages.getString("order.none") + " " + orderId);
      }
      if (model.orderUpdatedAt != null && !model.orderUpdatedAt.equals(
          LocalDateUtil.formatCustom(order.getUpdatedOn(), Constants.DATETIME_FORMAT, null))) {
        throw new LogiException("O004", order.getUpdatedBy(),
            LocalDateUtil.format(order.getUpdatedOn(), user.getLocale(), user.getTimezone()));
      }
      if (IOrder.ITEMS_UNEDITABLE_STATUSES.contains(order.getStatus())) {
        xLogger.warn("User {0} tried to edit materials of {1} order {2}", user, order.getStatus(),
            orderId);
        throw new BadRequestException("O003", orderId,
            OrderUtils.getStatusDisplay(order.getStatus(), locale));
      }
      DomainConfig dc = DomainConfig.getInstance(domainId);
      order.setItems(demandService.getDemandItems(orderId));
      Date now = new Date();
      List<ITransaction> transactions = orderAPIBuilder.buildTransactionsForNewItems(order, model.items, now, user.getUsername());
      if (transactions != null && !transactions.isEmpty()) {
        orderManagementService.
            modifyOrder(order, user.getUsername(), transactions, new Date(), domainId,
                ITransaction.TYPE_REORDER, model.msg, null, null, BigDecimal.ZERO, null,
                dc.allowEmptyOrders(), order.getTags(TagUtil.TYPE_ORDER),
                order.getSalesReferenceID(), pm, order.getPurchaseReferenceId(), order.getTransferReferenceId());
      }
      order = orderAPIBuilder.buildOrderMaterials(order, model.items, now, user.getUsername());
      //TODO use OrderManagementServiceImpl updateOrderWithAllocations
      String oIdStr = String.valueOf(order.getOrderId());
      if (dc.autoGI() && order.getServicingKiosk() != null) {
        String tag = IInvAllocation.Type.ORDER.toString() + CharacterConstants.COLON + oIdStr;
        for (DemandModel item : model.items) {
          List<ShipmentItemBatchModel> batchDetails = null;
          List<IInvAllocation> invAllocations = inventoryManagementService
              .getAllocationsByTagMaterial(item.id, tag);
          BigDecimal totalShipmentAllocation = BigDecimal.ZERO;
          for (IInvAllocation invAllocation : invAllocations) {
            if (IInvAllocation.Type.SHIPMENT.toString().equals(invAllocation.getType())) {
              totalShipmentAllocation = totalShipmentAllocation
                  .add(invAllocation.getQuantity());
            }
          }
          IDemandItem demandItem = order.getItem(item.id);
          if (item.astk != null && BigUtil.greaterThan(item.astk, item.q.subtract(
              demandItem.getShippedQuantity().add(totalShipmentAllocation)))) {
            throw new ServiceException(backendMessages.getString("allocated.qty.greater"));
          }
          if (item.isBa) {
            item.astk = null;
            if (item.bts != null) {
              batchDetails = new ArrayList<>(item.bts.size());
              for (DemandItemBatchModel bt : item.bts) {
                ShipmentItemBatchModel details = new ShipmentItemBatchModel();
                details.id = bt.id;
                details.q = bt.q;
                details.smst = bt.mst;
                batchDetails.add(details);
              }
            }
          }
          if (item.astk != null || batchDetails != null) {
            inventoryManagementService.allocate(order.getServicingKiosk(), item.id,
                IInvAllocation.Type.ORDER, oIdStr, tag,
                item.astk, batchDetails, user.getUsername(), pm, item.isBa ? null : item.mst);
          } else {
            inventoryManagementService.clearAllocation(order.getServicingKiosk(), item.id,
                IInvAllocation.Type.ORDER, String.valueOf(orderId), pm);
          }
        }
      }
      UpdatedOrder updorder = orderManagementService
          .updateOrder(order, SourceConstants.WEB, true, true, user.getUsername(), pm);
      tx.commit();
      return orderAPIBuilder.buildOrderResponseModel(updorder, true, domainId, true,
          OrdersAPIBuilder.DEFAULT_EMBED);
    } catch (InventoryAllocationException ie) {
      xLogger.severe("Error in updating demand items for order {0}", orderId, ie);
      if (ie.getCode() != null) {
        throw new InvalidDataException(ie.getMessage());
      } else {
        throw new InvalidServiceException(backendMessages.getString("demand.items.update.error"));
      }
    } catch (LogiException e) {
      xLogger.severe("Error in updating demand items for order {0}", orderId, e);
      if ("T001".equals(e.getCode()) || "O004".equals(e.getCode())) {
        throw new InvalidServiceException(e.getMessage());
      } else {
        throw new InvalidServiceException(backendMessages.getString("demand.items.update.error"));
      }
    } catch (BadRequestException | InvalidServiceException e) {
      throw e;
    } catch (Exception e) {
      xLogger.severe("Error in updating demand items for order {0}", orderId, e);
      throw new InvalidServiceException(backendMessages.getString("demand.items.update.error"));
    } finally {
      if (tx != null && tx.isActive()) {
        tx.rollback();
      }
      if (pm != null) {
        pm.close();
      }
      if (LockUtil.shouldReleaseLock(lockStatus) && !LockUtil.release(Constants.TX_O + orderId)) {
        xLogger.warn("Unable to release lock for key {0}", Constants.TX_O + orderId);
      }
    }
  }

  private OrderResponseModel updateOrder(String updType, Long orderId, String orderUpdatedAt,
                                         PaymentModel paymentDetails,
                                         Long vendorId, String data,
                                         List<String> tags, String referenceType,
                                         HttpServletRequest request) {
    SecureUserDetails user = SecurityUtils.getUserDetails();
    Locale locale = user.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    try {
      Long domainId = SecurityUtils.getCurrentDomainId();
      IOrder order = orderManagementService.getOrder(orderId);
      boolean fullOrder = false;
      if (order == null) {
        throw new Exception(backendMessages.getString("order.none") + " " + orderId);
      }
      if (!OrderUtils.validateOrderUpdatedTime(orderUpdatedAt, order.getUpdatedOn())) {
        throw new LogiException("O004", order.getUpdatedBy(),
            LocalDateUtil.format(order.getUpdatedOn(), user.getLocale(), user.getTimezone()));
      }
      String labelText = null;
      order.setItems(demandService.getDemandItems(orderId));
      if (updType.equals("pmt") && paymentDetails != null) {
        order.addPayment(paymentDetails.pay);
        order.setPaymentOption(paymentDetails.po);
      } else if (updType.equals("vend")) {
        fullOrder = true;
        order.setServicingKiosk(vendorId);
      } else if (updType.equals("cft")) {
        order.setConfirmedFulfillmentTimeRange(data);
      } else if (updType.equals("tgs")) {
        order.setTgs(tagDao.getTagsByNames(tags, ITag.ORDER_TAG), TagUtil.TYPE_ORDER);
      } else if (updType.equals("rid")) {
        if ("salesRefId".equals(referenceType)) {
          order.setSalesReferenceID(data);
        } else if ("purchaseRefId".equals(referenceType)) {
          order.setPurchaseReferenceId(data);
        } else if ("transferRefId".equals(referenceType)) {
          order.setTransferReferenceId(data);
        }
      } else if (updType.equals("efd")) {
        if (StringUtils.isNotEmpty(data)) {
          SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT);
          order.setExpectedArrivalDate(sdf.parse(data));
          labelText =
              LocalDateUtil
                  .format(order.getExpectedArrivalDate(), user.getLocale(), user.getTimezone(),
                      true);
        } else {
          order.setExpectedArrivalDate(null);
        }
      } else if (updType.equals("edd")) {
        if (StringUtils.isNotEmpty(data)) {
          SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT);
          order.setDueDate(sdf.parse(data));
          labelText =
              LocalDateUtil.format(order.getDueDate(), user.getLocale(), user.getTimezone(), true);
        } else {
          order.setDueDate(null);
        }
      }
      order.setUpdatedBy(user.getUsername());
      order.setUpdatedOn(new Date());
      UpdatedOrder updOrder = orderManagementService.updateOrder(order, SourceConstants.WEB);
      OrderResponseModel
          orderResponseModel =
          orderAPIBuilder.buildOrderResponseModel(updOrder, true, domainId, fullOrder,
              fullOrder ? OrdersAPIBuilder.DEFAULT_EMBED : null);
      orderResponseModel.respData = labelText;
      return orderResponseModel;
    } catch (LogiException le) {
      xLogger.severe("Error in updating order", le);
      throw new InvalidServiceException(le.getMessage());
    } catch (Exception e) {
      xLogger.severe("Error in updating order {0}", orderId, e);
      throw new InvalidServiceException(backendMessages.getString("order.update.error"));
    }
  }

  public Results getOrders(Long entityId, int offset, int size,
                           String status, String from, String until, String otype, String tgType,
                           String tag, Integer oty, String salesRefId, String approvalStatus,
                           String purchaseRefId, String transferRefId, Long linkedKioskId,
                           HttpServletRequest request) {
    SecureUserDetails user = SecurityUtils.getUserDetails();
    Locale locale = user.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    try {
      Long domainId = SecurityUtils.getCurrentDomainId();
      DomainConfig dc = DomainConfig.getInstance(domainId);
      Date startDate = null;
      Date endDate = null;
      if (from != null && !from.isEmpty()) {
        startDate = LocalDateUtil.parseCustom(from, Constants.DATE_FORMAT, dc.getTimezone());
      }
      if (until != null && !until.isEmpty()) {
        endDate = LocalDateUtil.parseCustom(until, Constants.DATE_FORMAT, dc.getTimezone());
      }
      oty = oty == null ? IOrder.NONTRANSFER : oty;
      Navigator navigator =
          new Navigator(request.getSession(), "OrdersController.getOrders", offset, size, "dummy",
              0);
      PageParams pageParams = new PageParams(navigator.getCursor(offset), offset, size);
      List<Long> kioskIds = null;
      if (user.getUsername() != null && SecurityConstants.ROLE_SERVICEMANAGER
          .equals(user.getRole())) {
        // Get user
        kioskIds = entitiesService.getKioskIdsForUser(user.getUsername(), null, null)
            .getResults();
        if (kioskIds == null || kioskIds.isEmpty()) {
          return new Results<>(null, null, 0, offset);
        }
      }
      Results or = orderManagementService.getOrders(domainId, entityId, status, startDate, endDate,
          otype, tgType, tag, kioskIds, pageParams, oty, salesRefId, approvalStatus, purchaseRefId,
          transferRefId, linkedKioskId);
      return orderAPIBuilder.buildOrders(or, SecurityUtils.getDomainId());
    } catch (Exception e) {
      xLogger.severe("Error in fetching orders for entity {0} of type {1}", entityId, otype, e);
      throw new InvalidServiceException(backendMessages.getString("orders.fetch.error"));
    }
  }

  @RequestMapping(value = "/add/", method = RequestMethod.POST)
  public
  @ResponseBody
  OrderMaterialsModel createOrder(@RequestBody Map<String, Object> orders) {
    xLogger.fine("Entered create Order");
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale;
    if (sUser.getLocale() != null) {
      locale = sUser.getLocale();
    } else {
      locale = new Locale(Constants.LANG_DEFAULT, Constants.COUNTRY_DEFAULT);
    }
    ResourceBundle backendMessages = Resources.getBundle(locale);

    String userId = sUser.getUsername();
    Long domainId = SecurityUtils.getCurrentDomainId();
    OrderMaterialsModel model = new OrderMaterialsModel();
    MemcacheService cache = null;
    String signature = orders.get("signature") != null ?
        CACHE_KEY + String.valueOf(orders.get("signature")) : null;
    if (signature != null) {
      cache = AppFactory.get().getMemcacheService();
      if (cache != null) {
        OrderMaterialsModel oModel = validateSignature(model, cache, signature);
        if (oModel != null) {
          return oModel;
        }
      }
    }
    DomainConfig dc = DomainConfig.getInstance(domainId);
    Long kioskId = Long.parseLong(String.valueOf(orders.get("kioskid")));
    Long vendorKioskId = orders.get("vkioskid") != null ?
        Long.parseLong(String.valueOf(orders.get("vkioskid"))) : null;
    String ordMsg = null;
    if (orders.get("ordMsg") != null) {
      ordMsg = String.valueOf(orders.get("ordMsg"));
    }
    ArrayList<String> oTag = (ArrayList<String>) orders.get("oTag");
    Integer oType = Integer.parseInt(String.valueOf(orders.get("orderType")));
    boolean skipCheck = Boolean.parseBoolean(String.valueOf(orders.get("skipCheck")));
    String referenceId = null;
    if (orders.get("rid") != null) {
      referenceId = String.valueOf(orders.get("rid"));
    }
    try {
      SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT);
      Date edd = null;
      if (orders.get("edd") != null) {
        edd = sdf.parse(orders.get("edd").toString());
      }
      Date efd = null;
      if (orders.get("efd") != null) {
        efd = sdf.parse(orders.get("efd").toString());
      }
      List<ITransaction> transList = new ArrayList<>();
      Date now = new Date();
      LinkedTreeMap materials = (LinkedTreeMap) orders.get("materials");
      for (Object m : materials.keySet()) {
        Long materialId = Long.parseLong(String.valueOf(m));
        LinkedTreeMap map = (LinkedTreeMap) materials.get(m);
        BigDecimal quantity = new BigDecimal(String.valueOf(map.get("q")));
        String reason = null;
        if (map.get("r") != null) {
          reason = String.valueOf(map.get("r"));
        }
        ITransaction
            trans =
            getInventoryTransaction(domainId, kioskId, materialId, quantity,
                ITransaction.TYPE_ORDER, userId, now, reason);
        transList.add(trans);
      }
      boolean removeSignature = false;
      if (!skipCheck && vendorKioskId != null) {
        for (ITransaction iTransaction : transList) {
          Results dItems = demandService.getDemandDetails(kioskId, iTransaction.getMaterialId(),
              true, false, IOrder.TYPE_PURCHASE, false);
          for (Object item : dItems.getResults()) {
            Object[] f = (Object[]) item;
            String kidStr = String.valueOf(f[0]);
            if (StringUtils.isNotEmpty(kidStr)) {
              Long kId = Long.parseLong(kidStr);
              if (vendorKioskId.equals(kId)) {
                if (model.items == null) {
                  model.items = new ArrayList<>();
                }
                model.items.add(demandBuilder.buildDemandModel(kioskId, iTransaction.getMaterialId(), f,
                    false));
              }
            }
          }
        }
        if (model.items != null) {
          removeSignature = true;
        }
      }
      if (model.items == null) {
        OrderResults orderResults =
            orderManagementService.updateOrderTransactions(
                new UpdateOrderTransactionsModel(domainId, userId, ITransaction.TYPE_ORDER,
                transList, kioskId, null, ordMsg, dc.autoOrderGeneration(), vendorKioskId, null,
                null, null,
                null, null, null, BigDecimal.ZERO, null, null, dc.allowEmptyOrders(), oTag, oType,
                oType == 2,
                    null, edd, efd, SourceConstants.WEB, null, null, null, referenceId));
        IOrder order = orderResults.getOrder();
        String prefix = backendMessages.getString("order");
        if (oType == 0) {
          if (dc.getOrdersConfig() != null && dc.getOrdersConfig().isTransferRelease()) {
            prefix = backendMessages.getString("order.release");
          } else {
            prefix = backendMessages.getString("order.transfer");
          }
        }
        model.orderId = order.getOrderId();
        model.msg =
            prefix + " <b>" + order.getOrderId()
                + "</b> " + backendMessages.getString("created.successwith") + " <b>" + order.size()
                + "</b> " + backendMessages.getString("items.lowercase") + ". ";
      }
      if (removeSignature) {
        DedupUtil.removeSignature(cache, signature);
      } else {
        DedupUtil.setSignatureAndStatus(cache, signature, DedupUtil.SUCCESS);
      }
    } catch (Exception e) {
      xLogger.severe("Error in creating Order on domain {0}", domainId, e);
      throw new InvalidServiceException(backendMessages.getString("order.create.error"));
    }
    return model;
  }

  /**
   * Check if the signature exists in cache
   */
  private OrderMaterialsModel validateSignature(OrderMaterialsModel model, MemcacheService cache,
                                                String signature) {
    Integer lastStatus = (Integer) cache.get(signature);
    if (lastStatus != null) {
      switch (lastStatus) {
        case DedupUtil.SUCCESS:
          model.msg = "This order request was previously successful. "
              + "Please check " + MsgUtil.bold("Orders") + " listing page.";
          return model;
        case DedupUtil.PENDING:
          model.msg = "The previous order request may or may not have been successful. Please "
              + "click the 'Orders' page to see if they are already submitted. If not, "
              + "please create the order again. We are sorry for the inconvenience caused.";
          return model;
        case DedupUtil.FAILED:
          DedupUtil.setSignatureAndStatus(cache, signature, DedupUtil.PENDING);
          break;
        default:
          break;
      }
    } else {
      DedupUtil.setSignatureAndStatus(cache, signature, DedupUtil.PENDING);
    }
    return null;
  }

  private ITransaction getInventoryTransaction(Long domainId, Long kioskId, Long materialId,
                                               BigDecimal quantity, String transType, String userId,
                                               Date now, String reason) {
    ITransaction t = JDOUtils.createInstance(ITransaction.class);
    t.setKioskId(kioskId);
    t.setMaterialId(materialId);
    t.setQuantity(quantity);
    t.setType(transType);
    t.setDomainId(domainId);
    t.setSourceUserId(userId);
    t.setTimestamp(now);
    t.setBatchId(null);
    t.setBatchExpiry(null);
    t.setReason(reason);
    return t;
  }

  @RequestMapping(value = "/order/reasons/{type}", method = RequestMethod.GET)
  public
  @ResponseBody
  List<String> getOrderReasons(@PathVariable String type) {
    Long domainId = SecurityUtils.getCurrentDomainId();
    DomainConfig dc = DomainConfig.getInstance(domainId);
    OrdersConfig oc = dc.getOrdersConfig();
    String reasons = null;
    switch (type) {
      case "eqr":
        reasons = oc.getEditingQuantityReasons();
        break;
      case "orr":
        reasons = oc.getOrderRecommendationReasons();
        break;
      case "psr":
        reasons = oc.getPartialShipmentReasons();
        break;
      case "cor":
        reasons = oc.getCancellingOrderReasons();
        break;
      case "pfr":
        reasons = oc.getPartialFulfillmentReasons();
        break;
    }
    if (reasons != null && reasons.length() > 0) {
      return new ArrayList<>(Arrays.asList(reasons.split(CharacterConstants.COMMA)));
    }
    return null;
  }

  @RequestMapping(value = "/order/{orderId}/tags", method = RequestMethod.POST)
  public
  @ResponseBody
  OrderResponseModel updateOrderTags(@PathVariable Long orderId,
                                     @RequestBody OrderUpdateModel model,
                                     HttpServletRequest request) {
    List<String> tags = StringUtil.getList(model.updateValue, true);
    return updateOrder("tgs", orderId, model.orderUpdatedAt, null, null, null, tags, null, request);
  }

  @RequestMapping(value = "/order/{orderId}/referenceid", method = RequestMethod.POST)
  public
  @ResponseBody
  OrderResponseModel updateReferenceID(@PathVariable Long orderId,
                                       @RequestBody OrderUpdateModel model,
                                       @RequestParam String referenceType,
                                       HttpServletRequest request) {
    return updateOrder("rid", orderId, model.orderUpdatedAt, null, null, model.updateValue,
        null, referenceType, request);
  }

  @RequestMapping(value = "/filter", method = RequestMethod.GET)
  public
  @ResponseBody
  List<String> getIdSuggestions(@RequestParam String id,
                                @RequestParam(required = false) String type,
                                @RequestParam(required = false) Integer oty,
                                HttpServletRequest request) {
    List<String> rid;
    List<Long> kioskIds = null;
    try {
      SecureUserDetails user = SecurityUtils.getUserDetails();
      Long domainId = SecurityUtils.getCurrentDomainId();
      if (user.getUsername() != null) {
        // Get user
        if (SecurityConstants.ROLE_SERVICEMANAGER.equals(user.getRole())) {
          kioskIds = entitiesService.getKioskIdsForUser(user.getUsername(), null, null).getResults();
          if (kioskIds == null || kioskIds.isEmpty()) {
            return new ArrayList<>(1);
          }
        }
      }
      rid = orderManagementService.getIdSuggestions(domainId, id, type, oty, kioskIds);
      return rid;
    } catch (Exception e) {
      xLogger
          .warn("Error in getting order id for suggestions starting with {0} of type {1}", id, type,
              e);
    }
    return null;
  }

  @RequestMapping(value = "/automate", method = RequestMethod.GET)
  public
  @ResponseBody
  void automateOrders(@RequestParam(value = "domain_id") Long domainId) throws ServiceException {
    //automate order creation for inventory items that hit min or less
    orderAutomationAction.invoke(domainId);
  }

  @RequestMapping(value = "/schedule-automation", method = RequestMethod.GET)
  public
  @ResponseBody
  void scheduleOrderAutomation() throws ServiceException {
    scheduleOrderAutomation.invoke();
  }

  @RequestMapping(value = "/{orderId}/delivery-requests", method = RequestMethod.POST)
  public ResponseEntity createDeliveryRequest(@PathVariable Long orderId,
                                    @RequestBody DeliveryRequestModel model) throws LogiException {
    if(model == null) {
      throw new BadRequestException("O020", null);
    }
    model.setOrderId(orderId);
    model.setShipmentId(null);
    orderManagementService.createDeliveryRequest(model);
    return new ResponseEntity(HttpStatus.OK);
  }

  @RequestMapping(value = "/{orderId}/shipments/{sId}/delivery-requests",
      method = RequestMethod.POST)
  public ResponseEntity createDeliveryRequest(@PathVariable Long orderId,
                                              @PathVariable String sId,
                                              @RequestBody DeliveryRequestModel model)
      throws LogiException {
    if(model == null) {
      throw new BadRequestException("O020", null);
    }
    model.setOrderId(orderId);
    model.setShipmentId(sId);
    orderManagementService.createDeliveryRequest(model);
    return new ResponseEntity(HttpStatus.OK);
  }
}
