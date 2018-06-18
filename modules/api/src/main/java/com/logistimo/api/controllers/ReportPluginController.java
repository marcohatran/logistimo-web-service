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

import com.logistimo.api.builders.ReportBuilder;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.constants.CharacterConstants;
import com.logistimo.exception.BadRequestException;
import com.logistimo.exports.ExportService;
import com.logistimo.logger.XLog;
import com.logistimo.reports.models.ReportMinMaxHistoryFilters;
import com.logistimo.reports.plugins.internal.ExportModel;
import com.logistimo.reports.plugins.internal.QueryHelper;
import com.logistimo.reports.plugins.models.ReportChartModel;
import com.logistimo.reports.plugins.models.TableResponseModel;
import com.logistimo.reports.plugins.service.ReportPluginService;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.users.service.UsersService;
import com.logistimo.utils.LocalDateUtil;

import org.apache.commons.collections.ListUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Created by mohan on 02/03/17.
 */
@Controller
@RequestMapping("/plugins/report")
public class ReportPluginController {

  private static final XLog xLogger = XLog.getLog(ReportPluginController.class);

  private static final String JSON_REPORT_TYPE = "type";
  private static final String JSON_REPORT_VIEW_TYPE = "viewtype";
  private static final String INVALID_REQUEST = "Invalid request";


  private ReportPluginService reportPluginService;
  private ExportService exportService;
  private UsersService usersService;
  private ReportBuilder reportBuilder;

  @Autowired
  private void setReportPluginService(ReportPluginService reportPluginService) {
    this.reportPluginService = reportPluginService;
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
  private void setReportBuilder(ReportBuilder reportBuilder) {
    this.reportBuilder = reportBuilder;
  }

  @RequestMapping(value = "/", method = RequestMethod.GET)
  public
  @ResponseBody
  List<ReportChartModel> getReportData(@RequestParam String json) {
    xLogger.fine("Entering getReportData");
    try {
      JSONObject jsonObject = new JSONObject(json);
      if (!jsonObject.has(JSON_REPORT_TYPE)) {
        xLogger.warn("Report type is mandatory.");
        throw new BadRequestException(INVALID_REQUEST);
      }
      return reportPluginService.getReportData(SecurityUtils.getCurrentDomainId(), json);
    } catch (Exception e) {
      xLogger.severe("Error while getting the report data", e);
      return ListUtils.EMPTY_LIST;
    }
  }

  @RequestMapping(value = "/breakdown", method = RequestMethod.GET)
  public
  @ResponseBody
  TableResponseModel getReportTableData(@RequestParam String json) {
    xLogger.fine("Entering getReportData");
    try {
      JSONObject jsonObject = new JSONObject(json);
      if (!jsonObject.has(JSON_REPORT_TYPE) || !jsonObject.has(JSON_REPORT_VIEW_TYPE)) {
        xLogger.warn("Both report type and view type is mandatory.");
        throw new BadRequestException(INVALID_REQUEST);
      }
      return reportPluginService.getReportTableData(SecurityUtils.getCurrentDomainId(), json);
    } catch (Exception e) {
      xLogger.severe("Error while getting the data", e);
      return null;
    }
  }

  /**
   * Get the last aggregated time based on report type
   */
  @RequestMapping(value = "/last-run-time", method = RequestMethod.GET)
  public
  @ResponseBody
  String getLastAggregatedTime(@RequestParam String reportType) {
    xLogger.fine("In getLastAggregatedTime");
    try {
      Date date = reportPluginService.getLastAggregatedTime(reportType);
      if (date != null) {
        SecureUserDetails secureUserDetails = SecurityUtils.getUserDetails();
        return LocalDateUtil.format(date,
            secureUserDetails.getLocale(), secureUserDetails.getTimezone());
      }
    } catch (Exception e) {
      xLogger.severe("Error while getting the data", e);
    }
    return CharacterConstants.EMPTY;
  }

  @RequestMapping(value = "/export", method = RequestMethod.POST)
  public
  @ResponseBody
  String exportData(@RequestBody String json) throws ParseException, ServiceException {
    ExportModel model = reportPluginService.buildExportModel(json);
    long jobId = exportService.scheduleExport(model,"report");
    ResourceBundle backendMessages = Resources.get().getBundle("BackendMessages", Locale.ENGLISH);
    IUserAccount u = usersService.getUserAccount(SecurityUtils.getUsername());
    return backendMessages.getString("export.success1") + " " + u.getEmail() + " "
        + backendMessages.getString("export.success2") + " "
        + backendMessages.getString("exportstatusinfo2") + " "
        + jobId + ". "
        + backendMessages.getString("exportstatusinfo1");
  }

  @RequestMapping(value ="/min-max-history", method = RequestMethod.GET)
  @ResponseBody
  List<ReportChartModel> getMinMaxHistory(@RequestParam String json) {
    xLogger.fine("Entering getMinMaxHistory");
    try {
      JSONObject jsonObject = new JSONObject(json);
      if (!jsonObject.has(JSON_REPORT_TYPE)) {
        xLogger.warn("Report type is mandatory.");
        throw new BadRequestException(INVALID_REQUEST);
      }
      ReportMinMaxHistoryFilters minMaxHistoryFilters = QueryHelper.parseMinMaxHistoryFilters(jsonObject);
      return reportBuilder.buildMinMaxHistoryReportsData(
          reportPluginService.getMinMaxHistoryReportData(minMaxHistoryFilters), minMaxHistoryFilters);
    } catch (Exception e) {
      xLogger.severe("Error while getting the report data", e);
      return ListUtils.EMPTY_LIST;
    }
  }
}
