/*
 * Copyright © 2019 Logistimo.
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

package com.logistimo.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by kumargaurav on 26/06/19.
 */
public class LogiDateDeserializer implements JsonDeserializer<Date> {

  @Override
  public Date deserialize(JsonElement json, Type type,
                          JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {

    if(json == null) {
      return null;
    }
    try {
      return new Date(json.getAsLong());
    } catch (NumberFormatException nex) {
      return parseDate(json.getAsString());
    }
  }

  private Date parseDate(String date) {
    try {
      SimpleDateFormat
          dateFormat =
          new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
      return dateFormat.parse(date);
    } catch(Exception e) {
      return new Date(date);
    }
  }
}
