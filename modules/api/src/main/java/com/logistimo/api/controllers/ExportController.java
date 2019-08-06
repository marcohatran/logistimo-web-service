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
import com.logistimo.api.builders.JobStatusBuilder;
import com.logistimo.api.request.ExportReportRequestObj;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.bulkuploads.BulkUploadMgr;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.constants.Constants;
import com.logistimo.entity.IJobStatus;
import com.logistimo.entity.IUploaded;
import com.logistimo.exception.BadRequestException;
import com.logistimo.exception.InvalidServiceException;
import com.logistimo.exception.TaskSchedulingException;
import com.logistimo.exports.BulkExportMgr;
import com.logistimo.exports.ExportService;
import com.logistimo.exports.model.RequestModel;
import com.logistimo.logger.XLog;
import com.logistimo.pagination.Navigator;
import com.logistimo.pagination.PageParams;
import com.logistimo.pagination.Results;
import com.logistimo.reports.ReportsConstants;
import com.logistimo.reports.utils.ReportsUtil;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.ObjectNotFoundException;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;
import com.logistimo.services.UploadService;
import com.logistimo.services.taskqueue.ITaskService;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.users.service.UsersService;
import com.logistimo.utils.JobUtil;
import com.logistimo.utils.LocalDateUtil;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by Mohan Raja on 23/01/15
 */
@Controller
@RequestMapping("/export")
public class ExportController {

  private static final XLog xLogger = XLog.getLog(ExportController.class);

  private static final String EXPORT_TASK_URL = "/task/export";
  private ITaskService taskService = AppFactory.get().getTaskService();

  private JobStatusBuilder jobStatusBuilder;
  private UploadService uploadService;
  private ExportService exportService;
  private UsersService usersService;

  @Autowired
  private void setExportService(ExportService exportService) {
    this.exportService = exportService;
  }

  @Autowired
  public void setJobStatusBuilder(JobStatusBuilder jobStatusBuilder) {
    this.jobStatusBuilder = jobStatusBuilder;
  }

  @Autowired
  private void setUsersService(UsersService usersService) {
    this.usersService = usersService;
  }

  @Autowired
  public void setUploadService(UploadService uploadService) {
    this.uploadService = uploadService;
  }

