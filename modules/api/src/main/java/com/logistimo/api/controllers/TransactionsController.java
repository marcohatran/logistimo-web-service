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

import com.google.gson.internal.LinkedTreeMap;

import com.logistimo.AppFactory;
import com.logistimo.api.builders.ConfigurationModelBuilder;
import com.logistimo.api.builders.MarkerBuilder;
import com.logistimo.api.builders.TransactionBuilder;
import com.logistimo.api.models.MarkerModel;
import com.logistimo.api.models.TransactionDomainConfigModel;
import com.logistimo.api.models.TransactionModel;
import com.logistimo.api.models.configuration.ReasonConfigModel;
import com.logistimo.api.util.DedupUtil;
import com.logistimo.auth.SecurityConstants;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.config.models.ActualTransConfig;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.config.models.InventoryConfig;
import com.logistimo.config.models.MatStatusConfig;
import com.logistimo.config.models.ReasonConfig;
import com.logistimo.constants.CharacterConstants;
import com.logistimo.constants.Constants;
import com.logistimo.constants.SourceConstants;
import com.logistimo.dao.JDOUtils;
import com.logistimo.entities.auth.EntityAuthoriser;
import com.logistimo.entities.entity.IKiosk;
import com.logistimo.entities.entity.IKioskLink;
import com.logistimo.entities.service.EntitiesService;
import com.logistimo.exception.BadRequestException;
import com.logistimo.exception.InvalidServiceException;
import com.logistimo.inventory.TransactionUtil;
import com.logistimo.inventory.dao.ITransDao;
import com.logistimo.inventory.entity.ITransaction;
import com.logistimo.inventory.service.InventoryManagementService;
import com.logistimo.logger.XLog;
import com.logistimo.materials.entity.IMaterial;
import com.logistimo.materials.service.MaterialCatalogService;
import com.logistimo.pagination.Navigator;
import com.logistimo.pagination.PageParams;
import com.logistimo.pagination.Results;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.DuplicationException;
import com.logistimo.services.ObjectNotFoundException;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;
import com.logistimo.services.cache.MemcacheService;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.users.service.UsersService;
import com.logistimo.utils.LocalDateUtil;
import com.logistimo.utils.MsgUtil;
import com.logistimo.utils.StringUtil;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/transactions")
public class TransactionsController {

  private static final XLog xLogger = XLog.getLog(TransactionsController.class);
  private TransactionBuilder transactionBuilder;
  private ConfigurationModelBuilder configurationModelBuilder;
  private MarkerBuilder markerBuilder;
  private ITransDao transDao;
  private InventoryManagementService inventoryManagementService;
  private EntitiesService entitiesService;
  private UsersService usersService;
  private MaterialCatalogService materialCatalogService;

  @Autowired
  public void setTransactionBuilder(TransactionBuilder transactionBuilder) {
    this.transactionBuilder = transactionBuilder;
  }

  @Autowired
  public void setConfigurationModelBuilder(ConfigurationModelBuilder configurationModelBuilder) {
    this.configurationModelBuilder = configurationModelBuilder;
  }

  @Autowired
  public void setMarkerBuilder(MarkerBuilder markerBuilder) {
    this.markerBuilder = markerBuilder;
  }

  @Autowired
  public void setTransDao(ITransDao transDao) {
    this.transDao = transDao;
  }

