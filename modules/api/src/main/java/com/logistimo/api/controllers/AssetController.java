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
import com.google.gson.JsonElement;

import com.logistimo.api.builders.AssetBuilder;
import com.logistimo.api.models.AssetBaseModel;
import com.logistimo.api.models.AssetDetailsModel;
import com.logistimo.api.models.TemperatureDomainModel;
import com.logistimo.api.util.SearchUtil;
import com.logistimo.assets.AssetUtil;
import com.logistimo.assets.entity.IAsset;
import com.logistimo.assets.entity.IAssetRelation;
import com.logistimo.assets.models.AssetDataModel;
import com.logistimo.assets.models.AssetModel;
import com.logistimo.assets.models.AssetModels;
import com.logistimo.assets.models.AssetRelationModel;
import com.logistimo.assets.models.DeviceConfigPushPullModel;
import com.logistimo.assets.models.DeviceTempsModel;
import com.logistimo.assets.service.AssetManagementService;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.config.entity.IConfig;
import com.logistimo.config.models.AssetConfig;
import com.logistimo.config.models.AssetSystemConfig;
import com.logistimo.config.models.ConfigurationException;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.config.service.ConfigurationMgmtService;
import com.logistimo.exception.InvalidServiceException;
import com.logistimo.exports.ExportService;
import com.logistimo.logger.XLog;
import com.logistimo.pagination.Navigator;
import com.logistimo.pagination.PageParams;
import com.logistimo.pagination.Results;
import com.logistimo.reports.plugins.internal.ExportModel;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.users.service.UsersService;
import com.logistimo.utils.LocalDateUtil;
import com.logistimo.utils.MsgUtil;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by kaniyarasu on 03/11/15.
 */
@Controller
@RequestMapping("/assets")
public class AssetController {
  private static final XLog xLogger = XLog.getLog(AssetController.class);
  private AssetBuilder assetBuilder;
  private ExportService exportService;
  private UsersService usersService;
  private AssetManagementService assetManagementService;
  private ConfigurationMgmtService configurationMgmtService;

  @Autowired
  public void setAssetBuilder(AssetBuilder assetBuilder) {
    this.assetBuilder = assetBuilder;
  }

  @Autowired
  private void setExportService(ExportService exportService) {
    this.exportService = exportService;
  }

  @Autowired
  private void setUsersService(UsersService usersService) {
    this.usersService = usersService;
  }

  @Autowired
  public void setAssetManagementService(AssetManagementService assetManagementService) {
    this.assetManagementService = assetManagementService;
  }

  @Autowired
  public void setConfigurationMgmtService(ConfigurationMgmtService configurationMgmtService) {
    this.configurationMgmtService = configurationMgmtService;
  }

