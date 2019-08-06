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

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import com.logistimo.AppFactory;
import com.logistimo.api.constants.ConfigConstants;
import com.logistimo.api.models.MenuStatsModel;
import com.logistimo.api.models.configuration.AccountingConfigModel;
import com.logistimo.api.models.configuration.AdminContactConfigModel;
import com.logistimo.api.models.configuration.ApprovalsConfigModel;
import com.logistimo.api.models.configuration.ApprovalsEnabledConfigModel;
import com.logistimo.api.models.configuration.AssetConfigModel;
import com.logistimo.api.models.configuration.AssetSystemConfigModel;
import com.logistimo.api.models.configuration.AssetType;
import com.logistimo.api.models.configuration.CapabilitiesConfigModel;
import com.logistimo.api.models.configuration.DashboardConfigModel;
import com.logistimo.api.models.configuration.FormsConfigModel;
import com.logistimo.api.models.configuration.GeneralConfigModel;
import com.logistimo.api.models.configuration.InventoryConfigModel;
import com.logistimo.api.models.configuration.Manufacturer;
import com.logistimo.api.models.configuration.Model;
import com.logistimo.api.models.configuration.MonitoringPoint;
import com.logistimo.api.models.configuration.OrdersConfigModel;
import com.logistimo.api.models.configuration.ReasonConfigModel;
import com.logistimo.api.models.configuration.ReturnsConfigModel;
import com.logistimo.api.models.configuration.Sensor;
import com.logistimo.api.models.configuration.StockRebalancingConfigModel;
import com.logistimo.api.models.configuration.SupportConfigModel;
import com.logistimo.api.models.configuration.TagsConfigModel;
import com.logistimo.api.models.configuration.WorkingStatus;
import com.logistimo.assets.entity.IAsset;
import com.logistimo.auth.SecurityConstants;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.common.builder.MediaBuilder;
import com.logistimo.config.entity.IConfig;
import com.logistimo.config.models.AccountingConfig;
import com.logistimo.config.models.ActualTransConfig;
import com.logistimo.config.models.AdminContactConfig;
import com.logistimo.config.models.ApprovalsConfig;
import com.logistimo.config.models.AssetConfig;
import com.logistimo.config.models.AssetSystemConfig;
import com.logistimo.config.models.CapabilityConfig;
import com.logistimo.config.models.ConfigurationException;
import com.logistimo.config.models.DashboardConfig;
import com.logistimo.config.models.DemandBoardConfig;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.config.models.FormsConfig;
import com.logistimo.config.models.GeneralConfig;
import com.logistimo.config.models.InventoryConfig;
import com.logistimo.config.models.LeadTimeAvgConfig;
import com.logistimo.config.models.MatStatusConfig;
import com.logistimo.config.models.OptimizerConfig;
import com.logistimo.config.models.OrdersConfig;
import com.logistimo.config.models.ReasonConfig;
import com.logistimo.config.models.ReturnsConfig;
import com.logistimo.config.models.StockRebalancingConfig;
import com.logistimo.config.models.SupportConfig;
import com.logistimo.config.models.SyncConfig;
import com.logistimo.config.models.TransportersConfig;
import com.logistimo.api.models.configuration.TransportersConfigModel;
import com.logistimo.config.models.TransportersSystemConfig;
import com.logistimo.config.service.ConfigurationMgmtService;
import com.logistimo.constants.CharacterConstants;
import com.logistimo.constants.Constants;
import com.logistimo.dao.JDOUtils;
import com.logistimo.domains.entity.IDomain;
import com.logistimo.domains.service.DomainsService;
import com.logistimo.entities.service.EntitiesService;
import com.logistimo.exception.ForbiddenAccessException;
import com.logistimo.exception.InvalidServiceException;
import com.logistimo.exception.SystemException;
import com.logistimo.inventory.entity.ITransaction;
import com.logistimo.logger.XLog;
import com.logistimo.media.endpoints.IMediaEndPoint;
import com.logistimo.media.entity.IMedia;
import com.logistimo.models.MediaModel;
import com.logistimo.orders.entity.IOrder;
import com.logistimo.pagination.PageParams;
import com.logistimo.pagination.Results;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.ObjectNotFoundException;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;
import com.logistimo.services.storage.StorageUtil;
import com.logistimo.services.utils.ConfigUtil;
import com.logistimo.tags.TagUtil;
import com.logistimo.transporters.actions.GetTransportersAction;
import com.logistimo.transporters.model.ConsignmentCategoryModel;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.users.service.UsersService;
import com.logistimo.utils.LocalDateUtil;
import com.logistimo.utils.StringUtil;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
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
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by Mohan Raja on 14/03/15
 */

@Component
public class ConfigurationModelBuilder {
  private static final XLog xLogger = XLog.getLog(ConfigurationModelBuilder.class);
  private static final String UPLOADS = "uploads";
  private static final String LOGO = "logo.png";
  private static final String INVOICE_TEMPLATE = "logistimo_invoice.jrxml";
  private static final String SHIPMENT_TEMPLATE = "logistimo_shipment.jrxml";
  private static final String DOWNLOAD_LINK = "/s2/api/export/download?isBlobKey=true";

  private UsersService usersService;
  private DomainsService domainsService;
  private EntitiesService entitiesService;
  private ConfigurationMgmtService configurationMgmtService;
  private UserBuilder userBuilder;
  private GetTransportersAction getTransportersAction;
  private ModelMapper modelMapper = new ModelMapper();

  @Autowired
  public void setUsersService(UsersService usersService) {
    this.usersService = usersService;
  }

  @Autowired
  public void setDomainsService(DomainsService domainsService) {
    this.domainsService = domainsService;
  }

  @Autowired
  public void setEntitiesService(EntitiesService entitiesService) {
    this.entitiesService = entitiesService;
  }

  @Autowired
  public void setUserBuilder(UserBuilder userBuilder) {
    this.userBuilder = userBuilder;
  }

  @Autowired
  public void setConfigurationMgmtService(ConfigurationMgmtService configurationMgmtService) {
    this.configurationMgmtService = configurationMgmtService;
  }

  @Autowired
  public void setGetTransportersAction(GetTransportersAction getTransportersAction) {
    this.getTransportersAction = getTransportersAction;
  }

  public MenuStatsModel buildMenuStats(SecureUserDetails user, DomainConfig config, Locale locale,
                                       String timezone)
      throws ServiceException {
    Long domainId = SecurityUtils.getCurrentDomainId();
    MenuStatsModel model = new MenuStatsModel();
    model.iAccTbEn = config.isAccountingEnabled();
    model.iOrdTbEn =
        !config.isCapabilityDisabled(DomainConfig.CAPABILITY_ORDERS);
    boolean isEntityManager = SecurityConstants.ROLE_SERVICEMANAGER.equals(user.getRole());
    boolean isEntityOperator = SecurityConstants.ROLE_KIOSKOWNER.equals(user.getRole());
    model.iConfTbEn = !isEntityManager && !isEntityOperator;
    model.iRepTbEn = !(isEntityManager || isEntityOperator);
    model.iAdm = !(isEntityManager || isEntityOperator);
    model.iSU = SecurityConstants.ROLE_SUPERUSER.equals(user.getRole());
    model.iMan = isEntityManager;
    IUserAccount userAccount;
    try {
      IDomain domain = domainsService.getDomain(domainId);
      userAccount = usersService.getUserAccount(user.getUsername());
      model.iAU = IUserAccount.PERMISSION_ASSET.equals(userAccount.getPermission());
      List<String>
          hideUTags =
          StringUtil.getList(config.getDashboardConfig().getDbOverConfig().dutg);
      if (hideUTags != null) {
        for (String userTag : userAccount.getTags()) {
          if (hideUTags.contains(userTag)) {
            model.hbUTag = true;
            break;
          }
        }
      }
      if (StringUtils.isNotEmpty(userAccount.getFullName())) {
        model.ufn = userAccount.getFullName();
      } else {
        model.ufn = userAccount.getFirstName();
      }
      model.unm = userAccount.getUserId();
      model.utgs = userAccount.getTags() != null ? userAccount.getTags() : Collections.EMPTY_LIST;
      model.dnm = domain.getName();
      model.dId = domain.getId();
      model.lng = userAccount.getLanguage();
      model.locale = userAccount.getLocale().toLanguageTag();
      model.utz = userAccount.getTimezone();
      model.em = userAccount.getEmail();
      if (userAccount.getDomainId().equals(domainId)) {
        model.eid = userAccount.getPrimaryKiosk();
      }
      model.createdOn = domain.getCreatedOn();
      model.hasChild = domain.getHasChild();
      if (model.eid == null && SecurityConstants.ROLE_SERVICEMANAGER.equals(user.getRole())) {
          Results results = entitiesService.getKioskIdsForUser(user.getUsername(), null,
              new PageParams(null, 0, 2));
          List kiosks = results.getResults();
          if (kiosks!=null && kiosks.size() == 1) {
            model.eid = (Long) kiosks.get(0);
          }
      }
    } catch (Exception e) {
      throw new ServiceException("Severe exception while fetching domain details", e);
    }
    if (config.getAssetConfig() != null) {
      model.iTempOnly = config.getAssetConfig().isTemperatureMonitoringEnabled();
      model.iTempWLg = config.getAssetConfig().isTemperatureMonitoringWithLogisticsEnabled();
    }
    model.iDmdOnly = "1".equals(config.getOrderGeneration());
    model.hImg = config.getPageHeader();
    model.cur = config.getCurrency();
    model.cnt = config.getCountry();
    model.st = config.getState();
    model.dst = config.getDistrict();
    model.iOCEnabled = !config.getUiPreference();
    boolean isForceNewUI = ConfigUtil.getBoolean("force.newui", false);
    model.onlyNewUI = isForceNewUI || config.isOnlyNewUIEnabled();
    model.support = buildAllSupportConfigModels(config);
    model.admin = buildAllAdminContactConfigModel(config.getAdminContactConfig());
    model.iPredEnabled =
        config.getInventoryConfig() != null && config.getInventoryConfig().showPredictions();
    model.mmt =
        config.getInventoryConfig() == null ? InventoryConfig.MIN_MAX_ABS_QTY
            : config.getInventoryConfig().getMinMaxType();
    model.mmd =
        config.getInventoryConfig() == null ? Constants.FREQ_DAILY
            : config.getInventoryConfig().getMinMaxDur();
    model.tr = config.getOrdersConfig() != null && config.getOrdersConfig().isTransferRelease();

    if (model.iSU) {
      model.accd = true;
    } else if (model.iAdm) {
      try {
        List<Long> accDids = userAccount.getAccessibleDomainIds();
        int size = accDids == null ? 0 : accDids.size();
        if (size > 1) {
          model.accd = true;
        } else if (size == 1) {
          IDomain domain = domainsService.getDomain(accDids.get(0));
          model.accd = domain.getHasChild();
        }
      } catch (Exception e) {
        xLogger
            .severe(
                "Unable to set accessible domain id flag for user {0}, domain: {1}, exce: {2}",
                user.getUsername(), user.getDomainId(), e);
        throw new ServiceException("Severe exception while fetching domain details", e);

      }
    }
    try {
      model.ac = buildAssetConfigModel(config, locale, timezone);
    } catch (Exception e) {
      xLogger
          .warn("Unable to get asset config for the domain {0}, exception: {1}",
              user.getDomainId(),
              e);
    }
    try {
      if (config.getDashboardConfig().getDbOverConfig() != null) {
        model.iATD = !config.getDashboardConfig().getDbOverConfig().atdd;
      }
    } catch (Exception e) {
      xLogger.warn("atdd value null", e);
    }
    try {
      if(config.getApprovalsConfig() != null) {
        model.apc = buildApprovalsConfigModel(config.getApprovalsConfig(), domainId, locale, timezone);
        if(CollectionUtils.isNotEmpty(model.apc.pa)) {
          model.toae = true;
        }
        if(CollectionUtils.isNotEmpty(model.apc.psoa)) {
          List<ApprovalsConfigModel.PurchaseSalesOrderApproval> psoa = model.apc.psoa;
          for(ApprovalsConfigModel.PurchaseSalesOrderApproval ps : psoa) {
            if(!model.poae && ps.poa) {
              model.poae = true;
            }
            if(!model.soae && ps.soa) {
              model.soae = true;
              break;
            }
          }
        }
      }
    } catch (Exception e) {
      xLogger.fine("Unable to fetch approval config for {0}", domainId, e);
    }
    model.allocateInventory = config.autoGI();
    if (SecurityConstants.ROLE_SERVICEMANAGER.equalsIgnoreCase(user.getRole())
        && config.getCapabilityMapByRole() != null) {
      CapabilityConfig cc;
      cc = config.getCapabilityMapByRole().get(SecurityConstants.ROLE_SERVICEMANAGER);
      if (cc != null) {
        model.vt = cc.isCapabilityDisabled("vt");
        model.ct = cc.isCapabilityDisabled("ct");
        model.vo = cc.isCapabilityDisabled("vo");
        model.ns = cc.isCapabilityDisabled("ns");
      }
    }
    //adding revenue report render flag
    if (null != config.getDashboardConfig().getRevenuePanelConfig()) {
      model.rpe = config.getDashboardConfig().getRevenuePanelConfig().showRevenuePanel;
    }
    model.mdp = model.iAdm;
    if(!model.mdp && null != config.getDashboardConfig().getDbOverConfig()) {
      model.mdp = config.getDashboardConfig().getDbOverConfig().edm;
    }
    return model;
  }

