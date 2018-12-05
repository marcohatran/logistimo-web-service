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
import com.logistimo.api.builders.AssetDashboardBuilder;
import com.logistimo.api.builders.DashboardBuilder;
import com.logistimo.api.builders.mobile.MobileInvDashboardBuilder;
import com.logistimo.api.models.AssetDashboardModel;
import com.logistimo.api.models.MainDashboardModel;
import com.logistimo.api.models.mobile.DashQueryModel;
import com.logistimo.api.models.mobile.MobileInvDashboardDetails;
import com.logistimo.api.models.mobile.MobileInvDashboardModel;
import com.logistimo.api.util.SearchUtil;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.config.models.AssetSystemConfig;
import com.logistimo.config.models.ConfigurationException;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.constants.CharacterConstants;
import com.logistimo.constants.Constants;
import com.logistimo.dashboards.service.IDashboardService;
import com.logistimo.exception.InvalidServiceException;
import com.logistimo.logger.XLog;
import com.logistimo.services.ServiceException;
import com.logistimo.services.cache.MemcacheService;
import com.logistimo.utils.LocalDateUtil;
import com.logistimo.utils.StringUtil;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

/**
 * Created by kumargaurav on 13/04/17.
 */

@Controller
@RequestMapping("/dashboards")
public class DashboardControllerMV1 {

  public static final String DISTRICT = "DISTRICT";
  public static final String STATE = "STATE";
  public static final String STATE_LOWER = "state";
  public static final String DISTRICT_LOWER = "district";
  public static final String COUNTRY_LOWER = "country";
  public static final String ACTIVITY = "activity";
  public static final String ALL_ACTIVITY = "all_activity";
  public static final String ALL_INV = "all_inv";
  public static final String INV = "inv";
  public static final String ASSET = "asset";
  public static final String STATUS = "status";
  public static final String PERIOD = "period";
  private static final XLog xLogger = XLog.getLog(DashboardControllerMV1.class);

  private IDashboardService dashboardService;
  private DashboardBuilder dashboardBuilder;
  private AssetDashboardBuilder assetDashboardBuilder;

  private MemcacheService memcacheService;

  private MemcacheService getMemcacheService() {
    if (memcacheService == null) {
      memcacheService = AppFactory.get().getMemcacheService();
    }
    return memcacheService;
  }

  @Autowired
  public void setDashboardService(IDashboardService dashboardService) {
    this.dashboardService = dashboardService;
  }

  @Autowired
  public void setDashboardBuilder(DashboardBuilder dashboardBuilder) {
    this.dashboardBuilder = dashboardBuilder;
  }

  @Autowired
  public void setAssetDashboardBuilder(AssetDashboardBuilder assetDashboardBuilder) {
    this.assetDashboardBuilder = assetDashboardBuilder;
  }

  @RequestMapping(value = "/inventory", method = RequestMethod.GET)
  public
  @ResponseBody
  MobileInvDashboardModel getInvDashBoard(
      @RequestParam(value = "incetags", required = false) String incetags,
      @RequestParam(value = "exetags", required = false) String exetags,
      @RequestParam(value = "mtags", required = false) String mtags,
      @RequestParam(value = "mnm", required = false) String mnm,
      @RequestParam(value = "loc", required = false) String loc,
      @RequestParam(value = "locty", required = false) String locty,
      @RequestParam(value = "p", required = false) Integer p,
      @RequestParam(value = "date", required = false) String date,
      @RequestParam(value = "refresh", required = false, defaultValue = "false") Boolean refresh)
      throws SQLException {

    long domainId = SecurityUtils.getCurrentDomainId();
    DomainConfig dc = DomainConfig.getInstance(domainId);
    MobileInvDashboardModel dashboardModel;
    if (dc.getCountry() == null) {
      return new MobileInvDashboardModel();
    }
    String country = dc.getCountry();
    String state = null;
    String district = null;
    if (StringUtils.isNotEmpty(dc.getDistrict())) {
      state = dc.getState();
      district = dc.getDistrict();
    } else if (StringUtils.isNotEmpty(dc.getState())) {
      state = dc.getState();
    }
    DashQueryModel paramModel = new DashQueryModel(country, state, district, incetags, exetags,
        mtags, mnm, loc, p, date, domainId, null, null);
    String
        cachekey =
        buildCacheKey(paramModel);
    MemcacheService cache = getMemcacheService();
    //getting results for cache
    if (!refresh) {
      dashboardModel = (MobileInvDashboardModel) cache.get(cachekey);
      if (dashboardModel != null) {
        return dashboardModel;
      }
    }
    paramModel.timezone = dc.getTimezone();
    paramModel.locty = locty;
    Map<String, String>
        filters =
        buildQueryFilters(paramModel);
    try (
        ResultSet invTyRes =
            dashboardService.getMainDashboardResults(domainId, filters, INV, true, null);
        ResultSet invAlRes =
            dashboardService.getMainDashboardResults(domainId, filters, ALL_INV, true, null);
        ResultSet acstRes =
            dashboardService.getMainDashboardResults(domainId, filters, ACTIVITY, true, null);
        ResultSet alstRes =
            dashboardService.getMainDashboardResults(domainId, filters, ALL_ACTIVITY, true, null);
    ) {
      //preparing the model
      dashboardModel =
          MobileInvDashboardBuilder.buildInvDashboard(invTyRes, invAlRes, acstRes, alstRes);
      dashboardModel.setGeneratedTime(getGeneratedTime());
      if (cache != null) {
        cache.put(cachekey, dashboardModel, 1800); // 30 min expiry
      }
    }

    return dashboardModel;
  }


