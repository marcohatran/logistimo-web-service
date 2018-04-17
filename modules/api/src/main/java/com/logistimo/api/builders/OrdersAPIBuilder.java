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

package com.logistimo.api.builders;

import com.logistimo.accounting.service.IAccountingService;
import com.logistimo.api.models.DemandItemBatchModel;
import com.logistimo.api.models.DemandModel;
import com.logistimo.api.models.OrderApprovalTypesModel;
import com.logistimo.api.models.OrderApproverModel;
import com.logistimo.api.models.OrderModel;
import com.logistimo.api.models.OrderResponseModel;
import com.logistimo.api.models.Permissions;
import com.logistimo.api.models.UserContactModel;
import com.logistimo.auth.SecurityConstants;
import com.logistimo.auth.SecurityUtil;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.config.models.ApprovalsConfig;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.constants.CharacterConstants;
import com.logistimo.constants.Constants;
import com.logistimo.constants.PermissionConstants;
import com.logistimo.dao.JDOUtils;
import com.logistimo.domains.entity.IDomain;
import com.logistimo.domains.service.DomainsService;
import com.logistimo.domains.utils.DomainsUtil;
import com.logistimo.entities.auth.EntityAuthoriser;
import com.logistimo.entities.entity.IApprover;
import com.logistimo.entities.entity.IKiosk;
import com.logistimo.entities.service.EntitiesService;
import com.logistimo.inventory.dao.IInvntryDao;
import com.logistimo.inventory.entity.IInvAllocation;
import com.logistimo.inventory.entity.IInvntry;
import com.logistimo.inventory.entity.IInvntryBatch;
import com.logistimo.inventory.entity.IInvntryEvntLog;
import com.logistimo.inventory.entity.ITransaction;
import com.logistimo.inventory.service.InventoryManagementService;
import com.logistimo.logger.XLog;
import com.logistimo.materials.entity.IHandlingUnit;
import com.logistimo.materials.entity.IMaterial;
import com.logistimo.materials.service.IHandlingUnitService;
import com.logistimo.materials.service.MaterialCatalogService;
import com.logistimo.models.shipments.ShipmentItemBatchModel;
import com.logistimo.models.shipments.ShipmentItemModel;
import com.logistimo.orders.OrderUtils;
import com.logistimo.orders.approvals.constants.ApprovalConstants;
import com.logistimo.orders.approvals.dao.IApprovalsDao;
import com.logistimo.orders.approvals.service.IOrderApprovalsService;
import com.logistimo.orders.entity.IDemandItem;
import com.logistimo.orders.entity.IOrder;
import com.logistimo.orders.entity.approvals.IOrderApprovalMapping;
import com.logistimo.orders.models.UpdatedOrder;
import com.logistimo.orders.service.IDemandService;
import com.logistimo.pagination.Results;
import com.logistimo.returns.Status;
import com.logistimo.returns.models.ReturnsFilters;
import com.logistimo.returns.service.ReturnsService;
import com.logistimo.returns.vo.ReturnsItemBatchVO;
import com.logistimo.returns.vo.ReturnsItemVO;
import com.logistimo.returns.vo.ReturnsVO;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.ObjectNotFoundException;
import com.logistimo.services.ServiceException;
import com.logistimo.shipments.ShipmentStatus;
import com.logistimo.shipments.entity.IShipment;
import com.logistimo.shipments.entity.IShipmentItem;
import com.logistimo.shipments.entity.IShipmentItemBatch;
import com.logistimo.shipments.service.IShipmentService;
import com.logistimo.tags.TagUtil;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.users.service.UsersService;
import com.logistimo.utils.BigUtil;
import com.logistimo.utils.CommonUtils;
import com.logistimo.utils.LocalDateUtil;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Component
public class OrdersAPIBuilder {

  private static final XLog xLogger = XLog.getLog(OrdersAPIBuilder.class);

  public static final String PERMISSIONS = "permissions";

  public static final String[] DEFAULT_EMBED = new String[]{OrdersAPIBuilder.PERMISSIONS};

  private IOrderApprovalsService orderApprovalsService;
  private IHandlingUnitService handlingUnitService;
  private MaterialCatalogService materialCatalogService;
  private InventoryManagementService inventoryManagementService;
  private DomainsService domainsService;
  private UsersService usersService;
  private EntitiesService entitiesService;
  private IAccountingService accountingService;
  private IDemandService demandService;
  private IShipmentService shipmentService;
  private IInvntryDao invntryDao;
  private IApprovalsDao approvalsDao;

  @Autowired
  private ReturnsService returnsService;

  @Autowired
  public void setOrderApprovalsService(IOrderApprovalsService orderApprovalsService) {
    this.orderApprovalsService = orderApprovalsService;
  }

