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
import com.logistimo.utils.FieldLimits;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.StringJoiner;

/**
 * Created by charan on 06/03/17.
 */
public class MaterialsHeader implements IHeader {

    // Get the uploadable CSV header
    @Override
    public String getUploadableCSVHeader(Locale locale, String type) {
        ResourceBundle bundle = Resources.getBundle(locale);
        StringJoiner header = new StringJoiner(CharacterConstants.COMMA);
        header.add(bundle.getString("bulkupload.material.operation.header"))
            .add(MessageFormat.format(bundle.getString("bulkupload.material.name.header"),
                FieldLimits.MATERIAL_NAME_MIN_LENGTH, FieldLimits.TEXT_FIELD_MAX_LENGTH))
            .add(MessageFormat.format(bundle.getString("bulkupload.material.shortname.header"),
                FieldLimits.MATERIAL_SHORTNAME_MAX_LENGTH))
            .add(MessageFormat.format(bundle.getString("bulkupload.material.description.header"),
                FieldLimits.MATERIAL_DESCRIPTION_MAX_LENGTH))
            .add(MessageFormat
                .format(bundle.getString("bulkupload.material.additional.info.header"),
                    FieldLimits.MATERIAL_ADDITIONAL_INFO_MAX_LENGTH))
            .add(MessageFormat
                .format(bundle.getString("bulkupload.material.additional.info.on.mobile.header"),
                    FieldLimits.YES, FieldLimits.NO))
            .add(bundle.getString("bulkupload.tags.header"))
            .add(MessageFormat.format(bundle.getString("bulkupload.material.seasonal.header"),
                FieldLimits.YES, FieldLimits.NO))
            .add(MessageFormat
                .format(bundle.getString("bulkupload.material.msrp.header"), FieldLimits.MAX_PRICE))
            .add(MessageFormat
                .format(bundle.getString("bulkupload.material.retailer.price.header"),
                    FieldLimits.MAX_PRICE))
            .add(bundle.getString("bulkupload.material.currency.header"))
            .add(bundle.getString("bulkupload.material.new.name.header"))
            .add(MessageFormat.format(
                bundle.getString("bulkupload.material.custom.id.header"),
                FieldLimits.TEXT_FIELD_MAX_LENGTH))
            .add(MessageFormat
                .format(bundle.getString("bulkupload.material.batch.management.header"),
                    FieldLimits.YES, FieldLimits.NO))
            .add(MessageFormat
                .format(bundle.getString("bulkupload.material.temperature.header"),
                    FieldLimits.YES, FieldLimits.NO))
            .add(bundle.getString("bulkupload.material.temperature.min.header"))
            .add(bundle.getString("bulkupload.material.temperature.max.header"));

        return header.toString();
    }
}
