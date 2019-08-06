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

import com.logistimo.AppFactory;
import com.logistimo.api.builders.BulletinBoardBuilder;
import com.logistimo.api.builders.ConfigurationModelBuilder;
import com.logistimo.api.builders.CurrentUserBuilder;
import com.logistimo.api.builders.CustomReportsBuilder;
import com.logistimo.api.builders.FormsConfigBuilder;
import com.logistimo.api.builders.NotificationBuilder;
import com.logistimo.api.builders.TransportersConfigBuilder;
import com.logistimo.api.builders.UserBuilder;
import com.logistimo.api.builders.UserMessageBuilder;
import com.logistimo.api.constants.ConfigConstants;
import com.logistimo.api.models.AccessLogModel;
import com.logistimo.api.models.CurrentUserModel;
import com.logistimo.api.models.MenuStatsModel;
import com.logistimo.api.models.TagsModel;
import com.logistimo.api.models.UserMessageModel;
import com.logistimo.api.models.configuration.AccountingConfigModel;
import com.logistimo.api.models.configuration.AdminContactConfigModel;
import com.logistimo.api.models.configuration.ApprovalsConfigModel;
import com.logistimo.api.models.configuration.ApprovalsEnabledConfigModel;
import com.logistimo.api.models.configuration.AssetConfigModel;
import com.logistimo.api.models.configuration.BulletinBoardConfigModel;
import com.logistimo.api.models.configuration.CapabilitiesConfigModel;
import com.logistimo.api.models.configuration.CustomReportsConfigModel;
import com.logistimo.api.models.configuration.DashboardConfigModel;
import com.logistimo.api.models.configuration.FormsConfigModel;
import com.logistimo.api.models.configuration.GeneralConfigModel;
import com.logistimo.api.models.configuration.InventoryConfigModel;
import com.logistimo.api.models.configuration.NotificationsConfigModel;
import com.logistimo.api.models.configuration.NotificationsModel;
import com.logistimo.api.models.configuration.OrdersConfigModel;
import com.logistimo.api.models.configuration.ReturnsConfigModel;
import com.logistimo.api.models.configuration.StockRebalancingConfigModel;
import com.logistimo.api.models.configuration.SupportConfigModel;
import com.logistimo.api.models.configuration.TagsConfigModel;
import com.logistimo.api.request.AddCustomReportRequestObj;
import com.logistimo.api.util.FileValidationUtil;
import com.logistimo.auth.GenericAuthoriser;
import com.logistimo.auth.SecurityConstants;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.communications.MessageHandlingException;
import com.logistimo.config.entity.IConfig;
import com.logistimo.config.models.AccountingConfig;
import com.logistimo.config.models.AdminContactConfig;
import com.logistimo.config.models.ApprovalsConfig;
import com.logistimo.config.models.AssetConfig;
import com.logistimo.config.models.AssetSystemConfig;
import com.logistimo.config.models.BBoardConfig;
import com.logistimo.config.models.CapabilityConfig;
import com.logistimo.config.models.ConfigurationException;
import com.logistimo.config.models.CustomReportsConfig;
import com.logistimo.config.models.DashboardConfig;
import com.logistimo.config.models.DemandBoardConfig;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.config.models.EventSummaryConfigModel;
import com.logistimo.config.models.EventsConfig;
import com.logistimo.config.models.FormsConfig;
import com.logistimo.config.models.InventoryConfig;
import com.logistimo.config.models.LeadTimeAvgConfig;
import com.logistimo.config.models.OptimizerConfig;
import com.logistimo.config.models.OrdersConfig;
import com.logistimo.config.models.ReportsConfig;
import com.logistimo.config.models.ReturnsConfig;
import com.logistimo.config.models.StockRebalancingConfig;
import com.logistimo.config.models.SupportConfig;
import com.logistimo.config.models.SyncConfig;
import com.logistimo.config.models.TransportersConfig;
import com.logistimo.api.models.configuration.TransportersConfigModel;
import com.logistimo.config.service.ConfigurationMgmtService;
import com.logistimo.config.utils.eventsummary.EventSummaryTemplateLoader;
import com.logistimo.constants.CharacterConstants;
import com.logistimo.constants.Constants;
import com.logistimo.dao.JDOUtils;
import com.logistimo.domains.entity.IDomain;
import com.logistimo.entity.IALog;
import com.logistimo.entity.IBBoard;
import com.logistimo.entity.IJobStatus;
import com.logistimo.entity.IMessageLog;
import com.logistimo.entity.IUploaded;
import com.logistimo.events.handlers.BBHandler;
import com.logistimo.exception.BadRequestException;
import com.logistimo.exception.ConfigurationServiceException;
import com.logistimo.exception.ForbiddenAccessException;
import com.logistimo.exception.InvalidDataException;
import com.logistimo.exception.InvalidServiceException;
import com.logistimo.exception.InvalidTaskException;
import com.logistimo.exception.TaskSchedulingException;
import com.logistimo.inventory.entity.ITransaction;
import com.logistimo.inventory.service.InventoryManagementService;
import com.logistimo.logger.XLog;
import com.logistimo.pagination.Navigator;
import com.logistimo.pagination.PageParams;
import com.logistimo.pagination.Results;
import com.logistimo.reports.ReportsConstants;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.ObjectNotFoundException;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;
import com.logistimo.services.blobstore.BlobstoreService;
import com.logistimo.services.cache.MemcacheService;
import com.logistimo.services.impl.PMF;
import com.logistimo.services.taskqueue.ITaskService;
import com.logistimo.tags.TagUtil;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.users.service.UsersService;
import com.logistimo.utils.JobUtil;
import com.logistimo.utils.LocalDateUtil;
import com.logistimo.utils.MessageUtil;
import com.logistimo.utils.QueryUtil;
import com.logistimo.utils.StringUtil;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.http.HttpServletRequest;

import static com.logistimo.auth.utils.SecurityUtils.getUserDetails;

@Controller
@RequestMapping("/config/domain")
public class DomainConfigController {
  private static final XLog xLogger = XLog.getLog(DomainConfigController.class);
  private static final int ENTITY = 1;
  private static final int MATERIAL = 2;
  private static final int ORDER = 3;
  private static final int ROUTE = 4;
  private static final int USER = 5;
  private static final String BACKEND_MESSAGES = "BackendMessages";
  private static final String PERMISSION_DENIED = "permission.denied";
  private static final String INVENTORY_CONFIG_FETCH_ERROR = "inventory.config.fetch.error";
  private static final String INVENTORY_CONFIG_UPDATE_ERROR = "inventory.config.update.error";
  private static final String CAPABILITIES_CONFIG_FETCH_ERROR = "capabilities.config.fetch.error";
  private static final String ORDERS_CONFIG_UPDATE_ERROR = "orders.config.update.error";
  private static final String NOTIFICATION_CONFIG_UPDATE_ERROR = "notif.config.update.error";
  private static final String NOTIFICATION_CONFIG_FETCH_ERROR = "notif.config.fetch.error";
  private static final String NOTIFICATION_CONFIG_DELETE_ERROR = "notif.delete.error";
  private static final String BULLETIN_BOARD_CONFIG_UPDATE_ERROR = "bulletin.config.update.error";

  private static final String ASSET_METADATA_FETCH_ERROR = "Error in reading asset meta data.";


  private UserMessageBuilder userMessageBuilder;
  private ConfigurationModelBuilder configurationModelBuilder;
  private CustomReportsBuilder customReportBuilder;
  private UserBuilder userBuilder;
  private BulletinBoardBuilder bulletinBoardBuilder;
  private CurrentUserBuilder currentUserBuilder;
  private NotificationBuilder notificationBuilder;
  private ConfigurationMgmtService configurationMgmtService;
  private UsersService usersService;
  private FormsConfigBuilder formsConfigBuilder;

  @Autowired
  private InventoryManagementService inventoryManagementService;

  @Autowired
  public void setUserMessageBuilder(UserMessageBuilder userMessageBuilder) {
    this.userMessageBuilder = userMessageBuilder;
  }

  @Autowired
  public void setConfigurationModelBuilder(ConfigurationModelBuilder configurationModelBuilder) {
    this.configurationModelBuilder = configurationModelBuilder;
  }

  @Autowired
  public void setCustomReportBuilder(CustomReportsBuilder customReportBuilder) {
    this.customReportBuilder = customReportBuilder;
  }

  @Autowired
  public void setUserBuilder(UserBuilder userBuilder) {
    this.userBuilder = userBuilder;
  }

  @Autowired
  public void setBulletinBoardBuilder(BulletinBoardBuilder bulletinBoardBuilder) {
    this.bulletinBoardBuilder = bulletinBoardBuilder;
  }

  @Autowired
  public void setCurrentUserBuilder(CurrentUserBuilder currentUserBuilder) {
    this.currentUserBuilder = currentUserBuilder;
  }

  @Autowired
  public void setNotificationBuilder(NotificationBuilder notificationBuilder) {
    this.notificationBuilder = notificationBuilder;
  }

  @Autowired
  public void setConfigurationMgmtService(ConfigurationMgmtService configurationMgmtService) {
    this.configurationMgmtService = configurationMgmtService;
  }

  @Autowired
  public void setFormsConfigBuilder(FormsConfigBuilder formsConfigBuilder) {
    this.formsConfigBuilder = formsConfigBuilder;
  }

  @Autowired
  public void setUsersService(UsersService usersService) {
    this.usersService = usersService;
  }

  @RequestMapping(value = "/tags/materials", method = RequestMethod.GET)
  public
  @ResponseBody
  TagsModel getMaterialTags() {
    return getTags(MATERIAL);
  }

  @RequestMapping(value = "/tags/route", method = RequestMethod.GET)
  public
  @ResponseBody
  TagsModel getRouteTags() {
    return getTags(ROUTE);
  }

  @RequestMapping(value = "/tags/order", method = RequestMethod.GET)
  public
  @ResponseBody
  TagsModel getOrderTags() {
    return getTags(ORDER);
  }

  @RequestMapping(value = "/tags/entities", method = RequestMethod.GET)
  public
  @ResponseBody
  TagsModel getEntityTags() {
    return getTags(ENTITY);
  }

  @RequestMapping(value = "/tags/user", method = RequestMethod.GET)
  public
  @ResponseBody
  TagsModel getUserTags() {
    return getTags(USER);
  }

  private TagsModel getTags(int type) {
    Long domainId = SecurityUtils.getCurrentDomainId();
    DomainConfig dc = DomainConfig.getInstance(domainId);
    String tags;
    boolean allowUserDef = false;
    switch (type) {
      case ENTITY:
        tags = dc.getKioskTags();
        allowUserDef = !dc.forceTagsKiosk();
        break;
      case MATERIAL:
        tags = dc.getMaterialTags();
        allowUserDef = !dc.forceTagsMaterial();
        break;
      case ROUTE:
        tags = dc.getRouteTags();
        break;
      case ORDER:
        tags = dc.getOrderTags();
        allowUserDef = !dc.forceTagsOrder();
        break;
      case USER:
        tags = dc.getUserTags();
        allowUserDef = !dc.forceTagsUser();
        break;
      default:
        throw new InvalidServiceException("Unrecognised tag type:" + type);
    }
    return new TagsModel(StringUtil.getList(tags), allowUserDef);
  }

  private List<String> generateUpdateList(String uId) {
    List<String> list = new ArrayList<>(2);
    list.add(uId);
    list.add(String.valueOf(System.currentTimeMillis()));
    return list;
  }

  @RequestMapping(value = "/country", method = RequestMethod.GET)
  public
  @ResponseBody
  List<String> getCountries() {
    Long domainId = SecurityUtils.getCurrentDomainId();
    DomainConfig dc = DomainConfig.getInstance(domainId);
    return StringUtil.getList(dc.getCountry());
  }

  @RequestMapping(value = "/artype", method = RequestMethod.GET)
  public
  @ResponseBody
  String getActualRouteType() {
    Long domainId = SecurityUtils.getCurrentDomainId();
    return DomainConfig.getInstance(domainId).getRouteBy();
  }

  @RequestMapping(value = "/", method = RequestMethod.GET)
  public
  @ResponseBody
  DomainConfig getDomainConfig() {
    Long domainId = SecurityUtils.getCurrentDomainId();
    return DomainConfig.getInstance(domainId);
  }

  @RequestMapping(value = "/menustats", method = RequestMethod.GET)
  public
  @ResponseBody
  MenuStatsModel getDomainConfigMenuStats() {
    SecureUserDetails user = getUserDetails();
    Locale locale = user.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    Long domainId = SecurityUtils.getCurrentDomainId();
    DomainConfig config = DomainConfig.getInstance(domainId);
    if (config != null) {
      try {
        return configurationModelBuilder.buildMenuStats(user, config, locale, user.getTimezone());
      } catch (ServiceException | ObjectNotFoundException e) {
        throw new InvalidServiceException(backendMessages.getString("menustats.fetch.error"));
      }
    } else {
      xLogger.severe("Error in fetching menu status");
      throw new InvalidServiceException(backendMessages.getString("menustats.fetch.error"));
    }
  }

  @RequestMapping(value = "/optimizer", method = RequestMethod.GET)
  public
  @ResponseBody
  OptimizerConfig getOptimizerConfig() {
    Long domainId = SecurityUtils.getCurrentDomainId();
    DomainConfig dc = DomainConfig.getInstance(domainId);
    return dc.getOptimizerConfig();
  }

  @RequestMapping(value = "/general", method = RequestMethod.GET)
  public
  @ResponseBody
  GeneralConfigModel getGeneralConfig(
      @RequestParam(name = "domain_id", required = false) Long domainId)
      throws ServiceException {
    SecureUserDetails sUser = getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    Long
        dId =
        (null == domainId) ? SecurityUtils.getCurrentDomainId() : domainId;
    if (!usersService.hasAccessToDomain(sUser.getUsername(), dId)) {
      xLogger.warn("User {0} does not have access to domain id {1}", sUser.getUsername(), domainId);
      throw new InvalidDataException("User does not have access to domain");
    }
    try {
      return configurationModelBuilder.buildDomainLocationModels(dId, locale, sUser.getTimezone());
    } catch (Exception e) {
      xLogger.severe("Error in fetching general configuration", e);
      throw new InvalidServiceException(backendMessages.getString("general.config.fetch.error"));
    }
  }

  @RequestMapping(value = "/support", method = RequestMethod.GET)
  public
  @ResponseBody
  SupportConfigModel getSupportConfig() {
    SecureUserDetails user = getUserDetails();
    Locale locale = user.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    try {
      return configurationModelBuilder.buildSCModelForWebDisplay();
    } catch (ObjectNotFoundException e) {
      xLogger.severe("Error in fetching support configuration for the domain", e);
      throw new InvalidServiceException(
          backendMessages.getString("general.support.config.fetch.error"));
    }
  }

  @RequestMapping(value = "/admin", method = RequestMethod.GET)
  public
  @ResponseBody
  AdminContactConfigModel getAdminConfig() {
    ResourceBundle
        backendMessages =
        Resources.getBundle(SecurityUtils.getLocale());
    try {
      return configurationModelBuilder.buildAdminContactModel(SecurityUtils.getUsername());
    } catch (ObjectNotFoundException e) {
      xLogger.severe("Error in fetching administrative contact configuration for the domain", e);
      throw new InvalidServiceException(
          backendMessages.getString("general.admin.config.fetch.error"));
    }
  }