  @RequestMapping(value = "/download", method = RequestMethod.GET)
  public
  @ResponseBody
  void downloadFile(@RequestParam String key, @RequestParam(required = false) boolean isBlobKey,
                    @RequestParam(required = false) String fileName,
                    HttpServletResponse response) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    if (key == null || key.isEmpty()) {
      throw new BadRequestException(backendMessages.getString("file.download.error"));
    }
    try {
      String blobKeyStr;
      String downloadFileName;
      if (!isBlobKey) {
        IUploaded uploaded = uploadService.getUploaded(key);
        blobKeyStr = uploaded.getBlobKey();
        downloadFileName = uploaded.getFileName();
      } else {
        blobKeyStr = key;
        downloadFileName = fileName;
      }
      response.addHeader("Content-Disposition", "attachment; filename=" + downloadFileName);
      response.setBufferSize(32 * 1024);
      if (blobKeyStr != null) {
        AppFactory.get().getBlobstoreService().serve(blobKeyStr, response);
      }
      response.flushBuffer();
    } catch (ServiceException | ObjectNotFoundException | IOException e) {
      xLogger.warn("Error in downloading file", e);
      throw new InvalidServiceException(backendMessages.getString("file.download.error"));
    }
  }

  @RequestMapping(value = "/uploadformat", method = RequestMethod.GET)
  public
  @ResponseBody
  void bulkUploadFormat(@RequestParam String type,
                        HttpServletResponse response) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    String csv = BulkUploadMgr.getCSVFormat(type, locale);
    if (csv == null) {
      throw new BadRequestException(backendMessages.getString("file.uploadformat.fetch.error"));
    }
    String typeName = null;
    try {
      ResourceBundle bckMessages = Resources.getBundle(locale);
      if ("kiosks".equalsIgnoreCase(type)) {
        typeName = bckMessages.getString(type);
      } else {
        typeName = bckMessages.getString(type);
      }
    } catch (Exception ignored) {
      xLogger.warn("Exception while getting resource bundle", ignored);
    }
    String filename = (typeName != null ? typeName : type) + ".csv";
    response.setCharacterEncoding("UTF-8");
    response.setContentType("text/csv");
    response.addHeader("Content-Disposition", "inline; filename=" + filename);
    PrintWriter pw;
    try {
      pw = response.getWriter();
      pw.write(csv);
      pw.close();
    } catch (IOException e) {
      xLogger.severe("Error in fetching upload format response", e);
      throw new InvalidServiceException(
          backendMessages.getString("file.uploadformat.fetch.response"));
    }
  }

  @RequestMapping(value = "/schedule/batch", method = RequestMethod.GET)
  public
  @ResponseBody
  String scheduleBatch(HttpServletRequest request) throws IOException {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Long jobId;
    Locale locale = sUser.getLocale();
    Map<String, String> params = taskService.getParamsFromQueryString(request.getQueryString());
    params.put("action", "be");
    params.put("sourceuserid", sUser.getUsername());
    params.put("userids", sUser.getUsername());
    Long domainId = sUser.getCurrentDomainId();
    params.put("domainid", String.valueOf(domainId));
    params.put("tz", sUser.getTimezone());
    Map<String, String> headers = BulkExportMgr.getExportBackendHeader();
    if ("transactions".equalsIgnoreCase(params.get("type")) || "orders"
        .equalsIgnoreCase(params.get("type"))) {
      if (StringUtils.isEmpty(params.get("to"))) {
        params.remove("to");
      }
      if (StringUtils.isEmpty(params.get("from"))) {
        params.remove("from");
      }
      if (StringUtils.isBlank(params.get("kioskid")) || "null"
          .equalsIgnoreCase(params.get("kioskid"))) {
        params.remove("kioskid");
      }
      if (StringUtils.isBlank(params.get("materialid")) || "null"
          .equalsIgnoreCase(params.get("materialid"))) {
        params.remove("materialid");
      }
      if (StringUtils.isBlank(params.get("batchid")) || "null"
          .equalsIgnoreCase(params.get("batchid"))) {
        params.remove("batchid");
      }
      if (StringUtils.isBlank(params.get("rsn"))) {
        params.remove("rsn");
      } else {
        params.put("rsn", URLDecoder.decode(params.get("rsn"), "UTF-8"));
      }
    }
    if ("orders".equalsIgnoreCase(params.get("type"))) {
      if (StringUtils.isNotEmpty(params.get("to")) && StringUtils.isNotEmpty(params.get("from"))) {
        String from = params.get("from");
        String to = params.get("to");
        params.put("startdate", from.substring(0, from.indexOf('%')));
        params.put("enddate", to.substring(0, to.indexOf('%')));
      }
      if (StringUtils.isNotEmpty(params.get("values"))) {
        params.put("attachtoemail", "");
        params.put("format", "csv");
        params.remove("sourceuserid");
        params.remove("domainid");
        params.remove("action");
        params.remove("userids");
      }
    }
    if ("assets".equalsIgnoreCase(params.get("type"))) {
      params.put("subType", "assets");
    }
    if ("discrepancies".equalsIgnoreCase(params.get("type"))) {
      if (StringUtils.isEmpty(params.get("to"))) {
        params.remove("to");
      }
      if (StringUtils.isEmpty(params.get("from"))) {
        params.remove("from");
      }
      if (StringUtils.isBlank(params.get("kioskid")) || "null"
          .equalsIgnoreCase(params.get("kioskid"))) {
        params.remove("kioskid");
      }
      if (StringUtils.isBlank(params.get("materialid")) || "null"
          .equalsIgnoreCase(params.get("materialid"))) {
        params.remove("materialid");
      }
      if (StringUtils.isBlank(params.get("etag")) || "null".equalsIgnoreCase(params.get("etag"))) {
        params.remove("etag");
      }
      if (StringUtils.isBlank(params.get("mtag")) || "null".equalsIgnoreCase(params.get("mtag"))) {
        params.remove("mtag");
      }
      if (StringUtils.isBlank(params.get("disctype")) || "null"
          .equalsIgnoreCase(params.get("disctype"))) {
        params.remove("disctype");
      }
      if (StringUtils.isBlank(params.get("orderid")) || "null"
          .equalsIgnoreCase(params.get("orderid"))) {
        params.remove("orderid");
      }
      if (StringUtils.isBlank(params.get("otype")) || "null"
          .equalsIgnoreCase(params.get("otype"))) {
        params.remove("otype");
      }

    }

    try {
      if (StringUtils.isNotBlank(params.get("from")) || StringUtils.isNotBlank(params.get("to"))) {
        DomainConfig dc = DomainConfig.getInstance(SecurityUtils.getCurrentDomainId());
        if (StringUtils.isNotBlank(params.get("from"))) {
          params.put("from", LocalDateUtil.formatCustom(
              LocalDateUtil.parseCustom(URLDecoder.decode(params.get("from"), Constants.UTF8),
                  Constants.DATETIME_FORMAT, dc.getTimezone()), Constants.DATETIME_FORMAT, null));
        }
        if (StringUtils.isNotBlank(params.get("to"))) {
          params.put("to", LocalDateUtil.formatCustom(
              LocalDateUtil.parseCustom(URLDecoder.decode(params.get("to"), Constants.UTF8),
                  Constants.DATETIME_FORMAT, dc.getTimezone()), Constants.DATETIME_FORMAT, null));
        }
      }
    } catch (ParseException e) {
      xLogger.warn("Error while parsing date during export:", e);
    }

    // Create a entry in the JobStatus table using JobUtil.createJob method.
    jobId =
        JobUtil.createJob(domainId,
            sUser.getUsername(), null, IJobStatus.TYPE_EXPORT, params.get("type"), params);
    params.put("jobid", jobId.toString());
    try {
      taskService.schedule(ITaskService.QUEUE_EXPORTER, EXPORT_TASK_URL, params, headers,
          ITaskService.METHOD_POST);
    } catch (TaskSchedulingException e) {
      xLogger.severe("{0} when scheduling export task with params {1}: {2}", e.getClass().getName(),
          params, e.getMessage());
      ResourceBundle backendMessages = Resources.getBundle(locale);
      throw new InvalidServiceException(
          backendMessages.getString("error.in") + " " + e.getClass().getName() + " "
              + backendMessages.getString("schedule.export.task"));
    }
    return String.valueOf(jobId);
  }

  @RequestMapping(value = "/schedule/report", method = RequestMethod.POST)
  public
  @ResponseBody
  String scheduleReport(@RequestBody ExportReportRequestObj model) throws IOException {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    String userIds = sUser.getUsername();
    String sourceUserId = sUser.getUsername();
    Long domainId = sUser.getCurrentDomainId();
    if (model.filterMap == null || model.filterMap.isEmpty()) {
      if (model.filterMap == null) {
        model.filterMap = new HashMap<>();
      }
      String[] domains = new String[1];
      domains[0] = domainId.toString();
      model.filterMap.put(ReportsConstants.FILTER_DOMAIN, domains);
    }
    String filtersQueryString = ReportsUtil.getQueryString(model.filterMap, null);
    String
        queryString =
        "action=be&type=" + model.type + "&sourceuserid=" + sourceUserId + "&userids=" + userIds
            + "&domainid=" + domainId;
    if (model.startDate != null) {
      queryString += "&from=" + model.startDate + " 00:00:00";
    }
    if (model.endDate != null) {
      queryString += "&to=" + model.endDate + " 00:00:00";
    }
    if (model.frequency == null || model.frequency.isEmpty()) {
      model.frequency = ReportsConstants.FREQ_MONTHLY;
    }
    queryString += "&frequency=" + model.frequency;
    queryString += "&" + filtersQueryString + "&reports";
    xLogger.fine("queryString: {0}", queryString);
    Map<String, String> params = taskService.getParamsFromQueryString(queryString);
    if (params == null) {
      xLogger.severe("Invalid params while scheduling report export task in domain {0}", domainId);
      return null;
    }
    // Create a entry in the JobStatus table using JobUtil.createJob method.
    Long jobId = JobUtil.createJob(domainId,
        sourceUserId, null, IJobStatus.TYPE_EXPORT, params.get("type"), params);
    params.put("jobid", jobId.toString());

    xLogger.fine("params: {0}", params);
    String url = EXPORT_TASK_URL;
    Map<String, String> headers = BulkExportMgr.getExportBackendHeader();
    try {
      taskService
          .schedule(ITaskService.QUEUE_EXPORTER, url, params, headers, ITaskService.METHOD_POST);
    } catch (Exception e) {
      xLogger.severe("{0} while scheduling report export task in domain {1}. Message: {2}",
          e.getClass().getName(), domainId, e.getMessage(), e);
    }
    xLogger.fine("Exiting scheduleReport");
    return String.valueOf(jobId);
  }


  @RequestMapping(value = "/exportjoblist", method = RequestMethod.GET)
  public
  @ResponseBody
  Results getExportJobList(
      @RequestParam(defaultValue = PageParams.DEFAULT_OFFSET_STR) int offset,
      @RequestParam(defaultValue = PageParams.DEFAULT_SIZE_STR) int size,
      @RequestParam String type,
      @RequestParam boolean allExports,
      HttpServletRequest request) {

    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Long domainId = sUser.getCurrentDomainId();
    Navigator navigator =
        new Navigator(request.getSession(), "ExportController.getExportList", offset, size, "dummy",
            0);
    PageParams pageParams = new PageParams(navigator.getCursor(offset), offset, size);
    Results results;
    try {
      if (allExports) {
        results = JobUtil.getRecentJobs(type, null, domainId, pageParams);
      } else {
        results = JobUtil.getRecentJobs(type, sUser.getUsername(), domainId, pageParams);
      }

      navigator.setResultParams(results);
      if (results != null) {
        results.setOffset(offset);
      }
      return jobStatusBuilder.buildJobs(results, sUser);
    } catch (Exception e) {
      xLogger.warn("{0} when trying to get recent jobs for sUser {1} in domain {2}. Message: {3}",
          e.getClass().getName(), sUser.getUsername(), domainId, e.getMessage(), e);
      throw new InvalidServiceException("When trying to get recent jobs", e);
    }
  }


  @RequestMapping(value = "/", method = RequestMethod.POST)
  public
  @ResponseBody
  String exportData(@RequestBody RequestModel model) throws ServiceException {
    long jobId = exportService.scheduleExportJob(model);
    IUserAccount u = usersService.getUserAccount(SecurityUtils.getUsername());
    ResourceBundle backendMessages = Resources.getBundle(u.getLocale());
    return MessageFormat.format(backendMessages.getString("export.status.message"), u.getEmail(), jobId);
  }
}
