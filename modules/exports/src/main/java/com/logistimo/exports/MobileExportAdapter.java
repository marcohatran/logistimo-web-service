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

import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.entities.service.EntitiesService;
import com.logistimo.exception.InvalidDataException;
import com.logistimo.exports.model.RequestModel;
import com.logistimo.exports.util.MobileExportConstants;
import com.logistimo.inventory.entity.ITransaction;
import com.logistimo.logger.XLog;
import com.logistimo.materials.service.MaterialCatalogService;
import com.logistimo.orders.entity.IOrder;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Created by pratheeka on 20/03/18.
 */
@Component
public class MobileExportAdapter {

  private static final String BACKEND_MESSAGES = "BackendMessages";

  @Autowired
  private EntitiesService entitiesService;

  @Autowired
  private MaterialCatalogService materialCatalogService;

  private static final XLog xLogger = XLog.getLog(MobileExportAdapter.class);


  @Autowired
  private ExportService exportService;

  public void buildExportRequest(Map<String, String> params) throws ServiceException {
    StringBuilder caption = new StringBuilder("");
    RequestModel requestModel = new RequestModel();
    final SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.get().getBundle(BACKEND_MESSAGES, locale);
    requestModel.setDomainId(params.get(MobileExportConstants.DOMAIN_ID_KEY));
    setEndDate(params, caption, requestModel, backendMessages);
    setFromDate(params, caption, requestModel, backendMessages);
    setEntityDetails(params, requestModel, caption, backendMessages);
    setMaterialDetails(params, caption, requestModel, backendMessages);
    setExportType(requestModel, params, caption, backendMessages);
    Map<String,String> titles=new HashMap<>(1);
    titles.put("filters",caption.toString());
    requestModel.setTitles(titles);
    requestModel.setEmail(params.get(MobileExportConstants.EMAIL_ID_KEY));
    exportService.scheduleExportJob(requestModel);

  }

  private void setFromDate(Map<String, String> params, StringBuilder caption,
                           RequestModel requestModel, ResourceBundle backendMessages) {
    if (params.containsKey(MobileExportConstants.FROM_DATE_KEY)) {
      String fromDate = params.get(MobileExportConstants.FROM_DATE_KEY);
      requestModel.setFromDate(fromDate);
      caption.append(backendMessages.getString("from")).append(": ").append(fromDate)
          .append("   ");
    }
  }

  private void setEndDate(Map<String, String> params, StringBuilder caption,
                          RequestModel requestModel, ResourceBundle backendMessages) {
    if (params.containsKey(MobileExportConstants.END_DATE_KEY)) {
      String endDate = params.get(MobileExportConstants.END_DATE_KEY);
      requestModel.setEndDate(endDate);
      caption.append(backendMessages.getString("to")).append(": ").append(endDate)
          .append("   ");
    }
  }

  private void setMaterialDetails(Map<String, String> params, StringBuilder caption,
                                  RequestModel requestModel, ResourceBundle backendMessages)
      throws ServiceException {
    if (params.containsKey(MobileExportConstants.MATERIAL_ID_KEY)) {
      String materialId = params.get(MobileExportConstants.MATERIAL_ID_KEY);
      String materialName = materialCatalogService.getMaterial(Long.valueOf(materialId)).getName();
      caption.append(backendMessages.getString("material")).append(": ").append(materialName)
          .append("   ");
      requestModel.setMaterialId(materialId);
    }
  }

  private void setEntityDetails(Map<String, String> params, RequestModel requestModel,
                                StringBuilder caption, ResourceBundle backendMessages)
      throws ServiceException {
    if (params.containsKey(MobileExportConstants.KIOSK_ID_KEY)) {
      String kioskId = params.get(MobileExportConstants.KIOSK_ID_KEY);
      String kioskName = entitiesService.getKiosk(Long.valueOf(kioskId)).getName();
      caption.append(backendMessages.getString("kiosk")).append(": ").append(kioskName)
          .append("   ");
      requestModel.setEntityId(kioskId);
    }
  }