  @RequestMapping(value = "/accounts", method = RequestMethod.GET)
  public
  @ResponseBody
  AccountingConfigModel getAccountingConfig() {
    SecureUserDetails sUser = getUserDetails();
    Long domainId = SecurityUtils.getCurrentDomainId();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    try {
      return configurationModelBuilder.buildAccountingConfigModel(domainId, locale, sUser.getTimezone());
    } catch (ObjectNotFoundException | JSONException e) {
      xLogger.severe("Error in fetching account configuration", e);
      throw new InvalidServiceException(backendMessages.getString("account.config.fetch.error"));
    }
  }

  @RequestMapping(value = "/tags", method = RequestMethod.GET)
  public
  @ResponseBody
  TagsConfigModel getTags() {
    SecureUserDetails sUser = getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    Long domainId = SecurityUtils.getCurrentDomainId();
    try {
      if (domainId == null) {
        throw new InvalidServiceException(backendMessages.getString("tags.config.fetch.error"));
      }
      DomainConfig dc = DomainConfig.getInstance(domainId);
      return configurationModelBuilder.buildTagsConfigModel(dc, locale, sUser.getTimezone());
    } catch (ObjectNotFoundException | ConfigurationServiceException e) {
      xLogger.severe("Error in fetching tags", e);
      throw new InvalidServiceException(backendMessages.getString("tags.config.fetch.error"));
    }
  }

  @RequestMapping(value = "/tags", method = RequestMethod.POST)
  public
  @ResponseBody
  String setTags(@RequestBody TagsConfigModel model) {
    SecureUserDetails sUser = getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    String updateTagsConfigErrorMessage = "tags.config.update.error";
    if (model == null) {
      throw new BadRequestException(backendMessages.getString(updateTagsConfigErrorMessage));
    }
    String userId = sUser.getUsername();
    Long domainId = SecurityUtils.getCurrentDomainId();
    try {
      if (domainId == null) {
        xLogger.severe("Error in updating tags configuration");
        throw new InvalidServiceException(backendMessages.getString(updateTagsConfigErrorMessage));
      }
      ConfigContainer cc = getDomainConfig(domainId, userId);
      cc.dc.addDomainData(ConfigConstants.TAGS, generateUpdateList(userId));
      StringBuilder mtags = new StringBuilder();
      for (int i = 0; i < model.mt.length; i++) {
        if (StringUtils.isNotEmpty(model.mt[i])) {
          mtags.append(model.mt[i].concat(","));
        }
      }
      if (mtags.length() > 0) {
        mtags.setLength(mtags.length() - 1);
      }
      cc.dc.setMaterialTags(TagUtil.getCleanTags(mtags.toString(), true));
      StringBuilder etags = new StringBuilder();
      for (int j = 0; j < model.et.length; j++) {
        if (StringUtils.isNotEmpty(model.et[j])) {
          etags.append(model.et[j].concat(","));
        }
      }
      cc.dc.setKioskTags(TagUtil.getCleanTags(etags.toString(), true));
      StringBuilder rtags = new StringBuilder();
      for (int z = 0; z < model.rt.length; z++) {
        if (StringUtils.isNotEmpty(model.rt[z])) {
          rtags.append(model.rt[z].concat(","));
        }
      }
      cc.dc.setRouteTags(TagUtil.getCleanTags(rtags.toString(), false));
      StringBuilder otags = new StringBuilder();
      for (int z = 0; z < model.ot.length; z++) {
        if (StringUtils.isNotEmpty(model.ot[z])) {
          otags.append(model.ot[z].concat(","));
        }
      }
      cc.dc.setOrderTags(TagUtil.getCleanTags(otags.toString(), true));
      StringBuilder utags = new StringBuilder();
      for (int z = 0; z < model.ut.length; z++) {
        if (StringUtils.isNotEmpty(model.ut[z])) {
          utags.append(model.ut[z].concat(","));
        }
      }

      Map<String, Integer> entityOrderMap = new LinkedHashMap<>();
      if (model.etr != null && !model.etr.isEmpty()) {
        for (TagsConfigModel.ETagOrder eto : model.etr) {
          entityOrderMap.put(eto.etg, eto.rnk);
        }
      }
      cc.dc.setEntityTagOrder(entityOrderMap);
      cc.dc.setUserTags(TagUtil.getCleanTags(utags.toString(), true));

      cc.dc.setForceTagsKiosk(model.eet);
      cc.dc.setForceTagsMaterial(model.emt);
      cc.dc.setForceTagsOrder(model.eot);
      cc.dc.setRouteBy(model.en);
      cc.dc.setForceTagsUser(model.eut);
      saveDomainConfig(domainId, cc, backendMessages);
      xLogger.info("AUDITLOG \t {0} \t {1} \t CONFIGURATION \t " +
          "SET TAGS ", domainId, sUser.getUsername());
    } catch (ServiceException | ConfigurationException e) {
      xLogger.severe("Error in updating tags configuration", e);
      throw new InvalidServiceException(backendMessages.getString(updateTagsConfigErrorMessage));
    }
    return backendMessages.getString("tags.config.update.success");
  }

  @RequestMapping(value = "/map/locations", method = RequestMethod.GET)
  public
  @ResponseBody
  String getConfiguredMapLocations() {
    Long domainId = SecurityUtils.getCurrentDomainId();

    try {
      IConfig c = configurationMgmtService.getConfiguration(IConfig.MAPLOCATIONCONFIG);
      return c.getConfig();
    } catch (Exception e) {
      xLogger.warn("{0} while fetching map location configuration for domain {1}", e.getMessage(),
          domainId);
    }

    return "{}";
  }

  @RequestMapping(value = "/dashboards", method = RequestMethod.GET)
  public
  @ResponseBody
  String getSystemDashboardConfig() {
    Long domainId = SecurityUtils.getCurrentDomainId();
    try {
      IConfig c = configurationMgmtService.getConfiguration(IConfig.DASHBOARDCONFIG);
      return c.getConfig();
    } catch (Exception e) {
      xLogger.warn("{0} while fetching map location configuration for domain {1}", e.getMessage(),
          domainId);
    }

    return "{}";
  }

  @RequestMapping(value = "/asset", method = RequestMethod.GET)
  public
  @ResponseBody
  AssetConfigModel getAssetConfig() {
    SecureUserDetails sUser = getUserDetails();
    Locale locale = sUser.getLocale();
    Long domainId = SecurityUtils.getCurrentDomainId();
    DomainConfig dc = DomainConfig.getInstance(domainId);
    try {
      return configurationModelBuilder.buildAssetConfigModel(dc, locale, sUser.getTimezone());
    } catch (ConfigurationException | ObjectNotFoundException e) {
      xLogger.severe("Error in fetching vendor names");
    }

    return null;
  }

  @RequestMapping(value = "/assetconfig", method = RequestMethod.GET)
  public
  @ResponseBody
  Map<Integer, String> getAsset(@RequestParam String type) {
    try {
      AssetSystemConfig assets = AssetSystemConfig.getInstance();
      return assets.getAssetsNameByType(Integer.valueOf(type));
    } catch (ConfigurationException e) {
      xLogger.severe("Error in reading Asset System Configuration", e);
      throw new InvalidServiceException(ASSET_METADATA_FETCH_ERROR);
    }
  }

  @RequestMapping(value = "/assetconfig/manufacturer", method = RequestMethod.GET)
  public
  @ResponseBody
  Map<String, String> getAssetManufacturer(@RequestParam String type) {
    try {
      AssetSystemConfig assets = AssetSystemConfig.getInstance();
      return assets.getManufacturersByType(Integer.valueOf(type));
    } catch (ConfigurationException e) {
      xLogger.severe("Error in reading Asset System Configuration for manufacturers", e);
      throw new InvalidServiceException(ASSET_METADATA_FETCH_ERROR);
    }
  }

  @RequestMapping(value = "/assetconfig/workingstatus", method = RequestMethod.GET)
  public
  @ResponseBody
  Map<Integer, String> getAssetWorkingStatus() {
    try {
      AssetSystemConfig assets = AssetSystemConfig.getInstance();
      return assets.getAllWorkingStatus();
    } catch (ConfigurationException e) {
      xLogger.severe("Error in reading Asset System Configuration for manufacturers", e);
      throw new InvalidServiceException(ASSET_METADATA_FETCH_ERROR);
    }
  }

  @RequestMapping(value = "/asset", method = RequestMethod.POST)
  public
  @ResponseBody
  String updateAssetConfig(@RequestBody AssetConfigModel assetConfigModel) {
    SecureUserDetails sUser = getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    if (!GenericAuthoriser.authoriseAdmin()) {
      throw new ForbiddenAccessException(
          backendMessages.getString(backendMessages.getString(PERMISSION_DENIED)));
    }
    if (assetConfigModel == null) {
      xLogger.severe("Error in updating temperature configuration");
      throw new BadRequestException(backendMessages.getString("temp.config.update.error"));
    }
    try {
      String userId = sUser.getUsername();
      Long domainId = SecurityUtils.getCurrentDomainId();

      List<String> cfgVals = new ArrayList<>(1), modelVals = new ArrayList<>(1),
          dSnsVals =
              new ArrayList<>(), dMpsVals = new ArrayList<>();
      if (assetConfigModel.assets != null) {
        for (AssetConfigModel.Asset asset : assetConfigModel.assets.values()) {
          if (asset.dMp != null) {
            dMpsVals.add(asset.id + Constants.KEY_SEPARATOR + asset.dMp);
          }
          if (asset.mcs != null) {
            for (AssetConfigModel.Mancfacturer mancfacturer : asset.mcs.values()) {
              if (mancfacturer.iC != null && mancfacturer.iC) {
                if (asset.id.equals(AssetSystemConfig.TYPE_TEMPERATURE_DEVICE)) {
                  cfgVals.add(mancfacturer.id);
                } else {
                  cfgVals.add(asset.id + Constants.KEY_SEPARATOR + mancfacturer.id);
                }

                if (mancfacturer.model != null) {
                  for (AssetConfigModel.Model model : mancfacturer.model.values()) {
                    if (model.iC != null && model.iC) {
                      modelVals.add(asset.id + Constants.KEY_SEPARATOR + mancfacturer.id
                          + Constants.KEY_SEPARATOR + model.name);
                      dSnsVals.add(
                          asset.id + Constants.KEY_SEPARATOR + model.name + Constants.KEY_SEPARATOR
                              + model.dS);
                    }
                  }
                }
              }
            }
          }
        }
      }

      ConfigContainer cc = getDomainConfig(domainId, userId);

      AssetConfig tc = cc.dc.getAssetConfig();
      if (tc == null) {
        tc = new AssetConfig();
      }
      tc.setVendorIds(cfgVals);
      tc.setAssetModels(modelVals);
      tc.setDefaultSns(dSnsVals);
      tc.setDefaultMps(dMpsVals);
      tc.setEnable(assetConfigModel.enable);
      tc.setNamespace(assetConfigModel.namespace);
      if (assetConfigModel.config.getLocale() == null) {
        assetConfigModel.config.setLocale(new AssetConfig.Locale());
      }
      cc.dc.addDomainData(ConfigConstants.TEMPERATURE, generateUpdateList(userId));
      assetConfigModel.config.getLocale().setCn(cc.dc.getCountry());
      assetConfigModel.config.getLocale().setLn(cc.dc.getLanguage());
      assetConfigModel.config.getLocale().setTz(AssetConfig.getTimezoneOffset(cc.dc.getTimezone()));
      assetConfigModel.config.getLocale().setTznm(cc.dc.getTimezone());
      tc.setConfiguration(assetConfigModel.config);
      cc.dc.setAssetConfig(tc);
      saveDomainConfig(domainId, cc, backendMessages);
      xLogger.info("AUDITLOG \t {0} \t {1} \t CONFIGURATION \t " +
          "UPDATE TEMPERATURE", domainId, sUser.getUsername());
      xLogger.info(cc.dc.toJSONSring());
    } catch (ServiceException e) {
      xLogger.severe("Error in updating temperature configuration", e);
      throw new BadRequestException(backendMessages.getString("temp.config.update.error"));
    } catch (ConfigurationException e) {
      xLogger.severe("Error while saving asset configuration", e);
    }
    return backendMessages.getString("temp.config.update.success");
  }

  @RequestMapping(value = "/add", method = RequestMethod.POST)
  public
  @ResponseBody
  String updateAccountingConfig(@RequestBody AccountingConfigModel model) {
    SecureUserDetails sUser = getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    if (!GenericAuthoriser.authoriseAdmin()) {
      throw new ForbiddenAccessException(backendMessages.getString(PERMISSION_DENIED));
    }
    if (model == null) {
      xLogger.severe("Error in updating Accounting configuration");
      throw new BadRequestException(backendMessages.getString("account.config.update.error"));
    }
    String userId = sUser.getUsername();
    Long domainId = SecurityUtils.getCurrentDomainId();
    try {
      ConfigContainer cc = getDomainConfig(domainId, userId);
      AccountingConfig ac = new AccountingConfig();
      cc.dc.addDomainData(ConfigConstants.ACCOUNTING, generateUpdateList(userId));
      ac.setAccountingEnabled(model.ea);
      ac.setCreditLimit(model.cl);
      if ("cf".equalsIgnoreCase(model.en)) {
        ac.setEnforceConfirm(true);
      } else if ("cm".equalsIgnoreCase(model.en)) {
        ac.setEnforceShipped(true);
      }
      cc.dc.setAccountingConfig(ac);
      saveDomainConfig(domainId, cc, backendMessages);
      xLogger.info("AUDITLOG \t {0} \t {1} \t CONFIGURATION \t " +
          "UPDATE ACCOUNTING", domainId, sUser.getUsername());
      xLogger.info(cc.dc.toJSONSring());
    } catch (ServiceException e) {
      xLogger.severe("Error in updating Accounting configuration", e);
      throw new InvalidServiceException(backendMessages.getString("account.config.update.error"));
    } catch (ConfigurationException e) {
      xLogger.severe("Error while saving accounting configuration", e);
    }
    return backendMessages.getString("account.config.update.success");
  }

