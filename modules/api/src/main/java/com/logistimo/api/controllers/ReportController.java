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

import com.logistimo.api.builders.DomainBuilder;
import com.logistimo.api.builders.DomainStatisticsBuilder;
import com.logistimo.api.builders.FChartBuilder;
import com.logistimo.api.models.DomainStatisticsModel;
import com.logistimo.api.models.FChartModel;
import com.logistimo.api.request.FusionChartRequest;
import com.logistimo.auth.GenericAuthoriser;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.constants.Constants;
import com.logistimo.domains.entity.IDomainLink;
import com.logistimo.domains.service.DomainsService;
import com.logistimo.exception.BadRequestException;
import com.logistimo.exception.ForbiddenAccessException;
import com.logistimo.exception.InvalidServiceException;
import com.logistimo.logger.XLog;
import com.logistimo.reports.ReportsConstants;
import com.logistimo.reports.entity.slices.IDomainStats;
import com.logistimo.reports.generators.ReportData;
import com.logistimo.reports.service.ReportsService;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;
import com.logistimo.services.utils.ConfigUtil;
import com.logistimo.utils.LocalDateUtil;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Created by Mohan Raja on 30/01/15
 */

@Controller
@RequestMapping("/report")
public class ReportController {

  private static final XLog xLogger = XLog.getLog(ReportController.class);

  private DomainsService domainsService;
  private DomainBuilder domainBuilder;
  private DomainStatisticsBuilder domainStatisticsBuilder;
  private FChartBuilder fChartBuilder;

  @Autowired
  private ReportsService reportsService;

  @Autowired
  public void setDomainsService(DomainsService domainsService) {
    this.domainsService = domainsService;
  }

  @Autowired
  public void setDomainBuilder(DomainBuilder domainBuilder) {
    this.domainBuilder = domainBuilder;
  }

  @Autowired
  public void setDomainStatisticsBuilder(DomainStatisticsBuilder domainStatisticsBuilder) {
    this.domainStatisticsBuilder = domainStatisticsBuilder;
  }

  @Autowired
  public void setfChartBuilder(FChartBuilder fChartBuilder) {
    this.fChartBuilder = fChartBuilder;
  }