  @Autowired
  public void setInventoryManagementService(InventoryManagementService inventoryManagementService) {
    this.inventoryManagementService = inventoryManagementService;
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
  public void setMaterialCatalogService(MaterialCatalogService materialCatalogService) {
    this.materialCatalogService = materialCatalogService;
  }

  @RequestMapping("/entity/{entityId}")
  public
  @ResponseBody
  Results getEntityTransactions(
      @PathVariable Long entityId,
      @RequestParam(required = false) Long lEntityId,
      @RequestParam(required = false) String tag, @RequestParam(required = false) String from,
      @RequestParam(required = false) String to, @RequestParam(required = false) String type,
      @RequestParam(defaultValue = PageParams.DEFAULT_OFFSET_STR) int offset,
      @RequestParam(defaultValue = PageParams.DEFAULT_SIZE_STR) int size,
      @RequestParam(required = false) String bId,
      @RequestParam(required = false) boolean atd,
      @RequestParam(required = false) String reason,
      HttpServletRequest request) {
    return getAndBuildTransactions(request, from, to, offset, size, null,
        tag, type, entityId, lEntityId, null, bId, atd, reason);
  }

  /**
   * Get Transactions for material
   */
  @RequestMapping(value = "/material/{materialId}", method = RequestMethod.GET)
  public
  @ResponseBody
  Results getMaterialTransactions(
      @PathVariable Long materialId,
      @RequestParam(required = false) String ktag,
      @RequestParam(required = false) String from, @RequestParam(required = false) String to,
      @RequestParam(required = false) String type,
      @RequestParam(defaultValue = PageParams.DEFAULT_OFFSET_STR) int offset,
      @RequestParam(defaultValue = PageParams.DEFAULT_SIZE_STR) int size,
      @RequestParam(required = false) String bId,
      @RequestParam(required = false) boolean atd,
      @RequestParam(required = false) String reason,
      HttpServletRequest request) {
    return getAndBuildTransactions(request, from, to, offset, size, ktag,
        null, type, null, null, materialId, bId, atd, reason);

  }

  /**
   * Get Transactions from the domain.
   */
  @RequestMapping("/")
  public
  @ResponseBody
  Results getTransactions(
      @RequestParam(required = false) String tag, @RequestParam(required = false) String ktag,
      @RequestParam(required = false) String from, @RequestParam(required = false) String to,
      @RequestParam(required = false) String type,
      @RequestParam(defaultValue = PageParams.DEFAULT_OFFSET_STR) int offset,
      @RequestParam(defaultValue = PageParams.DEFAULT_SIZE_STR) int size,
      @RequestParam(required = false) String bId,
      @RequestParam(required = false) boolean atd,
      @RequestParam(required = false) String reason,
      @RequestParam(required = false) Long eid,
      @RequestParam(required = false) Long mid,
      @RequestParam(required = false) Long lEntityId,
      HttpServletRequest request) {
    return getAndBuildTransactions(request, from, to, offset, size, ktag, tag, type, eid, lEntityId,
        mid, bId, atd, reason);
  }

  @SuppressWarnings("unchecked")
  private Results getAndBuildTransactions(HttpServletRequest request,
                                          String from, String to, int offset, int size, String ktag,
                                          String mtag, String type, Long entityId, Long lEntityId,
                                          Long materialId, String bId, boolean atd, String reason) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    Long domainId = sUser.getCurrentDomainId();
    DomainConfig dc = DomainConfig.getInstance(domainId);
    ResourceBundle backendMessages = Resources.get().getBundle("BackendMessages", locale);
    try {
      Date startDate = null, endDate = null;
      if (from != null && !from.isEmpty()) {
        try {
          startDate = LocalDateUtil.parseCustom(from, Constants.DATE_FORMAT, dc.getTimezone());
        } catch (Exception e) {
          xLogger.warn("Exception when parsing start date " + from, e);
        }
      }
      if (to != null && !to.isEmpty()) {
        try {
          endDate = LocalDateUtil.parseCustom(to, Constants.DATE_FORMAT, dc.getTimezone());
        } catch (Exception e) {
          xLogger.warn("Exception when parsing start date " + to, e);
        }
      }
      Navigator navigator =
          new Navigator(request.getSession(), "TransactionsController.getAndBuildTransactions",
              offset, size, "dummy", 0);
      PageParams pageParams = new PageParams(navigator.getCursor(offset), offset, size);
      Results trnResults;
      List<Long> kioskIds = null;
      if (SecurityConstants.ROLE_SERVICEMANAGER.equals(sUser.getRole())) {
        kioskIds = entitiesService.getKioskIdsForUser(sUser.getUsername(), null, null).getResults();
        if (kioskIds.isEmpty()) {
          return new Results();
        }
      }
      trnResults = inventoryManagementService.getInventoryTransactions(startDate, endDate,
              domainId, entityId, materialId, type, lEntityId, ktag, mtag, kioskIds,
              pageParams, bId, atd, reason);
      trnResults.setOffset(offset);
      return transactionBuilder.buildTransactions(trnResults, sUser, SecurityUtils.getDomainId());
    } catch (ServiceException e) {
      xLogger.severe("Error in fetching transactions : {0}", e);
      throw new InvalidServiceException(backendMessages.getString("transactions.fetch.error"));
    }
  }

