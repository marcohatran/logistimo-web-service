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

package com.logistimo.exports;

import com.logistimo.exception.InvalidDataException;
import com.logistimo.exports.model.RequestModel;
import com.logistimo.exports.util.MobileExportConstants;
import com.logistimo.logger.XLog;
import com.logistimo.services.ServiceException;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Created by pratheeka on 20/03/18.
 */
@Component
public class MobileExportAdapter {

  private static final XLog xLogger = XLog.getLog(MobileExportAdapter.class);


  @Autowired
  private ExportService exportService;

  public void buildExportRequest(Map<String, String> params) throws ServiceException{
      RequestModel requestModel = new RequestModel();
      requestModel.setDomainId(params.get(MobileExportConstants.DOMAIN_ID_KEY));
      requestModel.setEndDate(params.get(MobileExportConstants.END_DATE_KEY));
      requestModel.setFromDate(params.get(MobileExportConstants.FROM_DATE_KEY));
      requestModel.setEntityId(params.get(MobileExportConstants.KIOSK_ID_KEY));
      requestModel.setMaterialId(params.get(MobileExportConstants.MATERIAL_ID_KEY));
      setExportType(requestModel, params);
      requestModel.setEmail(params.get(MobileExportConstants.EMAIL_ID_KEY));
      exportService.scheduleExportJob(requestModel);

  }

  public void setExportType(RequestModel requestModel, Map<String, String> params) {
    String exportType = params.get(MobileExportConstants.EXPORT_TYPE_KEY);
    if (StringUtils.isEmpty(exportType)) {
      throw new InvalidDataException("No export type present");
    }
    xLogger.info("export type received is",exportType);
    if (exportType.equalsIgnoreCase(MobileExportConstants.ORDERS_KEY) || exportType
        .equalsIgnoreCase(MobileExportConstants.TRANSFERS_KEY)) {
      requestModel.setOrderSubType(params.get(MobileExportConstants.ORDERS_SUB_TYPE_KEY));
      requestModel.setType(params.get(MobileExportConstants.ORDER_TYPE_KEY));
      requestModel.setTemplateId(exportType);
      requestModel.setModule(exportType);
    } else if (exportType.equalsIgnoreCase(MobileExportConstants.TRANSACTIONS_KEY)) {
      requestModel.setType(params.get(MobileExportConstants.TRANSACTIONS_TYPE_KEY));
      requestModel.setTemplateId(MobileExportConstants.TRANSACTIONS_KEY);
      requestModel.setModule(MobileExportConstants.TRANSACTIONS_KEY);
    }else if(exportType.equalsIgnoreCase(MobileExportConstants.INVENTORY_KEY)){
      requestModel.setTemplateId(MobileExportConstants.INVENTORY_TEMPLATE_KEY);
      requestModel.setModule(MobileExportConstants.INVENTORY_MODULE_KEY);
    }
  }

}