  @RequestMapping(value = "/", method = RequestMethod.POST)
  public
  @ResponseBody
  String createAssets(@RequestBody final AssetModel assetModel,
                      @RequestParam(required = false, defaultValue = "false") boolean update) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    try {
      long domainId = sUser.getCurrentDomainId();
      if (!update) {
        assetModel.cb = sUser.getUsername();
      }
      assetModel.ub = sUser.getUsername();

      assetBuilder.buildAssetTags(assetModel);
      //Registering device in LS
      if (!update) {
        IAsset asset = assetBuilder.buildAsset(assetModel, sUser.getUsername(), true);
        assetManagementService.createAsset(domainId, asset, assetModel);
      } else {
        IAsset asset = assetManagementService.getAsset(assetModel.id);
        asset = assetBuilder.buildAsset(asset, assetModel, sUser.getUsername(), false);
        assetManagementService.updateAsset(domainId, asset, assetModel);
      }

      AssetSystemConfig asc = AssetSystemConfig.getInstance();
      String
          message =
          backendMessages.getString("asset") + " " + MsgUtil.bold(
              assetModel.dId + "(" + asc.getManufacturerName(assetModel.typ, assetModel.vId) + ")")
              + " " + backendMessages.getString("created.success");
      if (update) {
        message =
            backendMessages.getString("asset") + " " + MsgUtil.bold(
                assetModel.dId + "(" + asc.getManufacturerName(assetModel.typ, assetModel.vId)
                    + ")") + " " + backendMessages.getString("updated.successfully").toLowerCase();
      }
      return message;

    } catch (ServiceException e) {
      xLogger.warn("Error while creating asset {0}", assetModel.toString(), e);
      if (!update) {
        throw new InvalidServiceException(backendMessages.getString("asset.create.error"));
      } else {
        throw new InvalidServiceException(backendMessages.getString("asset.update.error"));
      }
    } catch (IllegalArgumentException e) {
      throw new InvalidServiceException(e);
    } catch (Exception e) {
      xLogger.severe("Error while creating asset {0}", assetModel.toString(), e);
      if (!update) {
        throw new InvalidServiceException(
            backendMessages.getString("asset.create.error") + ": " + backendMessages
                .getString("error.systemerror"));
      } else {
        throw new InvalidServiceException(
            backendMessages.getString("asset.update.error") + ": " + backendMessages
                .getString("error.systemerror"));
      }
    }
  }

  @RequestMapping(value = "/", method = RequestMethod.GET)
  public
  @ResponseBody
  Results getAssets(@RequestParam(required = false) String q,
                    @RequestParam(required = false) Integer at,
                    @RequestParam(defaultValue = PageParams.DEFAULT_SIZE_STR) int size,
                    @RequestParam(defaultValue = PageParams.DEFAULT_OFFSET_STR) int offset,
                    HttpServletRequest request) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    String timezone = sUser.getTimezone();
    Results assetResults;
    Navigator
        navigator =
        new Navigator(request.getSession(), "AssetController.getAssets", offset, size, "dummy", 0);
    PageParams pageParams = new PageParams(navigator.getCursor(offset), offset, size);
    long domainId = sUser.getCurrentDomainId();
    try {
      if (StringUtils.isNotBlank(q)) {
        q = AssetUtil.decodeURLParameters(q);
        assetResults = SearchUtil.findAssets(domainId, q, pageParams);
        assetResults.setNumFound(-1);
      } else {
        assetResults = assetManagementService.getAssetsByDomain(domainId, at, pageParams);
      }
      assetResults.setOffset(offset);
      navigator.setResultParams(assetResults);
    } catch (Exception e) {
      xLogger.severe("Error while getting asset for the domain {0}", domainId, e);
      throw new InvalidServiceException(
          backendMessages.getString("asset.detail.fetch.domain.error"));
    }
    return assetBuilder.buildAssetResults(assetResults, locale, timezone);
  }

  @RequestMapping(value = "/{assetId}", method = RequestMethod.GET)
  public
  @ResponseBody
  AssetDetailsModel getAssets(@PathVariable Long assetId) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    String timezone = sUser.getTimezone();
    try {
      AssetDetailsModel assetDetailsModel =
          assetBuilder.buildAssetDetailsModel(assetManagementService.getAsset(assetId), null, null, locale, timezone);

      //Fetching asset details from AMS, becuase asset meta is available only in AMS.
      AssetModel
          assetModel =
          new Gson()
              .fromJson(
                  AssetUtil.getAssetDetails(assetDetailsModel.getvId(), assetDetailsModel.getdId()),
                  AssetModel.class);
      assetDetailsModel.meta = assetModel.meta;

      return assetDetailsModel;
    } catch (Exception e) {
      xLogger.severe("Error while getting the asset {0}", assetId, e);
      throw new InvalidServiceException(
          backendMessages.getString("asset.detail.fetch.domain.error"));
    }
  }

  @RequestMapping(value = "/details", method = RequestMethod.GET)
  public
  @ResponseBody
  Results getAssetsInDetail(@RequestParam(required = false) Long eid,
                            @RequestParam(required = false) String loc,
                            @RequestParam(required = false) String at,
                            @RequestParam(required = false) Integer ws,
                            @RequestParam(required = false) Integer alrmtype,
                            @RequestParam(required = false) Integer dur,
                            @RequestParam(required = false) Integer awr,
                            @RequestParam(defaultValue = PageParams.DEFAULT_SIZE_STR) int size,
                            @RequestParam(defaultValue = PageParams.DEFAULT_OFFSET_STR) int offset,
                            HttpServletRequest request) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    Navigator
        navigator =
        new Navigator(request.getSession(), "AssetController.getAssets", offset, size, "dummy", 0);
    long domainId = sUser.getCurrentDomainId();
    String tag = String.valueOf(domainId);
    if (eid != null) {
      tag = "kiosk." + eid;
    } else if (StringUtils.isNotEmpty(loc)) {
      tag += "." + loc;
    }
    try {
      Results
          assetResults =
          assetBuilder.buildAssetsFromJson(AssetUtil
                  .getAssetsByTag(tag, null, at, ws, alrmtype, dur, awr, (offset / size) + 1, size),
              size, locale, sUser.getTimezone(), offset);
      assetResults.setNumFound(-1);
      assetResults.setOffset(offset);
      navigator.setResultParams(assetResults);
      return assetResults;
    } catch (Exception e) {
      xLogger.severe("Error while getting the assets for tag {0}", tag, e);
      throw new InvalidServiceException(
          backendMessages.getString("asset.detail.fetch.domain.error"));
    }
  }

  @RequestMapping(value = "/{manufacturerId}/{assetId}", method = RequestMethod.GET)
  public
  @ResponseBody
  AssetDetailsModel getAsset(@PathVariable String manufacturerId,
                             @PathVariable String assetId) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    assetId = AssetUtil.decodeURLParameters(assetId);
    AssetDetailsModel assetDetailsModel;
    try {
      assetDetailsModel =
          assetBuilder
              .buildAssetModelFromJson(AssetUtil.getAssetDetails(manufacturerId, assetId), locale,
                  sUser.getTimezone());
    } catch (Exception e) {
      xLogger.severe("Error while getting asset {0}, {1}", manufacturerId, assetId, e);
      throw new InvalidServiceException(
          backendMessages.getString("asset.detail.fetch.domain.error"));
    }
    return assetDetailsModel;
  }

  @RequestMapping(value = "/relation/{manufacturerId}/{assetId}", method = RequestMethod.GET)
  public
  @ResponseBody
  Object getAssetRelations(@PathVariable String manufacturerId,
                           @PathVariable String assetId) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    String timezone = sUser.getTimezone();
    assetId = AssetUtil.decodeURLParameters(assetId);
    try {
      IAsset asset = assetManagementService.getAsset(manufacturerId, assetId);
      AssetSystemConfig asc = AssetSystemConfig.getInstance();
      if (asc != null && asset != null) {
        AssetSystemConfig.Asset assetConfig = asc.getAsset(asset.getType());
        if (assetConfig != null && assetConfig.type == IAsset.MONITORED_ASSET) {
          return new Gson()
              .fromJson(AssetUtil.getAssetRelations(manufacturerId, assetId), JsonElement.class);
        } else {
          try {
            IAssetRelation assetRelation = assetManagementService.getAssetRelationByRelatedAsset(asset.getId());
            IAsset relatedAsset = assetManagementService.getAsset(assetRelation.getAssetId());
            return assetBuilder.buildAssetDetailsModel(relatedAsset, null, null, locale, timezone);
          } catch (Exception e) {
            return null;
          }
        }
      } else {
        throw new ServiceException(
            "Asset system configuration not available, please contact administrator.");
      }
    } catch (Exception e) {
      xLogger.severe("Error while getting relationship for the asset {0}, {1}", manufacturerId,
          assetId, e);
      throw new InvalidServiceException(
          backendMessages.getString("asset.relation.fetch.domain.error"));
    }
  }

  @RequestMapping(value = "/filter", method = RequestMethod.GET)
  public
  @ResponseBody
  Results getFilteredEntity(@RequestParam(required = false) Long eid,
                            @RequestParam(required = false) String q,
                            @RequestParam(required = false) String at,
                            @RequestParam(required = false, defaultValue = "true") Boolean all,
                            @RequestParam(required = false, defaultValue = "false") Boolean ns) {
    Long domainId = SecurityUtils.getCurrentDomainId();
    if (q == null) {
      q = "";
    }
    q = q.toLowerCase();
    q = AssetUtil.decodeURLParameters(q);
    List<AssetModel> assetModels = new ArrayList<>(1);
    try {
      List<IAsset> assets = assetManagementService.getAssets(domainId, eid, q, at, all);
      if (assets != null && !assets.isEmpty()) {
        assetModels = assetBuilder.buildFilterModels(assets);
      }

      if (ns && assetModels.size() < 10) {
        DomainConfig dc = DomainConfig.getInstance(domainId);
        AssetConfig assetConfig = dc.getAssetConfig();
        if (assetConfig != null && assetConfig.getNamespace() != null && !assetConfig.getNamespace()
            .isEmpty()) {
          assetModels.addAll(assetBuilder.buildAssetFilterModel(AssetUtil
              .getAssetsByTag(assetConfig.getNamespace(), q, at, null, null, null, null, 1, 10)));
        }
      }
    } catch (Exception e) {
      xLogger.warn("Exception: {0}", e.getMessage());
    }

    return new Results<>(assetModels, null);
  }

  @RequestMapping(value = "/relations", method = RequestMethod.POST)
  public
  @ResponseBody
  void createOrUpdateAssetRelations(@RequestBody AssetRelationModel assetRelationModel,
                                    @RequestParam(required = false, defaultValue = "false") Boolean delete) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    Long domainId = sUser.getCurrentDomainId();
    try {
      if (delete) {
        AssetUtil.createOrUpdateAssetRelations(new Gson().toJson(assetRelationModel));
        for (AssetRelationModel.AssetRelations assetRelations : assetRelationModel.data) {
          IAsset asset = assetManagementService.getAsset(assetRelations.vId, assetRelations.dId);
          assetManagementService.deleteAssetRelation(asset.getId(), domainId, asset);
        }
      } else {
        AssetUtil.createOrUpdateAssetRelations(new Gson().toJson(assetRelationModel));
        List<IAssetRelation>
            assetRelationList =
            assetBuilder.buildAssetRelations(assetRelationModel);
        for (IAssetRelation assetRelation : assetRelationList) {
          assetManagementService.createOrUpdateAssetRelation(domainId, assetRelation);
        }
      }
    } catch (Exception e) {
      xLogger.severe("Error while updating relationship for the asset {0}",
          assetRelationModel.toString(), e);
      throw new InvalidServiceException(backendMessages.getString("asset.relation.create.error"));
    }
  }

  @RequestMapping(value = "/temperature/{vendorId}/{deviceId}/{mpOrSensorId}", method = RequestMethod.GET)
  public
  @ResponseBody
  DeviceTempsModel getTemperature(@PathVariable String vendorId, @PathVariable String deviceId,
                                  @PathVariable String mpOrSensorId,
                                  @RequestParam("size") Integer size,
                                  @RequestParam("sint") Integer samplingInt,
                                  @RequestParam(value = "edate", required = false) String endDate,
                                  @RequestParam(value = "at", defaultValue = "1") Integer assetType) {
    String timezone = SecurityUtils.getTimezone();
    deviceId = AssetUtil.decodeURLParameters(deviceId);
    long end = LocalDateUtil.getCurrentTimeInSeconds(timezone);
    //long end = -1;

    if (endDate != null && !endDate.isEmpty()) {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
      Calendar cal = GregorianCalendar.getInstance(TimeZone.getTimeZone(timezone));
      try {
        cal.setTime(sdf.parse(endDate));
        cal.add(Calendar.DATE, 1);
        LocalDateUtil.resetTimeFields(cal);
        end = cal.getTimeInMillis() / 1000;
      } catch (ParseException e) {
        xLogger.warn("Exception while parsing end date", e);
      }
    }

    return assetBuilder.buildAssetTemperatures(AssetUtil
            .getTemperatureResponse(vendorId, deviceId, mpOrSensorId, assetType, 1, end, 1, size),
        (endDate != null && !endDate.isEmpty()) ? end : 0, samplingInt, timezone);
  }

  @RequestMapping(value = "/config/{vendorId}/{deviceId}", method = RequestMethod.GET)
  public
  @ResponseBody
  AssetModels.AssetConfigResponseModel getAssetConfig(@PathVariable String vendorId,
                                                      @PathVariable String deviceId) {
    deviceId = AssetUtil.decodeURLParameters(deviceId);
    return new Gson().fromJson(AssetUtil.getConfig(vendorId, deviceId),
        AssetModels.AssetConfigResponseModel.class);
  }

  @RequestMapping(value = "/config", method = RequestMethod.POST)
  public
  @ResponseBody
  void updateAssetConfig(@RequestBody AssetModels.AssetConfigModel assetConfigModel,
                         @RequestParam(defaultValue = "false") Boolean pushConfig)
      throws ServiceException, ConfigurationException {
    if (assetConfigModel != null) {
      AssetUtil.registerConfig(new Gson().toJson(assetConfigModel));

      if (pushConfig && assetConfigModel.configuration != null
          && assetConfigModel.configuration.getComm() != null) {
        DeviceConfigPushPullModel deviceConfigPushPullModel = new DeviceConfigPushPullModel(
            assetConfigModel.vId, assetConfigModel.dId);
        deviceConfigPushPullModel.stub = SecurityUtils.getUsername();
        IAsset asset = assetManagementService.getAsset(assetConfigModel.vId, assetConfigModel.dId);
        if (AssetSystemConfig.getInstance().isConfigPullEnabled(asset.getType(),
            asset.getVendorId(), asset.getModel())
            && assetConfigModel.configuration.getComm().getChnl()
            == IAsset.COMM_CHANNEL_INTERNET) {
          deviceConfigPushPullModel.typ = IAsset.COMM_CHANNEL_SMS;
        }
        AssetUtil.pushDeviceConfig(new Gson().toJson(deviceConfigPushPullModel));
      }
    }
  }

  @RequestMapping(value = "/stats/{vendorId}/{deviceId}", method = RequestMethod.GET)
  public
  @ResponseBody
  AssetModels.DeviceStatsModel getStats(@PathVariable("vendorId") String vendorId,
                                        @PathVariable("deviceId") String deviceId,
                                        @RequestParam("from") String from,
                                        @RequestParam("to") String to) {
    deviceId = AssetUtil.decodeURLParameters(deviceId);
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    String timezone = sUser.getTimezone();
    Locale locale = sUser.getLocale();
    return assetBuilder
        .buildDeviceStatsModel(AssetUtil.getAssetStats(vendorId, deviceId, from, to), locale,
            timezone);
  }

  @RequestMapping(value = "/alerts/recent/{vendorId}/{deviceId}", method = RequestMethod.GET)
  public
  @ResponseBody
  AssetModels.TempDeviceRecentAlertsModel getRecentAlerts(@PathVariable("vendorId") String vendorId,
                                                          @PathVariable("deviceId") String deviceId,
                                                          @RequestParam("page") String page,
                                                          @RequestParam("size") String size) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    String timezone = sUser.getTimezone();
    deviceId = AssetUtil.decodeURLParameters(deviceId);
    return assetBuilder
        .buildTempDeviceRecentAlertsModel(AssetUtil.getRecentAlerts(vendorId, deviceId, page, size),
            locale, timezone);
  }

  @RequestMapping(value = "/tags/child", method = RequestMethod.GET)
  public
  @ResponseBody
  String getChildTagSummary(@RequestParam("tagid") String tagId) {
    return AssetUtil.getChildTagSummary(tagId);
  }

  @RequestMapping(value = "/tags/abnormal", method = RequestMethod.GET)
  public
  @ResponseBody
  String getTagAbnormalDevices(@RequestParam("tagid") String tagId) {
    return AssetUtil.getTagAbnormalDevices(tagId);
  }

  @RequestMapping(value = "/domain/location", method = RequestMethod.GET)
  public
  @ResponseBody
  TemperatureDomainModel getDomainLocation() {
    DomainConfig dc = DomainConfig.getInstance(SecurityUtils.getCurrentDomainId());
    String topLocation = "India";
    String countryCode = dc != null && dc.getCountry() != null ? dc.getCountry() : "IN";
    String configuredState = dc != null ? dc.getState() : null;

    try {
      String strCountries = configurationMgmtService.getConfiguration(IConfig.LOCATIONS).getConfig();

      if (strCountries != null && !strCountries.isEmpty()) {
        JSONObject jsonObject = new JSONObject(strCountries);

        if (configuredState != null && !configuredState.isEmpty()) {
          topLocation = configuredState;
        } else {
          topLocation =
              jsonObject.getJSONObject("data").getJSONObject(countryCode).getString("name");
        }
      }
    } catch (Exception e) {
      //do nothing
    }
    return assetBuilder.buildTemperatureDomainModel(topLocation, countryCode, configuredState);
  }

  @RequestMapping(value = "/tags", method = RequestMethod.GET)
  public
  @ResponseBody
  String getTagSummary(@RequestParam("tagid") String tagId) {
    return AssetUtil.getTagSummary(tagId);
  }

  @RequestMapping(value = "/device/config", method = RequestMethod.POST)
  public
  @ResponseBody
  String pushPullDeviceConfig(@RequestBody DeviceConfigPushPullModel configModel) throws ServiceException {
    configModel.stub = SecurityUtils.getUsername();
    String json = new Gson().toJson(configModel);
    try {
      AssetUtil.pushDeviceConfig(json);
    } catch (ServiceException e) {
      throw new InvalidServiceException(e.getMessage());
    }
    return "";
  }

  @RequestMapping(value = "/delete", method = RequestMethod.POST)
  public
  @ResponseBody
  String deleteAsset(@RequestBody AssetModels.AssetsDeleteModel assetsDeleteModel) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    String userId = sUser.getUsername();
    final Long domainId = sUser.getCurrentDomainId();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    if (assetsDeleteModel != null) {
      Map<String, List<String>> deleteModel = new HashMap<>(5);
      for (AssetModel assetModel : assetsDeleteModel.data) {
        if (!deleteModel.containsKey(assetModel.vId)) {
          deleteModel.put(assetModel.vId, new ArrayList<>(1));
        }
        deleteModel.get(assetModel.vId).add(assetModel.dId);
        assetModel.tags = new ArrayList<String>() {{
          add("DELETED" + "." + domainId);
        }};
        assetModel.ub = userId;

        try {
          IAsset asset = assetManagementService.getAsset(assetModel.vId, assetModel.dId);
          AssetUtil.deleteRelationShip(asset, domainId);
        } catch (Exception e) {
          xLogger.warn("Error deleting asset relationship: " + assetsDeleteModel.toString(), e);
          throw new InvalidServiceException(backendMessages.getString("asset.delete.error"));
        }
      }

      for (String key : deleteModel.keySet()) {
        try {
          assetManagementService.deleteAsset(key, deleteModel.get(key), domainId);
        } catch (Exception e) {
          xLogger.warn("Error deleting asset: " + assetsDeleteModel.toString(), e);
          throw new InvalidServiceException(backendMessages.getString("asset.delete.error"));
        }
      }
      try {
        AssetUtil.registerDevices(
            new Gson().toJson(new AssetModels.AssetRegistrationModel(assetsDeleteModel.data)));
      } catch (ServiceException e) {
        xLogger.warn("Error deleting asset: " + assetsDeleteModel.toString(), e);
        throw new InvalidServiceException(backendMessages.getString("asset.delete.error"));
      }
    }

    return backendMessages.getString("one.or.more.asset") + " " + backendMessages.getString("delete.success");
  }

  @RequestMapping(value = "/model", method = RequestMethod.GET)
  public
  @ResponseBody
  List<String> getModelSuggestions(@RequestParam(required = false) String query) throws ServiceException {
    try {
      Long domainId = SecurityUtils.getCurrentDomainId();
      return assetManagementService.getModelSuggestion(domainId, query);
    } catch (Exception e) {
      xLogger.warn("Error while getting model suggestions", e);
    }
    return null;
  }

  @RequestMapping(value = "/get-by-ids", method = RequestMethod.POST)
  public
  @ResponseBody
  List<AssetBaseModel> getAssets(@RequestBody AssetDataModel tempData)
      throws ServiceException {
    return assetBuilder.buildAssets(tempData);
  }

  @RequestMapping(value = "/export", method = RequestMethod.POST)
  public
  @ResponseBody
  String exportData(@RequestBody String json) throws ParseException, ServiceException {
    ExportModel eModel = assetBuilder.buildExportModel(json);
    long jobId = exportService.scheduleExport(eModel, "status.assets");
    ResourceBundle backendMessages = Resources.getBundle(Locale.ENGLISH);
    IUserAccount u = usersService.getUserAccount(SecurityUtils.getUsername());
    return backendMessages.getString("export.success1") + " " + u.getEmail() + " "
        + backendMessages.getString("export.success2") + " "
        + backendMessages.getString("exportstatusinfo2") + " "
        + jobId + ". "
        + backendMessages.getString("exportstatusinfo1");
  }

  @RequestMapping(value = "/{vendorId}/{deviceId}/status", method = RequestMethod.POST)
  public
  @ResponseBody
  String updateWorkingStatus(@PathVariable String vendorId,
                             @PathVariable String deviceId,
                             @RequestBody final AssetModels.AssetStatus assetStatusModel) throws ServiceException {
    ResourceBundle backendMessages = Resources.getBundle(SecurityUtils.getUserDetails().getLocale());
    deviceId = AssetUtil.decodeURLParameters(deviceId);
    assetStatusModel.stub = SecurityUtils.getUsername();
    IAsset asset = assetManagementService.getAsset(vendorId, deviceId);
    assetManagementService.updateWorkingStatus(asset, assetStatusModel);
    return backendMessages.getString("asset.status.update.success");
  }
}
