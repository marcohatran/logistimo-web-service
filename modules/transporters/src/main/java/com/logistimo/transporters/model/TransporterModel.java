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

package com.logistimo.transporters.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

@Data
@NoArgsConstructor
public class TransporterModel {

  // copy constructor
  public TransporterModel(TransporterModel model) {
    setId(model.getId());
    setSerialNo(model.getSerialNo());
    setSourceDomainId(model.getSourceDomainId());
    setName(model.getName());
    setType(model.getType());
    setPhoneNumber(model.getPhoneNumber());
    setDefaultCategoryId(model.getDefaultCategoryId());
    setTspName(model.getTspName());
    setTspId(model.getTspId());
    setCategories(model.getCategories());
    setVehicle(model.getVehicle());
    setAttributes(model.getAttributes());
    setPriceEnabled(model.isPriceEnabled());
    setUpdatedBy(model.getUpdatedBy());
    setUpdatedByName(model.getUpdatedByName());
    setUpdatedAt(model.getUpdatedAt());
  }

  @SerializedName("id")
  private Long id;

  @SerializedName("sno")
  private Integer serialNo;

  @SerializedName("sdid")
  private Long sourceDomainId;

  @SerializedName("name")
  private String name;

  @SerializedName("type")
  private TransporterType type;

  @SerializedName("phnm")
  private String phoneNumber;

  @SerializedName("vhcl")
  private String vehicle;

  @SerializedName("dfCtgId")
  private String defaultCategoryId;

  @SerializedName("tsp_id")
  private String tspId;

  @SerializedName("tsp_name")
  private String tspName;

  @SerializedName("categories")
  private List<ConsignmentCategoryModel> categories;

  @SerializedName("attributes")
  @Singular
  private Map<String, String> attributes;

  @SerializedName("price_en")
  private boolean priceEnabled;

  @SerializedName("udby")
  private String updatedBy;

  @SerializedName("udbyn")
  private String updatedByName;

  @SerializedName("t")
  private String updatedAt;

}