  public List<List<String>> populateEntityTagsCombination(StockRebalancingConfigModel model) {
    List<List<String>> tagsCombination = new ArrayList<>();
    if(model != null && model.getEntityTagsCombination() != null && !model.getEntityTagsCombination().isEmpty()) {
      for(StockRebalancingConfigModel.EntityTagsCombination combination : model.getEntityTagsCombination()) {
        List<String> tags = combination.getEntityTags().stream().collect(Collectors.toList());
        tagsCombination.add(tags);
      }
    }
    return tagsCombination;
  }

  public StockRebalancingConfig buildStockRebalancingConfig(StockRebalancingConfigModel model) {
    if(model != null) {
      StockRebalancingConfig config = new StockRebalancingConfig();
      config.setEnableStockRebalancing(model.isEnableStockRebalancing());
      config.setMtTags(model.getMtTags());
      config.setEntityTagsCombination(populateEntityTagsCombination(model));
      config.setGeoFencing(model.getGeoFencing());
      config.setExpiryCheck(model.isExpiryCheck());
      if(model.isStockOutDurationExceedsThreshold()) {
        config.setStockOutDurationExceedsThreshold(model.isStockOutDurationExceedsThreshold());
        config.setAcceptableLeadTime(model.getAcceptableLeadTime());
      }
      if(model.isMaxStock()) {
        config.setMaxStock(model.isMaxStock());
        config.setMaxStockDays(model.getMaxStockDays());
      }
      config.setTransportationCost(model.getTransportationCost());
      config.setInventoryHoldingCost(model.getInventoryHoldingCost());
      config.setHandlingCharges(model.getHandlingCharges());

      return config;
    }
    return null;
  }

  public ApprovalsConfig.OrderConfig buildApprovalsOrderConfig(ApprovalsConfigModel model) {
    if (model != null) {
      ApprovalsConfig.OrderConfig orderConfig = new ApprovalsConfig.OrderConfig();
      List<ApprovalsConfig.PurchaseSalesOrderConfig>
          purchaseSalesOrderConfigList =
          new ArrayList<>();
      if (CollectionUtils.isNotEmpty(model.psoa)) {
        for (ApprovalsConfigModel.PurchaseSalesOrderApproval psa : model.psoa) {
          ApprovalsConfig.PurchaseSalesOrderConfig
              psConfig =
              new ApprovalsConfig.PurchaseSalesOrderConfig();
          psConfig.setPurchaseOrderApproval(psa.poa);
          psConfig.setSalesOrderApproval(psa.soa);
          psConfig.setEntityTags(psa.eTgs);
          purchaseSalesOrderConfigList.add(psConfig);
        }
        orderConfig.setPurchaseSalesOrderApproval(purchaseSalesOrderConfigList);
        if (model.px == 0) {
          orderConfig.setPurchaseOrderApprovalExpiry(24);
        } else {
          orderConfig.setPurchaseOrderApprovalExpiry(model.px);
        }
        if (model.sx == 0) {
          orderConfig.setSalesOrderApprovalExpiry(24);
        } else {
          orderConfig.setSalesOrderApprovalExpiry(model.sx);
        }
      }
      if (CollectionUtils.isNotEmpty(model.pa)) {
        List<String> puIds = new ArrayList<>();
        for (int i = 0; i < model.pa.size(); i++) {
          puIds.add(model.pa.get(i).id);
        }
        orderConfig.setPrimaryApprovers(puIds);
      }
      if (CollectionUtils.isNotEmpty(model.sa)) {
        List<String> suIds = new ArrayList<>();
        for (int i = 0; i < model.sa.size(); i++) {
          suIds.add(model.sa.get(i).id);
        }
        orderConfig.setSecondaryApprovers(suIds);
      }

      if (model.tx == 0) {
        orderConfig.setTransferOrderApprovalExpiry(24);
      } else {
        orderConfig.setTransferOrderApprovalExpiry(model.tx);
      }
      return orderConfig;
    }
    return null;
  }

  public GeneralConfigModel buildDomainLocationModels(Long domainId, Locale locale, String timezone) {
    DomainConfig dc = DomainConfig.getInstance(domainId);
    GeneralConfigModel model = new GeneralConfigModel();

    List<String> val = dc.getDomainData(ConfigConstants.GENERAL);
    if (val != null) {
      model.createdBy = val.get(0);
      model.lastUpdated =
          LocalDateUtil.format(new Date(Long.parseLong(val.get(1))), locale, timezone);
      model.fn = getFullName(model.createdBy);
    }
    model.cnt = dc.getCountry() != null ? dc.getCountry() : "";
    model.st = dc.getState() != null ? dc.getState() : "";
    model.ds = dc.getDistrict() != null ? dc.getDistrict() : "";
    model.lng = StringUtils.isNotEmpty(dc.getLanguage()) ? dc.getLanguage() : "en";
    model.tz = dc.getTimezone() != null ? dc.getTimezone() : "";
    model.cur = dc.getCurrency() != null ? dc.getCurrency() : "";
    model.pgh = dc.getPageHeader() != null ? dc.getPageHeader() : "";
    model.sc = !dc.getUiPreference();
    model.domainId = domainId;
    model.snh = dc.isEnableSwitchToNewHost();
    model.nhn = dc.getNewHostName();
    model.support = buildAllSupportConfigModels(dc);
    model.adminContact = buildAllAdminContactConfigModel(dc.getAdminContactConfig());
    return model;
  }

  public GeneralConfigModel buildDomainLocationModel(Long domainId) {
    DomainConfig dc = DomainConfig.getInstance(domainId);
    GeneralConfigModel model = new GeneralConfigModel();
    model.cnt = dc.getCountry() != null ? dc.getCountry() : "";
    model.st = dc.getState() != null ? dc.getState() : "";
    model.ds = dc.getDistrict() != null ? dc.getDistrict() : "";
    model.domainId = domainId;
    return model;
  }

  public Map<String, AdminContactConfigModel> buildAllAdminContactConfigModel(
      AdminContactConfig config) {
    if(config == null) {
      return null;
    }
    Map<String, AdminContactConfigModel> model = new HashMap<>(2);
    model.put(AdminContactConfig.PRIMARY_ADMIN_CONTACT,
        buildAdminContactModel(config.getPrimaryAdminContact()));
    model.put(AdminContactConfig.SECONDARY_ADMIN_CONTACT,
        buildAdminContactModel(config.getSecondaryAdminContact()));
    return model;
  }

  public AdminContactConfigModel buildAdminContactModel(String userId) {
    AdminContactConfigModel model = new AdminContactConfigModel();
    if (StringUtils.isNotEmpty(userId)) {
      IUserAccount userAccount = usersService.getUserAccount(userId);
      if (userAccount != null) {
        model.userId = userId;
        model.email = userAccount.getEmail();
        model.phn = userAccount.getMobilePhoneNumber();
        model.userNm = userAccount.getFullName();
        model.setPhoto(constructMediaModel(model.userId));
      }
    }
    return model;
  }

  private List<MediaModel> constructMediaModel(String key) {
    IMediaEndPoint endPoint = JDOUtils.createInstance(IMediaEndPoint.class);
    MediaBuilder builder = new MediaBuilder();
    List<IMedia> mediaList = endPoint.getMedias(key);
    return builder.constructMediaModelList(mediaList);
  }

  public List<SupportConfigModel> buildAllSupportConfigModels(DomainConfig dc) {
    List<SupportConfigModel> support = new ArrayList<>(3);
    SupportConfigModel scModel = new SupportConfigModel();
    scModel.role = SecurityConstants.ROLE_KIOSKOWNER;
    scModel = buildSupportConfigModel(scModel.role, dc);
    support.add(scModel);
    scModel = new SupportConfigModel();
    scModel.role = SecurityConstants.ROLE_SERVICEMANAGER;
    scModel = buildSupportConfigModel(scModel.role, dc);
    support.add(scModel);
    scModel = new SupportConfigModel();
    scModel.role = SecurityConstants.ROLE_DOMAINOWNER;
    scModel = buildSupportConfigModel(scModel.role, dc);
    support.add(scModel);
    return support;
  }


  public SupportConfigModel buildSupportConfigModel(String role, DomainConfig dc) {
    SupportConfig config = null;
    if (StringUtils.isNotEmpty(role) && dc != null) {
      config = dc.getSupportConfigByRole(role);
    }
    SupportConfigModel model = new SupportConfigModel();
    if (config != null) {
      model.usrid = config.getSupportUser();
      if (StringUtils.isNotEmpty(model.usrid)) {
        try {
          IUserAccount userAccount = usersService.getUserAccount(model.usrid);
          if (userAccount != null) {
            model.phnm = userAccount.getMobilePhoneNumber();
            model.em = userAccount.getEmail();
            model.userpopulate = true;
            model.usrname = userAccount.getFullName();
          }
        } catch (ObjectNotFoundException e) {
          xLogger
              .warn("Configured support user {0} for role {1} no longer exists",
                  model.usrid, role,
                  e);
          model.usrid = null;
        } catch (SystemException e) {
          xLogger.severe("Error in fetching user details", e);
          throw new InvalidServiceException("Unable to fetch user details for " + model.usrid);
        }
      } else {
        model.phnm = config.getSupportPhone();
        model.em = config.getSupportEmail();
        model.usrname = config.getSupportUserName();
      }
    }
    model.role = role;
    return model;
  }

