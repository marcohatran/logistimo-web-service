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
public class TransactionsHeader implements IHeader {

  // Method that returns the CSV header for the csv file used during bulk upload of transactions.
  public String getUploadableCSVHeader(Locale locale, String type) {
    ResourceBundle messages = Resources.getBundle(locale);
    StringJoiner
        header = new StringJoiner(CharacterConstants.COMMA);
    header.add(messages.getString("bulkupload.mnltransaction.entity.name.header"))
        .add(messages.getString("bulkupload.mnltransaction.material.name.header"))
        .add(messages.getString("bulkupload.mnltransaction.opening.stock.header"))
        .add(messages.getString("bulkupload.transaction.type.header"))
        .add(messages.getString("bulkupload.transaction.quantity.header"))
        .add(messages.getString("reason"))
        .add(messages.getString("inventory.material.status"))
        .add(messages.getString("bulkupload.transaction.related.entity.header"))
        .add(messages.getString("batch"))
        .add(messages.getString("bulkupload.transaction.batch.expiry.header"))
        .add(messages.getString("manufacturer"))
        .add(messages.getString("bulkupload.transaction.batch.manufactured.date.header"))
        .add(messages.getString("latitude"))
        .add(messages.getString("longitude"))
        .add(messages.getString("bulkupload.transaction.accuracy.header"))
        .add(messages.getString("bulkupload.transaction.date.pf.actual.transaction.header"));

    return header.toString();
  }
}