  @RequestMapping(value = "/fchartdata", method = RequestMethod.POST)
  public
  @ResponseBody
  List<FChartModel> getFChartData(@RequestBody FusionChartRequest fcRequest) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    String userId = sUser.getUsername();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.get().getBundle("BackendMessages", locale);
    String timezone = sUser.getTimezone();
    Long domainId = sUser.getCurrentDomainId();
    Map<String, Object> filters = getFilters(fcRequest, domainId);
    DomainConfig dc = DomainConfig.getInstance(domainId);
    try {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
      if (fcRequest.daily) {
        // The fcRequest.stDate is set to the the first day of the month for which the report is being drilled down.
        // Set the fcRequest.enDate to the first day of the next month before sending request to the backend.
        Calendar d = new GregorianCalendar();
        d.setTime(sdf.parse(fcRequest.stDate));
        if (ReportsConstants.TYPE_CONSUMPTION.equals(fcRequest.rty)) {
          d.add(Calendar.DAY_OF_MONTH, -1);
          fcRequest.stDate = sdf.format(d.getTime());
          d.add(Calendar.DAY_OF_MONTH, 1);
        }
        d.add(Calendar.MONTH, 1);
        d.set(Calendar.DAY_OF_MONTH, 1);
        fcRequest.enDate = sdf.format(d.getTime());
      } else {
        // The fcRequest.enDate comes in as the last day of the selected month in the UI.
        // It should be set to the should be set to first day of next month before sending request to the backend.
        Calendar d = new GregorianCalendar();
        if (ReportsConstants.TYPE_CONSUMPTION.equals(fcRequest.rty)) {
          d.setTime(sdf.parse(fcRequest.stDate));
          d.add(Calendar.MONTH, -1);
          fcRequest.stDate = sdf.format(d.getTime());
        }
        d.setTime(sdf.parse(fcRequest.enDate));
        d.add(Calendar.DATE, 1);
        fcRequest.enDate = sdf.format(d.getTime());
      }

      ReportData r =
          reportsService.getReportData(fcRequest.rty, sdf.parse(fcRequest.stDate), sdf.parse(fcRequest.enDate),
              fcRequest.freq, filters, locale, timezone, null, dc, userId);
      String repGenTime = reportsService.getRepGenTime(domainId, locale, timezone);
      if (r.getResults() == null) {
        return new ArrayList<>(0);
      }
      switch (fcRequest.rty) {
        case ReportsConstants.TYPE_CONSUMPTION:
          return fChartBuilder.buildConsumptionChartModel(r, repGenTime);
        case ReportsConstants.TYPE_ORDERRESPONSETIMES:
          return fChartBuilder.buildORTChartModel(r, repGenTime);
        case ReportsConstants.TYPE_STOCKEVENTRESPONSETIME:
          return fChartBuilder.buildRRTChartModel(r, repGenTime);
        case ReportsConstants.TYPE_TRANSACTION:
          return fChartBuilder.buildTCChartModel(r, repGenTime);
        case ReportsConstants.TYPE_USERACTIVITY:
          return fChartBuilder.buildUAChartModel(r, repGenTime);
        default:
          xLogger.warn("invalid report type found while fetching report data: " + fcRequest.rty);
          throw new BadRequestException(backendMessages.getString("chart.data.fetch.error"));
      }
    } catch (ServiceException | ParseException e) {
      xLogger.severe(backendMessages.getString("chart.data.fetch.error"), e);
      throw new InvalidServiceException(backendMessages.getString("chart.data.fetch.error"));
    }
  }


  private Map<String, Object> getFilters(FusionChartRequest fcRequest, Long domainId) {
    Map<String, Object> filters = new HashMap<>();
    filters.put(ReportsConstants.FILTER_DOMAIN, domainId);
    if (fcRequest.eid != null) {
      filters.put(ReportsConstants.FILTER_KIOSK, fcRequest.eid);
    }
    if (fcRequest.mid != null) {
      filters.put(ReportsConstants.FILTER_MATERIAL, fcRequest.mid);
    }
    if (StringUtils.isNotBlank(fcRequest.st)) {
      filters.put(ReportsConstants.FILTER_STATE, fcRequest.st);
    }
    if (StringUtils.isNotBlank(fcRequest.dis)) {
      filters.put(ReportsConstants.FILTER_DISTRICT, fcRequest.dis);
    }
    if (fcRequest.egrp != null) {
      filters.put(ReportsConstants.FILTER_POOLGROUP, fcRequest.egrp);
    }
    if (StringUtils.isNotBlank(fcRequest.uid)) {
      filters.put(ReportsConstants.FILTER_USER, fcRequest.uid);
    }
    if (StringUtils.isNotBlank(fcRequest.mtag)) {
      filters.put(ReportsConstants.FILTER_MATERIALTAG, fcRequest.mtag);
    }
    if (StringUtils.isNotBlank(fcRequest.etag)) {
      filters.put(ReportsConstants.FILTER_KIOSKTAG, fcRequest.etag);
    }
    if (StringUtils.isNotBlank(fcRequest.mtag)) {
      filters.put(ReportsConstants.FILTER_MATERIALTAG, fcRequest.mtag);
    }
        /*if (filters.containsKey(ReportsConstants.FILTER_MATERIAL) && filters.size() >= 3) {
            filters.remove(ReportsConstants.FILTER_DOMAIN);
        }*/
    return filters;
  }

  @RequestMapping(value = "/domainstats", method = RequestMethod.GET)
  public
  @ResponseBody
  DomainStatisticsModel getDomainStatistics(Long domainId) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    String userId = sUser.getUsername();
    DomainStatisticsModel domainStatisticsModel = null;
    try {
      if (!GenericAuthoriser.authoriseUser(userId)) {
        throw new ForbiddenAccessException("Permission denied of the user to access this domain");
      }
      domainId = (domainId != null ? domainId : sUser.getCurrentDomainId());
      List<IDomainLink> linkedDomains;
      Set<Long> domains;
      List<? extends IDomainStats> parentData;
      List<IDomainStats> childrenData = new ArrayList<>();
      String domainStatsApp = ConfigUtil.get(ReportsConstants.MASTER_DATA_APP_NAME);
      parentData = reportsService.getDomainStatistics(domainId);
      if (parentData != null && parentData.size() > 0) {
        domainStatisticsModel = domainStatisticsBuilder.buildParentModel(parentData.get(0));
        Date date = LocalDateUtil.parseCustom(
            reportsService.getReportLastRunTime(domainStatsApp), Constants.ANALYTICS_DATE_FORMAT, null);
        domainStatisticsModel.lrt = LocalDateUtil.format(date, sUser.getLocale(),
            sUser.getTimezone());
      }
      linkedDomains = domainsService.getDomainLinks(domainId, 0, 0);
      domains = domainBuilder.buildChildDomainsList(linkedDomains);
      if (domains != null) {
        for (Long d : domains) {
          List<IDomainStats> domainStats = (List<IDomainStats>) reportsService.getDomainStatistics(d);
          if (domainStats != null && domainStats.size() > 0) {
            childrenData.add(reportsService.getDomainStatistics(d).get(0));
          }
        }
        if (childrenData.size() > 0) {
          domainStatisticsModel =
              domainStatisticsBuilder
                  .buildChildModel(childrenData, domainStatisticsModel
                  );
        }
      }
    } catch (ServiceException | ParseException e) {
      xLogger
          .severe("Exception while fetching domain statistics details for the domain {0}",
              domainId,
              e);
      xLogger.severe(
          "Invalid service exception while fetching domain level statistics for the domain {0} for user {1} ",
          domainId, userId);
    }
    return domainStatisticsModel;
  }

  @RequestMapping(value = "/domainstats/tag", method = RequestMethod.GET)
  public
  @ResponseBody
  Map<String, String> getDomainStatisticsByTag(Long domainId,
                                               String tag, String c) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    String userId = sUser.getUsername();
    Map<String, String> results;
    try {
      if (!GenericAuthoriser.authoriseUser(userId)) {
        throw new ForbiddenAccessException("Permission denied of the user to access this domain");
      }
      domainId = (domainId != null ? domainId : sUser.getCurrentDomainId());
      if (StringUtils.isBlank(tag)) {
        return null;
      }
      results = reportsService.getDomainStatisticsByTag(domainId, tag, c);
    } catch (ServiceException e) {
      xLogger
          .severe(
              "Exception while fetching domain statistics details by tags for the domain {0}",
              domainId, e.getMessage());
      throw new InvalidServiceException("Unable to fetch domain statistics data for the domain");
    }
    return results;
  }


}
