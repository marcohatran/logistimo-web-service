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

/**
 *
 */
package com.logistimo.api.controllers;

import com.logistimo.api.action.ClearAllocationAction;
import com.logistimo.api.builders.DemandBuilder;
import com.logistimo.api.builders.DemandItemBuilder;
import com.logistimo.api.builders.DiscrepancyBuilder;
import com.logistimo.api.models.DBDRequestModel;
import com.logistimo.api.models.DemandBreakdownModel;
import com.logistimo.auth.SecurityConstants;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.constants.Constants;
import com.logistimo.entities.auth.EntityAuthoriser;
import com.logistimo.entities.service.EntitiesService;
import com.logistimo.exception.InvalidServiceException;
import com.logistimo.inventory.entity.IInvAllocation;
import com.logistimo.inventory.service.InventoryManagementService;
import com.logistimo.logger.XLog;
import com.logistimo.models.shipments.ShipmentItemBatchModel;
import com.logistimo.orders.service.OrderManagementService;
import com.logistimo.orders.service.impl.DemandService;
import com.logistimo.pagination.Navigator;
import com.logistimo.pagination.PageParams;
import com.logistimo.pagination.Results;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.ObjectNotFoundException;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;
import com.logistimo.services.impl.PMF;
import com.logistimo.utils.LocalDateUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

import javax.jdo.PersistenceManager;
import javax.jdo.Transaction;
import javax.servlet.http.HttpServletRequest;

/**
 * @author charan
 */
@Controller
@RequestMapping("/demand")
public class DemandController {

  private static final XLog xLogger = XLog.getLog(DemandController.class);

  private DemandItemBuilder demandItemBuilder;
  private DemandBuilder demandBuilder;
  private DiscrepancyBuilder discrepancyBuilder;
  private DemandService demandService;
  private OrderManagementService orderManagementService;
  private EntitiesService entitiesService;
  private InventoryManagementService inventoryManagementService;
  private ClearAllocationAction clearAllocationAction;

  @Autowired
  public void setDemandItemBuilder(DemandItemBuilder demandItemBuilder) {
    this.demandItemBuilder = demandItemBuilder;
  }

  @Autowired
  public void setDemandBuilder(DemandBuilder demandBuilder) {
    this.demandBuilder = demandBuilder;
  }

  @Autowired
  public void setDiscrepancyBuilder(DiscrepancyBuilder discrepancyBuilder) {
    this.discrepancyBuilder = discrepancyBuilder;
  }

  @Autowired
  public void setDemandService(DemandService demandService) {
    this.demandService = demandService;
  }

  @Autowired
  public void setOrderManagementService(OrderManagementService orderManagementService) {
    this.orderManagementService = orderManagementService;
  }

  @Autowired
  public void setEntitiesService(EntitiesService entitiesService) {
    this.entitiesService = entitiesService;
  }

  @Autowired
  public void setInventoryManagementService(InventoryManagementService inventoryManagementService) {
    this.inventoryManagementService = inventoryManagementService;
  }

  @Autowired
  public void setClearAllocationAction(
      ClearAllocationAction clearAllocationAction) {
    this.clearAllocationAction = clearAllocationAction;
  }

  @RequestMapping("/")
  public
  @ResponseBody
  Results getDemandList(
      @RequestParam(defaultValue = PageParams.DEFAULT_OFFSET_STR) int offset,
      @RequestParam(defaultValue = PageParams.DEFAULT_SIZE_STR) int size,
      @RequestParam(required = false) String etag,
      @RequestParam(required = false) String tag,
      @RequestParam(required = false) Long kioskId,
      @RequestParam(required = false) Long mId,
      @RequestParam(required = false) Boolean etrn,
      @RequestParam(required = false) Boolean sbo,
      @RequestParam(required = false) String otype) throws ServiceException {
    Long domainId = SecurityUtils.getCurrentDomainId();
    Results idm = null;
    etrn = etrn == null ? false : etrn;
    sbo = sbo == null ? false : sbo;

    try {
      idm = demandService.getDemandItems(domainId, kioskId, mId, etag, tag, etrn, sbo, otype, offset, size);
    } catch (Exception e) {
      xLogger.warn("Error in fetching demand items for domain ", domainId, e);
    }
    return demandBuilder.buildDemandModelList(idm, null, null);
  }

  @RequestMapping(value = "/alloc", method = RequestMethod.POST)
  public
  @ResponseBody
  boolean clearAllocations(
      @RequestParam(required = false) Long kId,
      @RequestParam Long mId,
      @RequestParam(required = false) Long oid,
      @RequestParam(required = false) Boolean etrn,
      @RequestParam(required = false) Boolean bo) throws Exception {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Long domainId = sUser.getDomainId();
    etrn = etrn == null ? false : etrn;
    bo = bo == null ? false : bo;
    ResourceBundle backendMessages = Resources.getBundle(sUser.getLocale());
    try {
      demandService.clearAllocations(kId, mId, oid, etrn, bo);
    } catch (Exception e) {
      xLogger.warn(
          "Error in clearing allocated quantities for domain: {0}, kiosk: {1}, material: {2} ",
          domainId, kId, mId, e);
      throw new InvalidServiceException(backendMessages.getString("inv.failed.clear.allocations"), e);
    }
    return true;
  }

