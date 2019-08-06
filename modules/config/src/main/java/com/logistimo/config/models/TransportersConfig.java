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

package com.logistimo.config.models;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TransportersConfig implements Serializable {

  private static final String ENABLED_TRANSPORTERS = "enbledtrs";
  private static final String LAST_UPDATED = "lupd";
  private static final String UPDATED_BY = "updby";

  private List<TSPConfig> enabledTransporters;
  private String lastUpdated;
  private String updatedBy;

  public void addEnabledTspConfig(TSPConfig config) {
    if(enabledTransporters == null) {
      enabledTransporters = new ArrayList<>();
    }
    enabledTransporters.add(config);
  }

  public TransportersConfig(JSONObject jsonObject) {
    enabledTransporters = new ArrayList<>();
    JSONArray enabledTransportersJson = jsonObject.getJSONArray(ENABLED_TRANSPORTERS);
    if(enabledTransportersJson != null) {
      for(int i=0;i<enabledTransportersJson.length();i++) {
        addEnabledTspConfig(new TSPConfig(enabledTransportersJson.getJSONObject(i)));
      }
    }
    setLastUpdated(jsonObject.getString(LAST_UPDATED));
    setUpdatedBy(jsonObject.getString(UPDATED_BY));
  }

  public JSONObject toJSONObject() {
    JSONObject jsonObject = new JSONObject();
    if(enabledTransporters != null) {
      JSONArray arr = new JSONArray();
      enabledTransporters.forEach(t -> arr.put(t.toJSONObject()));
      jsonObject.put(ENABLED_TRANSPORTERS, arr);
      jsonObject.put(LAST_UPDATED, lastUpdated);
      jsonObject.put(UPDATED_BY, updatedBy);
    }
    return jsonObject;
  }

  @Data
  public static class TSPConfig implements Serializable {

    private static final String TSP_ID = "tid";
    private static final String DEFAULT_CATEGORY_ID = "defctgid";

    private String tspId;
    private String defaultCategoryId;

    public TSPConfig(String tspId, String defaultCategory) {
      this.tspId = tspId;
      this.defaultCategoryId = defaultCategory;
    }

    private TSPConfig(JSONObject jsonObject) {
      this.tspId = jsonObject.getString(TSP_ID);
      this.defaultCategoryId = jsonObject.getString(DEFAULT_CATEGORY_ID);
    }

    private JSONObject toJSONObject() {
      JSONObject jsonObject = new JSONObject();
      jsonObject.put(TSP_ID, tspId);
      jsonObject.put(DEFAULT_CATEGORY_ID, defaultCategoryId);
      return jsonObject;
    }
  }
}