  public AccountingConfigModel buildAccountingConfigModel(Long domainId, Locale locale,
                                                          String timezone) {
    DomainConfig dc = DomainConfig.getInstance(domainId);
    AccountingConfig ac = dc.getAccountingConfig();
    AccountingConfigModel model = new AccountingConfigModel();
    List<String> val = dc.getDomainData(ConfigConstants.ACCOUNTING);
    if (val != null) {
      model.createdBy = val.get(0);
      model.lastUpdated =
          LocalDateUtil.format(new Date(Long.parseLong(val.get(1))), locale, timezone);
      model.fn = getFullName(model.createdBy);
    }
    if (ac != null) {
      model.ea = ac.isAccountingEnabled();
      model.cl = ac.getCreditLimit();
      if (ac.enforceConfirm()) {
        model.en = IOrder.CONFIRMED;
      } else if (ac.enforceShipped()) {
        model.en = IOrder.COMPLETED;
      }
    }
    return model;
  }

  public String getFullName(String userId) {
    try {
      return usersService.getUserAccount(userId).getFullName();
    } catch (Exception e) {
      //ignore.. get for display only.
    }
    return null;
  }

  public TagsConfigModel buildTagsConfigModel(DomainConfig dc, Locale locale, String timezone) {
    TagsConfigModel model = new TagsConfigModel();
    List<String> val = dc.getDomainData(ConfigConstants.TAGS);
    if (val != null) {
      model.createdBy = val.get(0);
      model.lastUpdated =
          LocalDateUtil.format(new Date(Long.parseLong(val.get(1))), locale, timezone);
      model.fn = getFullName(model.createdBy);
    }

    model.mt = TagUtil.getTagsArray(dc.getMaterialTags());
    model.et = TagUtil.getTagsArray(dc.getKioskTags());
    model.rt = TagUtil.getTagsArray(dc.getRouteTags());
    model.ot = TagUtil.getTagsArray(dc.getOrderTags());
    model.ut = TagUtil.getTagsArray(dc.getUserTags());
    model.emt = dc.forceTagsMaterial();
    model.eet = dc.forceTagsKiosk();
    model.eot = dc.forceTagsOrder();
    model.en = dc.getRouteBy();
    model.eut = dc.forceTagsUser();
    if (dc.getEntityTagOrder() != null && !dc.getEntityTagOrder().isEmpty()) {
      for (Map.Entry<String, Integer> entry : dc.getEntityTagOrder().entrySet()) {
        TagsConfigModel.ETagOrder eto = new TagsConfigModel.ETagOrder();
        eto.etg = entry.getKey();
        eto.rnk = entry.getValue();
        model.etr.add(eto);
      }
    }
    return model;
  }

  public AssetSystemConfigModel buildAssetSystemConfigModel(AssetSystemConfig asc,
                                                            AssetConfigModel assetConfigModel) {

    AssetSystemConfigModel model = new AssetSystemConfigModel();

    // build working statuses
    model.workingStatus = buildWorkingStatuses(asc);

    // build asset types
    model.assetTypes = buildAssetTypes(asc, assetConfigModel);

    return model;
  }

  public List<WorkingStatus> buildWorkingStatuses(AssetSystemConfig asc) {
    List<WorkingStatus> workingStatusList = new ArrayList<>(asc.workingStatuses.size());
    for (AssetSystemConfig.WorkingStatus workingStatus : asc.workingStatuses) {
      WorkingStatus ws = new WorkingStatus();
      ws.status = workingStatus.status;
      ws.displayName = workingStatus.displayValue;
      workingStatusList.add(ws);
    }
    return workingStatusList;
  }

  public List<AssetType> buildAssetTypes(AssetSystemConfig asc,
                                                                AssetConfigModel assetConfigModel) {

    List<AssetType> assetTypeList = new ArrayList<>(asc.assets.size());
    Map<Integer, AssetConfigModel.Asset> assetMap = assetConfigModel.assets;

    for (Integer key : asc.assets.keySet()) {
      AssetSystemConfig.Asset asset = asc.getAsset(key);
      AssetType assetType = new AssetType();
      assetType.id = key;
      assetType.manufacturers = buildManufacturers(asset.getManufacturers(), assetMap.get(key).mcs);
      assetType.monitoringType = asset.type;
      assetType.gsmEnabled = asset.isGSMEnabled();
      assetType.name = asset.getName();
      assetType.temperatureSensitive = asset.isTemperatureEnabled();
      if (asset.monitoringPositions != null) {
        assetType.monitoringPoints = buildAssetTypeMonitoringPoints(asset,assetMap.get(key));
      }
      assetTypeList.add(assetType);
    }
    return assetTypeList;
  }

  public List<MonitoringPoint> buildAssetTypeMonitoringPoints(
      AssetSystemConfig.Asset asset, AssetConfigModel.Asset ast) {
    List<MonitoringPoint> monitoringPointList = new ArrayList<>(asset.monitoringPositions.size());
    for (AssetSystemConfig.MonitoringPosition position : asset.monitoringPositions) {
      MonitoringPoint
          monitoringPoint =
          new MonitoringPoint();
      monitoringPoint.point = position.mpId;
      monitoringPoint.position = position.name;
      monitoringPoint.sensor = position.sId;
      if (null != ast.dMp && (ast.dMp.intValue() == monitoringPoint.point.intValue())) {
        monitoringPoint.defaultPoint = true;
      }
      monitoringPointList.add(monitoringPoint);
    }
    return monitoringPointList;
  }

  public List<Manufacturer> buildManufacturers(
      Map<String, AssetSystemConfig.Manufacturer> systemManufacturerMap,
      Map<String, AssetConfigModel.Mancfacturer> domainManufacturerMap) {
    List<Manufacturer> manufacturerList = new ArrayList<>(1);
    if (systemManufacturerMap != null) {
      for (Map.Entry<String, AssetSystemConfig.Manufacturer> entry : systemManufacturerMap
          .entrySet()) {
        Manufacturer model = new Manufacturer();
        boolean match = false;
        AssetConfigModel.Mancfacturer domainManufacturer = null;
        for (Map.Entry<String, AssetConfigModel.Mancfacturer> man : domainManufacturerMap
            .entrySet()) {
          if (Objects.equals(entry.getKey(), man.getKey())
              && domainManufacturerMap.get(man.getKey()).iC != null && domainManufacturerMap
              .get(man.getKey()).iC) {
            domainManufacturer = domainManufacturerMap.get(man.getKey());
            match = true;
            break;
          }
        }
        if (match) {
          AssetSystemConfig.Manufacturer manufacturer = systemManufacturerMap.get(entry.getKey());
          model.id = entry.getKey();
          model.name = manufacturer.name;
          model.serialNumberFormatDescription = manufacturer.serialFormatDescription;
          model.serialNumberValidationRegex = manufacturer.serialFormat;
          model.modelNumberFormatDescription = manufacturer.modelFormatDescription;
          model.modelNumberValidationRegex = manufacturer.modelFormat;
          if (manufacturer.model != null && !manufacturer.model.isEmpty()) {
            model.models = buildManufacturerModels(manufacturer.model, domainManufacturer.model);
          }
          manufacturerList.add(model);
        }

      }
    }
    return manufacturerList;
  }


  public List<Model> buildManufacturerModels(
      List<AssetSystemConfig.Model> systemConfigModels,
      Map<String, AssetConfigModel.Model> domainConfigModels) {
    List<Model> modelList = new ArrayList<>(1);
    if (domainConfigModels != null && !domainConfigModels.isEmpty()) {
      for (Map.Entry<String, AssetConfigModel.Model> acm : domainConfigModels.entrySet()) {
        AssetConfigModel.Model assetConfigModel = domainConfigModels.get(acm.getKey());
        systemConfigModels.forEach(scm -> {
          if (Objects.equals(scm.name, acm.getKey()) && assetConfigModel.iC != null
              && assetConfigModel.iC) {
            Model model = new Model();
            model.name = assetConfigModel.name;
            if (assetConfigModel.sns != null && !assetConfigModel.sns.isEmpty()) {
              model.sensors = buildModelSensors(assetConfigModel.sns);
            }
            modelList.add(model);
          }
        });
      }
    }
    return modelList;
  }

  public List<Sensor> buildModelSensors(
      Map<String, AssetConfigModel.Sensor> sensorMap) {
    List<Sensor> sensorList = new ArrayList<>(sensorMap.size());
    for (Map.Entry<String, AssetConfigModel.Sensor> sns : sensorMap.entrySet()) {
      Sensor sensor = new Sensor();
      AssetConfigModel.Sensor acs = sensorMap.get(sns.getKey());
      sensor.name = acs.name;
      sensor.color = acs.cd;
      sensor.monitoringPosition = acs.mpId;
      sensorList.add(sensor);
    }

    return sensorList;
  }