  @Autowired
  public void setHandlingUnitService(IHandlingUnitService handlingUnitService) {
    this.handlingUnitService = handlingUnitService;
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
  public void setDomainsService(DomainsService domainsService) {
    this.domainsService = domainsService;
  }

  @Autowired
  public void setUsersService(UsersService usersService) {
    this.usersService = usersService;
  }

  @Autowired
  public void setEntitiesService(EntitiesService entitiesService) {
    this.entitiesService = entitiesService;
  }

  @Autowired
  public void setAccountingService(IAccountingService accountingService) {
    this.accountingService = accountingService;
  }

  @Autowired
  public void setDemandService(IDemandService demandService) {
    this.demandService = demandService;
  }

  @Autowired
  public void setShipmentService(IShipmentService shipmentService) {
    this.shipmentService = shipmentService;
  }

  @Autowired
  public void setInvntryDao(IInvntryDao invntryDao) {
    this.invntryDao = invntryDao;
  }

  @Autowired
  public void setApprovalsDao(IApprovalsDao approvalsDao) {
    this.approvalsDao = approvalsDao;
  }

  public Results buildOrders(Results results, Long domainId) {
    List orders = results.getResults();
    List<OrderModel> newOrders = new ArrayList<>();
    int sno = results.getOffset() + 1;
    Map<Long, String> domainNames = new HashMap<>(1);
    for (Object obj : orders) {
      IOrder o = (IOrder) obj;
      // Add row
      OrderModel model = build(o, domainId, domainNames);
      if (model != null) {
        model.sno = sno++;
        newOrders.add(model);
      }
    }
    return new Results<>(newOrders, results.getCursor(),
        results.getNumFound(), results.getOffset());
  }

  public OrderModel build(IOrder o, Long domainId,
                          Map<Long, String> domainNames) {
    OrderModel model = new OrderModel();
    Long kioskId = o.getKioskId();
    SecureUserDetails user = SecurityUtils.getUserDetails();
    Locale locale = user.getLocale();
    String timezone = user.getTimezone();
    IKiosk k;
    IKiosk vendor = null;
    try {
      k = entitiesService.getKiosk(kioskId, false);
    } catch (Exception e) {
      xLogger.warn("{0} when getting kiosk data for order {1}: {2}",
          e.getClass().getName(), o.getOrderId(), e.getMessage());
      return null;
    }
    try {
      String domainName = domainNames.get(o.getDomainId());

      model.id = o.getOrderId();
      model.size = o.getNumberOfItems();
      model.cur = o.getCurrency();
      model.price = o.getFormattedPrice();
      model.status = OrderUtils.getStatusDisplay(o.getStatus(), locale);
      model.st = o.getStatus();
      model.enm = k.getName();
      model.eadd = CommonUtils.getAddress(k.getCity(), k.getTaluk(), k.getDistrict(), k.getState());
      model.cdt = LocalDateUtil.format(o.getCreatedOn(), locale, timezone);
      model.statusUpdateDate =
          LocalDateUtil.formatCustom(o.getStatusUpdatedOn(), Constants.DATETIME_FORMAT, timezone);
      model.ubid = o.getUpdatedBy();
      model.src = o.getSrc();
      model.salesRefId = o.getSalesReferenceID();
      model.setPurchaseReferenceId(o.getPurchaseReferenceId());
      model.setTransferReferenceId(o.getTransferReferenceId());
      if (o.getUpdatedBy() != null) {
        try {
          model.uby = usersService.getUserAccount(o.getUpdatedBy()).getFullName();
          model.udt = LocalDateUtil.format(o.getUpdatedOn(), locale, timezone);
          model.orderUpdatedAt =
              LocalDateUtil.formatCustom(o.getUpdatedOn(), Constants.DATETIME_FORMAT, null);
        } catch (Exception e) {
          // ignore
        }
      }
      model.tax = o.getTax();
      model.uid = o.getUserId();

      if (model.uid != null) {
        try {
          IUserAccount orderedBy = usersService.getUserAccount(model.uid);
          model.unm = orderedBy.getFullName();
        } catch (Exception e) {
          xLogger.warn("{0} when getting details for user who created the order {1}: ",
              e.getClass().getName(), o.getOrderId(), e);
        }
      }
      model.eid = o.getKioskId();
      model.tgs = o.getTags(TagUtil.TYPE_ORDER);
      model.sdid = o.getDomainId();
      model.lt = o.getLatitude();
      model.ln = o.getLongitude();
      Long vendorId = o.getServicingKiosk();

      if (vendorId != null) {
        try {
          vendor = entitiesService.getKiosk(vendorId, false);
          model.vid = vendorId.toString();
          model.vnm = vendor.getName();
          model.vadd =
              CommonUtils.getAddress(vendor.getCity(), vendor.getTaluk(), vendor.getDistrict(),
                  vendor.getState());
          model.hva =
              EntityAuthoriser
                  .authoriseEntity(vendorId, user.getRole(), user.getUsername(), domainId);
        } catch (Exception e) {
          xLogger.warn("{0} when getting vendor data for order {1}: {2}",
              e.getClass().getName(), o.getOrderId(), e.getMessage());
          if (o.getStatus().equals(IOrder.FULFILLED)) {
            model.vid = "-1";
          }
        }
      }
      Integer vPermission = k.getVendorPerm();
      if (vPermission < 2 && model.vid != null) {
        vPermission =
            EntityAuthoriser
                .authoriseEntityPerm(Long.valueOf(model.vid), user.getRole(),
                    user.getUsername(), user.getDomainId());
      }
      model.atv = vPermission > 1;
      model.atvv = vPermission > 0;

      if (vendor != null) {
        Integer cPermission = vendor.getCustomerPerm();
        if (cPermission < 2 && model.eid != null) {
          cPermission =
              EntityAuthoriser.authoriseEntityPerm(model.eid, user.getRole(),
                  user.getUsername(), user.getDomainId());
        }
        model.atc = cPermission > 1;
        model.atvc = cPermission > 0;
      }
      if (domainName == null) {
        IDomain domain = null;
        try {
          domain = domainsService.getDomain(o.getDomainId());
        } catch (Exception e) {
          xLogger.warn("Error while fetching Domain {0}", o.getDomainId());
        }
        if (domain != null) {
          domainName = domain.getName();
        } else {
          domainName = Constants.EMPTY;
        }
        domainNames.put(o.getDomainId(), domainName);
      }
      model.sdname = domainName;
      SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT_CSV);
      if (o.getExpectedArrivalDate() != null) {
        model.efd = sdf.format(o.getExpectedArrivalDate());
        model.efdLabel =
            LocalDateUtil
                .format(o.getExpectedArrivalDate(), user.getLocale(), user.getTimezone(), true);
      }
      if (o.getDueDate() != null) {
        model.edd = sdf.format(o.getDueDate());
        model.eddLabel =
            LocalDateUtil.format(o.getDueDate(), user.getLocale(), user.getTimezone(), true);
      }
    } catch (Exception e) {
      xLogger.warn("{0} when trying to get kiosk {1} and create a new row for order {2}: {3}",
          e.getClass().getName(), kioskId, o.getOrderId(), e);
    }
    model.oty = o.getOrderType();
    model.crsn = o.getCancelledDiscrepancyReason();
    return model;
  }

  /**
   * Returns the primary approvers for a particular order and approval type
   */
  public List<UserContactModel> buildPrimaryApprovers(IOrder order, Integer approvalType)
      throws ServiceException, ObjectNotFoundException {
    List<String> prApprovers = new ArrayList<>(1);
    if (IOrder.TRANSFER_ORDER == approvalType) {
      prApprovers = DomainConfig.getInstance(order.getDomainId()).getApprovalsConfig()
          .getOrderConfig().getPrimaryApprovers();
    } else {
      List<IApprover> primaryApprovers;
      if (IOrder.PURCHASE_ORDER == approvalType) {
        primaryApprovers =
            entitiesService.getApprovers(order.getKioskId(), IApprover.PRIMARY_APPROVER,
                IApprover.PURCHASE_ORDER);
      } else {
        primaryApprovers =
            entitiesService.getApprovers(order.getServicingKiosk(), IApprover.PRIMARY_APPROVER,
                IApprover.SALES_ORDER);
      }
      if (primaryApprovers != null) {
        for (IApprover apr : primaryApprovers) {
          prApprovers.add(apr.getUserId());
        }
      }
    }
    return buildUserContactModels(prApprovers);
  }