  @RequestMapping(value = "/inventory/breakdown", method = RequestMethod.GET)
  public
  @ResponseBody
  MobileInvDashboardDetails getInvDashBoardDetail(
      @RequestParam(value = "incetags", required = false) String incetags,
      @RequestParam(value = "exetags", required = false) String exetags,
      @RequestParam(value = "mtags", required = false) String mtags,
      @RequestParam(value = "mnm", required = false) String mnm,
      @RequestParam(value = "loc", required = false) String loc,
      @RequestParam(value = "locty", required = false) String locty,
      @RequestParam(value = "p", required = false) Integer p,
      @RequestParam(value = "date", required = false) String date,
      @RequestParam(value = "groupby", required = true) String groupby,
      @RequestParam(value = "refresh", required = false, defaultValue = "false") Boolean refresh)
      throws SQLException {

    long domainId = SecurityUtils.getCurrentDomainId();
    DomainConfig dc = DomainConfig.getInstance(domainId);
    MobileInvDashboardDetails details;
    String country = dc.getCountry();
    String state = null;
    String district = null;
    if (StringUtils.isNotEmpty(dc.getDistrict())) {
      state = dc.getState();
      district = dc.getDistrict();
    } else if (StringUtils.isNotEmpty(dc.getState())) {
      state = dc.getState();
    }
    DashQueryModel paramModel = new DashQueryModel(country, state, district, incetags, exetags,
        mtags, mnm, loc, p, date, domainId, groupby, null);
    String
        cachekey =
        buildCacheKey(paramModel);
    MemcacheService cache = getMemcacheService();
    //getting results for cache
    if (!refresh) {
      details = (MobileInvDashboardDetails) cache.get(cachekey);
      if (details != null) {
        return details;
      }
    }
    paramModel.timezone = dc.getTimezone();
    paramModel.locty = locty;
    Map<String, String>
        filters =
        buildQueryFilters(paramModel);
    try (
        ResultSet invTyRes =
            dashboardService.getMainDashboardResults(domainId, filters, INV, false, groupby);
        ResultSet invAlRes =
            dashboardService.getMainDashboardResults(domainId, filters, ALL_INV, false, groupby);
        ResultSet alstRes =
            dashboardService.getMainDashboardResults(domainId, filters, ALL_ACTIVITY, false, null);
    ) {
      //preparing the model
      details =
          MobileInvDashboardBuilder
              .buildInvDetailDashboard(invTyRes, invAlRes, alstRes, paramModel.locty, groupby);
    }
    //adding loc level in response
    if (StringUtils.isBlank(loc) && paramModel.state == null) {
      details.level = COUNTRY_LOWER;
    } else if (StringUtils.isBlank(loc) && paramModel.district == null) {
      details.level = STATE_LOWER;
    } else {
      details.level = paramModel.locty != null ? paramModel.locty : DISTRICT_LOWER;
    }
    details.setGeneratedTime(getGeneratedTime());
    if (cache != null) {
      cache.put(cachekey, details, 1800); // 30 min expiry
    }

    return details;
  }

