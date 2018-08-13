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

package com.logistimo.returns.models;

import com.google.gson.annotations.SerializedName;

import com.logistimo.constants.SourceConstants;
import com.logistimo.returns.models.submodels.ReturnsTrackingModel;

import java.util.List;

import javax.validation.constraints.NotNull;

import lombok.Getter;

/**
 * @author Mohan Raja
 */
@Getter
public class ReturnsRequestModel {

  @SerializedName("order_id")
  @NotNull(message = "Order Id cannot be null!!")
  private Long orderId;

  @SerializedName("geo_location")
  private Location location;

  private String comment;

  @NotNull(message = "Items cannot be null!!")
  private List<ReturnsItemModel> items;

  private Integer source = SourceConstants.MOBILE;

  @SerializedName("send_time")
  private Long sendTime;

  @SerializedName("tracking_details")
  private ReturnsTrackingModel trackingModel;

  @Getter
  public class Location {

    private Double latitude;

    private Double longitude;

    @SerializedName("geo_accuracy")
    private Double geoAccuracy;

    @SerializedName("geo_error")
    private String geoError;

  }

}