  public List<UserContactModel> buildUserContactModels(List<String> approvers)
      throws ObjectNotFoundException {
    List<UserContactModel> models = new ArrayList<>(1);
    if (approvers != null && !approvers.isEmpty()) {
      for (String s : approvers) {
        IUserAccount userAccount = usersService.getUserAccount(s);
        UserContactModel model = new UserContactModel();
        model.setEmail(userAccount.getEmail());
        model.setName(userAccount.getFullName());
        model.setPhone(userAccount.getMobilePhoneNumber());
        model.setUserId(userAccount.getUserId());
        models.add(model);
      }
    }
    return models;
  }

  /**
   * Returns the permission to be restricted
   */
  public Permissions buildPermissionModel(IOrder order, OrderModel orderModel, Integer approvalType,
                                          boolean isApprovalRequired, String userName) {
    Permissions model = new Permissions();
    List<String> permissions = new ArrayList<>(1);
    if (isApprovalRequired) {
      IOrderApprovalMapping approvalMapping =
          approvalsDao.getOrderApprovalMapping(order.getOrderId(), approvalType);

      if (IOrder.PURCHASE_ORDER == approvalType) {
        if (approvalMapping != null) {
          if (ApprovalConstants.CANCELLED.equals(approvalMapping.getStatus()) ||
              ApprovalConstants.REJECTED.equals(approvalMapping.getStatus()) ||
              ApprovalConstants.EXPIRED.equals(approvalMapping.getStatus())) {
            if (orderModel.atc) {
              permissions.add(PermissionConstants.CANCEL);
              permissions.add(PermissionConstants.EDIT);
              permissions.add(PermissionConstants.EDIT_META_DATA);
            }
          } else if (ApprovalConstants.APPROVED.equals(approvalMapping.getStatus())) {
            if (orderModel.atc) {
              permissions.add(PermissionConstants.EDIT_META_DATA);
            }
            if (orderModel.atv) {
              permissions.add(PermissionConstants.ALLOCATE);
              permissions.add(PermissionConstants.EDIT);
              permissions.add(PermissionConstants.CONFIRM);
              permissions.add(PermissionConstants.CANCEL);
              permissions.add(PermissionConstants.REOPEN);
              permissions.add(PermissionConstants.EDIT_META_DATA);
            }
          }
        } else {
          if (orderModel.atc) {
            permissions.add(PermissionConstants.CANCEL);
            permissions.add(PermissionConstants.EDIT);
            permissions.add(PermissionConstants.EDIT_META_DATA);
          }
        }
      } else if (IOrder.SALES_ORDER == approvalType) {
        if (approvalMapping != null) {
          if (ApprovalConstants.CANCELLED.equals(approvalMapping.getStatus()) ||
              ApprovalConstants.REJECTED.equals(approvalMapping.getStatus()) ||
              ApprovalConstants.EXPIRED.equals(approvalMapping.getStatus())) {
            if (orderModel.atv) {
              permissions.add(PermissionConstants.ALLOCATE);
              permissions.add(PermissionConstants.CANCEL);
              permissions.add(PermissionConstants.CONFIRM);
              permissions.add(PermissionConstants.EDIT);
              permissions.add(PermissionConstants.REOPEN);
              permissions.add(PermissionConstants.EDIT_META_DATA);
            }
            if (orderModel.atc) {
              permissions.add(PermissionConstants.EDIT_META_DATA);
            }
          } else if (ApprovalConstants.APPROVED.equals(approvalMapping.getStatus())) {
            if (orderModel.atv) {
              permissions.add(PermissionConstants.ALLOCATE);
              permissions.add(PermissionConstants.SHIP);
              permissions.add(PermissionConstants.CREATE_SHIPMENT);
              permissions.add(PermissionConstants.CONFIRM);
              permissions.add(PermissionConstants.CANCEL);
              permissions.add(PermissionConstants.EDIT_META_DATA);
            }
            if (orderModel.atc) {
              permissions.add(PermissionConstants.EDIT_META_DATA);
            }
          }
        } else {
          if (orderModel.atv) {
            permissions.add(PermissionConstants.CANCEL);
            permissions.add(PermissionConstants.CONFIRM);
            permissions.add(PermissionConstants.ALLOCATE);
            permissions.add(PermissionConstants.EDIT);
            permissions.add(PermissionConstants.REOPEN);
            permissions.add(PermissionConstants.EDIT_META_DATA);
          }
          if (orderModel.atc) {
            permissions.add(PermissionConstants.EDIT_META_DATA);
          }
        }
      } else if (IOrder.TRANSFER_ORDER == approvalType) {
        if (approvalMapping != null) {
          if (ApprovalConstants.CANCELLED.equals(approvalMapping.getStatus())
              || ApprovalConstants.REJECTED.equals(approvalMapping.getStatus())
              || ApprovalConstants.EXPIRED.equals(approvalMapping.getStatus())) {
            permissions.add(PermissionConstants.EDIT);
            permissions.add(PermissionConstants.CANCEL);
            permissions.add(PermissionConstants.EDIT_META_DATA);
          } else if (ApprovalConstants.APPROVED.equals(approvalMapping.getStatus())) {
            if (userName.equals(approvalMapping.getCreatedBy())) {
              permissions.add(PermissionConstants.CANCEL);
            }
            if (orderModel.atc) {
              permissions.add(PermissionConstants.EDIT_META_DATA);
            }
            if (orderModel.atv) {
              permissions.add(PermissionConstants.CONFIRM);
              permissions.add(PermissionConstants.ALLOCATE);
              permissions.add(PermissionConstants.SHIP);
              permissions.add(PermissionConstants.CREATE_SHIPMENT);
              permissions.add(PermissionConstants.EDIT_META_DATA);
            }
          }
        } else {
          if (!(order.isVisibleToCustomer() && order.isVisibleToVendor())) {
            permissions.add(PermissionConstants.CANCEL);
            permissions.add(PermissionConstants.EDIT);
            permissions.add(PermissionConstants.EDIT_META_DATA);
          } else {
            permissions.add(PermissionConstants.CANCEL);
            permissions.add(PermissionConstants.CONFIRM);
            permissions.add(PermissionConstants.ALLOCATE);
            permissions.add(PermissionConstants.EDIT);
            permissions.add(PermissionConstants.SHIP);
            permissions.add(PermissionConstants.CREATE_SHIPMENT);
            permissions.add(PermissionConstants.REOPEN);
            permissions.add(PermissionConstants.EDIT_META_DATA);
          }
        }
      }
    } else {
      permissions.add(PermissionConstants.CANCEL);
      permissions.add(PermissionConstants.CONFIRM);
      permissions.add(PermissionConstants.ALLOCATE);
      permissions.add(PermissionConstants.EDIT);
      permissions.add(PermissionConstants.SHIP);
      permissions.add(PermissionConstants.CREATE_SHIPMENT);
      permissions.add(PermissionConstants.REOPEN);
      permissions.add(PermissionConstants.EDIT_META_DATA);
    }
    model.setPermissions(permissions);
    return model;
  }

