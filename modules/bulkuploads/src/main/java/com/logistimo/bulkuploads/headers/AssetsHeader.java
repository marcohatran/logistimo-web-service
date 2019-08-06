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

/**
 * Created by charan on 06/03/17.
 */
public class AssetsHeader implements IHeader {

  @Override
  public String getUploadableCSVHeader(Locale locale, String type) {
    ResourceBundle bundle = Resources.getBundle(locale);
    StringBuilder builder = new StringBuilder();
    builder.append(bundle.getString("kiosk")).append(CharacterConstants.COMMA)
        .append(bundle.getString("custom.entity")).append(CharacterConstants.COMMA)
        .append(bundle.getString("city")).append(CharacterConstants.COMMA)
        .append(bundle.getString("district")).append(CharacterConstants.COMMA)
        .append(bundle.getString("state")).append(CharacterConstants.COMMA)
        .append(bundle.getString("asset.type")).append(CharacterConstants.COMMA)
        .append(bundle.getString("bulk.asset.id")).append(CharacterConstants.COMMA)
        .append(bundle.getString("manufacturer")).append(CharacterConstants.COMMA)
        .append(bundle.getString("model")).append(CharacterConstants.COMMA)
        .append(bundle.getString("monitored.asset.manufacture.year")).append(CharacterConstants.COMMA)
        .append(bundle.getString("asset.monitored.owners")).append(CharacterConstants.COMMA)
        .append(bundle.getString("asset.monitored.maintainers")).append(CharacterConstants.COMMA)
        .append(bundle.getString("sensor.device.id")).append(CharacterConstants.COMMA)
        .append(bundle.getString("sim1")).append(CharacterConstants.COMMA)
        .append(bundle.getString("sim1.id")).append(CharacterConstants.COMMA)
        .append(bundle.getString("sim1.ntw.provider")).append(CharacterConstants.COMMA)
        .append(bundle.getString("sim2")).append(CharacterConstants.COMMA)
        .append(bundle.getString("sim2.id")).append(CharacterConstants.COMMA)
        .append(bundle.getString("sim2.ntw.provider")).append(CharacterConstants.COMMA)
        .append(bundle.getString("imei.of.monitoring.asset")).append(CharacterConstants.COMMA)
        .append(bundle.getString("manufacturer.name")).append(CharacterConstants.COMMA)
        .append(bundle.getString("asset.model")).append(CharacterConstants.COMMA)
        .append(bundle.getString("monitoring.asset.manufacture.year")).append(CharacterConstants.COMMA)
        .append(bundle.getString("asset.monitoring.owners")).append(CharacterConstants.COMMA)
        .append(bundle.getString("asset.monitoring.maintainers"));
    return builder.toString();
  }
}