  @RequestMapping(value = "/assets/temperature", method = RequestMethod.GET)
  public
  @ResponseBody
  MainDashboardModel getAssetDashboard(@RequestParam(required = false) String filter,
                                       @RequestParam(required = false) String level,
                                       @RequestParam(required = false) String tPeriod,
                                       @RequestParam(required = false) String aType,
                                       @RequestParam(required = false) String excludeETag,
                                       @RequestParam(required = false) String includeETag,
                                       @RequestParam(required = false, defaultValue = "false")
                                       Boolean refresh) throws SQLException {
    MainDashboardModel model;
    Long domainId = SecurityUtils.getCurrentDomainId();
    MemcacheService cache = getMemcacheService();
    DomainConfig dc = DomainConfig.getInstance(domainId);
    String country = dc.getCountry();
    String state = null;
    String district = null;
    if (StringUtils.isNotEmpty(dc.getDistrict())) {
      state = dc.getState();
      district = dc.getDistrict();
    } else if (StringUtils.isNotEmpty(dc.getState())) {
      state = dc.getState();
    }
    DashQueryModel
        queryModel =
        new DashQueryModel(country, state, district, excludeETag, filter, tPeriod, domainId, aType,
            level, includeETag);
    String cacheKey = buildCacheKey(queryModel);
    cacheKey += "_AD";
    if (!refresh) {
      model = (MainDashboardModel) cache.get(cacheKey);
      if (model != null) {
        return model;
    }
    }
    Map<String, String> filters = buildQueryFilters(queryModel);
    level = queryModel.locty;
    String colFilter = getLocationFilter(level, queryModel);
    ResultSet tempRes = dashboardService.getMainDashboardResults(domainId, filters, "temperature");
    model = dashboardBuilder.getMainDashBoardData(null, null, null, null, tempRes, null,
        colFilter);
    model.mLev = getLevel(filter, level, queryModel);
    model.setGeneratedTime(getGeneratedTime());
    if (cache != null) {
      cache.put(cacheKey, model, 1800); // 30 min expiry
    }
    return model;
  }

  private String getLevel(@RequestParam(required = false) String filter,
                          @RequestParam(required = false) String level, DashQueryModel queryModel) {
    if (StringUtils.isBlank(filter) && queryModel.state == null) {
      return COUNTRY_LOWER;
    } else if (StringUtils.isBlank(filter) && queryModel.district == null) {
      return STATE_LOWER;
    } else {
      return level != null ? level : DISTRICT_LOWER;
    }
  }

  private String getLocationFilter(@RequestParam(required = false) String level,
                                   DashQueryModel queryModel) {
    String colFilter;
    if (DISTRICT_LOWER.equals(level) || queryModel.district != null) {
      colFilter = "NAME";
    } else if (STATE_LOWER.equals(level) || queryModel.state != null) {
      colFilter = DISTRICT;
    } else {
      colFilter = STATE;
    }
    return colFilter;
  }