  /**
   * Gives the types of approval to be shown to the user
   */
  public List<OrderApprovalTypesModel> buildOrderApprovalTypesModel(OrderModel orderModel,
                                                                    IOrder order)
      throws ServiceException, ObjectNotFoundException {
    List<OrderApprovalTypesModel> models = new ArrayList<>(1);
    SecureUserDetails user = SecurityUtils.getUserDetails();
    if (order.isTransfer() &&
        (!(orderModel.isVisibleToCustomer() && orderModel.isVisibleToVendor()) ||
            (SecurityUtil.compareRoles(user.getRole(), SecurityConstants.ROLE_DOMAINOWNER) >= 0) ||
            user.getUsername().equals(order.getUserId()))
        && orderApprovalsService.isApprovalRequired(order, IOrder.TRANSFER_ORDER)) {
      buildTransferApprovalTypeModel(models, order);
    } else {
      if (order.isPurchase() && orderModel.isVisibleToCustomer() &&
          orderModel.atc &&
          orderApprovalsService.isApprovalRequired(order, IOrder.PURCHASE_ORDER)) {
        buildPurchaseApprovalTypeModel(models, order);
      }
      if (!order.isTransfer() && orderModel.isVisibleToVendor() &&
          orderModel.atv &&
          orderApprovalsService.isApprovalRequired(order, IOrder.SALES_ORDER)) {
        buildSalesApprovalTypeModel(models, order);
      }
    }
    return models;
  }

  private void buildSalesApprovalTypeModel(List<OrderApprovalTypesModel> models,
                                           IOrder order) {
    IOrderApprovalMapping orderApprovalMapping =
        approvalsDao.getOrderApprovalMapping(order.getOrderId(), IOrder.SALES_ORDER);
    if (orderApprovalMapping != null) {
      OrderApprovalTypesModel model = new OrderApprovalTypesModel();
      model.setType(ApprovalConstants.SALES);
      model.setId(orderApprovalMapping.getApprovalId());
      List<IOrderApprovalMapping> approvalMappings =
          approvalsDao.getTotalOrderApprovalMapping(order.getOrderId());
      if (approvalMappings != null && !approvalMappings.isEmpty()) {
        model.setCount(approvalMappings.size());
      }
      models.add(model);
    } else {
      if (order.getStatus().equals(IOrder.PENDING) || order.getStatus().equals(IOrder.CONFIRMED)) {
        OrderApprovalTypesModel model = new OrderApprovalTypesModel();
        model.setType(ApprovalConstants.SALES);
        models.add(model);
      }
    }
  }

  private void buildPurchaseApprovalTypeModel(List<OrderApprovalTypesModel> models,
                                              IOrder order) {
    IOrderApprovalMapping orderApprovalMapping =
        approvalsDao.getOrderApprovalMapping(order.getOrderId(), IOrder.PURCHASE_ORDER);
    if (orderApprovalMapping != null) {
      OrderApprovalTypesModel model = new OrderApprovalTypesModel();
      model.setType(ApprovalConstants.PURCHASE);
      model.setId(orderApprovalMapping.getApprovalId());
      List<IOrderApprovalMapping> approvalMappings =
          approvalsDao.getTotalOrderApprovalMapping(order.getOrderId());
      if (approvalMappings != null && !approvalMappings.isEmpty()) {
        model.setCount(approvalMappings.size());
      }
      models.add(model);
    } else if (!order.isVisibleToVendor()) {
      OrderApprovalTypesModel model = new OrderApprovalTypesModel();
      model.setType(ApprovalConstants.PURCHASE);
      models.add(model);
    }
  }

  private void buildTransferApprovalTypeModel(List<OrderApprovalTypesModel> models,
                                              IOrder order) {
    IOrderApprovalMapping orderApprovalMapping =
        approvalsDao.getOrderApprovalMapping(order.getOrderId(), IOrder.TRANSFER_ORDER);
    if (orderApprovalMapping != null) {
      OrderApprovalTypesModel model = new OrderApprovalTypesModel();
      model.setType(ApprovalConstants.TRANSFER);
      model.setId(orderApprovalMapping.getApprovalId());
      List<IOrderApprovalMapping> approvalMappings =
          approvalsDao.getTotalOrderApprovalMapping(order.getOrderId());
      if (approvalMappings != null && !approvalMappings.isEmpty()) {
        model.setCount(approvalMappings.size());
      }
      models.add(model);
    } else if (!order.isVisibleToVendor() || !order.isVisibleToCustomer()) {
      OrderApprovalTypesModel model = new OrderApprovalTypesModel();
      model.setType(ApprovalConstants.TRANSFER);
      models.add(model);
    }
  }

  public OrderModel buildFullOrderModel(IOrder order,
                                        Long domainId, String[] embed) throws Exception {
    OrderModel model = buildOrderModel(order, domainId);
    includeApprovals(model, order, domainId,
        embed != null && Arrays.asList(embed).contains(PERMISSIONS));
    return model;
  }

