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

package com.logistimo.deliveryrequest.fleet.model.estimates;

import com.google.gson.annotations.SerializedName;

import java.util.Collection;

import lombok.Data;

/**
 * Created by kumargaurav on 28/02/19.
 */
@Data
public class PricingInfo {

  @SerializedName("wts")
  private Collection<Double> weights;
  @SerializedName("eff_wt")
  private Double effectiveWeight;
  @SerializedName("dist")
  private Double distance;

  @SerializedName("sur")
  private Double surcharge;
  @SerializedName("tsur")
  private Boolean timeSurcharge;

  @SerializedName("price")
  private Double price;
  @SerializedName("l_chrg")
  private Double laborCharges;
  @SerializedName("pr_ver")
  private String pricingVersion;

  @SerializedName("disc")
  private Double discount = 0.0;
  @SerializedName("d_ver")
  private String discountVersion;
  @SerializedName("d_cd")
  private String discountCode;
  @SerializedName("tax")
  private Double tax;
  @SerializedName("fn_pr")
  private Double finalPrice;
  @SerializedName("act_pr")
  private Double actualPrice;

  @SerializedName("fx_pr")
  private Boolean fixPrice;
}