  public AssetConfigModel buildAssetConfigModel(DomainConfig dc, Locale locale, String timezone)
      throws ConfigurationException {
    AssetSystemConfig asc = AssetSystemConfig.getInstance();
    if (asc == null) {
      throw new ConfigurationException();
    }
    Map<Integer, AssetSystemConfig.Asset> assets = asc.assets;
    if (assets == null) {
      throw new ConfigurationException();
    }
    AssetConfig tc = dc.getAssetConfig();
    List<String> vendorIdsList = null, assetModelList = null, defaultSnsList = null,
        defaultMpsList =
            null;
    int enableTemp = 0;
    AssetConfig.Configuration configuration = null;
    AssetConfigModel acm = new AssetConfigModel();

    if (tc != null) {
      vendorIdsList = tc.getVendorIds();
      enableTemp = tc.getEnable();
      configuration = tc.getConfiguration();
      acm.namespace = tc.getNamespace();
      assetModelList = tc.getAssetModels();
      defaultSnsList = tc.getDefaultSns();
      defaultMpsList = tc.getDefaultMps();
    }
    for (Map.Entry<Integer, AssetSystemConfig.Asset> entry: assets.entrySet()) {
      Integer key = entry.getKey();
      AssetSystemConfig.Asset asset = entry.getValue();
      if (asset.getManufacturers() == null) {
        throw new ConfigurationException();
      }

      AssetConfigModel.Asset acmAsset = new AssetConfigModel.Asset();
      acmAsset.an = asset.getName();
      acmAsset.id = key;
      acmAsset.at = asset.type;
      acmAsset.iGe = asset.isGSMEnabled();
      acmAsset.iTs = asset.isTemperatureEnabled();
      if (asset.monitoringPositions != null) {
        for (AssetSystemConfig.MonitoringPosition monitoringPosition : asset.monitoringPositions) {
          AssetConfigModel.MonitoringPosition mp = new AssetConfigModel.MonitoringPosition();
          mp.mpId = monitoringPosition.mpId;
          mp.name = monitoringPosition.name;
          mp.sId = monitoringPosition.sId;
          acmAsset.mps.put(mp.mpId, mp);

          if (defaultMpsList != null && defaultMpsList
              .contains(key + Constants.KEY_SEPARATOR + monitoringPosition.mpId)) {
            acmAsset.dMp = monitoringPosition.mpId;
          }
        }
      }
      for (String manuKey : asset.getManufacturers().keySet()) {
        AssetSystemConfig.Manufacturer manufacturer = asset.getManufacturers().get(manuKey);
        AssetConfigModel.Mancfacturer acmManc = new AssetConfigModel.Mancfacturer();
        if (vendorIdsList != null && key.equals(IAsset.TEMP_DEVICE) && vendorIdsList.contains(manuKey)
              || vendorIdsList.contains(key + Constants.KEY_SEPARATOR + manuKey)) {
            acmManc.iC = true;

        }
        acmManc.id = manuKey;
        acmManc.name = manufacturer.name;
        if(asset.type == IAsset.MONITORED_ASSET) {
          acmManc.serialFormat = manufacturer.serialFormat;
          acmManc.modelFormat = manufacturer.modelFormat;
          acmManc.serialFormatDescription = manufacturer.serialFormatDescription;
          acmManc.modelFormatDescription = manufacturer.modelFormatDescription;
        }
        if (manufacturer.model != null) {
          for (AssetSystemConfig.Model model : manufacturer.model) {
            AssetConfigModel.Model configModel = new AssetConfigModel.Model();
            configModel.name = model.name;
            configModel.type = model.type;
            configModel.capacity = model.capacityInLitres;

            if (acmManc.iC != null && acmManc.iC && assetModelList != null && assetModelList
                .contains(
                    key + Constants.KEY_SEPARATOR + manuKey + Constants.KEY_SEPARATOR
                        + model.name)) {
              configModel.iC = true;
            }
            if (model.sns != null) {
              for (AssetSystemConfig.Sensor sensor : model.sns) {
                AssetConfigModel.Sensor configSensor = new AssetConfigModel.Sensor();
                configSensor.name = sensor.name;
                configSensor.mpId = sensor.mpId;
                configSensor.cd = sensor.cd;
                configModel.sns.put(configSensor.name, configSensor);
                if (defaultSnsList != null && defaultSnsList.contains(
                    key + Constants.KEY_SEPARATOR + model.name + Constants.KEY_SEPARATOR
                        + sensor.name)) {
                  configModel.dS = sensor.name;
                }
              }
            }
            if (model.feature != null) {
              AssetConfigModel.Feature feature = new AssetConfigModel.Feature();
              feature.pc = model.feature.pullConfig;
              feature.ds = model.feature.dailyStats;
              feature.ps = model.feature.powerStats;
              feature.dl = model.feature.displayLabel;
              feature.ct = model.feature.currentTemperature;
              configModel.fts = feature;
            }
            acmManc.model.put(configModel.name, configModel);
          }
        }
        acmAsset.mcs.put(acmManc.id, acmManc);
      }
      acm.assets.put(acmAsset.id, acmAsset);
    }
    acm.enable = enableTemp;
    acm.config = configuration;
    if (asc.workingStatuses != null) {
      AssetConfigModel.WorkingStatus workingStatus;
      for (AssetSystemConfig.WorkingStatus ws : asc.workingStatuses) {
        workingStatus = new AssetConfigModel.WorkingStatus();
        workingStatus.status = ws.status;
        workingStatus.dV = ws.displayValue;
        workingStatus.color = ws.color;
        acm.wses.put(workingStatus.status, workingStatus);
      }
    }
    List<String> val = dc.getDomainData(ConfigConstants.TEMPERATURE);
    if (val != null) {
      acm.createdBy = val.get(0);
      acm.lastUpdated =
          LocalDateUtil.format(new Date(Long.parseLong(val.get(1))), locale, timezone);
      acm.fn = getFullName(acm.createdBy);
    }

    return acm;
  }

  public CapabilitiesConfigModel buildCapabilitiesConfigModel(Locale locale,
                                                              DomainConfig dc, String timezone) {
    Map<String, CapabilityConfig> map = dc.getCapabilityMapByRole();
    CapabilitiesConfigModel model = new CapabilitiesConfigModel();

    if (dc.getCapabilities() != null) {
      model.cap = dc.getCapabilities().toArray(new String[dc.getCapabilities().size()]);
    }
    if (dc.getTransactionMenus() != null) {
      model.tm = dc.getTransactionMenus().toArray(new String[dc.getTransactionMenus().size()]);
    }
    if (dc.getCreatableEntityTypes() != null) {
      model.et =
          dc.getCreatableEntityTypes().toArray(
              new String[dc.getCreatableEntityTypes().size()]);
    }
    List<String> val = dc.getDomainData(ConfigConstants.CAPABILITIES);
    if (val != null) {
      model.createdBy = val.get(0);
      model.lastUpdated =
          LocalDateUtil.format(new Date(Long.parseLong(val.get(1))), locale, timezone);
      model.fn = getFullName(model.createdBy);
    }
    model.er = dc.allowRouteTagEditing();
    model.lr = dc.isLoginAsReconnect();
    model.sv = dc.sendVendors();
    model.sc = dc.sendCustomers();
    model.gcs = dc.getGeoCodingStrategy();
    model.atexp = dc.getAuthenticationTokenExpiry();
    model.llr = dc.isLocalLoginRequired();

    Map<String, String> tagInvByOperation = dc.gettagsInvByOperation();
    model.hii = getTagsByInvOperationAsList(tagInvByOperation, ITransaction.TYPE_ISSUE);
    model.hir = getTagsByInvOperationAsList(tagInvByOperation, ITransaction.TYPE_RECEIPT);
    model.hip = getTagsByInvOperationAsList(tagInvByOperation, ITransaction.TYPE_PHYSICALCOUNT);
    model.hiw = getTagsByInvOperationAsList(tagInvByOperation, ITransaction.TYPE_WASTAGE);
    model.hit = getTagsByInvOperationAsList(tagInvByOperation, ITransaction.TYPE_TRANSFER);
    model.hiri =
        getTagsByInvOperationAsList(tagInvByOperation, ITransaction.TYPE_RETURNS_INCOMING);
    model.hiro =
        getTagsByInvOperationAsList(tagInvByOperation, ITransaction.TYPE_RETURNS_OUTGOING);


    if (dc.getTagsInventory() != null) {
      model.hi = new ArrayList<>(Arrays.asList(dc.getTagsInventory().split(",")));
    }
    if (dc.getTagsOrders() != null) {
      model.ho = new ArrayList<>(Arrays.asList(dc.getTagsOrders().split(",")));
    }
    model.dshp = dc.isDisableShippingOnMobile();
    model.bcs = dc.isBarcodingEnabled();
    model.rfids = dc.isRFIDEnabled();
    model.ro = "";
    Map<String, CapabilitiesConfigModel> roleMap = new HashMap<>(4);
    roleMap.put(SecurityConstants.ROLE_KIOSKOWNER,
        constructCCM(map.get(SecurityConstants.ROLE_KIOSKOWNER), SecurityConstants.ROLE_KIOSKOWNER, dc));
    roleMap.put(SecurityConstants.ROLE_SERVICEMANAGER,
        constructCCM(map.get(SecurityConstants.ROLE_SERVICEMANAGER), SecurityConstants.ROLE_SERVICEMANAGER,
            dc));
    roleMap.put(SecurityConstants.ROLE_DOMAINOWNER,
        constructCCM(map.get(SecurityConstants.ROLE_DOMAINOWNER), SecurityConstants.ROLE_DOMAINOWNER, dc));
    roleMap.put(SecurityConstants.ROLE_SUPERUSER,
        constructCCM(map.get(SecurityConstants.ROLE_SUPERUSER), SecurityConstants.ROLE_SUPERUSER, dc));
    model.roleConfig = roleMap;

    if (dc.getSyncConfig() != null) {
      SyncConfig sc = dc.getSyncConfig();
      model.mdri = sc.getMasterDataRefreshInterval() / SyncConfig.HOURS_IN_A_DAY;
      model.aplui = sc.getAppLogUploadInterval() / SyncConfig.HOURS_IN_A_DAY;
      model.stwd = sc.getSmsTransmissionWaitDuration() / SyncConfig.HOURS_IN_A_DAY;
    }
    model.setTheme(dc.getStoreAppTheme());
    model.setTwoFactorAuthenticationEnabled(dc.isTwoFactorAuthenticationEnabled());
    return model;
  }

  public CapabilitiesConfigModel buildRoleCapabilitiesConfigModel(String sRole,
                                                                  Locale locale, DomainConfig dc,
                                                                  String timezone) {
    Map<String, CapabilityConfig> map = dc.getCapabilityMapByRole();
    CapabilitiesConfigModel model;
    CapabilityConfig roleCapabConfig = map.get(sRole);
    if (roleCapabConfig != null) {
      model = constructCCM(roleCapabConfig, sRole, dc);
    } else {
      model = buildCapabilitiesConfigModel(locale, dc, timezone);
    }
    return model;
  }

  private CapabilitiesConfigModel constructCCM(CapabilityConfig config, String role,
                                               DomainConfig dc) {
    CapabilitiesConfigModel model = new CapabilitiesConfigModel();
    model.ro = role;
    if (config != null) {
      if (config.getCapabilities() != null) {
        model.tm = config.getCapabilities().toArray(new String[config.getCapabilities().size()]);
      }
      if (config.getCreatableEntityTypes() != null) {
        model.et =
            config.getCreatableEntityTypes()
                .toArray(new String[config.getCreatableEntityTypes().size()]);
      }
      model.er = config.allowRouteTagEditing();
      model.lr = config.isLoginAsReconnect();
      model.sv = config.sendVendors();
      model.sc = config.sendCustomers();
      model.gcs = config.getGeoCodingStrategy();
      if (config.getTagsInventory() != null) {
        model.hi = new ArrayList<>(Arrays.asList(config.getTagsInventory().split(CharacterConstants.COMMA)));
      }
      if (config.getTagsOrders() != null) {
        model.ho = new ArrayList<>(Arrays.asList(config.getTagsOrders().split(CharacterConstants.COMMA)));
      }
      Map<String,String> tagInvByOperation = config.gettagsInvByOperation();
      model.hii =
          getTagsByInvOperationAsList(tagInvByOperation, ITransaction.TYPE_ISSUE);
      model.hir =
          getTagsByInvOperationAsList(tagInvByOperation, ITransaction.TYPE_RECEIPT);
      model.hip =
          getTagsByInvOperationAsList(tagInvByOperation, ITransaction.TYPE_PHYSICALCOUNT);
      model.hiw =
          getTagsByInvOperationAsList(tagInvByOperation, ITransaction.TYPE_WASTAGE);
      model.hit =
          getTagsByInvOperationAsList(tagInvByOperation, ITransaction.TYPE_TRANSFER);
      model.hiri =
          getTagsByInvOperationAsList(tagInvByOperation, ITransaction.TYPE_RETURNS_INCOMING);
      model.hiro =
          getTagsByInvOperationAsList(tagInvByOperation, ITransaction.TYPE_RETURNS_OUTGOING);

      model.dshp = config.isDisableShippingOnMobile();
      model.bcs = config.isBarcodingEnabled();
      model.rfids = config.isRFIDEnabled();
    }
    if (dc != null) {
      model.atexp = dc.getAuthenticationTokenExpiry();
      if (dc.getSyncConfig() != null) {
        model.mdri = dc.getSyncConfig().getMasterDataRefreshInterval() / SyncConfig.HOURS_IN_A_DAY;
        model.aplui = dc.getSyncConfig().getAppLogUploadInterval() / SyncConfig.HOURS_IN_A_DAY;
        model.stwd =
            dc.getSyncConfig().getSmsTransmissionWaitDuration() / SyncConfig.HOURS_IN_A_DAY;
      }
      model.setTheme(dc.getStoreAppTheme());
      model.setTwoFactorAuthenticationEnabled(dc.isTwoFactorAuthenticationEnabled());
    }
    return model;
  }

