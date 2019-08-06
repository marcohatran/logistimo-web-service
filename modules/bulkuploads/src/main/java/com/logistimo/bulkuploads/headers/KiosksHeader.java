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
public class KiosksHeader implements IHeader {
    @Override
    public String getUploadableCSVHeader(Locale locale, String type) {
        ResourceBundle bundle = Resources.getBundle(locale);
        StringJoiner header = new StringJoiner(CharacterConstants.COMMA);
        if ("assets".equalsIgnoreCase(type)) {
            header.add(bundle.getString("kiosk"))
                .add(bundle.getString("custom.entity"))
                .add(bundle.getString("city"))
                .add(bundle.getString("district"))
                .add(bundle.getString("state"))
                .add(bundle.getString("asset.type.header"))
                .add(bundle.getString("bulk.asset.id"))
                .add(bundle.getString("asset.manufacturer.header"))
                .add(bundle.getString("model"))
                .add(bundle.getString("sensor.device.id"))
                .add(bundle.getString("sim1"))
                .add(bundle.getString("sim1.id"))
                .add(bundle.getString("sim1.ntw.provider"))
                .add(bundle.getString("sim2"))
                .add(bundle.getString("sim2.id"))
                .add(bundle.getString("sim2.ntw.provider"))
                .add(bundle.getString("imei.of.monitoring.asset"));
        } else {
            header.add(bundle.getString("bulkupload.entity.operation.header"))
                .add(MessageFormat.format(bundle.getString("bulkupload.entity.name.header"),
                    FieldLimits.ENTITY_NAME_MIN_LENGTH, FieldLimits.TEXT_FIELD_MAX_LENGTH))
                .add(bundle.getString("bulkupload.entity.users.header"))
                .add(bundle.getString("bulkupload.country.header"))
                .add(bundle.getString("bulkupload.state.header"))
                .add(MessageFormat.format(bundle.getString("bulkupload.village.header"), CharacterConstants.ASTERISK,
                    FieldLimits.TEXT_FIELD_MAX_LENGTH))
                .add(MessageFormat.format(bundle.getString("bulkupload.latitude.header"), FieldLimits.LAT_LONG_MAX_DIGITS_AFTER_DECIMAL, FieldLimits.LATITUDE_MIN, FieldLimits.LATITUDE_MAX))
                .add(MessageFormat.format(bundle.getString("bulkupload.longitude.header"), FieldLimits.LAT_LONG_MAX_DIGITS_AFTER_DECIMAL, FieldLimits.LONGITUDE_MIN, FieldLimits.LONGITUDE_MAX))
                .add(bundle.getString("bulkupload.district.header"))
                .add(bundle.getString("bulkupload.taluk.header"))
                .add(MessageFormat.format(bundle.getString("bulkupload.street.address.header"), FieldLimits.STREET_ADDRESS_MAX_LENGTH))
                .add(MessageFormat.format(bundle.getString("bulkupload.zipcode.header"), FieldLimits.TEXT_FIELD_MAX_LENGTH))
                .add(bundle.getString("bulkupload.material.currency.header"))
                .add(MessageFormat.format(bundle.getString("bulkupload.tax.header"), FieldLimits.TAX_MIN_VALUE, FieldLimits.TAX_MAX_VALUE))
                .add(MessageFormat.format(bundle.getString("bulkupload.tax.id.header"), FieldLimits.TEXT_FIELD_MAX_LENGTH))
                .add(MessageFormat.format(bundle.getString("bulkupload.inventory.policy.header"), FieldLimits.SYSTEM_DETERMINED_REPLENISHMENT))
                .add(MessageFormat.format(bundle.getString("bulkupload.service.level.header"), FieldLimits.MIN_SERVICE_LEVEL, FieldLimits.MAX_SERVICE_LEVEL))
                .add(bundle.getString("bulkupload.kiosk.name.new.header"))
                .add(bundle.getString("bulkupload.entity.add.all.materials.header"))
                .add(bundle.getString("bulkupload.entity.materials.header"))
                .add(bundle.getString("bulkupload.entity.materials.initial.stock.header"))
                .add(bundle.getString("bulkupload.entity.customers.header"))
                .add(bundle.getString("bulkupload.entity.vendors.header"))
                .add(bundle.getString("bulkupload.tags.header"))
                .add(MessageFormat.format(bundle.getString("bulkupload.entity.custom.id.header"), FieldLimits.TEXT_FIELD_MAX_LENGTH))
                .add(bundle.getString("bulkupload.entity.disable.batch.management"));
        }

        return header.toString();
    }
}