  private void includeApprovals(OrderModel model, IOrder order,
                                Long domainId, boolean includePermissions)
      throws ServiceException, ObjectNotFoundException {
    model.setApprovalTypesModels(buildOrderApprovalTypesModel(model,
        order));
    Integer approvalType = orderApprovalsService.getApprovalType(order);
    boolean isApprovalRequired = false;
    if (approvalType != null) {
      model.setApprover(
          buildOrderApproverModel(SecurityUtils.getUsername(), approvalType, domainId, order));
      isApprovalRequired = orderApprovalsService.isApprovalRequired(order, approvalType);
    }
    if (includePermissions) {
      Permissions
          permissions =
          buildPermissionModel(order, model, approvalType, isApprovalRequired,
              SecurityUtils.getUsername());
      model.setPermissions(permissions);
    }
  }

  public OrderModel buildOrderModel(IOrder order, Long domainId) throws Exception {
    Map<Long, String> domainNames = new HashMap<>(1);
    OrderModel model = build(order, domainId, domainNames);
    DomainConfig dc = DomainConfig.getInstance(domainId);
    IKiosk k = null;
    IKiosk vendorKiosk = null;
    SecureUserDetails user = SecurityUtils.getUserDetails();
    Locale locale = user.getLocale();
    boolean
        showStocks =
        IOrder.PENDING.equals(order.getStatus()) || IOrder.CONFIRMED.equals(order.getStatus())
            || IOrder.BACKORDERED.equals(order.getStatus());
    boolean isReturnsAllowed = IOrder.FULFILLED.equals(order.getStatus());
    boolean showVendorStock = dc.autoGI() && order.getServicingKiosk() != null;

    if (order.getServicingKiosk() != null) {
      try {
        vendorKiosk = entitiesService.getKiosk(order.getServicingKiosk(), false);
      } catch (Exception e) {
        xLogger.warn("Error when trying to get kiosk {0} and create a new row for order {1}",
            order.getKioskId(), order.getOrderId(), e);
      }
    }

    boolean accountingEnabled = dc.isAccountingEnabled();

    String creditLimitErr = null;
    BigDecimal availableCredit = BigDecimal.ZERO;
    if (accountingEnabled) {
      try {
        Long customerId = order.getKioskId();
        if (customerId != null && model.vid != null) {
          availableCredit = accountingService.getCreditData(customerId,
              order.getServicingKiosk(), dc).availabeCredit;
        }
      } catch (Exception e) {
        creditLimitErr = e.getMessage();
      }
    }
    model.setVisibleToCustomer(order.isVisibleToCustomer());
    model.setVisibleToVendor(order.isVisibleToVendor());
    model.avc = availableCredit;
    model.avcerr = creditLimitErr;

    model.lt = order.getLatitude();
    model.ln = order.getLongitude();
    model.ac = order.getGeoAccuracy();

    if (order.getKioskId() != null) {
      try {
        k = entitiesService.getKiosk(order.getKioskId(), false);
      } catch (Exception e) {
        xLogger.warn(
            "{0} when trying to get kiosk {1} while fetching order details for order {2}: {3}",
            e.getClass().getName(), order.getKioskId(), order.getOrderId(), e);
      }
    }

    if (k != null) {
      model.elt = k.getLatitude();
      model.eln = k.getLongitude();
    }

    model.tp = order.getTotalPrice();
    model.pst = order.getPriceStatement();

    if (accountingEnabled) {
      model.pd = order.getPaid();
      model.po = order.getPaymentOption();

    }

    if (model.uid != null) {
      try {
        IUserAccount orderedBy = usersService.getUserAccount(model.uid);
        model.unm = orderedBy.getFullName();
      } catch (Exception ignored) {
      }
    }
    if (order.getOrderType() != null && order.getOrderType() == 0) {
      model.alc = false;
      if (SecurityConstants.ROLE_SUPERUSER.equals(user.getRole()) || user.getUsername()
          .equals(order.getUserId())) {
        model.alc = true;
      } else if ((SecurityConstants.ROLE_DOMAINOWNER.equals(user.getRole()))) {
        if (user.getDomainId().equals(order.getDomainId())) {
          model.alc = true;
        } else {
          Set<Long> rs = DomainsUtil.getDomainParents(order.getDomainId(), true);
          if (rs != null) {
            model.alc = rs.contains(user.getDomainId());
          }
        }
      }
    } else {
      model.alc = true;
    }
    model.tgs = order.getTags(TagUtil.TYPE_ORDER);
    model.salesRefId = order.getSalesReferenceID();
    model.setPurchaseReferenceId(order.getPurchaseReferenceId());
    model.setTransferReferenceId(order.getTransferReferenceId());
    model.pt = LocalDateUtil.getFormattedMillisInHoursDays(order.getProcessingTime(), locale, true);
    model.dlt =
        LocalDateUtil.getFormattedMillisInHoursDays(order.getDeliveryLeadTime(), locale, true);

    List<IShipment> shipments = shipmentService.getShipmentsByOrderId(order.getOrderId());
    Map<Long, Map<String, BigDecimal>> quantityByBatches = null;
    Map<Long, Map<String, DemandBatchMeta>> fQuantityByBatches = null;
    Map<Long, List<ShipmentItemModel>> fReasons = null;
    Map<Long, Map<String, BigDecimal>> receivedQuantityByBatches = null;
    if (!showStocks || isReturnsAllowed) {
      quantityByBatches = new HashMap<>();
      for (IShipment shipment : shipments) {
        boolean isFulfilled = ShipmentStatus.FULFILLED.equals(shipment.getStatus());
        if (isFulfilled && fQuantityByBatches == null) {
          fQuantityByBatches = new HashMap<>();
        }
        if (isFulfilled && fReasons == null) {
          fReasons = new HashMap<>();
        }
        if (ShipmentStatus.SHIPPED.equals(shipment.getStatus()) || isFulfilled) {
          shipmentService.includeShipmentItems(shipment);
          for (IShipmentItem iShipmentItem : shipment.getShipmentItems()) {
            if (iShipmentItem.getShipmentItemBatch() != null
                && iShipmentItem.getShipmentItemBatch().size() > 0) {
              for (IShipmentItemBatch iShipmentItemBatch : iShipmentItem.getShipmentItemBatch()) {
                if (!quantityByBatches.containsKey(iShipmentItem.getMaterialId())) {
                  quantityByBatches
                      .put(iShipmentItem.getMaterialId(), new HashMap<>());
                }
                Map<String, BigDecimal> batches =
                    quantityByBatches.get(iShipmentItem.getMaterialId());
                if (batches.containsKey(iShipmentItemBatch.getBatchId())) {
                  batches.put(iShipmentItemBatch.getBatchId(),
                      batches.get(iShipmentItemBatch.getBatchId())
                          .add(iShipmentItemBatch.getQuantity()));
                } else {
                  batches.put(iShipmentItemBatch.getBatchId(), iShipmentItemBatch.getQuantity());
                }
                if (isFulfilled) {
                  if (!fQuantityByBatches.containsKey(iShipmentItem.getMaterialId())) {
                    fQuantityByBatches
                        .put(iShipmentItem.getMaterialId(), new HashMap<>());
                  }
                  Map<String, DemandBatchMeta> fBatches =
                      fQuantityByBatches.get(iShipmentItem.getMaterialId());
                  if (fBatches.containsKey(iShipmentItemBatch.getBatchId())) {
                    fBatches.get(iShipmentItemBatch.getBatchId()).quantity =
                        fBatches.get(iShipmentItemBatch.getBatchId()).quantity
                            .add(iShipmentItemBatch.getFulfilledQuantity());
                    fBatches.get(iShipmentItemBatch.getBatchId()).bd
                        .add(getShipmentItemBatchBD(shipment.getShipmentId(), iShipmentItemBatch));
                  } else {
                    DemandBatchMeta dMeta =
                        new DemandBatchMeta(iShipmentItemBatch.getFulfilledQuantity());
                    dMeta.bd
                        .add(getShipmentItemBatchBD(shipment.getShipmentId(), iShipmentItemBatch));
                    fBatches.put(iShipmentItemBatch.getBatchId(), dMeta);
                  }
                }
              }
            } else if (isFulfilled) {
              if (!fReasons.containsKey(iShipmentItem.getMaterialId())) {
                fReasons.put(iShipmentItem.getMaterialId(), new ArrayList<>());
              }
              ShipmentItemModel m = new ShipmentItemModel();
              m.sid = shipment.getShipmentId();
              m.q = iShipmentItem.getQuantity();
              m.fq = iShipmentItem.getFulfilledQuantity();
              m.frsn = iShipmentItem.getFulfilledDiscrepancyReason();
              fReasons.get(iShipmentItem.getMaterialId()).add(m);
            }
          }
        }
      }
      if (isReturnsAllowed) {
        receivedQuantityByBatches = getReceivedQuantityByBatches(order.getOrderId());
      }
    }
    List<IDemandItem> items = demandService.getDemandItems(order.getOrderId());
    if (items != null) {
      Set<DemandModel> modelItems = new TreeSet<>();
      for (IDemandItem item : items) {
        Long mid = item.getMaterialId();
        IMaterial m;
        try {
          m = materialCatalogService.getMaterial(item.getMaterialId());
        } catch (Exception e) {
          xLogger.warn("WARNING: " + e.getClass().getName() + " when getting material "
              + item.getMaterialId() + ": " + e.getMessage());
          continue;
        }
        DemandModel itemModel = new DemandModel();
        itemModel.nm = m.getName();
        itemModel.id = mid;
        itemModel.materialTags = m.getTags();
        itemModel.q = item.getQuantity();
        itemModel.p = item.getFormattedPrice();
        itemModel.t = item.getTax();
        itemModel.d = item.getDiscount();
        itemModel.a = CommonUtils.getFormattedPrice(item.computeTotalPrice(false));
        itemModel.isBn = m.isBinaryValued();
        itemModel.isBa =
            (vendorKiosk == null || vendorKiosk.isBatchMgmtEnabled()) && m.isBatchEnabled();
        itemModel.oq = item.getOriginalQuantity();
        itemModel.tx = item.getTax();
        itemModel.rsn = item.getReason();
        itemModel.sdrsn = item.getShippedDiscrepancyReason();
        itemModel.sq = item.getShippedQuantity();
        itemModel.yts = itemModel.q.subtract(itemModel.sq);
        itemModel.isq = item.getInShipmentQuantity();
        itemModel.ytcs = itemModel.q.subtract(itemModel.isq);
        //itemModel.mst = item.getMaterialStatus();
        itemModel.rq = item.getRecommendedOrderQuantity();
        itemModel.fq = item.getFulfilledQuantity();
        itemModel.returnedQuantity = item.getReturnedQuantity();
        itemModel.oastk = BigDecimal.ZERO;
        itemModel.astk = BigDecimal.ZERO;
        itemModel.tm = m.isTemperatureSensitive();
        if (showStocks) {
          List<IInvAllocation>
              allocationList =
              inventoryManagementService.getAllocationsByTagMaterial(mid,
                  IInvAllocation.Type.ORDER + CharacterConstants.COLON + order.getOrderId());
          for (IInvAllocation ia : allocationList) {
            if (IInvAllocation.Type.ORDER.toString().equals(ia.getType())) {
              if (itemModel.isBa) {
                if (BigUtil.equalsZero(ia.getQuantity())) {
                  continue;
                }
                DemandItemBatchModel batchModel = new DemandItemBatchModel();
                batchModel.q = ia.getQuantity();
                batchModel.id = ia.getBatchId();
                IInvntryBatch b = inventoryManagementService
                        .getInventoryBatch(order.getServicingKiosk(), item.getMaterialId(),
                            batchModel.id, null);
                if (b == null) {
                  b = inventoryManagementService
                          .getInventoryBatch(order.getKioskId(), item.getMaterialId(),
                              batchModel.id, null);
                }
                if (b == null) {
                  xLogger.warn("Error while getting inventory batch for kiosk {0}, material {1}, "
                          + "batch id {2}, order id: {3}", order.getServicingKiosk(),
                      item.getMaterialId(), batchModel.id, order.getOrderId());
                  continue;
                }
                batchModel.e = b.getBatchExpiry() != null ? LocalDateUtil
                    .formatCustom(b.getBatchExpiry(), "dd/MM/yyyy", null) : "";
                batchModel.m = b.getBatchManufacturer();
                batchModel.mdt = b.getBatchManufacturedDate() != null ? LocalDateUtil
                    .formatCustom(b.getBatchManufacturedDate(), "dd/MM/yyyy", null) : "";
                itemModel.astk = itemModel.astk.add(batchModel.q);
                if (itemModel.bts == null) {
                  itemModel.bts = new HashSet<>();
                }
                batchModel.mst = ia.getMaterialStatus();
                itemModel.bts.add(batchModel);
              } else {
                itemModel.astk = ia.getQuantity();
                itemModel.mst = ia.getMaterialStatus();
              }
            }
            itemModel.oastk = itemModel.oastk.add(ia.getQuantity());
          }
        }
        Set<DemandItemBatchModel> batches = new HashSet<>();
        BigDecimal allocatedStock = BigDecimal.ZERO;
        if (!showStocks || isReturnsAllowed) {
          if (itemModel.isBa) {
            Map<String, BigDecimal> batchMap = quantityByBatches.get(item.getMaterialId());
            Map<String, DemandBatchMeta> fBatchMap = null;
            if (fQuantityByBatches != null) {
              fBatchMap = fQuantityByBatches.get(item.getMaterialId());
            }
            if (batchMap != null) {
              for (String batchId : batchMap.keySet()) {
                DemandItemBatchModel batchModel = new DemandItemBatchModel();
                batchModel.q = batchMap.get(batchId);
                if (fBatchMap != null && fBatchMap.get(batchId) != null) {
                  batchModel.fq = fBatchMap.get(batchId).quantity;
                  batchModel.bd = fBatchMap.get(batchId).bd;
                }
                batchModel.id = batchId;
                IInvntryBatch b = inventoryManagementService
                        .getInventoryBatch(order.getServicingKiosk(), item.getMaterialId(),
                            batchModel.id, null);
                if (b == null) {
                  b = inventoryManagementService
                          .getInventoryBatch(order.getKioskId(), item.getMaterialId(),
                              batchModel.id,
                              null);
                }
                if (b == null) {
                  xLogger.warn("Error while getting inventory batch for kiosk {0}, material {1}, "
                          + "batch id {2}, order id: {3}", order.getServicingKiosk(),
                      item.getMaterialId(), batchModel.id, order.getOrderId());
                  continue;
                }
                batchModel.e = b.getBatchExpiry() != null ? LocalDateUtil
                    .formatCustom(b.getBatchExpiry(), "dd/MM/yyyy", null) : "";
                batchModel.m = b.getBatchManufacturer();
                batchModel.mdt = b.getBatchManufacturedDate() != null ? LocalDateUtil
                    .formatCustom(b.getBatchManufacturedDate(), "dd/MM/yyyy", null) : "";
                allocatedStock = allocatedStock.add(batchModel.q);
                batches.add(batchModel);
              }
              if (isReturnsAllowed) {
                final Map<String, BigDecimal> receivedBatchQuantity =
                    receivedQuantityByBatches.get(item.getMaterialId());
                for (DemandItemBatchModel batch : batches) {
                  if (receivedBatchQuantity != null && receivedBatchQuantity
                      .containsKey(batch.id)) {
                    batch.returnedQuantity = receivedBatchQuantity.get(batch.id);
                  }
                }
              }
            }
          } else {
            if (fReasons != null) {
              itemModel.bd = fReasons.get(item.getMaterialId());
            }
          }
        }
        itemModel.returnBatches = batches;
        if (!showStocks) {
          itemModel.bts = batches;
          itemModel.astk = allocatedStock;
        }
        if ((showVendorStock && showStocks) || isReturnsAllowed) {
          try {
            IInvntry inv = inventoryManagementService.getInventory(order.getServicingKiosk(), mid);
            if (inv != null) {
              itemModel.vs = inv.getStock();
              itemModel.vsavibper = inventoryManagementService.getStockAvailabilityPeriod(inv, dc);
              itemModel.atpstk =
                  inv.getAvailableStock(); //todo: Check Available to promise stock is right??????
              itemModel.itstk = inv.getInTransitStock();
              itemModel.vmax = inv.getMaxStock();
              itemModel.vmin = inv.getReorderLevel();
              IInvntryEvntLog lastEventLog = invntryDao.getInvntryEvntLog(inv);
              if (lastEventLog != null) {
                itemModel.vevent = inv.getStockEvent();
              }
            }
          } catch (Exception ignored) {
          }
        }
        if (showStocks) {
          try {
            IInvntry inv = inventoryManagementService.getInventory(order.getKioskId(), mid);
            if (inv != null) {
              itemModel.stk = inv.getStock();
              itemModel.max = inv.getMaxStock();
              itemModel.min = inv.getReorderLevel();
              IInvntryEvntLog lastEventLog = invntryDao.getInvntryEvntLog(inv);
              if (lastEventLog != null) {
                itemModel.event = inv.getStockEvent();
              }
              itemModel.im = inv.getInventoryModel();
              itemModel.eoq = inv.getEconomicOrderQuantity();
              itemModel.csavibper = inventoryManagementService.getStockAvailabilityPeriod(inv, dc);
            }
          } catch (Exception ignored) {
            // ignore
          }
        }
        try {
          Map<String, String> hu = handlingUnitService.getHandlingUnitDataByMaterialId(mid);
          if (hu != null) {
            itemModel.huQty = new BigDecimal(hu.get(IHandlingUnit.QUANTITY));
            itemModel.huName = hu.get(IHandlingUnit.NAME);
          }
        } catch (Exception e) {
          xLogger.warn("Error while fetching Handling Unit {0}", mid, e);
        }
        modelItems.add(itemModel);
      }
      model.its = modelItems;
    }
    return model;
  }