  @RequestMapping(value = "/general", method = RequestMethod.POST)
  public
  @ResponseBody
  String updateGeneralConfig(@RequestBody GeneralConfigModel model) {
    SecureUserDetails sUser = getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    if (!GenericAuthoriser.authoriseAdmin()) {
      throw new ForbiddenAccessException(backendMessages.getString(PERMISSION_DENIED));
    }
    String userId = sUser.getUsername();
    Long domainId = SecurityUtils.getCurrentDomainId();
    try {
      ConfigContainer cc = getDomainConfig(domainId, userId);
      cc.dc.addDomainData(ConfigConstants.GENERAL, generateUpdateList(userId));
      cc.dc.setCountry(model.cnt);
      cc.dc.setState(model.st);
      cc.dc.setDistrict(model.ds);
      cc.dc.setLanguage(model.lng);
      cc.dc.setTimezone(model.tz);
      cc.dc.setCurrency(model.cur);
      cc.dc.setPageHeader(model.pgh);
      if (model.sc) {
        cc.dc.setUiPreference(false);
      } else {
        cc.dc.setUiPreference(true);
      }
      if (model.support != null && !model.support.isEmpty()) {
        for (SupportConfigModel supportConfigModel : model.support) {
          SupportConfig sc = new SupportConfig();
          sc.setSupportUserRole(supportConfigModel.role);
          sc.setSupportUser(supportConfigModel.usrid);
          sc.setSupportUserName(supportConfigModel.usrname);
          sc.setSupportPhone(supportConfigModel.phnm);
          sc.setsupportEmail(supportConfigModel.em);
          cc.dc.setSupportConfigByRole(supportConfigModel.role, sc);
        }
      }

      if (cc.dc.getAssetConfig() != null && cc.dc.getAssetConfig().getConfiguration() != null
          && cc.dc.getAssetConfig().getConfiguration().getLocale() != null) {
        cc.dc.getAssetConfig().getConfiguration().getLocale().setCn(model.cnt);
        cc.dc.getAssetConfig().getConfiguration().getLocale().setLn(model.lng);
        cc.dc.getAssetConfig().getConfiguration().getLocale().setTznm(model.tz);
        cc.dc.getAssetConfig().getConfiguration().getLocale()
            .setTz(AssetConfig.getTimezoneOffset(model.tz));
      }
      AdminContactConfig adminContactConfig = cc.dc.getAdminContactConfig();
      adminContactConfig.setPrimaryAdminContact(
          model.adminContact.get(AdminContactConfig.PRIMARY_ADMIN_CONTACT).userId);
      adminContactConfig.setSecondaryAdminContact(
          model.adminContact.get(AdminContactConfig.SECONDARY_ADMIN_CONTACT).userId);
      cc.dc.setAdminContactConfigMap(adminContactConfig);
      cc.dc.setEnableSwitchToNewHost(model.snh);
      cc.dc.setNewHostName(model.nhn);

      saveDomainConfig(domainId, cc, backendMessages);
      xLogger.info("AUDITLOG \t {0} \t {1} \t CONFIGURATION \t " +
          "UPDATE GENERAL", domainId, sUser.getUsername());
      xLogger.info(cc.dc.toJSONSring());
    } catch (ServiceException e) {
      xLogger.severe("Error in updating general configuration", e);
      throw new InvalidServiceException(backendMessages.getString("general.config.update.error"));
    } catch (ConfigurationException e) {
      xLogger.warn("Error while printing configuration to log", e);
    }
    return backendMessages.getString("general.config.update.success");
  }

  @RequestMapping(value = "/capabilities", method = RequestMethod.GET)
  public
  @ResponseBody
  CapabilitiesConfigModel getCapabilitiesConfig() {
    SecureUserDetails sUser = getUserDetails();
    Long domainId = SecurityUtils.getCurrentDomainId();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    try {
      if (domainId == null) {
        xLogger.severe("Error in fetching capabilities configuration");
        throw new InvalidServiceException(
            backendMessages.getString(CAPABILITIES_CONFIG_FETCH_ERROR));
      }
      DomainConfig dc = DomainConfig.getInstance(domainId);
      return configurationModelBuilder.buildCapabilitiesConfigModel(locale, dc, sUser.getTimezone());
    } catch (ObjectNotFoundException e) {
      xLogger.warn("Error in fetching capabilities configuration for {0}: {1}", locale, e);
      throw new InvalidServiceException(
          backendMessages.getString(CAPABILITIES_CONFIG_FETCH_ERROR));
    }
  }

  @RequestMapping(value = "/rolecapabs", method = RequestMethod.GET)
  public
  @ResponseBody
  CapabilitiesConfigModel getRoleCapabilitiesConfig() {
    SecureUserDetails sUser = getUserDetails();
    Long domainId = SecurityUtils.getCurrentDomainId();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    try {
      if (domainId == null) {
        xLogger.severe("Error in fetching role capabilities configuration");
        throw new InvalidServiceException(
            backendMessages.getString(CAPABILITIES_CONFIG_FETCH_ERROR));
      }
      DomainConfig dc = DomainConfig.getInstance(domainId);
      return configurationModelBuilder.buildRoleCapabilitiesConfigModel(sUser.getRole(), locale, dc,
          sUser.getTimezone());
    } catch (ObjectNotFoundException e) {
      xLogger.warn("Error in fetching role capabilities configuration for {0}: {1}", locale, e);
      throw new InvalidServiceException(
          backendMessages.getString(CAPABILITIES_CONFIG_FETCH_ERROR));
    }
  }

  @RequestMapping(value = "/capabilities", method = RequestMethod.POST)
  public
  @ResponseBody
  List<String> updateCapabilitiesConfig(@RequestBody CapabilitiesConfigModel model) {
    SecureUserDetails sUser = getUserDetails();
    Locale locale = sUser.getLocale();
    String timezone = sUser.getTimezone();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    if (!GenericAuthoriser.authoriseAdmin()) {
      throw new ForbiddenAccessException(backendMessages.getString(PERMISSION_DENIED));
    }
    String userId = sUser.getUsername();
    Long domainId = SecurityUtils.getCurrentDomainId();
    try {
      ConfigContainer cc = getDomainConfig(domainId, userId);
      cc.dc.addDomainData(ConfigConstants.CAPABILITIES, generateUpdateList(userId));
      cc.dc.setAuthenticationTokenExpiry(model.atexp);
      cc.dc.setLocalLoginRequired(model.llr);
      SyncConfig
          syncCfg =
          generateSyncConfig(
              model); // Generate SyncConfig from the model and set it in domain config object.
      cc.dc.setSyncConfig(syncCfg);
      cc.dc.setTwoFactorAuthenticationEnabled(model.isTwoFactorAuthenticationEnabled());
      if (StringUtils.isNotEmpty(model.ro)) {
        CapabilityConfig cconf = new CapabilityConfig();
        cconf.setCapabilities(model.tm);
        if (model.hi != null) {
          cconf.setTagsInventory(StringUtils.join(model.hi, ','));
        }
        if (model.ho != null) {
          cconf.setTagsOrders(StringUtils.join(model.ho, ','));
        }
        cconf.setSendVendors(model.sv);
        cconf.setSendCustomers(model.sc);
        if (model.gcs != null) {
          cconf.setGeoCodingStrategy(model.gcs);
        }
        List<String> creatableEntityTypes = null;
        if (model.et != null && model.et.length > 0) {
          creatableEntityTypes = StringUtil.getList(model.et);
        }
        cconf.setCreatableEntityTypes(creatableEntityTypes);
        cconf.setAllowRouteTagEditing(model.er);
        cconf.setLoginAsReconnect(model.lr);
        cconf.setDisableShippingOnMobile(model.dshp);
        cconf.setBarcodingEnabled(model.bcs);
        cconf.setRFIDEnabled(model.rfids);
        cc.dc.setCapabilityByRole(model.ro, cconf);
        cconf.settagInvByOperation(configurationModelBuilder.getTagsByInventoryOperation(model));
      } else {
        cc.dc.setCapabilities(model.cap);
        cc.dc.setTransactionMenus(model.tm);
        if (model.hi != null) {
          cc.dc.setTagsInventory(TagUtil.getCleanTags(StringUtils.join(model.hi, ','), true));
        }
        if (model.ho != null) {
          cc.dc.setTagsOrders(TagUtil.getCleanTags(StringUtils.join(model.ho, ','), true));
        }
        cc.dc.setSendVendors(model.sv);
        cc.dc.setSendCustomers(model.sc);
        if (model.gcs != null) {
          cc.dc.setGeoCodingStrategy(model.gcs);
        }
        List<String> creatableEntityTypes = null;
        if (model.et != null && model.et.length > 0) {
          creatableEntityTypes = StringUtil.getList(model.et);
        }
        cc.dc.setCreatableEntityTypes(creatableEntityTypes);
        cc.dc.setAllowRouteTagEditing(model.er);
        cc.dc.setLoginAsReconnect(model.lr);
        cc.dc.setDisableShippingOnMobile(model.dshp);
        cc.dc.setBarcodingEnabled(model.bcs);
        cc.dc.setRFIDEnabled(model.rfids);
        cc.dc.setStoreAppTheme(model.getTheme());
        cc.dc.settagInvByOperation(configurationModelBuilder.getTagsByInventoryOperation(model));
      }
      saveDomainConfig(domainId, cc, backendMessages);
      xLogger.info("AUDITLOG \t {0} \t {1} \t CONFIGURATION \t " +
          "UPDATE CAPABILITIES", domainId, sUser.getUsername());
      xLogger.info(cc.dc.toJSONSring());
      return Arrays.asList(backendMessages.getString("capabilities.config.update.success"),
          LocalDateUtil.format(
              new Date(Long.parseLong(cc.dc.getDomainData(ConfigConstants.CAPABILITIES).get(1))),
              locale, timezone));
    } catch (ServiceException | ConfigurationException e) {
      xLogger.severe("Error in updating capabilities configuration", e);
      throw new InvalidServiceException(
          backendMessages.getString("capabilities.config.update.error"));
    }
  }

  @RequestMapping(value = "/inventory", method = RequestMethod.GET)
  public
  @ResponseBody
  InventoryConfigModel getInventoryConfiguration() {
    SecureUserDetails sUser = getUserDetails();
    Locale locale = sUser.getLocale();
    Long domainId = SecurityUtils.getCurrentDomainId();
    DomainConfig dc = DomainConfig.getInstance(domainId);
    ResourceBundle backendMessages = Resources.getBundle(locale);
    try {
      if (domainId == null) {
        xLogger.severe(backendMessages.getString(INVENTORY_CONFIG_FETCH_ERROR));
        throw new InvalidServiceException(
            backendMessages.getString(INVENTORY_CONFIG_FETCH_ERROR));
      }
      return configurationModelBuilder.buildInventoryConfigModel(dc, locale, sUser.getTimezone());
    } catch (ConfigurationException | ObjectNotFoundException e) {
      xLogger.severe(backendMessages.getString(INVENTORY_CONFIG_FETCH_ERROR), e);
      throw new InvalidServiceException(backendMessages.getString(INVENTORY_CONFIG_FETCH_ERROR));
    }
  }

  @RequestMapping(value = "/inventory/transReasons", method = RequestMethod.GET)
  public
  @ResponseBody
  Collection<String> getICTransactionReasons() {
    SecureUserDetails sUser = getUserDetails();
    Locale locale = sUser.getLocale();
    Long domainId = SecurityUtils.getCurrentDomainId();
    DomainConfig dc = DomainConfig.getInstance(domainId);
    ResourceBundle backendMessages = Resources.getBundle(locale);
    try {
      if (domainId == null) {
        xLogger.severe("Error in fetching Inventory configuration");
        throw new InvalidServiceException(
            backendMessages.getString(INVENTORY_CONFIG_FETCH_ERROR));
      }
      return configurationModelBuilder.buildUniqueTransactionReasons(dc.getInventoryConfig());
    } catch (Exception e) {
      xLogger.warn("Error in fetching reasons for transactions", e);
      return Collections.emptyList();
    }
  }

  @RequestMapping(value = "/getactualtrans", method = RequestMethod.GET)
  public
  @ResponseBody
  boolean getActualTransDateCheck() {
    SecureUserDetails sUser = getUserDetails();
    Locale locale = sUser.getLocale();
    Long domainId = SecurityUtils.getCurrentDomainId();
    boolean hasAtd;
    ResourceBundle backendMessages = Resources.getBundle(locale);
    if (domainId == null) {
      xLogger.severe("Error in fetching Inventory configuration");
      throw new InvalidServiceException(backendMessages.getString(INVENTORY_CONFIG_FETCH_ERROR));
    }
    DomainConfig dc = DomainConfig.getInstance(domainId);
    InventoryConfig ic = dc.getInventoryConfig();
    hasAtd =
        ic.getActualTransConfigByType(ITransaction.TYPE_ISSUE) != null && !ic
            .getActualTransConfigByType(ITransaction.TYPE_ISSUE).getTy().equals("0");
    if (!hasAtd) {
      hasAtd =
          ic.getActualTransConfigByType(ITransaction.TYPE_RECEIPT) != null && !ic
              .getActualTransConfigByType(ITransaction.TYPE_RECEIPT).getTy().equals("0");
    }
    if (!hasAtd) {
      hasAtd =
          ic.getActualTransConfigByType(ITransaction.TYPE_PHYSICALCOUNT) != null && !ic
              .getActualTransConfigByType(ITransaction.TYPE_PHYSICALCOUNT).getTy().equals("0");
    }
    if (!hasAtd) {
      hasAtd =
          ic.getActualTransConfigByType(ITransaction.TYPE_TRANSFER) != null && !ic
              .getActualTransConfigByType(ITransaction.TYPE_TRANSFER).getTy().equals("0");
    }
    if (!hasAtd) {
      hasAtd =
          ic.getActualTransConfigByType(ITransaction.TYPE_WASTAGE) != null && !ic
              .getActualTransConfigByType(ITransaction.TYPE_WASTAGE).getTy().equals("0");
    }
    return hasAtd;
  }

