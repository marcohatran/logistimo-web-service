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

/**
 *
 */
package com.logistimo.utils;

import com.logistimo.services.Resources;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * @author Arun
 */
public class GeoUtil {
  // Get the geo-error message string
  public static String getGeoErrorMessage(String geoErrorCode, Locale locale) {
    if (geoErrorCode == null || geoErrorCode.isEmpty()) {
      return null;
    }
    ResourceBundle messages = Resources.getBundle(locale);
    String str;
    switch (geoErrorCode) {
      case "1":
        str = messages.getString("geocodes.error.1");
        break;
      case "2":
        str = messages.getString("geocodes.error.2");
        break;
      case "3":
        str = messages.getString("geocodes.error.3");
        break;
      default:
        str = messages.getString("geocodes.error.someerror");
        break;
    }
    return str;
  }
}
