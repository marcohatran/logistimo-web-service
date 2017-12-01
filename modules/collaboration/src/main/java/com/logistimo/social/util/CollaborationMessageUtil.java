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

package com.logistimo.social.util;

import com.logistimo.services.Resources;

import org.apache.commons.lang.StringUtils;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Created by kumargaurav on 21/11/17.
 */
public class CollaborationMessageUtil {

  private CollaborationMessageUtil() {
  }


  public static String constructMessage(String key, Locale locale) {
    String message = getParameterizedMessage(key, locale);
    if (StringUtils.isNotEmpty(message)) {
      return message;
    }
    return key;
  }

  public static String constructMessage(String key, Locale locale, Object[] params) {
    String message = getParameterizedMessage(key, locale);
    if (message != null && params != null && params.length > 0) {
      return MessageFormat.format(message, params);
    }
    return key;
  }

  private static String getParameterizedMessage(String key, Locale locale) {
    ResourceBundle
        resourceBundle =
        Resources.get()
            .getBundle("CollaborationMessages", locale != null ? locale : Locale.ENGLISH);
    return resourceBundle.getString(key);
  }
}