  public InventoryConfigModel buildInventoryConfigModel(DomainConfig dc, Locale locale,
                                                        String timezone)
      throws ConfigurationException {
    InventoryConfigModel model = new InventoryConfigModel();
    InventoryConfig ic = dc.getInventoryConfig();
    if (ic == null) {
      throw new ConfigurationException("");
    }
    //Get the transaction reasons
    Map<String, ReasonConfig> transReasons = ic.getTransReasons();
    if (MapUtils.isNotEmpty(transReasons)) {
      model.ri = buildReasonConfigModel(transReasons.get(ITransaction.TYPE_ISSUE));
      model.rr = buildReasonConfigModel(transReasons.get(ITransaction.TYPE_RECEIPT));
      model.rs = buildReasonConfigModel(transReasons.get(ITransaction.TYPE_PHYSICALCOUNT));
      model.rd = buildReasonConfigModel(transReasons.get(ITransaction.TYPE_WASTAGE));
      model.rt = buildReasonConfigModel(transReasons.get(ITransaction.TYPE_TRANSFER));
      model.rri = buildReasonConfigModel(transReasons.get(ITransaction.TYPE_RETURNS_INCOMING));
      model.rro = buildReasonConfigModel(transReasons.get(ITransaction.TYPE_RETURNS_OUTGOING));
    }
    model.setTransactionTypesWithReasonMandatory(ic.getTransactionTypesWithReasonMandatory());

    List<String> val = dc.getDomainData(ConfigConstants.INVENTORY);
    if (val != null) {
      model.createdBy = val.get(0);
      model.lastUpdated =
          LocalDateUtil.format(new Date(Long.parseLong(val.get(1))), locale, timezone);
      model.fn = getFullName(model.createdBy);
    }
    if (ic.isCimt()) {
      model.cimt = true;
      model.imt = buildMTagReasonModelList(ic.getImTransReasons());
    }
    if (ic.isCrmt()) {
      model.crmt = true;
      model.rmt = buildMTagReasonModelList(ic.getRmTransReasons());
    }
    if (ic.isCsmt()) {
      model.csmt = true;
      model.smt = buildMTagReasonModelList(ic.getSmTransReasons());
    }
    if (ic.isCtmt()) {
      model.ctmt = true;
      model.tmt = buildMTagReasonModelList(ic.getTmTransReasons());
    }
    if (ic.isCdmt()) {
      model.cdmt = true;
      model.dmt = buildMTagReasonModelList(ic.getDmTransReasons());
    }
    if (ic.isCrimt()) {
      model.crimt = true;
      model.rimt = buildMTagReasonModelList(ic.getMtagRetIncRsns());
    }
    if (ic.isCromt()) {
      model.cromt = true;
      model.romt = buildMTagReasonModelList(ic.getMtagRetOutRsns());
    }
    //Get transaction export schedules
    model.etdx = ic.isEnabled();
    if (model.etdx) {
      List<String> times = ic.getTimes();
      List<String>
          dsTimes =
          LocalDateUtil.convertTimeStringList(times, dc.getTimezone(),
              false); // Convert times from UTC to domain specific timezone
      if (dsTimes != null && !dsTimes.isEmpty()) {
        model.et = StringUtil.getCSV(dsTimes);
      }
      model.an = StringUtil.getCSV(ic.getExportUsers());
    }
    InventoryConfig.ManualTransConfig manualTransConfig = ic.getManualTransConfig();
    if (manualTransConfig != null) {
      model.emuidt = manualTransConfig.enableManualUploadInvDataAndTrans;
      model.euse = manualTransConfig.enableUploadPerEntityOnly;
    }
    model.eidb = ic.showInventoryDashboard();
    if (ic.getEnTags() != null && !ic.getEnTags().isEmpty()) {
      model.enTgs = ic.getEnTags();
    }
    if (ic.getUserTags() != null && !ic.getUserTags().isEmpty()) {
      model.usrTgs = ic.getUserTags();
    }

    //Get Permissions
    InventoryConfig.Permissions perms = ic.getPermissions();
    if (perms != null) {
      model.ivc = perms.invCustomersVisible;
    }
    //Get Optimization Config
    OptimizerConfig oc = dc.getOptimizerConfig();
    int compute = oc.getCompute();
    model.co = String.valueOf(compute);
    model.crfreq = oc.getComputeFrequency();
    model.edis = oc.isExcludeDiscards();
    model.ersns = StringUtil.getList(oc.getExcludeReasons());
    model.erirsns = StringUtil.getList(oc.getExcludeReturnIncomingReasons());
    model.aopfd = String.valueOf(oc.getMinAvgOrderPeriodicity());
    model.nopfd = String.valueOf(oc.getNumPeriods());

    model.im = oc.getInventoryModel();
    model.aopeoq = String.valueOf(oc.getMinAvgOrderPeriodicity());
    model.nopeoq = String.valueOf(oc.getNumPeriods());
    model.lt = String.valueOf(oc.getLeadTimeDefault());

    MatStatusConfig msi = ic.getMatStatusConfigByType(ITransaction.TYPE_ISSUE);
    if (msi != null) {
      model.idf = msi.getDf();
      model.iestm = msi.getEtsm();
      model.ism = msi.isStatusMandatory();
    }
    MatStatusConfig msr = ic.getMatStatusConfigByType(ITransaction.TYPE_RECEIPT);
    if (msr != null) {
      model.rdf = msr.getDf();
      model.restm = msr.getEtsm();
      model.rsm = msr.isStatusMandatory();
    }
    MatStatusConfig mst = ic.getMatStatusConfigByType(ITransaction.TYPE_TRANSFER);
    if (mst != null) {
      model.tdf = mst.getDf();
      model.testm = mst.getEtsm();
      model.tsm = mst.isStatusMandatory();
    }
    MatStatusConfig msp = ic.getMatStatusConfigByType(ITransaction.TYPE_PHYSICALCOUNT);
    if (msp != null) {
      model.pdf = msp.getDf();
      model.pestm = msp.getEtsm();
      model.psm = msp.isStatusMandatory();
    }
    MatStatusConfig msw = ic.getMatStatusConfigByType(ITransaction.TYPE_WASTAGE);
    if (msw != null) {
      model.wdf = msw.getDf();
      model.westm = msw.getEtsm();
      model.wsm = msw.isStatusMandatory();
    }
    MatStatusConfig msri = ic.getMatStatusConfigByType(ITransaction.TYPE_RETURNS_INCOMING);
    if (msri != null) {
      model.ridf = msri.getDf();
      model.riestm = msri.getEtsm();
      model.rism = msri.isStatusMandatory();
    }
    MatStatusConfig msro = ic.getMatStatusConfigByType(ITransaction.TYPE_RETURNS_OUTGOING);
    if (msro != null) {
      model.rodf = msro.getDf();
      model.roestm = msro.getEtsm();
      model.rosm = msro.isStatusMandatory();
    }

    model.catdi = getActualTransConfigType(ITransaction.TYPE_ISSUE, ic);
    model.catdr = getActualTransConfigType(ITransaction.TYPE_RECEIPT, ic);
    model.catdp = getActualTransConfigType(ITransaction.TYPE_PHYSICALCOUNT, ic);
    model.catdw = getActualTransConfigType(ITransaction.TYPE_WASTAGE, ic);
    model.catdt = getActualTransConfigType(ITransaction.TYPE_TRANSFER, ic);
    model.catdri = getActualTransConfigType(ITransaction.TYPE_RETURNS_INCOMING, ic);
    model.catdro = getActualTransConfigType(ITransaction.TYPE_RETURNS_OUTGOING, ic);

    model.crc = String.valueOf(ic.getConsumptionRate());
    if (ic.getConsumptionRate() == InventoryConfig.CR_MANUAL) {
      model.mcrfreq = ic.getManualCRFreq();
    }
    model.dispcr = ic.displayCR();
    if (model.dispcr) {
      model.dcrfreq = ic.getDisplayCRFreq();
    }
    model.minhpccr = String.valueOf(oc.getMinHistoricalPeriod());
    model.maxhpccr = String.valueOf(oc.getMaxHistoricalPeriod());
    model.showpr = ic.showPredictions();
    model.ddf = oc.isDisplayDF();
    model.dooq = oc.isDisplayOOQ();
    model.mmType = ic.getMinMaxType();
    model.mmFreq = ic.getMinMaxFreq();
    model.mmDur = ic.getMinMaxDur();
    // Lead time average configuration
    LeadTimeAvgConfig leadTimeAvgConfig = oc.getLeadTimeAvgCfg();
    if (leadTimeAvgConfig != null) {
      model.ltacm = new InventoryConfigModel.LeadTimeAvgConfigModel();
      model.ltacm.mino = leadTimeAvgConfig.getMinNumOfOrders();
      model.ltacm.maxo = leadTimeAvgConfig.getMaxNumOfOrders();
      model.ltacm.maxop = leadTimeAvgConfig.getMaxOrderPeriods();
      model.ltacm.exopt = leadTimeAvgConfig.getExcludeOrderProcTime();
    }
    model.rcm = buildReturnsConfigModels(ic.getReturnsConfig());
    return model;
  }

  public List<StockRebalancingConfigModel.EntityTagsCombination> populateEntityTagsCombinationModel(StockRebalancingConfig config) {
    List<StockRebalancingConfigModel.EntityTagsCombination> entityTagsCombinationList = new ArrayList<>();
    for(List<String> entityTags : config.getEntityTagsCombination()) {
      StockRebalancingConfigModel.EntityTagsCombination combination = new StockRebalancingConfigModel.EntityTagsCombination();
      List<String> tags = entityTags.stream().collect(Collectors.toList());
      combination.setEntityTags(tags);
      entityTagsCombinationList.add(combination);
    }

    return entityTagsCombinationList;
  }

