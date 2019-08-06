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

package com.logistimo.exports.handlers;

import com.logistimo.config.models.DomainConfig;
import com.logistimo.constants.CharacterConstants;
import com.logistimo.constants.Constants;
import com.logistimo.constants.SourceConstants;
import com.logistimo.context.StaticApplicationContext;
import com.logistimo.entities.entity.IKiosk;
import com.logistimo.entities.service.EntitiesService;
import com.logistimo.entities.service.EntitiesServiceImpl;
import com.logistimo.inventory.TransactionUtil;
import com.logistimo.inventory.entity.ITransaction;
import com.logistimo.logger.XLog;
import com.logistimo.materials.entity.IMaterial;
import com.logistimo.materials.service.MaterialCatalogService;
import com.logistimo.materials.service.impl.MaterialCatalogServiceImpl;
import com.logistimo.services.Resources;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.users.service.UsersService;
import com.logistimo.users.service.impl.UsersServiceImpl;
import com.logistimo.utils.BigUtil;
import com.logistimo.utils.GeoUtil;
import com.logistimo.utils.LocalDateUtil;
import com.logistimo.utils.NumberUtil;
import com.logistimo.utils.StringUtil;

import org.apache.commons.lang.StringEscapeUtils;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Created by charan on 08/03/17.
 */
public class TransactionExportHandler implements IExportHandler {

  private static final XLog xLogger = XLog.getLog(TransactionExportHandler.class);


  private final ITransaction transaction;

  public TransactionExportHandler(ITransaction transaction){
    this.transaction = transaction;
  }