  @RequestMapping(value = "/inventory", method = RequestMethod.POST)
  public
  @ResponseBody
  String updateInventoryConfig(@RequestBody InventoryConfigModel model) {
    SecureUserDetails sUser = getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    if (!GenericAuthoriser.authoriseAdmin()) {
      throw new ForbiddenAccessException(backendMessages.getString(PERMISSION_DENIED));
    }
    String userId = sUser.getUsername();
    Long domainId = SecurityUtils.getCurrentDomainId();
    try {
      if (domainId == null) {
        xLogger.severe(backendMessages.getString(INVENTORY_CONFIG_UPDATE_ERROR));
        throw new InvalidServiceException(
            backendMessages.getString(INVENTORY_CONFIG_UPDATE_ERROR));
      }
      ConfigContainer cc = getDomainConfig(domainId, userId);

      cc.dc.addDomainData(ConfigConstants.INVENTORY, generateUpdateList(userId));
      //Get transaction export schedules
      String et = model.et;

      // Form object
      InventoryConfig inventoryConfig = new InventoryConfig();
      inventoryConfig.setTransReasons(configurationModelBuilder.buildReasonConfigByTransType(model));

      //reset the wastage reasons
      cc.dc.setWastageReasons(null);
      boolean cimt = model.cimt;
      if (cimt) {
        inventoryConfig.setCimt(true);
        inventoryConfig.setImtransreasons(configurationModelBuilder.buildReasonConfigByTagMap(
            model.imt));
      }
      boolean crmt = model.crmt;
      if (crmt) {
        inventoryConfig.setCrmt(true);
        inventoryConfig.setRmtransreasons(configurationModelBuilder.buildReasonConfigByTagMap(
            model.rmt));
      }
      boolean csmt = model.csmt;
      if (csmt) {
        inventoryConfig.setCsmt(true);
        inventoryConfig.setSmtransreasons(configurationModelBuilder.buildReasonConfigByTagMap(
            model.smt));
      }
      boolean ctmt = model.ctmt;
      if (ctmt) {
        inventoryConfig.setCtmt(true);
        inventoryConfig.setTmtransreasons(configurationModelBuilder.buildReasonConfigByTagMap(
            model.tmt));
      }
      boolean cdmt = model.cdmt;
      if (cdmt) {
        inventoryConfig.setCdmt(true);
        inventoryConfig.setDmtransreasons(configurationModelBuilder.buildReasonConfigByTagMap(
            model.dmt));
          }
      boolean crimt = model.crimt;
      if (crimt) {
        inventoryConfig.setCrimt(true);
        inventoryConfig.setMtagRetIncRsns(configurationModelBuilder.buildReasonConfigByTagMap(
            model.rimt));
      }
      boolean cromt = model.cromt;
      if (cromt) {
        inventoryConfig.setCromt(true);
        inventoryConfig.setMtagRetOutRsns(configurationModelBuilder.buildReasonConfigByTagMap(model.romt));
      }
      inventoryConfig.setTransactionTypesWithReasonMandatory(model.getTransactionTypesWithReasonMandatory());
      inventoryConfig.setMatStatusConfigByType(ITransaction.TYPE_ISSUE, configurationModelBuilder.buildMatStatusConfig(
          model.idf, model.iestm, model.ism));
      inventoryConfig.setMatStatusConfigByType(ITransaction.TYPE_RECEIPT, configurationModelBuilder.buildMatStatusConfig(
          model.rdf, model.restm, model.rsm));
      inventoryConfig.setMatStatusConfigByType(ITransaction.TYPE_TRANSFER, configurationModelBuilder.buildMatStatusConfig(
          model.tdf, model.testm, model.tsm));
      inventoryConfig.setMatStatusConfigByType(ITransaction.TYPE_PHYSICALCOUNT, configurationModelBuilder.buildMatStatusConfig(
          model.pdf, model.pestm, model.psm));
      inventoryConfig.setMatStatusConfigByType(ITransaction.TYPE_WASTAGE, configurationModelBuilder.buildMatStatusConfig(
          model.wdf, model.westm, model.wsm));
      inventoryConfig.setMatStatusConfigByType(ITransaction.TYPE_RETURNS_INCOMING, configurationModelBuilder.buildMatStatusConfig(
          model.ridf, model.riestm, model.rism));
      inventoryConfig.setMatStatusConfigByType(ITransaction.TYPE_RETURNS_OUTGOING, configurationModelBuilder.buildMatStatusConfig(
          model.rodf, model.roestm, model.rosm));

      //Actual date of transaction configuration
      inventoryConfig.setActualTransDateByType(ITransaction.TYPE_ISSUE, configurationModelBuilder.buildActualTransConfig(model.catdi));
      inventoryConfig.setActualTransDateByType(ITransaction.TYPE_RECEIPT, configurationModelBuilder.buildActualTransConfig(
          model.catdr));
      inventoryConfig.setActualTransDateByType(ITransaction.TYPE_PHYSICALCOUNT, configurationModelBuilder.buildActualTransConfig(
          model.catdp));
      inventoryConfig.setActualTransDateByType(ITransaction.TYPE_WASTAGE, configurationModelBuilder.buildActualTransConfig(
          model.catdw));
      inventoryConfig.setActualTransDateByType(ITransaction.TYPE_TRANSFER, configurationModelBuilder.buildActualTransConfig(
          model.catdt));
      inventoryConfig.setActualTransDateByType(ITransaction.TYPE_RETURNS_INCOMING, configurationModelBuilder.buildActualTransConfig(
          model.catdri));
      inventoryConfig.setActualTransDateByType(ITransaction.TYPE_RETURNS_OUTGOING, configurationModelBuilder
          .buildActualTransConfig(model.catdro));

      if (et != null && !et.trim().isEmpty()) {
        List<String> times = StringUtil.getList(et.trim());
        List<String> utcTimes = null;
        if (times != null && !times.isEmpty()) {
          utcTimes = LocalDateUtil.convertTimeStringList(times, cc.dc.getTimezone(), true);
        }
        inventoryConfig.setTimes(utcTimes);
      } else {
        inventoryConfig.setTimes(null);
      }
      if (userId != null && !userId.isEmpty()) {
        inventoryConfig.setSourceUserId(userId);
      }
      //Get the parameters for manual consumption rates
      inventoryConfig.setConsumptionRate(Integer.parseInt(model.crc));
      if (String.valueOf(InventoryConfig.CR_MANUAL).equals(model.crc)) {
        inventoryConfig.setManualCRFreq(model.mcrfreq);
      }
      inventoryConfig.setDisplayCR(model.dispcr);
      if (model.dispcr) {
        inventoryConfig.setDisplayCRFreq(model.dcrfreq);
      }
      inventoryConfig.setShowPredictions(model.showpr);
      InventoryConfig.ManualTransConfig manualTransConfig = new InventoryConfig.ManualTransConfig();
      manualTransConfig.enableManualUploadInvDataAndTrans = model.emuidt;
      manualTransConfig.enableUploadPerEntityOnly = model.euse;
      inventoryConfig.setShowInventoryDashboard(model.eidb);
      if (model.eidb && model.enTgs != null && !model.enTgs.isEmpty()) {
        inventoryConfig.setEnTags(model.enTgs);
      }
      inventoryConfig.setManualTransConfig(manualTransConfig);
      if (model.etdx) {
        inventoryConfig.setEnabled(true);
        if (StringUtils.isNotEmpty(model.an)) {
          inventoryConfig.setExportUsers(model.an);
        }
        if (CollectionUtils.isNotEmpty(model.usrTgs)) {
          inventoryConfig.setUserTags(model.usrTgs);
        }
      }
      // Update permissions
      InventoryConfig.Permissions perms = new InventoryConfig.Permissions();
      perms.invCustomersVisible = model.ivc;
      inventoryConfig.setPermissions(perms);

      inventoryConfig.setMinMaxType(model.mmType);
      if (model.mmType == InventoryConfig.MIN_MAX_ABS_QTY) {
        inventoryConfig.setMinMaxDur(null);
        inventoryConfig.setMinMaxFreq(null);
      } else {
        inventoryConfig.setMinMaxDur(model.mmDur);
        inventoryConfig.setMinMaxFreq(model.mmFreq);
      }
      // Get the optimization config
      OptimizerConfig oc = cc.dc.getOptimizerConfig();
      String computeOption = model.co;
      int coption = OptimizerConfig.COMPUTE_NONE;
      if (computeOption != null && !computeOption.isEmpty()) {
        coption = Integer.parseInt(computeOption);
      }
      oc.setCompute(coption);
      oc.setComputeFrequency(model.crfreq);
      oc.setExcludeDiscards(model.edis);
      oc.setExcludeReasons(StringUtil.getCSV(model.ersns));
      oc.setExcludeReturnIncomingReasons(StringUtil.getCSV(model.erirsns));

      if (String.valueOf(InventoryConfig.CR_AUTOMATIC).equals(model.crc)) {
        if (StringUtils.isNotEmpty(model.minhpccr)) {
          oc.setMinHistoricalPeriod(Float.parseFloat(model.minhpccr));
        }
        oc.setMaxHistoricalPeriod(Float.parseFloat(model.maxhpccr));
        // Get parameters for demand forecasting
        if (coption == OptimizerConfig.COMPUTE_FORECASTEDDEMAND) {
          String avgPeriodicity = model.aopfd;
          if (avgPeriodicity != null && !avgPeriodicity.isEmpty()) {
            try {
              oc.setMinAvgOrderPeriodicity(Float.parseFloat(avgPeriodicity));
            } catch (NumberFormatException e) {
              xLogger.warn("Invalid avg. periodicity for Demand Forecasting: {0}", avgPeriodicity);
            }
          }
          String numPeriods = model.nopfd;
          if (numPeriods != null && !numPeriods.isEmpty()) {
            try {
              oc.setNumPeriods(Float.parseFloat(numPeriods));
            } catch (NumberFormatException e) {
              xLogger
                  .warn("Invalid number of order periods for Demand Forecasting: {0}",
                      numPeriods);
            }
          }
        }
        // Get parameters for EOQ computation
        if (coption == OptimizerConfig.COMPUTE_EOQ) {
          oc.setInventoryModel(model.im);
          String avgPeriodicity = model.aopeoq;
          if (avgPeriodicity != null && !avgPeriodicity.isEmpty()) {
            try {
              oc.setMinAvgOrderPeriodicity(Float.parseFloat(avgPeriodicity));
            } catch (NumberFormatException e) {
              xLogger.warn("Invalid avg. periodicity for EOQ: {0}", avgPeriodicity);
            }
          }
          String numPeriods = model.nopeoq;
          if (numPeriods != null && !numPeriods.isEmpty()) {
            try {
              oc.setNumPeriods(Float.parseFloat(numPeriods));
            } catch (NumberFormatException e) {
              xLogger.warn("Invalid number of order periods for EOQ: {0}", numPeriods);
            }
          }
          String leadTime = model.lt;
          if (leadTime != null && !leadTime.isEmpty()) {
            try {
              oc.setLeadTimeDefault(Float.parseFloat(leadTime));
            } catch (NumberFormatException e) {
              xLogger.warn("Invalid lead time for EOQ: {0}", leadTime);
            }
          }
          InventoryConfigModel.LeadTimeAvgConfigModel leadTimeAvgConfigModel = model.ltacm;
          if (leadTimeAvgConfigModel != null) {
            LeadTimeAvgConfig leadTimeAvgConfig = oc.getLeadTimeAvgCfg();
            leadTimeAvgConfig.setMaxNumOfOrders(leadTimeAvgConfigModel.maxo);
            leadTimeAvgConfig.setMinNumOfOrders(leadTimeAvgConfigModel.mino);
            leadTimeAvgConfig.setMaxOrderPeriods(leadTimeAvgConfigModel.maxop);
            leadTimeAvgConfig.setExcludeOrderProcTime(leadTimeAvgConfigModel.exopt);
          }
        }
      }
      oc.setDisplayDF(model.ddf);
      oc.setDisplayOOQ(model.dooq);
      cc.dc.setOptimizerConfig(oc);
      inventoryConfig.setReturnsConfig(configurationModelBuilder.buildReturnsConfigs(model.rcm));
      cc.dc.setInventoryConfig(inventoryConfig);
      saveDomainConfig(domainId, cc, backendMessages);
      xLogger.info("AUDITLOG \t {0} \t {1} \t CONFIGURATION \t " +
          "UPDATE INVENTORY", domainId, sUser.getUsername());
      xLogger.info(cc.dc.toJSONSring());
    } catch (ServiceException | ObjectNotFoundException e) {
      xLogger.severe(backendMessages.getString(INVENTORY_CONFIG_UPDATE_ERROR));
      throw new InvalidServiceException(backendMessages.getString(INVENTORY_CONFIG_UPDATE_ERROR));
    } catch (ConfigurationException e) {
      xLogger.severe(backendMessages.getString(INVENTORY_CONFIG_UPDATE_ERROR));
    }
    return backendMessages.getString("inventory.config.update.success");
  }

  @RequestMapping(value = "/orders", method = RequestMethod.GET)
  public
  @ResponseBody
  OrdersConfigModel getOrdersConfig(HttpServletRequest request) {
    SecureUserDetails sUser = getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    Long domainId = SecurityUtils.getCurrentDomainId();
    try {
      if (domainId == null) {
        xLogger.severe(backendMessages.getString(INVENTORY_CONFIG_FETCH_ERROR));
        throw new InvalidServiceException(
            backendMessages.getString(INVENTORY_CONFIG_FETCH_ERROR));
      }
      return configurationModelBuilder.buildOrderConfigModel(request, domainId, locale, sUser.getTimezone());
    } catch (ConfigurationException | ObjectNotFoundException | UnsupportedEncodingException e) {
      xLogger.severe(backendMessages.getString(INVENTORY_CONFIG_FETCH_ERROR), e);
      throw new InvalidServiceException(backendMessages.getString(INVENTORY_CONFIG_FETCH_ERROR));
    }
  }

