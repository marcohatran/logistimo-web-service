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

package com.logistimo.api.models.configuration;

import com.google.gson.annotations.SerializedName;

import com.logistimo.transporters.model.ConsignmentCategoryModel;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TransportersConfigModel {

  @SerializedName("cfgs")
  private List<TransporterConfigModel> transporterConfigList;

  @SerializedName("lud")
  private String lastUpdated;

  @SerializedName("ub")
  private String updatedBy;

  @SerializedName("ubn")
  private String updatedByName;

  public void addTransporterConfigModel(TransporterConfigModel model) {
    if(transporterConfigList == null) {
      transporterConfigList = new ArrayList<>();
    }
    transporterConfigList.add(model);
  }

  @Data
  public static class TransporterConfigModel {
    @SerializedName("tsp_id")
    private String tspId;

    @SerializedName("name")
    private String name;

    @SerializedName("enabled")
    private boolean enabled;

    @SerializedName("df_ctg")
    private String defaultCategory;

    @SerializedName("categories")
    private List<ConsignmentCategoryModel> categories;
  }
}