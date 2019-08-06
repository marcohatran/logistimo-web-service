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

import com.logistimo.api.builders.FChartBuilder;
import com.logistimo.api.models.FChartModel;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.context.StaticApplicationContext;
import com.logistimo.exception.InvalidServiceException;
import com.logistimo.logger.XLog;
import com.logistimo.reports.models.DomainCounts;
import com.logistimo.reports.service.ReportsService;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;
import com.logistimo.services.utils.ConfigUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Created by naveensnair on 27/02/15.
 */

@Controller
@RequestMapping("/home")
public class HomeController {
  private static final XLog xLogger = XLog.getLog(HomeController.class);

  private FChartBuilder fChartBuilder;

  @Autowired
  private ReportsService reportsService;

  @Autowired
  public void setfChartBuilder(FChartBuilder fChartBuilder) {
    this.fChartBuilder = fChartBuilder;
  }

  @RequestMapping(value = "/reports/stats/", method = RequestMethod.GET)
  public
  @ResponseBody
  List<FChartModel> getStatsReport(@RequestParam String month, @RequestParam String prd,
                                   @RequestParam String mTag, @RequestParam String matId,
                                   @RequestParam(required = false) String reportType) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    Long domainId = sUser.getCurrentDomainId();
    if (domainId == null) {
      xLogger.severe("Error in fetching Monthly/Daily Status");
      throw new InvalidServiceException(backendMessages.getString("monthly.daily.status.fetch"));
    }
    String periodType = null;
    int period = 6;
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    try {
      Calendar cal = GregorianCalendar.getInstance();
      cal.setTime(sdf.parse(month));
      if (prd.equalsIgnoreCase("m")) {
        periodType = "monthly";
        cal.add(Calendar.MONTH, 1);
        cal.set(Calendar.DAY_OF_MONTH, 1);
      } else if (prd.equalsIgnoreCase("d")) {
        periodType = "daily";
        cal.add(Calendar.DATE, 1);
        period = 30;
      }
      String repGenTime;
      DomainCounts domainCounts;
      repGenTime = reportsService.getRepGenTime(domainId, locale, sUser.getTimezone());
      domainCounts = reportsService.getDomainCounts(domainId, cal.getTime(), period, periodType, mTag, matId,
          reportType);
      boolean
          isCurrentMonth =
          (cal.get(Calendar.MONTH) == Calendar.getInstance().get(Calendar.MONTH) + 1);

      return fChartBuilder.buildHDashboardChartModel(domainCounts, periodType,
          isCurrentMonth, repGenTime);
    } catch (ServiceException | ParseException e) {
      xLogger.severe("Error in fetching Monthly/Daily Status", e);
      throw new InvalidServiceException(backendMessages.getString("monthly.daily.status.fetch"));
    }
  }
}