  public StockRebalancingConfigModel buildStockRebalancingConfigModel(StockRebalancingConfig config,
                                                                      Long domainId, Locale locale,
                                                                      String timezone) {
    if(config == null) {
      return null;
    }

    StockRebalancingConfigModel model = new StockRebalancingConfigModel();
    DomainConfig dc = DomainConfig.getInstance(domainId);
    List<String> val = dc.getDomainData(ConfigConstants.STOCK_REBALANCING);
    if(val != null) {
      model.setCreatedBy(val.get(0));
      model.setLastUpdated(LocalDateUtil.format(new Date(Long.parseLong(val.get(1))), locale, timezone));
      model.setFirstName(getFullName(model.getCreatedBy()));
    }

    model.setEnableStockRebalancing(config.isEnableStockRebalancing());
    if(config.getMtTags() != null && !config.getMtTags().isEmpty()) {
      model.setMtTags(config.getMtTags());
    }
    if(config.getEntityTagsCombination() != null && !config.getEntityTagsCombination().isEmpty()) {
      model.setEntityTagsCombination(populateEntityTagsCombinationModel(config));
    }

    if(config.getGeoFencing() > 0) {
      model.setGeoFencing(config.getGeoFencing());
    }

    if(config.isStockOutDurationExceedsThreshold()) {
      model.setStockOutDurationExceedsThreshold(config.isStockOutDurationExceedsThreshold());
      model.setAcceptableLeadTime(config.getAcceptableLeadTime());
    }

    if(config.isExpiryCheck()) {
      model.setExpiryCheck(config.isExpiryCheck());
    }

    if(config.isMaxStock()) {
      model.setMaxStock(config.isMaxStock());
      model.setMaxStockDays(config.getMaxStockDays());
    }

    if(config.getTransportationCost() > 0) {
      model.setTransportationCost(config.getTransportationCost());
    }

    if(config.getHandlingCharges() > 0) {
      model.setHandlingCharges(config.getHandlingCharges());
    }

    if(config.getInventoryHoldingCost() > 0) {
      model.setInventoryHoldingCost(config.getInventoryHoldingCost());
    }

    return model;

  }

  public ApprovalsConfigModel buildApprovalsConfigModel(ApprovalsConfig config,
                                                        Long domainId, Locale locale,
                                                        String timezone){
    if(config == null) {
      return null;
    }
    ApprovalsConfigModel model = new ApprovalsConfigModel();
    DomainConfig dc = DomainConfig.getInstance(domainId);
    List<String> val = dc.getDomainData(ConfigConstants.APPROVALS);
    if(val != null) {
      model.createdBy = val.get(0);
      model.lastUpdated = LocalDateUtil.format(new Date(Long.parseLong(val.get(1))), locale, timezone);
      model.fn = getFullName(model.createdBy);
    }
    ApprovalsConfig.OrderConfig orderConfig = config.getOrderConfig();
    if(orderConfig != null) {
      if(CollectionUtils.isNotEmpty(orderConfig.getPrimaryApprovers())) {
        model.pa =
            userBuilder.buildUserModels(constructUserAccount(orderConfig.getPrimaryApprovers()), locale,
                timezone, true);
      }
      if(CollectionUtils.isNotEmpty(orderConfig.getSecondaryApprovers())) {
        model.sa =
            userBuilder.buildUserModels(constructUserAccount(orderConfig.getSecondaryApprovers()), locale,
                timezone, true);
      }
      model.px = orderConfig.getPurchaseOrderApprovalExpiry();
      model.sx = orderConfig.getSalesOrderApprovalExpiry();
      model.tx = orderConfig.getTransferOrderApprovalExpiry();
      if(CollectionUtils.isNotEmpty(orderConfig.getPurchaseSalesOrderApproval())) {
        List<ApprovalsConfig.PurchaseSalesOrderConfig> psocs = orderConfig.getPurchaseSalesOrderApproval();
        List<ApprovalsConfigModel.PurchaseSalesOrderApproval> psoas = new ArrayList<>();
        for(ApprovalsConfig.PurchaseSalesOrderConfig psos : psocs) {
          ApprovalsConfigModel.PurchaseSalesOrderApproval psoa = new ApprovalsConfigModel.PurchaseSalesOrderApproval();
          psoa.eTgs = psos.getEntityTags();
          psoa.poa = psos.isPurchaseOrderApproval();
          psoa.soa = psos.isSalesOrderApproval();
          psoas.add(psoa);
        }
        model.psoa = psoas;
      }
    }
    return model;
  }

  public ApprovalsEnabledConfigModel buildApprovalsEnabledConfigModel(Long domainId){
    DomainConfig dc = DomainConfig.getInstance(domainId);
    ApprovalsEnabledConfigModel model = new ApprovalsEnabledConfigModel();
    model.itae = dc.isTransferApprovalEnabled();
    model.ipae = dc.isPurchaseApprovalEnabled();
    model.isae = dc.isSalesApprovalEnabled();
    return model;
  }

  public FormsConfigModel buildFormsConfigModel(FormsConfig config) {
    FormsConfigModel formsConfigModel = new FormsConfigModel();
    if (config.getForms() != null) {
      formsConfigModel.setFormsList(config.getForms().toString());
    }
    return formsConfigModel;
  }

  public TransportersConfigModel buildTransportersConfigModel(TransportersConfig config,
                                                              Locale locale, String timezone)
      throws ServiceException {
    TransportersConfigModel transportersConfigModel = new TransportersConfigModel();
    if(config.getLastUpdated() != null) {
      transportersConfigModel.setLastUpdated(
          LocalDateUtil.format(new Date(Long.parseLong(config.getLastUpdated())),
              locale, timezone));
    }
    transportersConfigModel.setUpdatedBy(config.getUpdatedBy());
    transportersConfigModel.setUpdatedByName(getFullName(config.getUpdatedBy()));
    IConfig systemConfig = configurationMgmtService.getConfiguration(IConfig.TRANSPORTER_CONFIG);
    TransportersSystemConfig tConfig = new Gson().fromJson(systemConfig.getConfig(), TransportersSystemConfig
        .class);
    Map<String, TransportersConfig.TSPConfig> transporterConfigMap = new HashMap<>();
    if(CollectionUtils.isNotEmpty(config.getEnabledTransporters())) {
      config.getEnabledTransporters()
          .forEach(c -> transporterConfigMap.put(c.getTspId(), c));
    }
    if(CollectionUtils.isNotEmpty(tConfig.getTransporters())) {
      tConfig.getTransporters().forEach(model -> {
        TransportersConfigModel.TransporterConfigModel tConfigModel =
            new TransportersConfigModel.TransporterConfigModel();
        tConfigModel.setTspId(model.getId());
        if(CollectionUtils.isNotEmpty(model.getCategories())) {
          Type targetType = new TypeToken<List<ConsignmentCategoryModel>>() {}.getType();
          tConfigModel.setCategories(modelMapper.map(model.getCategories(), targetType));
        }
        tConfigModel.setName(model.getName());
        if (transporterConfigMap.containsKey(model.getId())) {
          TransportersConfig.TSPConfig TSPConfig = transporterConfigMap.get(
              model.getId());
          tConfigModel.setDefaultCategory(TSPConfig.getDefaultCategoryId());
          tConfigModel.setEnabled(true);
        } else {
          tConfigModel.setEnabled(false);
          tConfigModel.setDefaultCategory("");
        }
        transportersConfigModel.addTransporterConfigModel(tConfigModel);
      });
    }
    return transportersConfigModel;
  }

  public DashboardConfigModel buildDashboardConfigModel(DashboardConfig config, Long domainId,
                                                        Locale locale, String timezone) {
    DashboardConfigModel model = new DashboardConfigModel();
    if (config == null) {
      config = new DashboardConfig();
    }
    DomainConfig dc = DomainConfig.getInstance(domainId);
    List<String> val = dc.getDomainData(ConfigConstants.DASHBOARD);
    if (val != null) {
      model.createdBy = val.get(0);
      model.lastUpdated =
          LocalDateUtil.format(new Date(Long.parseLong(val.get(1))), locale, timezone);
      model.fn = getFullName(model.createdBy);
    }
    model.ape =
        config.getActivityPanelConfig() != null && config
            .getActivityPanelConfig().showActivityPanel;
    model.rpe =
        config.getRevenuePanelConfig() != null && config.getRevenuePanelConfig().showRevenuePanel;
    model.ope = config.getOrderPanelConfig() != null && config.getOrderPanelConfig().showOrderPanel;
    model.ipe =
        config.getInventoryPanelConfig() != null && config.getInventoryPanelConfig().showInvPanel;
    if (config.getDbOverConfig() != null) {
      model.dmtg = TagUtil.getTagsArray(config.getDbOverConfig().dmtg);
      model.dimtg = config.getDbOverConfig().dimtg;
      model.detg = config.getDbOverConfig().detg;
      model.aper =
          StringUtils.isBlank(config.getDbOverConfig().aper) ? "7" : config.getDbOverConfig().aper;
      model.dtt = config.getDbOverConfig().dtt;
      model.atdd = config.getDbOverConfig().atdd;
      model.edm = config.getDbOverConfig().edm;
      model.exet = TagUtil.getTagsArray(config.getDbOverConfig().exet);
      model.exts = TagUtil.getTagsArray(config.getDbOverConfig().exts);
      model.dutg = TagUtil.getTagsArray(config.getDbOverConfig().dutg);
    }
    if(StringUtils.isNotEmpty(config.getAssetsDbConfig().dmt)) {
      model.dmt = config.getAssetsDbConfig().dmt;
    }
    if(config.getAssetsDbConfig().dats != null) {
      model.dats = TagUtil.getTagsArray(config.getAssetsDbConfig().dats);
    }
    return model;
  }