  /**
   * Provide statistics related to active/inactive entities which contains total count
   * and the detail breakdown by location wise
   *
   * @param incetags Entity tags to be included
   * @param exetags  Entity tags to be excluded
   * @param mtags    Material tags to be included
   * @param mnm      Material name
   * @param loc      Location
   * @param locty    Location type: country/state/district
   * @param p        Period in days
   * @param date     Day of activity to be viewed
   * @param refresh  true/false; whether to use cached data or not
   */
  @RequestMapping(value = "/activity", method = RequestMethod.GET)
  public
  @ResponseBody
  MainDashboardModel getActivityDashboard(@RequestParam(required = false) String incetags,
                                          @RequestParam(required = false) String exetags,
                                          @RequestParam(required = false) String mtags,
                                          @RequestParam(required = false) String mnm,
                                          @RequestParam(required = false) String loc,
                                          @RequestParam(required = false) String locty,
                                          @RequestParam(required = false) Integer p,
                                          @RequestParam(required = false) String date,
                                          @RequestParam(required = false, defaultValue = "false") Boolean refresh)
      throws SQLException {
    MainDashboardModel model;
    Long domainId = SecurityUtils.getCurrentDomainId();
    DomainConfig dc = DomainConfig.getInstance(domainId);
    String country = dc.getCountry();
    String state = null;
    String district = null;
    if (StringUtils.isNotEmpty(dc.getDistrict())) {
      state = dc.getState();
      district = dc.getDistrict();
    } else if (StringUtils.isNotEmpty(dc.getState())) {
      state = dc.getState();
    }
    DashQueryModel
        activityQueryModel =
        new DashQueryModel(country, state, district, incetags, exetags,
            mtags, mnm, loc, p, date, domainId, null, locty);
    String cacheKey = buildCacheKey(activityQueryModel);
    cacheKey += "_ACT";
    MemcacheService cache = getMemcacheService();
    if (!refresh) {
      model = (MainDashboardModel) cache.get(cacheKey);
      if (model != null) {
        return model;
      }
    }
    Map<String, String> filters = buildQueryFilters(activityQueryModel);
    String colFilter;
    colFilter = getLocationFilter(activityQueryModel.locty, activityQueryModel);
    ResultSet activity = dashboardService.getMainDashboardResults(domainId, filters, ACTIVITY);
    ResultSet
        activityDomain =
        dashboardService.getMainDashboardResults(domainId, filters, ALL_ACTIVITY);
    model =
        dashboardBuilder.getMainDashBoardData(null, null, activity, activityDomain, null, null,
            colFilter);
    if (StringUtils.isBlank(activityQueryModel.loc) && activityQueryModel.state == null) {
      model.mLev = COUNTRY_LOWER;
    } else if (StringUtils.isBlank(activityQueryModel.loc) && activityQueryModel.district == null) {
      model.mLev = STATE_LOWER;
    } else {
      model.mLev = activityQueryModel.locty;
    }
    model.setGeneratedTime(getGeneratedTime());
    if (cache != null) {
      cache.put(cacheKey, model, 1800);
    }
    return model;
  }

  /**
   * Provides statistics related to asset's statuses example the number of assets that are working or condemned for a given location
   */
  @RequestMapping(value = "/assets/status", method = RequestMethod.GET)
  public
  @ResponseBody
  AssetDashboardModel getAssetStatusDashboard(@RequestParam(required = false) String monitoringType,
                                              @RequestParam(required = false) String assetType,
                                              @RequestParam(required = false) String eTag,
                                              @RequestParam(required = false) String eeTag,
                                              @RequestParam(required = false) String status,
                                              @RequestParam(required = false) String period,
                                              @RequestParam(required = false) String level,
                                              @RequestParam(required = false) String location,
                                              @RequestParam(required = false, defaultValue = "false") Boolean skipCache)
      throws SQLException, ServiceException {
    AssetDashboardModel model;
    Long domainId = SecurityUtils.getCurrentDomainId();
    MemcacheService cache = getMemcacheService();
    DomainConfig dc = DomainConfig.getInstance(domainId);
    String country = dc.getCountry();
    String state = null;
    String district = null;
    if (StringUtils.isNotEmpty(dc.getDistrict())) {
      state = dc.getState();
      district = dc.getDistrict();
    } else if (StringUtils.isNotEmpty(dc.getState())) {
      state = dc.getState();
    }
    assetType = assetType != null ? assetType : getAssetType(monitoringType);

    if (StringUtils.isNotBlank(eTag)) {
      eTag = StringUtil.getSingleQuotesCSV(StringUtil.getList(eTag));
    } else if (StringUtils.isNotBlank(eeTag)) {
      eeTag = StringUtil.getSingleQuotesCSV(StringUtil.getList(eeTag));
    }

    DashQueryModel
        queryModel =
        new DashQueryModel(assetType, eTag, eeTag, status, period, country, state, district,
            domainId, level, location);
    String cacheKey = buildCacheKey(queryModel);
    if (!skipCache) {
      model = (AssetDashboardModel) cache.get(cacheKey);
      if (model != null) {
        return model;
      }
    }
    Map<String, String> filters = buildQueryFilters(queryModel);
    level = queryModel.locty;
    String columnFilter = getLocationFilter(level, queryModel);
    ResultSet assetRes = dashboardService.getMainDashboardResults(domainId, filters, ASSET);
    ResultSet assetOverallRes = null;
    if(StringUtils.isNotBlank(filters.get(STATUS))) {
      Map<String, String> assetsOverAllDashboardFilter = buildQueryFilters(queryModel);
      assetOverallRes = getAssetsOverAllData(domainId, assetsOverAllDashboardFilter);
    }
    model = assetDashboardBuilder.buildAssetDashboardModel(assetRes, assetOverallRes, columnFilter);
    setMapDrillDownLevel(level, location, model, dc, country, state, queryModel);
    model.setUpdatedTime(LocalDateUtil.getFormattedTimeStamp(getGeneratedTime(),
        SecurityUtils.getLocale(), SecurityUtils.getTimezone(), domainId));
    model.setGeneratedTime(getGeneratedTime());
    if (cache != null) {
      cache.put(cacheKey, model, 1800); // 30 min expiry
    }
    return model;
  }
  private ResultSet getAssetsOverAllData(Long domainId, Map<String, String> filters) {
      filters.remove(STATUS);
      filters.remove(PERIOD);
      return dashboardService.getMainDashboardResults(domainId, filters, ASSET);
  }

