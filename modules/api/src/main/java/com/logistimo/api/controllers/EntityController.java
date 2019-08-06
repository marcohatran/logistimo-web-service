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

package com.logistimo.api.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import com.logistimo.AppFactory;
import com.logistimo.api.builders.EntityBuilder;
import com.logistimo.api.builders.InventoryBuilder;
import com.logistimo.api.builders.StockBoardBuilder;
import com.logistimo.api.builders.UserBuilder;
import com.logistimo.api.models.EntityApproversModel;
import com.logistimo.api.models.EntityDomainModel;
import com.logistimo.api.models.EntityModel;
import com.logistimo.api.models.EntitySummaryModel;
import com.logistimo.api.models.InventoryModel;
import com.logistimo.api.models.PermissionModel;
import com.logistimo.api.models.RelationModel;
import com.logistimo.api.models.StockBoardModel;
import com.logistimo.api.request.AddMaterialsRequestObj;
import com.logistimo.api.request.EditMaterialsReqObj;
import com.logistimo.api.request.NetworkViewResponseObj;
import com.logistimo.api.request.RemoveMaterialsRequestObj;
import com.logistimo.api.request.ReorderEntityRequestObj;
import com.logistimo.api.util.DomainRemover;
import com.logistimo.api.util.SearchUtil;
import com.logistimo.auth.GenericAuthoriser;
import com.logistimo.auth.SecurityConstants;
import com.logistimo.auth.SecurityUtil;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.config.entity.IConfig;
import com.logistimo.config.models.ApprovalsConfig;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.config.models.InventoryConfig;
import com.logistimo.config.models.KioskConfig;
import com.logistimo.config.models.Permissions;
import com.logistimo.config.models.StockboardConfig;
import com.logistimo.config.service.ConfigurationMgmtService;
import com.logistimo.constants.CharacterConstants;
import com.logistimo.constants.Constants;
import com.logistimo.dao.JDOUtils;
import com.logistimo.domains.entity.IDomain;
import com.logistimo.domains.service.DomainsService;
import com.logistimo.entities.auth.EntityAuthoriser;
import com.logistimo.entities.entity.IApprover;
import com.logistimo.entities.entity.IKiosk;
import com.logistimo.entities.entity.IKioskLink;
import com.logistimo.entities.models.EntityLinkModel;
import com.logistimo.entities.models.LocationSuggestionModel;
import com.logistimo.entities.models.UserEntitiesModel;
import com.logistimo.entities.service.EntitiesService;
import com.logistimo.entities.utils.EntityDomainUpdater;
import com.logistimo.entities.utils.EntityMover;
import com.logistimo.exception.BadRequestException;
import com.logistimo.exception.ForbiddenAccessException;
import com.logistimo.exception.InvalidServiceException;
import com.logistimo.exception.TaskSchedulingException;
import com.logistimo.inventory.entity.IInvntry;
import com.logistimo.inventory.service.InventoryManagementService;
import com.logistimo.logger.XLog;
import com.logistimo.models.ICounter;
import com.logistimo.models.superdomains.DomainSuggestionModel;
import com.logistimo.pagination.Navigator;
import com.logistimo.pagination.PageParams;
import com.logistimo.pagination.Results;
import com.logistimo.reports.service.ReportsService;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.ObjectNotFoundException;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;
import com.logistimo.services.cache.MemcacheService;
import com.logistimo.services.impl.PMF;
import com.logistimo.services.taskqueue.ITaskService;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.users.service.UsersService;
import com.logistimo.utils.Counter;
import com.logistimo.utils.GsonUtils;
import com.logistimo.utils.LocalDateUtil;
import com.logistimo.utils.MsgUtil;
import com.logistimo.utils.QueryUtil;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/entities")
public class EntityController {

  private static final XLog xLogger = XLog.getLog(EntityController.class);
  private static final String CREATEENTITY_TASK_URL = "/task/createentity";
  private static final String ADD_MATERIALS_TASKS = "/s2/api/entities/materials/";
  private static final String CREATE_ENTITY_TASK_URL = "/task/createentity";
  private static final String MOVE_ENTITY_TASK_URL = "/s2/api/entities/move";
  private static final String DOMAIN_ENTITY_TASK_URL = "/s2/api/entities/domainupdate";

  private EntityBuilder entityBuilder;
  private InventoryBuilder inventoryBuilder;
  private UserBuilder userBuilder;
  private StockBoardBuilder stockBoardBuilder;
  private EntitiesService entitiesService;
  private UsersService usersService;
  private InventoryManagementService inventoryManagementService;
  private ConfigurationMgmtService configurationMgmtService;
  private DomainsService domainsService;

  @Autowired ReportsService reportsService;

  @Autowired
  public void setEntityBuilder(EntityBuilder entityBuilder) {
    this.entityBuilder = entityBuilder;
  }

  @Autowired
  public void setInventoryBuilder(InventoryBuilder inventoryBuilder) {
    this.inventoryBuilder = inventoryBuilder;
  }

  @Autowired
  public void setUserBuilder(UserBuilder userBuilder) {
    this.userBuilder = userBuilder;
  }

  @Autowired
  public void setStockBoardBuilder(StockBoardBuilder stockBoardBuilder) {
    this.stockBoardBuilder = stockBoardBuilder;
  }

  @Autowired
  public void setEntitiesService(EntitiesService entitiesService) {
    this.entitiesService = entitiesService;
  }

  @Autowired
  public void setUsersService(UsersService usersService) {
    this.usersService = usersService;
  }

  @Autowired
  public void setInventoryManagementService(InventoryManagementService inventoryManagementService) {
    this.inventoryManagementService = inventoryManagementService;
  }

  @Autowired
  public void setConfigurationMgmtService(ConfigurationMgmtService configurationMgmtService) {
    this.configurationMgmtService = configurationMgmtService;
  }

  @Autowired
  public void setDomainsService(DomainsService domainsService) {
    this.domainsService = domainsService;
  }