  // Get the CSV header - Modified to suit export only.
  @Override
  public String getCSVHeader(Locale locale, DomainConfig dc, String type) {
    ResourceBundle messages = Resources.getBundle(locale);

    StringBuilder header = new StringBuilder();
    header.append(messages.getString("transactionid")).append(CharacterConstants.COMMA)
        .append(messages.getString("type")).append(CharacterConstants.COMMA)
        .append(messages.getString("trackingobjecttype")).append(CharacterConstants.COMMA)
        .append(messages.getString("trackingid")).append(CharacterConstants.COMMA)
        .append(messages.getString("kiosk")).append(CharacterConstants.SPACE)
        .append(messages.getString("id")).append(CharacterConstants.COMMA)
        .append(messages.getString("customid.entity")).append(CharacterConstants.COMMA)
        .append(messages.getString("kiosk.name")).append(CharacterConstants.COMMA)
        .append(messages.getString("material")).append(CharacterConstants.SPACE)
        .append(messages.getString("id")).append(CharacterConstants.COMMA)
        .append(messages.getString("customid.material")).append(CharacterConstants.COMMA)
        .append(messages.getString("material.name")).append(CharacterConstants.COMMA)
        .append(messages.getString("quantity")).append(CharacterConstants.COMMA)
        .append(messages.getString("openingstock")).append(CharacterConstants.COMMA)
        .append(messages.getString("closingstock")).append(CharacterConstants.COMMA)
        .append(messages.getString("stockadjustment")).append(CharacterConstants.COMMA)
        .append(messages.getString("reason")).append(CharacterConstants.COMMA)
        .append(messages.getString("material")).append(CharacterConstants.SPACE)
        .append(messages.getString("status.lower")).append(CharacterConstants.COMMA)
        .append(messages.getString("time")).append(CharacterConstants.COMMA)
        .append(messages.getString("date.actual.transaction")).append(CharacterConstants.COMMA)
        .append(messages.getString("relatedentity.id")).append(CharacterConstants.COMMA)
        .append(messages.getString("customid.relatedentity")).append(CharacterConstants.COMMA)
        .append(messages.getString("relatedentity.name")).append(CharacterConstants.COMMA)
        .append(messages.getString("kiosk")).append(CharacterConstants.SPACE)
        .append(messages.getString("tags.lower")).append(CharacterConstants.COMMA)
        .append(messages.getString("material")).append(CharacterConstants.SPACE)
        .append(messages.getString("tags.lower")).append(CharacterConstants.COMMA)
        .append(messages.getString("batchid")).append(CharacterConstants.COMMA)
        .append(messages.getString("batch")).append(CharacterConstants.SPACE)
        .append(messages.getString("expiry.lower")).append(CharacterConstants.COMMA)
        .append(messages.getString("batch")).append(CharacterConstants.SPACE)
        .append(messages.getString("manufacturer.lower")).append(CharacterConstants.COMMA)
        .append(messages.getString("batch")).append(CharacterConstants.SPACE)
        .append(messages.getString("manufactured.lower")).append(CharacterConstants.COMMA)
        .append(messages.getString("openingstock")).append(CharacterConstants.SPACE)
        .append(CharacterConstants.O_SBRACKET).append(messages.getString("in"))
        .append(CharacterConstants.SPACE)
        .append(messages.getString("batch.lower")).append(CharacterConstants.C_SBRACKET)
        .append(CharacterConstants.COMMA)
        .append(messages.getString("closingstock")).append(CharacterConstants.SPACE)
        .append(CharacterConstants.O_SBRACKET).append(messages.getString("in"))
        .append(CharacterConstants.SPACE).append(messages.getString("batch.lower"))
        .append(CharacterConstants.C_SBRACKET).append(CharacterConstants.COMMA)
        .append(messages.getString("country")).append(CharacterConstants.COMMA)
        .append(messages.getString("state")).append(CharacterConstants.COMMA)
        .append(messages.getString("district")).append(CharacterConstants.COMMA)
        .append(messages.getString("taluk")).append(CharacterConstants.COMMA)
        .append(messages.getString("village")).append(CharacterConstants.COMMA)
        .append(messages.getString("streetaddress")).append(CharacterConstants.COMMA)
        .append(messages.getString("zipcode")).append(CharacterConstants.COMMA)
        .append(messages.getString("latitude")).append(CharacterConstants.COMMA)
        .append(messages.getString("longitude")).append(CharacterConstants.COMMA)
        .append(messages.getString("altitude")).append(CharacterConstants.COMMA)
        .append(messages.getString("accuracy")).append(CharacterConstants.SPACE)
        .append(CharacterConstants.O_BRACKET).append(messages.getString("meters"))
        .append(CharacterConstants.C_BRACKET).append(CharacterConstants.COMMA)
        .append(messages.getString("gps")).append(CharacterConstants.SPACE)
        .append(messages.getString("errors.small")).append(CharacterConstants.COMMA)
        .append(messages.getString("transaction")).append(CharacterConstants.SPACE)
        .append(messages.getString("source")).append(CharacterConstants.COMMA)
        .append(messages.getString("createdby")).append(CharacterConstants.SPACE)
        .append(messages.getString("id")).append(CharacterConstants.COMMA)
        .append(messages.getString("createdby")).append(CharacterConstants.SPACE)
        .append(messages.getString("customid.lower")).append(CharacterConstants.COMMA)
        .append(messages.getString("createdby")).append(CharacterConstants.SPACE)
        .append(messages.getString("fullname.lower"));

    return header.toString();
  }

