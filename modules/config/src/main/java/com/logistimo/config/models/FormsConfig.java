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

package com.logistimo.config.models;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class FormsConfig implements Serializable {

  private static final String KEY_FORMS = "forms";
  private List<String> forms;

  public FormsConfig() {}

  public FormsConfig(JSONObject jsonObject) {
    JSONArray jsonArray = jsonObject.getJSONArray(KEY_FORMS);
    forms = new ArrayList<>(jsonArray.length());
    for (int i = 0; i < jsonArray.length(); i++) {
      forms.add(jsonArray.getString(i));
    }
  }

  public JSONObject toJSONObject() {
    JSONObject jsonObject = new JSONObject();
    if(forms != null) {
      jsonObject.put(KEY_FORMS, forms);
    }
    return jsonObject;
  }

  public String toJSONString() {
    JSONArray jsonArray = new JSONArray(forms);
    return jsonArray.toString();
  }
}