  private Map<Long, Map<String, BigDecimal>> getReceivedQuantityByBatches(Long orderId) {
    final ReturnsFilters
        f =
        ReturnsFilters.builder().orderId(orderId).domainId(SecurityUtils.getCurrentDomainId())
            .build();
    List<ReturnsVO> returnsVOs =
        returnsService.getReturns(f);
    Map<Long, Map<String, BigDecimal>> returnQuantityByBatches = new HashMap<>();
    for (ReturnsVO returnsVO : returnsVOs) {
      if(returnsVO.getStatus().getStatus()!= Status.CANCELLED) {
        final List<ReturnsItemVO> returnsItemVOs = returnsService.getReturnsItem(returnsVO.getId());
        returnsItemVOs.stream()
          .filter(returnsItemVO -> CollectionUtils.isNotEmpty(returnsItemVO.getReturnItemBatches()))
          .forEach(returnsItemVO -> {
            Map<String, BigDecimal> returnQuantityByBatch = returnsItemVO.getReturnItemBatches()
                .stream()
                .collect(Collectors
                    .toMap(b -> b.getBatch().getBatchId(), ReturnsItemBatchVO::getQuantity));
            if (!returnQuantityByBatches.containsKey(returnsItemVO.getMaterialId())) {
              returnQuantityByBatches.put(returnsItemVO.getMaterialId(), returnQuantityByBatch);
            } else {
              Map<String, BigDecimal> batchQuantityMap =
                  returnQuantityByBatches.get(returnsItemVO.getMaterialId());
              for (Map.Entry<String, BigDecimal> entry : returnQuantityByBatch.entrySet()) {
                if (batchQuantityMap.containsKey(entry.getKey())) {
                  batchQuantityMap.put(entry.getKey(),
                      batchQuantityMap.get(entry.getKey()).add(entry.getValue()));
                } else {
                  batchQuantityMap.put(entry.getKey(), entry.getValue());
                }
              }
            }
          });
      }
    }
    return returnQuantityByBatches;
  }