  private void setMapDrillDownLevel(@RequestParam(required = false) String level,
                                    @RequestParam(required = false) String location,
                                    AssetDashboardModel model, DomainConfig dc, String country,
                                    String state, DashQueryModel queryModel) {
    if (StringUtils.isBlank(location) && queryModel.state == null) {
      model.setlevel(COUNTRY_LOWER);
      model.setMapType(country);
    } else if (StringUtils.isBlank(location) && queryModel.district == null) {
      model.setlevel(STATE_LOWER);
      model.setMapTypeName(state);
      model.setMapType(dc.getState().replace(" ", ""));
      model.setmPTy(country);
    } else {
      level = level != null ? level : DISTRICT_LOWER;
      model.setlevel(level);
      String mtn = location != null ? location : dc.getDistrict();
      model.setMapTypeName(mtn);
      model.setMapType(model.getMapTypeName().replace(" ", ""));
      model.setmPTy(country);
    }
  }

  private String getAssetType(@RequestParam(required = false) String monitoringType) {
    if (monitoringType != null) {
      try {
        AssetSystemConfig assets = AssetSystemConfig.getInstance();
        return assets.getAssetsByMonitoringType(Integer.valueOf(monitoringType));
      } catch (ConfigurationException e) {
        xLogger.severe("Error in reading Asset System Configuration", e);
        throw new InvalidServiceException("Error in reading asset meta data.");
      }
    }
    return null;
  }


  private String buildCacheKey(DashQueryModel model) {

    String cacheKey = Constants.MDASHBOARD_CACHE_PREFIX + String.valueOf(model.domainId);
    cacheKey += CharacterConstants.UNDERSCORE + model.country;
    if (StringUtils.isNotEmpty(model.district)) {
      cacheKey += CharacterConstants.UNDERSCORE + model.state;
      cacheKey += CharacterConstants.UNDERSCORE + model.district;
    } else if (StringUtils.isNotEmpty(model.state)) {
      cacheKey += CharacterConstants.UNDERSCORE + model.state;
    }
    if (null != model.domainId) {
      cacheKey += CharacterConstants.UNDERSCORE + model.domainId;
    }
    if (StringUtils.isNotEmpty(model.loc)) {
      cacheKey += CharacterConstants.UNDERSCORE + model.loc;
    }
    if (StringUtils.isNotEmpty(model.mtags)) {
      cacheKey += CharacterConstants.UNDERSCORE + model.mtags;
    }
    if (StringUtils.isNotEmpty(model.mnm)) {
      cacheKey += CharacterConstants.UNDERSCORE + model.mnm;
    }
    if (model.p != null) {
      cacheKey += CharacterConstants.UNDERSCORE + model.p;
    }
    if (StringUtils.isNotEmpty(model.incetags)) {
      cacheKey += CharacterConstants.UNDERSCORE + model.incetags;
    }
    if (StringUtils.isNotEmpty(model.exetags)) {
      cacheKey += CharacterConstants.UNDERSCORE + "E" + model.exetags;
    }
    if (StringUtils.isNotBlank(model.date)) {
      cacheKey += CharacterConstants.UNDERSCORE + model.date;
    }
    if (StringUtils.isNotBlank(model.groupby)) {
      cacheKey += CharacterConstants.UNDERSCORE + model.groupby;
    }
    if (StringUtils.isNotBlank(model.aty)) {
      cacheKey += "_A" + model.aty;
    }
    if (StringUtils.isNotBlank(model.tp)) {
      cacheKey += "_T" + model.tp;
    }
    if (StringUtils.isNotBlank(model.status) && StringUtils.isNotBlank(model.period)) {
      cacheKey += CharacterConstants.UNDERSCORE.concat("S").concat(model.status).concat(
          CharacterConstants.UNDERSCORE).concat(model.period);
    }

    return cacheKey;
  }

