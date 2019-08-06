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

import com.logistimo.AppFactory;
import com.logistimo.api.builders.DashboardBuilder;
import com.logistimo.api.models.DashboardModel;
import com.logistimo.api.models.MainDashboardModel;
import com.logistimo.api.models.SessionDashboardModel;
import com.logistimo.api.request.DBWUpdateRequest;
import com.logistimo.api.util.SearchUtil;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.constants.Constants;
import com.logistimo.dashboards.entity.IDashboard;
import com.logistimo.dashboards.service.IDashboardService;
import com.logistimo.exception.InvalidServiceException;
import com.logistimo.logger.XLog;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;
import com.logistimo.services.cache.MemcacheService;
import com.logistimo.utils.LocalDateUtil;
import com.logistimo.utils.MsgUtil;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TimeZone;

/**
 * @author Mohan Raja
 */
@Controller
@RequestMapping("/dashboard")
public class DashboardController {
  private static final XLog xLogger = XLog.getLog(DashboardController.class);

  private DashboardBuilder dashboardBuilder;
  private static final String ENT_INV_DASHBOARD = "en_inv";
  private static final String ENT_TEMP_DASHBOARD = "en_temp";

  private IDashboardService dashboardService;

  @Autowired
  public void setDashboardBuilder(DashboardBuilder dashboardBuilder) {
    this.dashboardBuilder = dashboardBuilder;
  }

  @Autowired
  public void setDashboardService(IDashboardService dashboardService) {
    this.dashboardService = dashboardService;
  }