  // Get the CSV
  @Override
  public String toCSV(Locale locale, String timezone, DomainConfig dc, String type) {
    ResourceBundle messages = Resources.getBundle(locale);
    try {
      // Get services
      EntitiesService as = StaticApplicationContext.getBean(EntitiesServiceImpl.class);
      UsersService usersService = StaticApplicationContext.getBean(UsersServiceImpl.class);
      MaterialCatalogService mcs = StaticApplicationContext.getBean(
          MaterialCatalogServiceImpl.class);
      IKiosk k = as.getKiosk(transaction.getKioskId(), false);
      List<String> ktgs = k.getTags();
      IMaterial m = mcs.getMaterial(transaction.getMaterialId());
      List<String> mtgs = m.getTags();
      IUserAccount u;
      String cbFullName = null, cbCustomId = null;
      if (transaction.getSourceUserId() != null) {
        try {
          u = usersService.getUserAccount(transaction.getSourceUserId()); // required for user ID
          cbFullName = u.getFullName();
          cbCustomId = u.getCustomId();
        } catch (Exception e) {
          cbFullName = Constants.UNKNOWN;
        }
      }
      // Add the tracking object type, tracking object id and the human readable transaction id
      String totStr = "";
      if (transaction.getTrackingId() != null) {
        // Get the totStr. // If tot is null then set tot to "Order" - Because earlier there was no "tot" corresponding to "tid"
        totStr = (transaction.getTrackingObjectType() == null ?
            ITransaction.TYPE_ORDER : transaction.getTrackingObjectType());

        if (ITransaction.TYPE_ORDER.equalsIgnoreCase(totStr)) {
          totStr = messages.getString("order");
        } else if (ITransaction.TYPE_TRANSFER.equalsIgnoreCase(totStr)) {
          totStr = messages.getString("transactions.transfer.upper");
        } else if (ITransaction.TRACKING_OBJECT_TYPE_ORDER_SHIPMENT.equalsIgnoreCase(totStr)) {
          totStr = messages.getString("order.shipment");
        } else if (ITransaction.TRACKING_OBJECT_TYPE_TRANSFER_SHIPMENT.equalsIgnoreCase(totStr)) {
          totStr = messages.getString("transfer.shipment");
        } else if (ITransaction.TRACKING_OBJECT_TYPE_ISSUE_TRANSACTION.equals(totStr)) {
          totStr = messages.getString("transactions.issue.upper");
        } else if (ITransaction.TRACKING_OBJECT_TYPE_RECEIPT_TRANSACTION.equals(totStr)) {
          totStr = messages.getString("transactions.receipt.upper");
        } else if (ITransaction.TRACKING_OBJECT_TYPE_TRANSFER.equals(totStr)) {
          totStr = messages.getString("transfer.returns");
        } else if (ITransaction.TRACKING_OBJECT_TYPE_ORDER.equals(totStr)) {
          totStr = messages.getString("order.returns");
        }
      }
      // Related entity
      IKiosk lk;
      String lkName = null, lkCustomId = null;
      if (transaction.getLinkedKioskId() != null) {
        try {
          lk = as.getKiosk(transaction.getLinkedKioskId(), false);
          lkName = lk.getName();
          lkCustomId = lk.getCustomId();
        } catch (Exception e) {
          lkName = Constants.UNKNOWN;
        }
      }
      // Source
      String srcStr = null;
      if (SourceConstants.WEB.equals(transaction.getSrc())) {
        srcStr = SourceConstants.WEBSTR;
      } else if (SourceConstants.MOBILE.equals(transaction.getSrc())) {
        srcStr = SourceConstants.MOBILESTR;
      } else if (SourceConstants.UPLOAD.equals(transaction.getSrc())) {
        srcStr = SourceConstants.UPLOADSTR;
      } else if (SourceConstants.SMS.equals(transaction.getSrc())) {
        srcStr = SourceConstants.SMSSTR;
      }

      StringBuilder csvSb = new StringBuilder();
      csvSb.append(transaction.getKeyString()).append(CharacterConstants.COMMA)
          .append(getDisplayName(this.transaction.getType(), locale)).append(CharacterConstants.COMMA)
          .append(totStr).append(CharacterConstants.COMMA)
          .append(transaction.getTrackingId() != null ? transaction.getTrackingId() : CharacterConstants.EMPTY).append(CharacterConstants.COMMA)
          .append(transaction.getKioskId()).append(CharacterConstants.COMMA)
          .append(k.getCustomId() != null ? StringEscapeUtils.escapeCsv(k.getCustomId())
              : CharacterConstants.EMPTY).append(CharacterConstants.COMMA)
          .append(StringEscapeUtils.escapeCsv(k.getName())).append(CharacterConstants.COMMA)
          .append(transaction.getMaterialId()).append(CharacterConstants.COMMA)
          .append(m.getCustomId() != null ? StringEscapeUtils.escapeCsv(m.getCustomId())
              : CharacterConstants.EMPTY).append(CharacterConstants.COMMA)
          .append(StringEscapeUtils.escapeCsv(m.getName())).append(CharacterConstants.COMMA)
          .append(BigUtil.getFormattedValue(transaction.getQuantity())).append(CharacterConstants.COMMA)
          .append(BigUtil.getFormattedValue(transaction.getOpeningStock())).append(CharacterConstants.COMMA)
          .append(BigUtil.getZeroIfNull(transaction.getClosingStock())).append(CharacterConstants.COMMA)
          .append(BigUtil.getZeroIfNull(transaction.getStockDifference())).append(CharacterConstants.COMMA)
          .append(transaction.getReason() != null ? StringEscapeUtils.escapeCsv(transaction.getReason()) : CharacterConstants.EMPTY)
          .append(CharacterConstants.COMMA)
          .append(transaction.getMaterialStatus() != null ? StringEscapeUtils.escapeCsv(transaction.getMaterialStatus()) : CharacterConstants.EMPTY)
          .append(CharacterConstants.COMMA)
          .append(LocalDateUtil.formatCustom(transaction.getTimestamp(), Constants.DATETIME_CSV_FORMAT, timezone))
          .append(CharacterConstants.COMMA)
          .append(transaction.getAtd() != null ? LocalDateUtil.formatCustom(transaction.getAtd(), Constants.DATE_FORMAT_CSV, null)
              : CharacterConstants.EMPTY).append(CharacterConstants.COMMA)
          .append(transaction.getLinkedKioskId() != null ? transaction.getLinkedKioskId() : CharacterConstants.EMPTY).append(CharacterConstants.COMMA)
          .append(lkCustomId != null ? StringEscapeUtils.escapeCsv(lkCustomId)
              : CharacterConstants.EMPTY).append(CharacterConstants.COMMA)
          .append(lkName != null ? StringEscapeUtils.escapeCsv(lkName) : CharacterConstants.EMPTY)
          .append(CharacterConstants.COMMA)
          .append(ktgs != null && !ktgs.isEmpty() ? StringEscapeUtils
              .escapeCsv(StringUtil.getCSV(ktgs, CharacterConstants.SEMICOLON))
              : CharacterConstants.EMPTY).append(CharacterConstants.COMMA)
          .append(mtgs != null && !mtgs.isEmpty() ? StringEscapeUtils
              .escapeCsv(StringUtil.getCSV(mtgs, CharacterConstants.SEMICOLON))
              : CharacterConstants.EMPTY).append(CharacterConstants.COMMA)
          .append(transaction.getBatchId() != null ? StringEscapeUtils.escapeCsv(transaction.getBatchId()) : CharacterConstants.EMPTY)
          .append(CharacterConstants.COMMA)
          .append(transaction.getBatchExpiry() != null ? LocalDateUtil.formatCustom(transaction.getBatchExpiry(), Constants.DATE_FORMAT_CSV, null)
              : CharacterConstants.EMPTY).append(CharacterConstants.COMMA)
          .append(transaction.getBatchManufacturer() != null ? StringEscapeUtils.escapeCsv(transaction.getBatchManufacturer()) : CharacterConstants.EMPTY)
          .append(CharacterConstants.COMMA)
          .append(transaction.getBatchManufacturedDate() != null ? LocalDateUtil.formatCustom(transaction.getBatchManufacturedDate(), Constants.DATE_FORMAT_CSV, null)
              : CharacterConstants.EMPTY).append(CharacterConstants.COMMA)
          .append(
              transaction.getBatchId() != null && !transaction.getBatchId().isEmpty() ? transaction.getOpeningStockByBatch() : CharacterConstants.EMPTY)
          .append(CharacterConstants.COMMA)
          .append(
              transaction.getBatchId() != null && !transaction.getBatchId().isEmpty() ? transaction.getClosingStockByBatch() : CharacterConstants.EMPTY)
          .append(CharacterConstants.COMMA)
          .append(k.getCountry() != null ? StringEscapeUtils.escapeCsv(k.getCountry())
              : CharacterConstants.EMPTY).append(CharacterConstants.COMMA)
          .append(k.getState() != null ? StringEscapeUtils.escapeCsv(k.getState())
              : CharacterConstants.EMPTY).append(CharacterConstants.COMMA)
          .append(k.getDistrict() != null ? StringEscapeUtils.escapeCsv(k.getDistrict())
              : CharacterConstants.EMPTY).append(CharacterConstants.COMMA)
          .append(k.getTaluk() != null ? StringEscapeUtils.escapeCsv(k.getTaluk())
              : CharacterConstants.EMPTY).append(CharacterConstants.COMMA)
          .append(k.getCity() != null ? StringEscapeUtils.escapeCsv(k.getCity())
              : CharacterConstants.EMPTY).append(CharacterConstants.COMMA)
          .append(k.getStreet() != null ? StringEscapeUtils.escapeCsv(k.getStreet())
              : CharacterConstants.EMPTY).append(CharacterConstants.COMMA)
          .append(k.getPinCode() != null ? StringEscapeUtils.escapeCsv(k.getPinCode())
              : CharacterConstants.EMPTY).append(CharacterConstants.COMMA)
          .append(transaction.getLatitude() != 0D ? transaction.getLatitude() : CharacterConstants.EMPTY).append(CharacterConstants.COMMA)
          .append(transaction.getLongitude() != 0D ? transaction.getLongitude() : CharacterConstants.EMPTY).append(CharacterConstants.COMMA)
          .append(transaction.getAltitude() != 0D ? transaction.getAltitude() : CharacterConstants.EMPTY).append(CharacterConstants.COMMA)
          .append(transaction.getGeoAccuracy() != 0D ? NumberUtil.getDoubleValue(transaction.getGeoAccuracy()) : CharacterConstants.EMPTY)
          .append(CharacterConstants.COMMA)
          .append(
              transaction.getGeoErrorCode() != null ? StringEscapeUtils.escapeCsv(GeoUtil.getGeoErrorMessage(transaction.getGeoErrorCode(), locale))
                  : CharacterConstants.EMPTY).append(CharacterConstants.COMMA)
          .append(srcStr != null ? StringEscapeUtils.escapeCsv(srcStr) : CharacterConstants.EMPTY)
          .append(CharacterConstants.COMMA)
          .append(transaction.getSourceUserId() != null ? transaction.getSourceUserId() : CharacterConstants.EMPTY).append(CharacterConstants.COMMA)
          .append(cbCustomId != null ? StringEscapeUtils.escapeCsv(cbCustomId)
              : CharacterConstants.EMPTY).append(CharacterConstants.COMMA)
          .append(cbFullName != null ? StringEscapeUtils.escapeCsv(cbFullName)
              : CharacterConstants.EMPTY);

      return csvSb.toString();
    } catch (Exception e) {
      xLogger.warn("{0} in toCSV on Transaction {1}: {2}", e.getClass().getName(), transaction.getKeyString(),
          e.getMessage());
      return null;
    }
  }

  public static String getDisplayName(String transType, Locale locale) {
    return getDisplayName(transType, DomainConfig.TRANSNAMING_DEFAULT, locale);
  }

  public static String getDisplayName(String transType, String transNaming, Locale locale) {
    return TransactionUtil.getDisplayName(transType, transNaming, locale);
  }
}