  @RequestMapping(value = "/orders", method = RequestMethod.POST)
  public
  @ResponseBody
  String updateOrdersConfig(@RequestBody OrdersConfigModel model) {
    SecureUserDetails sUser = getUserDetails();
    Locale locale = sUser.getLocale();
    BlobstoreService blobstoreService = AppFactory.get().getBlobstoreService();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    if (!GenericAuthoriser.authoriseAdmin()) {
      throw new ForbiddenAccessException(backendMessages.getString(PERMISSION_DENIED));
    }
    String userId = sUser.getUsername();
    Long domainId = SecurityUtils.getCurrentDomainId();
    try {
      if (domainId == null) {
        xLogger.severe(backendMessages.getString(ORDERS_CONFIG_UPDATE_ERROR));
        throw new InvalidServiceException(backendMessages.getString(ORDERS_CONFIG_UPDATE_ERROR));
      }
      ConfigContainer cc = getDomainConfig(domainId, userId);
      DemandBoardConfig dbc = cc.dc.getDemandBoardConfig();
      if (dbc == null) {
        dbc = new DemandBoardConfig();
        cc.dc.setDemandBoardConfig(dbc);
      }
      OrdersConfig oc = cc.dc.getOrdersConfig();
      if (oc == null) {
        oc = new OrdersConfig();
        cc.dc.setOrdersConfig(oc);
      }
      if (model == null) {
        xLogger.severe(backendMessages.getString(ORDERS_CONFIG_UPDATE_ERROR));
        throw new ConfigurationServiceException(
            backendMessages.getString(ORDERS_CONFIG_UPDATE_ERROR));
      }
      cc.dc.addDomainData(ConfigConstants.ORDERS, generateUpdateList(userId));
      cc.dc.setOrderGeneration(model.og);
      cc.dc.setAutoGI(model.agi);
      cc.dc.setAutoGR(model.agr);
      cc.dc.setTransporterMandatory(model.tm);
      cc.dc.setTransporterInStatusSms(model.tiss);
      cc.dc.setAllowEmptyOrders(model.ao);
      cc.dc.setAllowMarkOrderAsFulfilled(model.aof);
      if (StringUtils.isNotBlank(model.po)) {
        cc.dc.setPaymentOptions(model.po);
      } else {
        cc.dc.setPaymentOptions(null);
      }
      if (StringUtils.isNotBlank(model.ps)) {
        cc.dc.setPackageSizes(model.ps);
      } else {
        cc.dc.setPackageSizes(null);
      }
      if (StringUtils.isNotBlank(model.vid)) {
        cc.dc.setVendorid(Long.valueOf(model.vid));
      } else {
        cc.dc.setVendorid(null);
      }
      cc.dc.setDisableOrdersPricing(model.dop);
      oc.setExportEnabled(model.eex);
      oc.setReasonsEnabled(model.enOrdRsn);
      oc.setOrderReason(model.enOrdRsn ? StringUtil.trimCSV(model.orsn) : null);
      if (model.enOrdRsn) {
        oc.setMandatory(model.md);
      }
      oc.setAllowMarkOrderAsConfirmed(model.aoc);
      oc.setAllocateStockOnConfirmation(model.asc);
      oc.setTransferRelease(model.tr);
      oc.setOrderRecommendationReasons(model.orr);
      oc.setOrderRecommendationReasonsMandatory(model.orrm);
      oc.setEditingQuantityReasons(model.eqr);
      oc.setEditingQuantityReasonsMandatory(model.eqrm);
      oc.setPartialShipmentReasons(model.psr);
      oc.setPartialShipmentReasonsMandatory(model.psrm);
      oc.setPartialFulfillmentReasons(model.pfr);
      oc.setPartialFulfillmentReasonsMandatory(model.pfrm);
      oc.setCancellingOrderReasons(model.cor);
      oc.setCancellingOrderReasonsMandatory(model.corm);
      oc.setCreationAutomated(model.autoCreate);
      oc.setAutoCreateOnMin(model.autoCreateOnMin);
      oc.setAutoCreateMaterialTags(model.autoCreateMaterialTags);
      oc.setAutoCreatePdos(model.pdos);
      oc.setAutoCreateEntityTags(model.autoCreateEntityTags);
      oc.setAutoAssignFirstMaterialStatus(model.aafmsc);
      oc.setReferenceIdMandatory(model.isReferenceIdMandatory());
      oc.setPurchaseReferenceIdMandatory(model.isPurchaseReferenceIdMandatory());
      oc.setTransferReferenceIdMandatory(model.isTransferReferenceIdMandatory());
      oc.setExpectedArrivalDateMandatory(model.isExpectedArrivalDateMandatory());
      oc.setMarkOrderAsShippedOnPickup(model.getMarkShippedOnPickup());
      oc.setMarkOrderAsFulfilledOnDelivery(model.getMarkFulfilledOnDelivery());
      oc.setDeliveryRequestByCustomerDisabled(model.getDisableDeliveryRequestByCustomer());
      if (model.getLogo() != null) {
        if (oc.getInvoiceLogo() != null && !oc.getInvoiceLogo().equals(model.getLogo())) {
          blobstoreService.remove(oc.getInvoiceLogo());

        }
        oc.setInvoiceLogo(model.getLogo());
        oc.setInvoiceLogoName(model.getLogoName());
      }
      if (model.getInvoiceTemplate() != null) {
        if (oc.getInvoiceTemplate() != null && !oc.getInvoiceTemplate()
            .equals(model.getInvoiceTemplate())) {
          blobstoreService.remove(oc.getInvoiceTemplate());
        }
        oc.setInvoiceTemplate(model.getInvoiceTemplate());
        oc.setInvoiceTemplateName(model.getInvoiceTemplateName());
      }
      if (model.getShipmentTemplate() != null) {
        if (oc.getShipmentTemplate() != null && !oc.getShipmentTemplate()
            .equals(model.getShipmentTemplate())) {
          blobstoreService.remove(oc.getShipmentTemplate());
        }
        oc.setShipmentTemplate(model.getShipmentTemplate());
        oc.setShipmentTemplateName(model.getShipmentTemplateName());
      }

      if (model.et != null && !model.et.trim().isEmpty()) {
        List<String> times = StringUtil.getList(model.et.trim());
        List<String> utcTimes = null;
        if (times != null && !times.isEmpty()) {
          utcTimes =
              LocalDateUtil.convertTimeStringList(times, cc.dc.getTimezone(),
                  true); // Convert from domain specific timezone to UTC
        }
        oc.setExportTimes(StringUtil.getCSV(utcTimes));
      } else {
        oc.setExportTimes(null);
      }
      oc.setExportUserIds(model.an);
      if (CollectionUtils.isNotEmpty(model.usrTgs)) {
        oc.setUserTags(model.usrTgs);
      } else {
        oc.setUserTags(null);
      }
      oc.setSourceUserId(userId);
      if ("p".equalsIgnoreCase(model.ip)) {
        dbc.setIsPublic(true);
      } else if ("r".equalsIgnoreCase(model.ip)) {
        dbc.setIsPublic(false);
      }
      dbc.setBanner(model.bn);
      dbc.setHeading(model.hd);
      dbc.setCopyright(model.cp);
      dbc.setShowStock(model.spb);
      cc.dc.setDemandBoardConfig(dbc);
      cc.dc.setOrdersConfig(oc);
      saveDomainConfig(domainId, cc, backendMessages);
      xLogger.info("AUDITLOG \t {0} \t {1} \t CONFIGURATION \t " +
          "UPDATE ORDERS", domainId, sUser.getUsername());
      xLogger.info(cc.dc.toJSONSring());
    } catch (ServiceException | ConfigurationException e) {
      xLogger.severe("Error in updating Orders configuration", e);
      throw new InvalidServiceException(backendMessages.getString(ORDERS_CONFIG_UPDATE_ERROR));
    }
    return backendMessages.getString("orders.config.update.success");
  }

  @RequestMapping(value = "/notifications", method = RequestMethod.POST)
  public
  @ResponseBody
  String updateNotificationsConfig(@RequestBody NotificationsConfigModel model) {
    SecureUserDetails sUser = getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    if (!GenericAuthoriser.authoriseAdmin()) {
      throw new ForbiddenAccessException(backendMessages.getString(PERMISSION_DENIED));
    }
    String userId = sUser.getUsername();
    Long domainId = SecurityUtils.getCurrentDomainId();
    try {
      if (domainId == null) {
        xLogger.severe(backendMessages.getString(NOTIFICATION_CONFIG_UPDATE_ERROR));
        throw new InvalidServiceException(backendMessages.getString(NOTIFICATION_CONFIG_UPDATE_ERROR));
      }
      EventsConfig ec = getEventsConfig(model, domainId, backendMessages);
      ConfigContainer cc = getDomainConfig(domainId, userId);
      try {
        xLogger.info("ec: {0}", ec.toJSONString());
      } catch (JSONException je) {
        // do nothing
      }
      cc.dc.setEventsConfig(ec);
      cc.dc.addDomainData(ConfigConstants.NOTIFICATIONS, generateUpdateList(userId));
      saveDomainConfig(domainId, cc, backendMessages);
      xLogger.info("AUDITLOG \t {0} \t {1} \t CONFIGURATION \t " +
          "UPDATE NOTIFICATIONS", domainId, sUser.getUsername());
      xLogger.info(cc.dc.toJSONSring());
    } catch (ServiceException | ConfigurationException e) {
      xLogger.severe(backendMessages.getString(NOTIFICATION_CONFIG_UPDATE_ERROR), e);
      throw new InvalidServiceException(backendMessages.getString(NOTIFICATION_CONFIG_UPDATE_ERROR));
    }
    if (model.add) {
      return backendMessages.getString("notif.config.create.success");
    } else {
      return backendMessages.getString("notif.config.update.success");
    }
  }

  private EventsConfig getEventsConfig(NotificationsConfigModel model, Long domainId,
                                       ResourceBundle backendMessages) throws ServiceException {
    DomainConfig dc = DomainConfig.getInstance(domainId);
    EventsConfig ec = dc.getEventsConfig();
    String eventSpecJson = null;
    try {
      eventSpecJson = ec.toJSONString();
    } catch (JSONException ignored) {
      // do nothing
    }
    try {
      String json = notificationBuilder.buildModel(model, eventSpecJson);
      if (json == null || json.isEmpty()) {
        xLogger.severe(backendMessages.getString(NOTIFICATION_CONFIG_UPDATE_ERROR));
        throw new ConfigurationServiceException(
            backendMessages.getString(NOTIFICATION_CONFIG_UPDATE_ERROR));
      }

      ec = new EventsConfig(json);
    } catch (JSONException e) {
      xLogger.severe(backendMessages.getString(NOTIFICATION_CONFIG_UPDATE_ERROR), e);
      throw new ConfigurationServiceException(
          backendMessages.getString(NOTIFICATION_CONFIG_UPDATE_ERROR));
    }
    return ec;
  }

  @RequestMapping(value = "/notifications/fetch", method = RequestMethod.GET)
  public
  @ResponseBody
  NotificationsModel getNotificationsConfig(@RequestParam String t) {
    SecureUserDetails sUser = getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    Long domainId = SecurityUtils.getCurrentDomainId();
    DomainConfig dc = DomainConfig.getInstance(domainId);
    if (dc == null) {
      xLogger.severe(backendMessages.getString(NOTIFICATION_CONFIG_FETCH_ERROR));
      throw new ConfigurationServiceException(
          backendMessages.getString(NOTIFICATION_CONFIG_FETCH_ERROR));
    }
    try {
      EventsConfig ec = dc.getEventsConfig();
      if (ec == null) {
        xLogger.severe(backendMessages.getString(NOTIFICATION_CONFIG_FETCH_ERROR));
        throw new ConfigurationServiceException(
            backendMessages.getString(NOTIFICATION_CONFIG_FETCH_ERROR));
      }
      return notificationBuilder.buildNotifConfigModel(ec.toJSONString(), t, domainId, locale, sUser.getTimezone());
    } catch (ServiceException | JSONException | ObjectNotFoundException e) {
      xLogger.severe(backendMessages.getString(NOTIFICATION_CONFIG_FETCH_ERROR), e);
      throw new ConfigurationServiceException(
          backendMessages.getString(NOTIFICATION_CONFIG_FETCH_ERROR));
    }
  }

  @RequestMapping(value = "/notifications/delete", method = RequestMethod.POST)
  public
  @ResponseBody
  String deleteNotification(@RequestBody NotificationsConfigModel model) {
    SecureUserDetails sUser = getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    if (!GenericAuthoriser.authoriseAdmin()) {
      throw new ForbiddenAccessException(backendMessages.getString(PERMISSION_DENIED));
    }
    String userId = sUser.getUsername();
    Long domainId = SecurityUtils.getCurrentDomainId();
    String key = IConfig.CONFIG_PREFIX + domainId;
    IConfig c;
    DomainConfig dc;
    EventsConfig ec;
    String eventSpecJson = null;
    boolean delete = false;
    if (domainId == null) {
      xLogger.severe(backendMessages.getString(NOTIFICATION_CONFIG_DELETE_ERROR));
      throw new InvalidServiceException(backendMessages.getString(NOTIFICATION_CONFIG_DELETE_ERROR));
    }
    try {
      try {
        c = configurationMgmtService.getConfiguration(key);
        dc = new DomainConfig(c.getConfig());
      } catch (ObjectNotFoundException e) {
        dc = new DomainConfig();
        c = JDOUtils.createInstance(IConfig.class);
        c.setKey(key);
        c.setUserId(userId);
        c.setDomainId(domainId);
        c.setLastUpdated(new Date());
        delete = true;
      } catch (ConfigurationException e) {
        xLogger.severe(backendMessages.getString(NOTIFICATION_CONFIG_DELETE_ERROR), e);
        throw new InvalidServiceException(backendMessages.getString(NOTIFICATION_CONFIG_DELETE_ERROR));
      }
      try {
        dc = DomainConfig.getInstance(domainId);
        ec = dc.getEventsConfig();
        eventSpecJson = ec.toJSONString();
      } catch (JSONException e) {
        xLogger.severe(backendMessages.getString(NOTIFICATION_CONFIG_DELETE_ERROR), e);
      }
      String json = notificationBuilder.deleteModel(model, eventSpecJson);

      if (json == null || json.isEmpty()) {
        xLogger.severe(backendMessages.getString(NOTIFICATION_CONFIG_DELETE_ERROR));
        throw new InvalidServiceException(backendMessages.getString(NOTIFICATION_CONFIG_DELETE_ERROR));
      }
      try {
        ec = new EventsConfig(json);
      } catch (JSONException e) {
        xLogger.severe(backendMessages.getString(NOTIFICATION_CONFIG_DELETE_ERROR), e);
        throw new InvalidServiceException(backendMessages.getString(NOTIFICATION_CONFIG_DELETE_ERROR));
      }
      dc.setEventsConfig(ec);
      ConfigContainer cc = new ConfigContainer();
      cc.add = delete;
      cc.c = c;
      cc.dc = dc;
      saveDomainConfig(domainId, cc, backendMessages);
      xLogger.info("AUDITLOG \t {0} \t {1} \t CONFIGURATION \t " +
          "DELETE NOTIFICATION", domainId, sUser.getUsername());
    } catch (ServiceException e) {
      xLogger.severe(backendMessages.getString(NOTIFICATION_CONFIG_DELETE_ERROR), e);
      throw new InvalidServiceException(backendMessages.getString(NOTIFICATION_CONFIG_DELETE_ERROR));
    }
    return backendMessages.getString("notif.delete.success");
  }

  @RequestMapping(value = "/notifcations/messages", method = RequestMethod.GET)
  public
  @ResponseBody
  Results getUsersMessageStatus(
      @RequestParam(defaultValue = PageParams.DEFAULT_OFFSET_STR) int offset,
      @RequestParam(defaultValue = PageParams.DEFAULT_SIZE_STR) int size,
      @RequestParam(required = false, defaultValue = "") String start,
      @RequestParam(required = false, defaultValue = "") String end,
      HttpServletRequest request) {

    SecureUserDetails sUser = getUserDetails();
    String userId = sUser.getUsername();
    Long domainId = SecurityUtils.getCurrentDomainId();
    DomainConfig dc = DomainConfig.getInstance(domainId);
    Navigator navigator =
        new Navigator(request.getSession(), "UsersController.getUsersMessageStatus", offset, size,
            "dummy", 0);
    PageParams pageParams = new PageParams(navigator.getCursor(offset), offset, size);
    Results results;
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    if (!GenericAuthoriser.authoriseAdmin()) {
      throw new ForbiddenAccessException(backendMessages.getString(PERMISSION_DENIED));
    }
    try {
      Date startDate = null;
      Date endDate = null;
      if (StringUtils.isNotEmpty(start)) {
        try {
          startDate = LocalDateUtil.parseCustom(start, Constants.DATE_FORMAT, dc.getTimezone());
        } catch (Exception e) {
          xLogger.warn("Exception when parsing start date " + start, e);
        }
      }
      if (StringUtils.isNotEmpty(end)) {
        try {
          endDate = LocalDateUtil.parseCustom(end, Constants.DATE_FORMAT, dc.getTimezone());
        } catch (Exception e) {
          xLogger.warn("Exception when parsing start date " + end, e);
        }
      }

      results = MessageUtil.getNotifactionLogs(domainId, startDate, endDate, pageParams);
      navigator.setResultParams(results);
    } catch (MessageHandlingException e) {
      xLogger.warn("Error in building message status", e);
      throw new InvalidServiceException(
          backendMessages.getString("message.status.build.error"));
    }
    String timezone = sUser.getTimezone();
    int no = offset;
    List<UserMessageModel> userMessageStatus = new ArrayList<>();
    for (Object res : results.getResults()) {
      IMessageLog ml = (IMessageLog) res;
      try {
        userMessageStatus.add(userMessageBuilder
            .buildUserMessageModel(ml, locale, userId, ++no, timezone));
      } catch (Exception e) {
        xLogger.warn("Error in building message status", e);
      }
    }
    return new Results<>(userMessageStatus, results.getCursor(), -1, offset);
  }