  @RequestMapping(value = "/delete", method = RequestMethod.POST)
  public
  @ResponseBody
  String delete(@RequestBody String entityIds) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    Long domainId = sUser.getCurrentDomainId();
    int delCnt = 0;
    StringBuilder entitiesNames = new StringBuilder();
    String[] entitiesArray = entityIds.split(",");
    Map<String, String> params = new HashMap<>();
    params.put("action", "remove");
    params.put("type", "kiosk");
    params.put("domainid", domainId.toString());
    params.put("execute", "true");
    params.put("sourceuser", sUser.getUsername());
    for (String entityId : entitiesArray) {
      params.put("kioskid", entityId);
      try {
        IKiosk k = entitiesService.getKiosk(Long.parseLong(entityId));
        entitiesNames.append(k.getName()).append(",");// getting entity names is just for logging
        AppFactory.get().getTaskService()
            .schedule(ITaskService.QUEUE_DEFAULT, CREATE_ENTITY_TASK_URL, params,
                ITaskService.METHOD_POST);
      } catch (Exception e) {
        xLogger.warn("{0} when scheduling task to delete kiosk {1} in domain {2}: {3}",
            e.getClass().getName(), entityId, domainId, e.getMessage());
      }
      delCnt++;
    }
    if (entitiesNames.length() > 0) {
      entitiesNames.setLength(entitiesNames.length() - 1);//for auditlog removing last comma
    }
    xLogger.info("AUDITLOG\t{0}\t{1}\tENTITY\t " +
        "DELETE\t{2}\t{3}", domainId, sUser.getUsername(), entityIds, entitiesNames.toString());
    return backendMessages.getString("schedule.task.remove.success") + " " + delCnt + " "
        + backendMessages.getString("entity.removal.time");
  }

  @RequestMapping(value = "/", method = RequestMethod.POST)
  public
  @ResponseBody
  String create(@RequestBody EntityModel entityModel) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    IKiosk k =
        entityBuilder.buildKiosk(entityModel, sUser.getUsername(), true);
    try {
      if (k.getName() != null) {
        long domainId = sUser.getCurrentDomainId();
        entitiesService.addKiosk(domainId, k);
        xLogger.info("AUDITLOG\t{0}\t{1}\tENTITY\t " +
            "CREATE\t{2}\t{3}", domainId, sUser.getUsername(), k.getKioskId(), k.getName());
      }
    } catch (ServiceException e) {
      xLogger.warn("Error creating Entity in domain:  " + k.getDomainId(), e);
      throw new InvalidServiceException(
          backendMessages.getString("entity.create.error") + " " + MsgUtil.bold(entityModel.nm) +
              MsgUtil.addErrorMsg(e.getMessage()));
    }
    return backendMessages.getString("kiosk") + " " + MsgUtil.bold(entityModel.nm) + " "
        + backendMessages.getString("create.success");
  }

  @RequestMapping(value = "/approvers", method = RequestMethod.POST)
  public
  @ResponseBody
  String setApprovers(@RequestBody EntityApproversModel model)
      throws ServiceException {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    try{
      IKiosk kiosk = entitiesService.getKiosk(model.entityId);
      List<IApprover> approversList =
          entityBuilder.buildApprovers(model, sUser.getUsername(), kiosk.getDomainId());
      entitiesService.addApprovers(model.entityId, approversList, sUser.getUsername());
      } catch (ServiceException se) {
    throw new InvalidServiceException(
        backendMessages.getString("kiosk.detail.fetch.error") + " " + model.entityId, se);
  }
    return backendMessages.getString("approvers.update.success");
  }

  @RequestMapping(value = "/approvers", method = RequestMethod.GET)
  public
  @ResponseBody
  EntityApproversModel getApprovers(@RequestParam Long kioskId) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    EntityApproversModel model = null;
    List<IApprover> approversList = entitiesService.getApprovers(kioskId);
    if (approversList != null && approversList.size() > 0) {
      model = entityBuilder.buildApprovalsModel(approversList, locale, sUser.getTimezone());

    }
    return model;
  }

  @RequestMapping(value = "/update", method = RequestMethod.POST)
  public
  @ResponseBody
  String updateEntity(@RequestBody EntityModel entityModel) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    Long domainId = sUser.getCurrentDomainId();
    try {
      IKiosk ksk = entitiesService.getKiosk(entityModel.id, false);
      if (entityModel.be != ksk.isBatchMgmtEnabled()) {
        if (!inventoryManagementService.validateEntityBatchManagementUpdate(ksk.getKioskId())) {
          return null;
        }
      }
      IKiosk k = entityBuilder.buildKiosk(entityModel, sUser.getUsername(), ksk, false);

      if (k.getName() != null) {
        if (!EntityAuthoriser.authoriseEntity(k.getKioskId())) {
          throw new ForbiddenAccessException(backendMessages.getString("permission.denied"));
        }
        entitiesService.updateKiosk(k, domainId);
        xLogger.info("AUDITLOG\t{0}\t{1}\tENTITY\t " +
            "UPDATE\t{2}\t{3}", domainId, sUser.getUsername(), k.getKioskId(), k.getName());
      }
    } catch (ServiceException e) {
      xLogger.warn("Error updating Entity " + entityModel.nm);
      throw new InvalidServiceException(
          backendMessages.getString("kiosk.update.error") + " " + MsgUtil.bold(entityModel.nm) +
              MsgUtil.addErrorMsg(e.getMessage()));
    }
    return backendMessages.getString("kiosk") + " " + MsgUtil.bold(entityModel.nm) + " "
        + backendMessages.getString("updated.success");
  }

  @RequestMapping(value = "/entity/{entityId}", method = RequestMethod.GET)
  public
  @ResponseBody
  EntityModel getEntityById(@PathVariable Long entityId,
                            @RequestParam(required = false) Boolean skipAuthCheck) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    EntityModel model;
    try {
      Integer permission = EntityAuthoriser.authoriseEntityPerm(entityId);
      if ((skipAuthCheck == null || !skipAuthCheck) && GenericAuthoriser.NO_ACCESS.equals(permission)) {
        throw new ForbiddenAccessException(backendMessages.getString("permission.denied"));
      }
      IKiosk k = entitiesService.getKiosk(entityId);
      IUserAccount u = null;
      IUserAccount lu = null;
      boolean found = false;
      boolean isPa = false;
      boolean isSa = false;
      ApprovalsConfig.OrderConfig orderConfig;
      if (k != null) {
        try {
          u = usersService.getUserAccount(k.getRegisteredBy());
        } catch (Exception e) {
          xLogger.warn("Error while getting entity, fetching user details for {0}: {1}",
              k.getRegisteredBy(), e.getMessage());
        }
        try {
          if (k.getUpdatedBy() != null) {
            lu = usersService.getUserAccount(k.getUpdatedBy());
          }
        } catch (Exception e) {
          xLogger.warn("Error while getting entity, fetching user details for {0}: {1}",
              k.getUpdatedBy(), e.getMessage());
        }
        try {
          DomainConfig dc = DomainConfig.getInstance(k.getDomainId());
          ApprovalsConfig ac = dc.getApprovalsConfig();
          orderConfig = ac.getOrderConfig();
          if(orderConfig.getPurchaseSalesOrderApproval() != null && orderConfig.getPurchaseSalesOrderApproval().size() > 0) {
            List<ApprovalsConfig.PurchaseSalesOrderConfig> psoa = orderConfig.getPurchaseSalesOrderApproval();
            List<String> currentEntityTags = k.getTags();
            if(currentEntityTags != null && currentEntityTags.size() > 0) {
              for(ApprovalsConfig.PurchaseSalesOrderConfig ps : psoa) {
                List<String> eTags = ps.getEntityTags();
                for(String eTag : eTags) {
                  if(currentEntityTags.contains(eTag)) {
                    found = true;
                    if(ps.isPurchaseOrderApproval()) {
                      isPa = true;
                    }
                    if(ps.isSalesOrderApproval()) {
                      isSa = true;
                    }
                    break;
                  }
                }
              }
            }
          }
        } catch (Exception e) {
          //do nothing
        }
      }
      model = entityBuilder.buildModel(k, u, lu, locale, sUser.getTimezone());
      model.perm = permission;
      model.appr = found;
      model.ipa = isPa;
      model.isa = isSa;
    } catch (ServiceException se) {
      xLogger.warn("Error fetching Entity details for " + entityId, se);
      throw new InvalidServiceException(
          backendMessages.getString("kiosk.detail.fetch.error") + " " + entityId);
    }
    return model;
  }

  @RequestMapping(value = "/entity/linkscount/{entityId}", method = RequestMethod.GET)
  public
  @ResponseBody
  String getLinksCountByEntityId(@PathVariable Long entityId,
                                 @RequestParam(required = false) String q,
                                 @RequestParam(required = false) Long linkedEntityId,
                                 @RequestParam(required = false) String entityTag) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    int customerCount;
    int vendorsCount;
    try {
      if (!EntityAuthoriser.authoriseEntity(entityId)) {
        throw new ForbiddenAccessException(backendMessages.getString("permission.denied"));
      }
      PageParams pageParams = new PageParams(1);
      customerCount =
          entitiesService.getKioskLinks(entityId, IKioskLink.TYPE_CUSTOMER, null, q, pageParams, true, linkedEntityId, entityTag)
              .getNumFound();
      vendorsCount =
          entitiesService.getKioskLinks(entityId, IKioskLink.TYPE_VENDOR, null, q, pageParams, true, linkedEntityId, entityTag)
              .getNumFound();
    } catch (ServiceException e) {
      xLogger.warn("Error in fetching Counts for " + entityId, e);
      throw new InvalidServiceException(
          backendMessages.getString("counts.fetch.error") + " " + entityId);
    }
    return customerCount + "," + vendorsCount;
  }

  @RequestMapping(value = "/entity/{entityId}/users", method = RequestMethod.GET)
  public
  @ResponseBody
  Results getEntityUsers(@PathVariable Long entityId,
                         @RequestParam(required = false) Long srcEntityId) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    try {
      //TODO Extend behavior to check if the requested entity has a relation to user's entities. Temporarily commented.
      if (!EntityAuthoriser.authoriseEntityDomain(sUser, entityId, SecurityUtils.getDomainId())) {
        try {
          if (srcEntityId == null || (!EntityAuthoriser.authoriseEntityDomain(sUser, srcEntityId, SecurityUtils.getDomainId())
              && entitiesService.getKioskLink(srcEntityId, IKioskLink.TYPE_VENDOR, entityId) == null)) {
            throw new ForbiddenAccessException(backendMessages.getString("permission.denied"));
          }
        } catch (ObjectNotFoundException e) {
          throw new ForbiddenAccessException(backendMessages.getString("permission.denied"));
        }
      }
      IKiosk k = entitiesService.getKiosk(entityId);
      return userBuilder.buildUsers(new Results(k.getUsers(), null), sUser, false);
    } catch (ServiceException se) {
      xLogger.warn("Error fetching Entity details for " + entityId, se);
      throw new InvalidServiceException(
          backendMessages.getString("kiosk.detail.fetch.error") + " " + entityId);
    }
  }

  @RequestMapping(value = "/entity/{entityId}/customers", method = RequestMethod.GET)
  public
  @ResponseBody
  Results getEntityCustomers(@PathVariable Long entityId,
                             @RequestParam(required = false) String q,
                             @RequestParam(required = false) String size,
                             @RequestParam(required = false) String offset,
                             @RequestParam(required = false) Long linkedEntityId,
                             @RequestParam(required = false) String entityTag,
                             HttpServletRequest request) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    String timezone = sUser.getTimezone();
    try {
      if (!EntityAuthoriser.authoriseEntity(entityId)) {
        throw new ForbiddenAccessException(backendMessages.getString("permission.denied"));
      }
      PageParams pageParams = null;
      int o = offset == null ? 0 : Integer.parseInt(offset);
      if (size != null && Integer.parseInt(size) > 0 || o > 0) {
        int s = size == null ? PageParams.DEFAULT_SIZE : Integer.parseInt(size);
        Navigator navigator =
            new Navigator(request.getSession(), "EntityController.getEntityCustomers", o, s,
                "custEntity", 0);
        pageParams = new PageParams(navigator.getCursor(o), o, s);
      }
      Results results =
          entitiesService.getKioskLinks(entityId, IKioskLink.TYPE_CUSTOMER, null, q, pageParams, false, linkedEntityId,
              entityTag);
      List customers = results.getResults();
      return new Results<>(entityBuilder.buildEntityLinks(customers, locale, timezone,
          sUser.getUsername(),
          sUser.getDomainId(), sUser.getRole()), null, results.getNumFound(), o);
    } catch (ServiceException se) {
      xLogger.warn("Error fetching Entity Customers for " + entityId, se);
      throw new InvalidServiceException(
          backendMessages.getString("kioskcustomer.fetch.error") + " " + entityId);
    }
  }

  @RequestMapping(value = "/{entityId}/relationship", method = RequestMethod.GET)
  public
  @ResponseBody
  Integer getEntityRelationship(@PathVariable Long entityId) {
    Integer ty = -1;
    try {
      List customer = entitiesService.getKioskLinks(entityId, IKioskLink.TYPE_CUSTOMER, null, null, null).getResults();
      List vendor = entitiesService.getKioskLinks(entityId, IKioskLink.TYPE_VENDOR, null, null, null).getResults();
      if (customer.size() != 0 && vendor.size() == 0) {
        ty = 0;
      } else if (vendor.size() != 0 && customer.size() == 0) {
        ty = 1;
      }
    } catch (Exception e) {
      xLogger.warn("Error in fetching entities Customer/Vendor" + entityId);
    }
    return ty;
  }


  @RequestMapping(value = "/entity/{entityId}/vendors", method = RequestMethod.GET)
  public
  @ResponseBody
  Results getEntityVendors(@PathVariable Long entityId,
                           @RequestParam(required = false) String size,
                           @RequestParam(required = false) String offset,
                           @RequestParam(required = false) String q,
                           @RequestParam(required = false) Long linkedEntityId,
                           @RequestParam(required = false) String entityTag,
                           HttpServletRequest request) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    String timezone = sUser.getTimezone();
    try {
      //TODO: Temporary fix. Required for Order listing on a sales order.
      if (!EntityAuthoriser
          .authoriseEntityDomain(sUser, entityId, SecurityUtils.getDomainId())) {
        throw new ForbiddenAccessException(backendMessages.getString("permission.denied"));
      }

      PageParams pageParams = null;
      int o = offset == null ? 0 : Integer.parseInt(offset);
      if (size != null && Integer.parseInt(size) > 0 || o > 0) {
        int s = size == null ? PageParams.DEFAULT_SIZE : Integer.parseInt(size);
        Navigator navigator =
            new Navigator(request.getSession(), Constants.CURSOR_KIOSKLINKS, o, s, "custEntity", 0);
        pageParams = new PageParams(navigator.getCursor(o), o, s);
      }
      Results results =
          entitiesService.getKioskLinks(entityId, IKioskLink.TYPE_VENDOR, null, q, pageParams, false, linkedEntityId, entityTag);
      List vendors = results.getResults();
      return new Results<>(entityBuilder
          .buildEntityLinks(vendors, locale, timezone, sUser.getUsername(),
              sUser.getDomainId(),
              sUser.getRole()), null, results.getNumFound(), o);
    } catch (ServiceException se) {
      xLogger.warn("Error fetching Entity Vendors for " + entityId, se);
      throw new InvalidServiceException(
          backendMessages.getString("kioskvendors.fetch.error") + " " + entityId);
    }
  }


  @RequestMapping(value = "/", method = RequestMethod.GET)
  public
  @ResponseBody
  Results getAll(
      @RequestParam(required = false) String tag,
      @RequestParam(required = false) String extag,
      @RequestParam(defaultValue = PageParams.DEFAULT_OFFSET_STR) int offset,
      @RequestParam(defaultValue = PageParams.DEFAULT_SIZE_STR) int size,
      @RequestParam(required = false) String q,
      @RequestParam(required = false) Long linkedEntityId,
      HttpServletRequest request) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    String userId = sUser.getUsername();
    Long domainId = sUser.getCurrentDomainId();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    Results kioskResults;
    Navigator
        navigator =
        new Navigator(request.getSession(), "EntityController.getAll", offset, size, "dummy", 0);
    PageParams pageParams = new PageParams(navigator.getCursor(offset), offset, size);
    try {
      IUserAccount user = usersService.getUserAccount(userId);
      if (linkedEntityId == null && StringUtils.isNotBlank(q)) {
        kioskResults = SearchUtil.findKiosks(domainId, q, pageParams, user);
      } else if(linkedEntityId != null) {
        List<IKiosk> kiosk = new ArrayList<>();
        kiosk.add(entitiesService.getKiosk(linkedEntityId));
        kioskResults = new Results<>(kiosk, null);
      } else {
        kioskResults = entitiesService.getKiosks(user, domainId, tag, extag, pageParams);
      }
      if (StringUtils.isBlank(q) && linkedEntityId == null
          && SecurityUtil.compareRoles(user.getRole(), SecurityConstants.ROLE_DOMAINOWNER) >= 0) {
        ICounter counter = Counter.getKioskCounter(domainId, tag, extag);
        kioskResults.setNumFound(counter.getCount());
      } else {
        kioskResults.setNumFound(-1);
      }
      navigator.setResultParams(kioskResults);
    } catch (ServiceException | ObjectNotFoundException se) {
      xLogger.warn("Error fetching entities for domain" + domainId, se);
      throw new InvalidServiceException(
          backendMessages.getString("domain.kiosks.fetch.error") + " " + domainId);
    }
    return entityBuilder.buildEntityResults(kioskResults, locale, sUser.getTimezone());
  }

  @RequestMapping(value = "/domain", method = RequestMethod.GET)
  public
  @ResponseBody
  Results getDomainEntities(
      @RequestParam(required = false) String tag,
      @RequestParam(required = false) String extag,
      @RequestParam(defaultValue = PageParams.DEFAULT_OFFSET_STR) int offset,
      @RequestParam(defaultValue = PageParams.DEFAULT_SIZE_STR) int size,
      @RequestParam(required = false) String q,
      HttpServletRequest request) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Long domainId = sUser.getCurrentDomainId();
    Locale locale = sUser.getLocale();
    Results kioskResults;
    Navigator navigator =
        new Navigator(request.getSession(), "EntityController.getDomainEntities", offset, size,
            "dummy", 0);
    PageParams pageParams = new PageParams(navigator.getCursor(offset), offset, size);
    try {
      IUserAccount user = usersService.getUserAccount(sUser.getUsername());
      if (StringUtils.isNotBlank(q)) {
        kioskResults = SearchUtil.findDomainKiosks(domainId, q, pageParams, user);
        kioskResults.setNumFound(-1);
      } else {
        if (SecurityUtil.compareRoles(user.getRole(), SecurityConstants.ROLE_DOMAINOWNER) >= 0) {
          kioskResults = entitiesService.getAllDomainKiosks(domainId, tag, extag, pageParams);
          kioskResults.setNumFound(Counter.getDomainKioskCounter(domainId).getCount());
        } else {
          UserEntitiesModel userModel =
              entitiesService.getUserWithKiosks(sUser.getUsername());
          kioskResults = new Results<>(userModel.getKiosks(), null, userModel.getKiosks().size(), 0);
        }
      }
      navigator.setResultParams(kioskResults);
    } catch (ServiceException | ObjectNotFoundException se) {
      xLogger.warn("Error fetching entities for domain" + domainId, se);
      ResourceBundle backendMessages = Resources.getBundle(locale);
      throw new InvalidServiceException(
          backendMessages.getString("domain.kiosks.fetch.error") + " " + domainId);
    }
    return entityBuilder.buildEntityResults(kioskResults, locale, sUser.getTimezone());
  }

  @RequestMapping(value = "/filter", method = RequestMethod.GET)
  public
  @ResponseBody
  Results getFilteredEntity(@RequestParam String text,
                            @RequestParam(required = false, defaultValue = "false") boolean sdOnly) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Long domainId = sUser.getCurrentDomainId();
    if (text == null) {
      text = CharacterConstants.EMPTY;
    }
    text = text.toLowerCase();
    List<IKiosk> kiosks = new ArrayList<>();
    PersistenceManager pm = null;
    try {
      IUserAccount user = usersService.getUserAccount(sUser.getUsername());
      if (SecurityUtil.compareRoles(user.getRole(), SecurityConstants.ROLE_DOMAINOWNER) < 0) {
        Results results = entitiesService.getKiosksForUser(user, null, null);
        if(results.getResults() != null) {
          for (Object kiosk : results.getResults()) {
            if (((IKiosk) kiosk).getName().toLowerCase().startsWith(text)) {
              kiosks.add(((IKiosk) kiosk));
            }
          }
        }
      } else {
        pm = PMF.get().getPersistenceManager();
        String filter = "dId.contains(domainIdParam)";
        if (sdOnly) {
          filter = "sdId == domainIdParam";
        }
        String declaration = "Long domainIdParam";
        Map<String, Object> params = new HashMap<>();
        params.put("domainIdParam", domainId);
        if (!text.isEmpty()) {
          filter += " && nName.startsWith(txtParam)";
          declaration += ", String txtParam";
          params.put("txtParam", text);
        }
        Query query = pm.newQuery(JDOUtils.getImplClass(IKiosk.class));
        query.setFilter(filter);
        query.declareParameters(declaration);
        query.setOrdering("nName asc");
        PageParams pageParams = new PageParams(null, 0, 10);
        QueryUtil.setPageParams(query, pageParams);
        try {
          kiosks = (List<IKiosk>) query.executeWithMap(params);
          kiosks = (List<IKiosk>) pm.detachCopyAll(kiosks);
        } finally {
          query.closeAll();
        }
      }
    } catch (Exception e) {
      xLogger.warn("Exception: {0}", e.getMessage());
    } finally {
      if (pm != null) {
        pm.close();
      }
    }
    return new Results<>(entityBuilder.buildFilterModelList(kiosks), QueryUtil.getCursor(kiosks));
  }

  @RequestMapping(value = "/addrelation", method = RequestMethod.POST)
  public
  @ResponseBody
  String addRelation(@RequestBody RelationModel model) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    String userId = sUser.getUsername();
    Long domainId = sUser.getCurrentDomainId();
    StringBuilder linkNames = new StringBuilder();
    int cnt;
    try {
      Date now = new Date();
      IKiosk k = entitiesService.getKiosk(model.entityId);
      List<IKioskLink> links = new ArrayList<>();
      for (String linkId : model.linkIds) {
        IKioskLink link = JDOUtils.createInstance(IKioskLink.class);
        link.setDomainId(domainId);
        link.setCreatedBy(userId);
        link.setCreatedOn(now);
        link.setDescription(model.desc);
        link.setKioskId(model.entityId);
        link.setLinkType(model.linkType);
        link.setLinkedKioskId(Long.parseLong(linkId));
        if (StringUtils.isNotBlank(String.valueOf(model.cl))) {
          link.setCreditLimit(model.cl);
        }
        links.add(link);
        linkNames.append(entitiesService.getKiosk(Long.parseLong(linkId)).getName()).append(",");
      }
      entitiesService.addKioskLinks(domainId, links);

      cnt = links.size();
      if (linkNames.length() > 0) {
        linkNames.setLength(linkNames.length() - 1);//for auditlog removing last comma
      }
      xLogger.info("AUDITLOG\t{0}\t{1}\tENTITY\t " +
              "ADD RELATION \t{2}\t{3}\tLINKTYPE={4}", domainId, sUser.getUsername(), k.getName(),
          linkNames.toString(), model.linkType);
    } catch (ServiceException e) {
      xLogger.warn("Error adding Relationship for " + model.entityId, e);
      throw new InvalidServiceException(
          backendMessages.getString("relation.add.error") + " " + model.entityId);
    }
    return cnt + " " + backendMessages.getString("relation.add.success");
  }

  @RequestMapping(value = "/updaterelation", method = RequestMethod.POST)
  public
  @ResponseBody
  String updateRelation(@RequestBody RelationModel model) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    String role = sUser.getRole();
    Long domainId = sUser.getCurrentDomainId();
    String linkedKioskName;
    try {
      IKioskLink link = entitiesService.getKioskLink(model.linkIds[0]);
      IKiosk k = entitiesService.getKiosk(model.entityId);
      linkedKioskName = entitiesService.getKiosk(link.getKioskId()).getName();
      if (SecurityConstants.ROLE_SERVICEMANAGER.equals(role)) {
        boolean hasPermission = false;
        List<Long> eIds = entitiesService.getKioskIdsForUser(sUser.getUsername(), null, null).getResults();
        if (eIds != null) {
          for (Long eId : eIds) {
            if (eId.equals(link.getLinkedKioskId())) {
              hasPermission = true;
              break;
            }
          }
        }
        if (!hasPermission) {
          Permissions perm = k.getPermissions();
          if (perm != null && !perm
              .hasAccess(model.linkType, Permissions.MASTER, Permissions.OP_MANAGE)) {
            throw new ForbiddenAccessException(backendMessages.getString("permission.denied"));
          }
        }
      }
      link.setDescription(model.desc);
      if (StringUtils.isNotBlank(String.valueOf(model.cl))) {
        link.setCreditLimit(model.cl);
      }
      entitiesService.updateKioskLink(link);
      xLogger.info("AUDITLOG\t{0}\t{1}\tENTITY\t " +
              "UPDATE RELATION \t{2}\t{3}\tLINKTYPE={4}", domainId, sUser.getUsername(),
          k.getName(),
          linkedKioskName, model.linkType);
    } catch (ServiceException | ObjectNotFoundException e) {
      xLogger.warn("Error Updating Relationship for " + model.entityId, e);
      throw new InvalidServiceException(
          backendMessages.getString("relation.update.error") + " " + model.entityId);
    }
    return backendMessages.getString("relation.update.success");
  }

  @RequestMapping(value = "/permission", method = RequestMethod.POST)
  public
  @ResponseBody
  String setPermission(@RequestBody PermissionModel per) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Long domainId = sUser.getCurrentDomainId();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    if (per == null || per.eid == null) {
      throw new BadRequestException(backendMessages.getString("kiosk.id.unavailable"));
    }
    try {
      IKiosk k = entitiesService.getKiosk(per.eid, true);
      k = entityBuilder.addRelationPermission(k, per);
      entitiesService.updateKiosk(k, domainId, sUser.getUsername());
      xLogger.info("AUDITLOG\t{0}\t{1}\tENTITY\t " +
          "SET PERMISSION \t{2}\t{3}", domainId, sUser.getUsername(), k.getKioskId(), k.getName());
    } catch (ServiceException e) {
      throw new InvalidServiceException(backendMessages.getString("kiosk.setpermission.error"));
    }
    return backendMessages.getString("setpermission.success");
  }

  @RequestMapping(value = "/permission", method = RequestMethod.GET)
  public
  @ResponseBody
  PermissionModel getPermission(@RequestParam Long eId) {
    ResourceBundle backendMessages = Resources.getBundle(SecurityUtils.getLocale());
    try {
      IKiosk kiosk = entitiesService.getKiosk(eId, false);
      return entityBuilder.buildPermissionModel(kiosk, kiosk.getCustomerPerm(), kiosk.getVendorPerm());
    } catch (ServiceException e) {
      throw new InvalidServiceException(backendMessages.getString("kiosk.setpermission.error"));
    }
  }

  @RequestMapping(value = "/{entityId}/materials", method = RequestMethod.GET)
  public
  @ResponseBody
  Results getEntityMaterials(
      @PathVariable Long entityId,
      @RequestParam(defaultValue = PageParams.DEFAULT_OFFSET_STR) int offset,
      @RequestParam(defaultValue = PageParams.DEFAULT_SIZE_STR) int size,
      @RequestParam(required = false) String tag,
      HttpServletRequest request) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    try {
      Long domainId = sUser.getCurrentDomainId();
      Navigator navigator =
          new Navigator(request.getSession(), "EntityController.getEntityMaterials", offset, size,
              "dummy/" + tag, 0);
      PageParams pageParams = new PageParams(navigator.getCursor(offset), offset, size);
      Results results;
      if (!EntityAuthoriser.authoriseEntity(entityId)) {
        throw new ForbiddenAccessException(backendMessages.getString("permission.denied"));
      }
      if (tag != null) {
        results = inventoryManagementService.getInventoryByKiosk(entityId, tag, pageParams);
      } else {
        results = inventoryManagementService.getInventoryByKiosk(entityId, pageParams);
      }
      ICounter counter = Counter.getMaterialCounter(domainId, entityId, tag);
      results.setNumFound(counter.getCount());
      results.setOffset(offset);
      navigator.setResultParams(results);
      return inventoryBuilder.buildInventoryModelListAsResult(results, domainId, entityId);
    } catch (Exception e) {
      xLogger.severe("Error in getting Entity Materials", e);
      throw new InvalidServiceException(
          backendMessages.getString("kioskmaterials.fetch.error") + " " + entityId);
    }
  }

  @RequestMapping(value = "/{entityId}/materials/materialStats", method = RequestMethod.GET)
  public
  @ResponseBody
  Map<String, Integer> getMaterialStats(
      @PathVariable Long entityId) {
    Map<String, Integer> counts = new HashMap<>(2);
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    try {
      if (!EntityAuthoriser.authoriseEntity(entityId)) {
        throw new ForbiddenAccessException(backendMessages.getString("permission.denied"));
      }
      counts.put("oos", inventoryManagementService.getOutOfStockCounts(entityId));
      counts.put("invSz",
          Counter.getMaterialCounter(SecurityUtils.getDomainId(), entityId, null)
              .getCount());
    } catch (ServiceException e) {
      xLogger.severe("Error in getting Status of materials", e);
      throw new InvalidServiceException(
          backendMessages.getString("material.status.error") + " " + entityId);
    }
    return counts;
  }

  @RequestMapping(value = "/monthlyStats/{entityId}", method = RequestMethod.GET)
  public
  @ResponseBody
  List<EntitySummaryModel> getMonthlyStats(@PathVariable Long entityId) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    Long domainId = sUser.getCurrentDomainId();
    Results results;
    Date startDate = new Date();
    List<EntitySummaryModel> summaryModelList;
    try {
      if (!EntityAuthoriser.authoriseEntity(entityId)) {
        throw new ForbiddenAccessException(backendMessages.getString("permission.denied"));
      }
      results = reportsService.getMonthlyUsageStatsForKiosk(domainId, entityId, startDate);
      summaryModelList = entityBuilder.buildStatsMap(results.getResults());
    } catch (ServiceException se) {
      xLogger.warn("Error fetching monthly reports for entity" + entityId);
      throw new InvalidServiceException(
          backendMessages.getString("monthlyreports.fetch.error") + " " + entityId);
    }
    return summaryModelList;

  }

  @RequestMapping(value = "/materials/", method = RequestMethod.POST)
  public
  @ResponseBody
  String addMaterialsToEntities(@RequestBody AddMaterialsRequestObj addMaterialsRequestObj) {
    Locale locale;
    ResourceBundle backendMessages;
    if (!addMaterialsRequestObj.execute) {
      SecureUserDetails sUser = SecurityUtils.getUserDetails();
      addMaterialsRequestObj.userId = sUser.getUsername();
      addMaterialsRequestObj.domainId = sUser.getCurrentDomainId();
      addMaterialsRequestObj.execute = true;
      locale = sUser.getLocale();
      backendMessages = Resources.getBundle(locale);
      try {

        AppFactory.get().getTaskService().schedule(ITaskService.QUEUE_DEFAULT, ADD_MATERIALS_TASKS,
            GsonUtils.toJson(addMaterialsRequestObj));
        return backendMessages.getString("schedule.task.add.success") + " " +
            addMaterialsRequestObj.materials.size() + " " +
            backendMessages.getString("materials.for") + " " +
            (addMaterialsRequestObj.entityIds != null && !addMaterialsRequestObj.entityIds.isEmpty()
                ? addMaterialsRequestObj.entityIds.size() : backendMessages.getString("all.lower")) +
            " " + backendMessages.getString("kiosks") + "." + MsgUtil.newLine() + MsgUtil.newLine()
            +
            backendMessages.getString("note") + ":" + MsgUtil.newLine() +
            "- " + (addMaterialsRequestObj.overwrite ? backendMessages.getString("overwrite")
            : backendMessages.getString("nooverwrite")) + MsgUtil.newLine() +
            "- " + backendMessages.getString("items.added.delay") + ".";
      } catch (TaskSchedulingException e) {
        xLogger.warn("Failed to schedule add materials to entities task", e);
        throw new InvalidServiceException("Failed to schedule add materials to entities task");
      }
    }
    IUserAccount srcUser;
    try {
      srcUser = usersService.getUserAccount(addMaterialsRequestObj.userId);
      locale = srcUser.getLocale();

    } catch (Exception e) {
      xLogger.severe("Failed to add materials to entities ", e);
      return null;
    }
    int addCnt = 0;
    if (addMaterialsRequestObj.materials != null && !addMaterialsRequestObj.materials.isEmpty()) {
      Map<String, String> params = new HashMap<>();
      List<String> multiValuedParams = new ArrayList<>(1);
      String midsStr = "";
      params.put("action", "add");
      params.put("type", "materialtokiosk");
      if (locale != null) {
        params.put("country", locale.getCountry());
        params.put("language", locale.getLanguage());
        params.put("sourceuserid", srcUser.getUserId());
        params.put("domainid", addMaterialsRequestObj.domainId.toString());
        params.put("overwrite", String.valueOf(addMaterialsRequestObj.overwrite));
        for (InventoryModel material : addMaterialsRequestObj.materials) {
          String midStr = material.mId.toString();
          params.put("reorderlevel" + midStr, material.reord.toString());
          params.put("minDur" + midStr, material.minDur.toString());
          params.put("max" + midStr, material.max.toString());
          params.put("maxDur" + midStr, material.maxDur.toString());
          params.put("cr" + midStr, material.crMnl.toString());
          params.put("price" + midStr, material.rp.toString());
          params.put("tax" + midStr, material.tx.toString());
          params.put("servicelevel" + midStr, Float.toString(material.sl));
          params.put("invmodel" + midStr, material.im);
          params.put("materialname" + midStr, material.mnm);
          if (material.event != -1) {
            params.put("datatype" + midStr, Integer.toString(material.event));
          }
          if (!midsStr.isEmpty()) {
            midsStr += ",";
          }
          midsStr += midStr;
          addCnt++;
        }
        params.put("materialid", midsStr);
        multiValuedParams.add("materialid");
      }

      if (addMaterialsRequestObj.entityIds == null || addMaterialsRequestObj.entityIds.isEmpty()) {
        try {
          Results kioskResults;

          if (SecurityUtil.compareRoles(srcUser.getRole(), SecurityConstants.ROLE_DOMAINOWNER) >= 0) {
            kioskResults = entitiesService.getAllDomainKiosks(addMaterialsRequestObj.domainId, null, null, null);
          } else {
            kioskResults = entitiesService.getKiosks(srcUser, addMaterialsRequestObj.domainId, null, null, null);
          }
          if (kioskResults.getResults().size() > 0) {
            for (Object o : kioskResults.getResults()) {
              addMaterialsRequestObj.entityIds.add(((IKiosk) o).getKioskId());
            }
          }
        } catch (ServiceException se) {
          xLogger
              .severe(
                  "Error fetching entities for domain" + addMaterialsRequestObj.domainId,
                  se);
        }
      }

      for (Long entityId : addMaterialsRequestObj.entityIds) {
        params.put("kioskid", entityId.toString());
        try {
          xLogger.fine("Scheduling " + "add" + " task for kiosk {0}, params = {1}", entityId,
              params.toString());
          AppFactory.get().getTaskService()
              .schedule(ITaskService.QUEUE_DEFAULT, CREATEENTITY_TASK_URL, params,
                  multiValuedParams, null, ITaskService.METHOD_POST, -1);
        } catch (Exception e) {
          xLogger
              .warn("Error scheduling materials operation {0} task for kiosk {0}", "add",
                  entityId);
        }
      }
      xLogger.info("Task scheduled to add " + " " + addCnt + " materials for "
          + addMaterialsRequestObj.entityIds.size() + " entities.");
    }
    return null;
  }


  @RequestMapping(value = "/materials/remove/", method = RequestMethod.POST)
  public
  @ResponseBody
  String removeMaterialsFromEntity(@RequestBody RemoveMaterialsRequestObj removeMaterialsRequestObj) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Long domainId = sUser.getCurrentDomainId();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    int addCnt = 0;
    Results kioskResults;
    if (removeMaterialsRequestObj.entityIds == null || removeMaterialsRequestObj.entityIds
        .isEmpty()) {
      kioskResults = entitiesService.getAllKiosks(domainId, null, null, null);
      if (kioskResults.getResults().size() > 0) {
        List entities = kioskResults.getResults();
        for (Object o : entities) {
          IKiosk k = (IKiosk) o;
          removeMaterialsRequestObj.entityIds.add(k.getKioskId());
        }
      }
    }
    boolean
        hasMaterials =
        (removeMaterialsRequestObj.materials != null
            && removeMaterialsRequestObj.materials.size() > 0);
    boolean
        hasKiosks =
        (removeMaterialsRequestObj.entityIds != null
            && removeMaterialsRequestObj.entityIds.size() > 0);

    if (hasMaterials) {
      Map<String, String> params = new HashMap<>();
      List<String> multiValuedParams = new ArrayList<>();
      List<IKiosk> kiosks = new ArrayList<>();
      String midsStr = "";
      params.put("action", "remove");
      params.put("type", "materialtokiosk");
      if (locale != null) {
        params.put("country", locale.getCountry());
        params.put("language", locale.getLanguage());
        params.put("domainid", domainId.toString());
        if (removeMaterialsRequestObj.materials.size() > 0) {
          for (int i = 0; i < removeMaterialsRequestObj.materials.size(); i++) {
            String midStr = removeMaterialsRequestObj.materials.get(i).mId.toString();
            if (!midsStr.isEmpty()) {
              midsStr += ",";
            }
            midsStr += midStr;
            addCnt++;
          }

        }
        params.put("materialid", midsStr);
        multiValuedParams.add("materialid");
      }
      if (hasKiosks) {
        for (int i = 0; i < removeMaterialsRequestObj.entityIds.size(); i++) {
          try {
            kiosks.add(entitiesService.getKiosk(removeMaterialsRequestObj.entityIds.get(i)));
          } catch (ServiceException e) {
            xLogger.warn(
                "Error fetching entity for entityId" + removeMaterialsRequestObj.entityIds.get(i),
                e);
            throw new InvalidServiceException(
                backendMessages.getString("entity.entityid.fetch.error")
                    + removeMaterialsRequestObj.entityIds.get(i));
          }
        }
      }
      Iterator<IKiosk> it = kiosks.iterator();
      String kioskname = "";
      while (it.hasNext()) {
        IKiosk k = it.next();
        kioskname = k.getName();
        params.put("kioskid", k.getKioskId().toString());
        try {
          xLogger
              .fine("Scheduling " + "remove" + " task for kiosk {0}, params = {1}",
                  k.getKioskId(),
                  params.toString());
          AppFactory.get().getTaskService()
              .schedule(ITaskService.QUEUE_DEFAULT, CREATE_ENTITY_TASK_URL, params,
                  multiValuedParams, null, ITaskService.METHOD_POST, -1);
          xLogger.info("AUDITLOG\t{0}\t{1}\tMATERIAL\tREMOVE\t{2}\t FOR ENTITY\t{3}", domainId,
              sUser.getUsername(), midsStr, k.getKioskId());
        } catch (Exception e) {
          xLogger.warn("Error scheduling materials operation {0} task for kiosk {0}", "add",
              k.getKioskId());
        }
      }
      if (removeMaterialsRequestObj.entityIds.size() == 1) {
        return addCnt + " " + backendMessages.getString("schedule.task.delete.success") + " "
            + kioskname + ".<br><br> " + backendMessages.getString("items.delete.delay") + ".";
      } else {
        return backendMessages.getString("schedule.task.remove.success") + " " + addCnt + " "
            + backendMessages.getString("materials.for") + " " + removeMaterialsRequestObj.entityIds
            .size() + " " + backendMessages.getString("kiosks") + ". <br><br> " + backendMessages
            .getString("items.delete.delay") + ".";
      }
    }
    return null;
  }

  /**
   * Edit Materials of Entity
   */
  @RequestMapping(value = "/{entityId}/materials/", method = RequestMethod.POST)
  public
  @ResponseBody
  String editMaterialsOfEntity(@PathVariable Long entityId,
                               @RequestBody EditMaterialsReqObj editMaterialsReqObj) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    Long domainId = sUser.getCurrentDomainId();
    List<InventoryModel> materials = editMaterialsReqObj.materials;
    int matCnt, errCnt = 0;
    List<String> errMat = new ArrayList<>();
    String kioskName;
    try {
      if (!EntityAuthoriser.authoriseEntity(entityId)) {
        throw new ForbiddenAccessException(backendMessages.getString("permission.denied"));
      }
      kioskName = entitiesService.getKiosk(entityId).getName();
      List<IInvntry> inventories = inventoryManagementService.getInventoryByKiosk(entityId, null).getResults();
      Map<Long, IInvntry> invMap = new HashMap<>();
      for (IInvntry inv : inventories) {
        invMap.put(inv.getMaterialId(), inv);
      }

      DomainConfig dc = DomainConfig.getInstance(domainId);
      boolean isDOS = dc.getInventoryConfig().getMinMaxType() == InventoryConfig.MIN_MAX_DOS;
      String mmd = dc.getInventoryConfig().getMinMaxDur();
      boolean isManual = dc.getInventoryConfig().getConsumptionRate() == InventoryConfig.CR_MANUAL;
      List<IInvntry> updItems = new ArrayList<>(materials.size());
      for (InventoryModel material : materials) {
        IInvntry inv = invMap.get(material.mId);
        if (inv != null) {
          inventoryBuilder.buildInvntry(domainId, kioskName, entityId, material, inv, sUser.getUsername(),
              isDOS, isManual, mmd, inventoryManagementService);
          updItems.add(inv);
        } else {
          xLogger.warn("Expected material {0} not found in Entity {1} inventory", material.mnm,
              kioskName);
          errCnt++;
          errMat.add(material.mId + ":" + material.mnm);
        }
      }
      inventoryManagementService.updateInventory(updItems, sUser.getUsername());
      matCnt = updItems.size();
    } catch (ServiceException e) {
      xLogger.warn("Exception when adding inventory: {0} to {1}", materials, entityId, e);
      throw new InvalidServiceException(
          backendMessages.getString("kiosk.add.inventory.exception") + " " + entityId);
    }
    if (errCnt > 0) {
      StringBuilder sb = new StringBuilder();
      for (String mat : errMat) {
        sb.append("\n").append(mat);
      }
      return backendMessages.getString("partial.success") + ". \n" + backendMessages
          .getString("materials.fail") + ":" + sb.toString();
    } else {
      return backendMessages.getString("update.success") + matCnt + " " + backendMessages
          .getString("materials.of") + " " + kioskName;
    }
  }


  @RequestMapping(value = "/reorder", method = RequestMethod.POST)
  public
  @ResponseBody
  String updateEntityOrder(@RequestBody ReorderEntityRequestObj obj) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    try {
      Long domainId = sUser.getCurrentDomainId();
      String noRouteTag = "--notag--";
      Map<String, Integer> tMap = new HashMap<>();
      if (obj.rta) {
        for (EntityModel model : obj.ordEntities) {
          String rt = StringUtils.isNotBlank(model.rt) ? model.rt : noRouteTag;
          tMap.put(rt, tMap.containsKey(rt) ? tMap.get(rt) + 1 : 1);
        }
      }
      final String empty = "";
      Map<String, Long[]> tagOrderEntities = new HashMap<>(tMap.size());
      int noRouteIndex = 0;
      for (EntityModel model : obj.ordEntities) {
        String rt = obj.rta ?
            StringUtils.isNotBlank(model.rt) ? model.rt : noRouteTag :
            empty;
        if (!tagOrderEntities.containsKey(rt)) {
          int size = obj.rta ? tMap.get(rt) : obj.ordEntities.size();
          tagOrderEntities.put(rt, new Long[size]);
        }
        if (model.ri != 2147483647) {
          int index = rt.equals(noRouteTag) ? noRouteIndex++ : model.osno - 1;
          tagOrderEntities.get(rt)[index] = model.id;
        }
      }

      StringBuilder routeQueryString = new StringBuilder();
      final String tg = "tag=";
      final String routeCSV = "&routecsv=";
      final String comma = ",";
      final String noRouteCSV = "&noroutecsv=";
      final String noRouteCSVVal = tagOrderEntities.get(noRouteTag) == null ?
          empty :
          joinArray(tagOrderEntities.get(noRouteTag), comma);
      for (String tag : tagOrderEntities.keySet()) {
        if (tagOrderEntities.size() == 1 && noRouteCSVVal.length() > 0) {
          routeQueryString
              .append(tg).append(empty)
              .append(routeCSV).append(empty)
              .append(noRouteCSV).append(noRouteCSVVal);
          entitiesService.updateRelatedEntitiesOrdering(domainId, obj.eid, obj.lt, routeQueryString.toString());
        } else if (!tag.equals(noRouteTag)) {
          routeQueryString
              .append(tg).append(tag)
              .append(routeCSV).append(joinArray(tagOrderEntities.get(tag), comma))
              .append(noRouteCSV).append(noRouteCSVVal);
          entitiesService.updateRelatedEntitiesOrdering(domainId, obj.eid, obj.lt, routeQueryString.toString());
          routeQueryString.setLength(0);
        }
      }
    } catch (ServiceException e) {
      xLogger.severe("Exception when saving linked kiosk route for userID {0}", obj.eid);
      throw new InvalidServiceException(
          backendMessages.getString("linked.kiosk.route.exception") + " " + obj.eid);
    }
    return backendMessages.getString("route.update.success");
  }

  private String joinArray(Long[] array, String separator) {
    StringBuilder buf = new StringBuilder();
    if (array != null) {
      for (Long aLong : array) {
        if (aLong != null) {
          buf.append(aLong).append(separator);
        }
      }
      if (buf.length() > 0) {
        buf.setLength(buf.length() - separator.length());
      }
    }
    return buf.toString();
  }

  @RequestMapping(value = "/manreorder", method = RequestMethod.POST)
  public
  @ResponseBody
  String updateManagedEntityOrder(@RequestBody ReorderEntityRequestObj obj) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    try {
      Long domainId = sUser.getCurrentDomainId();
      String noRouteTag = "--notag--";
      Map<String, Integer> tMap = new HashMap<>();
      if (obj.rta) {
        for (EntityModel model : obj.ordEntities) {
          String rt = StringUtils.isNotBlank(model.rt) ? model.rt : noRouteTag;
          tMap.put(rt, tMap.containsKey(rt) ? tMap.get(rt) + 1 : 1);
        }
      }
      final String empty = "";
      Map<String, Long[]> tagOrderEntities = new HashMap<>(tMap.size());
      int noRouteIndex = 0;
      for (EntityModel model : obj.ordEntities) {
        String rt = obj.rta ?
            StringUtils.isNotBlank(model.rt) ? model.rt : noRouteTag :
            empty;
        if (!tagOrderEntities.containsKey(rt)) {
          int size = obj.rta ? tMap.get(rt) : obj.ordEntities.size();
          tagOrderEntities.put(rt, new Long[size]);
        }
        if (model.ri != 2147483647) {
          int index = rt.equals(noRouteTag) ? noRouteIndex++ : model.osno - 1;
          tagOrderEntities.get(rt)[index] = model.id;
        }
      }

      StringBuilder routeQueryString = new StringBuilder();
      final String tg = "tag=";
      final String routeCSV = "&routecsv=";
      final String comma = ",";
      final String noRouteCSV = "&noroutecsv=";
      final String noRouteCSVVal = tagOrderEntities.get(noRouteTag) == null ?
          empty :
          joinArray(tagOrderEntities.get(noRouteTag), comma);
      for (String tag : tagOrderEntities.keySet()) {
        if (tagOrderEntities.size() == 1 && noRouteCSVVal.length() > 0) {
          routeQueryString
              .append(tg).append(empty)
              .append(routeCSV).append(empty)
              .append(noRouteCSV).append(noRouteCSVVal);
          entitiesService.updateManagedEntitiesOrdering(domainId, obj.uid, routeQueryString.toString());
        } else if (!tag.equals(noRouteTag)) {
          routeQueryString
              .append(tg).append(tag)
              .append(routeCSV).append(joinArray(tagOrderEntities.get(tag), comma))
              .append(noRouteCSV).append(noRouteCSVVal);
          entitiesService.updateManagedEntitiesOrdering(domainId, obj.uid,
              routeQueryString.toString());
          routeQueryString.setLength(0);
        }
      }
    } catch (ServiceException e) {
      xLogger.severe("Exception when saving linked kiosk route for userID {0}", obj.eid);
      throw new InvalidServiceException(
          backendMessages.getString("linked.kiosk.route.exception") + " " + obj.eid);
    }
    return backendMessages.getString("route.update.success");
  }

  @RequestMapping(value = "/deleteRelation", method = RequestMethod.POST)
  public
  @ResponseBody
  String deleteEntityRelation(@RequestBody List<String> linkIds) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Long domainId = sUser.getCurrentDomainId();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    try {
      entitiesService.deleteKioskLinks(domainId, linkIds, sUser.getUsername());
    } catch (ServiceException e) {
      xLogger.severe("Error in deleting Relationship for entities", e);
      throw new InvalidServiceException(backendMessages.getString("kiosk.relation.delete.error"));
    }

    return backendMessages.getString("kiosk.relation.delete.success");
  }

  @RequestMapping(value = "/user/{userId:.+}", method = RequestMethod.GET)
  public
  @ResponseBody
  Results getUserEntities(@PathVariable String userId,
                          @RequestParam(required = false) String size,
                          @RequestParam(required = false) String offset,
                          HttpServletRequest request) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    try {
      if (!GenericAuthoriser.authoriseUser(userId)) {
        throw new ForbiddenAccessException(backendMessages.getString("permission.denied"));
      }
      IUserAccount u = usersService.getUserAccount(userId);
      PageParams pageParams = null;
      int o = offset == null ? 0 : Integer.parseInt(offset);
      if (size != null && Integer.parseInt(size) > 0 || o > 0) {
        int s = size == null ? PageParams.DEFAULT_SIZE : Integer.parseInt(size);
        Navigator
            navigator =
            new Navigator(request.getSession(), "EntityController.getUserEntities", o, s,
                "userEntity", 0);
        pageParams = new PageParams(navigator.getCursor(o), o, s);
      }
      Results results = entitiesService.getKiosksForUser(u, null, pageParams);
      if (results.getResults() != null) {
        return new Results<>(entityBuilder.buildUserEntities(results.getResults()), null,
            pageParams == null ? 0
                : Counter.getUserToKioskCounter(SecurityUtils.getDomainId(), userId)
                    .getCount(), o);
      }
    } catch (ServiceException | ObjectNotFoundException e) {
      xLogger.severe("Error in getting entities for user " + userId, e);
      throw new InvalidServiceException(
          backendMessages.getString("user.fetch.kiosks") + " " + userId);
    }
    return null;
  }

  @RequestMapping("/check/")
  public
  @ResponseBody
  boolean checkEntityExist(@RequestParam String enm) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Long domainId = sUser.getCurrentDomainId();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    try {
      if (entitiesService.getKioskByName(domainId, enm) != null) {
        return true;
      }
    } catch (ServiceException e) {
      xLogger.severe("Exception when checking Entity existence for domain {0}", domainId);
      throw new InvalidServiceException(
          backendMessages.getString("kiosk.existence") + " " + domainId);
    }
    return false;
  }

  @RequestMapping(value = "/stockboard", method = RequestMethod.POST)
  public
  @ResponseBody
  String setStockBoard(@RequestBody StockBoardModel model) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    IConfig c;
    KioskConfig kc;
    StockboardConfig sbConfig;
    Long domainId = null;
    String key;
    try {
      c = JDOUtils.createInstance(IConfig.class);
      domainId = sUser.getCurrentDomainId();
      if (model != null) {
        key = stockBoardBuilder.getKey(model.kid);
        kc = KioskConfig.getInstance(model.kid);
        if (kc == null) {
          xLogger.severe(
              "Failed to create KioskConfig object. Exiting from updateKioskConfig method...");
        }
        try {
          if (configurationMgmtService.getConfiguration(key) != null) {
            model.add = false;
          }
        } catch (ObjectNotFoundException e) {
          model.add = true;
        }
        sbConfig = kc.getStockboardConfig();
        sbConfig = stockBoardBuilder.buildStockBoardConfig(sbConfig, model);
        kc.setSbConfig(sbConfig);
        String kioskConfig = kc.toJSONString();
        c.setKey(key);
        c.setUserId(sUser.getUsername());
        c.setDomainId(domainId);
        c.setLastUpdated(new Date());
        c.setConfig(kioskConfig);
        if (model.add) {
          configurationMgmtService.addConfiguration(key, c);
        } else {
          configurationMgmtService.updateConfiguration(c);
        }

      }
    } catch (ServiceException e) {
      xLogger.warn("Error saving stock board configuration for this entity");
      throw new InvalidServiceException(backendMessages.getString("stock.board.save.error"));
    } catch (JSONException e) {
      e.printStackTrace();
    }
    if (model.add) {
      xLogger.info("AUDITLOG\t{0}\t{1}\tENTITY\t " +
          "SET STOCK BOARD\t{2}", domainId, sUser.getUsername(), model.kid);
      return backendMessages.getString("stock.board.config.success") + ".";
    } else {
      xLogger.info("AUDITLOG\t{0}\t{1}\tENTITY\t " +
          "STOCK BOARD UPDATE\t{2}", domainId, sUser.getUsername(), model.kid);
      return backendMessages.getString("stock.board.config.update") + ".";
    }

  }

  @RequestMapping(value = "/stockboard", method = RequestMethod.GET)
  public
  @ResponseBody
  StockBoardModel getStockBoard(@RequestParam Long entityId) {
    KioskConfig kc;
    StockBoardModel model = new StockBoardModel();
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    try {
      if (entityId != null) {
        kc = KioskConfig.getInstance(entityId);
        if (kc != null) {
          model = stockBoardBuilder.buildStockBoardModel(kc, model);
        }
      }
      return model;
    } catch (Exception e) {
      xLogger.severe("Exception when retrieving stock board config");
      throw new InvalidServiceException(backendMessages.getString("stock.board.config.fetch"));
    }

  }

  @RequestMapping(value = "/move", method = RequestMethod.POST)
  public
  @ResponseBody
  String moveEntity(@RequestParam String kids, @RequestParam String dDid,
                    @RequestParam(required = false) String sDid, HttpServletRequest request) {
    ResourceBundle backendMessages = null;
    try {
      backendMessages = Resources.getBundle(Locale.ENGLISH);
      if (StringUtils.isBlank(kids)) {
        throw new ServiceException(
            "No " + backendMessages.getString("kiosk.lowercase") + " is selected for moving");
      }
      final String executeStr = "execute";
      boolean execute = request.getParameter(executeStr) != null;
      List<Long> kidList = null;
      if (!"all".equalsIgnoreCase(kids)) {
        String[] strKids = StringUtils.split(kids, CharacterConstants.COMMA);
        kidList = new ArrayList<>(strKids.length);
        for (String strKid : strKids) {
          kidList.add(Long.valueOf(strKid));
        }
      }
      if (!execute) {
        if ("all".equalsIgnoreCase(kids)) {
          Long domainId = SecurityUtils.getDomainId();
          kidList = entitiesService.getAllDomainKioskIds(domainId);
          if (kidList == null) {
            throw new ServiceException(
                "Error in getting all " + backendMessages.getString("kiosk.lowercase")
                    + " ids of domain Id:" + domainId);
          }
          StringBuilder sb = new StringBuilder();
          for (Long kid : kidList) {
            sb.append(kid).append(CharacterConstants.COMMA);
          }
          if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
          }
          kids = sb.toString();
        }
        String message = EntityMover.validateMoveEntitiesToDomain(kidList);
        if (!message.equals("success")) {
          return message;
        }
        Map<String, String> params = new HashMap<>();
        params.put("kids", kids);
        params.put(executeStr, "true");
        params.put("dDid", dDid);
        if (StringUtils.isNotBlank(sDid)) {
          params.put("sDid", sDid);
        }
        try {
          AppFactory.get().getTaskService()
              .schedule(ITaskService.QUEUE_DEFAULT, MOVE_ENTITY_TASK_URL, params,
                  ITaskService.METHOD_POST);
        } catch (Exception e) {
          xLogger.warn("{0} when scheduling task to move kiosk {1} to domain {2}: {3}",
              e.getClass().getName(), kids, dDid, e.getMessage(), e);
        }
        return backendMessages.getString("kiosk") + " move scheduled successfully." + MsgUtil
            .newLine() + MsgUtil.newLine() +
            "Note: Moving " + backendMessages.getString("kiosk.lowercase")
            + " will take some time to complete.";
      }
      Long destDomainId = Long.valueOf(dDid);
      EntityMover.moveEntitiesToDomain(kidList, destDomainId, sDid);
      return null;
    } catch (ServiceException e) {
      xLogger.severe("Error in scheduling entity move:", e);
      throw new InvalidServiceException(
          "Error in scheduling " + backendMessages.getString("kiosk.lowercase") + " move");
    }
  }

  @RequestMapping(value = "/removedomain", method = RequestMethod.POST)
  public
  @ResponseBody
  String removeDomain(@RequestParam Long did, @RequestParam Long kid, HttpServletRequest request) {
    ResourceBundle backendMessages = null;
    try {
      backendMessages = Resources.getBundle(Locale.ENGLISH);
      if (kid == null) {
        throw new ServiceException(
            "Please select " + backendMessages.getString("kiosk.lowercase") + " to remove domain.");
      }
      if (did == null) {
        throw new ServiceException("Please select domain to remove from selected " + backendMessages
            .getString("kiosk.lowercase") + ".");
      }
      SecureUserDetails sUser = SecurityUtils.getUserDetails();
      final String executeStr = "execute";
      boolean execute = (request.getParameter(executeStr) != null);
      if (!execute) {
        String message = DomainRemover.validateDomainRemove(did, kid);
        if (!message.equals("success")) {
          return message;
        }
        Map<String, String> params = new HashMap<>();
        params.put("kid", String.valueOf(kid));
        params.put(executeStr, "true");
        params.put("did", String.valueOf(did));
        try {
          AppFactory.get().getTaskService()
              .schedule(ITaskService.QUEUE_DEFAULT, MOVE_ENTITY_TASK_URL, params,
                  ITaskService.METHOD_POST);
        } catch (Exception e) {
          xLogger.warn("{0} when scheduling task to delete domain {1} from kiosk {2}: {3}",
              e.getClass().getName(), did, kid, e.getMessage(), e);
        }
        return "Remove domain from " + backendMessages.getString("kiosk.lowercase")
            + " scheduled successfully." + MsgUtil.newLine() + MsgUtil.newLine() +
            "Note: Removing domain from " + backendMessages.getString("kiosk.lowercase")
            + " will take some time to complete.";
      }
      DomainRemover.removeDomain(did, kid);
      xLogger.info("AUDITLOG\t{0}\t{1}\tENTITY\t " +
          "REMOVE DOMAIN\t{2}", did, sUser.getUsername(), kid);
      return null;
    } catch (Exception e) {
      xLogger.severe("Error in scheduling entity move:", e);
      if (backendMessages != null) {
        throw new InvalidServiceException(
            "Error in scheduling " + backendMessages.getString("kiosk.lowercase") + " move");
      } else {
        throw new InvalidServiceException("Error in scheduling entity move");
      }
    }
  }

  @RequestMapping(value = "/domains", method = RequestMethod.GET)
  public
  @ResponseBody
  List<EntityDomainModel> getDomainData(@RequestParam Long eId) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    try {
      if (!EntityAuthoriser.authoriseEntity(eId)) {
        ResourceBundle backendMessages = Resources.getBundle(locale);
        throw new ForbiddenAccessException(backendMessages.getString("permission.denied"));
      }
      IKiosk k = entitiesService.getKiosk(eId);
      return entityBuilder.buildEntityDomainData(k);
    } catch (ServiceException se) {
      xLogger.warn("Error fetching Entity's domain details for " + eId, se);
      ResourceBundle backendMessages = Resources.getBundle(locale);
      throw new InvalidServiceException(
          backendMessages.getString("kiosk.detail.fetch.error") + " " + eId);
    }
  }

  @RequestMapping(value = "/domainupdate", method = RequestMethod.POST)
  public
  @ResponseBody
  String addOrRemoveDomainData(@RequestParam String eIds, @RequestParam String dIds,
                               @RequestParam String type, HttpServletRequest request) {
    ResourceBundle backendMessages = null;
    try {
      backendMessages = Resources.getBundle(Locale.ENGLISH);
      if (eIds == null) {
        throw new ServiceException("Please select " + backendMessages.getString("kiosk.lowercase")
            + " to add/remove domain to it.");
      }
      if (dIds == null) {
        throw new ServiceException(
            "Please select domain to add/remove from selected " + backendMessages
                .getString("kiosk.lowercase") + ".");
      }
      final String executeStr = "execute";
      final String ADD = "add";
      boolean execute = request.getParameter(executeStr) != null;
      if (!execute) {
        if ("all".equalsIgnoreCase(eIds)) {
          Long domainId = SecurityUtils.getDomainId();
          List<Long> kIdList = entitiesService.getAllDomainKioskIds(domainId);
          if (kIdList == null) {
            throw new ServiceException(
                "Error in getting all " + backendMessages.getString("kiosk.lowercase")
                    + " ids of on domain Id:" + domainId);
          }
          StringBuilder sb = new StringBuilder();
          for (Long kid : kIdList) {
            sb.append(kid).append(CharacterConstants.COMMA);
          }
          if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
          }
          eIds = sb.toString();
        }
        Map<String, String> params = new HashMap<>();
        params.put("eIds", eIds);
        params.put(executeStr, "true");
        params.put("dIds", dIds);
        params.put("type", type);
        try {
          AppFactory.get().getTaskService()
              .schedule(ITaskService.QUEUE_DEFAULT, DOMAIN_ENTITY_TASK_URL, params,
                  ITaskService.METHOD_POST);
        } catch (Exception e) {
          throw new ServiceException(e); //Will be caught in below catch
        }
        String[] did = dIds.split(CharacterConstants.COMMA);
        String dName;
        if (did.length == 1) {
          dName = domainsService.getDomain(Long.valueOf(did[0])).getName();
        } else {
          dName = String.valueOf(did.length);
        }
        int noe = StringUtils.split(eIds, CharacterConstants.COMMA).length;
        int nod = did.length;
        return "Task scheduled successfully for "
            + (ADD.equals(type) ? "adding " : "removing ")
            + noe + (noe == 1 ? " " + backendMessages.getString("kiosk.lowercase")
            : " " + backendMessages.getString("kiosks.lower"))
            + (ADD.equals(type) ? " to " : " from ")
            + (nod == 1 ? dName + " domain" : nod + " domains");
      }
      String[] strKids = StringUtils.split(eIds, CharacterConstants.COMMA);
      List<Long> eIdList = new ArrayList<>(strKids.length);
      for (String strKid : strKids) {
        eIdList.add(Long.valueOf(strKid));
      }
      String[] did = dIds.split(CharacterConstants.COMMA);
      if (did.length == 1) { // Move/Add Entity under entity
        for (Long eId : eIdList) {
          IKiosk k = entitiesService.getKiosk(eId);
          if ((ADD.equals(type) && !k.getDomainIds().contains(Long.parseLong(did[0]))) ||
              //Add entity
              (!ADD.equals(type) && k.getDomainIds()
                  .contains(Long.parseLong(did[0])))) { //Remove entity
            EntityDomainUpdater.updateEntityDomain(eId, dIds, ADD.equals(type));
          }
        }
      } else { // Add domains under entity details, domain check filter already applied in UI
        for (Long eId : eIdList) {
          EntityDomainUpdater.updateEntityDomain(eId, dIds, ADD.equals(type));
        }
      }
      return null;
    } catch (Exception e) {
      xLogger.severe("{0} when scheduling task to add/delete domain {1} to kiosk {2}: {3}",
          e.getClass().getName(), dIds, eIds, e.getMessage(), e);
      if (backendMessages != null) {
        throw new InvalidServiceException(
            "Error in scheduling domain add/remove to " + backendMessages
                .getString("kiosk.lowercase"));
      } else {
        throw new InvalidServiceException("Error in scheduling domain add/remove to entity");
      }

    }
  }

  @SuppressWarnings("unchecked")
  @RequestMapping(value = "/filterdomains", method = RequestMethod.GET)
  public
  @ResponseBody
  List<DomainSuggestionModel> getAddDomainSuggestion(@RequestParam Long eId,
                                                     @RequestParam String text,
                                                     HttpServletRequest request) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    try {
      if (!EntityAuthoriser.authoriseEntity(eId)) {
        ResourceBundle backendMessages = Resources.getBundle(locale);
        throw new ForbiddenAccessException(backendMessages.getString("permission.denied"));
      }
      IKiosk k = entitiesService.getKiosk(eId);
      if (text == null) {
        text = CharacterConstants.EMPTY;
      }
      text = text.toLowerCase();
      PersistenceManager pm = null;
      try {
        IUserAccount user = usersService.getUserAccount(sUser.getUsername());
        if (SecurityConstants.ROLE_SUPERUSER.equals(user.getRole())) {
          pm = PMF.get().getPersistenceManager();
          Query query = pm.newQuery(JDOUtils.getImplClass(IDomain.class));
          Map<String, Object> params = new HashMap<>();
          if (!text.isEmpty()) {
            query.setFilter("nNm.startsWith(txtParam)");
            query.declareParameters("String txtParam");
            params.put("txtParam", text);
          }
          Navigator navigator =
              new Navigator(request.getSession(), "EntityController.getAddDomainSuggestion", 0, 15,
                  "filterdomain", 0);
          PageParams pageParams = new PageParams(navigator.getCursor(0), 0, 15);
          QueryUtil.setPageParams(query, pageParams);
          List<IDomain> domains;
          try {
            domains = (List<IDomain>) query.executeWithMap(params);
            domains = (List<IDomain>) pm.detachCopyAll(domains);
          } finally {
            query.closeAll();
          }
          return entityBuilder.buildEntityDomainSuggestions(k, domains);
        }
      } catch (Exception e) {
        xLogger.warn("Exception: {0}", e.getMessage());
      } finally {
        if (pm != null) {
          pm.close();
        }
      }
    } catch (ServiceException se) {
      xLogger.warn("Error fetching Entity's domain details for " + eId, se);
      ResourceBundle backendMessages = Resources.getBundle(locale);
      throw new InvalidServiceException(
          backendMessages.getString("kiosk.detail.fetch.error") + " " + eId);
    }
    return null;
  }

  @RequestMapping(value = "/task/updateentityactivitytimestamps", method = RequestMethod.POST)
  public
  @ResponseBody
  void updateEntityActivityTimestamps(@RequestParam Long entityId, @RequestParam Long timestamp,
                                      @RequestParam int actType) {
    try {
      if (entityId != null && timestamp != null) {
        entitiesService.updateEntityActivityTimestamps(entityId, new Date(timestamp), actType);
      }
    } catch (Exception e) {
      xLogger.severe("Error while updating activity timestamps for kiosk: {0} for actType: {1}",
          entityId, actType, e);
    }
  }

  @RequestMapping(value = "/networkview", method = RequestMethod.GET)
  public
  @ResponseBody
  NetworkViewResponseObj getNetworkHierarchy(@RequestParam(required = false) Long domainId) {
    try {
      SecureUserDetails sUser = SecurityUtils.getUserDetails();
      Locale locale = sUser.getLocale();
      IUserAccount userAccount = usersService.getUserAccount(sUser.getUsername());
      if (domainId == null) {
        domainId = sUser.getDomainId();
      }
      MemcacheService cache = AppFactory.get().getMemcacheService();
      NetworkViewResponseObj obj;
      List<EntityLinkModel> kioskLinks;
      String cacheKey = Constants.NW_HIERARCHY_CACHE_PREFIX + domainId;
      if (cache != null && cache.get(cacheKey) != null) {
        obj = (NetworkViewResponseObj) cache.get(cacheKey);
        if (obj != null) {
          obj.network.updatedOn =
              LocalDateUtil.format(obj.network.upd, locale, sUser.getTimezone());
          return obj;
        }
      }
      kioskLinks =
          entitiesService.getKioskLinksInDomain(domainId, userAccount.getUserId(), userAccount.getRole());
      obj = entityBuilder.buildNetworkViewRequestObject(kioskLinks, domainId);
      if (cache != null) {
        cache.put(cacheKey, obj, 24 * 60 * 60); // 1 day expiry
      }
      return obj;
    } catch (Exception e) {
      xLogger.warn("Error while getting network view of domain {0}", domainId, e);
    }
    return null;
  }

  @RequestMapping(value = "/location", method = RequestMethod.GET)
  public @ResponseBody List<LocationSuggestionModel> getLocationSuggestion(
      @RequestParam String text,
      @RequestParam String type,
      @RequestParam(required = false) String parentLoc) {
    try {
      LocationSuggestionModel parentLocation = null;
      if (StringUtils.isNotEmpty(parentLoc)) {
        try {
          parentLocation = new Gson().fromJson(parentLoc, LocationSuggestionModel.class);
        } catch (JsonSyntaxException e) {
          xLogger.warn("Error while parsing super location", e);
        }
      }
      Long domainId = SecurityUtils.getCurrentDomainId();
      switch (type) {
        case "state":
          return entitiesService.getStateSuggestions(domainId, text);
        case "district":
          return entitiesService.getDistrictSuggestions(domainId, text, parentLocation);
        case "taluk":
          return entitiesService.getTalukSuggestions(domainId, text, parentLocation);
        case "city":
          return entitiesService.getCitySuggestions(domainId, text);
      }
    } catch (Exception e) {
      xLogger.warn("Error while getting location suggestions", e);
    }
    return null;
  }
}