  @RequestMapping(value = "/undo", method = RequestMethod.POST)
  public
  @ResponseBody
  String undoTransactions(@RequestBody String[] tids, HttpServletRequest request) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale;
    if (sUser != null) {
      locale = sUser.getLocale();
    } else {
      String country = request.getParameter("country");
      if (country == null) {
        country = Constants.COUNTRY_DEFAULT;
      }
      String language = request.getParameter("language");
      if (language == null) {
        language = Constants.LANG_DEFAULT;
      }
      locale = new Locale(language, country);
    }
    ResourceBundle backendMessages = Resources.get().getBundle("BackendMessages", locale);
    if (tids == null || tids.length == 0) {
      throw new BadRequestException(backendMessages.getString("transactions.undo.error"));
    }
    List<String> tidsL = StringUtil.getList(tids);
    try {
      List<ITransaction> errorList = inventoryManagementService.undoTransactions(tidsL);
      int errors = errorList.size();
      int successes = tidsL.size() - errors;
      if (successes > 0 && errors == 0) {
        return successes + " " + backendMessages.getString("transactions.undo.success");
      } else {
        return backendMessages.getString("partial.success") + "." + MsgUtil.newLine() +
            successes + " " + backendMessages.getString("transactions.undo.success") +
            MsgUtil.newLine() + errors + " " + backendMessages.getString("transactions.undo.fail");
      }
    } catch (ServiceException e) {
      xLogger.severe("Error in undo transactions: {0}", e);
      throw new InvalidServiceException(backendMessages.getString("transactions.undo.error"));
    }
  }

  @RequestMapping(value = "/transconfig/", method = RequestMethod.GET)
  public
  @ResponseBody
  TransactionDomainConfigModel getTransactionConfig(@RequestParam Long kioskId) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    String userId = sUser.getUsername();
    Long domainId = sUser.getCurrentDomainId();
    String role = sUser.getRole();
    DomainConfig dc = DomainConfig.getInstance(domainId);
    InventoryConfig ic = dc.getInventoryConfig();
    TransactionDomainConfigModel model = new TransactionDomainConfigModel();
    Map<String, ReasonConfig> reasons = ic.getTransReasons();
    model.showCInv = ic.getPermissions() != null && ic.getPermissions().invCustomersVisible;
    reasons.entrySet().forEach(entry -> model.addReason(entry.getKey(),configurationModelBuilder.buildReasonConfigModel(entry.getValue())));
    try {
      List cust = entitiesService.getKioskLinks(kioskId, IKioskLink.TYPE_CUSTOMER, null, null, null).getResults();
      model.customers = constructKioskMap(cust);
      List vend = entitiesService.getKioskLinks(kioskId, IKioskLink.TYPE_VENDOR, null, null, null).getResults();
      model.vendors = constructKioskMap(vend);
      model.isMan = SecurityConstants.ROLE_SERVICEMANAGER.equals(role);
      IUserAccount u = usersService.getUserAccount(userId);
      List dest = entitiesService.getKiosks(u, domainId, null, null, null).getResults();
      model.dest = constructKioskMap(dest);

      ActualTransConfig atci = ic.getActualTransConfigByType(ITransaction.TYPE_ISSUE);
      model.atdi = atci != null ? atci.getTy() : ActualTransConfig.ACTUAL_NONE;

      ActualTransConfig atcr = ic.getActualTransConfigByType(ITransaction.TYPE_RECEIPT);
      model.atdr = atcr != null ? atcr.getTy() : ActualTransConfig.ACTUAL_NONE;

      ActualTransConfig atcp = ic.getActualTransConfigByType(ITransaction.TYPE_PHYSICALCOUNT);
      model.atdp = atcp != null ? atcp.getTy() : ActualTransConfig.ACTUAL_NONE;

      ActualTransConfig atcw = ic.getActualTransConfigByType(ITransaction.TYPE_WASTAGE);
      model.atdw = atcw != null ? atcw.getTy() : ActualTransConfig.ACTUAL_NONE;

      ActualTransConfig atct = ic.getActualTransConfigByType(ITransaction.TYPE_TRANSFER);
      model.atdt = atct != null ? atct.getTy() : ActualTransConfig.ACTUAL_NONE;

      ActualTransConfig atcri = ic.getActualTransConfigByType(ITransaction.TYPE_RETURNS_INCOMING);
      model.atdri = atcri != null ? atcri.getTy() : ActualTransConfig.ACTUAL_NONE;

      ActualTransConfig atcro = ic.getActualTransConfigByType(ITransaction.TYPE_RETURNS_OUTGOING);
      model.atdro = atcro != null ? atcro.getTy() : ActualTransConfig.ACTUAL_NONE;
      model.atdc = configurationModelBuilder.buildActualTransConfigAsStringByTransType(ic);
      PageParams pageParams = new PageParams(1);
      model.noc =
          entitiesService.getKioskLinks(kioskId, IKioskLink.TYPE_CUSTOMER, null, null, pageParams).getNumFound();
      model.nov =
          entitiesService.getKioskLinks(kioskId, IKioskLink.TYPE_VENDOR, null, null, pageParams).getNumFound();
    } catch (ServiceException | ObjectNotFoundException e) {
      xLogger.severe("Error in getting Transaction Domain Config: {0}", e);
    }


    return model;
  }

  private List<TransactionModel> constructKioskMap(List cust)
      throws ServiceException {
    List<TransactionModel> kioskList = new ArrayList<>(cust.size());
    for (Object o : cust) {
      IKiosk kiosk = null;
      if (o instanceof IKioskLink) {
        try {
          kiosk = entitiesService.getKiosk(((IKioskLink) o).getLinkedKioskId(), false);
        } catch (Exception e) {
          xLogger.warn("Kiosk not found: {0}", ((IKioskLink) o).getLinkedKioskId());
          continue;
        }
      } else if (o instanceof IKiosk) {
        kiosk = (IKiosk) o;
      }
      kioskList.add(constructKioskTModel(kiosk));
    }
    return kioskList;
  }

  @RequestMapping(value = "/reasons", method = RequestMethod.GET)
  public
  @ResponseBody
  ReasonConfigModel getReasons(@RequestParam String type, String[] tags) {
    Long domainId = SecurityUtils.getCurrentDomainId();
    DomainConfig dc = DomainConfig.getInstance(domainId);
    InventoryConfig ic = dc.getInventoryConfig();
    List<ReasonConfigModel> reasonConfigModels = null;
    if (tags != null) {
      reasonConfigModels = new ArrayList<>(tags.length);
      switch (type) {
        case ITransaction.TYPE_ISSUE:
          for (String mtag : tags) {
            reasonConfigModels.add(
                configurationModelBuilder.buildReasonConfigModel(ic.getImTransReasonConfig(mtag)));
          }
          break;
        case ITransaction.TYPE_RECEIPT:
          for (String mtag : tags) {
            reasonConfigModels.add(configurationModelBuilder.buildReasonConfigModel(ic.getRmTransReasonConfig(
                mtag)));
          }
          break;
        case ITransaction.TYPE_TRANSFER:
          for (String mtag : tags) {
            reasonConfigModels.add(configurationModelBuilder.buildReasonConfigModel(ic.getTmTransReasonConfig(
                mtag)));
          }
          break;
        case ITransaction.TYPE_PHYSICALCOUNT:
          for (String mtag : tags) {
            reasonConfigModels.add(configurationModelBuilder.buildReasonConfigModel(ic.getSmTransReasonConfig(
                mtag)));
          }
          break;
        case ITransaction.TYPE_WASTAGE:
          for (String mtag : tags) {
            reasonConfigModels.add(configurationModelBuilder.buildReasonConfigModel(ic.getDmTransReasonConfig(
                mtag)));
          }
          break;
        case ITransaction.TYPE_RETURNS_INCOMING:
          for (String mtag : tags) {
            reasonConfigModels.add(configurationModelBuilder.buildReasonConfigModel(ic.getReturnIncomingReasonConfigByMtag(
                mtag)));
          }
          break;
        case ITransaction.TYPE_RETURNS_OUTGOING:
          for (String mtag : tags) {
            reasonConfigModels.add(configurationModelBuilder.buildReasonConfigModel(ic.getReturnOutgoingReasonConfigByMtag(
                mtag)));
          }
          break;
      }
    }
    ReasonConfigModel reasonConfigModel = new ReasonConfigModel();
    if (CollectionUtils.isNotEmpty(reasonConfigModels)) {
      reasonConfigModels.forEach(entry -> {
        reasonConfigModel.rsns.addAll(new ArrayList<>(new LinkedHashSet<>(entry.rsns)));
        if (reasonConfigModel.defRsn == null) {
          reasonConfigModel.defRsn = entry.defRsn;
        }
      });
    }
    return reasonConfigModel;
  }

  @RequestMapping(value = "/matStatus", method = RequestMethod.GET)
  public
  @ResponseBody
  List<String> getMatStatus(@RequestParam String type, Boolean ts) {
    DomainConfig dc = DomainConfig.getInstance(SecurityUtils.getCurrentDomainId());
    InventoryConfig ic = dc.getInventoryConfig();
    List<String> statusList = null;
    if (type != null && !type.isEmpty()) {
      MatStatusConfig ms = ic.getMatStatusConfigByType(type);
      if (ms != null) {
        if (ts && ms.getEtsm() != null && !ms.getEtsm().isEmpty()) {
          statusList =
              new ArrayList<>(
                  new LinkedHashSet<>(Arrays.asList(ms.getEtsm().split(CharacterConstants.COMMA))));
        } else if (ms.getDf() != null && !ms.getDf().isEmpty()) {
          statusList =
              new ArrayList<>(
                  new LinkedHashSet<>(Arrays.asList(ms.getDf().split(CharacterConstants.COMMA))));
        }
      }
    }
    if (statusList != null && !statusList.get(0).isEmpty()) {
      statusList.add(0, CharacterConstants.EMPTY);
    }
    return statusList;
  }

  private TransactionModel constructKioskTModel(IKiosk kiosk) {
    TransactionModel m = new TransactionModel();
    m.eid = kiosk.getKioskId();
    m.enm = kiosk.getName();
    m.st = kiosk.getState();
    m.ds = kiosk.getDistrict();
    m.ct = kiosk.getCity();
    return m;
  }


  @RequestMapping(value = "/add/", method = RequestMethod.POST)
  public
  @ResponseBody
  String addTransaction(@RequestBody Map<String, Object> transaction) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale;
    if (sUser.getLocale() != null) {
      locale = sUser.getLocale();
    } else {
      locale = new Locale(Constants.LANG_DEFAULT, Constants.COUNTRY_DEFAULT);
    }
    ResourceBundle backendMessages = Resources.get().getBundle("BackendMessages", locale);
    String userId = sUser.getUsername();
    Long domainId = sUser.getCurrentDomainId();

    MemcacheService cache = null;
    String signature =
        transaction.get("signature") != null ? String.valueOf(transaction.get("signature")) : null;
    if (signature != null) {
      cache = AppFactory.get().getMemcacheService();
      if (cache != null) {
        // Check if the signature exists in cache
        Integer lastStatus = (Integer) cache.get(signature);
        if (lastStatus != null) {
          switch (lastStatus) {
            case DedupUtil.SUCCESS:
              return backendMessages.getString("transactions.create.success");
            case DedupUtil.PENDING:
              return backendMessages.getString("transaction.verify.message");
            case DedupUtil.FAILED:
              DedupUtil.setSignatureAndStatus(cache, signature, DedupUtil.PENDING);
              break;
            default:
              break;
          }
        } else {
          DedupUtil.setSignatureAndStatus(cache, signature, DedupUtil.PENDING);
        }
      }
    }

    String transType = String.valueOf(transaction.get("transtype"));
    DomainConfig dc = DomainConfig.getInstance(domainId);
    InventoryConfig ic = dc.getInventoryConfig();
    String
        atdStr =
        ic.getActualTransConfigByType(transType) != null ? ic.getActualTransConfigByType(transType)
            .getTy() : null;
    boolean transMandate = "2".equals(atdStr);
    Long kioskId = Long.parseLong((String) transaction.get("kioskid"));
    Long linkedKioskId = null;
    if (transaction.containsKey("lkioskid")) {
      linkedKioskId = Long.parseLong(String.valueOf(transaction.get("lkioskid")));
    }
    boolean checkBatchMgmt = ITransaction.TYPE_TRANSFER.equals(transType);
    try {
      IKiosk kiosk = null;
      IKiosk destKiosk = null;
      if (checkBatchMgmt) {
        kiosk = entitiesService.getKiosk(kioskId, false);
        destKiosk = entitiesService.getKiosk(linkedKioskId, false);
        checkBatchMgmt = !kiosk.isBatchMgmtEnabled() && destKiosk.isBatchMgmtEnabled();
      }
      List<ITransaction> transList = new ArrayList<>();
      Date now = new Date();
      Date actualTransDate = null;
      if (transMandate && !transaction.containsKey("transactual")) {
        return backendMessages.getString("error.adt.entry.mandate");
      }
      if (transaction.containsKey("transactual") && !"0".equals(atdStr)) {
        actualTransDate =
            LocalDateUtil
                .parseCustom(String.valueOf(transaction.get("transactual")), Constants.DATE_FORMAT,
                    null);
      }
      List<LinkedTreeMap> items = (List) transaction.get("materials");
      List<String> berrorMaterials = new ArrayList<>(1);
      for (LinkedTreeMap materials : items) {
        for (Object m : materials.keySet()) {
          Long materialId = Long.parseLong(String.valueOf(m));
          LinkedTreeMap mat = (LinkedTreeMap) materials.get(m);
          BigDecimal quantity = new BigDecimal(Long.parseLong(String.valueOf(mat.get("q"))));
          String reason = String.valueOf(mat.get("r"));
          if (reason.equals("null")) {
            reason = "";
          }
          String status = String.valueOf(mat.get("mst"));
          if (status.equals("null")) {
            status = "";
          }
          if (checkBatchMgmt) {
            IMaterial material = materialCatalogService.getMaterial(materialId);
            if (material.isBatchEnabled()) {
              berrorMaterials.add(material.getName());
            }
          }
          String trkid = String.valueOf(mat.get("trkid"));
          ITransaction
              trans =
              getTransaction(userId, domainId, transType, kioskId, linkedKioskId, reason, status,
                  now,
                  materialId, quantity, "", actualTransDate, trkid);
          transList.add(trans);
        }
      }
      if (!berrorMaterials.isEmpty()) {
        xLogger.info(
            "Transfer rejected from {0} to {1}, Source is batch disabled but destination is enabled.",
            kiosk.getName(), destKiosk.getName());
        StringBuilder builder = new StringBuilder();
        builder.append(backendMessages.getString("transactions.restricted.error.1"))
            .append(" ")
            .append(berrorMaterials.size())
            .append(" ")
            .append(backendMessages.getString("materials"))
            .append(" ")
            .append(backendMessages.getString("from"))
            .append(" ")
            .append(kiosk.getName())
            .append(" ")
            .append(backendMessages.getString("transactions.restricted.error.2"))
            .append(MsgUtil.newLine())
            .append(StringUtil.getCSV(berrorMaterials));
        throw new BadRequestException(builder.toString());
      }
      List<LinkedTreeMap> batchItems = (List) transaction.get("bmaterials");
      for (LinkedTreeMap batchMaterials: batchItems) {
        for (Object m : batchMaterials.keySet()) {
          String keys[] = String.valueOf(m).split("\t");
          Long materialId = Long.parseLong(keys[0]);
          String batch = keys[1];
          LinkedTreeMap batchMaterial = (LinkedTreeMap) batchMaterials.get(m);
          BigDecimal
              quantity =
              new BigDecimal(Long.parseLong(String.valueOf(batchMaterial.get("q"))));
          String expiry = String.valueOf(batchMaterial.get("e"));
          String manufacturer = String.valueOf(batchMaterial.get("mr"));
          String manufactured = String.valueOf(batchMaterial.get("md"));
          String reason = String.valueOf(batchMaterial.get("r"));
          if (reason.equals("null")) {
            reason = "";
          }
          String status = String.valueOf(batchMaterial.get("mst"));
          if (status.equals("null")) {
            status = "";
          }
          if (manufactured.equals("null")) {
            manufactured = "";
          }
          String trkid = String.valueOf(batchMaterial.get("trkid"));
          ITransaction
              trans =
              getTransaction(userId, domainId, transType, kioskId, linkedKioskId, reason, status,
                  now,
                  materialId, quantity, batch, actualTransDate, trkid);
          TransactionUtil.setBatchData(trans, batch, expiry, manufacturer, manufactured);
          transList.add(trans);
        }
    }

      List<ITransaction> errors = inventoryManagementService.updateInventoryTransactions(domainId, transList, true).getErrorTransactions();
      if (errors != null && errors.size() > 0) {
        StringBuilder errorMsg = new StringBuilder();
        for (ITransaction error : errors) {
          errorMsg.append("-").append(error.getMessage()).append(MsgUtil.newLine());
        }
        return backendMessages.getString("errors.oneormore") + ":" +
            MsgUtil.newLine() + errorMsg + MsgUtil.newLine() + MsgUtil.newLine() +
            backendMessages.getString("transactions.resubmit");
      }
      DedupUtil.setSignatureAndStatus(cache, signature, DedupUtil.SUCCESS);
    } catch (ServiceException e) {
      xLogger.severe("Error in creating transaction: {0}", e);
      DedupUtil.setSignatureAndStatus(cache, signature, DedupUtil.FAILED);
      throw new InvalidServiceException(backendMessages.getString("transactions.create.error"));
    } catch (DuplicationException | ParseException e) {
      xLogger.warn("Error in creating transaction: {0}", e);
      DedupUtil.setSignatureAndStatus(cache, signature, DedupUtil.FAILED);
      throw new InvalidServiceException(
          backendMessages.getString("transactions.create.error") + ". " + e.getMessage());
    }

    return backendMessages.getString("transactions.create.success");
  }

  private ITransaction getTransaction(String userId, Long domainId, String transType, Long kioskId,
                                      Long linkedKioskId, String reason, String matStatus, Date now,
                                      Long materialId, BigDecimal quantity, String batch,
                                      Date actualTransDate, String trkid) {
    ITransaction trans = JDOUtils.createInstance(ITransaction.class);
    trans.setDomainId(domainId);
    trans.setKioskId(kioskId);
    trans.setMaterialId(materialId);
    trans.setQuantity(quantity);
    trans.setType(transType);
    trans.setSourceUserId(userId);
    trans.setTimestamp(now);
    trans.setReason(reason);
    trans.setSrc(SourceConstants.WEB);
    trans.setBatchId(batch);
    trans.setMaterialStatus(matStatus);
    trans.setAtd(actualTransDate);
    if (linkedKioskId != null) {
      trans.setLinkedKioskId(linkedKioskId);
    }
    trans.setTrackingId(trkid);
    if (ITransaction.TYPE_RETURNS_INCOMING.equals(transType)) {
      trans.setTrackingObjectType(ITransaction.TYPE_ISSUE_TRANSACTION);
    } else if (ITransaction.TYPE_RETURNS_OUTGOING.equals(transType)) {
      trans.setTrackingObjectType(ITransaction.TYPE_RECEIPT_TRANSACTION);
    }
    transDao.setKey(trans);
    return trans;
  }

  @RequestMapping(value = "/actualroute", method = RequestMethod.GET)
  public
  @ResponseBody
  List<MarkerModel> getActualRoute(@RequestParam String userId, @RequestParam String from,
                                   @RequestParam String to) {
    try {
      SecureUserDetails sUser = SecurityUtils.getUserDetails();
      DomainConfig dc = DomainConfig.getInstance(sUser.getCurrentDomainId());
      Results
          results =
          inventoryManagementService.getInventoryTransactionsByUser(userId,
              LocalDateUtil.parseCustom(from, Constants.DATE_FORMAT_CSV, dc.getTimezone()),
              LocalDateUtil.parseCustom(to, Constants.DATE_FORMAT_CSV, dc.getTimezone()), null);
      return markerBuilder.buildMarkerListFromTransactions(results.getResults(),
          sUser.getLocale(), sUser.getTimezone());
    } catch (ServiceException | ParseException e) {
      xLogger.severe("Error in reading destination inventories: {0}", e);
    }
    return null;
  }

  @RequestMapping(value = "/checkpermission", method = RequestMethod.GET)
  public
  @ResponseBody
  Integer checkPermission(@RequestParam String userId, @RequestParam Long kioskId) {
    Integer permission = 0;
    try {
      IUserAccount userAccount = usersService.getUserAccount(userId);
      permission = EntityAuthoriser.authoriseEntityPerm(kioskId,
          userAccount.getRole(), userId, userAccount.getDomainId());
    } catch (ServiceException | ObjectNotFoundException e) {
      xLogger.severe("Error in reading user details : {0}", userId);
    }
    return permission;
  }
  @RequestMapping(value = "/statusmandatory", method = RequestMethod.GET)
  public
  @ResponseBody
  Map<String,Boolean> getStatusMandatory() {
    Map<String,Boolean> statusList = new HashMap<>(5);
    DomainConfig dc = DomainConfig.getInstance(SecurityUtils.getCurrentDomainId());
    InventoryConfig ic = dc.getInventoryConfig();
    MatStatusConfig msc = ic.getMatStatusConfigByType(ITransaction.TYPE_ISSUE);
    if(msc != null) {
      statusList.put("ism", msc.isStatusMandatory());
    }
    msc = ic.getMatStatusConfigByType(ITransaction.TYPE_RECEIPT);
    if(msc != null) {
      statusList.put("rsm", msc.isStatusMandatory());
    }
    msc = ic.getMatStatusConfigByType(ITransaction.TYPE_PHYSICALCOUNT);
    if(msc != null) {
      statusList.put("psm", msc.isStatusMandatory());
    }
    msc = ic.getMatStatusConfigByType(ITransaction.TYPE_WASTAGE);
    if(msc != null) {
      statusList.put("wsm", msc.isStatusMandatory());
    }
    msc = ic.getMatStatusConfigByType(ITransaction.TYPE_TRANSFER);
    if(msc != null) {
      statusList.put("tsm", msc.isStatusMandatory());
    }
    msc = ic.getMatStatusConfigByType(ITransaction.TYPE_RETURNS_INCOMING);
    if(msc != null) {
      statusList.put("rism", msc.isStatusMandatory());
    }
    msc = ic.getMatStatusConfigByType(ITransaction.TYPE_RETURNS_OUTGOING);
    if(msc != null) {
      statusList.put("rosm", msc.isStatusMandatory());
    }
    return statusList;
  }
}