  private ShipmentItemBatchModel getShipmentItemBatchBD(String shipmentID,
                                                        IShipmentItemBatch iShipmentItemBatch) {
    ShipmentItemBatchModel bd = new ShipmentItemBatchModel();
    bd.fq = iShipmentItemBatch.getFulfilledQuantity();
    bd.frsn = iShipmentItemBatch.getFulfilledDiscrepancyReason();
    bd.sid = shipmentID;
    bd.q = iShipmentItemBatch.getQuantity();
    return bd;
  }

  public OrderResponseModel buildOrderResponseModel(UpdatedOrder updOrder,
                                                    boolean includeOrder,
                                                    Long domainId, boolean isFullOrder,
                                                    String[] embed)
      throws Exception {
    OrderModel order = null;
    if (includeOrder) {
      if (isFullOrder) {
        order = buildFullOrderModel(updOrder.order, domainId, embed);
      } else {
        order = build(updOrder.order, domainId, new HashMap<>());
      }
    }
    return new OrderResponseModel(order, updOrder.message, updOrder.inventoryError, null);
  }

  public List<ITransaction> buildTransactionsForNewItems(IOrder order,
                                                         List<DemandModel> demandItemModels) {
    Long domainId = order.getDomainId();
    Long kioskId = order.getKioskId();
    String userId = order.getUserId();
    Date now = new Date();
    List<ITransaction> transactions = new ArrayList<>();
    for (DemandModel demandModel : demandItemModels) {
      IDemandItem item = order.getItem(demandModel.id);
      if (item == null) {
        ITransaction trans = JDOUtils.createInstance(ITransaction.class);
        trans.setDomainId(domainId);
        trans.setKioskId(kioskId);
        trans.setMaterialId(demandModel.id);
        trans.setQuantity(demandModel.q);
        trans.setType(ITransaction.TYPE_REORDER);
        trans.setTrackingId(String.valueOf(order.getOrderId()));
        trans.setSourceUserId(userId);
        trans.setTimestamp(now);
        transactions.add(trans);
      }

    }
    return transactions;
  }


