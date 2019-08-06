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

import com.logistimo.auth.SecurityConstants;
import com.logistimo.constants.CharacterConstants;
import com.logistimo.constants.Constants;
import com.logistimo.services.Resources;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.utils.FieldLimits;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.StringJoiner;

/**
 * Created by charan on 06/03/17.
 */
public class UsersHeader implements IHeader {
    @Override
    public String getUploadableCSVHeader(Locale locale, String type) {
        ResourceBundle bundle = Resources.getBundle(locale);
        StringJoiner
            header = new StringJoiner(CharacterConstants.COMMA);
        header.add(bundle.getString("bulkupload.user.operation.header"))
            .add(MessageFormat.format(bundle.getString("bulkupload.user.id.header"),
                FieldLimits.USERID_MIN_LENGTH, FieldLimits.USERID_MAX_LENGTH))
            .add(bundle.getString("bulkupload.password.header"))
            .add(bundle.getString("bulkupload.confirm.password.header"))
            .add(MessageFormat.format(bundle.getString("bulkupload.role.header"),
                SecurityConstants.ROLE_DOMAINOWNER,
                SecurityConstants.ROLE_SERVICEMANAGER,
                SecurityConstants.ROLE_KIOSKOWNER))
            .add(MessageFormat.format(bundle.getString("bulkupload.permission.header"),IUserAccount.PERMISSION_DEFAULT,
                IUserAccount.PERMISSION_VIEW, IUserAccount.PERMISSION_ASSET))
            .add(MessageFormat.format(bundle.getString("bulkupload.token.expiry.header"), FieldLimits.TOKEN_EXPIRY_MIN, FieldLimits.TOKEN_EXPIRY_MAX))
            .add(MessageFormat.format(bundle.getString("bulkupload.user.firstname.header"), FieldLimits.FIRSTNAME_MIN_LENGTH, FieldLimits.TEXT_FIELD_MAX_LENGTH))
            .add(MessageFormat.format(bundle.getString("bulkupload.user.lastname.header"), FieldLimits.TEXT_FIELD_MAX_LENGTH))
            .add(MessageFormat.format(bundle.getString("bulkupload.user.mobile.header"), FieldLimits.MOBILE_PHONE_MAX_LENGTH))
            .add(MessageFormat.format(bundle.getString("bulkupload.user.email.header"), FieldLimits.EMAIL_MAX_LENGTH, bundle.getString("role.servicemanager")))
            .add(bundle.getString("bulkupload.country.header"))
            .add(bundle.getString("bulkupload.language.header"))
            .add(bundle.getString("bulkupload.timezone.header"))
            .add(MessageFormat.format(bundle.getString("bulkupload.user.gender.header"),
                IUserAccount.GENDER_MALE, IUserAccount.GENDER_FEMALE, IUserAccount.GENDER_OTHER))
            .add(MessageFormat.format(bundle.getString("bulkupload.user.date.of.birth.header"), Constants.DATE_FORMAT_EXCEL))
            .add(MessageFormat.format(bundle.getString("bulkupload.alternate.phone.header"), FieldLimits.LAND_PHONE_MAX_LENGTH))
            .add(bundle.getString("bulkupload.state.header"))
            .add(bundle.getString("bulkupload.district.header"))
            .add(bundle.getString("bulkupload.taluk.header"))
            .add(MessageFormat.format(bundle.getString("bulkupload.village.header"),CharacterConstants.EMPTY,
                FieldLimits.TEXT_FIELD_MAX_LENGTH))
            .add(MessageFormat.format(bundle.getString("bulkupload.street.address.header"), FieldLimits.STREET_ADDRESS_MAX_LENGTH))
            .add(MessageFormat.format(bundle.getString("bulkupload.zipcode.header"), FieldLimits.TEXT_FIELD_MAX_LENGTH))
            .add(bundle.getString("bulkupload.old.password.header"))
            .add(MessageFormat.format(bundle.getString("bulkupload.user.custom.id.header"),
                FieldLimits.TEXT_FIELD_MAX_LENGTH))
            .add(MessageFormat.format(bundle.getString("bulkupload.mobile.brand.header"), FieldLimits. TEXT_FIELD_MAX_LENGTH))
            .add(MessageFormat.format(bundle.getString("bulkupload.mobile.model.header"), FieldLimits.TEXT_FIELD_MAX_LENGTH))
            .add(MessageFormat.format(bundle.getString("bulkupload.imei.number.header"), FieldLimits.TEXT_FIELD_MAX_LENGTH))
            .add(MessageFormat.format(bundle.getString("bulkupload.sim.provider.header"), FieldLimits.TEXT_FIELD_MAX_LENGTH))
            .add(MessageFormat.format(bundle.getString("bulkupload.sim.id.header"), FieldLimits.TEXT_FIELD_MAX_LENGTH))
            .add(bundle.getString("bulkupload.tags.header"))
            .add(bundle.getString("bulkupload.mobile.gui.theme.header"))
        ;
        return header.toString();
    }
}