  public OrdersConfigModel buildOrderConfigModel(HttpServletRequest request, Long domainId,
                                                 Locale locale, String timezone)
      throws ConfigurationException,
      UnsupportedEncodingException {
    DomainConfig dc = DomainConfig.getInstance(domainId);
    OrdersConfigModel model = new OrdersConfigModel();
    if (dc == null) {
      throw new ConfigurationException();
    }
    OrdersConfig oc = dc.getOrdersConfig();
    DemandBoardConfig dbc = dc.getDemandBoardConfig();

    List<String> val = dc.getDomainData(ConfigConstants.ORDERS);
    if (val != null) {
      model.createdBy = val.get(0);
      model.lastUpdated =
          LocalDateUtil.format(new Date(Long.parseLong(val.get(1))), locale, timezone);
      model.fn = getFullName(model.createdBy);
    }

    model.og = dc.getOrderGeneration();
    model.agi = dc.autoGI();
    model.tm = dc.isTransporterMandatory();
    model.tiss = dc.isTransporterInStatusSms();
    model.ao = dc.allowEmptyOrders();
    model.aof = false;
    model.dop = dc.isDisableOrdersPricing();
    if (dc.getPaymentOptions() != null) {
      model.po = dc.getPaymentOptions();
    }
    if (dc.getPackageSizes() != null) {
      model.ps = dc.getPackageSizes();
    }
    if (dc.getVendorId() != null) {
      model.vid = String.valueOf(dc.getVendorId());
    }
    if (oc != null) {
      model.eex = oc.isExportEnabled();
      if (model.eex) {
        List<String> times = StringUtil.getList(oc.getExportTimes());
        List<String>
            dsTimes =
            LocalDateUtil.convertTimeStringList(times, dc.getTimezone(),
                false); // Convert times from UTC to domain specific timezone
        if (dsTimes != null && !dsTimes.isEmpty()) {
          model.et = StringUtil.getCSV(dsTimes);
        }
        if (oc.getExportUserIds() != null) {
          model.an = oc.getExportUserIds();
        }
        if (oc.getUserTags() != null && !oc.getUserTags().isEmpty()) {
          model.usrTgs = oc.getUserTags();
        }
      }
      model.enOrdRsn = oc.isReasonsEnabled();
      model.orsn = oc.getOrderReason();
      model.md = oc.isMandatory();
      model.aoc = oc.allowSalesOrderAsConfirmed();
      model.asc = oc.allocateStockOnConfirmation();
      model.tr = oc.isTransferRelease();
      model.orr =
          StringUtils.isNotBlank(oc.getOrderRecommendationReasons()) ? oc
              .getOrderRecommendationReasons() : null;
      model.orrm = oc.getOrderRecommendationReasonsMandatory();
      model.eqr =
          StringUtils.isNotBlank(oc.getEditingQuantityReasons()) ? oc.getEditingQuantityReasons()
              : null;
      model.eqrm = oc.getEditingQuantityReasonsMandatory();
      model.psr =
          StringUtils.isNotBlank(oc.getPartialShipmentReasons()) ? oc.getPartialShipmentReasons()
              : null;
      model.psrm = oc.getPartialShipmentReasonsMandatory();
      model.pfr =
          StringUtils.isNotBlank(oc.getPartialFulfillmentReasons()) ? oc
              .getPartialFulfillmentReasons() : null;
      model.pfrm = oc.getPartialFulfillmentReasonsMandatory();
      model.cor =
          StringUtils.isNotBlank(oc.getCancellingOrderReasons()) ? oc.getCancellingOrderReasons()
              : null;
      model.corm = oc.getCancellingOrderReasonsMandatory();
      model.aafmsc = oc.autoAssignFirstMatStatus();
      model.autoCreate = oc.isCreationAutomated();
      model.autoCreateOnMin = oc.isAutoCreateOnMin();
      model.pdos = oc.getAutoCreatePdos();
      model.autoCreateEntityTags = oc.getAutoCreateEntityTags();
      model.autoCreateMaterialTags = oc.getAutoCreateMaterialTags();
      model.setLogo(oc.getInvoiceLogo());
      model.setLogoName(oc.getInvoiceLogoName() != null ? oc.getInvoiceLogoName() : LOGO);
      model.setLogoDownloadLink(buildDownloadLink(oc.getInvoiceLogo(), oc.getInvoiceLogoName(), LOGO));
      model.setInvoiceTemplate(oc.getInvoiceTemplate());
      model.setInvoiceTemplateName(oc.getInvoiceTemplateName() != null ? oc.getInvoiceTemplateName() : INVOICE_TEMPLATE);
      model.setInvoiceTemplateDownloadLink(buildDownloadLink(oc.getInvoiceTemplate(), oc.getInvoiceTemplateName(), INVOICE_TEMPLATE));
      model.setShipmentTemplate(oc.getShipmentTemplate());
      model.setShipmentTemplateName(oc.getShipmentTemplateName() != null ? oc.getShipmentTemplateName() : SHIPMENT_TEMPLATE);
      model.setShipmentTemplateDownloadLink(buildDownloadLink(oc.getShipmentTemplate(), oc.getShipmentTemplateName(), SHIPMENT_TEMPLATE));
      model.setReferenceIdMandatory(oc.isReferenceIdMandatory());
      model.setPurchaseReferenceIdMandatory(oc.isPurchaseReferenceIdMandatory());
      model.setTransferReferenceIdMandatory(oc.isTransferReferenceIdMandatory());
      model.setExpectedArrivalDateMandatory(oc.isExpectedArrivalDateMandatory());
      model.setMarkShippedOnPickup(oc.markOrderAsShippedOnPickup());
      model.setMarkFulfilledOnDelivery(oc.markOrderAsFulfilledOnDelivery());
      model.setDisableDeliveryRequestByCustomer(oc.isDeliveryRequestByCustomerDisabled());
      model.tspc = CollectionUtils.isNotEmpty(
          getTransportersAction.invoke(domainId, true, null, null).getResults());
    }
    if (dbc != null) {
      if (dbc.isPublic()) {
        model.ip = "p";
      } else {
        model.ip = "r";
      }
      if (dbc.getBanner() != null) {
        model.bn = dbc.getBanner();
      }
      if (dbc.getHeading() != null) {
        model.hd = dbc.getHeading();
      }
      if (dbc.getCopyright() != null) {
        model.cp = dbc.getCopyright();
      }
      model.spb = dbc.showStock();
      model.url = "https://" + request.getServerName() + "/pub/demand?id=" + domainId;
    }
    return model;
  }

  private String buildDownloadLink(String key, String fileName, String defaultFileName)
      throws UnsupportedEncodingException {
    String downloadLink;
    if (StringUtils.isNotEmpty(key)) {
      downloadLink = DOWNLOAD_LINK + "&fileName=" +
          URLEncoder.encode(fileName != null ? fileName : defaultFileName, "UTF-8") +
          "&key=" + URLEncoder.encode(key, "UTF-8");
    } else {
      StorageUtil storageUtil = AppFactory.get().getStorageUtil();
      downloadLink = storageUtil.getExternalUrl(UPLOADS,
          fileName != null ? fileName : defaultFileName);
    }

    return downloadLink;
  }

  /**
   * Builds collection of unique reasons configured in the given domains inventory configuration. Trims and removes
   * any empty reason codes.
   *
   * @param ic - Inventory config of the domain
   * @return unique collection of reason codes configured
   */
  public Collection<String> buildUniqueTransactionReasons(InventoryConfig ic) {
    Set<String> reasons = new HashSet<>(1);
    addAllReasons(reasons, ic.getTransReasons());
    if (ic.isCimt()) {
      addAllReasons(reasons, ic.getImTransReasons());
    }
    if (ic.isCrmt()) {
      addAllReasons(reasons, ic.getRmTransReasons());
    }
    if (ic.isCsmt()) {
      addAllReasons(reasons, ic.getSmTransReasons());
    }
    if (ic.isCtmt()) {
      addAllReasons(reasons, ic.getTmTransReasons());
    }
    if (ic.isCdmt()) {
      addAllReasons(reasons, ic.getDmTransReasons());
    }
    if (ic.isCrimt()) {
      addAllReasons(reasons, ic.getMtagRetIncRsns());
    }
    if (ic.isCromt()) {
      addAllReasons(reasons, ic.getMtagRetOutRsns());
    }
    reasons.remove(Constants.EMPTY);
    return reasons;
  }

  private void addAllReasons(Set<String> reasons, Map<String, ReasonConfig> configuredReasons) {
    if (MapUtils.isNotEmpty(configuredReasons)) {
      List<String> reasonsList = new ArrayList<>();
      configuredReasons.entrySet()
          .stream()
          .filter(entry->CollectionUtils.isNotEmpty(entry.getValue().getReasons()))
          .forEach(entry -> reasonsList.addAll(entry.getValue().getReasons()));
      reasons.addAll(reasonsList);
    }
  }

  public SupportConfigModel buildSCModelForWebDisplay() {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    Long domainId = sUser.getCurrentDomainId();
    String timezone = sUser.getTimezone();
    try {
      GeneralConfigModel generalConfigModel = buildDomainLocationModels(domainId, locale, timezone);
      List<SupportConfigModel> supportConfigModels = generalConfigModel.support;
      for (SupportConfigModel supportConfigModel : supportConfigModels) {
        if (sUser.getRole().equals(supportConfigModel.role)) {
          if (StringUtils.isEmpty(supportConfigModel.em) || StringUtils
              .isEmpty(supportConfigModel.phnm)) {
            SupportConfigModel scm = getAlternateSupportConfig();
            if (StringUtils.isEmpty(supportConfigModel.em)) {
              supportConfigModel.em = scm.em;
            }
            if (StringUtils.isEmpty(supportConfigModel.phnm)) {
              supportConfigModel.phnm = scm.phnm;
            }
          }
          return supportConfigModel;
        }
      }

      return getAlternateSupportConfig();
    } catch (ObjectNotFoundException e) {
      xLogger.severe("Error in fetching support configuration for the domain", e);
      throw new InvalidServiceException(
          backendMessages.getString("general.support.config.fetch.error"));
    }
  }

  private SupportConfigModel getAlternateSupportConfig() {
    // Get support configuration from System configuration ("generalconfig")
    SupportConfigModel scm = getSupportFromSystemConfig();
    if (StringUtils.isEmpty(scm.em) || StringUtils.isEmpty(scm.phnm)) {
      // Get default support configuration from samaanguru.properties
      SupportConfigModel defScm = getSupportConfigFromProperties();
      if (StringUtils.isEmpty(scm.em)) {
        scm.em = defScm.em;
      }
      if (StringUtils.isEmpty(scm.phnm)) {
        scm.phnm = defScm.phnm;
      }
    }
    return scm;
  }

  private SupportConfigModel getSupportFromSystemConfig() {
    SecureUserDetails user = SecurityUtils.getUserDetails();
    Locale locale = user.getLocale();
    SupportConfigModel scm = new SupportConfigModel();

    ResourceBundle backendMessages = Resources.getBundle(locale);
    try {
      IConfig c = configurationMgmtService.getConfiguration(IConfig.GENERALCONFIG);
      if (c != null && c.getConfig() != null) {
        String jsonString = c.getConfig();
        JSONObject jsonObject;
        try {
          jsonObject = new JSONObject(jsonString);
        } catch (JSONException e) {
          xLogger.severe("Error while getting generalconfig from System configuration");
          throw new InvalidServiceException(
              backendMessages.getString("general.config.fetch.error"));
        }
        try {
          scm.em = jsonObject.getString("supportemail");
        } catch (JSONException e) {
          xLogger.warn(
              "Ignoring JSONException while getting supportemail from system configuration due to {0}",
              e.getMessage(), e);
        }
        try {
          scm.phnm = jsonObject.getString("supportphone");
        } catch (JSONException e) {
          xLogger.warn(
              "Ignoring JSONException while getting supportphone from system configuration due to {0}",
              e.getMessage(), e);
        }
      }
    } catch (ServiceException | ObjectNotFoundException e) {
      xLogger.severe("Error while getting generalconfig from System configuration");
      throw new InvalidServiceException(backendMessages.getString("general.config.fetch.error"));
    }
    return scm;
  }

  private SupportConfigModel getSupportConfigFromProperties() {
    SupportConfigModel scm = new SupportConfigModel();
    scm.em = ConfigUtil.get("support.email", GeneralConfig.DEFAULT_SUPPORT_EMAIL);
    scm.phnm = ConfigUtil.get("support.phone", GeneralConfig.DEFAULT_SUPPORT_PHONE);
    return scm;
  }

  private List<IUserAccount> constructUserAccount(List<String> userIds) {
    if (userIds != null && !userIds.isEmpty()) {
      List<IUserAccount> list = new ArrayList<>(userIds.size());
      for (String userId : userIds) {
        try {
          list.add(usersService.getUserAccount(userId));
        } catch (Exception ignored) {
          // do nothing
        }
      }
      return list;
    }
    return new ArrayList<>();
  }

  public List<GeneralConfigModel> buildDomainLocationModels(List<String> domainIds,
                                                            UsersService userService,
                                                            String userName)
      throws ServiceException{
    List<GeneralConfigModel> configModel = new ArrayList<>();
    for(String dId: domainIds) {
      if (!userService.hasAccessToDomain(userName, Long.valueOf(dId))) {
        xLogger.warn("User {0} does not have access to domain id {1}", userName, dId);
        throw new ForbiddenAccessException("User does not have access to domain");
      }
      configModel.add(buildDomainLocationModel(Long.valueOf(dId)));
    }
    return configModel;
  }

