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

package com.logistimo.exports.processors;

import com.codahale.metrics.Meter;
import com.logistimo.communications.MessageHandlingException;
import com.logistimo.constants.CharacterConstants;
import com.logistimo.context.StaticApplicationContext;
import com.logistimo.domains.entity.IDomain;
import com.logistimo.domains.service.DomainsService;
import com.logistimo.entity.IJobStatus;
import com.logistimo.exports.model.ExportResponseModel;
import com.logistimo.exports.util.EmailHelper;
import com.logistimo.exports.util.ExportConstants;
import com.logistimo.logger.XLog;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;
import com.logistimo.utils.JobUtil;
import com.logistimo.utils.MetricsUtil;

import org.apache.camel.Handler;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * @author Mohan Raja
 */
public class DataXExportStatusProcessor {
  private static Meter jmsMeter = MetricsUtil.getMeter(DataXExportStatusProcessor.class,
      "DataXExportStatusProcessor");

  private static final XLog xLogger = XLog.getLog(DataXExportStatusProcessor.class);

  @Handler
  public void execute(ExportResponseModel model)
      throws MessageHandlingException, IOException, ServiceException {
    jmsMeter.mark();
    ResourceBundle backendMessages = Resources.get().getBundle("BackendMessages", Locale.ENGLISH);
    Long jobId = Long.parseLong(model.meshId.substring(model.meshId.lastIndexOf(
        CharacterConstants.UNDERSCORE) + 1));

    IJobStatus jobStatus = JobUtil.getJobById(jobId);
    IDomain domain=StaticApplicationContext.getBean(DomainsService.class).getDomain(jobStatus.getDomainId());
    EmailHelper emailHelper = StaticApplicationContext.getBean(EmailHelper.class);
    String fileName =
        emailHelper.getFileName(emailHelper.getFileName(jobStatus),
            jobStatus.getMetadataMap().get(ExportConstants.EXPORT_TIME),domain.getName());
    if (ExportConstants.STATUS_SUCCESS.equals(model.status)) {
      try {
        emailHelper.sendMail(jobStatus, model.meshId);
      } catch (Exception e) {
        xLogger.warn("Email could not be sent!!", e);
      } finally {
        JobUtil.setJobCompleted(jobStatus.getJobId(), IJobStatus.TYPE_EXPORT, 1, fileName,
            backendMessages, model.meshId);
      }
    } else if (ExportConstants.STATUS_FAILED.equals(model.status)) {
      JobUtil.setJobFailed(jobId, model.reason);
      emailHelper.sendMail(jobStatus, model.meshId, "Error during export.");
    }
  }


}