  @RequestMapping(value = "/bulletinboard", method = RequestMethod.POST)
  public
  @ResponseBody
  String updateBulletinBoardConfig(@RequestBody BulletinBoardConfigModel model) {
    SecureUserDetails sUser = getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    if (!GenericAuthoriser.authoriseAdmin()) {
      throw new ForbiddenAccessException(backendMessages.getString(PERMISSION_DENIED));
    }
    if (model == null) {
      xLogger.severe(backendMessages.getString(BULLETIN_BOARD_CONFIG_UPDATE_ERROR));
      throw new BadRequestException(backendMessages.getString(BULLETIN_BOARD_CONFIG_UPDATE_ERROR));
    }
    String userId = sUser.getUsername();
    Long domainId = SecurityUtils.getCurrentDomainId();
    if (domainId == null) {
      xLogger.severe(backendMessages.getString(BULLETIN_BOARD_CONFIG_UPDATE_ERROR));
      throw new InvalidServiceException(backendMessages.getString(BULLETIN_BOARD_CONFIG_UPDATE_ERROR));
    }
    try {
      ConfigContainer cc = getDomainConfig(domainId, userId);
      cc.dc.addDomainData(ConfigConstants.BULLETIN_BOARD, generateUpdateList(userId));
      BBoardConfig bbc = bulletinBoardBuilder.buildBBoardConfig(model);
      if (bbc != null) {
        cc.dc.setBBoardConfig(bbc);
      }
      saveDomainConfig(domainId, cc, backendMessages);
      xLogger.info("AUDITLOG \t {0} \t {1} \t CONFIGURATION \t " +
          "UPDATE BULLETINBOARD", domainId, sUser.getUsername());
      xLogger.info(cc.dc.toJSONSring());
    } catch (ServiceException | ConfigurationException e) {
      xLogger.severe(backendMessages.getString(BULLETIN_BOARD_CONFIG_UPDATE_ERROR), e);
      throw new InvalidServiceException(backendMessages.getString(BULLETIN_BOARD_CONFIG_UPDATE_ERROR));
    }
    return backendMessages.getString("bulletin.config.update.success");
  }

  @RequestMapping(value = "/bulletinboard", method = RequestMethod.GET)
  public
  @ResponseBody
  BulletinBoardConfigModel getBulletinBoardConfig() {
    SecureUserDetails sUser = getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    Long domainId = SecurityUtils.getCurrentDomainId();
    DomainConfig dc = DomainConfig.getInstance(domainId);
    if (dc == null) {
      xLogger.severe("Error in fetching bulletin board configuration");
      throw new ConfigurationServiceException(
          backendMessages.getString("bulletin.config.fetch.error"));
    }
    BBoardConfig config = dc.getBBoardConfig();
    BulletinBoardConfigModel model = new BulletinBoardConfigModel();
    if (config != null) {
      model = bulletinBoardBuilder.buildModel(config);
    }
    model.did = Long.toString(domainId);
    List<String> val = dc.getDomainData(ConfigConstants.BULLETIN_BOARD);
    if (val != null) {
      model.createdBy = val.get(0);
      model.lastUpdated =
          LocalDateUtil.format(new Date(Long.parseLong(val.get(1))), locale, sUser.getTimezone());
      try {
        model.fn = String.valueOf(usersService.getUserAccount(model.createdBy).getFullName());
      } catch (ObjectNotFoundException e) {
        //Ignore.. Users should still be able to edit config
      }
    }
    return model;
  }

  @RequestMapping(value = "/posttoboard", method = RequestMethod.POST)
  public
  @ResponseBody
  String setMessageToBoard(@RequestBody String msg) {
    SecureUserDetails sUser = getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    if (!GenericAuthoriser.authoriseAdmin()) {
      throw new ForbiddenAccessException(backendMessages.getString(PERMISSION_DENIED));
    }
    String userId = sUser.getUsername();
    Long domainId = SecurityUtils.getCurrentDomainId();
    if (domainId == null) {
      xLogger.severe("Error in posting message to board");
      throw new InvalidServiceException(backendMessages.getString("message.post.error"));
    }
    IBBoard board = JDOUtils.createInstance(IBBoard.class);
    board.setDomainId(domainId);
    board.setUserId(userId);
    board.setTimestamp(new Date());
    board.setType(IBBoard.TYPE_POST);
    if (StringUtils.isNotBlank(msg)) {
      board.setMessage(msg);
    }
    BBHandler.add(board);
    xLogger.info("AUDITLOG \t {0} \t {1} \t CONFIGURATION \t " +
        "SET MESSAGE BOARD", domainId, sUser.getUsername());
    return backendMessages.getString("message.post.success");
  }

  @RequestMapping(value = "/accesslogs", method = RequestMethod.GET)
  public
  @ResponseBody
  Results getAccessLogs(@RequestParam(required = false) String duration,
                        @RequestParam(defaultValue = "1") int o,
                        @RequestParam(defaultValue = PageParams.DEFAULT_SIZE_STR) int s,
                        HttpServletRequest request) {
    SecureUserDetails sUser = getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    if (!GenericAuthoriser.authoriseAdmin()) {
      throw new ForbiddenAccessException(backendMessages.getString(PERMISSION_DENIED));
    }
    Long domainId = SecurityUtils.getCurrentDomainId();
    if (domainId == null) {
      xLogger.severe("Error in fetching Access log");
      throw new InvalidServiceException(backendMessages.getString("accesslog.fetch.error"));
    }
    Date start = null;
    if (duration != null && !duration.isEmpty()) {
      Calendar cal = LocalDateUtil.getZeroTime(DomainConfig.getInstance(domainId).getTimezone());
      cal.add(Calendar.DATE, -1 * Integer.parseInt(duration));
      start = cal.getTime();
    }
    Navigator navigator =
        new Navigator(request.getSession(), "DomainConfigController.getAccessLogs", o, s, "dummy",
            0);
    PageParams pageParams = new PageParams(navigator.getCursor(o), o, s);
    Results results = XLog.getRequestLogs(domainId, IALog.TYPE_BBOARD, start, pageParams);
    navigator.setResultParams(results);
    if (results == null || results.getResults() == null) {
      return null;
    }
    List<IALog> logs = results.getResults();
    List<AccessLogModel> models = new ArrayList<>();
    for (IALog log : logs) {
      AccessLogModel model = new AccessLogModel();
      if (log != null) {
        if (StringUtils.isNotEmpty(Long.toString(log.getKey()))) {
          model.key = Long.toString(log.getKey());
        }
        if (StringUtils.isNotEmpty(Long.toString(log.getDomainId()))) {
          model.did = Long.toString(log.getDomainId());
        }
        if (StringUtils.isNotEmpty(log.getIPAddress())) {
          model.ip = log.getIPAddress();
        }
        if (StringUtils.isNotEmpty(log.getTimestamp().toString())) {
          model.t = log.getTimestamp().toString();
        }
        if (StringUtils.isNotEmpty(log.getType())) {
          model.type = log.getType();
        }
        if (StringUtils.isNotEmpty(log.getUserAgent())) {
          model.ua = log.getUserAgent();
        }
      }
      models.add(model);
    }
    return new Results<>(models, "accesslog", -1, o);
  }

  @RequestMapping(value = "/customreports", method = RequestMethod.GET)
  public
  @ResponseBody
  String upload(HttpServletRequest request) {
    SecureUserDetails sUser = getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    if (!GenericAuthoriser.authoriseAdmin()) {
      throw new ForbiddenAccessException(backendMessages.getString(PERMISSION_DENIED));
    }
    BlobstoreService blobstoreService = AppFactory.get().getBlobstoreService();
    String fullURL = request.getServletPath() + request.getPathInfo();
    if (StringUtils.isNotEmpty(fullURL)) {
      return blobstoreService.createUploadUrl(fullURL);
    }
    return null;
  }

  @RequestMapping(value = "/customreports", method = RequestMethod.POST)
  public
  @ResponseBody
  CustomReportsConfig.Config getConfig(@RequestParam String templateName, @RequestParam String edit,
                                       @RequestParam String templateKey,
                                       HttpServletRequest request) {
    SecureUserDetails sUser = getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    Long domainId = SecurityUtils.getCurrentDomainId();
    if (StringUtils.isEmpty(templateName)) {
      xLogger.severe("Error in fetching configuration");
      throw new BadRequestException(backendMessages.getString("config.fetch.error"));
    }
    try {
      if (edit.equalsIgnoreCase("true")) {
        customReportBuilder.removeUploadedObject(templateKey);
      }
      IUploaded
          uploaded =
          customReportBuilder.updateUploadedObject(request, sUser, domainId,
              AppFactory.get().getBlobstoreService(), templateName);
      CustomReportsConfig.Config config = new CustomReportsConfig.Config();
      if (uploaded != null) {
        config.fileName = uploaded.getFileName();
        config.templateKey = uploaded.getId();
      }
      return config;
    } catch (ServiceException e) {
      xLogger.severe("Error in fetching configuration", e);
      throw new InvalidServiceException(backendMessages.getString("config.fetch.error"));
    }
  }

  @RequestMapping(value = "/customreport/add", method = RequestMethod.POST)
  public
  @ResponseBody
  String setCustomReports(@RequestBody AddCustomReportRequestObj addCustomReportRequestObj) {
    SecureUserDetails sUser = getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    if (!GenericAuthoriser.authoriseAdmin()) {
      throw new ForbiddenAccessException(backendMessages.getString(PERMISSION_DENIED));
    }
    if (addCustomReportRequestObj == null || addCustomReportRequestObj.customReport == null
        || addCustomReportRequestObj.config == null) {
      throw new BadRequestException(backendMessages.getString("customreports.update.error"));
    }
    String userId = sUser.getUsername();
    Long domainId = SecurityUtils.getCurrentDomainId();
    if (domainId == null) {
      xLogger.severe("Error in updating Custom Reports");
      throw new InvalidServiceException(backendMessages.getString("customreports.update.error"));
    }
    try {
      ConfigContainer cc = getDomainConfig(domainId, userId);
      CustomReportsConfig crc = cc.dc.getCustomReportsConfig();
      if (!addCustomReportRequestObj.customReport.tk
          .equalsIgnoreCase(addCustomReportRequestObj.config.templateKey)) {
        crc.removeConfig(addCustomReportRequestObj.customReport.tn);
      }
      if (StringUtils.isNotEmpty(addCustomReportRequestObj.customReport.origname)) {
        crc.removeConfig(addCustomReportRequestObj.customReport.origname);
      }
      CustomReportsConfig.Config
          config =
          customReportBuilder.populateConfig(addCustomReportRequestObj.customReport,
              addCustomReportRequestObj.config, true, cc.dc.getTimezone());
      crc.getCustomReportsConfig().add(config);
      cc.dc.setCustomReportsConfig(crc);
      cc.dc.addDomainData(ConfigConstants.CUSTOM_REPORTS, generateUpdateList(userId));
      saveDomainConfig(domainId, cc, backendMessages);
      xLogger.info("AUDITLOG \t {0} \t {1} \t CONFIGURATION \t " +
          "ADD CUSTOM REPORTS", domainId, sUser.getUsername());
      xLogger.info(cc.dc.toJSONSring());
    } catch (ServiceException | ConfigurationException e) {
      xLogger.severe("Error in updating Custom Reports", e);
      throw new InvalidServiceException(backendMessages.getString("customreports.update.error"));
    }
    return backendMessages.getString("customreports.update.success");
  }

  @RequestMapping(value = "/customreport", method = RequestMethod.GET)
  public
  @ResponseBody
  NotificationsModel getCustomReports() {
    SecureUserDetails sUser = getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    Long domainId = SecurityUtils.getCurrentDomainId();
    if (domainId == null) {
      xLogger.severe("Error in fetching list of Custom Reports");
      throw new InvalidServiceException(
          backendMessages.getString("customreports.list.fetch.error"));
    }
    DomainConfig dc = DomainConfig.getInstance(domainId);
    CustomReportsConfig crc = dc.getCustomReportsConfig();
    if (crc == null || crc.getCustomReportsConfig() == null) {
      xLogger.severe("Error in fetching list of Custom Reports");
      throw new ConfigurationServiceException(
          backendMessages.getString("customreports.list.fetch.error"));
    }
    try {
      return customReportBuilder
          .populateCustomReportModelsList(crc.getCustomReportsConfig(), locale, domainId,
              sUser.getTimezone(), true);
    } catch (ServiceException | ObjectNotFoundException e) {
      xLogger.severe("Error in fetching list of Custom Reports", e);
      throw new InvalidServiceException("Error in fetching list of Custom Reports");
    }
  }

  @RequestMapping(value = "/customreport/delete", method = RequestMethod.POST)
  public
  @ResponseBody
  String deleteCustomReport(@RequestBody String name) {
    SecureUserDetails sUser = getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    if (!GenericAuthoriser.authoriseAdmin()) {
      throw new ForbiddenAccessException(backendMessages.getString(PERMISSION_DENIED));
    }
    String userId = sUser.getUsername();
    Long domainId = SecurityUtils.getCurrentDomainId();
    if (domainId == null) {
      xLogger.severe("Error in deleting Custom Reports");
      throw new InvalidServiceException(backendMessages.getString("customreports.delete.error"));
    }
    try {
      DomainConfig dc = DomainConfig.getInstance(domainId);
      if (dc == null || dc.getCustomReportsConfig() == null) {
        xLogger.severe(
            "Error in deleting Custom Reports since domain config is null, domain: {0} name: {1}",
            domainId, name);
        throw new InvalidServiceException(backendMessages.getString("customreports.delete.error"));
      }
      CustomReportsConfig crc = dc.getCustomReportsConfig();
      CustomReportsConfig.Config config = crc.getConfig(name);
      if (config == null) {
        xLogger
            .severe(
                "Error in deleting custom reports since config is null, domain: {0} name: {1}",
                domainId, name);
        throw new InvalidServiceException(backendMessages.getString("customreports.delete.error"));
      }
      crc.removeConfig(name);
      if (StringUtils.isEmpty(config.templateKey) || !customReportBuilder
          .removeUploadedObject(config.templateKey)) {
        xLogger.warn(
            "Unable to delete the uploaded template for custom report: {0} name: {1} and domain: {2}",
            config.templateKey, name, domainId);
      }
      dc.setCustomReportsConfig(crc);
      String key = IConfig.CONFIG_PREFIX + domainId;
      ConfigContainer cc = new ConfigContainer();
      cc.add = false;
      cc.c = configurationMgmtService.getConfiguration(key);
      cc.dc = dc;
      cc.dc.addDomainData(ConfigConstants.CUSTOM_REPORTS, generateUpdateList(userId));
      saveDomainConfig(domainId, cc, backendMessages);
      xLogger.info("AUDITLOG \t {0} \t {1} \t CONFIGURATION \t " +
          "DELETE CUSTOM REPORTS", domainId, sUser.getUsername());
    } catch (ServiceException | ObjectNotFoundException e) {
      xLogger.severe("Error in deleting Custom Reports, domain: {0} name: {1}", domainId, name, e);
      throw new InvalidServiceException(backendMessages.getString("customreports.delete.error"));
    }
    return backendMessages.getString("customreports.delete.success");
  }