  private Map<String, String> buildQueryFilters(DashQueryModel model) {

    if (StringUtils.isBlank(model.locty)) {
      model.locty = "";
    }
    Map<String, String> filters = new HashMap<>(1);
    buildLocationQueryFilters(model, filters);
    if (StringUtils.isNotEmpty(model.mtags)) {
      filters.put("mTag", model.mtags);
    } else if (StringUtils.isNotEmpty(model.mnm)) {
      filters.put("mId", model.mnm);
    }
    if (model.p != null) {
      filters.put("period", String.valueOf(model.p));
    }
    if (StringUtils.isNotEmpty(model.incetags)) {
      filters.put("eTag", model.incetags);
    }
    if (StringUtils.isNotEmpty(model.exetags)) {
      filters.put("eeTag", model.exetags);
    }
    if (StringUtils.isNotEmpty(model.tp)) {
      filters.put("tPeriod", model.tp);
    }
    if (StringUtils.isNotEmpty(model.aty)) {
      filters.put("type", model.aty);
    }
    buildDateQueryFilter(model, filters);
    if (model.domainId != null) {
      filters.put("domainId", String.valueOf(model.domainId));
    }
    if (StringUtils.isNotBlank(model.status)) {
      filters.put("status", model.status);
    }
    if (StringUtils.isNotEmpty(model.period)) {
      filters.put("period", model.period);
    }

    return filters;
  }

  private void buildDateQueryFilter(DashQueryModel model, Map<String, String> filters) {
    if (StringUtils.isNotBlank(model.date)) {
      try {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date
            startDate =
            LocalDateUtil.parseCustom(model.date, Constants.DATE_FORMAT, model.timezone);
        //Increment date to use less than queries
        Calendar calendar;
        if (model.timezone != null) {
          calendar = GregorianCalendar.getInstance(TimeZone.getTimeZone(model.timezone));
        } else {
          calendar = GregorianCalendar.getInstance();
        }
        calendar.setTime(startDate);
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        calendar.add(Calendar.MILLISECOND, -1);
        filters.put("date", sdf.format(calendar.getTime()));
      } catch (Exception e) {
        xLogger.warn("M Dashboard: Exception when parsing start date {0} , domain: {1}", model.date,
            model.domainId, e);
        throw new InvalidServiceException("Unable to parse date " + model.date);
      }
    }
  }

  private void buildLocationQueryFilters(DashQueryModel model, Map<String, String> filters) {
    filters.put(COUNTRY_LOWER, model.country);
    if (StringUtils.isBlank(model.loc) && model.district != null) {
      filters.put(DISTRICT_LOWER, model.district);
      filters.put(STATE_LOWER, model.state);
      //adding this
      model.locty = DISTRICT_LOWER;
    } else if (StringUtils.isBlank(model.loc) && model.state != null) {
      filters.put(STATE_LOWER, model.state);
      boolean noDistrict = !SearchUtil.isDistrictAvailable(model.country, model.state);
      if (noDistrict) {
        model.locty = DISTRICT_LOWER;
        filters.put(DISTRICT_LOWER, "");
        model.district = model.state;
      } else {
        model.locty = STATE_LOWER;
      }
    } else if (STATE_LOWER.equals(model.locty)) {
      filters.put(STATE_LOWER, model.loc);
      boolean noDistrict = !SearchUtil.isDistrictAvailable(model.country, model.loc);
      if (noDistrict) {
        model.locty = DISTRICT_LOWER;
        filters.put(DISTRICT_LOWER, "");
      }
    } else if (DISTRICT_LOWER.equals(model.locty)) {
      String[] locarr = model.loc.split(CharacterConstants.UNDERSCORE);
      model.state = locarr[0];
      model.district = locarr[1];
      filters.put(STATE_LOWER, model.state);
      filters.put(DISTRICT_LOWER, model.district);
    }
  }

  private String getGeneratedTime() {
    SimpleDateFormat sdf = new SimpleDateFormat(Constants.ANALYTICS_DATE_FORMAT);
    return sdf.format(new Date());
  }

}
