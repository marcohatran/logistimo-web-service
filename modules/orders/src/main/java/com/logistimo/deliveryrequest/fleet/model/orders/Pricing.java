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

import lombok.Data;

@Data
public class Pricing {

    @SerializedName("dist")
    private Double distance;

    @SerializedName("sur")
    private Double surcharge = 0.0;
    @SerializedName("pr_t_sur")
    private Double timeSurcharge = 0.0;

    @SerializedName("p_ver")
    private String pricingVersion;
    @SerializedName("pr_init")
    private Double priceInitial;
    @SerializedName("lb_ch")
    private Double laborCharges;

    @SerializedName("d_cd")
    private String discountCode;
    @SerializedName("d_ver")
    private String discountVersion;
    @SerializedName("disc")
    private Double discount = 0.0;
    @SerializedName("d_md")
    private Integer genrationMode = 0;

    @SerializedName("tax")
    private Double tax;

    @SerializedName("pr_f")
    private Double priceFinal;  // Final price
    @SerializedName("pr_act")
    private Double priceActual; // Rounded(Final Price)

    @SerializedName("pr_fx")
    private Boolean fixedPrice = Boolean.FALSE;
    // Flag indicating if non-default pricing is to be applied to the order (the default pricing scheme/formula will not be used)

    @SerializedName("pr_rcv")
    private Double priceReceived;   // An indicator of whether the money was received.
    @SerializedName("pr_rcv_by")
    private String paymentReceivedBy; // This is present only until Payment component is not built.



}