  @RequestMapping("/details")
  public
  @ResponseBody
  Results getDemandDetails(
      @RequestParam Long kId,
      @RequestParam Long mId,
      @RequestParam(required = false) Boolean etrn,
      @RequestParam(required = false) Boolean bo,
      @RequestParam(required = false, defaultValue = "false") Boolean incShip,
      @RequestParam String otype) throws ServiceException, ObjectNotFoundException {
    Results rs = null;
    if (EntityAuthoriser.authoriseEntity(kId)) {
      try {
        etrn = etrn == null ? false : etrn;
        bo = bo == null ? false : bo;
        try {
          rs = demandService.getDemandDetails(kId, mId, etrn, bo, otype, incShip);
        } catch (Exception e) {
          xLogger.warn("Error in getting demand details for entity {0} in domain {1}", kId, e);
        }
        return demandBuilder.buildDemandModelList(rs, kId, mId);
      } catch (Exception e) {
        xLogger.warn("Error while fetching demand details:", e);
        throw new InvalidServiceException("Error while fetching demand details");
      }
    }
    return null;
  }

  @RequestMapping("/allocate/")
  public
  @ResponseBody
  boolean allocateQuantity(@RequestBody DBDRequestModel allocate)
      throws ServiceException {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    String userId = sUser.getUsername();
    DomainConfig dc = DomainConfig.getInstance(sUser.getDomainId());
    ResourceBundle
        backendMessages =
        Resources.getBundle(sUser.getLocale());
    if (dc.autoGI()) {
      for (DemandBreakdownModel dbm : allocate.model) {
        List<ShipmentItemBatchModel> sim = null;
        String
            tag =
            IInvAllocation.Type.ORDER.toString().concat(":").concat(String.valueOf(dbm.orderId));
        PersistenceManager pm = null;
        Transaction tx = null;
        try {
          pm = PMF.get().getPersistenceManager();
          tx = pm.currentTransaction();
          tx.begin();
          if (dbm.bid != null) {
            ShipmentItemBatchModel sibm = new ShipmentItemBatchModel();
            sibm.id = dbm.bid;
            sibm.q = dbm.bQty;
            sibm.smst = dbm.mst;
            dbm.oQty = null;
            sim = new ArrayList<>(1);
            sim.add(sibm);
          }
          inventoryManagementService.allocate(dbm.kId, dbm.matId, IInvAllocation.Type.ORDER, String.valueOf(dbm.orderId),
              tag, dbm.oQty, sim, userId, pm, dbm.mst);

          tx.commit();
        } catch (Exception e) {
          throw new InvalidServiceException(backendMessages.getString("inv.alloc.failed"), e);
        } finally {
          if (tx != null && tx.isActive()) {
            tx.rollback();
          }
          if (pm != null) {
            pm.close();
          }
        }
      }
    }
    return true;
  }

  @RequestMapping("/entity/{entityId}")
  public
  @ResponseBody
  Results getEntityOrders(
      @PathVariable Long entityId,
      @RequestParam(defaultValue = PageParams.DEFAULT_OFFSET_STR) int offset,
      @RequestParam(defaultValue = PageParams.DEFAULT_SIZE_STR) int size,
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String etag,
      @RequestParam(required = false) String tag,
      HttpServletRequest request) {
    return getDemandBoard(entityId, null, etag, tag, from, offset, size, request);
  }

  @RequestMapping("/material/{mid}")
  public
  @ResponseBody
  Results getMaterialOrders(
      @PathVariable Long mid,
      @RequestParam(defaultValue = PageParams.DEFAULT_OFFSET_STR) int offset,
      @RequestParam(defaultValue = PageParams.DEFAULT_SIZE_STR) int size,
      @RequestParam(defaultValue = "false") boolean noDups,
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String etag,
      @RequestParam(required = false) String tag,
      HttpServletRequest request) {
    return getDemandBoard(null, mid, etag, tag, from, offset, size, request, noDups);
  }


  public Results getDemandBoard(Long kioskId, Long materialId, String kioskTag, String materialTag,
                                String from, int offset, int size, HttpServletRequest request) {
    return getDemandBoard(kioskId, materialId, kioskTag, materialTag, from, offset, size, request,
        false);
  }