  @RequestMapping(value = "/customreport/fetch", method = RequestMethod.GET)
  public
  @ResponseBody
  NotificationsModel getCustomReport(@RequestParam String n) {
    SecureUserDetails sUser = getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    Long domainId = SecurityUtils.getCurrentDomainId();
    if (domainId == null) {
      xLogger.severe("Error in fetching Custom Report");
      throw new InvalidServiceException(backendMessages.getString("customreports.fetch.error"));
    }
    DomainConfig dc = DomainConfig.getInstance(domainId);
    CustomReportsConfig crc = dc.getCustomReportsConfig();
    CustomReportsConfig.Config config = crc.getConfig(n);
    if (config == null) {
      xLogger.severe("Error in fetching Custom Report");
      throw new ConfigurationServiceException(
          backendMessages.getString("customreports.fetch.error"));
    }
    try {
      NotificationsModel
          model =
          customReportBuilder
              .populateCustomReportModelsList(Collections.singletonList(config), locale,
                  domainId,
                  sUser.getTimezone(), false);
      if (model.config instanceof CustomReportsConfigModel) {
        CustomReportsConfigModel m = (CustomReportsConfigModel) model.config;
        m.mn =
            userBuilder.buildUserModels(constructUserAccount(config.managers), locale,
                sUser.getTimezone(), true);
        m.an =
            userBuilder.buildUserModels(constructUserAccount(config.users), locale,
                sUser.getTimezone(), true);
        m.sn =
            userBuilder.buildUserModels(constructUserAccount(config.superUsers), locale,
                sUser.getTimezone(), true);
        m.exusrs = config.extUsers;
        m.usrTgs = config.usrTgs;
      }
      return model;
    } catch (ServiceException | ObjectNotFoundException e) {
      throw new InvalidServiceException("Ero");
    }

  }