  protected String getActualTransConfigType(String transType, InventoryConfig ic) {
    ActualTransConfig atc = ic.getActualTransConfigByType(transType);
    return (atc != null ? atc.getTy() : ActualTransConfig.ACTUAL_NONE);
    }

  protected List<InventoryConfigModel.MTagReason> buildMTagReasonModelList(Map<String,ReasonConfig> mtagRsnsMap) {
    if (MapUtils.isEmpty(mtagRsnsMap)) {
    return Collections.emptyList();
    }
    return (mtagRsnsMap.entrySet()
                .stream()
                .map(entry->buildMtagReasonModel(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList()));
    }

  protected InventoryConfigModel.MTagReason buildMtagReasonModel(String mtag, ReasonConfig reasonConfig) {
    InventoryConfigModel.MTagReason mtagReason = new InventoryConfigModel.MTagReason();
    mtagReason.mtg = mtag;
    mtagReason.rsnCfgModel = buildReasonConfigModel(reasonConfig);
    return mtagReason;
  }

  protected List<String> getTagsByInvOperationAsList(Map<String,String> invOpTypeTags, String invOperation) {
    if (MapUtils.isNotEmpty(invOpTypeTags) &&
      StringUtils.isNotEmpty(invOpTypeTags.get(invOperation))) {
      return Arrays.asList(invOpTypeTags.get(invOperation).split(CharacterConstants.COMMA));
    }
    return Collections.emptyList();
  }

  public Map<String,String> getTagsByInventoryOperation(CapabilitiesConfigModel capConfigModel) {
    Map<String, String> tagsByInvOper = new HashMap<>(7);
    String issueTags = TagUtil.getCleanTags(StringUtils.join(capConfigModel.hii, ','),true);
    String receiptsTags = TagUtil.getCleanTags(StringUtils.join(capConfigModel.hir, ','), true);
    String stockTags = TagUtil.getCleanTags(StringUtils.join(capConfigModel.hip, ','), true);
    String discardTags = TagUtil.getCleanTags(StringUtils.join(capConfigModel.hiw, ','), true);
    String transferTags = TagUtil.getCleanTags(StringUtils.join(capConfigModel.hit, ','), true);
    String retIncTags = TagUtil.getCleanTags(StringUtils.join(capConfigModel.hiri, ','), true);
    String retOutTags = TagUtil.getCleanTags(StringUtils.join(capConfigModel.hiro, ','), true);

    tagsByInvOper.put(ITransaction.TYPE_ISSUE, issueTags);
    tagsByInvOper.put(ITransaction.TYPE_RECEIPT, receiptsTags);
    tagsByInvOper.put(ITransaction.TYPE_PHYSICALCOUNT, stockTags);
    tagsByInvOper.put(ITransaction.TYPE_WASTAGE, discardTags);
    tagsByInvOper.put(ITransaction.TYPE_TRANSFER, transferTags);
    tagsByInvOper.put(ITransaction.TYPE_RETURNS_INCOMING, retIncTags);
    tagsByInvOper.put(ITransaction.TYPE_RETURNS_OUTGOING, retOutTags);
    return tagsByInvOper;
  }

  public Map<String,ReasonConfig> buildReasonConfigByTransType(InventoryConfigModel invConfigModel) {
    // Set reasons
    Map<String, ReasonConfig> reasonConfigByTransType = new HashMap<>(7,1);
    reasonConfigByTransType.put(ITransaction.TYPE_ISSUE, buildReasonConfig(invConfigModel.ri));
    reasonConfigByTransType.put(ITransaction.TYPE_RECEIPT, buildReasonConfig(invConfigModel.rr));
    reasonConfigByTransType.put(ITransaction.TYPE_PHYSICALCOUNT, buildReasonConfig(
        invConfigModel.rs));
    reasonConfigByTransType.put(ITransaction.TYPE_WASTAGE, buildReasonConfig(invConfigModel.rd));
    reasonConfigByTransType.put(ITransaction.TYPE_TRANSFER, buildReasonConfig(invConfigModel.rt));
    reasonConfigByTransType.put(ITransaction.TYPE_RETURNS_INCOMING, buildReasonConfig(
        invConfigModel.rri));
    reasonConfigByTransType.put(ITransaction.TYPE_RETURNS_OUTGOING, buildReasonConfig(
        invConfigModel.rro));
    return reasonConfigByTransType;

  }

  public ReasonConfig buildReasonConfig(ReasonConfigModel reasonConfigModel) {
    ReasonConfig reasonConfig = new ReasonConfig();
    if (reasonConfigModel == null) {
      return reasonConfig;
    }
    reasonConfig.setDefaultReason(reasonConfigModel.defRsn);
    reasonConfig.setReasons(reasonConfigModel.rsns);
    return reasonConfig;
  }

  public Map<String,ReasonConfig> buildReasonConfigByTagMap(List<InventoryConfigModel.MTagReason> mTagRsnList) {
    if (CollectionUtils.isEmpty(mTagRsnList)) {
      return Collections.emptyMap();
    }
    Map<String,ReasonConfig> reasonConfigByTagMap = new HashMap<>(1);
    for (InventoryConfigModel.MTagReason mTagReason : mTagRsnList) {
      reasonConfigByTagMap.put(mTagReason.mtg, buildReasonConfig(mTagReason.rsnCfgModel));
    }
    return reasonConfigByTagMap;
  }

  public ActualTransConfig buildActualTransConfig(String actTransConfig) {
    ActualTransConfig actualTransConfig = new ActualTransConfig();
    actualTransConfig.setTy(actTransConfig != null ? actTransConfig : ActualTransConfig.ACTUAL_NONE);
    return actualTransConfig;
  }

  public MatStatusConfig buildMatStatusConfig(String defaultStatus, String mstTempSensMat, boolean mandatory) {
    MatStatusConfig matStConfig = new MatStatusConfig();
    matStConfig.setDf(StringUtil.trimCommas(defaultStatus));
    matStConfig.setEtsm(StringUtil.trimCommas(mstTempSensMat));
    matStConfig.setStatusMandatory(mandatory);
    return matStConfig;
  }

  public List<ReturnsConfig> buildReturnsConfigs(List<ReturnsConfigModel> returnsConfigModels) {
    if (CollectionUtils.isEmpty(returnsConfigModels)) {
      return Collections.emptyList();
    }
    return returnsConfigModels.stream()
        .map(this::buildReturnsConfig)
        .collect(Collectors.toList());
  }

  public ReturnsConfig buildReturnsConfig(ReturnsConfigModel returnsConfigModel) {
    ReturnsConfig returnsConfig = new ReturnsConfig();
    if (returnsConfigModel == null) {
      return returnsConfig;
    }
    returnsConfig.setEntityTags(returnsConfigModel.eTags);
    returnsConfig.setIncomingDuration(returnsConfigModel.incDur);
    returnsConfig.setOutgoingDuration(returnsConfigModel.outDur);
    return returnsConfig;
  }

  public List<ReturnsConfigModel> buildReturnsConfigModels(List<ReturnsConfig> returnsConfigList) {
    if (CollectionUtils.isEmpty(returnsConfigList)) {
      return Collections.emptyList();
    }
    return returnsConfigList.stream()
        .map(this::buildReturnsConfigModel)
        .collect(Collectors.toList());
  }

  public ReturnsConfigModel buildReturnsConfigModel(ReturnsConfig returnsConfig) {
    ReturnsConfigModel returnsConfigModel = new ReturnsConfigModel();
    if (returnsConfig == null) {
      return returnsConfigModel;
    }
    returnsConfigModel.eTags = returnsConfig.getEntityTags();
    returnsConfigModel.incDur = returnsConfig.getIncomingDuration();
    returnsConfigModel.outDur = returnsConfig.getOutgoingDuration();
    return returnsConfigModel;
  }

  public ReasonConfigModel buildReasonConfigModel(ReasonConfig reasonConfig) {
    ReasonConfigModel reasonConfigModel = new ReasonConfigModel();
    if (reasonConfig == null) {
      return reasonConfigModel;
    }
    reasonConfigModel.rsns = reasonConfig.getReasons();
    reasonConfigModel.defRsn = reasonConfig.getDefaultReason();
    return reasonConfigModel;
  }

  /**
   * Builds a map of Actual Date Of Transaction Configuration (as a String) by transaction type
   */
  public Map<String, String> buildActualTransConfigAsStringByTransType(
      InventoryConfig inventoryConfig) {
    if (inventoryConfig == null) {
      throw new IllegalArgumentException(
          "Invalid input parameter while building actual date of transaction configuration as string by transaction type: "
              + inventoryConfig);
    }
    Map<String, String> actTransConfigStringByType = new HashMap<>(7, 1);
    actTransConfigStringByType.put(ITransaction.TYPE_ISSUE,
        inventoryConfig.getActualTransConfigByType(ITransaction.TYPE_ISSUE) != null
            ? inventoryConfig.getActualTransConfigByType(ITransaction.TYPE_ISSUE).getTy()
            : ActualTransConfig.ACTUAL_NONE);
    actTransConfigStringByType.put(ITransaction.TYPE_RECEIPT,
        inventoryConfig.getActualTransConfigByType(ITransaction.TYPE_RECEIPT) != null
            ? inventoryConfig.getActualTransConfigByType(ITransaction.TYPE_RECEIPT).getTy()
            : ActualTransConfig.ACTUAL_NONE);
    actTransConfigStringByType.put(ITransaction.TYPE_PHYSICALCOUNT,
        inventoryConfig.getActualTransConfigByType(ITransaction.TYPE_PHYSICALCOUNT) != null
            ? inventoryConfig.getActualTransConfigByType(ITransaction.TYPE_PHYSICALCOUNT).getTy()
            : ActualTransConfig.ACTUAL_NONE);
    actTransConfigStringByType.put(ITransaction.TYPE_WASTAGE,
        inventoryConfig.getActualTransConfigByType(ITransaction.TYPE_WASTAGE) != null
            ? inventoryConfig.getActualTransConfigByType(ITransaction.TYPE_WASTAGE).getTy()
            : ActualTransConfig.ACTUAL_NONE);
    actTransConfigStringByType.put(ITransaction.TYPE_TRANSFER,
        inventoryConfig.getActualTransConfigByType(ITransaction.TYPE_TRANSFER) != null
            ? inventoryConfig.getActualTransConfigByType(ITransaction.TYPE_TRANSFER).getTy()
            : ActualTransConfig.ACTUAL_NONE);
    actTransConfigStringByType.put(ITransaction.TYPE_RETURNS_INCOMING,
        inventoryConfig.getActualTransConfigByType(ITransaction.TYPE_RETURNS_INCOMING) != null
            ? inventoryConfig.getActualTransConfigByType(ITransaction.TYPE_RETURNS_INCOMING).getTy()
            : ActualTransConfig.ACTUAL_NONE);
    actTransConfigStringByType.put(ITransaction.TYPE_RETURNS_OUTGOING,
        inventoryConfig.getActualTransConfigByType(ITransaction.TYPE_RETURNS_OUTGOING) != null
            ? inventoryConfig.getActualTransConfigByType(ITransaction.TYPE_RETURNS_OUTGOING).getTy()
            : ActualTransConfig.ACTUAL_NONE);
    return actTransConfigStringByType;
  }
}
