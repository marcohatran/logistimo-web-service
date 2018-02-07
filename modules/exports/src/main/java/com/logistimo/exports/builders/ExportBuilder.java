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

package com.logistimo.exports.builders;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.constants.Constants;
import com.logistimo.domains.entity.IDomain;
import com.logistimo.domains.service.DomainsService;
import com.logistimo.exports.model.RequestModel;
import com.logistimo.logger.XLog;
import com.logistimo.reports.plugins.internal.ExportModel;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.ServiceException;
import com.logistimo.utils.LocalDateUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by pratheeka on 02/02/18.
 */
@Component
public class ExportBuilder {

  private static final XLog xLogger = XLog.getLog(ExportBuilder.class);

  private DomainsService domainsService;

  @Autowired
  public void setDomainsService(DomainsService domainsService) {
    this.domainsService = domainsService;
  }

  public ExportModel buildExportModel(RequestModel model)
      throws ServiceException {
    ExportModel exportModel = new ExportModel();
    final SecureUserDetails userDetails = SecurityUtils.getUserDetails();
    Long domainId = userDetails.getCurrentDomainId();
    IDomain domain = domainsService.getDomain(domainId);
    GsonBuilder builder = new GsonBuilder();
    builder.excludeFieldsWithoutExposeAnnotation();
    Gson gson = builder.create();
    model.setDomainId(String.valueOf(domainId));
    Type type = new TypeToken<Map<String, String>>() {
    }.getType();
    DomainConfig dc = DomainConfig.getInstance(domainId);
    Map<String, String> filters = gson.fromJson(gson.toJson(model), type);
    exportModel.userId = userDetails.getUsername();
    exportModel.locale = userDetails.getLocale().getLanguage();
    exportModel.timezone = userDetails.getTimezone();
    exportModel.templateId = model.getTemplateId();
    exportModel.filters = filters;
    model.setFromDate(getDomainTime(model.getFromDate(),dc.getTimezone()));
    model.setEndDate(getDomainTime(model.getEndDate(),dc.getTimezone()));
    exportModel.additionalData = new HashMap<>();
    exportModel.additionalData.put("typeId", "DEFAULT");
    exportModel.additionalData.put("domainName", domain.getName());
    exportModel.additionalData.put("domainTimezone", dc.getTimezone());
    exportModel.additionalData.put("exportTime", LocalDateUtil
        .formatCustom(new Date(), Constants.DATETIME_CSV_FORMAT, userDetails.getTimezone()));
    exportModel.titles = model.getTitles();
    return exportModel;
  }

  private String getDomainTime(String date, String timezone) {
    if(StringUtils.isNotEmpty(date)) {

      SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATETIME_CSV_MILLIS_FORMAT);
      Date startDate;
      try {
        startDate=LocalDateUtil.parseCustom(date, Constants.DATE_FORMAT_CSV,timezone);
        return sdf.format(startDate);
      } catch (ParseException e) {
        xLogger.warn("Exception parsing date", e);
      }
    }
    return date;
  }
}