  private void setExportType(RequestModel requestModel, Map<String, String> params,
                            StringBuilder caption, ResourceBundle backendMessages) {
    String exportType = params.get(MobileExportConstants.EXPORT_TYPE_KEY);
    if (StringUtils.isEmpty(exportType)) {
      throw new InvalidDataException("No export type present");
    }
    xLogger.info("export type received is", exportType);
    if (exportType.equalsIgnoreCase(MobileExportConstants.ORDERS_KEY) || exportType
        .equalsIgnoreCase(MobileExportConstants.TRANSFERS_KEY)) {
      int orderType = Integer.parseInt(params.get(MobileExportConstants.ORDER_TYPE_KEY));
      requestModel.setOrderSubType(params.get(MobileExportConstants.ORDERS_SUB_TYPE_KEY));
      caption.append(backendMessages.getString("exports.type")).append(": ").append(
          getOrderSubTypeLabel(backendMessages,
              params.get(MobileExportConstants.ORDERS_SUB_TYPE_KEY), orderType)).append("   ");
      requestModel.setType(params.get(MobileExportConstants.ORDER_TYPE_KEY));
      requestModel.setTemplateId(exportType);
      requestModel.setModule(exportType);
    } else if (exportType.equalsIgnoreCase(MobileExportConstants.TRANSACTIONS_KEY)) {
      requestModel.setType(params.get(MobileExportConstants.TRANSACTIONS_TYPE_KEY));
      requestModel.setTemplateId(MobileExportConstants.TRANSACTIONS_KEY);
      requestModel.setModule(MobileExportConstants.TRANSACTIONS_KEY);
      if( params.containsKey(MobileExportConstants.TRANSACTIONS_TYPE_KEY))
      caption.append(backendMessages.getString("exports.type")).append(": ").append(
          getTransactionType(backendMessages,
              params.get(MobileExportConstants.TRANSACTIONS_TYPE_KEY))).append("   ");
    } else if (exportType.equalsIgnoreCase(MobileExportConstants.INVENTORY_KEY)) {
      requestModel.setTemplateId(MobileExportConstants.INVENTORY_TEMPLATE_KEY);
      requestModel.setModule(MobileExportConstants.INVENTORY_MODULE_KEY);
    }
  }

  private String getTransactionType(ResourceBundle backendMessages, String transactionType) {
    switch (transactionType) {
      case ITransaction.TYPE_ISSUE:
        return backendMessages.getString("exports.transaction.type.issue");
      case ITransaction.TYPE_RECEIPT:
        return backendMessages.getString("exports.transaction.type.receipt");
      case ITransaction.TYPE_PHYSICALCOUNT:
        return backendMessages.getString("exports.transaction.type.stockcount");
      case ITransaction.TYPE_WASTAGE:
        return backendMessages.getString("exports.transaction.type.wastage");
      case ITransaction.TYPE_TRANSFER:
        return backendMessages.getString("exports.transaction.type.transfers");
      case ITransaction.TYPE_RETURNS_INCOMING:
        return backendMessages.getString("exports.transaction.type.wastage");
      case ITransaction.TYPE_RETURNS_OUTGOING:
        return backendMessages.getString("exports.transaction.type.outgoing");
      default:
        return transactionType;
    }
  }

  private String getOrderSubTypeLabel(ResourceBundle backendMessages, String orderSubType,
                                     int isTransfers) {
    if (IOrder.TYPE_SALE.equalsIgnoreCase(orderSubType)) {
      if (isTransfers == IOrder.TRANSFER) {
        return backendMessages.getString("exports.outgoing");
      }
      return backendMessages.getString("exports.salesorders");
    } else {
      if (isTransfers == IOrder.TRANSFER) {
        return backendMessages.getString("exports.incoming");
      }
      return backendMessages.getString("exports.purchaseorders");
    }
  }


}
