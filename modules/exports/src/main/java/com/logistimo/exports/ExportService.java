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

package com.logistimo.exports;

import com.google.gson.Gson;
import com.logistimo.AppFactory;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.entity.IJobStatus;
import com.logistimo.exports.builders.ExportBuilder;
import com.logistimo.exports.model.ExportConfigModel;
import com.logistimo.exports.model.ExportRequestModel;
import com.logistimo.exports.model.RequestModel;
import com.logistimo.reports.constants.ReportViewType;
import com.logistimo.reports.plugins.internal.ExportModel;
import com.logistimo.reports.plugins.internal.QueryHelper;
import com.logistimo.services.ServiceException;
import com.logistimo.utils.JobUtil;
import org.apache.camel.ExchangePattern;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Mohan Raja
 */
@Service
public class ExportService {

  public static final String REPORT_VIEW_TYPE = "reportViewType";


  private ExportBuilder exportBuilder;

  @Autowired
  public void setExportBuilder(ExportBuilder exportBuilder) {
    this.exportBuilder = exportBuilder;
  }

  public long scheduleExport(ExportModel model, String jobSubType) {
    Map<String, String>
        filters = new HashMap<>(model.filters.size() + model.additionalData.size() + 1, 1);
    filters.putAll(model.filters);
    filters.putAll(model.additionalData);
    filters.put("userId", model.userId);

    clearMetaFilters(filters);

    long jobId = JobUtil
        .createJob(SecurityUtils.getCurrentDomainId(), SecurityUtils.getUsername(), null,
            IJobStatus.TYPE_EXPORT,
            jobSubType, filters);

    ExportRequestModel exportRequestModel = new ExportRequestModel();
    ExportConfigModel ecModel = new ExportConfigModel();
    ecModel.value = new Gson().toJson(model);
    exportRequestModel.meshId = "exp_" + System.currentTimeMillis() + "_" + jobId;
    exportRequestModel.addRequestConfig(ecModel);
    submitExportJob(exportRequestModel);
    return jobId;
  }

  private void clearMetaFilters(Map<String, String> filters) {
    if(ReportViewType.BY_ASSET.toString().equals(filters.get(REPORT_VIEW_TYPE))) {
      filters.remove(QueryHelper.TOKEN + QueryHelper.QUERY_DVID);
    } else if(ReportViewType.BY_MANUFACTURER.toString().equals(filters.get(REPORT_VIEW_TYPE))) {
      filters.remove(QueryHelper.TOKEN + QueryHelper.QUERY_VENDOR_ID);
    } else if(ReportViewType.BY_ASSET_TYPE.toString().equals(filters.get(REPORT_VIEW_TYPE))) {
      filters.remove(QueryHelper.TOKEN + QueryHelper.QUERY_ATYPE);
    }
  }

  private void submitExportJob(ExportRequestModel model) {
    ProducerTemplate camelTemplate =
        AppFactory.get().getTaskService().getContext()
            .getBean("camel-client", ProducerTemplate.class);
    camelTemplate.sendBody("direct:export-tasks", ExchangePattern.InOnly, model);
  }

  public long scheduleExportJob(RequestModel model) throws ServiceException {
    String moduleName = model.getModule();
    ExportModel exportModel = exportBuilder.buildExportModel(model);
    return scheduleExport(exportModel, moduleName);
  }

}