  @RequestMapping(value = "/", method = RequestMethod.POST)
  public
  @ResponseBody
  String create(@RequestBody DashboardModel model) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    long domainId = sUser.getCurrentDomainId();
    try {
      IDashboard db = dashboardBuilder.buildDashboard(model, sUser.getUsername());
      dashboardService.createDashboard(db);
    } catch (ServiceException e) {
      xLogger.severe("Error creating Dashboard for domain ", domainId);
      throw new InvalidServiceException("Error creating Dashboard for " + domainId);
    }
    return "Dashboard " + MsgUtil.bold(model.nm) + " " + backendMessages
        .getString("created.success");
  }

  @RequestMapping(value = "/all", method = RequestMethod.GET)
  public
  @ResponseBody
  List<DashboardModel> getAll() {
    List<IDashboard> dbList = dashboardService.getDashBoards(SecurityUtils.getCurrentDomainId());
    return dashboardBuilder.buildDashboardModelList(dbList);
  }

  @RequestMapping(value = "/{dbId}", method = RequestMethod.GET)
  public
  @ResponseBody
  DashboardModel get(@PathVariable Long dbId) {
    try {
      IDashboard db = dashboardService.getDashBoard(dbId);
      return dashboardBuilder.buildDashboardModel(db, true);
    } catch (ServiceException e) {
      xLogger.warn("Error in getting Dashboard {0}", dbId, e);
      throw new InvalidServiceException("Error in getting Dashboard " + dbId);
    }
  }

  @RequestMapping(value = "/", method = RequestMethod.GET)
  public
  @ResponseBody
  MainDashboardModel getData(@RequestParam(required = false) String filter,
                             @RequestParam(required = false) String level,
                             @RequestParam(required = false) String extraFilter,
                             @RequestParam(required = false) String exType,
                             @RequestParam(required = false) String period,
                             @RequestParam(required = false) String tPeriod,
                             @RequestParam(required = false) String eTag,
                             @RequestParam(required = false) String aType,
                             @RequestParam(required = false) String date,
                             @RequestParam(required = false) String excludeETag,
                             @RequestParam(required = false) Boolean skipCache) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Long domainId = sUser.getCurrentDomainId();
    MainDashboardModel model;
    try {
      MemcacheService cache = AppFactory.get().getMemcacheService();
      DomainConfig dc = DomainConfig.getInstance(domainId);
      if (dc.getCountry() == null) {
        return new MainDashboardModel();
      }
      if(skipCache==null){
        skipCache=false;
      }
      String stateFilter = null;
      String districtFilter = null;
      String cacheKey = Constants.DASHBOARD_CACHE_PREFIX + String.valueOf(domainId);
      String countryFilter = dc.getCountry();
      cacheKey += "_" + countryFilter;
      if (StringUtils.isNotEmpty(dc.getDistrict())) {
        stateFilter = dc.getState();
        cacheKey += "_" + stateFilter;
        districtFilter = dc.getDistrict();
        cacheKey += "_" + districtFilter;
      } else if (StringUtils.isNotEmpty(dc.getState())) {
        stateFilter = dc.getState();
        cacheKey += "_" + stateFilter;
      }
      if (StringUtils.isNotEmpty(filter)) {
        cacheKey += "_" + filter;
      }
      if (StringUtils.isNotEmpty(extraFilter)) {
        cacheKey += "_" + exType + "_" + extraFilter;
      }
      if (StringUtils.isNotEmpty(period)) {
        cacheKey += "_" + period;
      }
      if (StringUtils.isNotEmpty(tPeriod)) {
        cacheKey += "_T" + tPeriod;
      }
      if (StringUtils.isNotEmpty(aType)) {
        cacheKey += "_A" + aType;
      }
      if (StringUtils.isNotEmpty(eTag)) {
        cacheKey += "_" + eTag;
      }
      if (StringUtils.isNotBlank(excludeETag)) {
        cacheKey += "_E" + excludeETag;
      }
      if (StringUtils.isNotBlank(date)) {
        cacheKey += "_" + date;
      }

      if (cache != null && !skipCache) {
        model = (MainDashboardModel) cache.get(cacheKey);
        if (model != null) {
          model.ut = LocalDateUtil.getFormattedTimeStamp(model.ut, sUser.getLocale(),
              sUser.getTimezone(), domainId);
          return model;
        }
      }
      Map<String, String> filters = new HashMap<>(1);
      if (StringUtils.isBlank(filter) && districtFilter != null) {
        filters.put("district", districtFilter);
        filters.put("state", stateFilter);
      } else if (StringUtils.isBlank(filter) && stateFilter != null) {
        filters.put("state", stateFilter);
        boolean noDistrict = !SearchUtil.isDistrictAvailable(dc.getCountry(), stateFilter);
        if (noDistrict) {
          level = "district";
          filters.put("district", "");
          districtFilter = stateFilter;
        }
      } else if ("state".equals(level)) {
        filters.put("state", filter);
        boolean noDistrict = !SearchUtil.isDistrictAvailable(dc.getCountry(), filter);
        if (noDistrict) {
          level = "district";
          filters.put("district", "");
        }
      } else if ("district".equals(level)) {
        String[] f = filter.split("_");
        filters.put("state", f[0]);
        filters.put("district", f[1]);
      }
      filters.put("country", countryFilter);
      if (StringUtils.isNotEmpty(extraFilter)) {
        filters.put(exType, extraFilter);
      }
      if (StringUtils.isNotEmpty(period)) {
        filters.put("period", period);
      }
      if (StringUtils.isNotEmpty(tPeriod)) {
        filters.put("tPeriod", tPeriod);
      }
      if (StringUtils.isNotEmpty(aType)) {
        filters.put("type", aType);
      }
      if (StringUtils.isNotEmpty(eTag)) {
        filters.put("eTag", eTag);
      }
      if (StringUtils.isNotEmpty(excludeETag)) {
        filters.put("eeTag", excludeETag);
      }
      if (StringUtils.isNotBlank(date)) {
        try {
          SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
          Date startDate = LocalDateUtil.parseCustom(date, Constants.DATE_FORMAT, dc.getTimezone());
          //Increment date to use less than queries
          Calendar calendar;
          if (dc.getTimezone() != null) {
            calendar = GregorianCalendar.getInstance(TimeZone.getTimeZone(dc.getTimezone()));
          } else {
            calendar = GregorianCalendar.getInstance();
          }
          calendar.setTime(startDate);
          calendar.add(Calendar.DAY_OF_MONTH, 1);
          calendar.add(Calendar.MILLISECOND, -1);
          filters.put("date", sdf.format(calendar.getTime()));
        } catch (Exception e) {
          xLogger.warn("Dashboard: Exception when parsing start date {0} , domain: {1}", date,
              domainId, e);
          throw new InvalidServiceException("Unable to parse date " + date);
        }
      }

      ResultSet predRes = null;
      try (
          ResultSet eventRes = dashboardService.getMainDashboardResults(domainId, filters, "inv");
          ResultSet invRes = dashboardService.getMainDashboardResults(domainId, filters, "all_inv");
          ResultSet actRes =
              dashboardService.getMainDashboardResults(domainId, filters, "activity");
          ResultSet entRes =
              dashboardService.getMainDashboardResults(domainId, filters, "all_activity");
          ResultSet tempRes =
              dashboardService.getMainDashboardResults(domainId, filters, "temperature");
      ) {
        if (dc.getInventoryConfig() != null && dc.getInventoryConfig().showPredictions()) {
          predRes = dashboardService.getMainDashboardResults(domainId, filters, "all_predictive");
        }
        String colFilter;
        if ("district".equals(level) || districtFilter != null) {
          colFilter = "NAME";
        } else if ("state".equals(level) || stateFilter != null) {
          colFilter = "DISTRICT";
        } else {
          colFilter = "STATE";
        }
        model =
            dashboardBuilder
                .getMainDashBoardData(eventRes, invRes, actRes, entRes, tempRes, predRes,
                    colFilter);
      } finally {
        if (predRes != null) {
          predRes.close();
        }
      }
      if (StringUtils.isBlank(filter) && stateFilter == null) {
        model.mTy = dc.getCountry();
        model.mLev = "country";
      } else if (StringUtils.isBlank(filter) && districtFilter == null) {
        model.mTyNm = dc.getState();
        model.mTy = dc.getState().replace(" ", "");
        model.mPTy = dc.getCountry();
        model.mLev = "state";
      } else {
        model.mTyNm = filter != null ? filter : districtFilter;
        model.mTy = model.mTyNm.replace(" ", "");
        model.mPTy = dc.getCountry(); // Required only for state level, rest ignored
        model.mLev = level != null ? level : "district";
      }
      model.ut = getGeneratedTimestamp();
      try {
        if (cache != null) {
          cache.put(cacheKey, model, 1800); // 30 min expiry
        }
      } catch (Exception e) {
        xLogger.warn("Error in caching dashboard data", e);
      }
      model.ut = LocalDateUtil.getFormattedTimeStamp(model.ut, sUser.getLocale(),
          sUser.getTimezone(), domainId);
      return model;
    } catch (Exception e) {
      xLogger.warn("Error in getting main Dashboard for domain {0}", domainId, e);
      throw new InvalidServiceException("Error in getting main Dashboard for domain " + domainId);
    }
  }

  @RequestMapping(value = "/predictive", method = RequestMethod.GET)
  public
  @ResponseBody
  MainDashboardModel getPredictiveData(@RequestParam(required = false) String filter,
                                       @RequestParam(required = false) String level,
                                       @RequestParam(required = false) String extraFilter,
                                       @RequestParam(required = false) String exType,
                                       @RequestParam(required = false) String eTag,
                                       @RequestParam(required = false) String excludeETag,
                                       @RequestParam(required = false) Boolean skipCache) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Long domainId = sUser.getCurrentDomainId();
    MainDashboardModel model;
    try {
      MemcacheService cache = AppFactory.get().getMemcacheService();
      DomainConfig dc = DomainConfig.getInstance(domainId);
      if (skipCache == null) {
        skipCache = false;
      }
      if (dc.getCountry() == null) {
        return new MainDashboardModel();
      }
      String stateFilter = null;
      String districtFilter = null;
      String cacheKey = Constants.PREDICTIVE_DASHBOARD_CACHE_PREFIX + String.valueOf(domainId);
      String countryFilter = dc.getCountry();
      cacheKey += "_" + countryFilter;
      if (StringUtils.isNotEmpty(dc.getDistrict())) {
        stateFilter = dc.getState();
        cacheKey += "_" + stateFilter;
        districtFilter = dc.getDistrict();
        cacheKey += "_" + districtFilter;
      } else if (StringUtils.isNotEmpty(dc.getState())) {
        stateFilter = dc.getState();
        cacheKey += "_" + stateFilter;
      }
      if (StringUtils.isNotEmpty(filter)) {
        cacheKey += "_" + filter;
      }
      if (StringUtils.isNotEmpty(extraFilter)) {
        cacheKey += "_" + exType + "_" + extraFilter;
      }
      if (StringUtils.isNotEmpty(eTag)) {
        cacheKey += "_" + eTag;
      }
      if (StringUtils.isNotBlank(excludeETag)) {
        cacheKey += "_E" + excludeETag;
      }
      if (cache != null && !skipCache) {
        model = (MainDashboardModel) cache.get(cacheKey);
        if (model != null) {
          model.ut = LocalDateUtil.getFormattedTimeStamp(model.ut, sUser.getLocale(),
              sUser.getTimezone(), domainId);
          return model;
        }
      }
      Map<String, String> filters = new HashMap<>(1);
      if (StringUtils.isBlank(filter) && districtFilter != null) {
        filters.put("district", districtFilter);
        filters.put("state", stateFilter);
      } else if (StringUtils.isBlank(filter) && stateFilter != null) {
        filters.put("state", stateFilter);
        boolean noDistrict = !SearchUtil.isDistrictAvailable(dc.getCountry(), stateFilter);
        if (noDistrict) {
          level = "district";
          filters.put("district", "");
          districtFilter = stateFilter;
        }
      } else if ("state".equals(level)) {
        filters.put("state", filter);
        boolean noDistrict = !SearchUtil.isDistrictAvailable(dc.getCountry(), filter);
        if (noDistrict) {
          level = "district";
          filters.put("district", "");
        }
      } else if ("district".equals(level)) {
        String[] f = filter.split("_");
        filters.put("state", f[0]);
        filters.put("district", f[1]);
      }
      filters.put("country", countryFilter);
      if (StringUtils.isNotEmpty(extraFilter)) {
        filters.put(exType, extraFilter);
      }
      if (StringUtils.isNotEmpty(eTag)) {
        filters.put("eTag", eTag);
      }
      if (StringUtils.isNotEmpty(excludeETag)) {
        filters.put("eeTag", excludeETag);
      }

      try (
          ResultSet soRes =
              dashboardService.getMainDashboardResults(domainId, filters, "pdb_stock_out");
          ResultSet invRes =
              dashboardService.getMainDashboardResults(domainId, filters, "all_inv");
      ) {
        String colFilter;
        if ("district".equals(level) || districtFilter != null) {
          colFilter = "NAME";
        } else if ("state".equals(level) || stateFilter != null) {
          colFilter = "DISTRICT";
        } else {
          colFilter = "STATE";
        }
        model = dashboardBuilder.getPredictiveDashBoardData(soRes, invRes, colFilter);
      }
      if (StringUtils.isBlank(filter) && stateFilter == null) {
        model.mTy = dc.getCountry();
        model.mLev = "country";
      } else if (StringUtils.isBlank(filter) && districtFilter == null) {
        model.mTyNm = dc.getState();
        model.mTy = dc.getState().replace(" ", "");
        model.mPTy = dc.getCountry();
        model.mLev = "state";
      } else {
        model.mTyNm = filter != null ? filter : districtFilter;
        model.mTy = model.mTyNm.replace(" ", "");
        model.mPTy = dc.getCountry(); // Required only for state level, rest ignored
        model.mLev = level != null ? level : "district";
      }
      model.ut = getGeneratedTimestamp();
      try {
        if (cache != null) {
          cache.put(cacheKey, model, 1800); // 30 min expiry
        }
      } catch (Exception e) {
        xLogger.warn("Error in caching predictive dashboard data", e);
      }
      model.ut = LocalDateUtil.getFormattedTimeStamp(model.ut, sUser.getLocale(),
          sUser.getTimezone(), domainId);
      return model;
    } catch (Exception e) {
      xLogger.warn("Error in getting predictive Dashboard for domain {0}", domainId, e);
      throw new InvalidServiceException(
          "Error in getting predictive Dashboard for domain " + domainId);
    }
  }

  @RequestMapping(value = "/session", method = RequestMethod.GET)
  public
  @ResponseBody
  SessionDashboardModel getSessionData(@RequestParam(required = false) String filter,
                                       @RequestParam(required = false) String level,
                                       @RequestParam(required = false) String extraFilter,
                                       @RequestParam(required = false) String exType,
                                       @RequestParam(required = false) String date,
                                       @RequestParam(required = false) String type,
                                       @RequestParam(required = false) String eTag,
                                       @RequestParam(required = false) Boolean skipCache) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Long domainId = sUser.getCurrentDomainId();
    SessionDashboardModel model;
    try {
      MemcacheService cache = AppFactory.get().getMemcacheService();
      DomainConfig dc = DomainConfig.getInstance(domainId);
      if (dc.getCountry() == null) {
        return new SessionDashboardModel();
      }
      if (skipCache == null) {
        skipCache = false;
      }
      SimpleDateFormat udf = new SimpleDateFormat(Constants.DATE_FORMAT);
      SimpleDateFormat pdf = new SimpleDateFormat(Constants.DATE_FORMAT_CSV);
      date = pdf.format(udf.parse(date));
      String stateFilter = null;
      String districtFilter = null;
      String cacheKey = Constants.SESSACT_DASHBOARD_CACHE_PREFIX + String.valueOf(domainId);
      String countryFilter = dc.getCountry();
      cacheKey += "_" + countryFilter;
      if (StringUtils.isNotEmpty(dc.getDistrict())) {
        stateFilter = dc.getState();
        cacheKey += "_" + stateFilter;
        districtFilter = dc.getDistrict();
        cacheKey += "_" + districtFilter;
      } else if (StringUtils.isNotEmpty(dc.getState())) {
        stateFilter = dc.getState();
        cacheKey += "_" + stateFilter;
      }
      if (StringUtils.isNotEmpty(filter)) {
        cacheKey += "_" + filter;
      }
      if (StringUtils.isNotEmpty(extraFilter)) {
        cacheKey += "_" + exType + "_" + extraFilter;
      }
      if (StringUtils.isNotEmpty(date)) {
        cacheKey += "_" + date;
      }
      if (StringUtils.isNotEmpty(eTag)) {
        cacheKey += "_" + eTag;
      }
      if (StringUtils.isNotEmpty(type)) {
        cacheKey += "_" + type;
      }
      if (cache != null && !skipCache) {
        model = (SessionDashboardModel) cache.get(cacheKey);
        if (model != null) {
          model.ut = LocalDateUtil.getFormattedTimeStamp(model.ut, sUser.getLocale(),
              sUser.getTimezone(), domainId);
          return model;
        }
      }
      Map<String, String> filters = new HashMap<>(1);
      if (StringUtils.isBlank(filter) && districtFilter != null) {
        filters.put("district", districtFilter);
        filters.put("state", stateFilter);
      } else if (StringUtils.isBlank(filter) && stateFilter != null) {
        filters.put("state", stateFilter);
        boolean noDistrict = !SearchUtil.isDistrictAvailable(dc.getCountry(), stateFilter);
        if (noDistrict) {
          level = "district";
          filters.put("district", "");
          districtFilter = stateFilter;
        }
      } else if ("state".equals(level)) {
        filters.put("state", filter);
        boolean noDistrict = !SearchUtil.isDistrictAvailable(dc.getCountry(), filter);
        if (noDistrict) {
          level = "district";
          filters.put("district", "");
        }
      } else if ("district".equals(level)) {
        String[] f = filter.split("_");
        filters.put("state", f[0]);
        filters.put("district", f[1]);
      }
      filters.put("country", countryFilter);
      if (StringUtils.isNotEmpty(extraFilter)) {
        filters.put(exType, extraFilter); // Either material tag / material name
      }
      if (StringUtils.isNotEmpty(date)) {
        filters.put("atd", date);
      }
      if (StringUtils.isNotEmpty(eTag)) {
        filters.put("eTag", eTag);
      }
      if (StringUtils.isNotEmpty(type)) {
        filters.put("type", type);
      }
      // Timezone difference in minutes
      int difference;
      if (dc.getTimezone() == null || Calendar.getInstance().getTimeZone().getID()
          .equals(TimeZone.getTimeZone(dc.getTimezone()).getID())) {
        difference = 0;
      } else {
        difference =
            (TimeZone.getTimeZone(dc.getTimezone())
                .getOffset(Calendar.getInstance().getTimeInMillis())) / (1000 * 60);
      }
      int m = Math.abs(difference);
      String
          hours =
          (difference < 0 ? "-" : "") + (m / 60 < 10 ? "0" + m / 60 : String.valueOf(m / 60));
      String
          minutes =
          (difference < 0 ? "-" : "") + (m % 60 < 10 ? "0" + m % 60 : String.valueOf(m % 60));
      filters.put("diff", hours + ":" + minutes + ":00");
      Date atd = LocalDateUtil.parseCustom(date, Constants.DATE_FORMAT_CSV, dc.getTimezone());
      SimpleDateFormat df = new SimpleDateFormat(Constants.DATETIME_CSV_FORMAT);
      //Increment date to use less than queries
      Calendar domainCal;
      if (dc.getTimezone() != null) {
        domainCal = GregorianCalendar.getInstance(TimeZone.getTimeZone(dc.getTimezone()));
      } else {
        domainCal = GregorianCalendar.getInstance();
      }
      domainCal.setTime(atd);
      domainCal.add(Calendar.DAY_OF_MONTH, -6); // 1 week
      filters.put("sDate", df.format(domainCal.getTime()));
      domainCal.add(Calendar.DAY_OF_MONTH, 10); // 3 days extra
      domainCal.add(Calendar.SECOND, -1);
      filters.put("eDate", df.format(domainCal.getTime()));

      try (
          ResultSet sessionRes =
              dashboardService.getMainDashboardResults(domainId, filters, "sdb_session");
          ResultSet allSessionRes =
              dashboardService.getMainDashboardResults(domainId, filters, "sdb_all_session");
      ) {
        model = dashboardBuilder.getSessionData(allSessionRes, sessionRes, pdf.parse(date));
      }
      if (StringUtils.isBlank(filter) && stateFilter == null) {
        model.mTy = dc.getCountry();
        model.mLev = "country";
      } else if (StringUtils.isBlank(filter) && districtFilter == null) {
        model.mTyNm = dc.getState();
        model.mTy = dc.getState().replace(" ", "");
        model.mLev = "state";
      } else {
        model.mTyNm = filter != null ? filter : districtFilter;
        model.mTy = model.mTyNm.replace(" ", "");
        model.mLev = level != null ? level : "district";
      }
      model.ut = getGeneratedTimestamp();
      try {
        if (cache != null) {
          cache.put(cacheKey, model, 1800); // 30 min expiry
        }
      } catch (Exception e) {
        xLogger.warn("Error in caching session dashboard data", e);
      }
      model.ut = LocalDateUtil.getFormattedTimeStamp(model.ut, sUser.getLocale(),
          sUser.getTimezone(), domainId);
      return model;
    } catch (Exception e) {
      xLogger.warn("Error in getting session Dashboard for domain {0}", domainId, e);
      throw new InvalidServiceException("Error in getting session Dashboard " + domainId);
    }
  }


  @RequestMapping(value = "/ent/", method = RequestMethod.GET)
  public
  @ResponseBody
  MainDashboardModel getEntityInvData(@RequestParam Long eid,
                                      @RequestParam(required = false) String mTag) {
    MainDashboardModel model;
    Long domainId = SecurityUtils.getCurrentDomainId();
    Map<String, String> filter = new HashMap<>(1);
    try {
      filter.put(Constants.ENTITY, String.valueOf(eid));
      filter.put(Constants.PARAM_DOMAINID, String.valueOf(domainId));
      if(mTag != null) {
        filter.put(Constants.MATERIAL_TAG, mTag);
      }
      try (
          ResultSet rs =
              dashboardService.getMainDashboardResults(domainId, filter, ENT_INV_DASHBOARD);
          ResultSet tempRs =
              dashboardService.getMainDashboardResults(domainId, filter, ENT_TEMP_DASHBOARD);
      ) {
        Integer total = dashboardService.getInvTotalCount(filter);
        model = dashboardBuilder.getEntityInvTempDashboard(rs, tempRs, total);
      }
      return model;
    } catch (Exception e) {
      xLogger.warn("Error while getting dashboard for entity:{0} ",eid,e);
      throw new InvalidServiceException(e);
    }
  }

  @RequestMapping(value = "/inv", method = RequestMethod.GET)
  public
  @ResponseBody
  MainDashboardModel getInvData(@RequestParam(required = false) String state,
                                @RequestParam(required = false) String district,
                                @RequestParam(required = false) String period,
                                @RequestParam(required = false) String eTag,
                                @RequestParam(required = false) Boolean skipCache) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Long domainId = sUser.getCurrentDomainId();
    MainDashboardModel model;
    try {
      if (skipCache == null) {
        skipCache = false;
      }
      MemcacheService cache = AppFactory.get().getMemcacheService();
      String cacheKey = Constants.INV_DASHBOARD_CACHE_PREFIX + String.valueOf(domainId);
      DomainConfig dc = DomainConfig.getInstance(domainId);
      String countryFilter = dc.getCountry();
      cacheKey += "_" + countryFilter;
      if (StringUtils.isNotBlank(state)) {
        cacheKey = cacheKey + "_" + state;
      }
      if (StringUtils.isNotBlank(district)) {
        cacheKey += "_" + district;
      }
      if (StringUtils.isNotEmpty(period)) {
        cacheKey += "_" + period;
        cacheKey += "_T" + period;
      }
      if (StringUtils.isNotEmpty(eTag)) {
        cacheKey += "_" + eTag;
      }
      if (cache != null && !skipCache) {
        model = (MainDashboardModel) cache.get(cacheKey);
        if (model != null) {
          model.ut = LocalDateUtil.getFormattedTimeStamp(model.ut, sUser.getLocale(),
              sUser.getTimezone(), domainId);
          return model;
        }
      }
      Map<String, String> filters = new HashMap<>(1);
      if (StringUtils.isNotBlank(countryFilter)) {
        filters.put("country", countryFilter);
      }
      if (StringUtils.isNotBlank(state)) {
        filters.put("state", state);
      }
      if (StringUtils.isNotBlank(district)) {
        filters.put("district", district);
      }
      if (StringUtils.isNotEmpty(period)) {
        filters.put("period", period);
        filters.put("tPeriod", period);
      }
      if (StringUtils.isNotEmpty(eTag)) {
        filters.put("eTag", eTag);
      }
      ResultSet tempRes = null;
      try (
          ResultSet eventRes =
              dashboardService.getMainDashboardResults(domainId, filters, "idb_events");
          ResultSet invRes = dashboardService.getMainDashboardResults(domainId, filters, "idb_inv");
      ) {
        if (filters.containsKey("eTag")) {
          filters.put("eTag", "'" + filters.get("eTag") + "'");
        }
        tempRes = dashboardService.getMainDashboardResults(domainId, filters, "temperature");
        model = dashboardBuilder.getInvDashBoardData(eventRes, invRes, tempRes,
                StringUtils.isEmpty(state) ? "STATE"
                    : StringUtils.isEmpty(district) ? "DISTRICT" : "ENTITY", sUser.getLocale(),
                sUser.getTimezone());
      } finally {
        if (tempRes != null) {
          tempRes.close();
        }
      }
      if (StringUtils.isBlank(state)) {
        model.mTy =
            StringUtils.isBlank(dc.getState()) ? countryFilter : dc.getState().replace(" ", "");
      } else {
        model.mTy = state.replace(" ", "");
      }
      model.ut = getGeneratedTimestamp();
      try {
        if (cache != null) {
          cache.put(cacheKey, model, 1800); // 30 min expiry
        }
      } catch (Exception e) {
        xLogger.warn("Error in caching dashboard data", e);
      }
      model.ut = LocalDateUtil.getFormattedTimeStamp(model.ut, sUser.getLocale(), sUser.getTimezone(), domainId);
      return model;
    } catch (Exception e) {
      xLogger.warn("Error in getting main Dashboard {0}", domainId, e);
      throw new InvalidServiceException("Error in getting main Dashboard " + domainId);
    }
  }

  @RequestMapping(value = "/delete", method = RequestMethod.POST)
  public
  @ResponseBody
  String delete(@RequestParam Long id) {
    try {
      String name = dashboardService.deleteDashboard(id);
      return "Dashboard " + MsgUtil.bold(name) + " deleted successfully.";
    } catch (ServiceException e) {
      xLogger.severe("Error deleting Dashboard: {0}", id);
      throw new InvalidServiceException("Error deleting Dashboard: " + id);
    }
  }

  @RequestMapping(value = "/update", method = RequestMethod.POST)
  public
  @ResponseBody
  String update(@RequestBody DBWUpdateRequest rObj) {
    try {
      String name = dashboardService.updateDashboard(rObj.id, rObj.ty, rObj.val);
      return "Dashboard " + MsgUtil.bold(name) + " updated successfully.";
    } catch (ServiceException e) {
      xLogger.severe("Error deleting dashboard: {0}", rObj.id);
      throw new InvalidServiceException("Error deleting dashboard: " + rObj.id);
    }
  }

  @RequestMapping(value = "/setdefault", method = RequestMethod.GET)
  public
  @ResponseBody
  String setDefault(@RequestParam Long oid, @RequestParam Long id) {
    try {
      String name = dashboardService.setDefault(oid, id);
      return "Dashboard " + MsgUtil.bold(name) + " is marked as default dashboard.";
    } catch (ServiceException e) {
      xLogger.severe("Error in setting dashboard {0} as default", id);
      throw new InvalidServiceException("Error in setting dashboard " + id + " as default");
    }
  }

  @RequestMapping(value = "/saveconfig", method = RequestMethod.POST)
  public
  @ResponseBody
  String saveConfig(@RequestBody DBWUpdateRequest rObj) {
    try {
      String name = dashboardService.updateDashboard(rObj.id, rObj.ty, rObj.val);
      return "Dashboard configuration for " + MsgUtil.bold(name) + " is updated successfully.";
    } catch (ServiceException e) {
      xLogger.severe("Error in updating dashboard configuration for {0}", rObj.id);
      throw new InvalidServiceException("Error in updating dashboard configuration for " + rObj.id);
    }
  }

  private String getGeneratedTimestamp() {
    SimpleDateFormat sdf = new SimpleDateFormat(Constants.ANALYTICS_DATE_FORMAT);
    return sdf.format(new Date());
  }
}