  private List<IUserAccount> constructUserAccount(List<String> userIds) {
    if (CollectionUtils.isNotEmpty(userIds)) {
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

  @RequestMapping(value = "/customreport/update", method = RequestMethod.POST)
  public
  @ResponseBody
  String updateCustomReport(@RequestBody CustomReportsConfigModel model) {
    SecureUserDetails sUser = getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    if (!GenericAuthoriser.authoriseAdmin()) {
      throw new ForbiddenAccessException(backendMessages.getString(PERMISSION_DENIED));
    }
    if (model == null) {
      throw new BadRequestException("Error in updating Custom Report");
    }
    FileValidationUtil.validateUploadFile(model.fn.substring(model.fn.lastIndexOf(".")+1));
    String userId = sUser.getUsername();
    Long domainId = SecurityUtils.getCurrentDomainId();
    if (domainId == null) {
      xLogger.severe("Error in updating Custom Report");
      throw new BadRequestException(backendMessages.getString("customreport.update.error"));
    }
    try {
      ConfigContainer cc = getDomainConfig(domainId, userId);
      CustomReportsConfig.Config config = new CustomReportsConfig.Config();
      config = customReportBuilder.populateConfig(model, config, false, cc.dc.getTimezone());
      CustomReportsConfig crc = cc.dc.getCustomReportsConfig();
      crc.removeConfig(model.origname);
      crc.getCustomReportsConfig().add(config);
      cc.dc.setCustomReportsConfig(crc);
      cc.dc.addDomainData(ConfigConstants.CUSTOM_REPORTS, generateUpdateList(userId));
      saveDomainConfig(domainId, cc, backendMessages);
      xLogger.info("AUDITLOG \t {0} \t {1} \t CONFIGURATION \t " +
          "UPDATE CUSTOM REPORTS", domainId, sUser.getUsername());
      xLogger.info(cc.dc.toJSONSring());
    } catch (ServiceException e) {
      xLogger.severe("Error in updating Custom Report", e);
      throw new InvalidServiceException(backendMessages.getString("customreport.update.error"));
    } catch (ConfigurationException e) {
      xLogger.severe("Error while saving Custom reports configuration", e);
    }
    return "Custom Report updated successfully";
  }

  @RequestMapping(value = "/customreport/export", method = RequestMethod.POST)
  public
  @ResponseBody
  String exportReport(@RequestBody String name) {
    SecureUserDetails sUser = getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    Long domainId = SecurityUtils.getCurrentDomainId();
    if (domainId == null) {
      throw new InvalidServiceException(backendMessages.getString("report.export.error"));
    }
    Map<String, String> params = new HashMap<>();
    params.put("action", "scheduleexport");
    params.put("domainid", domainId.toString());
    params.put("reportname", name);
    Map<String, String> headers = new HashMap<>();
    headers.put("Host", AppFactory.get().getBackendService().getBackendAddress(Constants.BACKEND1));
    Long jobId =
        JobUtil.createJob(domainId, sUser.getUsername(), null, IJobStatus.TYPE_CUSTOMREPORT, name,
            params);
    params.put("jobid", jobId.toString());

    try {
      AppFactory.get().getTaskService()
          .schedule(ITaskService.QUEUE_EXPORTER, "/task/customreportsexport", params, headers,
              ITaskService.METHOD_POST);
    } catch (TaskSchedulingException e) {
      xLogger.severe("{0} when scheduling task for custom report export of {1}: {2}",
          e.getClass().getName(), name, e.getMessage());
      throw new InvalidTaskException(
          e.getClass().getName() + " " + backendMessages
              .getString("customreports.schedule.export")
              + " " + name);
    }
    return String.valueOf(jobId);
  }

  @RequestMapping(value = "/report/filters", method = RequestMethod.GET)
  public
  @ResponseBody
  Map<String, List<String>> getReportFilters() {
    Long domainId = SecurityUtils.getCurrentDomainId();
    ReportsConfig rConfig = ReportsConfig.getInstance(domainId);
    Map<String, List<String>> filters = new HashMap<>(6);
    filters.put(ReportsConstants.FILTER_COUNTRY,
        rConfig.getFilterValues(ReportsConstants.FILTER_COUNTRY));
    filters
        .put(ReportsConstants.FILTER_STATE, rConfig.getFilterValues(ReportsConstants.FILTER_STATE));
    filters.put(ReportsConstants.FILTER_DISTRICT,
        rConfig.getFilterValues(ReportsConstants.FILTER_DISTRICT));
    filters
        .put(ReportsConstants.FILTER_TALUK, rConfig.getFilterValues(ReportsConstants.FILTER_TALUK));
    filters
        .put(ReportsConstants.FILTER_CITY, rConfig.getFilterValues(ReportsConstants.FILTER_CITY));
    filters.put(ReportsConstants.FILTER_PINCODE,
        rConfig.getFilterValues(ReportsConstants.FILTER_PINCODE));
    return filters;
  }

  @RequestMapping(value = "/domains/all", method = RequestMethod.GET)
  public
  @ResponseBody
  Results getDomainsAsResult(
      @RequestParam(required = false, defaultValue = PageParams.DEFAULT_SIZE_STR) String s,
      @RequestParam(required = false, defaultValue = PageParams.DEFAULT_OFFSET_STR) String o,
      @RequestParam(required = false) String q,
      HttpServletRequest request) {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    Query query = pm.newQuery(JDOUtils.getImplClass(IDomain.class));
    Map<String, Object> params = new HashMap<>();
    String filter;
    if (q != null && !q.isEmpty()) {
      filter = "nNm.startsWith(txtParam)";
      query.setFilter(filter);
      query.declareParameters("String txtParam");
      params.put("txtParam", q.toLowerCase());
    } else {
      filter = "hasParent==false";
      query.setFilter(filter);
      query.setOrdering("nNm asc");
    }
    if (o != null) {
      int off = Integer.parseInt(o);
      int sz = Integer.parseInt(s);
      Navigator navigator = new Navigator(request.getSession(), "DomainConfigController.getDomains", off, sz, "dummy",
          0);
      PageParams pageParams = new PageParams(navigator.getCursor(off), off, sz);
      QueryUtil.setPageParams(query, pageParams);
    }
    List<IDomain> domains = null;
    SecureUserDetails sUser = getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    try {
      if (q != null && !q.isEmpty()) {
        domains = (List<IDomain>) query.executeWithMap(params);
      } else {
        domains = (List<IDomain>) query.execute();
      }
      domains = (List<IDomain>) pm.detachCopyAll(domains);
      if (domains != null) {
        domains.size(); // to retrieve the results before closing the PM
      }
    } catch (Exception e) {
      xLogger.severe("Error in fetching list of domains", e);
      throw new InvalidServiceException(backendMessages.getString("domains.fetch.error"));
    } finally {
      try {
        query.closeAll();
      } catch (Exception ignored) {
        xLogger.warn("Exception while closing query", ignored);
      }
      pm.close();
    }
    return new Results<>(domains, null, -1, Integer.parseInt(o));
  }

  @RequestMapping(value = "/domaininfo")
  public
  @ResponseBody
  CurrentUserModel getCurrentSessionDetails() {
    return currentUserBuilder.buildCurrentUserModel();
  }

  @RequestMapping(value = "/stock-rebalancing", method = RequestMethod.POST)
  public
  @ResponseBody
  String updateStockRebalancingConfig(@RequestBody StockRebalancingConfigModel model)
      throws ServiceException, ConfigurationException {
    SecureUserDetails sUser = getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    if (!GenericAuthoriser.authoriseAdmin()) {
      throw new ForbiddenAccessException(backendMessages.getString(PERMISSION_DENIED));
    }
    String userId = sUser.getUsername();
    Long domainId = SecurityUtils.getCurrentDomainId();
    ConfigContainer cc = getDomainConfig(domainId, userId);
    StockRebalancingConfig stockRebalancingConfig = configurationModelBuilder.buildStockRebalancingConfig(model);
    cc.dc.setStockRebalancingConfig(stockRebalancingConfig);
    cc.dc.addDomainData(ConfigConstants.STOCK_REBALANCING, generateUpdateList(userId));
    saveDomainConfig(domainId, cc, backendMessages);
    return backendMessages.getString("config.stock.rebalancing.updated.successfully");
  }

  @RequestMapping(value = "/approvals", method = RequestMethod.POST)
  public
  @ResponseBody
  String updateApprovalsConfig(@RequestBody ApprovalsConfigModel model)
      throws ServiceException, ConfigurationException {
    SecureUserDetails sUser = getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    if (!GenericAuthoriser.authoriseAdmin()) {
      throw new ForbiddenAccessException(backendMessages.getString(PERMISSION_DENIED));
    }
    String userId = sUser.getUsername();
    Long domainId = SecurityUtils.getCurrentDomainId();
    ConfigContainer cc = getDomainConfig(domainId, userId);
    ApprovalsConfig.OrderConfig orderConfig = configurationModelBuilder.buildApprovalsOrderConfig(model);
    ApprovalsConfig approvalsConfig = new ApprovalsConfig();
    approvalsConfig.setOrderConfig(orderConfig);
    cc.dc.setApprovalsConfig(approvalsConfig);
    cc.dc.addDomainData(ConfigConstants.APPROVALS, generateUpdateList(userId));
    saveDomainConfig(domainId, cc, backendMessages);
    xLogger.info("AUDITLOG \t {0} \t {1} \t CONFIGURATION \t " +
        "UPDATE APPROVALS", domainId, sUser.getUsername());
    xLogger.info(cc.dc.toJSONSring());

    return backendMessages.getString("approvals.config.update.success");
  }

  @RequestMapping(value = "/dashboard", method = RequestMethod.GET)
  public
  @ResponseBody
  DashboardConfigModel getDashboardConfig() {
    SecureUserDetails sUser = getUserDetails();
    Locale locale = sUser.getLocale();
    Long domainId = SecurityUtils.getCurrentDomainId();
    DomainConfig dc = DomainConfig.getInstance(domainId);
    DashboardConfig dbc = dc.getDashboardConfig();
    try {
      return configurationModelBuilder.buildDashboardConfigModel(dbc, domainId, locale, sUser.getTimezone());
    } catch (ObjectNotFoundException e) {
      xLogger.severe("Error in fetching Dashboard configuration", e);
      throw new InvalidServiceException("Error in fetching Dashboard configuration");
    }
  }

  @RequestMapping(value = "/stock-rebalancing", method = RequestMethod.GET)
  public
  @ResponseBody
  StockRebalancingConfigModel getStockRebalancingConfig() {
    SecureUserDetails sUser = getUserDetails();
    Locale locale = sUser.getLocale();
    Long domainId = SecurityUtils.getCurrentDomainId();
    DomainConfig dc = DomainConfig.getInstance(domainId);
    StockRebalancingConfig stockRebalancingConfig = dc.getStockRebalancingConfig();
    try {
      return configurationModelBuilder.buildStockRebalancingConfigModel(stockRebalancingConfig, domainId, locale, sUser.getTimezone());
    } catch (Exception e) {
      xLogger.severe("Error in fetching stock rebalancing configuration", e);
      throw new InvalidServiceException("Error in fetching stock rebalancing configuration");
    }
  }

  @RequestMapping(value = "/approvals", method = RequestMethod.GET)
  public
  @ResponseBody
  ApprovalsConfigModel getApprovalsConfig() {
    SecureUserDetails sUser = getUserDetails();
    Locale locale = sUser.getLocale();
    Long domainId = SecurityUtils.getCurrentDomainId();
    DomainConfig dc = DomainConfig.getInstance(domainId);
    ApprovalsConfig ac = dc.getApprovalsConfig();
    try {
      return configurationModelBuilder.buildApprovalsConfigModel(ac, domainId, locale, sUser.getTimezone());
    } catch (Exception e) {
      xLogger.severe("Error in fetching Approval configuration", e);
      throw new InvalidServiceException("Error in fetching Approvals configuration");
    }

  }

  @RequestMapping(value = "/approvals-enabled", method = RequestMethod.GET)
  public
  @ResponseBody
  ApprovalsEnabledConfigModel getApprovalsEnabledConfig() {
    Long domainId = SecurityUtils.getCurrentDomainId();
    try {
      return configurationModelBuilder.buildApprovalsEnabledConfigModel(domainId);
    } catch (Exception e) {
      throw new InvalidServiceException("Error in fetching Approvals enabled configuration");
    }
  }

  /**
   * Get the event summary configurations
   */
  @RequestMapping(value = "/event-summary", method = RequestMethod.GET)
  public
  @ResponseBody
  EventSummaryConfigModel getEventSummaryConfig(@RequestParam(required = false) Long domainId) {
    EventSummaryConfigModel model = null;
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    String timezone = sUser.getTimezone();
    try {
      Long
          dId =
          (null == domainId) ? SecurityUtils.getCurrentDomainId() : domainId;
      if (!usersService.hasAccessToDomain(sUser.getUsername(), dId)) {
        xLogger.warn("User {0} does not have access to domain id {1}", sUser.getUsername(),
            dId);
        throw new ForbiddenAccessException("User does not have access to domain");
      }

      //Get the configuration for the domain
      DomainConfig dc = DomainConfig.getInstance(dId);
      if (dc != null) {
        model = dc.getEventSummaryConfig();
      }
      //Get the template
      EventSummaryConfigModel templateEvent = EventSummaryTemplateLoader.getDefaultTemplate();
      if (templateEvent != null) {
        if (model == null) {
          return templateEvent;
        } else if (model.getEvents().isEmpty()) {
          templateEvent.setTagDistribution(model.getTagDistribution());
          templateEvent.setTag(model.getTag());
          return templateEvent;
        } else {
          model.buildEvents(templateEvent.getEvents());
          List<String> val = dc.getDomainData(ConfigConstants.EVENT_SUMMARY_INVENTORY);
          if (val != null) {
            model.setCreatedBy(val.get(0));
            model.setLastUpdated(
                LocalDateUtil.format(new Date(Long.parseLong(val.get(1))), locale, timezone));
            model.setFullName(usersService.getUserAccount(model.getCreatedBy()).getFullName());
          }
          xLogger.info(" Event summary events built based on template");
        }
      }
    } catch (Exception e) {
      xLogger.warn("Exception fetching event summary configuration", e);
      ResourceBundle
          backendMessages =
          Resources.getBundle(sUser.getLocale());
      throw new BadRequestException(backendMessages.getString("error.fetch.event.summary"));
    }
    return model;
  }

  /**
   * Update the event summary configuration
   *
   * @param configModel configuration model
   */
  @RequestMapping(value = "/event-summary", method = RequestMethod.PUT)
  public
  @ResponseBody
  String updateEventSummary(@RequestBody EventSummaryConfigModel configModel) {
    //Get logged in user's details
    SecureUserDetails sUser = getUserDetails();
    ResourceBundle
        backendMessages =
        Resources.getBundle(sUser.getLocale());

    String responseMsg;
    try {
      ConfigContainer
          cc =
          getDomainConfig(SecurityUtils.getCurrentDomainId(), sUser.getUsername()
          );
      EventSummaryConfigModel
          eventSummaryConfigModel =
          cc.dc.getEventSummaryConfig() != null ? cc.dc.getEventSummaryConfig()
              : new EventSummaryConfigModel();
      //if event summary is configured, remove the events with specified category
      if (configModel.getEvents() != null && !configModel.getEvents().isEmpty()) {
        //remove the events from template, with the category same as in the request
        eventSummaryConfigModel
            .removeEventsByCategory(configModel.getEvents().get(0).getCategory());

        // generate unique id for each thresholds
        configModel.setUniqueIdentifier();
        //add the event for the category
        eventSummaryConfigModel.addEvents(configModel.getEvents());


      }

      //Add tag distribution details
      if (configModel.getTagDistribution() != null) {
        eventSummaryConfigModel.setTagDistribution(configModel.getTagDistribution());
      }
      eventSummaryConfigModel.setTag(configModel.getTag());

      //update configuration
      cc.dc.setEventSummaryConfig(eventSummaryConfigModel);
      cc.dc.addDomainData(ConfigConstants.EVENT_SUMMARY_INVENTORY,
          generateUpdateList(sUser.getUsername()));
      saveDomainConfig(SecurityUtils.getCurrentDomainId(), cc, backendMessages);
      xLogger.info("Event summary configured for the domain" + SecurityUtils.getCurrentDomainId());
      responseMsg = backendMessages.getString("event.summary.update.success");
    } catch (ServiceException e) {
      xLogger.severe("Error in updating Event Summary", e);
      throw new InvalidServiceException(backendMessages.getString("event.summary.update.error"));
    } catch (Exception e) {
      xLogger.warn("Configuration exception", e);
      throw new InvalidServiceException(backendMessages.getString("event.summary.update.error"));
    }
    return responseMsg;
  }

  @RequestMapping(value="/general-notifications", method = RequestMethod.GET)
  public
  @ResponseBody
  String getGeneralNotificationsConfig() {
    return DomainConfig.getInstance(SecurityUtils.getCurrentDomainId()).getLangPreference();
  }

  @RequestMapping(value="general-notifications", method = RequestMethod.POST)
  public
  @ResponseBody
  String updateGeneralNotificationsConfig(@RequestBody String language)
      throws ServiceException, ConfigurationException {
    SecureUserDetails sUser = getUserDetails();
    ResourceBundle backendMessages = Resources.getBundle(sUser.getLocale());
    if(!GenericAuthoriser.authoriseAdmin()) {
      throw new ForbiddenAccessException(backendMessages.getString(PERMISSION_DENIED));
    }
    Long domainId = SecurityUtils.getCurrentDomainId();
    ConfigContainer cc = getDomainConfig(domainId, sUser.getUsername());
    cc.dc.setLangPreference(language);
    saveDomainConfig(domainId, cc, backendMessages);
    xLogger.info("General notifications configuration updated successfully for domain " + domainId);
    return backendMessages.getString("general.notifications.update.success");
  }

  @RequestMapping(value = "/dashboard", method = RequestMethod.POST)
  public
  @ResponseBody
  String updateDashboardConfig(@RequestBody DashboardConfigModel model) {
    SecureUserDetails sUser = getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    if (!GenericAuthoriser.authoriseAdmin()) {
      throw new ForbiddenAccessException(backendMessages.getString(PERMISSION_DENIED));
    }
    try {
      String userId = sUser.getUsername();
      Long domainId = SecurityUtils.getCurrentDomainId();
      DashboardConfig dashboardConfig = new DashboardConfig();

      DashboardConfig.ActivityPanelConfig
          actPanelConfig =
          new DashboardConfig.ActivityPanelConfig();
      actPanelConfig.showActivityPanel = model.ape;

      DashboardConfig.RevenuePanelConfig rvnPanelConfig = new DashboardConfig.RevenuePanelConfig();
      rvnPanelConfig.showRevenuePanel = model.rpe;

      DashboardConfig.OrderPanelConfig ordPanelConfig = new DashboardConfig.OrderPanelConfig();
      ordPanelConfig.showOrderPanel = model.ope;

      DashboardConfig.InventoryPanelConfig
          invPanelConfig =
          new DashboardConfig.InventoryPanelConfig();
      invPanelConfig.showInvPanel = model.ipe;

      DashboardConfig.AssetsDbConfig assetsDbConfig = new DashboardConfig.AssetsDbConfig();
      assetsDbConfig.dats = StringUtil.getCSV(model.dats);
      assetsDbConfig.dmt = model.dmt;

      DashboardConfig.DBOverviewConfig dbOverviewConfig = new DashboardConfig.DBOverviewConfig();
      if (model.dmtg != null) {
        dbOverviewConfig.dmtg = StringUtil.getCSV(model.dmtg);
      } else {
        dbOverviewConfig.dmtg = null;
      }
      dbOverviewConfig.dimtg = model.dimtg;
      dbOverviewConfig.detg = model.detg;
      dbOverviewConfig.dtt = model.dtt;
      dbOverviewConfig.atdd = model.atdd;
      dbOverviewConfig.edm = model.edm;
      dbOverviewConfig.aper = model.aper;
      if (model.exet != null) {
        StringBuilder eetags = new StringBuilder();
        for (int i = 0; i < model.exet.length; i++) {
          if (StringUtils.isNotEmpty(model.exet[i])) {
            eetags.append(model.exet[i].concat(","));
          }
        }
        dbOverviewConfig.exet = eetags.toString();
      } else {
        dbOverviewConfig.exet = null;
      }
      if (model.dutg != null) {
        StringBuilder eutgs = new StringBuilder();
        for (String uTag : model.dutg) {
          if (StringUtils.isNotEmpty(uTag)) {
            eutgs.append(uTag.concat(CharacterConstants.COMMA));
          }
        }
        eutgs.setLength(eutgs.length() - 1);
        dbOverviewConfig.dutg = eutgs.toString();
      } else {
        dbOverviewConfig.dutg = null;
      }
      if (model.exts != null) {
        dbOverviewConfig.exts = StringUtil.getCSV(model.exts);
      }
      dashboardConfig.setActivityPanelConfig(actPanelConfig);
      dashboardConfig.setRevenuePanelConfig(rvnPanelConfig);
      dashboardConfig.setOrderPanelConfig(ordPanelConfig);
      dashboardConfig.setInventoryPanelConfig(invPanelConfig);
      dashboardConfig.setDbOverConfig(dbOverviewConfig);
      dashboardConfig.setAssetsDbConfig(assetsDbConfig);

      ConfigContainer cc = getDomainConfig(domainId, userId);
      cc.dc.setDashboardConfig(dashboardConfig);
      cc.dc.addDomainData(ConfigConstants.DASHBOARD, generateUpdateList(userId));
      saveDomainConfig(domainId, cc, backendMessages);
      xLogger.info("AUDITLOG \t {0} \t {1} \t CONFIGURATION \t " +
          "UPDATE DASHBOARD", domainId, sUser.getUsername());
      xLogger.info(cc.dc.toJSONSring());
    } catch (ServiceException | ConfigurationException e) {
      xLogger.severe("Error in updating Custom Report", e);
      throw new InvalidServiceException(backendMessages.getString("customreport.update.error"));
    }

    return backendMessages.getString("dashboard.config.update.success");
  }

  private void saveDomainConfig(Long domainId, ConfigContainer cc,
                                ResourceBundle backendMessages)
      throws ServiceException {
    try {
      cc.c.setConfig(cc.dc.toJSONSring());
      //added to send user
      if (StringUtils.isEmpty(cc.c.getUserId())) {
        cc.c.setUserId(getUserDetails().getUsername());
      }
      if (cc.add) {
        configurationMgmtService.addConfiguration(cc.c.getKey(), cc.c);
      } else {
        configurationMgmtService.updateConfiguration(cc.c);
      }
      MemcacheService cache = AppFactory.get().getMemcacheService();
      if (cache != null) {
        cache.put(DomainConfig.getCacheKey(domainId), cc.dc);
      }
    } catch (ConfigurationException | ServiceException e) {
      xLogger
          .severe("Invalid format of configuration for domain {0}: {1}", domainId,
              e);
      throw new ConfigurationServiceException(
          backendMessages.getString("invalid.domain.config") + " " + domainId);
    }
  }

  private ConfigContainer getDomainConfig(Long domainId, String userId)
      throws ServiceException, ConfigurationException {
    String key = IConfig.CONFIG_PREFIX + domainId;
    ConfigContainer cc = new ConfigContainer();
    try {
      cc.c = configurationMgmtService.getConfiguration(key);
      cc.dc = new DomainConfig(cc.c.getConfig());
    } catch (ObjectNotFoundException e) {
      cc.dc = new DomainConfig();
      cc.c = JDOUtils.createInstance(IConfig.class);
      cc.c.setKey(key);
      cc.c.setUserId(userId);
      cc.c.setDomainId(domainId);
      cc.c.setLastUpdated(new Date());
      cc.add = true;
    }
    return cc;
  }

  @RequestMapping(value="/general/domains", method = RequestMethod.GET)
  public
  @ResponseBody
  List<GeneralConfigModel> getGeneralConfigForDomains(
      @RequestParam(name = "domain_ids") List<String> domainIds)
      throws ServiceException{
    SecureUserDetails sUser = getUserDetails();
    return configurationModelBuilder.buildDomainLocationModels(domainIds, usersService,
        sUser.getUsername());

  }

  @RequestMapping(value = "/return-config", method = RequestMethod.GET)
  public
  @ResponseBody
  ReturnsConfigModel getReturnConfig(@RequestParam(required = true) Long entityId) {
    try {
      Optional<ReturnsConfig> returnsConfig = inventoryManagementService.getReturnsConfig(entityId);
      if (returnsConfig.isPresent()) {
        return configurationModelBuilder.buildReturnsConfigModel(returnsConfig.get());
      }
    } catch (Exception e) {
      xLogger.warn("Error in fetching reasons for transactions", e);
    }
    return null;
  }

  @RequestMapping(value = "/forms", method = RequestMethod.GET)
  public
  @ResponseBody
  FormsConfigModel getFormsConfig() {
    Long domainId = SecurityUtils.getCurrentDomainId();
    DomainConfig dc = DomainConfig.getInstance(domainId);
    FormsConfig formsConfig = dc.getFormsConfig();
    return configurationModelBuilder.buildFormsConfigModel(formsConfig);
  }

  @RequestMapping(value = "/forms", method = RequestMethod.POST)
  public
  @ResponseBody
  String updateFormsConfig(@RequestBody FormsConfigModel formsConfigModel) {
    SecureUserDetails sUser = getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    if(!sUser.getRole().equals(SecurityConstants.ROLE_SUPERUSER)) {
      throw new ForbiddenAccessException(backendMessages.getString(PERMISSION_DENIED));
    }
    if(formsConfigModel == null) {
      xLogger.warn("Error in updating Forms config");
      throw new BadRequestException(backendMessages.getString("forms.config.update.error"));
    }
    String userId = sUser.getUsername();
    Long domainId = SecurityUtils.getCurrentDomainId();
    try {
      ConfigContainer cc = getDomainConfig(domainId, userId);
      FormsConfig formsConfig = formsConfigBuilder.buildFormsConfig(formsConfigModel);
      cc.dc.addDomainData(ConfigConstants.FORMS, generateUpdateList(userId));
      cc.dc.setFormsConfig(formsConfig);
      saveDomainConfig(domainId, cc, backendMessages);
      xLogger.info(cc.dc.toJSONSring());
    } catch (ServiceException | ConfigurationException e) {
      xLogger.severe(backendMessages.getString("Error in updating forms configuration"), e);
      throw new InvalidServiceException(backendMessages.getString("forms.config.update.error"));
    }
    return backendMessages.getString("forms.config.update.success");
  }

  @RequestMapping(value = "/transporters", method = RequestMethod.GET)
  public
  @ResponseBody
  TransportersConfigModel getTransportersConfig() throws ServiceException {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    DomainConfig dc = DomainConfig.getInstance(sUser.getCurrentDomainId());
    TransportersConfig transportersConfig = dc.getTransportersConfig();
    if(transportersConfig != null) {
      return configurationModelBuilder.buildTransportersConfigModel(transportersConfig,
          sUser.getLocale(), sUser.getTimezone());
    }
    return new TransportersConfigModel();
  }

  @RequestMapping(value = "/transporters", method = RequestMethod.POST)
  public
  @ResponseBody
  String updateTransportersConfig(@RequestBody TransportersConfigModel configModel) {
    SecureUserDetails sUser = getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(BACKEND_MESSAGES, locale);
    if(!SecurityUtils.isAdmin()) {
      throw new ForbiddenAccessException(backendMessages.getString(PERMISSION_DENIED));
    }
    if(configModel == null) {
      xLogger.warn("Error in updating Transporters config");
      throw new BadRequestException(backendMessages.getString("transporters.config.update.error"));
    }
    String userId = sUser.getUsername();
    Long domainId = SecurityUtils.getCurrentDomainId();
    try {
      ConfigContainer cc = getDomainConfig(domainId, userId);
      TransportersConfig config = TransportersConfigBuilder.buildTransportersConfig(configModel);
      config.setUpdatedBy(userId);
      config.setLastUpdated(String.valueOf(System.currentTimeMillis()));
      cc.dc.setTransportersConfig(config);
      saveDomainConfig(domainId, cc, backendMessages);
    } catch (ServiceException | ConfigurationException e) {
      xLogger.severe(backendMessages.getString("Error in updating transpoters configuration"), e);
      throw new InvalidServiceException(backendMessages.getString("transporters.config.update.error"));
    }
    return backendMessages.getString("transporters.config.update.success");
  }

  private SyncConfig generateSyncConfig(CapabilitiesConfigModel model) {
    SyncConfig syncCfg = new SyncConfig();
    syncCfg.setMasterDataRefreshInterval(model.mdri * SyncConfig.HOURS_IN_A_DAY);
    syncCfg.setAppLogUploadInterval(model.aplui * SyncConfig.HOURS_IN_A_DAY);
    syncCfg.setSmsTransmissionWaitDuration(model.stwd * SyncConfig.HOURS_IN_A_DAY);
    return syncCfg;
  }

  private class ConfigContainer {
    public IConfig c = null;
    public DomainConfig dc = null;
    public SupportConfig sc = null;
    public boolean add = false;
  }
}