  public Results getDemandBoard(Long kioskId, Long materialId, String kioskTag, String materialTag,
                                String from, int offset, int size, HttpServletRequest request,
                                boolean skipDuplicates) {
    SecureUserDetails user = SecurityUtils.getUserDetails();
    Long domainId = user.getCurrentDomainId();
    DomainConfig dc = DomainConfig.getInstance(domainId);
    Navigator
        navigator =
        new Navigator(request.getSession(), "DemandBoardController.getDemandBoard", offset, size,
            "dummy", 0);
    PageParams pageParams = new PageParams(navigator.getCursor(offset), offset, size);
    try {
      Date startDate = null;
      if (from != null && !from.isEmpty()) {
        try {
          startDate = LocalDateUtil.parseCustom(from, Constants.DATE_FORMAT, dc.getTimezone());
        } catch (ParseException e) {
          xLogger.warn("Invalid date:", e);
        }
      }
      Results results =
          orderManagementService.getDemandItems(kioskId == null ? domainId : null, kioskId, materialId, kioskTag,
              materialTag, startDate, pageParams);
      String cursor = results.getCursor();
      if (cursor != null) {
        navigator.setResultParams(results);
      }
      results.setOffset(offset);
      results.setNumFound(-1);
      return demandItemBuilder.buildDemandItems(results, user, skipDuplicates);
    } catch (ServiceException e) {
      xLogger.severe("Error in fetching domain board");
      throw new InvalidServiceException(e.getMessage());
    } catch (ObjectNotFoundException e) {
      throw new InvalidServiceException(e.getMessage());
    }
  }

  @RequestMapping("/discrepancies")
  public
  @ResponseBody
  Results getDiscrepanices(
      @RequestParam(defaultValue = PageParams.DEFAULT_OFFSET_STR) int offset,
      @RequestParam(defaultValue = PageParams.DEFAULT_SIZE_STR) int size,
      @RequestParam(required = false) String oType,
      @RequestParam(required = false) Boolean etrn,
      @RequestParam(required = false) String entityId,
      @RequestParam(required = false) String materialId,
      @RequestParam(required = false) String eTag,
      @RequestParam(required = false) String mTag,
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to,
      @RequestParam(required = false) String orderId,
      @RequestParam(required = false) String discType,
      HttpServletRequest request) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Long domainId = sUser.getCurrentDomainId();
    DomainConfig dc = DomainConfig.getInstance(domainId);
    Results results;
    Long eId = null, mId = null, oId = null;
    List<Long> kioskIds = null;
    etrn = etrn == null ? false : etrn;

    try {
      if (sUser.getUsername() != null) {
        if (SecurityConstants.ROLE_SERVICEMANAGER.equals(sUser.getRole())) {
          kioskIds = entitiesService.getKioskIdsForUser(sUser.getUsername(), null, null).getResults();
          if (kioskIds == null || kioskIds.isEmpty()) {
            return new Results();
          }
        }
      }
      if (entityId != null) {
        try {
          eId = Long.parseLong(entityId);
        } catch (NumberFormatException e) {
          xLogger.warn("Exception while parsing entityId {0} while getting discrepancies", entityId,
              e);
        }
      }
      if (materialId != null) {
        try {
          mId = Long.parseLong(materialId);
        } catch (NumberFormatException e) {
          xLogger.warn("Exception while parsing materialId {0} while getting discrepancies",
              materialId, e);
        }
      }
      Date fromDate = null, toDate = null;
      if (from != null && !from.isEmpty()) {
        try {
          fromDate = LocalDateUtil.parseCustom(from, Constants.DATE_FORMAT, dc.getTimezone());
        } catch (Exception e) {
          xLogger.warn("Exception when parsing start date while getting discrepancies{0}", from, e);
        }
      }
      if (to != null && !to.isEmpty()) {
        try {
          toDate = LocalDateUtil.parseCustom(to, Constants.DATE_FORMAT, dc.getTimezone());
        } catch (Exception e) {
          xLogger.warn("Exception when parsing start date while getting discrepancies {0}", to, e);
        }
      }
      if (orderId != null) {
        try {
          oId = Long.parseLong(orderId);
        } catch (NumberFormatException e) {
          xLogger
              .warn("Exception while parsing orderId {0} while getting discrepancies", orderId, e);
        }
      }

      Navigator
          navigator =
          new Navigator(request.getSession(), "DemandController.getDiscrepanices", offset, size,
              "dummy", 0);
      PageParams pageParams = new PageParams(navigator.getCursor(offset), offset, size);
      results =
          demandService.getDemandItemsWithDiscrepancies(domainId, oType, etrn, eId, kioskIds, mId, eTag, mTag,
              fromDate, toDate, oId, discType, pageParams);
      results.setOffset(offset);
      return discrepancyBuilder.buildDiscrepancyModels(results);
    } catch (Exception e) {
      xLogger.severe("Failed to get Demand discrepancy data", e);
      throw new InvalidServiceException("System error occurred while fetching discrepancies");
    }
  }

  @RequestMapping(value = "/clear-allocations", method = RequestMethod.GET)
  public
  @ResponseBody
  void clearAllocations() {
    //set system user as this is executed as background task
    SecurityUtils.setSystemUser();
    clearAllocationAction.invoke();
  }
}
