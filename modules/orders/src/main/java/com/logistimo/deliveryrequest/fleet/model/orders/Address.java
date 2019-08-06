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

package com.logistimo.deliveryrequest.fleet.model.orders;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

import lombok.Data;

@Data
class Address implements Serializable {
    @SerializedName("a_l1")
    private String line1;

    @SerializedName("a_l2")
    private String line2;

    @SerializedName("strt")
    private String street;

    @SerializedName("plc_id")
    private String placeId; // Corresponds to Place.Id and all of the following data maps to Place Data

    @SerializedName("loc")
    private String locality;    // Locality inside the city/village

    @SerializedName("pin")
    private String postalCode;

    private String city;

    @SerializedName("s_dist")
    private String subDistrict; // Taluka

    @SerializedName("dst")
    private String district;

    private String state;

    @SerializedName("cntry")
    private String countryCode;

    @SerializedName("lmrk")
    private String landMark;

}
