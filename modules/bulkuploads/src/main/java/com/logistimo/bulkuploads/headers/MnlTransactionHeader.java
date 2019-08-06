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

package com.logistimo.bulkuploads.headers;

import com.logistimo.constants.CharacterConstants;
import com.logistimo.services.Resources;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.StringJoiner;

/**
 * Created by charan on 06/03/17.
 */
public class MnlTransactionHeader implements IHeader {

  @Override
  public String getUploadableCSVHeader(Locale locale, String type) {
    ResourceBundle messages = Resources.getBundle(locale);
    StringJoiner header = new StringJoiner(CharacterConstants.COMMA);
    header.add(messages.getString("bulkupload.mnltransaction.entity.name.header"))
        .add(messages.getString("bulkupload.mnltransaction.material.name.header"))
        .add(messages.getString("bulkupload.mnltransaction.opening.stock.header"))
        .add(messages.getString("bulkupload.mnltransaction.reporting.period.header"))
        .add(messages.getString("receipts"))
        .add(messages.getString("issues"))
        .add(messages.getString("transactions.wastage.upper"))
        .add(messages.getString("bulkupload.mnltransaction.stockouts.duration.header"))
        .add(messages.getString("bulkupload.mnltransaction.manual.consumption.header"))
        .add(messages.getString("bulkupload.mnltransaction.computed.consumption.header"))
        .add(messages.getString("bulkupload.mnltransaction.ordered.quantity.header"))
        .add(messages.getString("bulkupload.mnltransaction.fulfilled.quantity.header"))
        .add(messages.getString("bulkupload.order.tags.header"))
        .add(messages.getString("bulkupload.mnltransaction.vendor.header"));
    return header.toString();
  }
}