  public IOrder buildOrderMaterials(IOrder order, List<DemandModel> items) {
    for (DemandModel model : items) {
      IDemandItem item = order.getItem(model.id);
      if (item != null) {
        if (BigUtil.notEquals(item.getQuantity(), model.q)) {
          item.setQuantity(model.q);
        }
        if (BigUtil.equals(model.q, model.oq)) {
          item.setOriginalQuantity(model.q);
          item.setShippedDiscrepancyReason(null);
        }
        if (BigUtil.notEquals(item.getDiscount(), model.d)) {
          item.setDiscount(model.d);
        }
        item.setShippedDiscrepancyReason(model.sdrsn);
        item.setReason(model.rsn);
      }
    }
    return order;
  }

  public OrderApproverModel buildOrderApproverModel(String userId, Integer approvalType,
                                                    Long domainId, IOrder order) {
    OrderApproverModel orderApproverModel = null;
    if (IOrder.TRANSFER_ORDER == approvalType) {
      DomainConfig dc = DomainConfig.getInstance(domainId);
      ApprovalsConfig ac = dc.getApprovalsConfig();
      ApprovalsConfig.OrderConfig orderConfig = ac.getOrderConfig();
      if (orderConfig != null) {
        if (orderConfig.getPrimaryApprovers() != null && !orderConfig.getPrimaryApprovers()
            .isEmpty()) {
          for (String s : orderConfig.getPrimaryApprovers()) {
            if (s.equals(userId)) {
              orderApproverModel = new OrderApproverModel();
              orderApproverModel.setApproverType(IApprover.PRIMARY_APPROVER);
              orderApproverModel.setOrderType("t");

            }
          }
        }
        if (orderConfig.getSecondaryApprovers() != null && !orderConfig.getSecondaryApprovers()
            .isEmpty()) {
          for (String s : orderConfig.getSecondaryApprovers()) {
            if (s.equals(userId)) {
              orderApproverModel = new OrderApproverModel();
              orderApproverModel.setApproverType(IApprover.SECONDARY_APPROVER);
              orderApproverModel.setOrderType("t");
            }
          }
        }
      }
    } else {
      Long kioskId = null;
      String oty = "";
      if (IOrder.PURCHASE_ORDER == approvalType) {
        kioskId = order.getKioskId();
        oty = "p";
      } else if (IOrder.SALES_ORDER == approvalType) {
        kioskId = order.getServicingKiosk();
        oty = "s";
      }
      if (kioskId != null) {
        List<IApprover> approvers = entitiesService.getApprovers(kioskId);
        if (approvers != null && !approvers.isEmpty()) {
          for (IApprover apr : approvers) {
            if (userId.equals(apr.getUserId()) && apr.getOrderType().equals(oty)) {
              orderApproverModel = new OrderApproverModel();
              orderApproverModel.setApproverType(apr.getType());
              if (IApprover.PURCHASE_ORDER.equals(apr.getOrderType())) {
                orderApproverModel.setOrderType(apr.getOrderType());
              } else if (IApprover.SALES_ORDER.equals(apr.getOrderType())) {
                orderApproverModel.setOrderType(apr.getOrderType());
              }
            }
          }
        }
      }
    }
    return orderApproverModel;
  }

  private class DemandBatchMeta {
    public BigDecimal quantity;
    List<ShipmentItemBatchModel> bd = new ArrayList<>();

    DemandBatchMeta(BigDecimal quantity) {
      this.quantity = quantity;
    }
  }

}
