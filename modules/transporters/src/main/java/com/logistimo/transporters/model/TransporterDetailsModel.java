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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransporterDetailsModel extends TransporterModel {

  public TransporterDetailsModel(TransporterModel transporterModel) {
    super(transporterModel);
  }

  @SerializedName("desc")
  private String description;

  @SerializedName("cnt")
  private String country;

  @SerializedName("st")
  private String state;

  @SerializedName("ds")
  private String district;

  @SerializedName("tlk")
  private String taluk;

  @SerializedName("ct")
  private String city;

  @SerializedName("stn")
  private String streetAddress;

  @SerializedName("pin")
  private String pinCode;

  @SerializedName("is_api_enabled")
  private boolean isApiEnabled;

  @SerializedName("url")
  private String url;

  @SerializedName("ac_id")
  private String accountId;

  @SerializedName("secret")
  private String secretToken;

  @SerializedName("secret_updated")
  private boolean isSecretUpdated;

  @SerializedName("creby")
  private String createdBy;

  @SerializedName("crebyn")
  private String createdByName;

  @SerializedName("cre_t")
  private String createdAt;

}